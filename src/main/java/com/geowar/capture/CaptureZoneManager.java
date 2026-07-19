package com.geowar.capture;

import com.geowar.service.economy.EconomyProvider;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class CaptureZoneManager {
    private final JavaPlugin plugin;
    private final EconomyProvider economy;
    private final Function<Player, String> townResolver;
    private final Map<String, CaptureZone> zones = new ConcurrentHashMap<>();
    private final Map<String, BossBar> bars = new ConcurrentHashMap<>();
    private BukkitTask task;

    public CaptureZoneManager(JavaPlugin plugin, EconomyProvider economy, Function<Player, String> townResolver) {
        this.plugin = plugin;
        this.economy = economy;
        this.townResolver = townResolver;
    }

    public void start(long intervalTicks) {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (task != null) task.cancel();
        bars.values().forEach(BossBar::removeAll);
        bars.clear();
    }

    public void register(CaptureZone zone) { zones.put(zone.getId(), zone); }
    public Optional<CaptureZone> get(String id) { return Optional.ofNullable(zones.get(id)); }
    public Collection<CaptureZone> all() { return List.copyOf(zones.values()); }
    public void delete(String id) { Optional.ofNullable(zones.remove(id)).ifPresent(zone -> Optional.ofNullable(bars.remove(id)).ifPresent(BossBar::removeAll)); }

    public boolean startCapture(CaptureZone zone, Player player) {
        String town = townResolver.apply(player);
        return town != null && zone.contains(player.getLocation()) && !Objects.equals(zone.getOwner(), town);
    }

    public void stopCapture(CaptureZone zone) {
        zone.update(Map.of(), 0);
    }

    private void tick() {
        double seconds = 1.0;
        for (CaptureZone zone : zones.values()) {
            Map<String, Integer> towns = new HashMap<>();
            for (Entity entity : nearby(zone)) {
                if (!(entity instanceof Player player) || !player.hasPermission("geowar.capturezone.capture")) continue;
                String town = townResolver.apply(player);
                if (town != null) towns.merge(town, 1, Integer::sum);
            }
            CaptureZone.CaptureUpdate update = zone.update(towns, seconds);
            notifyPlayers(zone, towns.keySet(), update);
            if (update.captured()) {
                Bukkit.getPluginManager().callEvent(new ZoneCapturedEvent(zone, first(towns.keySet())));
            }
        }
    }

    private List<Entity> nearby(CaptureZone zone) {
        Location center = zone.getCenter();
        return new ArrayList<>(center.getWorld().getNearbyEntities(center, zone.getRadius(), zone.getRadius(), zone.getRadius()));
    }

    private void notifyPlayers(CaptureZone zone, Collection<String> towns, CaptureZone.CaptureUpdate update) {
        if (update.started()) Bukkit.broadcastMessage("§e" + first(towns) + " started capturing " + zone.getName());
        if (update.contested()) Bukkit.broadcastMessage("§c" + zone.getName() + " is being contested");
        if (update.captured()) {
            Bukkit.broadcastMessage("§a" + first(towns) + " captured " + zone.getName());
            if (update.previousOwner() != null) Bukkit.broadcastMessage("§c" + update.previousOwner() + " lost control of " + zone.getName());
            payReward(zone, first(towns));
            Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1));
        }
        Location center = zone.getCenter();
        for (Entity entity : nearby(zone)) {
            if (!(entity instanceof Player player)) continue;
            player.sendActionBar("§e" + zone.getName() + " §7" + Math.round(zone.getProgress() * 100) + "% §f" + (zone.getCapturingTown() == null ? "" : zone.getCapturingTown()));
            player.spawnParticle(Particle.END_ROD, center.clone().add(0, 1, 0), 2, zone.getRadius() / 4, 0.3, zone.getRadius() / 4, 0);
            BossBar bar = bars.computeIfAbsent(zone.getId(), key -> Bukkit.createBossBar(zone.getName(), BarColor.YELLOW, BarStyle.SOLID));
            bar.setProgress(Math.max(0, Math.min(1, zone.getProgress())));
            bar.addPlayer(player);
        }
        removeDistantBarPlayers(zone);
    }

    private void removeDistantBarPlayers(CaptureZone zone) {
        BossBar bar = bars.get(zone.getId());
        if (bar == null) return;
        for (Player player : new ArrayList<>(bar.getPlayers())) {
            if (!zone.contains(player.getLocation())) bar.removePlayer(player);
        }
    }

    private void payReward(CaptureZone zone, String town) {
        if (zone.getRewardMoney() <= 0) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (Objects.equals(townResolver.apply(player), town)) economy.deposit(player.getUniqueId(), zone.getRewardMoney());
        }
    }

    private String first(Collection<String> towns) { return towns.stream().findFirst().orElse("Unknown"); }

    public interface CaptureZoneEvent {
        HandlerList getHandlers();
    }

    public static final class ZoneCapturedEvent extends Event {
        private static final HandlerList HANDLERS = new HandlerList();
        private final CaptureZone zone;
        private final String town;
        public ZoneCapturedEvent(CaptureZone zone, String town) { this.zone = zone; this.town = town; }
        public CaptureZone getZone() { return zone; }
        public String getTown() { return town; }
        public HandlerList getHandlers() { return HANDLERS; }
        public static HandlerList getHandlerList() { return HANDLERS; }
    }
}
