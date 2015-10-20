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
package org.mapsforge.routing.hh.preprocessing;


import org.mapsforge.routing.graph.RgDAO;
import org.mapsforge.routing.hh.preprocessing.hierarchyComputation.HHComputation;
import org.mapsforge.routing.hh.preprocessing.mobile.HHBinaryFileWriter;
import org.mapsforge.routing.hh.server.HHRouterServerside;
import org.mapsforge.routing.preprocessing.sql.DBConnection;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Properties;

/**
 * this class implements a command line interface to highway hierarchies preprocessing.
 */
public class CommandLineUtil {

    // TODO: add DB-arguments to the main method

	private static final String DEFAULT_CONFIG_FILE = "config.properties";
	private static final String DEFAULT_FORMAT = "mobile";
	private static final int OUTPUT_BUFFER_SIZE = 32 * 1024 * 1024;

	private static final DecimalFormat df = new DecimalFormat("###,###,###,###");

	/**
	 * entry point.
	 *
	 * @param args
	 *            see usage()
	 */
	public static void main(String[] args) {
		try {
			run(args);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println(usage());
			return;
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println(usage());
			return;
		} catch (NumberFormatException e) {
			e.printStackTrace();
			System.out.println(usage());
			return;
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println(usage());
			return;
		}
	}

