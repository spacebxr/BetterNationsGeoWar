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
import org.geysermc.floodgate.api.FloodgateApi;

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

        boolean isBedrock = Bukkit.getPluginManager().getPlugin("floodgate") != null
            && FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());

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

        form.validResultHandler((response) -> {
            int id = response.clickedButtonId();
            if (id == 0) MilitaryGui.openBedrockGui(player);
            else if (id == 1) EconomyGui.openBedrockGui(player);
            else if (id == 2) DiplomacyGui.openBedrockGui(player);
        });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form.build());
    }

    private String getRankLine(Resident resident) {
        if (resident.isKing()) return ChatColor.LIGHT_PURPLE + "Rank: King";
        if (resident.isMayor()) return ChatColor.AQUA + "Rank: Mayor";
        return ChatColor.GRAY + "Rank: Citizen";
    }
}
