package com.geowar.gui;

import com.geowar.GeoWarPlugin;
import com.geowar.data.TreatyManager;
import com.geowar.data.WarManager;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import com.geowar.integration.FloodgateBridge;

import java.util.Collection;
import java.util.List;

public class DiplomacyGui {

    public static void openJavaGui(Player player) {
        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null || resident.getNationOrNull() == null) {
            player.sendMessage(ChatColor.RED + "You must be in a nation to use diplomacy.");
            return;
        }
        Nation nation = resident.getNationOrNull();

        WarManager wm = GeoWarPlugin.getInstance().getWarManager();
        TreatyManager tm = GeoWarPlugin.getInstance().getTreatyManager();

        int activeWars = wm.getWarsFor(nation.getName()).size();
        int activeTreaties = tm.getTreatiesFor(nation.getName()).size();

        Inventory gui = Bukkit.createInventory(null, 45, "Diplomacy");
        for (int i = 0; i < 45; i++) gui.setItem(i, GuiUtil.createItem(Material.GRAY_STAINED_GLASS_PANE, " "));

        gui.setItem(4, GuiUtil.createItem(Material.GLOBE_BANNER_PATTERN,
            ChatColor.GOLD + "Status: " + nation.getName(),
            ChatColor.RED + "Active Wars: " + activeWars,
            ChatColor.GREEN + "Treaties: " + activeTreaties
        ));

        gui.setItem(10, GuiUtil.createItem(Material.IRON_AXE,
            ChatColor.DARK_RED + "Declare War",
            ChatColor.GRAY + "Declare war on another nation",
            ChatColor.DARK_GRAY + "Click to select target"
        ));

        gui.setItem(12, GuiUtil.createItem(Material.WHITE_BANNER,
            ChatColor.AQUA + "Propose Peace",
            ChatColor.GRAY + "End an ongoing war",
            ChatColor.DARK_GRAY + "Click to select war to end"
        ));

        gui.setItem(14, GuiUtil.createItem(Material.PAPER,
            ChatColor.GOLD + "Log Treaty",
            ChatColor.GRAY + "Form an alliance or sign a peace treaty",
            ChatColor.DARK_GRAY + "Click to create"
        ));

        gui.setItem(16, GuiUtil.createItem(Material.BOOK,
            ChatColor.GREEN + "View Treaties",
            ChatColor.GRAY + "See all active treaties",
            ChatColor.DARK_GRAY + "Click to view"
        ));

        gui.setItem(34, GuiUtil.createItem(Material.SHEARS,
            ChatColor.RED + "Revoke Treaty",
            ChatColor.GRAY + "Dissolve an existing treaty with another nation",
            ChatColor.DARK_GRAY + "Click to select treaty to revoke"
        ));

        gui.setItem(28, GuiUtil.createItem(Material.WRITABLE_BOOK,
            ChatColor.LIGHT_PURPLE + "Request Meeting",
            ChatColor.GRAY + "Send a meeting request to another leader",
            ChatColor.DARK_GRAY + "Click to send"
        ));

        gui.setItem(30, GuiUtil.createItem(Material.LECTERN,
            ChatColor.YELLOW + "View Meetings",
            ChatColor.GRAY + "See pending and accepted meeting requests",
            ChatColor.DARK_GRAY + "Click to view"
        ));

