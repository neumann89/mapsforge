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
package org.mapsforge.routing.ch.preprocessing.graph;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mapsforge.routing.preprocessing.data.DatabaseUtils;

/**
 * Reads the Contraction Hierarchies graph related data from a database and provides access to them.
 * 
 * @author Patrick Jungermann
 * @version $Id: DatabaseReader.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
 */
class DatabaseReader {

	/**
	 * Class-level logger.
	 */
	private static final Logger LOGGER = Logger.getLogger(DatabaseReader.class.getName());

	/**
	 * SQL statement for counting all OSM street types.
	 */
	private static final String SQL_COUNT_OSM_STREET_TYPES = "SELECT COUNT(*) as \"count\" FROM rg_hwy_lvl;";
	/**
	 * SQL statement for retrieving all OSM street types.
	 */
	private static final String SQL_SELECT_OSM_STREET_TYPES = "SELECT id, \"name\" FROM rg_hwy_lvl;";
	/**
	 * SQL statement for counting all Contraction Hierarchies graph's vertices.
	 */
	private static final String SQL_COUNT_VERTICES = "SELECT COUNT(*) as \"count\" FROM ch_vertex;";
	/**
	 * SQL statement for counting all Contraction Hierarchies graph's edges.
	 */
	private static final String SQL_COUNT_EDGES = "SELECT COUNT(*) as \"count\" FROM ch_edge;";
	/**
	 * SQL statement for counting all Contraction Hierarchies graph's shortcut edges.
	 */
	private static final String SQL_COUNT_SHORTCUTS = "SELECT COUNT(*) as \"count\" FROM ch_edge WHERE original_edge_id IS NULL;";
	/**
	 * SQL statement for counting all original edges.
	 */
	private static final String SQL_COUNT_ORIGINAL_EDGES = "SELECT COUNT(*) as \"count\" FROM rg_edge;";
	/**
	 * SQL statement for retrieving all Contraction Hierarchies graph's vertices.
	 */
	private static final String SQL_SELECT_VERTICES = "SELECT chv.id, chv.level, rgv.lon, rgv.lat FROM ch_vertex chv LEFT JOIN rg_vertex rgv ON chv.id = rgv.id;";
	/**
	 * SQL statement for retrieving all Contraction Hierarchies graph's edges.
	 */
	private static final String SQL_SELECT_EDGES = "select id, source_id, target_id, weight, undirected, original_edge_id, bypassed_edge_id1, bypassed_edge_id2 from ch_edge;";
	/**
	 * SQL statement for retrieving all original edges.
	 */
	private static final String SQL_SELECT_ORIGINAL_EDGES = "SELECT id, \"name\", ref, hwy_lvl AS \"type\", roundabout, longitudes, latitudes FROM rg_edge;";

	/**
	 * The fetch size used for prepared statement of iterators.
	 */
	private static final int FETCH_SIZE = 1000;

	/**
	 * The database connection, from which the data can be requested.
	 */
	private final Connection connection;
	/**
	 * The number of all vertices.
	 */
	private final int numVertices;
	/**
	 * The number of all edges.
	 */
	private final int numEdges;
	/**
	 * The number of shortcuts.
	 */
	private final int numShortcuts;
	/**
	 * The number of all original edges.
	 */
	private final int numOriginalEdges;
	/**
	 * All OSM street types.
	 */
	private final String[] osmStreetTypes;

