package com.geowar.listeners;

import com.geowar.GeoWarPlugin;
import com.geowar.data.MilitaryManager;
import com.geowar.data.TreatyManager;
import com.geowar.data.WarManager;
import com.geowar.gui.DiplomacyGui;
import com.geowar.gui.EconomyGui;
import com.geowar.gui.GuiUtil;
import com.geowar.gui.MilitaryGui;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class GuiListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (title.equals("Nation Management")) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item == null || !item.hasItemMeta()) return;
            String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            if (name.equals("Military Control")) {
                MilitaryGui.openJavaGui(player);
            } else if (name.equals("Economy & Taxes")) {
                EconomyGui.openJavaGui(player);
            } else if (name.equals("Diplomacy")) {
                DiplomacyGui.openJavaGui(player);
            }
            return;
        }

        if (title.equals("Military Control") || title.startsWith("Roster:") || title.equals("Promote Soldier") || title.equals("Dismiss Soldier")) {
            event.setCancelled(true);
            handleMilitarySubScreens(event, player, title);
            return;
        }

        if (title.equals("Economy & Taxes")) {
            event.setCancelled(true);
            EconomyGui.handleClick(event);
            return;
        }

        if (title.equals("Diplomacy") || title.equals("Select Nation") || title.equals("Propose Peace")
            || title.equals("Active Treaties") || title.equals("Meetings") || title.equals("Active Wars")) {
            event.setCancelled(true);
            handleDiplomacySubScreens(event, player, title);
            return;
        }
    }

    private void handleMilitarySubScreens(InventoryClickEvent event, Player player, String title) {
        if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;
        String itemName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());

        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null) return;
        Nation nation = resident.getNationOrNull();
        if (nation == null) return;

        if (title.startsWith("Roster:") || title.equals("Active Wars")) {
            if (itemName.equals("Back")) {
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(GeoWarPlugin.getInstance(), () -> MilitaryGui.openJavaGui(player), 1L);
            }
            return;
        }

        if (title.equals("Promote Soldier")) {
            if (itemName.equals("Back")) {
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(GeoWarPlugin.getInstance(), () -> MilitaryGui.openJavaGui(player), 1L);
                return;
            }
            String playerName = ChatColor.stripColor(itemName);
            Resident target = TownyAPI.getInstance().getResident(playerName);
            if (target == null) return;
            MilitaryManager mm = GeoWarPlugin.getInstance().getMilitaryManager();
            String currentRank = mm.getMilitaryRank(target.getUUID());
            String newRank = cycleRank(currentRank);
            mm.setMilitaryRank(target.getUUID(), newRank);
            player.sendMessage(ChatColor.GREEN + playerName + " has been set to rank: " + newRank);
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(GeoWarPlugin.getInstance(), () -> MilitaryGui.openJavaGui(player), 1L);
            return;
        }

        if (title.equals("Dismiss Soldier")) {
            if (itemName.equals("Back")) {
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(GeoWarPlugin.getInstance(), () -> MilitaryGui.openJavaGui(player), 1L);
                return;
            }
            String playerName = ChatColor.stripColor(itemName);
            Resident target = TownyAPI.getInstance().getResident(playerName);
            if (target == null) return;
            GeoWarPlugin.getInstance().getMilitaryManager().removeMilitaryRank(target.getUUID());
            player.sendMessage(ChatColor.RED + playerName + " has been dismissed from the military.");
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(GeoWarPlugin.getInstance(), () -> MilitaryGui.openJavaGui(player), 1L);
            return;
        }

        MilitaryGui.handleClick(event);
    }

    private void handleDiplomacySubScreens(InventoryClickEvent event, Player player, String title) {
        if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;
        String itemName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());

        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null) return;
        Nation myNation = resident.getNationOrNull();
        if (myNation == null) return;

        if (title.equals("Active Treaties") || title.equals("Meetings") || title.equals("Active Wars")) {
            if (itemName.equals("Back")) {
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(GeoWarPlugin.getInstance(), () -> DiplomacyGui.openJavaGui(player), 1L);
            }
            return;
        }

        if (title.equals("Select Nation")) {
            String pendingAction = GeoWarPlugin.getInstance().getPendingActions().get(player.getUniqueId());
            if (pendingAction == null) return;

            if (itemName.equals("Back")) {
                GeoWarPlugin.getInstance().getPendingActions().remove(player.getUniqueId());
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(GeoWarPlugin.getInstance(), () -> DiplomacyGui.openJavaGui(player), 1L);
                return;
            }

            String targetNationName = ChatColor.stripColor(itemName);
            Nation targetNation = TownyAPI.getInstance().getNation(targetNationName);
            if (targetNation == null) return;

            String[] parts = pendingAction.split(":", 2);
            String action = parts[0];

            GeoWarPlugin.getInstance().getPendingActions().remove(player.getUniqueId());
            player.closeInventory();

            if (action.equals("declare_war")) {
                WarManager wm = GeoWarPlugin.getInstance().getWarManager();
                if (wm.isAtWar(myNation.getName(), targetNationName)) {
                    player.sendMessage(ChatColor.RED + "You are already at war with " + targetNationName + ".");
                    return;
                }
                wm.declareWar(myNation.getName(), targetNationName, "No reason stated", "None");
                DiplomacyGui.broadcastWar(myNation.getName(), targetNationName, "No reason stated", "None");
                player.sendMessage(ChatColor.YELLOW + "Use /ngui and Diplomacy to view the war record.");

            } else if (action.equals("log_treaty")) {
                GeoWarPlugin.getInstance().getTreatyManager().logTreaty(myNation.getName(), targetNationName, "Alliance", "Mutual cooperation");
                Bukkit.broadcastMessage(ChatColor.GOLD + "[GeoWar] " + myNation.getName() + " and " + targetNationName + " have formed an Alliance.");

            } else if (action.equals("request_meeting")) {
                GeoWarPlugin.getInstance().getMeetingManager().requestMeeting(myNation.getName(), player.getName(), targetNationName, "Diplomatic meeting");
                player.sendMessage(ChatColor.GREEN + "Meeting request sent to " + targetNationName + ".");
            }
            return;
        }

        if (title.equals("Propose Peace")) {
            if (itemName.equals("Back")) {
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(GeoWarPlugin.getInstance(), () -> DiplomacyGui.openJavaGui(player), 1L);
                return;
            }
            if (itemName.startsWith("Peace with ")) {
                String opponent = itemName.replace("Peace with ", "");
                GeoWarPlugin.getInstance().getWarManager().endWar(myNation.getName(), opponent);
                Bukkit.broadcastMessage(ChatColor.GREEN + "[GeoWar] " + myNation.getName() + " and " + opponent + " have ended their war.");
                player.closeInventory();
            }
            return;
        }

        if (title.equals("Meetings")) {
            if (itemName.startsWith("From: ")) {
                String fromNation = itemName.replace("From: ", "");
                GeoWarPlugin.getInstance().getMeetingManager().acceptMeeting(fromNation, myNation.getName());
                player.sendMessage(ChatColor.GREEN + "Meeting with " + fromNation + " accepted.");
                Bukkit.broadcastMessage(ChatColor.GOLD + "[GeoWar] " + myNation.getName() + " has accepted a meeting with " + fromNation + ".");
                player.closeInventory();
                return;
            }
            if (itemName.equals("Back")) {
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(GeoWarPlugin.getInstance(), () -> DiplomacyGui.openJavaGui(player), 1L);
                return;
            }
        }

        DiplomacyGui.handleClick(event);
    }

    private String cycleRank(String currentRank) {
        switch (currentRank) {
            case "Civilian": return "Soldier";
            case "Soldier": return "Sergeant";
            case "Sergeant": return "Captain";
            case "Captain": return "General";
            case "General": return "Civilian";
            default: return "Soldier";
        }
    }
}
