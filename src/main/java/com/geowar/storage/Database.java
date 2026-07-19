package com.geowar.storage;

import com.geowar.config.PluginConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Owns the JDBC connection pool. Abstracts SQLite (file-backed, default) and
 * MySQL behind a single {@link #connection()} accessor so repositories are
 * dialect-agnostic. SQLite is configured with a single pooled connection because
 * the driver serialises writes anyway.
 */
public final class Database {

    private final PluginConfig config;
    private final File dataFolder;
    private final boolean mysql;
    private HikariDataSource dataSource;

    public Database(PluginConfig config, File dataFolder) {
        this.config = config;
        this.dataFolder = dataFolder;
        this.mysql = config.storageType() == PluginConfig.StorageType.MYSQL;
    }

    public void connect() {
        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("GeoWar-Pool");

        if (mysql) {
            hikari.setJdbcUrl("jdbc:mysql://" + config.mysqlHost() + ":" + config.mysqlPort()
                    + "/" + config.mysqlDatabase() + "?useSSL=false&allowPublicKeyRetrieval=true"
                    + "&characterEncoding=utf8");
            hikari.setUsername(config.mysqlUser());
            hikari.setPassword(config.mysqlPassword());
            hikari.setMaximumPoolSize(10);
        } else {
            if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                throw new IllegalStateException("Could not create data folder " + dataFolder);
            }
            File file = new File(dataFolder, "geowar.db");
            hikari.setDriverClassName("org.sqlite.JDBC");
            hikari.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
            hikari.setMaximumPoolSize(1);
            hikari.setConnectionTestQuery("SELECT 1");
        }

        hikari.setMaxLifetime(600_000L);
        hikari.setConnectionTimeout(10_000L);
        this.dataSource = new HikariDataSource(hikari);
    }

    public Connection connection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Database is not connected");
        }
        return dataSource.getConnection();
    }

    public boolean isMySql() {
        return mysql;
    }

    /** SQL type for an auto-incrementing/text primary key column, per dialect. */
    public String primaryKeyType() {
        return mysql ? "VARCHAR(36)" : "TEXT";
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