	/**
	 * Creates a database reader for the data related to a Contraction Hierarchies graph.
	 * 
	 * @param connection
	 *            The database connection, from which the data has to be read.
	 * @throws SQLException
	 *             if there was a problem with reading the data.
	 */
	public DatabaseReader(final Connection connection) throws SQLException {
		this.connection = connection;
		connection.setAutoCommit(false);

		ResultSet resultSet = connection.createStatement().executeQuery(SQL_COUNT_OSM_STREET_TYPES);
		resultSet.next();
		osmStreetTypes = new String[resultSet.getInt("count")];

		resultSet = connection.createStatement().executeQuery(SQL_SELECT_OSM_STREET_TYPES);
		while (!resultSet.isLast() && !resultSet.isAfterLast()) {
			resultSet.next();
			osmStreetTypes[resultSet.getInt("id")] = resultSet.getString("name");
		}

		resultSet = connection.createStatement().executeQuery(SQL_COUNT_VERTICES);
		resultSet.next();
		numVertices = resultSet.getInt("count");

		resultSet = connection.createStatement().executeQuery(SQL_COUNT_ORIGINAL_EDGES);
		resultSet.next();
		numOriginalEdges = resultSet.getInt("count");

		resultSet = connection.createStatement().executeQuery(SQL_COUNT_EDGES);
		resultSet.next();
		numEdges = resultSet.getInt("count");

		resultSet = connection.createStatement().executeQuery(SQL_COUNT_SHORTCUTS);
		resultSet.next();
		numShortcuts = resultSet.getInt("count");
	}

	/**
	 * Returns the number of vertices.
	 * 
	 * @return The number of vertices.
	 */
	public int getNumVertices() {
		return numVertices;
	}

	/**
	 * Returns the number of edges.
	 * 
	 * @return The number of edges.
	 */
	public int getNumEdges() {
		return numEdges;
	}

	/**
	 * Returns the number of shortcut edges.
	 * 
	 * @return The number of shortcut edges.
	 */
	public int getNumShortcuts() {
		return numShortcuts;
	}

	/**
	 * Returns the number of original edges.
	 * 
	 * @return The number of original edges.
	 */
	public int getNumOriginalEdges() {
		return numOriginalEdges;
	}

	/**
	 * Returns all OSM street names.
	 * 
	 * @return All OSM street names.
	 */
	public String[] getOsmStreetTypes() {
		return osmStreetTypes.clone();
	}

