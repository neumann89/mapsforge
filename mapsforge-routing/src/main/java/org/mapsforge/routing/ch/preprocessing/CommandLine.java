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
package org.mapsforge.routing.ch.preprocessing;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mapsforge.routing.ch.preprocessing.io.FileWriter;
import org.mapsforge.routing.preprocessing.data.clustering.ClusteringAlgorithm;
import org.mapsforge.routing.preprocessing.data.clustering.ClusteringSettings;
import org.mapsforge.routing.preprocessing.sql.DBConnection;

/**
 * Provides an easy access from the command line to the generation of Contraction Hierarchies' binary
 * file.
 * 
 * @author Patrick Jungermann
 * @version $Id: CommandLine.java 2092 2012-08-06 00:05:51Z Patrick.Jungermann@googlemail.com $
 */
public class CommandLine { // TODO: some cleanup and refactoring

	/**
	 * Class-level logger.
	 */
	private static final Logger LOGGER = Logger.getLogger(CommandLine.class.getName());

	/**
	 * Provides access to the preprocessing of the Contraction Hierarchies routing and gives the user
	 * some possibilities to configure this process via arguments.
	 * 
	 * @param args
	 *            The user's arguments.
	 */
	public static void main(String[] args) {
		Connection connection = null;
		try {
			final Settings settings = parseArgs(args).toSettings();
			Properties config = settings.config;

			// initialize database connection
			connection = DBConnection.getConnectionToPostgreSQL(
                    config.getProperty("db.host"),
                    Integer.parseInt(config.getProperty("db.port")),
                    config.getProperty("db.name"),
                    config.getProperty("db.user"),
                    config.getProperty("db.pass"));

			if (!Boolean.parseBoolean(config.getProperty("preprocessing.skip"))) {
				final PreprocessorSettings preprocessorSettings = new PreprocessorSettings();
				preprocessorSettings.setNumThreads(
						config.getProperty("preprocessing.numThreads"));

				final Preprocessor preprocessor = new Preprocessor(preprocessorSettings);
				if (!preprocessor.execute(connection).saveGraph(connection)) {
					throw new Exception("There was a problem with saving the graph to the database.");
				}
			}

			switch (settings.format) {
				case MOBILE:
					// clustering settings
					final ClusteringSettings clusteringSettings = new ClusteringSettings();
					clusteringSettings.algorithm = ClusteringAlgorithm.valueOf(
							config.getProperty("clustering.algorithm"));
					clusteringSettings.clusterSizeThreshold = Integer.parseInt(
							config.getProperty(
									"clustering." + clusteringSettings.algorithm.name()
											+ ".cluster_size_threshold"), 10);
					clusteringSettings.oversamplingFactor = Integer.parseInt(
							config.getProperty(
									"clustering." + clusteringSettings.algorithm.name()
											+ ".oversampling_factor", "2"), 10);

					// address lookup table settings
					// TODO: good value for parameter "indexGroupSizeThreshold"?
					final int idxGrpSizeThreshold = Integer.parseInt(
							config.getProperty("r_tree.index_group_size_threshold"), 10);
					// R-tree settings
					final int rTreeBlockSize = Integer.parseInt(
							config.getProperty("r_tree.block_size"), 10);

					// write the binary file
					FileWriter.write(settings.file, connection, clusteringSettings,
							idxGrpSizeThreshold, rTreeBlockSize);
					break;
				case SERVER:
					// TODO: server-side format and serialization
					break;
			}

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getClass() + ": " + e.getMessage(), e);
			printHelp();

		} finally {
			if (connection != null) {
				try {
					connection.close();

				} catch (SQLException e) {
					LOGGER.log(Level.SEVERE, e.getClass() + ": " + e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * Prints out information about how to use it.
	 */
	private static void printHelp() {
		System.out.println(new StringBuilder("usage:\n")
				.append("\t<output file> --format=<mobile|server> [--config=<configuration file>]")
				.toString());

	}

	/**
	 * Parses the command line arguments and returns them as structured object.
	 * 
	 * @param args
	 *            The command line arguments.
	 * @return A structured object, which is representing the arguments.
	 */
	private static CommandLineArguments parseArgs(String[] args) {
		CommandLineArguments cmdArgs = new CommandLineArguments();
		for (String arg : args) {
			if (!arg.startsWith("--")) {
				cmdArgs.file = arg;
			} else if (arg.startsWith("--format=")) {
				cmdArgs.format = arg.substring(9);
			} else if (arg.startsWith("--config=")) {
				cmdArgs.configFile = arg.substring(9);
			}
		}

		return cmdArgs;
	}

	/**
	 * Representation of the user's command line arguments.
	 */
	private static class CommandLineArguments {

		/**
		 * The argument {@code file}.
		 */
		public String file;
		/**
		 * The argument {@code format}.
		 */
		public String format;
		/**
		 * The argument {@code configFile}.
		 */
		public String configFile;

		/**
		 * Converts these command line arguments into a settings object.
		 * 
		 * @return The settings object.
		 * @throws IOException
		 *             if the config file is not readable.
		 */
		public Settings toSettings() throws IOException {
			Properties properties = null;
			if (configFile != null) {
				File config = new File(configFile);
				properties = new Properties();
				properties.load(new FileInputStream(config));
			}

			Format formatEnum;
			try {
				formatEnum = Format.valueOf(format.toUpperCase());
			} catch (IllegalArgumentException e) {
				formatEnum = null;
			}

			return new Settings(file == null ? null : new File(file), formatEnum, properties);
		}
	}

	/**
	 * The settings used for the execution.
	 */
	public static class Settings {
		/**
		 * The default configuration file.
		 */
		private static final String DEFAULT_CONFIG_FILE_NAME = "config.properties";

		/**
		 * The target file for the binary output.
		 */
		public File file;
		/**
		 * The format for the binary output.
		 */
		public Format format;
		/**
		 * The configuration settings.
		 */
		public Properties config;

		/**
		 * Constructor. Creates an instance of this object with the given setting objects.
		 * 
		 * @param file
		 *            The target file for the binary output.
		 * @param format
		 *            The format for the binary file. Defaults to {@link Format#MOBILE}.
		 * @param config
		 *            The general configuration settings. The default configuration will be used, if
		 *            this parameter is {@code null}.
		 * @throws IOException
		 *             if the reading of the default configuration failed.
		 */
		public Settings(File file, Format format, Properties config) throws IOException {
			this.file = file;
			this.format = format != null ? format : Format.MOBILE;
			this.config = config != null ? config : getDefaultConfig();
		}

		/**
		 * Returns the default configuration.
		 * 
		 * @return The default configuration.
		 * @throws IOException
		 *             if the reading of the default configuration failed.
		 */
		public static Properties getDefaultConfig() throws IOException {
			Properties properties = new Properties();
			properties.load(CommandLine.class.getResourceAsStream(DEFAULT_CONFIG_FILE_NAME));

			return properties;
		}
	}

	/**
	 * Possible formats of the binary file.
	 */
	public enum Format {
		/**
		 * Format used for mobile devices.
		 */
		MOBILE,
		/**
		 * Format used for server environments.
		 */
		SERVER
	}
}
