package com.geowar.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyService {

    private Economy economy;
    private boolean available = false;

    public EconomyService() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return;
        }
        economy = rsp.getProvider();
        available = economy != null;
    }

    public boolean isAvailable() {
        return available;
    }

    public double getBalance(Player player) {
        if (!available) return 0;
        return economy.getBalance(player);
    }

    public boolean has(Player player, double amount) {
        if (!available) return false;
        return economy.has(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        if (!available) return false;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean deposit(Player player, double amount) {
        if (!available) return false;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public String format(double amount) {
        if (!available) return "$" + String.format("%.2f", amount);
        return economy.format(amount);
    }
}