	/**
	 * Returns all vertices, accessible via the provided iterator.
	 * 
	 * @return All vertices, accessible via the provided iterator.
	 * @throws SQLException
	 *             if there any problem with the reading of the related data.
	 */
	public Iterator<Vertex> getVertices() throws SQLException {
		final PreparedStatement preparedStatement = streamingPreparedStatement(
				connection, SQL_SELECT_VERTICES, FETCH_SIZE);
		final ResultSet resultSet = preparedStatement.executeQuery();

		return new Iterator<Vertex>() {

			@Override
			public boolean hasNext() {
				try {
					return !(resultSet.isLast() || resultSet.isAfterLast());

				} catch (SQLException e) {
					LOGGER.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage(), e);

					return false;
				}
			}

			@Override
			public Vertex next() {
				try {
					if (resultSet.next()) {
						return new Vertex(resultSet.getInt("id"), resultSet.getDouble("lon"),
								resultSet.getDouble("lat"), resultSet.getInt("level"));
					}

				} catch (SQLException e) {
					LOGGER.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage(), e);
				}

				return null;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException(
						"remove() is not available for this read-only iterator.");
			}
		};
	}

	/**
	 * Returns all edges, accessible via the provided iterator.
	 * 
	 * @return All edges, accessible via the provided iterator.
	 * @throws SQLException
	 *             if there any problem with the reading of the related data.
	 */
	public Iterator<Edge> getEdges() throws SQLException {
		final PreparedStatement preparedStatement = streamingPreparedStatement(
				connection, SQL_SELECT_EDGES, FETCH_SIZE);
		final ResultSet resultSet = preparedStatement.executeQuery();

		return new Iterator<Edge>() {

			@Override
			public boolean hasNext() {
				try {
					return !(resultSet.isLast() || resultSet.isAfterLast());

				} catch (SQLException e) {
					LOGGER.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage(), e);

					return false;
				}
			}

			@Override
			public Edge next() {
				try {
					if (resultSet.next()) {
						// e.g., original edge might be null, if it is a shortcut
						// getInt() will return 0 for NULL values -> use wasNull() to decide afterwards
						int originalEdgeId = resultSet.getInt("original_edge_id");
						if (resultSet.wasNull()) {
							originalEdgeId = -1;
						}
						int bypassedEdgeId1 = resultSet.getInt("bypassed_edge_id1");
						if (resultSet.wasNull()) {
							bypassedEdgeId1 = -1;
						}
						int bypassedEdgeId2 = resultSet.getInt("bypassed_edge_id2");
						if (resultSet.wasNull()) {
							bypassedEdgeId2 = -1;
						}

						return new Edge(resultSet.getInt("id"), resultSet.getInt("source_id"),
								resultSet.getInt("target_id"), resultSet.getInt("weight"),
								resultSet.getBoolean("undirected"),
								originalEdgeId, bypassedEdgeId1, bypassedEdgeId2);
					}

				} catch (SQLException e) {
					LOGGER.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage(), e);
				}

				return null;
			}

			/*
			 * (non-JavaDoc)
			 * 
			 * @see java.util.Iterator#remove()
			 */
			@Override
			public void remove() {
				throw new UnsupportedOperationException(
						"remove() is not available for this read-only iterator.");
			}
		};
	}

	/**
	 * Returns all original edges, accessible via the provided iterator.
	 * 
	 * @return All original edges, accessible via the provided iterator.
	 * @throws SQLException
	 *             if there any problem with the reading of the related data.
	 */
	public Iterator<OriginalEdge> getOriginalEdges() throws SQLException {
		final PreparedStatement preparedStatement = streamingPreparedStatement(
				connection, SQL_SELECT_ORIGINAL_EDGES, FETCH_SIZE);
		final ResultSet resultSet = preparedStatement.executeQuery();

		return new Iterator<OriginalEdge>() {

			@Override
			public boolean hasNext() {
				try {
					return !(resultSet.isLast() || resultSet.isAfterLast());

				} catch (SQLException e) {
					LOGGER.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage(), e);

					return false;
				}
			}

			@Override
			public OriginalEdge next() {
				try {
					if (resultSet.next()) {
						double[] longitudes = DatabaseUtils.toDoubleArray(
								resultSet.getArray("longitudes"));
						double[] latitudes = DatabaseUtils.toDoubleArray(
								resultSet.getArray("latitudes"));

						return new OriginalEdge(resultSet.getInt("id"), resultSet.getString("name"),
								resultSet.getString("ref"), resultSet.getInt("type"),
								resultSet.getBoolean("roundabout"), longitudes, latitudes);
					}

				} catch (SQLException e) {
					LOGGER.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage(), e);
				}

				return null;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException(
						"remove() is not available for this read-only iterator.");
			}
		};
	}

	/**
	 * Creates a {@link PreparedStatement}, which can be used as stream (e.g., within iterators).
	 * 
	 * @param connection
	 *            The connection, which has to be used by the statement.
	 * @param sql
	 *            The SQL, which has to be executed.
	 * @param fetchSize
	 *            The fetch size, used by the statement.
	 * @return A {@link PreparedStatement}, which can be used as stream (e.g., within iterators).
	 * @throws SQLException
	 *             if there was any problem with the creation of this statement.
	 */
	private static PreparedStatement streamingPreparedStatement(
			final Connection connection, final String sql, final int fetchSize) throws SQLException {
		final PreparedStatement preparedStatement = connection.prepareStatement(sql,
				ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY);
		preparedStatement.setFetchSize(fetchSize);

		return preparedStatement;
	}

	/**
	 * Data bean for a Contraction Hierarchies graph's edge.
	 */
	static class Vertex {

		/**
		 * Its identifier.
		 */
		public final int id;
		/**
		 * Its longitude value.
		 */
		public final double longitude;
		/**
		 * Its latitude value.
		 */
		public final double latitude;
		/**
		 * Its hierarchy level.
		 */
		public final int level;

		/**
		 * Constructs a vertex instance with the given attributes.
		 * 
		 * @param id
		 *            Its identifier.
		 * @param longitude
		 *            Its longitude value.
		 * @param latitude
		 *            Its latitude value.
		 * @param level
		 *            Its hierarchy level.
		 */
		public Vertex(final int id, final double longitude, final double latitude, final int level) {
			this.id = id;
			this.longitude = longitude;
			this.latitude = latitude;
			this.level = level;
		}
	}

	/**
	 * Data bean for a Contraction Hierarchies graph's edge.
	 */
	static class Edge {

		/**
		 * Its identifier.
		 */
		public final int id;
		/**
		 * Its source vertex' identifier.
		 */
		public final int sourceId;
		/**
		 * Its target vertex' identifier.
		 */
		public final int targetId;
		/**
		 * Its weight.
		 */
		public final int weight;
		/**
		 * Whether this edge is undirected or not.
		 */
		public final boolean undirected;
		/**
		 * Its related original edge's identifier.
		 */
		public final int originalEdgeId;
		/**
		 * The first bypassed edge, if it's a shortcut.
		 */
		public final int bypassedEdgeId1;
		/**
		 * The second bypassed edge, if it's a shortcut.
		 */
		public final int bypassedEdgeId2;

		/**
		 * Constructs an edge instance with the given attributes.
		 * 
		 * @param id
		 *            Its identifier.
		 * @param sourceId
		 *            Its source vertex' identifier.
		 * @param targetId
		 *            Its target vertex' identifier.
		 * @param weight
		 *            Its weight.
		 * @param undirected
		 *            Whether it's an undirected edge or not.
		 * @param originalEdgeId
		 *            Its related original edge's identifier. (Non-shortcuts only.)
		 * @param bypassedEdgeId1
		 *            The identifier of the first of its bypassed edges. (Shortcuts only.)
		 * @param bypassedEdgeId2
		 *            The identifier of the second of its bypassed edges. (Shortcuts only.)
		 */
		public Edge(final int id, final int sourceId, final int targetId, final int weight,
				final boolean undirected,
				final int originalEdgeId, final int bypassedEdgeId1, final int bypassedEdgeId2) {
			this.id = id;
			this.sourceId = sourceId;
			this.targetId = targetId;
			this.weight = weight;
			this.undirected = undirected;
			this.originalEdgeId = originalEdgeId;
			this.bypassedEdgeId1 = bypassedEdgeId1;
			this.bypassedEdgeId2 = bypassedEdgeId2;
		}

		/**
		 * Returns {@code true}, if it's a shortcut, otherwise {@code false}.
		 * 
		 * @return {@code true}, if it's a shortcut, otherwise {@code false}.
		 */
		public boolean isShortcut() {
			return originalEdgeId >= 0;
		}
	}

	/**
	 * Data bean for an original edge (edge of the routing graph, prior to the preprocessing), which can
	 * be referenced by an edge of the Contraction Hierarchies graph.
	 */
	static class OriginalEdge {

		/**
		 * Its identifier.
		 */
		public final int id;
		/**
		 * Its name.
		 */
		public final String name;
		/**
		 * Its reference.
		 */
		public final String ref;
		/**
		 * Its OSM street type (only a reference!).
		 */
		public final int osmStreetType;
		/**
		 * Whether its a roundabout or not.
		 */
		public final boolean roundabout;
		/**
		 * Its waypoints' longitude values.
		 */
		public final double[] longitudes;
		/**
		 * Its waypoints' latitude values.
		 */
		public final double[] latitudes;

		/**
		 * Constructs an original edge instance with the given attributes.
		 * 
		 * @param id
		 *            Its identifier.
		 * @param name
		 *            Its name.
		 * @param ref
		 *            Its reference.
		 * @param osmStreetType
		 *            Its OSM street type.
		 * @param roundabout
		 *            Whether its a roundabout or not.
		 * @param longitudes
		 *            Its waypoints' longitude values.
		 * @param latitudes
		 *            Its waypoints' latitude values.
		 */
		public OriginalEdge(final int id, final String name, final String ref, final int osmStreetType,
				final boolean roundabout, final double[] longitudes, final double[] latitudes) {
			this.id = id;
			this.name = name;
			this.ref = ref;
			this.osmStreetType = osmStreetType;
			this.roundabout = roundabout;
			this.longitudes = longitudes;
			this.latitudes = latitudes;
		}
	}
}
