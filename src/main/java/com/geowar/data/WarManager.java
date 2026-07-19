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

public class WarManager {

    private final GeoWarPlugin plugin;
    private File file;
    private FileConfiguration config;

    private final List<WarRecord> activeWars = new ArrayList<>();

    public WarManager(GeoWarPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "wars.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(file);

        activeWars.clear();
        List<?> raw = config.getList("wars");
        if (raw != null) {
            for (Object obj : raw) {
                if (obj instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) obj;
                    String attacker = (String) map.get("attacker");
                    String defender = (String) map.get("defender");
                    String reason = (String) map.get("reason");
                    String demands = (String) map.get("demands");
                    long timestamp = map.get("timestamp") instanceof Long ? (Long) map.get("timestamp") : 0L;
                    activeWars.add(new WarRecord(attacker, defender, reason, demands, timestamp));
                }
            }
        }
    }

    public void save() {
        List<Map<String, Object>> serialized = new ArrayList<>();
        for (WarRecord war : activeWars) {
            Map<String, Object> map = new HashMap<>();
            map.put("attacker", war.attacker);
            map.put("defender", war.defender);
            map.put("reason", war.reason);
            map.put("demands", war.demands);
            map.put("timestamp", war.timestamp);
            serialized.add(map);
        }
        config.set("wars", serialized);
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public boolean isAtWar(String nationA, String nationB) {
        for (WarRecord war : activeWars) {
            if ((war.attacker.equalsIgnoreCase(nationA) && war.defender.equalsIgnoreCase(nationB)) ||
                (war.attacker.equalsIgnoreCase(nationB) && war.defender.equalsIgnoreCase(nationA))) {
                return true;
            }
        }
        return false;
    }

    public void declareWar(String attacker, String defender, String reason, String demands) {
        activeWars.add(new WarRecord(attacker, defender, reason, demands, System.currentTimeMillis()));
        save();
    }

    public boolean endWar(String nationA, String nationB) {
        boolean removed = activeWars.removeIf(war ->
            (war.attacker.equalsIgnoreCase(nationA) && war.defender.equalsIgnoreCase(nationB)) ||
            (war.attacker.equalsIgnoreCase(nationB) && war.defender.equalsIgnoreCase(nationA))
        );
        save();
        return removed;
    }

    public List<WarRecord> getActiveWars() {
        return activeWars;
    }

    public List<WarRecord> getWarsFor(String nation) {
        List<WarRecord> result = new ArrayList<>();
        for (WarRecord war : activeWars) {
            if (war.attacker.equalsIgnoreCase(nation) || war.defender.equalsIgnoreCase(nation)) {
                result.add(war);
            }
        }
        return result;
    }

    public static class WarRecord {
        public final String attacker;
        public final String defender;
        public final String reason;
        public final String demands;
        public final long timestamp;

        public WarRecord(String attacker, String defender, String reason, String demands, long timestamp) {
            this.attacker = attacker;
            this.defender = defender;
            this.reason = reason;
            this.demands = demands;
            this.timestamp = timestamp;
        }
    }
}