        gui.setItem(32, GuiUtil.createItem(Material.SHIELD,
            ChatColor.AQUA + "View Active Wars",
            ChatColor.GRAY + "See all nations at war",
            ChatColor.DARK_GRAY + "Click to view"
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
            player.sendMessage(ChatColor.RED + "You must be in a nation to use diplomacy.");
            return;
        }
        Nation nation = resident.getNationOrNull();
        WarManager wm = GeoWarPlugin.getInstance().getWarManager();
        TreatyManager tm = GeoWarPlugin.getInstance().getTreatyManager();

        String content = "Nation: " + nation.getName() +
            "\nActive Wars: " + wm.getWarsFor(nation.getName()).size() +
            "\nTreaties: " + tm.getTreatiesFor(nation.getName()).size() +
            "\n\nManage your international relations.";

        SimpleForm.Builder form = SimpleForm.builder()
            .title("Diplomacy")
            .content(content)
            .button("Declare War")
            .button("Propose Peace")
            .button("Log Treaty")
            .button("View Treaties")
            .button("Revoke Treaty")
            .button("Request Meeting")
            .button("View Meetings")
            .button("View Active Wars");

        form.validResultHandler((response) -> {
            int id = response.clickedButtonId();
            switch (id) {
                case 0:
                    if (!resident.isKing()) { player.sendMessage(ChatColor.RED + "Only the King can declare war."); return; }
                    openDeclareWarBedrockStep1(player, nation); break;
                case 1:
                    if (!resident.isKing()) { player.sendMessage(ChatColor.RED + "Only the King can propose peace."); return; }
                    openPeaceBedrock(player, nation); break;
                case 2:
                    if (!resident.isKing()) { player.sendMessage(ChatColor.RED + "Only the King can sign treaties."); return; }
                    openLogTreatyBedrock(player, nation); break;
                case 3: showTreatiesBedrock(player, nation); break;
                case 4:
                    if (!resident.isKing()) { player.sendMessage(ChatColor.RED + "Only the King can revoke treaties."); return; }
                    openRevokeTreatyBedrock(player, nation); break;
                case 5: openRequestMeetingBedrock(player, nation); break;
                case 6: showMeetingsBedrock(player, nation); break;
                case 7: showWarsBedrock(player, nation); break;
            }
        });

        FloodgateBridge.sendForm(player.getUniqueId(), form.build());
    }

    private static void openDeclareWarBedrockStep1(Player player, Nation nation) {
        Collection<Nation> allNations = TownyAPI.getInstance().getNations();
        SimpleForm.Builder form = SimpleForm.builder()
            .title("Declare War - Select Nation")
            .content("Select a nation to declare war on:");

        for (Nation n : allNations) {
            if (!n.getName().equalsIgnoreCase(nation.getName())) {
                form.button(n.getName());
            }
        }

        List<Nation> nationList = allNations.stream()
            .filter(n -> !n.getName().equalsIgnoreCase(nation.getName()))
            .collect(java.util.stream.Collectors.toList());

        form.validResultHandler((response) -> {
            Nation target = nationList.get(response.clickedButtonId());
            openDeclareWarBedrockStep2(player, nation, target);
        });

        FloodgateBridge.sendForm(player.getUniqueId(), form.build());
    }

    private static void openDeclareWarBedrockStep2(Player player, Nation attacker, Nation defender) {
        CustomForm.Builder form = CustomForm.builder()
            .title("Declare War on " + defender.getName())
            .input("Reason", "State your reason for war")
            .input("Demands", "What do you demand from " + defender.getName() + "?");

        form.validResultHandler((response) -> {
            String reason = response.asInput(0);
            String demands = response.asInput(1);
            WarManager wm = GeoWarPlugin.getInstance().getWarManager();

            if (wm.isAtWar(attacker.getName(), defender.getName())) {
                player.sendMessage(ChatColor.RED + "You are already at war with " + defender.getName() + ".");
                return;
            }

            wm.declareWar(attacker.getName(), defender.getName(), reason, demands);
            broadcastWar(attacker.getName(), defender.getName(), reason, demands, player.getName());
        });

        FloodgateBridge.sendForm(player.getUniqueId(), form.build());
    }

