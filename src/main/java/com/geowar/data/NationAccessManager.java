package com.geowar.data;

import com.geowar.GeoWarPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class NationAccessManager {

    private final File file;
    private FileConfiguration config;

    public NationAccessManager(GeoWarPlugin plugin) {
        file = new File(plugin.getDataFolder(), "access.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) { plugin.getLogger().severe("Could not create access.yml: " + e.getMessage()); }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void grantAccess(String nationName, UUID uuid, String playerName) {
        String key = "nations." + nationName + "." + uuid.toString();
        config.set(key, playerName);
        save();
    }

    public void revokeAccess(String nationName, UUID uuid) {
        String key = "nations." + nationName + "." + uuid.toString();
        config.set(key, null);
        save();
    }

    public boolean hasAccess(String nationName, UUID uuid) {
        return config.contains("nations." + nationName + "." + uuid.toString());
    }

    public List<AccessEntry> getAccessList(String nationName) {
        List<AccessEntry> entries = new ArrayList<>();
        if (!config.contains("nations." + nationName)) return entries;
        for (String uuidStr : config.getConfigurationSection("nations." + nationName).getKeys(false)) {
            String name = config.getString("nations." + nationName + "." + uuidStr, "Unknown");
            try {
                entries.add(new AccessEntry(UUID.fromString(uuidStr), name));
            } catch (IllegalArgumentException ignored) {}
        }
        return entries;
    }

    public void save() {
        try { config.save(file); } catch (IOException e) { GeoWarPlugin.getInstance().getLogger().severe("Could not save access.yml: " + e.getMessage()); }
    }

    public static class AccessEntry {
        public final UUID uuid;
        public final String name;
        public AccessEntry(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }
    }
}
