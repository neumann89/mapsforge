/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.routing.graph.creation.sql;

import gnu.trove.map.hash.TIntObjectHashMap;
import org.mapsforge.routing.GeoCoordinate;
import org.mapsforge.routing.graph.creation.extraction.CompleteEdge;
import org.mapsforge.routing.graph.creation.extraction.CompleteVertex;
import org.mapsforge.routing.graph.creation.extraction.turnRestrictions.TurnRestriction;
import org.mapsforge.routing.graph.creation.weighting.DistanceMetric;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class to load the complete graph into an SQL file. First vertex by vertex, then edge by edge and last
 * turn restriction by turn restriction.
 *
 * @author Robert Fels
 */
public class SqlWriter { // TODO: rewrite this class to be more like a real writer implementation

    /**
     * Logger, used for this class.
     */
    private static final Logger LOGGER = Logger.getLogger(SqlWriter.class.getName());

    // sql statements to be written to the sql file :
    private static final String SQL_COPY_EDGES = "COPY rg_edge (id, source_id, target_id, weight, osm_way_id, name, ref, destination, length_meters, undirected, urban, roundabout, hwy_lvl, longitudes, latitudes) FROM stdin;";
    private static final String SQL_COPY_VERTICES = "COPY rg_vertex (id, osm_node_id, lon, lat) FROM stdin;";
    private static final String SQL_COPY_HWY_LEVELS = "COPY rg_hwy_lvl (id, name) FROM stdin;";
    private static final String SQL_COPY_TURN_RESTRICTIONS = "COPY turn_restrictions (id, osm_relation_id, via_node, from_edge, to_edge) FROM stdin;";
    private static final String SQL_TERMINAL = "\\.";

    // sql create tables file (to be written to the output) :
    private static final String RESOURCE_NAME_CREATE_TABLES_SQL = "createTables.sql";

    private State state = State.NOTHING_INITIALIZED;

    // HashMap to store the highway levels
    private TIntObjectHashMap<String> hwyLevels;

    // count new written edges and vertices
    private int amountOfEdgesWritten = 0;
    private int amountOfVerticesWritten = 0;
    private int amountOfTRWritten = 0;

    // output files
    private final File outputFile;
    private PrintWriter out;

    private long neededTime;

    /**
     * Constructor for HHAdapter
     *
     * @param hwyLevels      highway levels mapped from int to level name
     * @param outputFilePath Path for the output sql file
     */
    public SqlWriter(TIntObjectHashMap<String> hwyLevels, String outputFilePath) {
        this.hwyLevels = hwyLevels;
        this.outputFile = new File(outputFilePath);

        // initialize sql file
        if (state == State.NOTHING_INITIALIZED) {
            initializeWriteSQLFile();

        } else {
            throw new IllegalStateException("You have to initialize the SQL file first!");
        }
    }

