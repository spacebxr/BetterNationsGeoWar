package com.geowar.listeners;

import com.geowar.GeoWarPlugin;
import com.geowar.gui.EconomyGui;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
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

        if (!action.startsWith("pay_") && !action.startsWith("deposit_") && !action.startsWith("set_")
            && !action.startsWith("pay_all") && !action.startsWith("war_") && !action.startsWith("access_")) return;

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

            case "access_add:":
                handleAccessAdd(player, input);
                break;

            default:
                if (action.startsWith("pay_citizen_amount:")) {
                    String targetName = action.replace("pay_citizen_amount:", "");
                    GeoWarPlugin.getInstance().getServer().getScheduler().runTask(
                        GeoWarPlugin.getInstance(),
                        () -> EconomyGui.payCitizen(player, targetName, input)
                    );
                } else if (action.startsWith("war_reason:")) {
                    String targetNation = action.replace("war_reason:", "");
                    com.palmergames.bukkit.towny.object.Resident res = com.palmergames.bukkit.towny.TownyAPI.getInstance().getResident(player);
                    if (res == null || res.getNationOrNull() == null || !res.isKing()) {
                        player.sendMessage(ChatColor.RED + "Only the King can declare war.");
                        break;
                    }
                    pending.put(uuid, "war_demands:" + targetNation + ":" + input);
                    player.sendMessage(ChatColor.YELLOW + "Now, type your demands for " + targetNation + " (or type 'None'):");
                } else if (action.startsWith("war_demands:")) {
                    String[] parts = action.replace("war_demands:", "").split(":", 2);
                    if (parts.length == 2) {
                        String targetNation = parts[0];
                        String reason = parts[1];
                        GeoWarPlugin.getInstance().getServer().getScheduler().runTask(
                            GeoWarPlugin.getInstance(),
                            () -> {
                                com.palmergames.bukkit.towny.object.Resident resident = com.palmergames.bukkit.towny.TownyAPI.getInstance().getResident(player);
                                if (resident == null || resident.getNationOrNull() == null) return;
                                if (!resident.isKing()) {
                                    player.sendMessage(ChatColor.RED + "Only the King can declare war.");
                                    return;
                                }
                                String myNation = resident.getNationOrNull().getName();
                                GeoWarPlugin.getInstance().getWarManager().declareWar(myNation, targetNation, reason, input);
                                com.geowar.gui.DiplomacyGui.broadcastWar(myNation, targetNation, reason, input, player.getName());
                                player.sendMessage(ChatColor.GREEN + "War declared successfully. Use /ngui and Diplomacy to view it.");
                            }
                        );
                    }
                }
                break;
        }
    }

    private void handleAccessAdd(Player player, String input) {
        GeoWarPlugin.getInstance().getServer().getScheduler().runTask(GeoWarPlugin.getInstance(), () -> {
            com.palmergames.bukkit.towny.object.Resident resident = com.palmergames.bukkit.towny.TownyAPI.getInstance().getResident(player);
            if (resident == null || !resident.isKing() || resident.getNationOrNull() == null) {
                player.sendMessage(ChatColor.RED + "Only the King can manage access.");
                return;
            }
            String nationName = resident.getNationOrNull().getName();

            OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(input);
            if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
                player.sendMessage(ChatColor.RED + "Player '" + input + "' has never joined this server.");
                return;
            }

            GeoWarPlugin.getInstance().getAccessManager().grantAccess(nationName, target.getUniqueId(), target.getName());
            player.sendMessage(ChatColor.GREEN + target.getName() + " can now access the Nation Management panel.");

            if (target.isOnline() && target.getPlayer() != null) {
                target.getPlayer().sendMessage(ChatColor.GREEN + "You have been granted access to the "
                    + nationName + " Nation Management panel by " + player.getName() + ".");
            }

            com.geowar.gui.AccessGui.openJavaGui(player);
        });
    }
}
