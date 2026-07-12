package com.geowar.config;

import com.geowar.model.role.NationPermission;
import com.geowar.model.role.NationRole;
import com.geowar.model.role.RolePermissionResolver;
import com.geowar.model.war.WarScoreCalculator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.logging.Level;

/**
 * Typed view over {@code config.yml}. Reads primitive settings once at load time
 * and builds the derived components (permission resolver, war score calculator)
 * so the rest of the plugin depends on this class rather than raw config keys.
 */
public final class PluginConfig {

    public enum StorageType { SQLITE, MYSQL }

    private final Plugin plugin;

    private StorageType storageType;
    private String mysqlHost;
    private int mysqlPort;
    private String mysqlDatabase;
    private String mysqlUser;
    private String mysqlPassword;

    private long economyIntervalTicks;
    private long warTickIntervalTicks;
    private int warPreparationSeconds;
    private double maxTaxRate;
    private double baseTownIncome;
    private double troopUpkeep;

    private boolean useVault;
    private boolean useTowny;
    private boolean usePlaceholderApi;
    private boolean announceToDiscord;

    private RolePermissionResolver permissionResolver;
    private WarScoreCalculator warScoreCalculator;

    public PluginConfig(Plugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.storageType = parseStorage(config.getString("storage.type", "SQLITE"));
        this.mysqlHost = config.getString("storage.mysql.host", "localhost");
        this.mysqlPort = config.getInt("storage.mysql.port", 3306);
        this.mysqlDatabase = config.getString("storage.mysql.database", "geowar");
        this.mysqlUser = config.getString("storage.mysql.user", "root");
        this.mysqlPassword = config.getString("storage.mysql.password", "");

        this.economyIntervalTicks = config.getLong("economy.interval-ticks", 20L * 60 * 10);
        this.maxTaxRate = config.getDouble("economy.max-tax-rate", 0.5);
        this.baseTownIncome = config.getDouble("economy.base-town-income", 250.0);
        this.troopUpkeep = config.getDouble("military.troop-upkeep", 5.0);

        this.warTickIntervalTicks = config.getLong("war.tick-interval-ticks", 20L * 60);
        this.warPreparationSeconds = config.getInt("war.preparation-seconds", 300);

        this.useVault = config.getBoolean("integrations.vault", true);
        this.useTowny = config.getBoolean("integrations.towny", true);
        this.usePlaceholderApi = config.getBoolean("integrations.placeholderapi", true);
        this.announceToDiscord = config.getBoolean("integrations.discordsrv", true);

        this.permissionResolver = buildPermissionResolver(config.getConfigurationSection("roles"));
        this.warScoreCalculator = buildWarScoreCalculator(config.getConfigurationSection("war.score-weights"));
    }

    private StorageType parseStorage(String raw) {
        try {
            return StorageType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Unknown storage type '" + raw + "', defaulting to SQLITE");
            return StorageType.SQLITE;
        }
    }

    private RolePermissionResolver buildPermissionResolver(ConfigurationSection section) {
        RolePermissionResolver resolver = new RolePermissionResolver();
        if (section == null) {
            return resolver;
        }
        for (NationRole role : NationRole.values()) {
            List<String> names = section.getStringList(role.name().toLowerCase() + ".permissions");
            if (names.isEmpty()) {
                continue;
            }
            java.util.EnumSet<NationPermission> granted = java.util.EnumSet.noneOf(NationPermission.class);
            for (String name : names) {
                try {
                    granted.add(NationPermission.valueOf(name.trim().toUpperCase()));
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().log(Level.WARNING,
                            "Ignoring unknown permission ''{0}'' for role {1}", new Object[]{name, role});
                }
            }
            resolver.override(role, granted);
        }
        return resolver;
    }

    private WarScoreCalculator buildWarScoreCalculator(ConfigurationSection section) {
        if (section == null) {
            return WarScoreCalculator.defaults();
        }
        return new WarScoreCalculator(
                section.getDouble("territory", 12.0),
                section.getDouble("battles", 6.0),
                section.getDouble("casualties", 0.08),
                section.getDouble("economic", 0.002));
    }

    public StorageType storageType() {
        return storageType;
    }

    public String mysqlHost() {
        return mysqlHost;
    }

    public int mysqlPort() {
        return mysqlPort;
    }

    public String mysqlDatabase() {
        return mysqlDatabase;
    }

    public String mysqlUser() {
        return mysqlUser;
    }

    public String mysqlPassword() {
        return mysqlPassword;
    }

    public long economyIntervalTicks() {
        return economyIntervalTicks;
    }

    public long warTickIntervalTicks() {
        return warTickIntervalTicks;
    }

    public int warPreparationSeconds() {
        return warPreparationSeconds;
    }

    public double maxTaxRate() {
        return maxTaxRate;
    }

    public double baseTownIncome() {
        return baseTownIncome;
    }

    public double troopUpkeep() {
        return troopUpkeep;
    }

    public boolean useVault() {
        return useVault;
    }

    public boolean useTowny() {
        return useTowny;
    }

    public boolean usePlaceholderApi() {
        return usePlaceholderApi;
    }

    public boolean announceToDiscord() {
        return announceToDiscord;
    }

    public RolePermissionResolver permissionResolver() {
        return permissionResolver;
    }

    public WarScoreCalculator warScoreCalculator() {
        return warScoreCalculator;
    }
}
