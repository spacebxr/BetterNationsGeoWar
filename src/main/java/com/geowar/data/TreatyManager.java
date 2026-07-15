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

public class TreatyManager {

    private final GeoWarPlugin plugin;
    private File file;
    private FileConfiguration config;

    private final List<TreatyRecord> treaties = new ArrayList<>();

    public TreatyManager(GeoWarPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "treaties.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(file);

        treaties.clear();
        List<?> raw = config.getList("treaties");
        if (raw != null) {
            for (Object obj : raw) {
                if (obj instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) obj;
                    String nationA = (String) map.get("nationA");
                    String nationB = (String) map.get("nationB");
                    String type = (String) map.get("type");
                    String terms = (String) map.get("terms");
                    long timestamp = map.get("timestamp") instanceof Long ? (Long) map.get("timestamp") : 0L;
                    treaties.add(new TreatyRecord(nationA, nationB, type, terms, timestamp));
                }
            }
        }
    }

    public void save() {
        List<Map<String, Object>> serialized = new ArrayList<>();
        for (TreatyRecord treaty : treaties) {
            Map<String, Object> map = new HashMap<>();
            map.put("nationA", treaty.nationA);
            map.put("nationB", treaty.nationB);
            map.put("type", treaty.type);
            map.put("terms", treaty.terms);
            map.put("timestamp", treaty.timestamp);
            serialized.add(map);
        }
        config.set("treaties", serialized);
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public void logTreaty(String nationA, String nationB, String type, String terms) {
        treaties.add(new TreatyRecord(nationA, nationB, type, terms, System.currentTimeMillis()));
        save();
    }

    public boolean revokeTreaty(String nationA, String nationB) {
        return treaties.removeIf(t ->
            (t.nationA.equalsIgnoreCase(nationA) && t.nationB.equalsIgnoreCase(nationB)) ||
            (t.nationA.equalsIgnoreCase(nationB) && t.nationB.equalsIgnoreCase(nationA))
        ) && (save() == null ? true : true);
    }

    public boolean hasTreaty(String nationA, String nationB) {
        for (TreatyRecord t : treaties) {
            if ((t.nationA.equalsIgnoreCase(nationA) && t.nationB.equalsIgnoreCase(nationB)) ||
                (t.nationA.equalsIgnoreCase(nationB) && t.nationB.equalsIgnoreCase(nationA))) {
                return true;
            }
        }
        return false;
    }

    public List<TreatyRecord> getTreatiesFor(String nation) {
        List<TreatyRecord> result = new ArrayList<>();
        for (TreatyRecord t : treaties) {
            if (t.nationA.equalsIgnoreCase(nation) || t.nationB.equalsIgnoreCase(nation)) {
                result.add(t);
            }
        }
        return result;
    }

    public static class TreatyRecord {
        public final String nationA;
        public final String nationB;
        public final String type;
        public final String terms;
        public final long timestamp;

        public TreatyRecord(String nationA, String nationB, String type, String terms, long timestamp) {
            this.nationA = nationA;
            this.nationB = nationB;
            this.type = type;
            this.terms = terms;
            this.timestamp = timestamp;
        }
    }
}
