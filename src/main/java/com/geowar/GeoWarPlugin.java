package com.geowar;

import com.geowar.commands.NationGuiCommand;
import com.geowar.data.MeetingManager;
import com.geowar.data.MilitaryManager;
import com.geowar.data.TreatyManager;
import com.geowar.data.WarManager;
import com.geowar.discord.DiscordWebhookService;
import com.geowar.economy.EconomyService;
import com.geowar.listeners.ChatInputListener;
import com.geowar.listeners.GuiListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GeoWarPlugin extends JavaPlugin {

    private static GeoWarPlugin instance;

    private WarManager warManager;
    private TreatyManager treatyManager;
    private MeetingManager meetingManager;
    private MilitaryManager militaryManager;
    private EconomyService economyService;
    private DiscordWebhookService discordWebhookService;

    private final Map<UUID, String> pendingActions = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        getDataFolder().mkdirs();
        saveDefaultConfig();

        economyService = new EconomyService();
        if (!economyService.isAvailable()) {
            getLogger().warning("Vault economy not found. Payment features will be disabled.");
        } else {
            getLogger().info("Vault economy hooked successfully.");
        }

        discordWebhookService = new DiscordWebhookService();
        getLogger().info("Discord webhook service initialized.");

        warManager = new WarManager(this);
        treatyManager = new TreatyManager(this);
        meetingManager = new MeetingManager(this);
        militaryManager = new MilitaryManager(this);

        getCommand("nationgui").setExecutor(new NationGuiCommand());
        getServer().getPluginManager().registerEvents(new GuiListener(), this);
        getServer().getPluginManager().registerEvents(new ChatInputListener(), this);

        getLogger().info("GeoWarPlugin enabled.");
    }

    @Override
    public void onDisable() {
        warManager.save();
        treatyManager.save();
        meetingManager.save();
        militaryManager.save();
        getLogger().info("GeoWarPlugin disabled. Data saved.");
    }

    public static GeoWarPlugin getInstance() { return instance; }

    public WarManager getWarManager() { return warManager; }
    public TreatyManager getTreatyManager() { return treatyManager; }
    public MeetingManager getMeetingManager() { return meetingManager; }
    public MilitaryManager getMilitaryManager() { return militaryManager; }
    public EconomyService getEconomy() { return economyService; }
    public DiscordWebhookService getDiscord() { return discordWebhookService; }
    public Map<UUID, String> getPendingActions() { return pendingActions; }
}
