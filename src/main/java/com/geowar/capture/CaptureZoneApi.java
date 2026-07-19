package com.geowar.capture;

import org.bukkit.Location;

public final class CaptureZoneApi {
    private final CaptureZoneManager manager;
    public CaptureZoneApi(CaptureZoneManager manager) { this.manager = manager; }
    public CaptureZone get(String id) { return manager.get(id).orElse(null); }
    public CaptureZone create(String id, String name, CaptureZoneType type, Location location, double radius, int seconds) {
        CaptureZone zone = new CaptureZone(id, name, type, location, radius, seconds);
        manager.register(zone);
        return zone;
    }
    public void delete(String id) { manager.delete(id); }
}