	private static void run(String[] args) throws FileNotFoundException, IOException,
			NumberFormatException, SQLException {
		CommandLineParameters p = parseArguments(args);
		if (p == null) {
			System.out.println(usage());
			return;
		}
		String outputFile = p.getOutputFile();

		// initialize configuration
		Properties config = new Properties();
		if (p.getConfigFile() != null) {
			config.load(new FileInputStream(p.getConfigFile()));
		} else {
			config.load(CommandLineUtil.class
					.getResourceAsStream(DEFAULT_CONFIG_FILE));
		}

		// initialize binary file format
		String format;
		if (p.getFormat() != null) {
			format = p.getFormat();
		} else {
			format = DEFAULT_FORMAT;
		}

		// initialize database connection
        Connection conn1 = DBConnection.getConnectionToPostgreSQL(
                p.getHost(),
                Integer.parseInt(p.getPort()),
                p.getDbName(),
                p.getUser(),
                p.getPwd());

        Connection conn2 = DBConnection.getConnectionToPostgreSQL(
                p.getHost(),
                Integer.parseInt(p.getPort()),
                p.getDbName(),
                p.getUser(),
                p.getPwd());

		if (!p.getSkipHierarchyComputation()) {
			// compute hierarchy
			HHComputation.doPreprocessing(
					new RgDAO(conn1),
					Integer.parseInt(config.getProperty(format + ".hierarchie.h")),
					Integer.parseInt(config
							.getProperty(format + ".hierarchie.hopLimit")),
					Double.parseDouble(config.getProperty(format + ".hierarchie.c")),
					Integer.parseInt(config.getProperty(format
							+ ".hierarchie.vertexThreshold")),
					Boolean.parseBoolean(config.getProperty(format
							+ ".hierarchie.downgradeEdges")),
					Integer.parseInt(config.getProperty("numThreads")),
					conn2);
		}

		// write output
		if (format.equals("mobile")) {
			String clusteringAlgorithm = config.getProperty("clusteringAlgorithm");
			int clusterSizeThreshold;
			if (clusteringAlgorithm
					.equals(HHBinaryFileWriter.CLUSTERING_ALGORITHM_QUAD_TREE)) {
				clusterSizeThreshold = Integer.parseInt(config
						.getProperty("quad-tree.clusterSizeThreshold"));
			} else {
				clusterSizeThreshold = Integer.parseInt(config
						.getProperty("k-center.clusterSizeThreshold"));
			}
			HHBinaryFileWriter
					.writeBinaryFile(
							conn1,
							clusteringAlgorithm,
							clusterSizeThreshold,
							Integer.parseInt(config
									.getProperty("k-center.oversamplingFactor")),
							new File(outputFile),
							Integer.parseInt(config
									.getProperty("addressLookupTable.maxGroupSize")),
							Integer.parseInt(config.getProperty("rtree.blockSize")),
							Boolean.parseBoolean(config.getProperty("includeHopIndices"))
					);
		} else {
			// format = 'server'
			HHRouterServerside router = HHRouterServerside.getFromDb(conn1);
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
					new FileOutputStream(outputFile), OUTPUT_BUFFER_SIZE));
			router.serialize(out);
			out.flush();
			out.close();
			System.out
					.println(df.format(out.size()) + " bytes written to '" + outputFile
							+ "'");
		}

		conn1.close();
		conn2.close();
	}

	private static CommandLineParameters parseArguments(String[] args) {
		if (args.length == 0) {
			return null;
		}
		CommandLineParameters p = new CommandLineParameters();
		p.setOutputFile(args[0]);
		for (int i = 1; i < args.length; i++) {
			if (!p.setParameter(args[i])) {
				return null;
			}
		}
		return p;
	}

	private static String usage() {
		StringBuilder sb = new StringBuilder();
		sb.append("routing-preprocessing OUTPUT-FILE [OPTIONS]");
		sb.append("\n");
		sb.append("  -f, --format=[FORMAT]");
		sb.append("\n");
		sb.append("  		      mobile (default) : binary for onboard routing");
		sb.append("\n");
		sb.append("  		      server : binary for server-sided routing");
		sb.append("\n");
		sb.append("  -c, --config-file=[CONFIG_FILE]");
		sb.append("\n");
		sb.append("  -wf, --weight-function=[AVERAGE_SPEED_FILE]");
		sb.append("\n");
		sb.append("  -s, --skip-hierarchy-computation");
		sb.append("\n");
		sb.append("      only write the contents from the database to a binary file");
		sb.append("\n");

		return sb.toString();
	}

	static class CommandLineParameters {
		private String outputFile;
		private String format;
		private String configFile;
		private String averageSpeedFile;
		private boolean skipHierarchyComputation = false;

        private String host;
        private String port;
        private String dbName;
        private String user;
        private String pwd;

		public void setOutputFile(String outputFile) {
			this.outputFile = outputFile;
		}

		public boolean setParameter(String arg) {
			String[] tmp = arg.split("=");
			if (tmp.length == 2) {
				return setParameter(tmp[0], tmp[1]);
			}
			return setParameter(arg, null);
		}

		private boolean setParameter(String name, String value) {
			if (name.equals("-f") || name.equals("--format")) {
				if (value == null) {
					return false;
				}
				if (format == null) {
					if (value.equals("mobile") || value.equals("server")) {
						format = value;
						return true;
					}
				}
				return false;
			} else if (name.equals("-c") || name.equals("--config.file")) {
				if (value == null) {
					return false;
				}
				if (configFile == null) {
					configFile = value;
					return true;
				}
				return false;
			} else if (name.equals("-wf") || name.equals("--weight-function")) {
				if (value == null) {
					return false;
				}
				if (averageSpeedFile == null) {
					averageSpeedFile = value;
					return true;
				}
				return false;
			} else if (name.equals("-s") || name.equals("--skip-hierarchy-computation")) {
				if (value != null) {
					return false;
				}
				skipHierarchyComputation = true;
				return true;
            } else if (name.equals("-host")) {
                host = value;
                return true;
            } else if (name.equals("-port")) {
                port = value;
                return true;
            } else if (name.equals("-dbName")) {
                dbName = value;
                return true;
            } else if (name.equals("-user")) {
                user = value;
                return true;
            } else if (name.equals("-pwd")) {
                pwd = value;
                return true;
            }

            return false;
        }

        public String getHost() {
            return host;
        }

        public String getPort() {
            return port;
        }

        public String getDbName() {
            return dbName;
        }

        public String getUser() {
            return user;
        }

        public String getPwd() {
            return pwd;
        }

		public String getOutputFile() {
			return outputFile;
		}

		public String getFormat() {
			return format;
		}

		public String getConfigFile() {
			return configFile;
		}

		public String getAverageSpeedFile() {
			return averageSpeedFile;
		}

		public boolean getSkipHierarchyComputation() {
			return skipHierarchyComputation;
		}

	}
}
