package org.mapsforge.routing.preprocessing.sql;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link DatabaseType}.
 *
 * @author Patrick Jungermann
 */
public class DatabaseTypeUnitTests {

    @Test(expected = IllegalArgumentException.class)
    public void url_hostIsNull_throwException() {
        DatabaseType.POSTGRE_SQL.url(null, 1234, "name");
    }

    @Test(expected = IllegalArgumentException.class)
    public void url_portIsZero_throwException() {
        DatabaseType.POSTGRE_SQL.url("host", 0, "name");
    }

    @Test(expected = IllegalArgumentException.class)
    public void url_portIsNegative_throwException() {
        DatabaseType.POSTGRE_SQL.url("host", -1234, "name");
    }

    @Test(expected = IllegalArgumentException.class)
    public void url_databaseNameIsNotNull_throwException() {
        DatabaseType.POSTGRE_SQL.url("host", 1234, null);
    }

    @Test
    public void url_validParameter_returnUrl() {
        final String url = DatabaseType.POSTGRE_SQL.url("host", 1234, "name");

        assertEquals("jdbc:postgresql://host:1234/name", url);
    }

}
