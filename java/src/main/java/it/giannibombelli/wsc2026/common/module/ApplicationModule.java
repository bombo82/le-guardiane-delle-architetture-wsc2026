package it.giannibombelli.wsc2026.common.module;

import org.flywaydb.core.Flyway;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.List;
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

    public abstract List<WebApi> webApis();

    public void stop() {
    }

    protected static void ensureDataDirectoryExists(String moduleName) {
        try {
            Files.createDirectories(Path.of("data", moduleName));
        } catch (IOException e) {
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
