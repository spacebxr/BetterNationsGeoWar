package com.geowar.service.economy;

import com.geowar.storage.Database;
import com.geowar.storage.repository.StorageException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Internal economy used when no Vault provider is available. Balances are cached
 * in memory and written through to the wallet table. Cache access is thread-safe
 * so the economy tick can pay players from the async executor.
 */
public class InternalEconomyProvider implements EconomyProvider {

    private static final DecimalFormat FORMAT = new DecimalFormat("#,##0.00");

    private final Database database;
    private final ConcurrentHashMap<UUID, Double> cache = new ConcurrentHashMap<>();

    public InternalEconomyProvider(Database database) {
        this.database = database;
    }

    @Override
    public double balance(UUID playerId) {
        return cache.computeIfAbsent(playerId, this::loadBalance);
    }

    private double loadBalance(UUID playerId) {
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT balance FROM geowar_wallets WHERE player_id = ?")) {
            statement.setString(1, playerId.toString());
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getDouble("balance") : 0.0;
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to load wallet " + playerId, ex);
        }
    }

    @Override
    public synchronized boolean withdraw(UUID playerId, double amount) {
        if (amount <= 0) {
            return false;
        }
        double current = balance(playerId);
        if (current < amount) {
            return false;
        }
        persist(playerId, current - amount);
        return true;
    }

    @Override
    public synchronized void deposit(UUID playerId, double amount) {
        if (amount <= 0) {
            return;
        }
        persist(playerId, balance(playerId) + amount);
    }

    @Override
    public boolean has(UUID playerId, double amount) {
        return balance(playerId) >= amount;
    }

    private void persist(UUID playerId, double balance) {
        cache.put(playerId, balance);
        String sql = database.isMySql()
                ? "INSERT INTO geowar_wallets (player_id, balance) VALUES (?, ?) " +
                  "ON DUPLICATE KEY UPDATE balance=VALUES(balance)"
                : "INSERT INTO geowar_wallets (player_id, balance) VALUES (?, ?) " +
                  "ON CONFLICT(player_id) DO UPDATE SET balance=excluded.balance";
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            statement.setDouble(2, balance);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new StorageException("Failed to save wallet " + playerId, ex);
        }
    }

    @Override
    public String format(double amount) {
        return "$" + FORMAT.format(amount);
    }

    @Override
    public String name() {
        return "Internal";
    }
}
