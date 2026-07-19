package com.geowar.config;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Loads user-facing strings from {@code messages.yml} and renders them with
 * colour codes and {@code {placeholder}} substitution. Centralising message
 * lookup keeps wording out of the game logic and makes it fully translatable.
 */
public final class Messages {

    private final Plugin plugin;
    private FileConfiguration messages;
    private String prefix;

    public Messages(Plugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.messages = YamlConfiguration.loadConfiguration(file);

        InputStream defaults = plugin.getResource("messages.yml");
        if (defaults != null) {
            messages.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaults, StandardCharsets.UTF_8)));
        }
        this.prefix = color(messages.getString("prefix", "&8[&bGeoWar&8] &r"));
    }

    public String get(String key) {
        return color(messages.getString(key, "&cMissing message: " + key));
    }

    public String format(String key, Map<String, String> placeholders) {
        String message = messages.getString(key, "&cMissing message: " + key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return color(message);
    }

    public void send(CommandSender target, String key) {
        target.sendMessage(prefix + get(key));
    }

    public void send(CommandSender target, String key, Map<String, String> placeholders) {
        target.sendMessage(prefix + format(key, placeholders));
    }

    public void sendRaw(CommandSender target, String key) {
        target.sendMessage(get(key));
    }

    public String prefix() {
        return prefix;
    }

    public static String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
