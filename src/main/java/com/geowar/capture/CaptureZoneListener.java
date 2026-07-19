package com.geowar.capture;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public final class CaptureZoneListener implements Listener {
    private final CaptureZoneManager manager;
    public CaptureZoneListener(CaptureZoneManager manager) { this.manager = manager; }

    @EventHandler(ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof Entity entity && manager.all().stream().anyMatch(zone -> zone.contains(entity.getLocation()))) {
            event.setCancelled(true);
        }
    }
}
