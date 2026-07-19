package com.geowar.storage;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Creates the relational schema on startup. Statements use a portable type
 * subset (TEXT/INTEGER/REAL) understood by both SQLite and MySQL; the MySQL
 * variant substitutes sized text columns so keys can be indexed.
 */
public final class SchemaInitializer {

    private final Database database;

    public SchemaInitializer(Database database) {
        this.database = database;
    }

    public void apply() throws SQLException {
        String key = database.isMySql() ? "VARCHAR(36)" : "TEXT";
        String text = database.isMySql() ? "VARCHAR(255)" : "TEXT";

        List<String> statements = List.of(
                "CREATE TABLE IF NOT EXISTS geowar_nations (" +
                        "id " + key + " PRIMARY KEY," +
                        "name " + text + " NOT NULL," +
                        "tag " + text + "," +
                        "leader_id " + key + " NOT NULL," +
                        "founded_at INTEGER NOT NULL," +
                        "power REAL NOT NULL DEFAULT 0," +
                        "balance REAL NOT NULL DEFAULT 0," +
                        "tax_rate REAL NOT NULL DEFAULT 0.1)",

                "CREATE TABLE IF NOT EXISTS geowar_citizens (" +
                        "player_id " + key + " PRIMARY KEY," +
                        "nation_id " + key + " NOT NULL," +
                        "name " + text + " NOT NULL," +
                        "role " + text + " NOT NULL," +
                        "job " + text + "," +
                        "salary REAL NOT NULL DEFAULT 0," +
                        "happiness REAL NOT NULL DEFAULT 50," +
                        "loyalty REAL NOT NULL DEFAULT 50," +
                        "joined_at INTEGER NOT NULL)",

                "CREATE TABLE IF NOT EXISTS geowar_military (" +
                        "nation_id " + key + " PRIMARY KEY," +
                        "troops INTEGER NOT NULL DEFAULT 0," +
                        "generals INTEGER NOT NULL DEFAULT 0," +
                        "morale REAL NOT NULL DEFAULT 50," +
                        "training REAL NOT NULL DEFAULT 10," +
                        "equipment REAL NOT NULL DEFAULT 10," +
                        "budget REAL NOT NULL DEFAULT 0)",

                "CREATE TABLE IF NOT EXISTS geowar_towns (" +
                        "id " + key + " PRIMARY KEY," +
                        "nation_id " + key + " NOT NULL," +
                        "name " + text + " NOT NULL," +
                        "population INTEGER NOT NULL DEFAULT 0," +
                        "defense_rating REAL NOT NULL DEFAULT 0," +
                        "buildings " + text + "," +
                        "external_ref " + text + ")",

                "CREATE TABLE IF NOT EXISTS geowar_relations (" +
                        "nation_a " + key + " NOT NULL," +
                        "nation_b " + key + " NOT NULL," +
                        "relation " + text + " NOT NULL," +
                        "since INTEGER NOT NULL," +
                        "PRIMARY KEY (nation_a, nation_b))",

                "CREATE TABLE IF NOT EXISTS geowar_wars (" +
                        "id " + key + " PRIMARY KEY," +
                        "attacker " + key + " NOT NULL," +
                        "defender " + key + " NOT NULL," +
                        "type " + text + " NOT NULL," +
                        "state " + text + " NOT NULL," +
                        "started_at INTEGER NOT NULL," +
                        "ended_at INTEGER," +
                        "atk_towns INTEGER NOT NULL DEFAULT 0," +
                        "def_towns INTEGER NOT NULL DEFAULT 0," +
                        "atk_battles INTEGER NOT NULL DEFAULT 0," +
                        "def_battles INTEGER NOT NULL DEFAULT 0," +
                        "atk_losses INTEGER NOT NULL DEFAULT 0," +
                        "def_losses INTEGER NOT NULL DEFAULT 0," +
                        "atk_econ REAL NOT NULL DEFAULT 0," +
                        "def_econ REAL NOT NULL DEFAULT 0)",

                "CREATE TABLE IF NOT EXISTS geowar_wallets (" +
                        "player_id " + key + " PRIMARY KEY," +
                        "balance REAL NOT NULL DEFAULT 0)",

                "CREATE INDEX IF NOT EXISTS idx_citizens_nation ON geowar_citizens (nation_id)",
                "CREATE INDEX IF NOT EXISTS idx_towns_nation ON geowar_towns (nation_id)",
                "CREATE INDEX IF NOT EXISTS idx_wars_attacker ON geowar_wars (attacker)",
                "CREATE INDEX IF NOT EXISTS idx_wars_defender ON geowar_wars (defender)");

        try (Connection connection = database.connection();
             Statement statement = connection.createStatement()) {
            for (String ddl : statements) {
                statement.execute(ddl);
            }
        }
    }
}
