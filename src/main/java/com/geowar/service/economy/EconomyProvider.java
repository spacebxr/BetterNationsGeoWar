package com.geowar.service.economy;

import java.util.UUID;

/**
 * Abstraction over the source of player money. Implemented by a Vault-backed
 * adapter when an economy plugin is present and by an internal ledger otherwise,
 * so the rest of the plugin charges and pays players without knowing which is in
 * use.
 */
public interface EconomyProvider {

    double balance(UUID playerId);

    boolean withdraw(UUID playerId, double amount);

    void deposit(UUID playerId, double amount);

    boolean has(UUID playerId, double amount);

    String format(double amount);

    String name();
}
