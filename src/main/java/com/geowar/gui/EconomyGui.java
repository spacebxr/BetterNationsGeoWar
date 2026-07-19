package com.geowar.gui;

import com.geowar.GeoWarPlugin;
import com.geowar.economy.EconomyService;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import com.geowar.integration.FloodgateBridge;

public class EconomyGui {

    public static void openJavaGui(Player player) {
        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null) {
            player.sendMessage(ChatColor.RED + "You are not a Towny resident.");
            return;
        }

        EconomyService eco = GeoWarPlugin.getInstance().getEconomy();
        Nation nation = resident.getNationOrNull();
        Town town = resident.getTownOrNull();

        String playerBalance = eco.isAvailable()
            ? eco.format(eco.getBalance(player))
            : ChatColor.RED + "Vault unavailable";
        String nationBank = nation != null
            ? eco.format(nation.getAccount().getHoldingBalance())
            : "N/A";
        String townBank = town != null
            ? eco.format(town.getAccount().getHoldingBalance())
            : "N/A";

        Inventory gui = Bukkit.createInventory(null, 45, "Economy & Taxes");
        for (int i = 0; i < 45; i++) gui.setItem(i, GuiUtil.createItem(Material.GRAY_STAINED_GLASS_PANE, " "));

        gui.setItem(4, GuiUtil.createItem(Material.GOLD_BLOCK,
            ChatColor.GOLD + "Balances",
            ChatColor.AQUA + "Your Balance: " + playerBalance,
            ChatColor.GREEN + "Nation Bank: " + nationBank,
            ChatColor.YELLOW + "Town Bank: " + townBank
        ));

        if (eco.isAvailable()) {
            gui.setItem(19, GuiUtil.createItem(Material.EMERALD,
                ChatColor.GREEN + "Pay Citizen",
                ChatColor.GRAY + "Send money from your wallet to a resident",
                ChatColor.DARK_GRAY + "Click to open"
            ));

            gui.setItem(21, GuiUtil.createItem(Material.GOLD_INGOT,
                ChatColor.YELLOW + "Deposit to Nation Bank",
                ChatColor.GRAY + "Transfer from your wallet to the nation treasury",
                ChatColor.DARK_GRAY + "Click to deposit"
            ));

            gui.setItem(23, GuiUtil.createItem(Material.IRON_INGOT,
                ChatColor.AQUA + "Deposit to Town Bank",
                ChatColor.GRAY + "Transfer from your wallet to the town bank",
                ChatColor.DARK_GRAY + "Click to deposit"
            ));
        } else {
            gui.setItem(22, GuiUtil.createItem(Material.BARRIER,
                ChatColor.RED + "Vault not available",
                ChatColor.GRAY + "Install Vault and EssentialsX to enable payments."
            ));
        }

        if (resident.isMayor()) {
            gui.setItem(28, GuiUtil.createItem(Material.GOLD_NUGGET,
                ChatColor.YELLOW + "Set Town Tax",
                ChatColor.GRAY + "Adjust daily tax for residents in your town",
                ChatColor.DARK_GRAY + "Click to configure"
            ));
            gui.setItem(30, GuiUtil.createItem(Material.TRIPWIRE_HOOK,
                ChatColor.AQUA + "Toggle Town Tax Type",
                ChatColor.GRAY + "Switch between flat and percentage tax",
                ChatColor.DARK_GRAY + "Click to toggle"
            ));
        }

        if (resident.isKing()) {
            gui.setItem(32, GuiUtil.createItem(Material.NETHER_STAR,
                ChatColor.LIGHT_PURPLE + "Set Nation Tax",
                ChatColor.GRAY + "Adjust daily tax towns pay to the nation",
                ChatColor.DARK_GRAY + "Click to configure"
            ));
            gui.setItem(34, GuiUtil.createItem(Material.BEACON,
                ChatColor.GOLD + "Pay All Citizens",
                ChatColor.GRAY + "Send equal payment from nation bank to all residents",
                ChatColor.DARK_GRAY + "Click to pay out"
            ));
        }

        gui.setItem(40, GuiUtil.createItem(Material.ARROW,
            ChatColor.WHITE + "Back",
            ChatColor.GRAY + "Return to main menu"
        ));

