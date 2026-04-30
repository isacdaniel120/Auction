package dev.auction.auctionhouse.gui;

import dev.auction.auctionhouse.AuctionHouse;
import dev.auction.auctionhouse.managers.AuctionManager;
import dev.auction.auctionhouse.models.AuctionListing;
import dev.auction.auctionhouse.models.SortType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.util.*;

public class AuctionHouseGUI {

    private final AuctionHouse plugin;
    private final AuctionManager manager;

    // GUI slots: rows 0-3 = items (4*9=36 slots 0-35), row 4-5 = controls
    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };
    private static final int SIZE = 54;

    public AuctionHouseGUI(AuctionHouse plugin, AuctionManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void open(Player player, int page, SortType sort, String search) {
        List<AuctionListing> listings = manager.getListings(sort, search);
        int totalPages = Math.max(1, (int) Math.ceil((double) listings.size() / ITEM_SLOTS.length));
        page = Math.max(0, Math.min(page, totalPages - 1));

        String title = plugin.color("&8» &6&lAuction House &8| &7Page " + (page + 1) + "/" + totalPages);
        Inventory inv = Bukkit.createInventory(null, SIZE, title);

        // Border glass
        fillBorder(inv);

        // Listing items
        int start = page * ITEM_SLOTS.length;
        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            int idx = start + i;
            if (idx >= listings.size()) break;
            AuctionListing listing = listings.get(idx);
            inv.setItem(ITEM_SLOTS[i], buildListingItem(listing));
        }

        String symbol = plugin.getConfig().getString("settings.currency-symbol", "$");

        // --- Row 4 (slots 36-44): Navigation & Sort ---

        // Prev page
        inv.setItem(45, buildNavItem(Material.ARROW, "&7← &ePrevious Page", page > 0 ? "&7Click to go back" : "&cNo previous page"));

        // Sort: Newest
        inv.setItem(47, buildSortButton(Material.CLOCK, "&e&lNewest First",
                sort == SortType.NEWEST ? "&a▶ Currently selected" : "&7Click to sort by newest",
                sort == SortType.NEWEST));

        // Sort: Oldest
        inv.setItem(48, buildSortButton(Material.COMPASS, "&e&lOldest First",
                sort == SortType.OLDEST ? "&a▶ Currently selected" : "&7Click to sort by oldest",
                sort == SortType.OLDEST));

        // Sort: Price Low
        inv.setItem(49, buildSortButton(Material.GOLD_NUGGET, "&a&lPrice: Low → High",
                (sort == SortType.PRICE_LOW ? "&a▶ Currently selected" : "&7Click to sort cheapest first"),
                sort == SortType.PRICE_LOW));

        // Sort: Price High
        inv.setItem(50, buildSortButton(Material.GOLD_INGOT, "&6&lPrice: High → Low",
                (sort == SortType.PRICE_HIGH ? "&a▶ Currently selected" : "&7Click to sort most expensive first"),
                sort == SortType.PRICE_HIGH));

        // Refresh button
        inv.setItem(51, buildRefreshButton());

        // Search button
        inv.setItem(52, buildNavItem(Material.BOOK, "&b&lSearch",
                search != null && !search.isEmpty() ? "&7Query: &f" + search + "\n&cClick to clear" : "&7Click to search items"));

        // Info / stats
        inv.setItem(53, buildInfoItem(listings.size(), symbol));

        // Next page
        inv.setItem(53 - 8, buildNavItem(Material.ARROW, "&ePage &e→ &7Next",
                page < totalPages - 1 ? "&7Click to go forward" : "&cNo next page"));

        // Sell button
        inv.setItem(40, buildSellButton());

        // Inbox button
        inv.setItem(42, buildInboxButton(player));

        player.openInventory(inv);
        plugin.getGuiTracker().track(player, page, sort, search);
    }

    private void fillBorder(Inventory inv) {
        ItemStack glass = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        ItemStack blackGlass = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);

        // Top row
        for (int i = 0; i < 9; i++) inv.setItem(i, blackGlass);
        // Bottom row
        for (int i = 45; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, glass);
        }
        // Side columns
        for (int row = 1; row <= 3; row++) {
            inv.setItem(row * 9, glass);
            inv.setItem(row * 9 + 8, glass);
        }
        // Row 4
        inv.setItem(36, blackGlass);
        inv.setItem(37, blackGlass);
        inv.setItem(38, blackGlass);
        inv.setItem(39, blackGlass);
        inv.setItem(43, blackGlass);
        inv.setItem(44, blackGlass);
    }

    private ItemStack buildListingItem(AuctionListing listing) {
        ItemStack display = listing.getItem().clone();
        ItemMeta meta = display.getItemMeta();
        if (meta == null) meta = Bukkit.getItemFactory().getItemMeta(display.getType());

        String symbol = plugin.getConfig().getString("settings.currency-symbol", "$");
        long timeLeft = listing.getExpiresAt() - System.currentTimeMillis();
        String timeStr = formatTimeLeft(timeLeft);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm");
        String listedDate = sdf.format(new Date(listing.getListedAt()));

        List<String> lore = new ArrayList<>();
        if (meta.hasLore()) lore.addAll(meta.getLore());
        lore.add("");
        lore.add(plugin.color("&8┌ &6Seller: &f" + listing.getSellerName()));
        lore.add(plugin.color("&8├ &6Price: &a" + symbol + String.format("%,.2f", listing.getPrice())));
        lore.add(plugin.color("&8├ &7Listed: &f" + listedDate));
        lore.add(plugin.color("&8└ &7Expires in: &e" + timeStr));
        lore.add("");
        lore.add(plugin.color("&e▶ Click to purchase"));

        meta.setLore(lore);
        display.setItemMeta(meta);
        return display;
    }

    private ItemStack buildSortButton(Material mat, String name, String loreText, boolean selected) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.color(name));
        List<String> lore = new ArrayList<>();
        for (String line : loreText.split("\n")) {
            lore.add(plugin.color(line));
        }
        if (selected) {
            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildRefreshButton() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.color("&b&lRefresh"));
        meta.setLore(List.of(plugin.color("&7Click to refresh listings")));
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildNavItem(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.color(name));
        List<String> loreList = new ArrayList<>();
        for (String line : lore.split("\n")) loreList.add(plugin.color(line));
        meta.setLore(loreList);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildSellButton() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.color("&a&lSell Item"));
        meta.setLore(List.of(
                plugin.color("&7Hold an item and type:"),
                plugin.color("&f/ah sell <price>"),
                plugin.color(""),
                plugin.color("&7Tax: &c" + plugin.getConfig().getDouble("settings.tax", 5) + "%")
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildInboxButton(Player player) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.color("&e&lInbox"));
        meta.setLore(List.of(
                plugin.color("&7Claim expired items"),
                plugin.color("&7and received payments."),
                plugin.color(""),
                plugin.color("&eClick to open")
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildInfoItem(int total, String symbol) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.color("&f&lAuction House Info"));
        meta.setLore(List.of(
                plugin.color("&7Total listings: &f" + total),
                plugin.color("&7Tax on sales: &c" + plugin.getConfig().getDouble("settings.tax", 5) + "%"),
                plugin.color("&7Currency: &6" + symbol)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.color(name));
        if (lore != null) meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String formatTimeLeft(long ms) {
        if (ms <= 0) return "Expired";
        long hours = ms / 3600000;
        long minutes = (ms % 3600000) / 60000;
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m";
    }
}
