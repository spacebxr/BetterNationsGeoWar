package com.geowar.data;

import com.geowar.GeoWarPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MilitaryManager {

    private final GeoWarPlugin plugin;
    private File file;
    private FileConfiguration config;

    public MilitaryManager(GeoWarPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "military.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public void setMilitaryRank(UUID playerUuid, String rank) {
        config.set("ranks." + playerUuid.toString(), rank);
        save();
    }

    public String getMilitaryRank(UUID playerUuid) {
        return config.getString("ranks." + playerUuid.toString(), "Civilian");
    }

    public void removeMilitaryRank(UUID playerUuid) {
        config.set("ranks." + playerUuid.toString(), null);
        save();
    }

    public List<MilitaryMember> getMembersOf(String nation) {
        List<MilitaryMember> result = new ArrayList<>();
        com.palmergames.bukkit.towny.object.Nation n =
            com.palmergames.bukkit.towny.TownyAPI.getInstance().getNation(nation);
        if (n == null) return result;

        for (com.palmergames.bukkit.towny.object.Resident resident : n.getResidents()) {
            String rank = getMilitaryRank(resident.getUUID());
            if (!rank.equals("Civilian")) {
                result.add(new MilitaryMember(resident.getName(), rank));
            }
        }
        return result;
    }

    public static class MilitaryMember {
        public final String name;
        public final String rank;

        public MilitaryMember(String name, String rank) {
            this.name = name;
            this.rank = rank;
        }
    }
}
