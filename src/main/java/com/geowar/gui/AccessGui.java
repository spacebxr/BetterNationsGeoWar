package com.geowar.gui;

import com.geowar.GeoWarPlugin;
import com.geowar.data.NationAccessManager;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

public class AccessGui {

    public static void openJavaGui(Player player) {
        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null || !resident.isKing() || resident.getNationOrNull() == null) {
            player.sendMessage(ChatColor.RED + "Only the King can manage nation access.");
            return;
        }
        Nation nation = resident.getNationOrNull();

        NationAccessManager am = GeoWarPlugin.getInstance().getAccessManager();
        List<NationAccessManager.AccessEntry> entries = am.getAccessList(nation.getName());

        Inventory gui = Bukkit.createInventory(null, 54, "Manage Nation Access");
        for (int i = 0; i < 54; i++) gui.setItem(i, GuiUtil.createItem(Material.GRAY_STAINED_GLASS_PANE, " "));

        gui.setItem(4, GuiUtil.createItem(Material.SHIELD,
            ChatColor.GOLD + "Nation Access Control",
            ChatColor.GRAY + "Nation: " + nation.getName(),
            ChatColor.GRAY + "Allowed players: " + entries.size(),
            ChatColor.DARK_GRAY + "King always has access"
        ));

        gui.setItem(48, GuiUtil.createItem(Material.EMERALD,
            ChatColor.GREEN + "Add Player",
            ChatColor.GRAY + "Grant a player access to this GUI",
            ChatColor.DARK_GRAY + "Click and type their name"
        ));

        gui.setItem(49, GuiUtil.createItem(Material.ARROW,
            ChatColor.WHITE + "Back",
            ChatColor.GRAY + "Return to main menu"
        ));

        if (entries.isEmpty()) {
            gui.setItem(22, GuiUtil.createItem(Material.BARRIER,
                ChatColor.RED + "No players granted access",
                ChatColor.GRAY + "Click 'Add Player' to grant access."
            ));
        } else {
            int slot = 10;
            for (NationAccessManager.AccessEntry entry : entries) {
                gui.setItem(slot, GuiUtil.createItem(Material.PLAYER_HEAD,
                    ChatColor.AQUA + entry.name,
                    ChatColor.GRAY + "UUID: " + entry.uuid.toString().substring(0, 8) + "...",
                    ChatColor.RED + "Click to revoke access"
                ));
                slot++;
                if (slot >= 44) break;
            }
        }

        player.openInventory(gui);
    }
}
