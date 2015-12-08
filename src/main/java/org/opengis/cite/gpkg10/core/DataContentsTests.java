package org.opengis.cite.gpkg10.core;

import org.opengis.cite.gpkg10.CommonFixture;
import org.opengis.cite.gpkg10.ErrorMessage;
import org.opengis.cite.gpkg10.ErrorMessageKeys;
import org.opengis.cite.gpkg10.GPKG10;
import org.opengis.cite.gpkg10.util.DatabaseUtility;
import org.testng.annotations.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Defines test methods that apply to descriptive information about the contents
 * of a GeoPackage. The {@code gpkg_contents} table describes the
 * geospatial data contained in the file.
 *
 * <p style="margin-bottom: 0.5em">
 * <strong>Sources</strong>
 * </p>
 * <ul>
 * <li><a href="http://www.geopackage.org/spec/#_contents" target= "_blank">
 * GeoPackage Encoding Standard - Contents</a> (OGC 12-128r12)</li>
 * </ul>
 *
 * @author Luke Lambert
 */
public class DataContentsTests extends CommonFixture
{
    /**
     * The columns of tables in a GeoPackage SHALL only be declared using one
     * of the data types specified in table <a href=
     * "http://www.geopackage.org/spec/#table_column_data_types">GeoPackage
     * Data Types</a>.
     *
     * @see <a href="http://www.geopackage.org/spec/#_requirement-5" target=
     *      "_blank">File Contents - Requirement 5</a>
     *
     * @throws SQLException
     *             If an SQL query causes an error
     */
    @Test(description = "See OGC 12-128r12: Requirement 5")
    public void columnDataTypes() throws SQLException
    {
        try(final Statement statement = this.databaseConnection.createStatement();
            final ResultSet resultSet = statement.executeQuery("SELECT table_name FROM gpkg_contents;"))
        {
            while(resultSet.next())
            {
                final String tableName = resultSet.getString("table_name");

                if(DatabaseUtility.doesTableOrViewExist(this.databaseConnection, tableName))
                {
                    try(final Statement preparedStatement = this.databaseConnection.createStatement();
                        final ResultSet pragmaTableInfo   = preparedStatement.executeQuery(String.format("PRAGMA table_info('%s');", tableName)))
                    {
                        while(pragmaTableInfo.next())
                        {
                            final String dataType = pragmaTableInfo.getString("type");
                            final boolean correctDataType = ALLOWED_SQL_TYPES.contains(dataType) ||
                                                            TEXT_TYPE.matcher(dataType).matches() ||
                                                            BLOB_TYPE.matcher(dataType).matches();

                            assertTrue(correctDataType,
                                       ErrorMessage.format(ErrorMessageKeys.INVALID_DATA_TYPE,
                                                           dataType,
                                                           tableName));
                        }
                    }
                }
            }
        }
    }

    /**
     * The SQLite PRAGMA integrity_check SQL command SHALL return "ok" for a
     * GeoPackage file.
     *
     * @see <a href="http://www.geopackage.org/spec/#_requirement-6" target=
     *      "_blank">File Integrity - Requirement 6</a>
     *
     * @throws SQLException
     *             If an SQL query causes an error
     */
    @Test(description = "See OGC 12-128r12: Requirement 6")
    public void pragmaIntegrityCheck() throws SQLException
    {
        try(final Statement statement = this.databaseConnection.createStatement();
            final ResultSet resultSet = statement.executeQuery("PRAGMA integrity_check;"))
        {
            resultSet.next();

            assertEquals(resultSet.getString("integrity_check").toLowerCase(),
                         GPKG10.PRAGMA_INTEGRITY_CHECK,
                         ErrorMessage.format(ErrorMessageKeys.PRAGMA_INTEGRITY_CHECK_NOT_OK));
        }
    }

    /**
     * The SQLite PRAGMA foreign_key_check SQL with no parameter value SHALL
     * return an empty result set indicating no invalid foreign key values for
     * a GeoPackage file.
     *
     * @see <a href="http://www.geopackage.org/spec/#_requirement-7" target=
     *      "_blank">File Integrity - Requirement 7</a>
     *
     * @throws SQLException
     *             If an SQL query causes an error
     */
    @Test(description = "See OGC 12-128r12: Requirement 7")
    public void foreignKeyCheck() throws SQLException
    {
        try(final Statement statement = this.databaseConnection.createStatement();
            final ResultSet resultSet = statement.executeQuery("PRAGMA foreign_key_check;"))
        {
            assertTrue(!resultSet.next(),
                       ErrorMessage.format(ErrorMessageKeys.INVALID_FOREIGN_KEY));
        }
    }

    /**
     * A GeoPackage SQLite Configuration SHALL provide SQL access to
     * GeoPackage contents via software APIs.
     *
     * @see <a href="http://www.geopackage.org/spec/#_requirement-8" target=
     *      "_blank">Structured Query Language (SQL) - Requirement 8</a>
     *
     * @throws SQLException
     *             If an SQL query causes an error
     */
    @Test(description = "See OGC 12-128r12: Requirement 8")
    public void sqlCheck() throws SQLException
    {
        try(final Statement stmt   = this.databaseConnection.createStatement();
            final ResultSet result = stmt.executeQuery("SELECT * FROM sqlite_master;"))
        {
            // If the statement can execute it has implemented the SQLite SQL API interface
            return;
        }
        catch(final SQLException ignored)
        {
            // fall through to failure
        }

        fail(ErrorMessage.format(ErrorMessageKeys.NO_SQL_ACCESS));
    }

    /**
     * Every GeoPackage SQLite Configuration SHALL have the SQLite library
     * compile and run time options specified in table <a href=
     * "http://www.geopackage.org/spec/#every_gpkg_sqlite_config_table"> Every
     * GeoPackage SQLite Configuration</a>.
     *
     * @see <a href="http://www.geopackage.org/spec/#_requirement-9" target=
     *      "_blank">Every GPKG SQLite Configuration - Requirement 9</a>
     *
     * @throws SQLException
     *             If an SQL query causes an error
     */
    @Test(description = "See OGC 12-128r12: Requirement 9")
    public void sqliteOptions() throws SQLException
    {
        try(final Statement statement = this.databaseConnection.createStatement();
            final ResultSet resultSet = statement.executeQuery("SELECT sqlite_compileoption_used('SQLITE_OMIT_*')"))
        {
            assertEquals(resultSet.getInt(1),
                         0,
                         ErrorMessage.format(ErrorMessageKeys.SQLITE_OMIT_OPTIONS));

        }
    }

    

    private static final Pattern TEXT_TYPE = Pattern.compile("TEXT\\([0-9]+\\)");
    private static final Pattern BLOB_TYPE = Pattern.compile("BLOB\\([0-9]+\\)");

    private static final List<String> ALLOWED_SQL_TYPES = Arrays.asList("BOOLEAN", "TINYINT", "SMALLINT", "MEDIUMINT",
                                                                        "INT", "FLOAT", "DOUBLE", "REAL",
                                                                        "TEXT", "BLOB", "DATE", "DATETIME",
                                                                        "GEOMETRY", "POINT", "LINESTRING", "POLYGON",
                                                                        "MULTIPOINT", "MULTILINESTRING", "MULTIPOLYGON", "GEOMETRYCOLLECTION",
                                                                        "INTEGER");
}
