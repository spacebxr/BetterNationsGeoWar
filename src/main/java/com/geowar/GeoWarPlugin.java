package com.geowar;

import com.geowar.capture.CaptureZoneApi;
import com.geowar.capture.CaptureZoneListener;
import com.geowar.capture.CaptureZoneManager;
import com.geowar.commands.CaptureZoneCommand;
import com.geowar.commands.NationGuiCommand;
import com.geowar.config.PluginConfig;
import com.geowar.data.MeetingManager;
import com.geowar.data.MilitaryManager;
import com.geowar.data.NationAccessManager;
import com.geowar.data.TreatyManager;
import com.geowar.data.WarManager;
import com.geowar.discord.DiscordWebhookService;
import com.geowar.economy.EconomyService;
import com.geowar.integration.TownyIntegration;
import com.geowar.listeners.ChatInputListener;
import com.geowar.listeners.GuiListener;
import com.geowar.service.economy.EconomyProvider;
import com.geowar.service.economy.InternalEconomyProvider;
import com.geowar.service.NationManager;
import com.geowar.service.TownManager;
import com.geowar.storage.AsyncExecutor;
import com.geowar.storage.repository.NationRepository;
import com.geowar.storage.repository.TownRepository;
import com.geowar.storage.Database;
import com.geowar.storage.SchemaInitializer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GeoWarPlugin extends JavaPlugin {
    private Database database;
    private AsyncExecutor asyncExecutor;
    private NationManager nationManager;
    private TownManager townManager;
    private CaptureZoneManager captureZoneManager;
    private CaptureZoneApi captureZoneApi;
    private WarManager warManager;
    private TreatyManager treatyManager;
    private MeetingManager meetingManager;
    private MilitaryManager militaryManager;
    private EconomyService economyService;
    private DiscordWebhookService discordWebhookService;
    private NationAccessManager accessManager;

    private final Map<UUID, String> pendingActions = new HashMap<>();

    @Override
    public void onEnable() {
        PluginConfig config = new PluginConfig(this);
        config.load();
        try {
            database = new Database(config, getDataFolder());
            database.connect();
            new SchemaInitializer(database).apply();
            asyncExecutor = new AsyncExecutor(this);
            nationManager = new NationManager(new NationRepository(database), asyncExecutor);
            townManager = new TownManager(new TownRepository(database), asyncExecutor);
        } catch (Exception exception) {
            getLogger().severe("Failed to initialize database: " + exception.getMessage());
            if (asyncExecutor != null) asyncExecutor.shutdown();
            if (database != null) database.close();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        warManager = new WarManager(this);
        treatyManager = new TreatyManager(this);
        meetingManager = new MeetingManager(this);
        militaryManager = new MilitaryManager(this);
        accessManager = new NationAccessManager(this);
        economyService = new EconomyService(config.useVault());
        discordWebhookService = new DiscordWebhookService();

        EconomyProvider economy = new InternalEconomyProvider(database);
        com.geowar.service.economy.EconomyService coreEconomy =
                new com.geowar.service.economy.EconomyService(nationManager, townManager, economy, config);
        getServer().getScheduler().runTaskTimer(this, coreEconomy::runCycle,
                config.economyIntervalTicks(), config.economyIntervalTicks());
        TownyIntegration towny = new TownyIntegration();
        captureZoneManager = new CaptureZoneManager(this, economy, towny.townResolver());
        captureZoneApi = new CaptureZoneApi(captureZoneManager);
        captureZoneManager.start(getConfig().getLong("capture.update-interval-ticks", 20));

        getServer().getPluginManager().registerEvents(new CaptureZoneListener(captureZoneManager), this);
        getServer().getPluginManager().registerEvents(new GuiListener(), this);
        getServer().getPluginManager().registerEvents(new ChatInputListener(), this);

        getCommand("nationgui").setExecutor(new NationGuiCommand());
        getCommand("capturezone").setExecutor(new CaptureZoneCommand(captureZoneManager));
        getCommand("oilrig").setExecutor(new CaptureZoneCommand(captureZoneManager));
        getCommand("koth").setExecutor(new CaptureZoneCommand(captureZoneManager));
        getLogger().info("GeoWarPlugin enabled with nation, diplomacy, economy, military, and capture systems.");
    }

    @Override
    public void onDisable() {
        if (warManager != null) warManager.save();
        if (treatyManager != null) treatyManager.save();
        if (meetingManager != null) meetingManager.save();
        if (militaryManager != null) militaryManager.save();
        if (captureZoneManager != null) captureZoneManager.stop();
        if (asyncExecutor != null) asyncExecutor.shutdown();
        if (database != null) database.close();
    }

    public static GeoWarPlugin getInstance() { return getPlugin(GeoWarPlugin.class); }
    public CaptureZoneApi getCaptureZoneApi() { return captureZoneApi; }
    public WarManager getWarManager() { return warManager; }
    public TreatyManager getTreatyManager() { return treatyManager; }
    public MeetingManager getMeetingManager() { return meetingManager; }
    public MilitaryManager getMilitaryManager() { return militaryManager; }
    public EconomyService getEconomy() { return economyService; }
    public DiscordWebhookService getDiscord() { return discordWebhookService; }
    public NationAccessManager getAccessManager() { return accessManager; }
    public Map<UUID, String> getPendingActions() { return pendingActions; }
}