        player.openInventory(gui);
    }

    public static void openBedrockGui(Player player) {
        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null) return;

        EconomyService eco = GeoWarPlugin.getInstance().getEconomy();
        Nation nation = resident.getNationOrNull();
        Town town = resident.getTownOrNull();

        String playerBalance = eco.isAvailable() ? eco.format(eco.getBalance(player)) : "Vault unavailable";
        String nationBank = nation != null ? eco.format(nation.getAccount().getHoldingBalance()) : "N/A";
        String townBank = town != null ? eco.format(town.getAccount().getHoldingBalance()) : "N/A";

        String content = "Your Balance: " + playerBalance
            + "\nNation Bank: " + nationBank
            + "\nTown Bank: " + townBank
            + "\n\nManage your economy.";

        SimpleForm.Builder form = SimpleForm.builder()
            .title("Economy & Taxes")
            .content(content)
            .button("Pay Citizen")
            .button("Deposit to Nation Bank")
            .button("Deposit to Town Bank")
            .button("Set Town Tax")
            .button("Set Nation Tax")
            .button("Pay All Citizens");

        form.validResultHandler((response) -> {
            int id = response.clickedButtonId();
            if (!eco.isAvailable() && id < 3) {
                player.sendMessage(ChatColor.RED + "Vault is not available. Cannot process payments.");
                return;
            }
            switch (id) {
                case 0: openPayCitizenBedrock(player); break;
                case 1: openDepositBedrock(player, "nation"); break;
                case 2: openDepositBedrock(player, "town"); break;
                case 3: openSetTaxBedrock(player, "town"); break;
                case 4: openSetTaxBedrock(player, "nation"); break;
                case 5: payAllCitizens(player); break;
            }
        });

        FloodgateBridge.sendForm(player.getUniqueId(), form.build());
    }

    private static void openPayCitizenBedrock(Player player) {
        CustomForm.Builder form = CustomForm.builder()
            .title("Pay Citizen")
            .input("Player Name", "Enter the recipient's name")
            .input("Amount", "Enter amount (e.g. 100)");

        form.validResultHandler((r) -> {
            String targetName = r.asInput(0);
            String amountStr = r.asInput(1);
            payCitizen(player, targetName, amountStr);
        });

        FloodgateBridge.sendForm(player.getUniqueId(), form.build());
    }

    private static void openDepositBedrock(Player player, String target) {
        CustomForm.Builder form = CustomForm.builder()
            .title("Deposit to " + (target.equals("nation") ? "Nation" : "Town") + " Bank")
            .input("Amount", "Enter amount to deposit");

        form.validResultHandler((r) -> depositToBank(player, r.asInput(0), target));
        FloodgateBridge.sendForm(player.getUniqueId(), form.build());
    }

    private static void openSetTaxBedrock(Player player, String type) {
        CustomForm.Builder form = CustomForm.builder()
            .title("Set " + (type.equals("nation") ? "Nation" : "Town") + " Tax")
            .input("Tax Amount", "Enter daily tax value");

        form.validResultHandler((r) -> {
            String amountStr = r.asInput(0);
            try {
                int amount = Integer.parseInt(amountStr.trim());
                String cmd = type.equals("nation") ? "nation set taxes " + amount : "town set taxes " + amount;
                player.performCommand(cmd);
                player.sendMessage(ChatColor.GREEN + "Tax set to " + amount + ".");
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid amount.");
            }
        });

        FloodgateBridge.sendForm(player.getUniqueId(), form.build());
    }

    public static void handleClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;
        String itemName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());

        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null) return;
        EconomyService eco = GeoWarPlugin.getInstance().getEconomy();

        switch (itemName) {
            case "Pay Citizen":
                if (!eco.isAvailable()) { player.sendMessage(ChatColor.RED + "Vault not available."); return; }
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "Enter the citizen's name:");
                GeoWarPlugin.getInstance().getPendingActions().put(player.getUniqueId(), "pay_citizen_name");
                break;

            case "Deposit to Nation Bank":
                if (!eco.isAvailable()) { player.sendMessage(ChatColor.RED + "Vault not available."); return; }
                if (resident.getNationOrNull() == null) { player.sendMessage(ChatColor.RED + "You are not in a nation."); return; }
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "Enter the amount to deposit to the nation bank:");
                GeoWarPlugin.getInstance().getPendingActions().put(player.getUniqueId(), "deposit_nation");
                break;

            case "Deposit to Town Bank":
                if (!eco.isAvailable()) { player.sendMessage(ChatColor.RED + "Vault not available."); return; }
                if (resident.getTownOrNull() == null) { player.sendMessage(ChatColor.RED + "You are not in a town."); return; }
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "Enter the amount to deposit to the town bank:");
                GeoWarPlugin.getInstance().getPendingActions().put(player.getUniqueId(), "deposit_town");
                break;

            case "Set Town Tax":
                if (!resident.isMayor()) { player.sendMessage(ChatColor.RED + "Only the Mayor can set town taxes."); return; }
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "Enter the new town tax amount:");
                GeoWarPlugin.getInstance().getPendingActions().put(player.getUniqueId(), "set_town_tax");
                break;

            case "Set Nation Tax":
                if (!resident.isKing()) { player.sendMessage(ChatColor.RED + "Only the King can set nation taxes."); return; }
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "Enter the new nation tax amount:");
                GeoWarPlugin.getInstance().getPendingActions().put(player.getUniqueId(), "set_nation_tax");
                break;

            case "Pay All Citizens":
                if (!resident.isKing()) { player.sendMessage(ChatColor.RED + "Only the King can pay all citizens."); return; }
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "Enter the amount to pay each citizen from the nation bank:");
                GeoWarPlugin.getInstance().getPendingActions().put(player.getUniqueId(), "pay_all_citizens");
                break;

            case "Back":
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(GeoWarPlugin.getInstance(), () ->
                    new com.geowar.commands.NationGuiCommand().openMainGui(player), 1L);
                break;
        }
    }

    public static void payCitizen(Player sender, String targetName, String amountStr) {
        EconomyService eco = GeoWarPlugin.getInstance().getEconomy();
        if (!eco.isAvailable()) {
            sender.sendMessage(ChatColor.RED + "Vault is not available.");
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountStr.trim());
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid amount: " + amountStr);
            return;
        }

        if (!eco.has(sender, amount)) {
            sender.sendMessage(ChatColor.RED + "You don't have enough money. Balance: " + eco.format(eco.getBalance(sender)));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Player '" + targetName + "' not found.");
            return;
        }

        eco.withdraw(sender, amount);
        eco.deposit(target.getPlayer() != null ? target.getPlayer() : sender, amount);
        sender.sendMessage(ChatColor.GREEN + "Paid " + eco.format(amount) + " to " + targetName + ".");
        if (target.isOnline()) {
            target.getPlayer().sendMessage(ChatColor.GREEN + "You received " + eco.format(amount) + " from " + sender.getName() + ".");
        }
    }

    public static void depositToBank(Player player, String amountStr, String bankType) {
        EconomyService eco = GeoWarPlugin.getInstance().getEconomy();
        if (!eco.isAvailable()) {
            player.sendMessage(ChatColor.RED + "Vault is not available.");
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountStr.trim());
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid amount: " + amountStr);
            return;
        }

        if (!eco.has(player, amount)) {
            player.sendMessage(ChatColor.RED + "Insufficient funds. Balance: " + eco.format(eco.getBalance(player)));
            return;
        }

        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null) return;

        if (bankType.equals("nation")) {
            Nation nation = resident.getNationOrNull();
            if (nation == null) { player.sendMessage(ChatColor.RED + "You are not in a nation."); return; }
            eco.withdraw(player, amount);
            nation.getAccount().deposit(amount, "GeoWar deposit by " + player.getName());
            player.sendMessage(ChatColor.GREEN + "Deposited " + eco.format(amount) + " to the " + nation.getName() + " nation bank.");
        } else {
            Town town = resident.getTownOrNull();
            if (town == null) { player.sendMessage(ChatColor.RED + "You are not in a town."); return; }
            eco.withdraw(player, amount);
            town.getAccount().deposit(amount, "GeoWar deposit by " + player.getName());
            player.sendMessage(ChatColor.GREEN + "Deposited " + eco.format(amount) + " to the " + town.getName() + " town bank.");
        }
    }

    public static void payAllCitizens(Player king) {
        EconomyService eco = GeoWarPlugin.getInstance().getEconomy();
        if (!eco.isAvailable()) {
            king.sendMessage(ChatColor.RED + "Vault is not available.");
            return;
        }
        Resident resident = TownyAPI.getInstance().getResident(king);
        if (resident == null || !resident.isKing()) {
            king.sendMessage(ChatColor.RED + "Only the King can pay all citizens.");
            return;
        }

        king.sendMessage(ChatColor.YELLOW + "Enter amount per citizen:");
        GeoWarPlugin.getInstance().getPendingActions().put(king.getUniqueId(), "pay_all_citizens");
    }

    public static void executePayAllCitizens(Player king, String amountStr) {
        EconomyService eco = GeoWarPlugin.getInstance().getEconomy();
        Resident resident = TownyAPI.getInstance().getResident(king);
        if (resident == null) return;
        Nation nation = resident.getNationOrNull();
        if (nation == null) { king.sendMessage(ChatColor.RED + "You are not in a nation."); return; }

        double amount;
        try {
            amount = Double.parseDouble(amountStr.trim());
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            king.sendMessage(ChatColor.RED + "Invalid amount.");
            return;
        }

        double total = amount * nation.getResidents().size();
        double nationBalance = nation.getAccount().getHoldingBalance();

        if (nationBalance < total) {
            king.sendMessage(ChatColor.RED + "Nation bank insufficient. Needs " + eco.format(total)
                + " but has " + eco.format(nationBalance) + ".");
            return;
        }

        int paid = 0;
        for (Resident r : nation.getResidents()) {
            Player target = Bukkit.getPlayer(r.getUUID());
            if (target != null) {
                eco.deposit(target, amount);
                target.sendMessage(ChatColor.GREEN + "You received " + eco.format(amount) + " from the " + nation.getName() + " treasury.");
                paid++;
            }
        }

        nation.getAccount().withdraw(total, "GeoWar pay-all by " + king.getName());
        king.sendMessage(ChatColor.GREEN + "Paid " + eco.format(amount) + " to " + paid + " online citizens. "
            + ChatColor.GRAY + "(" + (nation.getResidents().size() - paid) + " offline residents skipped)");
    }
}
