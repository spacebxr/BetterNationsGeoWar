package com.geowar.storage.repository;

import com.geowar.model.military.Military;
import com.geowar.model.nation.Citizen;
import com.geowar.model.nation.Nation;
import com.geowar.model.role.NationRole;
import com.geowar.storage.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Persists the nation aggregate: the nation row, its treasury columns, its
 * citizens and its military, kept consistent within a single transaction per
 * save. All methods are blocking and are expected to be called from the async
 * executor.
 */
public class NationRepository {

    private final Database database;

    public NationRepository(Database database) {
        this.database = database;
    }

    public List<Nation> loadAll() {
        List<Nation> nations = new ArrayList<>();
        try (Connection connection = database.connection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id, name, tag, leader_id, founded_at, power, balance, tax_rate FROM geowar_nations");
                 ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    nations.add(readNation(rows));
                }
            }
            for (Nation nation : nations) {
                loadCitizens(connection, nation);
                loadMilitary(connection, nation);
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to load nations", ex);
        }
        return nations;
    }

    private Nation readNation(ResultSet rows) throws SQLException {
        Nation nation = new Nation(
                UUID.fromString(rows.getString("id")),
                rows.getString("name"),
                rows.getString("tag"),
                UUID.fromString(rows.getString("leader_id")),
                rows.getLong("founded_at"));
        nation.setPower(rows.getDouble("power"));
        nation.treasury().setBalance(rows.getDouble("balance"));
        nation.treasury().setTaxRate(rows.getDouble("tax_rate"));
        return nation;
    }

    private void loadCitizens(Connection connection, Nation nation) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT player_id, name, role, job, salary, happiness, loyalty, joined_at " +
                        "FROM geowar_citizens WHERE nation_id = ?")) {
            statement.setString(1, nation.id().toString());
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    Citizen citizen = new Citizen(
                            UUID.fromString(rows.getString("player_id")),
                            rows.getString("name"),
                            NationRole.fromString(rows.getString("role"), NationRole.CITIZEN),
                            rows.getLong("joined_at"));
                    citizen.setJob(rows.getString("job"));
                    citizen.setSalary(rows.getDouble("salary"));
                    citizen.setHappiness(rows.getDouble("happiness"));
                    citizen.setLoyalty(rows.getDouble("loyalty"));
                    nation.addCitizen(citizen);
                }
            }
        }
    }

    private void loadMilitary(Connection connection, Nation nation) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT troops, generals, morale, training, equipment, budget " +
                        "FROM geowar_military WHERE nation_id = ?")) {
            statement.setString(1, nation.id().toString());
            try (ResultSet rows = statement.executeQuery()) {
                if (rows.next()) {
                    Military military = nation.military();
                    military.setTroops(rows.getInt("troops"));
                    military.setGenerals(rows.getInt("generals"));
                    military.setMorale(rows.getDouble("morale"));
                    military.setTraining(rows.getDouble("training"));
                    military.setEquipment(rows.getDouble("equipment"));
                    military.setBudget(rows.getDouble("budget"));
                }
            }
        }
    }

    public void save(Nation nation) {
        try (Connection connection = database.connection()) {
            connection.setAutoCommit(false);
            try {
                upsertNation(connection, nation);
                replaceCitizens(connection, nation);
                upsertMilitary(connection, nation);
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to save nation " + nation.id(), ex);
        }
    }

    private void upsertNation(Connection connection, Nation nation) throws SQLException {
        String sql = database.isMySql()
                ? "INSERT INTO geowar_nations (id, name, tag, leader_id, founded_at, power, balance, tax_rate) " +
                  "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                  "name=VALUES(name), tag=VALUES(tag), leader_id=VALUES(leader_id), power=VALUES(power), " +
                  "balance=VALUES(balance), tax_rate=VALUES(tax_rate)"
                : "INSERT INTO geowar_nations (id, name, tag, leader_id, founded_at, power, balance, tax_rate) " +
                  "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(id) DO UPDATE SET " +
                  "name=excluded.name, tag=excluded.tag, leader_id=excluded.leader_id, power=excluded.power, " +
                  "balance=excluded.balance, tax_rate=excluded.tax_rate";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, nation.id().toString());
            statement.setString(2, nation.name());
            statement.setString(3, nation.tag());
            statement.setString(4, nation.leaderId().toString());
            statement.setLong(5, nation.foundedAt());
            statement.setDouble(6, nation.power());
            statement.setDouble(7, nation.treasury().balance());
            statement.setDouble(8, nation.treasury().taxRate());
            statement.executeUpdate();
        }
    }

    private void replaceCitizens(Connection connection, Nation nation) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM geowar_citizens WHERE nation_id = ?")) {
            delete.setString(1, nation.id().toString());
            delete.executeUpdate();
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO geowar_citizens (player_id, nation_id, name, role, job, salary, happiness, loyalty, joined_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            for (Citizen citizen : nation.citizens()) {
                insert.setString(1, citizen.playerId().toString());
                insert.setString(2, nation.id().toString());
                insert.setString(3, citizen.name());
                insert.setString(4, citizen.role().name());
                insert.setString(5, citizen.job());
                insert.setDouble(6, citizen.salary());
                insert.setDouble(7, citizen.happiness());
                insert.setDouble(8, citizen.loyalty());
                insert.setLong(9, citizen.joinedAt());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private void upsertMilitary(Connection connection, Nation nation) throws SQLException {
        Military military = nation.military();
        String sql = database.isMySql()
                ? "INSERT INTO geowar_military (nation_id, troops, generals, morale, training, equipment, budget) " +
                  "VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                  "troops=VALUES(troops), generals=VALUES(generals), morale=VALUES(morale), " +
                  "training=VALUES(training), equipment=VALUES(equipment), budget=VALUES(budget)"
                : "INSERT INTO geowar_military (nation_id, troops, generals, morale, training, equipment, budget) " +
                  "VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT(nation_id) DO UPDATE SET " +
                  "troops=excluded.troops, generals=excluded.generals, morale=excluded.morale, " +
                  "training=excluded.training, equipment=excluded.equipment, budget=excluded.budget";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, nation.id().toString());
            statement.setInt(2, military.troops());
            statement.setInt(3, military.generals());
            statement.setDouble(4, military.morale());
            statement.setDouble(5, military.training());
            statement.setDouble(6, military.equipment());
            statement.setDouble(7, military.budget());
            statement.executeUpdate();
        }
    }

    public void delete(UUID nationId) {
        try (Connection connection = database.connection()) {
            connection.setAutoCommit(false);
            try {
                deleteFrom(connection, "geowar_citizens", "nation_id", nationId);
                deleteFrom(connection, "geowar_military", "nation_id", nationId);
                deleteFrom(connection, "geowar_towns", "nation_id", nationId);
                deleteFrom(connection, "geowar_nations", "id", nationId);
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to delete nation " + nationId, ex);
        }
    }

    private void deleteFrom(Connection connection, String table, String column, UUID id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM " + table + " WHERE " + column + " = ?")) {
            statement.setString(1, id.toString());
            statement.executeUpdate();
        }
    }
}
