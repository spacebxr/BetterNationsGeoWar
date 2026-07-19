package com.geowar.economy;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public final class EconomyService {
    private final Object economy;

    public EconomyService() {
        this(true);
    }

    public EconomyService(boolean enabled) {
        this.economy = enabled ? findProvider() : null;
    }

    private Object findProvider() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return null;
        try {
            Class<?> economyType = Class.forName("net.milkbowl.vault.economy.Economy");
            Object registration = Bukkit.getServicesManager().getClass()
                    .getMethod("getRegistration", Class.class)
                    .invoke(Bukkit.getServicesManager(), economyType);
            return registration == null ? null : registration.getClass().getMethod("getProvider").invoke(registration);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    public boolean isAvailable() { return economy != null; }

    public double getBalance(Player player) {
        return invokeNumber("getBalance", player);
    }

    public boolean has(Player player, double amount) {
        return invokeBoolean("has", player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        return transaction("withdrawPlayer", player, amount);
    }

    public boolean deposit(Player player, double amount) {
        return transaction("depositPlayer", player, amount);
    }

    public String format(double amount) {
        if (economy == null) return "$" + String.format("%.2f", amount);
        try {
            Object result = economy.getClass().getMethod("format", double.class).invoke(economy, amount);
            return String.valueOf(result);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return "$" + String.format("%.2f", amount);
        }
    }

    private double invokeNumber(String method, Player player) {
        if (economy == null) return 0;
        try {
            Object result = economy.getClass().getMethod(method, Player.class).invoke(economy, player);
            return result instanceof Number number ? number.doubleValue() : 0;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return 0;
        }
    }

    private boolean invokeBoolean(String method, Player player, double amount) {
        if (economy == null) return false;
        try {
            Object result = economy.getClass().getMethod(method, Player.class, double.class).invoke(economy, player, amount);
            return result instanceof Boolean value && value;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    private boolean transaction(String method, Player player, double amount) {
        if (economy == null) return false;
        try {
            Object response = economy.getClass().getMethod(method, Player.class, double.class).invoke(economy, player, amount);
            Method success = response.getClass().getMethod("transactionSuccess");
            return Boolean.TRUE.equals(success.invoke(response));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }
}
