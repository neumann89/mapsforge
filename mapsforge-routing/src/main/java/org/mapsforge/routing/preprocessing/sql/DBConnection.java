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
package org.mapsforge.routing.preprocessing.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * lookup connections to databases.
 */
public class DBConnection {

    /**
     * Logger, used for this class.
     */
    private static final Logger LOGGER = Logger.getLogger(DBConnection.class.getName());

	/**
	 * Opens a connection to a PostgreSQL database.
	 * 
	 * @param hostName Host's name.
	 * @param port Port.
	 * @param dbName Database's name.
	 * @param username User's name.
	 * @param password The password.
	 * @return null on error, else the connection.
	 * @throws SQLException if an error occurred during establishing the connection.
	 */
	public static Connection getConnectionToPostgreSQL(String hostName, int port, String dbName,
                                                       String username, String password) throws SQLException {
        return getConnection(DatabaseType.POSTGRE_SQL, hostName, port, dbName, username, password);
    }

	/**
     * Opens a connection to a PostgreSQL database.
     *
     * @param type The database's type.
     * @param hostName Host's name.
     * @param port Port.
     * @param dbName Database's name.
     * @param username User's name.
     * @param password The password.
     * @return null on error, else the connection.
     * @throws SQLException if an error occurred during establishing the connection.
	 */
	public static Connection getConnection(DatabaseType type, String hostName, int port, String dbName,
                                           String username, String password) throws SQLException {
		try {
			Class.forName(type.className).newInstance();

		} catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Not able to load the Driver's class: " + e.getMessage(), e);

		} catch (InstantiationException e) {
            LOGGER.log(Level.SEVERE, "Not able to instantiate the Driver's class: " + e.getMessage(), e);

		} catch (IllegalAccessException e) {
            LOGGER.log(Level.SEVERE, "Not able to access the Driver's class: " + e.getMessage(), e);
		}

        final String url = type.url(hostName, port, dbName);
		return DriverManager.getConnection(url, username, password);
	}

}
