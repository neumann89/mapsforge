DROP TABLE IF EXISTS dummy_edge CASCADE;
DROP TABLE IF EXISTS dummy_node CASCADE;
DROP TABLE IF EXISTS old_from_target CASCADE;
DROP TABLE IF EXISTS turn_restrictions CASCADE;
DROP TABLE IF EXISTS rg_edge CASCADE;
DROP TABLE IF EXISTS rg_vertex CASCADE;
DROP TABLE IF EXISTS rg_hwy_lvl CASCADE;
DROP FUNCTION IF EXISTS remove_unconnected_vertices();


CREATE TABLE rg_hwy_lvl (
	id INTEGER PRIMARY KEY NOT NULL,
	name VARCHAR(100)
);

CREATE TABLE rg_vertex (
	id INTEGER NOT NULL,
	--osm_node_id BIGINT UNIQUE NOT NULL,   (not unique anymore)  
	osm_node_id BIGINT NOT NULL,
	lon DOUBLE PRECISION NOT NULL,
	lat DOUBLE PRECISION NOT NULL,
	CONSTRAINT pk_rgv PRIMARY KEY (id)
);

CREATE TABLE rg_edge( 
	id INTEGER NOT NULL,
	source_id INTEGER NOT NULL,  
	target_id INTEGER NOT NULL,
	weight INTEGER NOT NULL,
	osm_way_id BIGINT NOT NULL,
 	name VARCHAR NOT NULL, 
 	ref VARCHAR NOT NULL, 
 	destination VARCHAR, 
 	length_meters DOUBLE PRECISION NOT NULL,
	undirected BOOLEAN NOT NULL,
	urban BOOLEAN NOT NULL, 
	roundabout BOOLEAN NOT NULL, 
	hwy_lvl INTEGER NOT NULL, 
	longitudes DOUBLE PRECISION[] NOT NULL, 
	latitudes DOUBLE PRECISION[] NOT NULL,
	CONSTRAINT pk_rge PRIMARY KEY (id),
	CONSTRAINT fk_1 FOREIGN KEY (source_id) REFERENCES rg_vertex (id) INITIALLY DEFERRED DEFERRABLE,
	CONSTRAINT fk_2 FOREIGN KEY (target_id) REFERENCES rg_vertex (id) INITIALLY DEFERRED DEFERRABLE,
	CONSTRAINT fk_3 FOREIGN KEY (hwy_lvl) REFERENCES rg_hwy_lvl (id) INITIALLY DEFERRED DEFERRABLE,
	CONSTRAINT chk_1 CHECK (length_meters >= 0)
);

CREATE TABLE turn_restrictions(
	id INTEGER NOT NULL,
	osm_relation_id BIGINT NOT NULL,
	via_node INTEGER NOT NULL,
	from_edge INTEGER NOT NULL,
	to_edge INTEGER NOT NULL,
	CONSTRAINT pk_rgtr PRIMARY KEY (id),
	CONSTRAINT fk_4 FOREIGN KEY (via_node) REFERENCES rg_vertex (id) INITIALLY DEFERRED DEFERRABLE,
	CONSTRAINT fk_5 FOREIGN KEY (from_edge) REFERENCES rg_edge (id) INITIALLY DEFERRED DEFERRABLE,
	CONSTRAINT fk_6 FOREIGN KEY (to_edge) REFERENCES rg_edge (id) INITIALLY DEFERRED DEFERRABLE
);

CREATE TABLE old_from_target(
	edge_id INTEGER NOT NULL,
	target_id INTEGER NOT NULL,
	CONSTRAINT pk_eid PRIMARY KEY (edge_id)
);

CREATE TABLE dummy_node(
	id INTEGER NOT NULL,
	original_id INTEGER NOT NULL,
	updated_edge INTEGER NOT NULL,
	CONSTRAINT pk_dn PRIMARY KEY (id)
);

CREATE TABLE dummy_edge(
	id INTEGER NOT NULL,
	original_id INTEGER NOT NULL,
	source_id INTEGER NOT NULL,
	CONSTRAINT pk_de PRIMARY KEY (id)
);

CREATE OR REPLACE FUNCTION node_split() RETURNS void AS '
DECLARE
new_vertex_id integer;
from_and_via RECORD;
via_vertex RECORD;
BEGIN
-- go through all from edges (with via)
FOR from_and_via IN 
					SELECT from_edge,via_node
	    			FROM turn_restrictions
	    			GROUP BY from_edge,via_node
