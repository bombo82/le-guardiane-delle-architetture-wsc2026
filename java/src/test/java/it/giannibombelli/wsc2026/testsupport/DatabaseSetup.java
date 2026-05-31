package it.giannibombelli.wsc2026.testsupport;

import org.flywaydb.core.Flyway;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Crea database SQLite isolati per i test. Ogni test gestisce il proprio lifecycle.
 */
public final class DatabaseSetup {

    private static final Path DATA_DIR = Path.of("data");

    public static DataSource initializeFileDb(String moduleName, String suiteName) {
        final Path testDbDir = DATA_DIR.resolve(moduleName);
        try {
            Files.createDirectories(testDbDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create test DB directory " + testDbDir, e);
        }
        Path dbFile = testDbDir.resolve(String.format("%s.db", suiteName));
        try {
            Files.deleteIfExists(dbFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete existing test DB file " + dbFile, e);
        }

        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(String.format("jdbc:sqlite:%s", dbFile.toAbsolutePath()));

        runFlywayMigrations(dataSource, String.format("classpath:db/migration/%s", moduleName));

        return dataSource;
    }

    public static DataSource initializeInMemoryDb(String moduleName) {
        // Use a unique name and cache=shared to allow multiple connections to the same in-memory DB
        String uniqueDbName = UUID.randomUUID().toString();

        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(String.format("jdbc:sqlite:file:%s?mode=memory&cache=shared", uniqueDbName));

        try {
            dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create keep-alive connection for in-memory DB", e);
        }

        runFlywayMigrations(dataSource, String.format("classpath:db/migration/%s", moduleName));

        return dataSource;
    }

    private static void runFlywayMigrations(DataSource dataSource, String location) {
        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations(location)
            .cleanDisabled(false)
            .load();

        flyway.clean();
        flyway.migrate();
    }
}
