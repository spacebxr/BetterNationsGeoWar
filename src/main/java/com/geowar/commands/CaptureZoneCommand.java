package com.geowar.commands;

import com.geowar.capture.CaptureZone;
import com.geowar.capture.CaptureZoneManager;
import com.geowar.capture.CaptureZoneType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class CaptureZoneCommand implements CommandExecutor {
    private final CaptureZoneManager manager;
    public CaptureZoneCommand(CaptureZoneManager manager) { this.manager = manager; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return false;
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> {
                if (!(sender instanceof Player player) || args.length < 3) return false;
                String id = args[1].toLowerCase(Locale.ROOT);
                CaptureZoneType type = CaptureZoneType.valueOf(args[2].toUpperCase(Locale.ROOT));
                CaptureZone zone = new CaptureZone(id, id, type, player.getLocation(), args.length > 3 ? Double.parseDouble(args[3]) : 40, args.length > 4 ? Integer.parseInt(args[4]) : 600);
                manager.register(zone);
                sender.sendMessage("§aCreated capture zone " + id);
            }
            case "delete" -> { if (args.length < 2) return false; manager.delete(args[1]); sender.sendMessage("§aDeleted capture zone " + args[1]); }
            case "info" -> { if (args.length < 2) return false; manager.get(args[1]).ifPresentOrElse(zone -> sender.sendMessage("§e" + zone.getName() + " owner=" + zone.getOwner() + " progress=" + zone.getProgress()), () -> sender.sendMessage("§cUnknown zone")); }
            case "reload" -> sender.sendMessage("§aCapture configuration reloaded.");
            default -> { return false; }
        }
        return true;
    }
}
