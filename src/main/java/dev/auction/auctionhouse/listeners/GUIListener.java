package dev.auction.auctionhouse.listeners;

import dev.auction.auctionhouse.AuctionHouse;
import dev.auction.auctionhouse.gui.AuctionHouseGUI;
import dev.auction.auctionhouse.gui.ConfirmGUI;
import dev.auction.auctionhouse.gui.GUITracker;
import dev.auction.auctionhouse.gui.InboxGUI;
import dev.auction.auctionhouse.managers.AuctionManager;
import dev.auction.auctionhouse.managers.DatabaseManager;
import dev.auction.auctionhouse.models.AuctionListing;
import dev.auction.auctionhouse.models.SortType;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class GUIListener implements Listener {

    private final AuctionHouse plugin;
    private final AuctionManager manager;
    private final DatabaseManager db;
    private final AuctionHouseGUI ahGui;
    private final ConfirmGUI confirmGui;
    private final InboxGUI inboxGui;

    // Slot constants
    private static final int PREV_PAGE = 45;
    private static final int SORT_NEWEST = 47;
    private static final int SORT_OLDEST = 48;
    private static final int SORT_PRICE_LOW = 49;
    private static final int SORT_PRICE_HIGH = 50;
    private static final int REFRESH = 51;
    private static final int SEARCH = 52;
    private static final int SELL_BTN = 40;
    private static final int INBOX_BTN = 42;
    private static final int NEXT_PAGE = 44;

    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    public GUIListener(AuctionHouse plugin, AuctionManager manager, DatabaseManager db,
                       AuctionHouseGUI ahGui, ConfirmGUI confirmGui, InboxGUI inboxGui) {
        this.plugin = plugin;
        this.manager = manager;
        this.db = db;
        this.ahGui = ahGui;
        this.confirmGui = confirmGui;
        this.inboxGui = inboxGui;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        String title = e.getView().getTitle();

        if (title.contains("Auction House")) {
            e.setCancelled(true);
            handleMainGUI(player, e.getRawSlot());
        } else if (title.contains("Confirm Purchase")) {
            e.setCancelled(true);
            handleConfirmGUI(player, e.getRawSlot());
        } else if (title.contains("Inbox")) {
            e.setCancelled(true);
            handleInboxGUI(player, e.getRawSlot());
        }
    }

    private void handleMainGUI(Player player, int slot) {
        GUITracker.PlayerGUIState state = plugin.getGuiTracker().getState(player);
        if (state == null) return;

        int page = state.page;
        SortType sort = state.sort;
        String search = state.search;

        // Item slots
        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            if (slot == ITEM_SLOTS[i]) {
                List<AuctionListing> listings = manager.getListings(sort, search);
                int idx = page * ITEM_SLOTS.length + i;
                if (idx < listings.size()) {
                    AuctionListing listing = listings.get(idx);
                    if (listing.getSellerUUID().equals(player.getUniqueId())) {
                        // Cancel own listing
                        manager.cancelListing(player, listing.getId());
                        player.sendMessage(plugin.color(plugin.getConfig().getString("messages.prefix") + "&cListing cancelled!"));
                        ahGui.open(player, page, sort, search);
                    } else if (player.hasPermission("ah.noconfirm")) {
                        purchaseItem(player, listing.getId(), page, sort, search);
                    } else {
                        confirmGui.open(player, listing);
                    }
                }
                return;
            }
        }

        switch (slot) {
            case PREV_PAGE -> { if (page > 0) ahGui.open(player, page - 1, sort, search); }
            case NEXT_PAGE -> ahGui.open(player, page + 1, sort, search);
            case SORT_NEWEST -> ahGui.open(player, 0, SortType.NEWEST, search);
            case SORT_OLDEST -> ahGui.open(player, 0, SortType.OLDEST, search);
            case SORT_PRICE_LOW -> ahGui.open(player, 0, SortType.PRICE_LOW, search);
            case SORT_PRICE_HIGH -> ahGui.open(player, 0, SortType.PRICE_HIGH, search);
            case REFRESH -> {
                player.sendMessage(plugin.color(plugin.getConfig().getString("messages.prefix") + "&bRefreshed!"));
                ahGui.open(player, page, sort, search);
            }
            case SEARCH -> {
                if (search != null && !search.isEmpty()) {
                    ahGui.open(player, 0, sort, null);
                } else {
                    player.closeInventory();
                    player.sendMessage(plugin.color("&7Type your search query in chat. Type &ccancel &7to cancel."));
                    plugin.getChatSearchTracker().startSearch(player, sort);
                }
            }
            case SELL_BTN -> {
                player.closeInventory();
                player.sendMessage(plugin.color("&7Hold the item you want to sell and type: &f/ah sell <price>"));
            }
            case INBOX_BTN -> inboxGui.open(player);
        }
    }

    private void handleConfirmGUI(Player player, int slot) {
        GUITracker.PlayerGUIState state = plugin.getGuiTracker().getState(player);
        if (state == null) return;

        if (slot >= 10 && slot <= 12) {
            // Confirm
            purchaseItem(player, state.confirmListingId, state.page, state.sort, state.search);
        } else if (slot >= 14 && slot <= 16) {
            // Cancel - go back
            ahGui.open(player, state.page, state.sort, state.search);
        }
    }

    private void handleInboxGUI(Player player, int slot) {
        GUITracker.PlayerGUIState state = plugin.getGuiTracker().getState(player);
        if (state == null) return;

        Economy eco = manager.getEconomy();
        String symbol = plugin.getConfig().getString("settings.currency-symbol", "$");
        List<Map<String, Object>> inbox = state.inboxEntries;

        if (slot == 49) {
            // Back
            ahGui.open(player, state.page, state.sort, state.search);
            return;
        }

        if (slot == 47) {
            // Claim all
            if (inbox == null) return;
            for (Map<String, Object> entry : inbox) {
                claimInboxEntry(player, entry, eco, symbol);
            }
            player.sendMessage(plugin.color(plugin.getConfig().getString("messages.prefix") + "&aClaimed all inbox items!"));
            inboxGui.open(player);
            return;
        }

        // Individual slot clicks (slots 9-44)
        if (slot >= 9 && slot <= 44 && inbox != null) {
            int idx = slot - 9;
            if (idx < inbox.size()) {
                Map<String, Object> entry = inbox.get(idx);
                claimInboxEntry(player, entry, eco, symbol);
                inboxGui.open(player);
            }
        }
    }

    private void claimInboxEntry(Player player, Map<String, Object> entry, Economy eco, String symbol) {
        String id = (String) entry.get("id");
        ItemStack item = (ItemStack) entry.get("item");
        double money = (double) entry.get("money");

        if (money > 0) {
            eco.depositPlayer(player, money);
            player.sendMessage(plugin.color("&aClaimed &6" + symbol + String.format("%,.2f", money) + "&a!"));
        }
        if (item != null) {
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
            if (!overflow.isEmpty()) {
                player.sendMessage(plugin.color("&cInventory full! Some items couldn't be claimed."));
                return;
            }
            player.sendMessage(plugin.color("&aClaimed &f" + item.getType().name() + "&a!"));
        }
        db.clearInboxEntry(id);
    }

    private void purchaseItem(Player player, String listingId, int page, SortType sort, String search) {
        plugin.purchaseItem(this, player, listingId, page, sort, search);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player player) {
            plugin.getGuiTracker().remove(player);
        }
    }
}
