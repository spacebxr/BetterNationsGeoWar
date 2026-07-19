package com.geowar.gui;

import com.geowar.GeoWarPlugin;
import com.geowar.data.MilitaryManager;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.geysermc.cumulus.form.SimpleForm;
import com.geowar.integration.FloodgateBridge;

import java.util.List;

public class MilitaryGui {

    public static void openJavaGui(Player player) {
        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null) {
            player.sendMessage(ChatColor.RED + "You are not a Towny resident.");
            return;
        }
        Nation nation = resident.getNationOrNull();
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "You are not part of a nation.");
            return;
        }

        boolean isKing = resident.isKing();
        boolean isMayor = resident.isMayor();

        Inventory gui = Bukkit.createInventory(null, 45, "Military Control");

        for (int i = 0; i < 45; i++) gui.setItem(i, GuiUtil.createItem(Material.GRAY_STAINED_GLASS_PANE, " "));

        gui.setItem(10, GuiUtil.createItem(Material.IRON_SWORD,
            ChatColor.RED + "Toggle Town PVP",
            ChatColor.GRAY + "Turn PVP on or off in your town",
            ChatColor.YELLOW + "Current status: " + (resident.isMayor() ? ChatColor.GREEN + "Mayor" : ChatColor.RED + "Not Mayor"),
            ChatColor.DARK_GRAY + "Click to toggle"
        ));

        gui.setItem(12, GuiUtil.createItem(Material.GOLDEN_SWORD,
            ChatColor.GOLD + "View Military Roster",
            ChatColor.GRAY + "See all ranked soldiers in your nation",
            ChatColor.DARK_GRAY + "Click to view"
        ));

        if (isKing || isMayor) {
            gui.setItem(14, GuiUtil.createItem(Material.NETHERITE_SWORD,
                ChatColor.AQUA + "Promote Soldier",
                ChatColor.GRAY + "Assign or promote a resident's military rank",
                ChatColor.DARK_GRAY + "Click to manage"
            ));
        }

        gui.setItem(16, GuiUtil.createItem(Material.BARRIER,
            ChatColor.DARK_RED + "Demote / Dismiss Soldier",
            ChatColor.GRAY + "Remove or lower a soldier's rank",
            ChatColor.DARK_GRAY + "Click to manage"
        ));

        gui.setItem(40, GuiUtil.createItem(Material.ARROW,
            ChatColor.WHITE + "Back",
            ChatColor.GRAY + "Return to main menu"
        ));

        player.openInventory(gui);
    }

    public static void openBedrockGui(Player player) {
        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null || resident.getNationOrNull() == null) {
            player.sendMessage(ChatColor.RED + "You need to be in a nation to use military controls.");
            return;
        }

        Nation nation = resident.getNationOrNull();
        MilitaryManager mm = GeoWarPlugin.getInstance().getMilitaryManager();
        List<MilitaryManager.MilitaryMember> roster = mm.getMembersOf(nation.getName());

        StringBuilder rosterText = new StringBuilder("Active Military Roster:\n");
        if (roster.isEmpty()) {
            rosterText.append("No soldiers assigned yet.\n");
        } else {
            for (MilitaryManager.MilitaryMember member : roster) {
                rosterText.append(member.rank).append(": ").append(member.name).append("\n");
            }
        }

        SimpleForm.Builder form = SimpleForm.builder()
            .title("Military Control")
            .content(rosterText + "\nManage your nation's military forces.")
            .button("Toggle Town PVP")
            .button("View Full Roster")
            .button("Promote Soldier")
            .button("Dismiss Soldier");

        form.validResultHandler((response) -> {
            int id = response.clickedButtonId();
            if (id == 0) {
                player.performCommand("town toggle pvp");
                player.sendMessage(ChatColor.YELLOW + "Toggled PvP in your town.");
            } else if (id == 1) {
                showRosterBedrock(player, nation.getName());
            } else if (id == 2) {
                player.sendMessage(ChatColor.YELLOW + "Soldier promotion requires Java client for input. Use /ngui.");
            } else if (id == 3) {
                player.sendMessage(ChatColor.YELLOW + "Soldier dismissal requires Java client for input. Use /ngui.");
            }
        });

        FloodgateBridge.sendForm(player.getUniqueId(), form.build());
    }

    private static void showRosterBedrock(Player player, String nationName) {
        MilitaryManager mm = GeoWarPlugin.getInstance().getMilitaryManager();
        List<MilitaryManager.MilitaryMember> roster = mm.getMembersOf(nationName);

        StringBuilder content = new StringBuilder("Military Roster of " + nationName + ":\n\n");
        if (roster.isEmpty()) {
            content.append("No soldiers have been assigned yet.");
        } else {
            for (MilitaryManager.MilitaryMember member : roster) {
                content.append("[").append(member.rank).append("] ").append(member.name).append("\n");
            }
        }

        SimpleForm.Builder form = SimpleForm.builder()
            .title("Military Roster - " + nationName)
            .content(content.toString())
            .button("Back");

        form.validResultHandler((r) -> openBedrockGui(player));
        FloodgateBridge.sendForm(player.getUniqueId(), form.build());
    }

    public static void handleClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;
        String itemName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());

        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null) return;
        Nation nation = resident.getNationOrNull();
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "You are not part of a nation.");
            return;
        }

        switch (itemName) {
            case "Toggle Town PVP":
                player.performCommand("town toggle pvp");
                player.sendMessage(ChatColor.YELLOW + "PvP toggled in your town.");
                player.closeInventory();
                break;

            case "View Military Roster":
                player.closeInventory();
                showRosterJava(player, nation);
                break;

            case "Promote Soldier":
                if (!resident.isKing() && !resident.isMayor()) {
                    player.sendMessage(ChatColor.RED + "Only Kings and Mayors can promote soldiers.");
                    return;
                }
                player.closeInventory();
                openPromoteGui(player, nation);
                break;

            case "Demote / Dismiss Soldier":
                if (!resident.isKing() && !resident.isMayor()) {
                    player.sendMessage(ChatColor.RED + "Only Kings and Mayors can dismiss soldiers.");
                    return;
                }
                player.closeInventory();
                openDismissGui(player, nation);
                break;

            case "Back":
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(GeoWarPlugin.getInstance(), () ->
                    new com.geowar.commands.NationGuiCommand().openMainGui(player), 1L);
                break;
        }
    }

    private static void showRosterJava(Player player, Nation nation) {
        MilitaryManager mm = GeoWarPlugin.getInstance().getMilitaryManager();
        List<MilitaryManager.MilitaryMember> roster = mm.getMembersOf(nation.getName());

        Inventory gui = Bukkit.createInventory(null, 54, "Roster: " + nation.getName());
        for (int i = 0; i < 54; i++) gui.setItem(i, GuiUtil.createItem(Material.GRAY_STAINED_GLASS_PANE, " "));

        if (roster.isEmpty()) {
            gui.setItem(22, GuiUtil.createItem(Material.BARRIER, ChatColor.RED + "No soldiers assigned", ChatColor.GRAY + "Promote residents in the Military menu."));
        } else {
            int slot = 10;
            for (MilitaryManager.MilitaryMember member : roster) {
                gui.setItem(slot, GuiUtil.createItem(Material.PLAYER_HEAD,
                    ChatColor.GOLD + member.name,
                    ChatColor.YELLOW + "Rank: " + member.rank
                ));
                slot++;
                if (slot >= 44) break;
            }
        }

        gui.setItem(49, GuiUtil.createItem(Material.ARROW, ChatColor.WHITE + "Back", ChatColor.GRAY + "Return to Military menu"));
        player.openInventory(gui);
    }

    private static void openPromoteGui(Player player, Nation nation) {
        Inventory gui = Bukkit.createInventory(null, 54, "Promote Soldier");
        for (int i = 0; i < 54; i++) gui.setItem(i, GuiUtil.createItem(Material.GRAY_STAINED_GLASS_PANE, " "));

        MilitaryManager mm = GeoWarPlugin.getInstance().getMilitaryManager();
        int slot = 10;
        for (Resident r : nation.getResidents()) {
            String currentRank = mm.getMilitaryRank(r.getUUID());
            gui.setItem(slot, GuiUtil.createItem(Material.PLAYER_HEAD,
                ChatColor.GREEN + r.getName(),
                ChatColor.GRAY + "Current rank: " + currentRank,
                ChatColor.DARK_GRAY + "Click to cycle rank"
            ));
            slot++;
            if (slot >= 44) break;
        }

        gui.setItem(49, GuiUtil.createItem(Material.ARROW, ChatColor.WHITE + "Back", ChatColor.GRAY + "Return to Military menu"));
        player.openInventory(gui);
    }

    private static void openDismissGui(Player player, Nation nation) {
        Inventory gui = Bukkit.createInventory(null, 54, "Dismiss Soldier");
        for (int i = 0; i < 54; i++) gui.setItem(i, GuiUtil.createItem(Material.GRAY_STAINED_GLASS_PANE, " "));

        MilitaryManager mm = GeoWarPlugin.getInstance().getMilitaryManager();
        int slot = 10;
        for (MilitaryManager.MilitaryMember member : mm.getMembersOf(nation.getName())) {
            gui.setItem(slot, GuiUtil.createItem(Material.PLAYER_HEAD,
                ChatColor.RED + member.name,
                ChatColor.GRAY + "Rank: " + member.rank,
                ChatColor.DARK_GRAY + "Click to dismiss"
            ));
            slot++;
            if (slot >= 44) break;
        }

        gui.setItem(49, GuiUtil.createItem(Material.ARROW, ChatColor.WHITE + "Back", ChatColor.GRAY + "Return to Military menu"));
        player.openInventory(gui);
    }
}
