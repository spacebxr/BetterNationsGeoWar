package com.geowar;

import com.geowar.commands.NationGuiCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class GeoWarPlugin extends JavaPlugin {
    
    private static GeoWarPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("GeoWarPlugin has been enabled!");
        
        getCommand("nationgui").setExecutor(new NationGuiCommand());
    }

    @Override
    public void onDisable() {
        getLogger().info("GeoWarPlugin has been disabled.");
    }
    
    public static GeoWarPlugin getInstance() {
        return instance;
    }
}
