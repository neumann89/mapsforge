/**
 * @author Patrick Jungermann
 * @version $Id: createTables.sql 2089 2012-08-05 23:17:17Z Patrick.Jungermann@googlemail.com $
 */
BEGIN;

DROP TABLE IF EXISTS ch_edge CASCADE;
DROP TABLE IF EXISTS ch_vertex CASCADE;

CREATE TABLE ch_vertex
(
    id integer NOT NULL,
	level integer NOT NULL,
	hierarchy_depth integer NOT NULL DEFAULT 1,
	CONSTRAINT pk_ch_v PRIMARY KEY (id),
	CONSTRAINT fk_id FOREIGN KEY (id)
	    REFERENCES rg_vertex (id) INITIALLY DEFERRED DEFERRABLE,
	CONSTRAINT chk1 CHECK (id >= 0),
	CONSTRAINT chk2 CHECK (hierarchy_depth >= 1)
);

/**
 * The column original_edge_id contains only values for "normal" edges and NULL for each shortcut.
 * Both "bypassed edges" are only present for shortcuts and otherwise NULL.
 */
CREATE TABLE ch_edge
(
	id integer NOT NULL,
  	source_id integer NOT NULL,
  	target_id integer NOT NULL,
  	weight integer NOT NULL,
  	undirected boolean NOT NULL,
	original_edge_id integer,
	bypassed_edge_id1 integer,
	bypassed_edge_id2 integer,
	original_edge_count integer NOT NULL DEFAULT 1,
  	CONSTRAINT pk_ch_e PRIMARY KEY (id),
  	CONSTRAINT fk_source_id FOREIGN KEY (source_id)
  	    REFERENCES ch_vertex (id) INITIALLY DEFERRED DEFERRABLE,
  	CONSTRAINT fk_target_id FOREIGN KEY (target_id)
  	    REFERENCES ch_vertex (id) INITIALLY DEFERRED DEFERRABLE,
  	CONSTRAINT fk_original_edge_id FOREIGN KEY (original_edge_id)
  	    REFERENCES rg_edge (id) INITIALLY DEFERRED DEFERRABLE,
  	CONSTRAINT fk_bypassed_edge_id1 FOREIGN KEY (bypassed_edge_id1)
  	    REFERENCES ch_edge (id) INITIALLY DEFERRED DEFERRABLE,
  	CONSTRAINT fk_bypassed_edge_id2 FOREIGN KEY (bypassed_edge_id2)
  	    REFERENCES ch_edge (id) INITIALLY DEFERRED DEFERRABLE,
  	CONSTRAINT chk1 CHECK (id >= 0),
  	CONSTRAINT chk2 CHECK (weight >= 0),
  	CONSTRAINT chk3 CHECK (original_edge_count >= 1)
);

END;