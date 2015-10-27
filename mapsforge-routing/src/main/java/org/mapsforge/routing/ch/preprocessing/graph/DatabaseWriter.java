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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Writer, to store a graph, which was preprocessed by the Contraction Hierarchies algorithm, to a
 * database.
 * 
 * @author Patrick Jungermann
 * @version $Id: DatabaseWriter.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
 */
class DatabaseWriter {

	/**
	 * Name of the SQL file, containing the create statements for creation all relevant database table
	 * to save this graph to this database.
	 */
	private static final String CREATE_TABLES_SQL = "createTables.sql";
	/**
	 * Prepared INSERT statement for vertices.
	 */
	private static final String INSERT_VERTEX = "INSERT INTO ch_vertex (id, level, hierarchy_depth) VALUES (?, ?, ?);";
	/**
	 * Prepared INSERT statement for edges.
	 */
	private static final String INSERT_EDGE = "INSERT INTO ch_edge (id, source_id, target_id, weight, undirected, original_edge_id, bypassed_edge_id1, bypassed_edge_id2, original_edge_count) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);";
	/**
	 * Batch size used at the inserting of the data.
	 */
	private static final int SQL_BATCH_SIZE = 10000;

	/**
	 * Connection to the database.
	 */
	private final Connection connection;
	/**
	 * Number of added batch statements, related to the inserting of vertices.
	 */
	private int numVertexStatements = 0;
	/**
	 * Number of added batch statements, related to the inserting of edges.
	 */
	private int numEdgeStatements = 0;
	/**
	 * Prepared statement for inserting vertices.
	 */
	private final PreparedStatement preparedStatementVertex;
	/**
	 * Prepared statements for inserting edges.
	 */
	private final PreparedStatement preparedStatementEdge;

	/**
	 * Creates a writer, which uses the given connection to interact with the underlying database.
	 * 
	 * @param connection
	 *            The connection to a database.
	 * @throws SQLException
	 *             if there was problem, preparing some statements, related to the database
	 *             interactions.
	 */
	public DatabaseWriter(final Connection connection) throws SQLException {
		this.connection = connection;

		preparedStatementVertex = connection.prepareStatement(INSERT_VERTEX);
		preparedStatementEdge = connection.prepareStatement(INSERT_EDGE);
	}

	/**
	 * Creates all related tables, needed to store the data at.
	 * 
	 * @throws IOException
	 *             if there was a problem related to the reading of the create statement SQL script.
	 * @throws SQLException
	 *             if there was a problem related to the database interactions.
	 */
	public void createTables() throws IOException, SQLException {
		final String createTablesSql = convertStreamToString(DatabaseWriter.class
				.getResourceAsStream(CREATE_TABLES_SQL));

		connection.createStatement().executeUpdate(createTablesSql);
		connection.commit();
	}

	/**
	 * Inserts a vertex to the database.
	 * 
	 * @param id
	 *            The vertex' identifier.
	 * @param level
	 *            The vertex' level.
	 * @param hierarchyDepth
	 *            The vertex' hierarchy depth.
	 * @throws SQLException
	 *             if there was any problem related to the database interactions.
	 */
	public void insertVertex(final int id, final int level, final int hierarchyDepth)
			throws SQLException {
		preparedStatementVertex.setInt(1, id);
		preparedStatementVertex.setInt(2, level);
		preparedStatementVertex.setInt(3, hierarchyDepth);

		preparedStatementVertex.addBatch();

		if (++numVertexStatements % SQL_BATCH_SIZE == 0) {
			preparedStatementVertex.executeBatch();
		}
	}

	/**
	 * Inserts an edge to the database.
	 * 
	 * @param id
	 *            The edge's identifier.
	 * @param sourceId
	 *            The edge's source's identifier.
	 * @param targetId
	 *            The edge's target's identifier.
	 * @param weight
	 *            The edge's weight.
	 * @param undirected
	 *            Whether the edge is undirected or not.
	 * @param originalEdgeId
	 *            The original/source edge's identifier.
	 * @param bypassedEdgeId1
	 *            The first edge's identifier, bypassed by this edge (shortcut).
	 * @param bypassedEdgeId2
	 *            The second edge's identifier, bypassed by this edge (shortcut).
	 * @param originalEdgeCount
	 *            The edge's original edge count (= number of original edges, represented by it).
	 * @throws SQLException
	 *             if there was any problem related to the database interactions.
	 */
	public void insertEdge(final int id, final int sourceId, final int targetId, final int weight,
			final boolean undirected, final int originalEdgeId, final int bypassedEdgeId1,
			final int bypassedEdgeId2, final int originalEdgeCount)
			throws SQLException {
		preparedStatementEdge.setInt(1, id);
		preparedStatementEdge.setInt(2, sourceId);
		preparedStatementEdge.setInt(3, targetId);
		preparedStatementEdge.setInt(4, weight);
		preparedStatementEdge.setBoolean(5, undirected);
		if (originalEdgeId >= 0) {
			preparedStatementEdge.setInt(6, originalEdgeId);

		} else {
			preparedStatementEdge.setNull(6, Types.INTEGER);
		}
		if (bypassedEdgeId1 >= 0) {
			preparedStatementEdge.setInt(7, bypassedEdgeId1);

		} else {
			preparedStatementEdge.setNull(7, Types.INTEGER);
		}
		if (bypassedEdgeId2 >= 0) {
			preparedStatementEdge.setInt(8, bypassedEdgeId2);

		} else {
			preparedStatementEdge.setNull(8, Types.INTEGER);
		}
		preparedStatementEdge.setInt(9, originalEdgeCount);

		preparedStatementEdge.addBatch();

		if (++numEdgeStatements % SQL_BATCH_SIZE == 0) {
			preparedStatementEdge.executeBatch();
		}
	}

	/**
	 * Executes and commits every open batch statements.
	 * 
	 * @throws SQLException
	 *             if there was any problem with the database interaction.
	 */
	public void commit() throws SQLException {
		if (numVertexStatements > 0 && numVertexStatements % SQL_BATCH_SIZE != 0) {
			preparedStatementVertex.executeBatch();
			numVertexStatements = 0;
		}

		if (numEdgeStatements > 0 && numEdgeStatements % SQL_BATCH_SIZE != 0) {
			preparedStatementEdge.executeBatch();
			numEdgeStatements = 0;
		}

		connection.commit();
	}

	/**
	 * Converts an {@link java.io.InputStream} into a {@link String}.
	 * 
	 * @param is
	 *            The {@link java.io.InputStream}, which has to be converted.
	 * @return The {@link String}, created from the data of the {@link java.io.InputStream}.
	 * @throws java.io.IOException
	 *             if there was a problem with reading the data or with the encoding.
	 */
	protected static String convertStreamToString(final InputStream is) throws IOException {
		String str;
		if (is != null) {
			Writer writer = new StringWriter();

			char[] buffer = new char[1024];
			try {
				Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}

			} finally {
				is.close();
			}

			str = writer.toString();

		} else {
			str = "";
		}

		return str;
	}
}
