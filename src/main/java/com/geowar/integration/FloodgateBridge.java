package com.geowar.integration;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

public final class FloodgateBridge {
    private FloodgateBridge() { }

    public static boolean isFloodgatePlayer(UUID uuid) {
        if (Bukkit.getPluginManager().getPlugin("floodgate") == null) return false;
        try {
            Class<?> apiType = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object api = apiType.getMethod("getInstance").invoke(null);
            return Boolean.TRUE.equals(apiType.getMethod("isFloodgatePlayer", UUID.class).invoke(api, uuid));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    public static void sendForm(UUID uuid, Object form) {
        if (Bukkit.getPluginManager().getPlugin("floodgate") == null) return;
        try {
            Class<?> apiType = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object api = apiType.getMethod("getInstance").invoke(null);
            for (Method method : api.getClass().getMethods()) {
                if (method.getName().equals("sendForm") && method.getParameterCount() == 2) {
                    method.invoke(api, uuid, form);
                    return;
                }
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
    }
}
