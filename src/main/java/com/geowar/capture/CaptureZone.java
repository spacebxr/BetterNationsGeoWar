package com.geowar.capture;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;
import java.util.UUID;

public final class CaptureZone {
    private final String id;
    private final String name;
    private final CaptureZoneType type;
    private Location center;
    private double radius;
    private int captureSeconds;
    private String ownerTown;
    private String capturingTown;
    private double progress;
    private long cooldownUntil;
    private double rewardMoney;
    private int rewardOil;

    public CaptureZone(String id, String name, CaptureZoneType type, Location center, double radius, int captureSeconds) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.center = center.clone();
        this.radius = radius;
        this.captureSeconds = captureSeconds;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public CaptureZoneType getType() { return type; }
    public Location getCenter() { return center.clone(); }
    public void setCenter(Location center) { this.center = center.clone(); }
    public double getRadius() { return radius; }
    public void setRadius(double radius) { this.radius = Math.max(1, radius); }
    public int getCaptureSeconds() { return captureSeconds; }
    public void setCaptureSeconds(int seconds) { this.captureSeconds = Math.max(1, seconds); }
    public String getOwner() { return ownerTown; }
    public void setOwner(String ownerTown) { this.ownerTown = normalize(ownerTown); }
    public String getCapturingTown() { return capturingTown; }
    public double getProgress() { return progress; }
    public long getCooldownUntil() { return cooldownUntil; }
    public void setCooldownUntil(long cooldownUntil) { this.cooldownUntil = cooldownUntil; }
    public double getRewardMoney() { return rewardMoney; }
    public void setRewardMoney(double amount) { rewardMoney = Math.max(0, amount); }
    public int getRewardOil() { return rewardOil; }
    public void setRewardOil(int amount) { rewardOil = Math.max(0, amount); }

    public boolean contains(Location location) {
        World world = center.getWorld();
        return world != null && location.getWorld() != null && world.getUID().equals(location.getWorld().getUID())
                && center.distanceSquared(location) <= radius * radius;
    }

    public boolean isCoolingDown() { return cooldownUntil > System.currentTimeMillis(); }

    public CaptureUpdate update(java.util.Map<String, Integer> townCounts, double seconds) {
        if (townCounts.isEmpty() || isCoolingDown()) {
            capturingTown = null;
            return new CaptureUpdate(false, false, false);
        }
        if (townCounts.size() != 1) {
            return new CaptureUpdate(false, true, false);
        }
        String town = townCounts.keySet().iterator().next();
        if (Objects.equals(ownerTown, town)) {
            capturingTown = null;
            progress = 0;
            return new CaptureUpdate(false, false, false);
        }
        boolean started = capturingTown == null;
        if (!Objects.equals(capturingTown, town)) {
            capturingTown = town;
            progress = 0;
            started = true;
        }
        progress = Math.min(1, progress + seconds / captureSeconds);
        if (progress >= 1) {
            String previous = ownerTown;
            ownerTown = town;
            capturingTown = null;
            progress = 0;
            cooldownUntil = System.currentTimeMillis() + 30_000L;
            return new CaptureUpdate(started, false, true, previous);
        }
        return new CaptureUpdate(started, false, false);
    }

    private static String normalize(String town) { return town == null || town.isBlank() ? null : town; }

    public record CaptureUpdate(boolean started, boolean contested, boolean captured, String previousOwner) {
        public CaptureUpdate(boolean started, boolean contested, boolean captured) {
            this(started, contested, captured, null);
        }
    }
}
