package com.geowar.integration;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.function.Function;

public final class TownyIntegration {
    private final Function<Player, String> resolver;

    public TownyIntegration() {
        resolver = player -> {
            try {
                Class<?> apiType = Class.forName("com.palmergames.bukkit.towny.TownyAPI");
                Object api = apiType.getMethod("getInstance").invoke(null);
                Object town = apiType.getMethod("getTown", Player.class).invoke(api, player);
                return town == null ? null : String.valueOf(town.getClass().getMethod("getName").invoke(town));
            } catch (ReflectiveOperationException | LinkageError ignored) {
                return null;
            }
        };
    }

    public Function<Player, String> townResolver() { return resolver; }
}
