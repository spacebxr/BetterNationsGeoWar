package com.geowar.integration;

import com.geowar.capture.CaptureZoneManager;
import com.geowar.service.economy.EconomyProvider;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.function.Function;

public final class TownyIntegration {
    private final Function<Player, String> resolver;
    public TownyIntegration() {
        resolver = player -> {
            try {
                com.palmergames.bukkit.towny.object.Town town = com.palmergames.bukkit.towny.TownyAPI.getInstance().getTown(player);
                return town == null ? null : town.getName();
            } catch (RuntimeException ignored) {
                return null;
            }
        };
    }
    public Function<Player, String> townResolver() { return resolver; }
}