    private static void openPeaceBedrock(Player player, Nation nation) {
        WarManager wm = GeoWarPlugin.getInstance().getWarManager();
        List<WarManager.WarRecord> wars = wm.getWarsFor(nation.getName());

        if (wars.isEmpty()) {
            player.sendMessage(ChatColor.GREEN + "You are not at war with anyone.");
            return;
        }

        SimpleForm.Builder form = SimpleForm.builder()
            .title("Propose Peace")
            .content("Select a war to end:");

        for (WarManager.WarRecord war : wars) {
            String opponent = war.attacker.equalsIgnoreCase(nation.getName()) ? war.defender : war.attacker;
            form.button(opponent);
        }

        form.validResultHandler((response) -> {
            WarManager.WarRecord war = wars.get(response.clickedButtonId());
            String opponent = war.attacker.equalsIgnoreCase(nation.getName()) ? war.defender : war.attacker;
            wm.endWar(nation.getName(), opponent);
            Bukkit.broadcastMessage(ChatColor.GREEN + "[GeoWar] " + nation.getName() + " and " + opponent + " have signed a peace agreement.");
            GeoWarPlugin.getInstance().getDiscord().sendPeaceAgreement(nation.getName(), opponent, player.getName());
        });

        FloodgateBridge.sendForm(player.getUniqueId(), form.build());
    }

    private static void openLogTreatyBedrock(Player player, Nation nation) {
        Collection<Nation> allNations = TownyAPI.getInstance().getNations();
        List<Nation> others = allNations.stream()
            .filter(n -> !n.getName().equalsIgnoreCase(nation.getName()))
            .collect(java.util.stream.Collectors.toList());

        SimpleForm.Builder nationPicker = SimpleForm.builder()
            .title("Log Treaty - Select Nation")
            .content("Select a nation to form a treaty with:");

        for (Nation n : others) nationPicker.button(n.getName());

        nationPicker.validResultHandler((resp) -> {
            Nation partner = others.get(resp.clickedButtonId());
            CustomForm.Builder form = CustomForm.builder()
                .title("Treaty with " + partner.getName())
                .dropdown("Treaty Type", java.util.Arrays.asList("Alliance", "Non-Aggression Pact", "Trade Agreement", "Vassalage"), 0)
                .input("Terms", "Describe the terms of the treaty");

            form.validResultHandler((r) -> {
                String type = r.asDropdown(0) == 0 ? "Alliance" :
                    r.asDropdown(0) == 1 ? "Non-Aggression Pact" :
                    r.asDropdown(0) == 2 ? "Trade Agreement" : "Vassalage";
                String terms = r.asInput(1);
                GeoWarPlugin.getInstance().getTreatyManager().logTreaty(nation.getName(), partner.getName(), type, terms);
                Bukkit.broadcastMessage(ChatColor.GOLD + "[GeoWar] " + nation.getName() + " and " + partner.getName() + " have signed a " + type + ".");
                GeoWarPlugin.getInstance().getDiscord().sendTreatyLogged(nation.getName(), partner.getName(), type, terms, player.getName());
            });

            FloodgateBridge.sendForm(player.getUniqueId(), form.build());
        });

        FloodgateBridge.sendForm(player.getUniqueId(), nationPicker.build());
    }

    private static void showTreatiesBedrock(Player player, Nation nation) {
        List<TreatyManager.TreatyRecord> treaties = GeoWarPlugin.getInstance().getTreatyManager().getTreatiesFor(nation.getName());
        StringBuilder sb = new StringBuilder("Active Treaties:\n\n");
        if (treaties.isEmpty()) {
            sb.append("No active treaties.");
        } else {
            for (TreatyManager.TreatyRecord t : treaties) {
                String partner = t.nationA.equalsIgnoreCase(nation.getName()) ? t.nationB : t.nationA;
                sb.append("[").append(t.type).append("] with ").append(partner).append("\n");
            }
        }
        SimpleForm.Builder form = SimpleForm.builder().title("Treaties").content(sb.toString()).button("Back");
        form.validResultHandler(r -> openBedrockGui(player));
        FloodgateBridge.sendForm(player.getUniqueId(), form.build());
    }

