package com.geowar.commands;

import com.geowar.GeoWarPlugin;
import com.geowar.gui.DiplomacyGui;
import com.geowar.gui.EconomyGui;
import com.geowar.gui.GuiUtil;
import com.geowar.gui.MilitaryGui;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.geysermc.cumulus.form.SimpleForm;
import com.geowar.integration.FloodgateBridge;

public class NationGuiCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        openMainGui(player);
        return true;
    }

    public void openMainGui(Player player) {
        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null) {
            player.sendMessage(ChatColor.RED + "You are not registered in Towny.");
            return;
        }

        Nation nation = resident.getNationOrNull();
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "You are not part of a nation.");
            return;
        }

        boolean isKing = resident.isKing();
        boolean hasAccess = isKing || GeoWarPlugin.getInstance().getAccessManager().hasAccess(nation.getName(), player.getUniqueId());
        if (!hasAccess) {
            player.sendMessage(ChatColor.RED + "You do not have access to the Nation Management panel.");
            return;
        }

        boolean isBedrock = Bukkit.getPluginManager().getPlugin("floodgate") != null
            && FloodgateBridge.isFloodgatePlayer(player.getUniqueId());

        if (isBedrock) {
            openBedrockMain(player, resident);
        } else {
            openJavaMain(player, resident);
        }
    }

    public void openJavaMain(Player player, Resident resident) {
        Town town = resident.getTownOrNull();
        Nation nation = resident.getNationOrNull();

        String townLine = town != null
            ? ChatColor.GREEN + "Town: " + town.getName() + " (" + town.getNumTownBlocks() + " claims)"
            : ChatColor.RED + "No Town";
        String nationLine = nation != null
            ? ChatColor.GREEN + "Nation: " + nation.getName() + " (" + nation.getTowns().size() + " towns)"
            : ChatColor.RED + "No Nation";
        String nationBank = nation != null ? ChatColor.YELLOW + "Treasury: $" + String.format("%.2f", nation.getAccount().getHoldingBalance()) : ChatColor.GRAY + "Treasury: N/A";
        String rank = getRankLine(resident);

        Inventory gui = Bukkit.createInventory(null, 45, "Nation Management");
        for (int i = 0; i < 45; i++) gui.setItem(i, GuiUtil.createItem(Material.GRAY_STAINED_GLASS_PANE, " "));

        gui.setItem(4, GuiUtil.createItem(Material.PLAYER_HEAD,
            ChatColor.GOLD + player.getName(),
            townLine,
            nationLine,
            nationBank,
            rank
        ));

        gui.setItem(20, GuiUtil.createItem(Material.IRON_SWORD,
            ChatColor.RED + "Military Control",
            ChatColor.GRAY + "PVP, soldiers, and military ranks",
            ChatColor.DARK_GRAY + "Click to open"
        ));

        gui.setItem(22, GuiUtil.createItem(Material.GOLD_INGOT,
            ChatColor.YELLOW + "Economy & Taxes",
            ChatColor.GRAY + "Pay citizens, deposits, and taxes",
            ChatColor.DARK_GRAY + "Click to open"
        ));

        gui.setItem(24, GuiUtil.createItem(Material.PAPER,
            ChatColor.AQUA + "Diplomacy",
            ChatColor.GRAY + "Wars, treaties, and meetings",
            ChatColor.DARK_GRAY + "Click to open"
        ));

        if (resident.isKing()) {
            gui.setItem(31, GuiUtil.createItem(Material.SHIELD,
                ChatColor.LIGHT_PURPLE + "Manage Access",
                ChatColor.GRAY + "Control who can open this panel",
                ChatColor.DARK_GRAY + "Click to manage"
            ));
        }

        player.openInventory(gui);
    }

    private void openBedrockMain(Player player, Resident resident) {
        Town town = resident.getTownOrNull();
        Nation nation = resident.getNationOrNull();

        String townLine = town != null ? "Town: " + town.getName() + " (" + town.getNumTownBlocks() + " claims)" : "No Town";
        String nationLine = nation != null ? "Nation: " + nation.getName() + " (" + nation.getTowns().size() + " towns)" : "No Nation";
        String nationBank = nation != null ? "Treasury: $" + String.format("%.2f", nation.getAccount().getHoldingBalance()) : "Treasury: N/A";
        String rank = ChatColor.stripColor(getRankLine(resident));

        String content = townLine + "\n" + nationLine + "\n" + nationBank + "\n" + rank + "\n\nSelect an option:";

        SimpleForm.Builder form = SimpleForm.builder()
            .title("Nation Management")
            .content(content)
            .button("Military Control")
            .button("Economy & Taxes")
            .button("Diplomacy");

        if (resident.isKing()) {
            form.button("Manage Access");
        }

        form.validResultHandler((response) -> {
            int id = response.clickedButtonId();
            if (id == 0) MilitaryGui.openBedrockGui(player);
            else if (id == 1) EconomyGui.openBedrockGui(player);
            else if (id == 2) DiplomacyGui.openBedrockGui(player);
            else if (id == 3 && resident.isKing()) openBedrockAccessManager(player, resident);
        });

        FloodgateBridge.sendForm(player.getUniqueId(), form.build());
    }

    private void openBedrockAccessManager(Player player, Resident resident) {
        if (resident.getNationOrNull() == null) return;
        com.geowar.data.NationAccessManager am = GeoWarPlugin.getInstance().getAccessManager();
        java.util.List<com.geowar.data.NationAccessManager.AccessEntry> entries = am.getAccessList(resident.getNationOrNull().getName());

        StringBuilder content = new StringBuilder("Players with access:\n");
        if (entries.isEmpty()) content.append("None\n");
        else for (com.geowar.data.NationAccessManager.AccessEntry e : entries) content.append(e.name).append("\n");
        content.append("\nUse 'Add Player' to grant access.");

        org.geysermc.cumulus.form.SimpleForm.Builder form = org.geysermc.cumulus.form.SimpleForm.builder()
            .title("Manage Nation Access")
            .content(content.toString())
            .button("Add Player")
            .button("Revoke Player");

        form.validResultHandler((r) -> {
            if (r.clickedButtonId() == 0) {
                player.sendMessage(ChatColor.YELLOW + "Type the player name to grant access:");
                GeoWarPlugin.getInstance().getPendingActions().put(player.getUniqueId(), "access_add:");
            } else if (r.clickedButtonId() == 1) {
                openBedrockAccessRevoke(player, resident);
            }
        });

        FloodgateBridge.sendForm(player.getUniqueId(), form.build());
    }

    private void openBedrockAccessRevoke(Player player, Resident resident) {
        if (resident.getNationOrNull() == null) return;
        String nationName = resident.getNationOrNull().getName();
        com.geowar.data.NationAccessManager am = GeoWarPlugin.getInstance().getAccessManager();
        java.util.List<com.geowar.data.NationAccessManager.AccessEntry> entries = am.getAccessList(nationName);

        if (entries.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No players to revoke.");
            return;
        }

        org.geysermc.cumulus.form.SimpleForm.Builder form = org.geysermc.cumulus.form.SimpleForm.builder()
            .title("Revoke Access")
            .content("Select a player to remove:");
        for (com.geowar.data.NationAccessManager.AccessEntry e : entries) form.button(e.name);

        form.validResultHandler((r) -> {
            com.geowar.data.NationAccessManager.AccessEntry target = entries.get(r.clickedButtonId());
            am.revokeAccess(nationName, target.uuid);
            player.sendMessage(ChatColor.GREEN + "Revoked access for " + target.name + ".");
        });

        FloodgateBridge.sendForm(player.getUniqueId(), form.build());
    }

    private String getRankLine(Resident resident) {
        if (resident.isKing()) return ChatColor.LIGHT_PURPLE + "Rank: King";
        if (resident.isMayor()) return ChatColor.AQUA + "Rank: Mayor";
        return ChatColor.GRAY + "Rank: Citizen";
    }
}
