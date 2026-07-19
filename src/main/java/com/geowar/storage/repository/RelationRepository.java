package com.geowar.storage.repository;

import com.geowar.model.diplomacy.RelationType;
import com.geowar.storage.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Persists standing relations between nation pairs. A relation is stored once
 * per unordered pair with a canonical ordering (lexicographically smaller id
 * first) so lookups never depend on argument order.
 */
public class RelationRepository {

    /** A stored relation between two nations. */
    public record RelationRecord(UUID first, UUID second, RelationType type, long since) {
    }

    private final Database database;

    public RelationRepository(Database database) {
        this.database = database;
    }

    public List<RelationRecord> loadAll() {
        List<RelationRecord> records = new ArrayList<>();
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT nation_a, nation_b, relation, since FROM geowar_relations");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                records.add(new RelationRecord(
                        UUID.fromString(rows.getString("nation_a")),
                        UUID.fromString(rows.getString("nation_b")),
                        RelationType.valueOf(rows.getString("relation")),
                        rows.getLong("since")));
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to load relations", ex);
        }
        return records;
    }

    public void save(UUID a, UUID b, RelationType type, long since) {
        UUID first = canonicalFirst(a, b);
        UUID second = canonicalSecond(a, b);
        String sql = database.isMySql()
                ? "INSERT INTO geowar_relations (nation_a, nation_b, relation, since) VALUES (?, ?, ?, ?) " +
                  "ON DUPLICATE KEY UPDATE relation=VALUES(relation), since=VALUES(since)"
                : "INSERT INTO geowar_relations (nation_a, nation_b, relation, since) VALUES (?, ?, ?, ?) " +
                  "ON CONFLICT(nation_a, nation_b) DO UPDATE SET relation=excluded.relation, since=excluded.since";
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, first.toString());
            statement.setString(2, second.toString());
            statement.setString(3, type.name());
            statement.setLong(4, since);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new StorageException("Failed to save relation", ex);
        }
    }

    public void delete(UUID a, UUID b) {
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM geowar_relations WHERE nation_a = ? AND nation_b = ?")) {
            statement.setString(1, canonicalFirst(a, b).toString());
            statement.setString(2, canonicalSecond(a, b).toString());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new StorageException("Failed to delete relation", ex);
        }
    }

    private static UUID canonicalFirst(UUID a, UUID b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    private static UUID canonicalSecond(UUID a, UUID b) {
        return a.compareTo(b) <= 0 ? b : a;
    }
}
