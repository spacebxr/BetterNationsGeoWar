package com.geowar.storage.repository;

import com.geowar.model.town.BuildingType;
import com.geowar.model.town.Town;
import com.geowar.storage.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Persists internally managed towns. Building levels are stored as a compact
 * {@code TYPE:level} comma-separated string to avoid a child table for what is a
 * small fixed-key map. Towny-backed towns are not stored here; they are resolved
 * live from the Towny integration.
 */
public class TownRepository {

    private final Database database;

    public TownRepository(Database database) {
        this.database = database;
    }

    public List<Town> loadAll() {
        List<Town> towns = new ArrayList<>();
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, nation_id, name, population, defense_rating, buildings, external_ref " +
                             "FROM geowar_towns");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                Town town = new Town(
                        UUID.fromString(rows.getString("id")),
                        rows.getString("name"),
                        UUID.fromString(rows.getString("nation_id")));
                town.setPopulation(rows.getInt("population"));
                town.setDefenseRating(rows.getDouble("defense_rating"));
                town.setExternalRef(rows.getString("external_ref"));
                deserializeBuildings(town, rows.getString("buildings"));
                towns.add(town);
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to load towns", ex);
        }
        return towns;
    }

    public void save(Town town) {
        String sql = database.isMySql()
                ? "INSERT INTO geowar_towns (id, nation_id, name, population, defense_rating, buildings, external_ref) " +
                  "VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                  "nation_id=VALUES(nation_id), name=VALUES(name), population=VALUES(population), " +
                  "defense_rating=VALUES(defense_rating), buildings=VALUES(buildings), external_ref=VALUES(external_ref)"
                : "INSERT INTO geowar_towns (id, nation_id, name, population, defense_rating, buildings, external_ref) " +
                  "VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT(id) DO UPDATE SET " +
                  "nation_id=excluded.nation_id, name=excluded.name, population=excluded.population, " +
                  "defense_rating=excluded.defense_rating, buildings=excluded.buildings, external_ref=excluded.external_ref";
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, town.id().toString());
            statement.setString(2, town.nationId().toString());
            statement.setString(3, town.name());
            statement.setInt(4, town.population());
            statement.setDouble(5, town.defenseRating());
            statement.setString(6, serializeBuildings(town));
            statement.setString(7, town.externalRef());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new StorageException("Failed to save town " + town.id(), ex);
        }
    }

    public void delete(UUID townId) {
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM geowar_towns WHERE id = ?")) {
            statement.setString(1, townId.toString());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new StorageException("Failed to delete town " + townId, ex);
        }
    }

    private String serializeBuildings(Town town) {
        StringBuilder builder = new StringBuilder();
        for (BuildingType type : BuildingType.values()) {
            int level = town.buildingLevel(type);
            if (level > 0) {
                if (builder.length() > 0) {
                    builder.append(',');
                }
                builder.append(type.name()).append(':').append(level);
            }
        }
        return builder.toString();
    }

    private void deserializeBuildings(Town town, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        for (String entry : raw.split(",")) {
            String[] parts = entry.split(":");
            if (parts.length != 2) {
                continue;
            }
            try {
                town.setBuildingLevel(BuildingType.valueOf(parts[0]), Integer.parseInt(parts[1]));
            } catch (IllegalArgumentException ignored) {
                // Skip entries for building types or levels no longer valid.
            }
        }
    }
}
