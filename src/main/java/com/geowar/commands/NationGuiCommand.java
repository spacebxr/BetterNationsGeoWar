package com.geowar.commands;

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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
        
        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null) {
            player.sendMessage(ChatColor.RED + "You are not registered in Towny.");
            return true;
        }
        
        if (Bukkit.getPluginManager().getPlugin("floodgate") != null && FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
            openBedrockGui(player, resident);
        } else {
            openJavaGui(player, resident);
        }

        return true;
    }

    private void openJavaGui(Player player, Resident resident) {
        Inventory gui = Bukkit.createInventory(null, 27, "Nation Management");
        
        Town town = resident.getTownOrNull();
        Nation nation = resident.getNationOrNull();
        
        String townInfo = town != null ? ChatColor.GREEN + "Town: " + town.getName() + " (" + town.getNumTownBlocks() + " claims)" : ChatColor.RED + "No Town";
        String nationInfo = nation != null ? ChatColor.GREEN + "Nation: " + nation.getName() : ChatColor.RED + "No Nation";
        String rankInfo = getRankInfo(resident, town, nation);
        
        gui.setItem(11, createGuiItem(Material.DIAMOND_SWORD, ChatColor.AQUA + "Military Control", "Manage your military forces"));
        gui.setItem(13, createGuiItem(Material.GOLD_INGOT, ChatColor.YELLOW + "Economy & Taxes", "Manage town finances"));
        gui.setItem(15, createGuiItem(Material.PAPER, ChatColor.WHITE + "Diplomacy", "Treaties and War"));
        
        gui.setItem(4, createGuiItem(Material.PLAYER_HEAD, ChatColor.GOLD + "Your Profile", townInfo, nationInfo, rankInfo));

        player.openInventory(gui);
    }

    private void openBedrockGui(Player player, Resident resident) {
        Town town = resident.getTownOrNull();
        Nation nation = resident.getNationOrNull();
        
        String townInfo = town != null ? "Town: " + town.getName() + " (" + town.getNumTownBlocks() + " claims)" : "No Town";
        String nationInfo = nation != null ? "Nation: " + nation.getName() : "No Nation";
        String rankInfo = getRankInfo(resident, town, nation);
        
        String content = townInfo + "\n" + nationInfo + "\n" + rankInfo + "\n\nManage your nation's affairs.";
        
        SimpleForm.Builder form = SimpleForm.builder()
                .title("Nation Management")
                .content(content)
                .button("Military Control")
                .button("Economy & Taxes")
                .button("Diplomacy");

        form.validResultHandler((response) -> {
            int clickedButtonId = response.clickedButtonId();
            switch (clickedButtonId) {
                case 0:
                    player.sendMessage(ChatColor.AQUA + "Military Control selected.");
                    break;
                case 1:
                    player.sendMessage(ChatColor.YELLOW + "Economy & Taxes selected.");
                    break;
                case 2:
                    player.sendMessage(ChatColor.WHITE + "Diplomacy selected.");
                    break;
            }
        });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form.build());
    }
    
    private String getRankInfo(Resident resident, Town town, Nation nation) {
        if (nation != null && resident.isKing()) {
            return ChatColor.LIGHT_PURPLE + "Rank: King";
        } else if (town != null && resident.isMayor()) {
            return ChatColor.LIGHT_PURPLE + "Rank: Mayor";
        }
        return ChatColor.GRAY + "Rank: Citizen";
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(java.util.Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}
