package dev.auction.auctionhouse.managers;

import dev.auction.auctionhouse.AuctionHouse;
import dev.auction.auctionhouse.models.AuctionListing;
import dev.auction.auctionhouse.models.SortType;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class AuctionManager {

    private final AuctionHouse plugin;
    private final DatabaseManager db;
    private final Economy economy;
    private final Map<String, AuctionListing> listings = new HashMap<>();

    public AuctionManager(AuctionHouse plugin, DatabaseManager db, Economy economy) {
        this.plugin = plugin;
        this.db = db;
        this.economy = economy;
        loadListings();
        startExpiryTask();
    }

    private void loadListings() {
        for (AuctionListing l : db.loadAllListings()) {
            if (!l.isExpired()) listings.put(l.getId(), l);
            else db.deleteListing(l.getId());
        }
    }

    private void startExpiryTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            List<String> expired = listings.values().stream()
                    .filter(AuctionListing::isExpired)
                    .map(AuctionListing::getId)
                    .toList();

            for (String id : expired) {
                AuctionListing listing = listings.remove(id);
                if (listing == null) continue;
                db.deleteListing(id);
                db.addInboxItem(listing.getSellerUUID(), listing.getItem(), 0, "expired");
                Player seller = Bukkit.getPlayer(listing.getSellerUUID());
                if (seller != null) {
                    seller.sendMessage(plugin.color(plugin.getConfig().getString("messages.prefix", "") +
                            plugin.getConfig().getString("messages.item-expired", "")
                                    .replace("{item}", listing.getItem().getType().name())));
                }
            }
        }, 200L, 200L);
    }

    public String createListing(Player seller, ItemStack item, double price) {
        String id = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        long expires = now + (plugin.getConfig().getLong("settings.expire-hours", 48) * 3600000L);
        AuctionListing listing = new AuctionListing(id, seller.getUniqueId(), seller.getName(), item, price, now, expires);
        listings.put(id, listing);
        db.saveListing(listing);
        return id;
    }

    public boolean purchaseListing(Player buyer, String listingId) {
        AuctionListing listing = listings.get(listingId);
        if (listing == null || listing.isExpired()) return false;
        if (listing.getSellerUUID().equals(buyer.getUniqueId())) return false;

        double price = listing.getPrice();
        if (!economy.has(buyer, price)) return false;

        economy.withdrawPlayer(buyer, price);
        double tax = plugin.getConfig().getDouble("settings.tax", 5) / 100.0;
        double payout = price * (1 - tax);
        economy.depositPlayer(Bukkit.getOfflinePlayer(listing.getSellerUUID()), payout);

        listings.remove(listingId);
        db.deleteListing(listingId);

        // Give item to buyer
        Map<Integer, ItemStack> overflow = buyer.getInventory().addItem(listing.getItem());
        if (!overflow.isEmpty()) {
            db.addInboxItem(buyer.getUniqueId(), listing.getItem(), 0, "purchase");
            buyer.sendMessage(plugin.color("&cInventory full! Item sent to inbox."));
        }

        // Notify seller
        db.addInboxItem(listing.getSellerUUID(), null, payout, "sold");
        Player seller = Bukkit.getPlayer(listing.getSellerUUID());
        String symbol = plugin.getConfig().getString("settings.currency-symbol", "$");
        if (seller != null) {
            seller.sendMessage(plugin.color(plugin.getConfig().getString("messages.prefix", "") +
                    plugin.getConfig().getString("messages.item-sold", "")
                            .replace("{item}", listing.getItem().getType().name())
                            .replace("{price}", symbol + String.format("%.2f", payout))));
        }

        return true;
    }

    public boolean cancelListing(Player player, String listingId) {
        AuctionListing listing = listings.get(listingId);
        if (listing == null) return false;
        if (!listing.getSellerUUID().equals(player.getUniqueId()) && !player.hasPermission("ah.admin")) return false;

        listings.remove(listingId);
        db.deleteListing(listingId);
        db.addInboxItem(listing.getSellerUUID(), listing.getItem(), 0, "cancelled");
        return true;
    }

    public int getActiveListingCount(UUID playerUUID) {
        return (int) listings.values().stream()
                .filter(l -> l.getSellerUUID().equals(playerUUID))
                .count();
    }

    public int getListingLimit(Player player) {
        if (player.hasPermission("ah.limit.vip"))
            return plugin.getConfig().getInt("settings.max-listings-vip", 15);
        return plugin.getConfig().getInt("settings.max-listings-default", 5);
    }

    public List<AuctionListing> getListings(SortType sort, String search) {
        return listings.values().stream()
                .filter(l -> !l.isExpired())
                .filter(l -> search == null || search.isEmpty() ||
                        l.getItem().getType().name().toLowerCase().contains(search.toLowerCase()) ||
                        (l.getItem().hasItemMeta() && l.getItem().getItemMeta().hasDisplayName() &&
                                l.getItem().getItemMeta().getDisplayName().toLowerCase().contains(search.toLowerCase())))
                .sorted(getComparator(sort))
                .collect(Collectors.toList());
    }

    private Comparator<AuctionListing> getComparator(SortType sort) {
        return switch (sort) {
            case NEWEST -> Comparator.comparingLong(AuctionListing::getListedAt).reversed();
            case OLDEST -> Comparator.comparingLong(AuctionListing::getListedAt);
            case PRICE_LOW -> Comparator.comparingDouble(AuctionListing::getPrice);
            case PRICE_HIGH -> Comparator.comparingDouble(AuctionListing::getPrice).reversed();
        };
    }

    public AuctionListing getListing(String id) {
        return listings.get(id);
    }

    public Economy getEconomy() { return economy; }
}