LOOP
	-- save old target
    INSERT INTO old_from_target VALUES (from_and_via.from_edge,from_and_via.via_node);
	--get via-vertex
	SELECT INTO via_vertex * FROM rg_vertex WHERE id=from_and_via.via_node;
		IF NOT FOUND THEN
    	RAISE EXCEPTION ''Via_node % nicht gefunden'', from_and_via.via_node;
		END IF;
	-- get new id for new vertex
	SELECT INTO new_vertex_id max(id) FROM rg_vertex;
	new_vertex_id=new_vertex_id+1;
	-- insert new vertex (with new id but apart from that same attributes like via_vertex)
	INSERT INTO rg_vertex VALUES (new_vertex_id,via_vertex.osm_node_id,via_vertex.lon,via_vertex.lat);
	INSERT INTO dummy_node VALUES (new_vertex_id,via_vertex.id, from_and_via.from_edge);
	-- update from-edge
	UPDATE rg_edge SET target_id = new_vertex_id WHERE id = from_and_via.from_edge;
END LOOP;
END;
' LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION edge_split() RETURNS void AS '
DECLARE
new_edge_id integer;
from_and_old_target RECORD;
via_edge RECORD;
from_edge_obj RECORD;

BEGIN
-- go through all from edges (with old target)
FOR from_and_old_target IN 
					SELECT edge_id from_edge,target_id via_node
	    			FROM old_from_target
	    			GROUP BY edge_id,target_id
LOOP
	SELECT INTO from_edge_obj * FROM rg_edge WHERE id=from_and_old_target.from_edge;
	-- get all outgoing edges at the via node
	FOR via_edge IN 
					SELECT *
	    			FROM rg_edge
					WHERE 	source_id=from_and_old_target.via_node
						OR	target_id=from_and_old_target.via_node
	LOOP
		--ignore ingoing edges of via_node(old target)
		IF (NOT(via_edge.target_id=from_and_old_target.via_node AND via_edge.undirected=false)) THEN
			-- check if it is verboten
			IF NOT (EXISTS (
							SELECT * FROM turn_restrictions WHERE from_edge=from_and_old_target.from_edge 
															AND		via_node=from_and_old_target.via_node
															AND 	to_edge=via_edge.id
							)
					)
			THEN
				-- create new id for the new edge
				SELECT INTO new_edge_id max(id) FROM rg_edge;
				new_edge_id=new_edge_id+1;
				-- insert edge case 1
				IF (via_edge.target_id=from_and_old_target.via_node) THEN
					-- id, source, target, weight, osm_way_id, name, ref, destination, length_meters, undirected, urban, roundabout, hwy_lvl, longitudes, latitudes
					INSERT INTO rg_edge VALUES (new_edge_id, from_edge_obj.target_id, via_edge.source_id, via_edge.weight, via_edge.osm_way_id, via_edge.name, via_edge.ref, via_edge.destination, via_edge.length_meters, false, via_edge.urban, via_edge.roundabout, via_edge.hwy_lvl,via_edge.longitudes, via_edge.latitudes);
					INSERT INTO dummy_edge VALUES (new_edge_id,via_edge.id, from_edge_obj.target_id);
				ELSE
					-- insert edge case 2
					INSERT INTO rg_edge VALUES (new_edge_id, from_edge_obj.target_id, via_edge.target_id, via_edge.weight, via_edge.osm_way_id, via_edge.name, via_edge.ref, via_edge.destination, via_edge.length_meters, false, via_edge.urban, via_edge.roundabout, via_edge.hwy_lvl,via_edge.longitudes, via_edge.latitudes);
					INSERT INTO dummy_edge VALUES (new_edge_id,via_edge.id, from_edge_obj.target_id);
				END IF;
			END IF;
		END IF;
	END LOOP;
END LOOP;
END;
' LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION include_turn_restrictions() RETURNS void AS '
BEGIN
	PERFORM node_split();
	PERFORM edge_split();
END;
' LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION remove_unconnected_vertices() RETURNS integer AS '
DECLARE
counter integer;
vertex_id integer;
BEGIN
	counter=0;
	FOR vertex_id IN
					SELECT via_node
	    			FROM turn_restrictions
					GROUP BY via_node
	LOOP
		IF NOT (EXISTS 	(SELECT * FROM rg_edge WHERE source_id=vertex_id OR target_id=vertex_id) )
		THEN
			DELETE FROM rg_vertex WHERE id=vertex_id;
			counter=counter+1;
		END IF;
	END LOOP;
	RETURN counter;
END;
' LANGUAGE plpgsql;
