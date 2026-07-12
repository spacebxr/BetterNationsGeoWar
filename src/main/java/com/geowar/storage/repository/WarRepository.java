package com.geowar.storage.repository;

import com.geowar.model.war.War;
import com.geowar.model.war.WarState;
import com.geowar.model.war.WarType;
import com.geowar.storage.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Persists wars and their running score metrics. Concluded wars are retained so
 * the statistics menu can report historical conflicts.
 */
public class WarRepository {

    private final Database database;

    public WarRepository(Database database) {
        this.database = database;
    }

    public List<War> loadAll() {
        List<War> wars = new ArrayList<>();
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, attacker, defender, type, state, started_at, ended_at, " +
                             "atk_towns, def_towns, atk_battles, def_battles, atk_losses, def_losses, " +
                             "atk_econ, def_econ FROM geowar_wars");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                wars.add(readWar(rows));
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to load wars", ex);
        }
        return wars;
    }

    private War readWar(ResultSet rows) throws SQLException {
        War war = new War(
                UUID.fromString(rows.getString("id")),
                UUID.fromString(rows.getString("attacker")),
                UUID.fromString(rows.getString("defender")),
                WarType.valueOf(rows.getString("type")),
                rows.getLong("started_at"));
        war.setState(WarState.valueOf(rows.getString("state")));
        war.setEndedAt(rows.getLong("ended_at"));

        replayMetrics(war, rows);
        return war;
    }

    private void replayMetrics(War war, ResultSet rows) throws SQLException {
        for (int i = 0; i < rows.getInt("atk_towns"); i++) {
            war.recordTownCapture(war.attacker());
        }
        for (int i = 0; i < rows.getInt("def_towns"); i++) {
            war.recordTownCapture(war.defender());
        }
        for (int i = 0; i < rows.getInt("atk_battles"); i++) {
            war.recordBattleWin(war.attacker());
        }
        for (int i = 0; i < rows.getInt("def_battles"); i++) {
            war.recordBattleWin(war.defender());
        }
        war.recordTroopLosses(war.attacker(), rows.getInt("atk_losses"));
        war.recordTroopLosses(war.defender(), rows.getInt("def_losses"));
        war.recordEconomicDamage(war.attacker(), rows.getDouble("atk_econ"));
        war.recordEconomicDamage(war.defender(), rows.getDouble("def_econ"));
    }

    public void save(War war) {
        String sql = database.isMySql()
                ? "INSERT INTO geowar_wars (id, attacker, defender, type, state, started_at, ended_at, " +
                  "atk_towns, def_towns, atk_battles, def_battles, atk_losses, def_losses, atk_econ, def_econ) " +
                  "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                  "state=VALUES(state), ended_at=VALUES(ended_at), atk_towns=VALUES(atk_towns), " +
                  "def_towns=VALUES(def_towns), atk_battles=VALUES(atk_battles), def_battles=VALUES(def_battles), " +
                  "atk_losses=VALUES(atk_losses), def_losses=VALUES(def_losses), atk_econ=VALUES(atk_econ), " +
                  "def_econ=VALUES(def_econ)"
                : "INSERT INTO geowar_wars (id, attacker, defender, type, state, started_at, ended_at, " +
                  "atk_towns, def_towns, atk_battles, def_battles, atk_losses, def_losses, atk_econ, def_econ) " +
                  "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(id) DO UPDATE SET " +
                  "state=excluded.state, ended_at=excluded.ended_at, atk_towns=excluded.atk_towns, " +
                  "def_towns=excluded.def_towns, atk_battles=excluded.atk_battles, def_battles=excluded.def_battles, " +
                  "atk_losses=excluded.atk_losses, def_losses=excluded.def_losses, atk_econ=excluded.atk_econ, " +
                  "def_econ=excluded.def_econ";
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, war.id().toString());
            statement.setString(2, war.attacker().toString());
            statement.setString(3, war.defender().toString());
            statement.setString(4, war.type().name());
            statement.setString(5, war.state().name());
            statement.setLong(6, war.startedAt());
            statement.setLong(7, war.endedAt());
            statement.setInt(8, war.attackerTownsCaptured());
            statement.setInt(9, war.defenderTownsCaptured());
            statement.setInt(10, war.attackerBattlesWon());
            statement.setInt(11, war.defenderBattlesWon());
            statement.setInt(12, war.attackerTroopLosses());
            statement.setInt(13, war.defenderTroopLosses());
            statement.setDouble(14, war.attackerEconomicDamage());
            statement.setDouble(15, war.defenderEconomicDamage());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new StorageException("Failed to save war " + war.id(), ex);
        }
    }
}
