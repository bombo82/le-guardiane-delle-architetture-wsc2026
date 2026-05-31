package it.giannibombelli.wsc2026.common.module;

import io.javalin.config.JavalinConfig;
import org.flywaydb.core.Flyway;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class ApplicationModule {

    public static DataSource initializeDb(String moduleName) {
        ensureDataDirectoryExists(moduleName);
        SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl(String.format("jdbc:sqlite:data/%s/%s.db", moduleName, moduleName));

        runFlywayMigrations(ds, moduleName);

        return ds;
    }

    public abstract void configure(JavalinConfig config);

    /**
     * Override this method if clean-up is needed during an application shutdown.
     * For example, shutdown scheduled executors or close resources.
     */
    public void stop() {
    }

    protected static void ensureDataDirectoryExists(String moduleName) {
        try {
            Files.createDirectories(Path.of("data", moduleName));
        } catch (java.io.IOException e) {
            throw new RuntimeException(String.format("Failed to create data/%s", moduleName), e);
        }
    }

    protected static void runFlywayMigrations(DataSource ds, String moduleName) {
        Flyway.configure()
            .dataSource(ds)
            .locations(String.format("classpath:db/migration/%s", moduleName))
            .cleanDisabled(true)
            .load()
            .migrate();
    }

}