    /**
     * Prepares for working on SQL file
     */
    public void initializeWriteSQLFile() {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("[RGC - write Rg into SQL file] begin writing graph data into DB file");
        }

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("[RGC - write Rg into SQL file] target file = '"
                    + outputFile.getAbsolutePath() + "'");
        }

        try {
            this.out = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(
                    new FileOutputStream(outputFile)), "UTF-8"));

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding is not available", e);

        } catch (FileNotFoundException e) {
            LOGGER.log(Level.SEVERE, "[RGC - write Rg into SQL file] Error: Output sql file not found!!", e);
        }

        // WRITE : createTables.sql
        neededTime = System.currentTimeMillis();
        try {
            writeCreateTables();

        } catch (IOException e) {
            throw new RuntimeException("cannot access file", e);
        }

        // WRITE : highway levels table

        out.println();
        out.println(SQL_COPY_HWY_LEVELS);
        writeHighwayLevels();
        finishSQLStatement();

        // change state
        state = State.SCHEMA_INITIALIZED;
    }

    /**
     * Adds one {@link CompleteVertex} to the SQL file, according to the needed format.
     *
     * @param cv the complete vertex
     */
    public void addCompleteVertex(final CompleteVertex cv) {
        if (cv != null) {
            if (state == State.SCHEMA_INITIALIZED) {
                initWriteVertices();
                // change state
                state = State.VERTICES_INITIALIZED;

            } else if (state != State.VERTICES_INITIALIZED) {
                throw new IllegalStateException(
                        "You have to initialize the vertices first, before the edges!");
            }

            writeVertex(
                    cv.getId(), cv.getOsmId(),
                    cv.getCoordinate().getLongitude(),
                    cv.getCoordinate().getLatitude());

        } else if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("[RGC - write Rg into SQL file] No vertex to write in sql file found!!!");
        }
    }

    /**
     * Adds one {@link CompleteEdge} to the SQL file, according to the needed format.
     *
     * @param ce the complete edge
     */
    public void addCompleteEdge(final CompleteEdge ce) {
        if (ce != null) {
            if (state == State.VERTICES_INITIALIZED) {
                // set finish line for sql statement
                finishSQLStatement();
                // begin with edges
                initWriteEdges();
                state = State.EDGES_INITIALIZED;

            } else if (state != State.EDGES_INITIALIZED) {
                throw new IllegalStateException("You have to initialize the edges after the vertices!");
            }

            // get longitudes and latitudes from waypoints
            final GeoCoordinate[] wp = ce.getAllWaypoints();
            final double[] lon = new double[wp.length];
            final double[] lat = new double[wp.length];
            for (int j = 0; j < wp.length; j++) {
                lon[j] = wp[j].getLongitude();
                lat[j] = wp[j].getLatitude();
            }
            // determine highway level
            Integer hwlLevel = 0;
            for (final int index : hwyLevels.keys()) {
                if (hwyLevels.get(index).equals(ce.getType())) {
                    hwlLevel = index;
                    break;
                }
            }
            // write edge
            writeEdge(
                    ce.getSourceId(),
                    ce.getTargetId(),
                    ce.getWeight(),
                    ce.getId(),
                    ce.getName(),
                    ce.getRef(),
                    ce.getDestination(),
                    new DistanceMetric().getCostDouble(ce), // get length of the edge
                    !ce.isOneWay(),
                    false,
                    ce.isRoundabout(),
                    hwlLevel,
                    lon,
                    lat);

        } else if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("[RGC - write Rg into SQL file] No edge to write in sql file found!!!");
        }
    }

    /**
     * Adds a turn restriction to the SQL file.
     *
     * @param tr turn restriction
     */
    public void addTurnRestriction(final TurnRestriction tr) {
        if (tr != null) {
            if (state == State.EDGES_INITIALIZED) {
                // set finish line for sql statement
                finishSQLStatement();
                // begin with TR
                initWriteTurnRestrictions();
                state = State.TURN_RESTRICTIONS_INITIALIZED;

            } else if (state != State.TURN_RESTRICTIONS_INITIALIZED) {
                throw new IllegalStateException("State now: " + state.name() +
                        "You have to initialize the turn restrictions after the edges!");
            }

            writeTurnRestrictions(tr.getId(), tr.getOsmId(), tr.getViaNodeId(), tr.getFromEdgeId(),
                    tr.getToEdgeId());

        } else if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("[RGC - write Rg into SQL file] No turn restriction to write in sql file found!!!");
        }
    }

    /**
     * Adds the finish line to the SQL file.
     */
    public void finishSQLStatement() {
        out.println(SQL_TERMINAL);
    }

    /**
     * Initialization needed before the writing of vertices.
     */
    public void initWriteVertices() {
        out.println();
        out.println(SQL_COPY_VERTICES);
    }

    /**
     * Initialization needed before the writing of edges.
     */
    public void initWriteEdges() {
        out.println();
        out.println(SQL_COPY_EDGES);
    }

    /**
     * Initialization needed before the writing of turn restrictions.
     */
    public void initWriteTurnRestrictions() {
        out.println();
        out.println(SQL_COPY_TURN_RESTRICTIONS);
    }

    /**
     * Finish the writing of the SQL file, closing output stream etc.
     */
    public void finishWriteSQLFile() {
        finishSQLStatement();

        out.flush();
        out.close();

        // free RAM
        hwyLevels = null;

        if (LOGGER.isLoggable(Level.INFO)) {
            neededTime = System.currentTimeMillis() - neededTime;

            LOGGER.info("[RGC - write Rg into SQL file] finished loading graph data into DB file in: "
                    + neededTime + " ms");

            // print summary
            LOGGER.info("[RGC - write Rg into SQL file] routing-graph written to '"
                    + outputFile.getAbsolutePath() + "'");
            LOGGER.info("[RGC - write Rg into SQL file] amountOfVerticesWritten = " + amountOfVerticesWritten);
            LOGGER.info("[RGC - write Rg into SQL file] amountOfEdgesWritten = " + amountOfEdgesWritten);
            LOGGER.info("[RGC - write Rg into SQL file] amountOfTRWritten = " + amountOfTRWritten);
        }
    }

    private void writeCreateTables() throws IOException {
        BufferedReader reader = null;
        try {
            final InputStream iStream = SqlWriter.class.getResourceAsStream(RESOURCE_NAME_CREATE_TABLES_SQL);
            reader = new BufferedReader(new InputStreamReader(iStream, "UTF-8"));

            String line;
            while ((line = reader.readLine()) != null) {
                out.println(line);
            }

        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    private void writeHighwayLevels() {
        for (final Integer key : hwyLevels.keys()) {
            out.append(key.toString()).append("\t")
                    .println(toPgString(hwyLevels.get(key)));
        }
    }

    /**
     * @param sourceId     id of source vertex
     * @param targetId     id of target vertex
     * @param weight       weight of the edge
     * @param wayId        way id
     * @param name         name of the street
     * @param ref          ref of street (name higher in the hierarchy)
     * @param destination  destination of a street (normally only on motorways)
     * @param lengthMeters length of an edge
     * @param undirected   oneway?
     * @param urban        urban street?
     * @param roundabout   is roundabout?
     * @param highwayLevel highwaylevel, the type of the street
     * @param longitudes   longitudes of waypoints
     * @param latitudes    latitudes of waypoints
     */
    private void writeEdge(final int sourceId, final int targetId, final int weight, final long wayId,
                           final String name, final String ref, final String destination,
                           final double lengthMeters, final boolean undirected, final boolean urban,
                           final boolean roundabout, final int highwayLevel, final double[] longitudes,
                           final double[] latitudes) {
        final int id = amountOfEdgesWritten;
        final StringBuilder builder = new StringBuilder()
                .append(id).append("\t")
                .append(sourceId).append("\t")
                .append(targetId).append("\t")
                .append(weight).append("\t")
                .append(wayId).append("\t")
                .append(toPgString(name)).append("\t")
                .append(toPgString(ref)).append("\t")
                .append(toPgString(destination)).append("\t")
                .append(lengthMeters).append("\t")
                .append(toPgString(undirected)).append("\t")
                .append(toPgString(urban)).append("\t")
                .append(toPgString(roundabout)).append("\t")
                .append(highwayLevel).append("\t")
                .append(toPgString(longitudes)).append("\t")
                .append(toPgString(latitudes));

        out.println(builder.toString());
        amountOfEdgesWritten++;
    }

    private void writeVertex(final int vertexId, final long nodeId, final double longitude, final double latitude) {
        final StringBuilder builder = new StringBuilder()
                .append(vertexId).append("\t")
                .append(nodeId).append("\t")
                .append(longitude).append("\t")
                .append(latitude);

        out.println(builder.toString());
        amountOfVerticesWritten++;
    }

    private void writeTurnRestrictions(final int id, final long osm_id, final int via, final int from, final int to) {
        final StringBuilder builder = new StringBuilder()
                .append(id).append("\t")
                .append(osm_id).append("\t")
                .append(via).append("\t")
                .append(from).append("\t")
                .append(to);

        out.println(builder.toString());
        amountOfTRWritten++;
    }

    private String toPgString(final double[] array) {
        if (array == null) {
            return "\\null";
        }

        final StringBuilder builder = new StringBuilder();
        builder.append("{");
        for (double entry : array) {
            builder.append(entry).append(",");
        }
        builder.setLength(builder.length() - 1);
        builder.append("}");

        return builder.toString();
    }

    private String toPgString(final boolean bool) {
        return bool ? "t" : "f";
    }

    private String toPgString(final String str) {
        if (str == null) {
            return "";
        }

        // takes a bit longer to create the sql file but solves some issues with street names
        return str.replaceAll("[\r\n\t\\\\]", "").trim();
    }

    /**
     * status of writing the sql file
     *
     * @author Robert Fels
     */
    private enum State {
        /**
         * SQL file is empty.
         */
        NOTHING_INITIALIZED,
        /**
         * Table definitions was done.
         */
        SCHEMA_INITIALIZED,
        /**
         * All COPY statements related to vertices have been added.
         */
        VERTICES_INITIALIZED,
        /**
         * All COPY statements related to edges have been added.
         */
        EDGES_INITIALIZED,
        /**
         * All COPY statements related to turn restrictions have been added.
         */
        TURN_RESTRICTIONS_INITIALIZED
    }

}