    private static void openRevokeTreatyBedrock(Player player, Nation nation) {
        List<TreatyManager.TreatyRecord> treaties = GeoWarPlugin.getInstance().getTreatyManager().getTreatiesFor(nation.getName());
        if (treaties.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You have no active treaties to revoke.");
            return;
        }
        SimpleForm.Builder form = SimpleForm.builder()
            .title("Revoke Treaty")
            .content("Select a treaty to dissolve:");
        for (TreatyManager.TreatyRecord t : treaties) {
            String partner = t.nationA.equalsIgnoreCase(nation.getName()) ? t.nationB : t.nationA;
            form.button("[" + t.type + "] " + partner);
        }
        form.validResultHandler((r) -> {
            TreatyManager.TreatyRecord t = treaties.get(r.clickedButtonId());
            String partner = t.nationA.equalsIgnoreCase(nation.getName()) ? t.nationB : t.nationA;
            GeoWarPlugin.getInstance().getTreatyManager().revokeTreaty(nation.getName(), partner);
            Bukkit.broadcastMessage(ChatColor.RED + "[GeoWar] " + nation.getName() + " has revoked their treaty with " + partner + ".");
            GeoWarPlugin.getInstance().getDiscord().sendTreatyRevoked(nation.getName(), partner, player.getName());
        });
        FloodgateBridge.sendForm(player.getUniqueId(), form.build());
    }

    private static void openRequestMeetingBedrock(Player player, Nation nation) {
        Collection<Nation> allNations = TownyAPI.getInstance().getNations();
        List<Nation> others = allNations.stream()
            .filter(n -> !n.getName().equalsIgnoreCase(nation.getName()))
            .collect(java.util.stream.Collectors.toList());

        SimpleForm.Builder picker = SimpleForm.builder()
            .title("Request Meeting - Select Nation")
            .content("Select the nation leader you want to meet:");

        for (Nation n : others) picker.button(n.getName());

        picker.validResultHandler((resp) -> {
            Nation target = others.get(resp.clickedButtonId());
            CustomForm.Builder form = CustomForm.builder()
                .title("Meeting with " + target.getName())
                .input("Topic", "What do you want to discuss?");

            form.validResultHandler((r) -> {
                String topic = r.asInput(0);
                GeoWarPlugin.getInstance().getMeetingManager().requestMeeting(
                    nation.getName(), player.getName(), target.getName(), topic
                );
                player.sendMessage(ChatColor.GREEN + "Meeting request sent to " + target.getName() + ".");
            });

            FloodgateBridge.sendForm(player.getUniqueId(), form.build());
        });

        FloodgateBridge.sendForm(player.getUniqueId(), picker.build());
    }

    private static void showMeetingsBedrock(Player player, Nation nation) {
        List<com.geowar.data.MeetingManager.MeetingRequest> incoming = GeoWarPlugin.getInstance().getMeetingManager().getIncomingRequests(nation.getName());
        List<com.geowar.data.MeetingManager.MeetingRequest> sent = GeoWarPlugin.getInstance().getMeetingManager().getSentRequests(nation.getName());

        StringBuilder sb = new StringBuilder("Incoming Requests:\n");
        if (incoming.isEmpty()) sb.append("None\n");
        else for (var r : incoming) sb.append(r.fromNation).append(": ").append(r.topic).append("\n");

        sb.append("\nSent Requests:\n");
        if (sent.isEmpty()) sb.append("None");
        else for (var r : sent) sb.append(r.toNation).append(" [").append(r.status).append("]\n");

        SimpleForm.Builder form = SimpleForm.builder().title("Meetings").content(sb.toString()).button("Back");
        form.validResultHandler(r -> openBedrockGui(player));
        FloodgateBridge.sendForm(player.getUniqueId(), form.build());
    }

    private static void showWarsBedrock(Player player, Nation nation) {
        List<WarManager.WarRecord> wars = GeoWarPlugin.getInstance().getWarManager().getActiveWars();
        StringBuilder sb = new StringBuilder("All Active Wars:\n\n");
        if (wars.isEmpty()) sb.append("No wars currently active.");
        else for (WarManager.WarRecord w : wars) sb.append(w.attacker).append(" vs ").append(w.defender).append("\n");

        SimpleForm.Builder form = SimpleForm.builder().title("Active Wars").content(sb.toString()).button("Back");
        form.validResultHandler(r -> openBedrockGui(player));
        FloodgateBridge.sendForm(player.getUniqueId(), form.build());
    }

