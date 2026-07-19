package com.geowar.data;

import com.geowar.GeoWarPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MeetingManager {

    private final GeoWarPlugin plugin;
    private File file;
    private FileConfiguration config;

    private final List<MeetingRequest> requests = new ArrayList<>();

    public MeetingManager(GeoWarPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "meetings.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(file);

        requests.clear();
        List<?> raw = config.getList("meetings");
        if (raw != null) {
            for (Object obj : raw) {
                if (obj instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) obj;
                    String fromNation = (String) map.get("fromNation");
                    String fromLeaderName = (String) map.get("fromLeaderName");
                    String toNation = (String) map.get("toNation");
                    String topic = (String) map.get("topic");
                    String status = (String) map.get("status");
                    long timestamp = map.get("timestamp") instanceof Long ? (Long) map.get("timestamp") : 0L;
                    requests.add(new MeetingRequest(fromNation, fromLeaderName, toNation, topic, status, timestamp));
                }
            }
        }
    }

    public void save() {
        List<Map<String, Object>> serialized = new ArrayList<>();
        for (MeetingRequest r : requests) {
            Map<String, Object> map = new HashMap<>();
            map.put("fromNation", r.fromNation);
            map.put("fromLeaderName", r.fromLeaderName);
            map.put("toNation", r.toNation);
            map.put("topic", r.topic);
            map.put("status", r.status);
            map.put("timestamp", r.timestamp);
            serialized.add(map);
        }
        config.set("meetings", serialized);
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public void requestMeeting(String fromNation, String fromLeaderName, String toNation, String topic) {
        requests.add(new MeetingRequest(fromNation, fromLeaderName, toNation, topic, "PENDING", System.currentTimeMillis()));
        save();
        notifyTargetNation(toNation, fromNation, fromLeaderName, topic);
    }

    private void notifyTargetNation(String toNation, String fromNation, String fromLeaderName, String topic) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            com.palmergames.bukkit.towny.object.Resident resident =
                com.palmergames.bukkit.towny.TownyAPI.getInstance().getResident(online);
            if (resident == null) continue;
            com.palmergames.bukkit.towny.object.Nation nation = resident.getNationOrNull();
            if (nation != null && nation.getName().equalsIgnoreCase(toNation) && resident.isKing()) {
                online.sendMessage(org.bukkit.ChatColor.GOLD + "[GeoWar] Meeting request from " +
                    org.bukkit.ChatColor.YELLOW + fromLeaderName + org.bukkit.ChatColor.GOLD +
                    " of " + fromNation + ": " + org.bukkit.ChatColor.WHITE + topic);
                online.sendMessage(org.bukkit.ChatColor.GRAY + "Check your Diplomacy GUI to respond.");
            }
        }
    }

    public List<MeetingRequest> getIncomingRequests(String toNation) {
        List<MeetingRequest> result = new ArrayList<>();
        for (MeetingRequest r : requests) {
            if (r.toNation.equalsIgnoreCase(toNation) && r.status.equals("PENDING")) {
                result.add(r);
            }
        }
        return result;
    }

    public List<MeetingRequest> getSentRequests(String fromNation) {
        List<MeetingRequest> result = new ArrayList<>();
        for (MeetingRequest r : requests) {
            if (r.fromNation.equalsIgnoreCase(fromNation)) {
                result.add(r);
            }
        }
        return result;
    }

    public void acceptMeeting(String fromNation, String toNation) {
        for (MeetingRequest r : requests) {
            if (r.fromNation.equalsIgnoreCase(fromNation) && r.toNation.equalsIgnoreCase(toNation) && r.status.equals("PENDING")) {
                r.status = "ACCEPTED";
                break;
            }
        }
        save();
    }

    public void declineMeeting(String fromNation, String toNation) {
        requests.removeIf(r ->
            r.fromNation.equalsIgnoreCase(fromNation) &&
            r.toNation.equalsIgnoreCase(toNation) &&
            r.status.equals("PENDING")
        );
        save();
    }

    public static class MeetingRequest {
        public final String fromNation;
        public final String fromLeaderName;
        public final String toNation;
        public final String topic;
        public String status;
        public final long timestamp;

        public MeetingRequest(String fromNation, String fromLeaderName, String toNation, String topic, String status, long timestamp) {
            this.fromNation = fromNation;
            this.fromLeaderName = fromLeaderName;
            this.toNation = toNation;
            this.topic = topic;
            this.status = status;
            this.timestamp = timestamp;
        }
    }
}
