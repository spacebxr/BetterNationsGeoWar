package com.geowar.listeners;

import com.geowar.GeoWarPlugin;
import com.geowar.gui.EconomyGui;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Map;
import java.util.UUID;

public class ChatInputListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Map<UUID, String> pending = GeoWarPlugin.getInstance().getPendingActions();

        String action = pending.get(uuid);
        if (action == null) return;

        if (!action.startsWith("pay_") && !action.startsWith("deposit_") && !action.startsWith("set_") && !action.startsWith("pay_all")) return;

        event.setCancelled(true);
        String input = event.getMessage().trim();

        pending.remove(uuid);

        switch (action) {
            case "pay_citizen_name":
                pending.put(uuid, "pay_citizen_amount:" + input);
                player.sendMessage(ChatColor.YELLOW + "Enter the amount to pay " + input + ":");
                break;

            case "deposit_nation":
                GeoWarPlugin.getInstance().getServer().getScheduler().runTask(
                    GeoWarPlugin.getInstance(),
                    () -> EconomyGui.depositToBank(player, input, "nation")
                );
                break;

            case "deposit_town":
                GeoWarPlugin.getInstance().getServer().getScheduler().runTask(
                    GeoWarPlugin.getInstance(),
                    () -> EconomyGui.depositToBank(player, input, "town")
                );
                break;

            case "set_town_tax":
                try {
                    int amount = Integer.parseInt(input);
                    GeoWarPlugin.getInstance().getServer().getScheduler().runTask(
                        GeoWarPlugin.getInstance(),
                        () -> player.performCommand("town set taxes " + amount)
                    );
                    player.sendMessage(ChatColor.GREEN + "Town tax set to " + amount + ".");
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid amount.");
                }
                break;

            case "set_nation_tax":
                try {
                    int amount = Integer.parseInt(input);
                    GeoWarPlugin.getInstance().getServer().getScheduler().runTask(
                        GeoWarPlugin.getInstance(),
                        () -> player.performCommand("nation set taxes " + amount)
                    );
                    player.sendMessage(ChatColor.GREEN + "Nation tax set to " + amount + ".");
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid amount.");
                }
                break;

            case "pay_all_citizens":
                GeoWarPlugin.getInstance().getServer().getScheduler().runTask(
                    GeoWarPlugin.getInstance(),
                    () -> EconomyGui.executePayAllCitizens(player, input)
                );
                break;

            default:
                if (action.startsWith("pay_citizen_amount:")) {
                    String targetName = action.replace("pay_citizen_amount:", "");
                    GeoWarPlugin.getInstance().getServer().getScheduler().runTask(
                        GeoWarPlugin.getInstance(),
                        () -> EconomyGui.payCitizen(player, targetName, input)
                    );
                }
                break;
        }
    }
}
