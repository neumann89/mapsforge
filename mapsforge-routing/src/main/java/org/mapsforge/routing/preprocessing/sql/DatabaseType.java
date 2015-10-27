package org.mapsforge.routing.preprocessing.sql;

/**
 * The different databases.
 *
 * @author Patrick Jungermann
 */
public enum DatabaseType {
    POSTGRE_SQL("jdbc:postgresql://", "org.postgresql.Driver");

    public final String protocol;
    public final String className;

    DatabaseType(final String protocol, final String className) {
        this.protocol = protocol;
        this.className = className;
    }

    public String url(final String host, final int port, final String databaseName) {
        if (host == null) {
            throw new IllegalArgumentException("\"host\" is invalid.");
        }
        if (port <= 0) {
            throw new IllegalArgumentException("\"port\" is invalid.");
        }
        if (databaseName == null) {
            throw new IllegalArgumentException("\"databaseName\" is invalid.");
        }

        return protocol + host + ":" + port + "/" + databaseName;
    }

}