    public static void handleClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;
        String itemName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());

        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null || resident.getNationOrNull() == null) return;
        Nation nation = resident.getNationOrNull();

        switch (itemName) {
            case "Declare War":
                if (!resident.isKing()) { player.sendMessage(ChatColor.RED + "Only the King can declare war."); return; }
                player.closeInventory();
                openNationPickerJava(player, nation, "declare_war");
                break;
            case "Propose Peace":
                if (!resident.isKing()) { player.sendMessage(ChatColor.RED + "Only the King can propose peace."); return; }
                player.closeInventory();
                openPeacePickerJava(player, nation);
                break;
            case "Log Treaty":
                if (!resident.isKing()) { player.sendMessage(ChatColor.RED + "Only the King can sign treaties."); return; }
                player.closeInventory();
                openNationPickerJava(player, nation, "log_treaty");
                break;
            case "View Treaties":
                player.closeInventory();
                openTreatiesJava(player, nation);
                break;
            case "Revoke Treaty":
                if (!resident.isKing()) { player.sendMessage(ChatColor.RED + "Only the King can revoke treaties."); return; }
                player.closeInventory();
                openRevokeTreatyJava(player, nation);
                break;
            case "Request Meeting":
                player.closeInventory();
                openNationPickerJava(player, nation, "request_meeting");
                break;
            case "View Meetings":
                player.closeInventory();
                openMeetingsJava(player, nation);
                break;
            case "View Active Wars":
                player.closeInventory();
                openWarsJava(player);
                break;
            case "Back":
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(GeoWarPlugin.getInstance(), () ->
                    new com.geowar.commands.NationGuiCommand().openMainGui(player), 1L);
                break;
        }
    }

    private static void openNationPickerJava(Player player, Nation myNation, String action) {
        Collection<Nation> allNations = TownyAPI.getInstance().getNations();
        List<Nation> others = allNations.stream()
            .filter(n -> !n.getName().equalsIgnoreCase(myNation.getName()))
            .collect(java.util.stream.Collectors.toList());

        Inventory gui = Bukkit.createInventory(null, 54, "Select Nation");
        for (int i = 0; i < 54; i++) gui.setItem(i, GuiUtil.createItem(Material.GRAY_STAINED_GLASS_PANE, " "));

        int slot = 10;
        for (Nation n : others) {
            WarManager wm = GeoWarPlugin.getInstance().getWarManager();
            TreatyManager tm = GeoWarPlugin.getInstance().getTreatyManager();
            String status = wm.isAtWar(myNation.getName(), n.getName()) ? ChatColor.RED + "At War" :
                tm.hasTreaty(myNation.getName(), n.getName()) ? ChatColor.GREEN + "Allied" : ChatColor.GRAY + "Neutral";

            gui.setItem(slot, GuiUtil.createItem(Material.GLOBE_BANNER_PATTERN,
                ChatColor.GOLD + n.getName(),
                ChatColor.GRAY + "Towns: " + n.getTowns().size(),
                ChatColor.GRAY + "Status: " + status,
                ChatColor.DARK_GRAY + "Click to " + action.replace("_", " ")
            ));
            slot++;
            if (slot >= 44) break;
        }

        gui.setItem(49, GuiUtil.createItem(Material.ARROW, ChatColor.WHITE + "Back", ChatColor.GRAY + "Return to Diplomacy"));
        player.openInventory(gui);

        GeoWarPlugin.getInstance().getPendingActions().put(player.getUniqueId(), action + ":" + myNation.getName());
    }

    private static void openPeacePickerJava(Player player, Nation nation) {
        WarManager wm = GeoWarPlugin.getInstance().getWarManager();
        List<WarManager.WarRecord> wars = wm.getWarsFor(nation.getName());

        Inventory gui = Bukkit.createInventory(null, 27, "Propose Peace");
        for (int i = 0; i < 27; i++) gui.setItem(i, GuiUtil.createItem(Material.GRAY_STAINED_GLASS_PANE, " "));

        if (wars.isEmpty()) {
            gui.setItem(13, GuiUtil.createItem(Material.BARRIER, ChatColor.GREEN + "Not at war", ChatColor.GRAY + "You have no active wars."));
        } else {
            int slot = 10;
            for (WarManager.WarRecord war : wars) {
                String opponent = war.attacker.equalsIgnoreCase(nation.getName()) ? war.defender : war.attacker;
                gui.setItem(slot, GuiUtil.createItem(Material.WHITE_BANNER,
                    ChatColor.AQUA + "Peace with " + opponent,
                    ChatColor.GRAY + "Reason: " + war.reason,
                    ChatColor.DARK_GRAY + "Click to end this war"
                ));
                slot++;
            }
        }

        gui.setItem(22, GuiUtil.createItem(Material.ARROW, ChatColor.WHITE + "Back", ChatColor.GRAY + "Return to Diplomacy"));
        player.openInventory(gui);

        GeoWarPlugin.getInstance().getPendingActions().put(player.getUniqueId(), "peace:" + nation.getName());
    }

    private static void openTreatiesJava(Player player, Nation nation) {
        List<TreatyManager.TreatyRecord> treaties = GeoWarPlugin.getInstance().getTreatyManager().getTreatiesFor(nation.getName());

        Inventory gui = Bukkit.createInventory(null, 54, "Active Treaties");
        for (int i = 0; i < 54; i++) gui.setItem(i, GuiUtil.createItem(Material.GRAY_STAINED_GLASS_PANE, " "));

        if (treaties.isEmpty()) {
            gui.setItem(22, GuiUtil.createItem(Material.BARRIER, ChatColor.RED + "No active treaties", ChatColor.GRAY + "Log a treaty in the Diplomacy menu."));
        } else {
            int slot = 10;
            for (TreatyManager.TreatyRecord t : treaties) {
                String partner = t.nationA.equalsIgnoreCase(nation.getName()) ? t.nationB : t.nationA;
                gui.setItem(slot, GuiUtil.createItem(Material.PAPER,
                    ChatColor.GOLD + "[" + t.type + "] " + partner,
                    ChatColor.GRAY + "Terms: " + t.terms
                ));
                slot++;
                if (slot >= 44) break;
            }
        }

        gui.setItem(49, GuiUtil.createItem(Material.ARROW, ChatColor.WHITE + "Back", ChatColor.GRAY + "Return to Diplomacy"));
        player.openInventory(gui);
    }

    public static void openRevokeTreatyJava(Player player, Nation nation) {
        List<TreatyManager.TreatyRecord> treaties = GeoWarPlugin.getInstance().getTreatyManager().getTreatiesFor(nation.getName());

        Inventory gui = Bukkit.createInventory(null, 54, "Revoke Treaty");
        for (int i = 0; i < 54; i++) gui.setItem(i, GuiUtil.createItem(Material.GRAY_STAINED_GLASS_PANE, " "));

        if (treaties.isEmpty()) {
            gui.setItem(22, GuiUtil.createItem(Material.BARRIER,
                ChatColor.RED + "No treaties to revoke",
                ChatColor.GRAY + "You have no active treaties."
            ));
        } else {
            int slot = 10;
            for (TreatyManager.TreatyRecord t : treaties) {
                String partner = t.nationA.equalsIgnoreCase(nation.getName()) ? t.nationB : t.nationA;
                gui.setItem(slot, GuiUtil.createItem(Material.SHEARS,
                    ChatColor.RED + "[" + t.type + "] " + partner,
                    ChatColor.GRAY + "Terms: " + t.terms,
                    ChatColor.DARK_RED + "Click to dissolve this treaty"
                ));
                slot++;
                if (slot >= 44) break;
            }
        }

        gui.setItem(49, GuiUtil.createItem(Material.ARROW, ChatColor.WHITE + "Back", ChatColor.GRAY + "Return to Diplomacy"));
        player.openInventory(gui);
    }

    private static void openMeetingsJava(Player player, Nation nation) {
        List<com.geowar.data.MeetingManager.MeetingRequest> incoming = GeoWarPlugin.getInstance().getMeetingManager().getIncomingRequests(nation.getName());
        List<com.geowar.data.MeetingManager.MeetingRequest> sent = GeoWarPlugin.getInstance().getMeetingManager().getSentRequests(nation.getName());

        Inventory gui = Bukkit.createInventory(null, 54, "Meetings");
        for (int i = 0; i < 54; i++) gui.setItem(i, GuiUtil.createItem(Material.GRAY_STAINED_GLASS_PANE, " "));

        gui.setItem(4, GuiUtil.createItem(Material.LECTERN, ChatColor.GOLD + "Meeting Requests", ChatColor.GRAY + "Manage incoming and sent requests"));

        int slot = 10;
        for (com.geowar.data.MeetingManager.MeetingRequest r : incoming) {
            gui.setItem(slot, GuiUtil.createItem(Material.WRITABLE_BOOK,
                ChatColor.LIGHT_PURPLE + "From: " + r.fromNation,
                ChatColor.GRAY + "Leader: " + r.fromLeaderName,
                ChatColor.WHITE + "Topic: " + r.topic,
                ChatColor.YELLOW + "[PENDING]",
                ChatColor.DARK_GRAY + "Click to accept/decline"
            ));
            slot++;
            if (slot >= 25) break;
        }

        int sentSlot = 28;
        for (com.geowar.data.MeetingManager.MeetingRequest r : sent) {
            gui.setItem(sentSlot, GuiUtil.createItem(Material.BOOK,
                ChatColor.YELLOW + "To: " + r.toNation,
                ChatColor.WHITE + "Topic: " + r.topic,
                ChatColor.GRAY + "Status: " + r.status
            ));
            sentSlot++;
            if (sentSlot >= 44) break;
        }

        gui.setItem(49, GuiUtil.createItem(Material.ARROW, ChatColor.WHITE + "Back", ChatColor.GRAY + "Return to Diplomacy"));
        player.openInventory(gui);
    }

    private static void openWarsJava(Player player) {
        List<WarManager.WarRecord> wars = GeoWarPlugin.getInstance().getWarManager().getActiveWars();

        Inventory gui = Bukkit.createInventory(null, 54, "Active Wars");
        for (int i = 0; i < 54; i++) gui.setItem(i, GuiUtil.createItem(Material.GRAY_STAINED_GLASS_PANE, " "));

        if (wars.isEmpty()) {
            gui.setItem(22, GuiUtil.createItem(Material.WHITE_BANNER, ChatColor.GREEN + "World is at peace", ChatColor.GRAY + "No active wars."));
        } else {
            int slot = 10;
            for (WarManager.WarRecord war : wars) {
                gui.setItem(slot, GuiUtil.createItem(Material.IRON_AXE,
                    ChatColor.RED + war.attacker + " vs " + war.defender,
                    ChatColor.GRAY + "Reason: " + war.reason,
                    ChatColor.GRAY + "Demands: " + war.demands
                ));
                slot++;
                if (slot >= 44) break;
            }
        }

        gui.setItem(49, GuiUtil.createItem(Material.ARROW, ChatColor.WHITE + "Back", ChatColor.GRAY + "Return to Diplomacy"));
        player.openInventory(gui);
    }

    public static void broadcastWar(String attacker, String defender, String reason, String demands, String declarerName) {
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "[GeoWar] " + ChatColor.RED + attacker +
            ChatColor.DARK_RED + " has declared war on " + ChatColor.RED + defender + ChatColor.DARK_RED + "!");
        Bukkit.broadcastMessage(ChatColor.GRAY + "Reason: " + ChatColor.WHITE + reason);
        Bukkit.broadcastMessage(ChatColor.GRAY + "Demands: " + ChatColor.WHITE + demands);
        GeoWarPlugin.getInstance().getDiscord().sendWarDeclaration(attacker, defender, reason, demands, declarerName);
    }
}
