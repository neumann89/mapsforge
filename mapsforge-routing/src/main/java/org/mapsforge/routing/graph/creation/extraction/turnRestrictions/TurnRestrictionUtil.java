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
package org.mapsforge.routing.graph.creation.extraction.turnRestrictions;

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.mapsforge.routing.graph.creation.extraction.CompleteEdge;
import org.mapsforge.routing.graph.creation.statistics.ProtobufCreator;
import org.mapsforge.routing.preprocessing.sql.DBConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * This class includes the turn restrictions into the routing graph. This is done via stored procedures
 * in the DB
 * 
 * @author Robert Fels
 * 
 */
public class TurnRestrictionUtil {
	// private static final String DEFAULT_CONFIG_FILE = "config.properties";
	private static final String SQL_INCLUDE_NODES = "SELECT node_split();";
	private static final String SQL_INCLUDE_EDGES = "SELECT edge_split();";
	private static final String SQL_COUNT_VERTICES = "SELECT COUNT(*) AS count FROM rg_vertex;";
	private static final String SQL_COUNT_EDGES = "SELECT COUNT(*) AS count FROM rg_edge;";
	private static final String SQL_COUNT_TR = "SELECT COUNT(*) AS count FROM turn_restrictions;";

	/**
	 * @param args
	 *            first argument is the location of the pbf file which shuld be updated
	 * @throws SQLException
	 *             expt
	 * @throws NumberFormatException
	 *             expt
	 */
	public static void main(String[] args) throws NumberFormatException, SQLException {

		// initialize configuration
		// Properties config = new Properties();
		// config.load(TurnRestrictionUtil.class
		// .getResourceAsStream(DEFAULT_CONFIG_FILE));

		// get arguments
		if (args.length != 7) {
			System.err
					.println("[ERROR]TurnRestrictionUtil usage: <db.host> <db.port> <db.name> <db.user> <db.pass> <true|false>(saveToPBF) <PBFLocation>");
			System.exit(1);
		}
		String hostDB = args[0];
		String portDB = args[1];
		String nameDB = args[2];
		String userDB = args[3];
		String pwdDB = args[4];

		boolean saveStatsToPbf = Boolean.valueOf(args[5]);
		String pbfFile = args[6];

		// initialize database connection
		Connection conn1 = DBConnection.getConnectionToPostgreSQL(hostDB,
                Integer.parseInt(portDB),
                nameDB,
                userDB,
                pwdDB);

		// include turn restrictions into routing graph
		ResultSet rs;
		rs = conn1.createStatement().executeQuery("Select count(*) as count from turn_restrictions;");
		rs.next();
		int tr_counter = rs.getInt("count");
		if (tr_counter != 0) {
			rs.close();
			System.out.println(tr_counter);
			long t = System.currentTimeMillis();
			conn1.createStatement().executeQuery(SQL_INCLUDE_NODES);
			conn1.createStatement().executeQuery(SQL_INCLUDE_EDGES);
			t = System.currentTimeMillis() - t;
			System.out.println("Tunr restrictions added in" + t + " milliseconds");
		} else {
			System.out.println("TR_NOT_INCLUDED");

		}
		rs.close();
		rs = conn1.createStatement().executeQuery(SQL_COUNT_VERTICES);
		rs.next();
		System.out.println("|V|= " + rs.getInt("count"));
		rs.close();
		rs = conn1.createStatement().executeQuery(SQL_COUNT_EDGES);
		rs.next();
		System.out.println("|E|= " + rs.getInt("count"));
		rs.close();
		rs = conn1.createStatement().executeQuery(SQL_COUNT_TR);
		rs.next();
		System.out.println("|TR|= " + rs.getInt("count"));
		rs.close();

		// ---- update pbf-file----
		if (saveStatsToPbf) {
			// load pbf
			System.out.println("Load file:" + args[0]);
			TIntObjectHashMap<CompleteEdge> completeEdges;
			TObjectIntHashMap<String> indexEdges;
			completeEdges = new TIntObjectHashMap<CompleteEdge>();
			indexEdges = new TObjectIntHashMap<String>();

			ProtobufCreator.loadFromFile(pbfFile, completeEdges, indexEdges);

			// 1. add new vertices
			// not needed
			// 2. update from edge
			rs = conn1.createStatement().executeQuery("SELECT updated_edge,id  FROM dummy_node;");
			int updatecount = 0;
			while (rs.next()) {
				int edgeId = rs.getInt("updated_edge");
				int newVertexId = rs.getInt("id");

				// update edge
				CompleteEdge cEdge = completeEdges.get(edgeId);
				String oldString = String.valueOf(cEdge.getSourceId())
						+ String.valueOf(cEdge.getTargetId());
				cEdge.setTargetId(newVertexId);
				completeEdges.put(edgeId, cEdge);

				String newString = String.valueOf(cEdge.getSourceId())
						+ String.valueOf(cEdge.getTargetId());
				// update index
				indexEdges.remove(oldString);
				indexEdges.put(newString, edgeId);
				updatecount++;
			}
			System.out.println("In PBF: " + updatecount + " edges updated");
			rs.close();

			// 3. add new edges
			rs = conn1.createStatement().executeQuery("SELECT *  FROM dummy_edge;");
			updatecount = 0;
			while (rs.next()) {
				int newEdgeId = rs.getInt("id");
				int origId = rs.getInt("original_id");
				int sourceId = rs.getInt("source_id");

				// copy edge
				CompleteEdge origEdge = completeEdges.get(origId);
				CompleteEdge newEdge = origEdge;
				// change src
				newEdge.setSourceId(sourceId);
				// add
				completeEdges.put(newEdgeId, newEdge);

				// update edge index
				String newString = String.valueOf(newEdge.getSourceId())
						+ String.valueOf(newEdge.getTargetId());

				indexEdges.put(newString, newEdgeId);
				updatecount++;
			}
			System.out.println("In PBF: " + updatecount + " edges added");
			rs.close();

			ProtobufCreator.saveToFile(args[0], completeEdges, indexEdges);

			conn1.close();

			completeEdges = null;
			indexEdges = null;
		}

	}
}
