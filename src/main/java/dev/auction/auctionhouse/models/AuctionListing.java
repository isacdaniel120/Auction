package dev.auction.auctionhouse.models;

import org.bukkit.inventory.ItemStack;
import java.util.UUID;

public class AuctionListing {

    private final String id;
    private final UUID sellerUUID;
    private final String sellerName;
    private final ItemStack item;
    private double price;
    private final long listedAt;
    private final long expiresAt;

    public AuctionListing(String id, UUID sellerUUID, String sellerName, ItemStack item, double price, long listedAt, long expiresAt) {
        this.id = id;
        this.sellerUUID = sellerUUID;
        this.sellerName = sellerName;
        this.item = item.clone();
        this.price = price;
        this.listedAt = listedAt;
        this.expiresAt = expiresAt;
    }

    public String getId() { return id; }
    public UUID getSellerUUID() { return sellerUUID; }
    public String getSellerName() { return sellerName; }
    public ItemStack getItem() { return item.clone(); }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public long getListedAt() { return listedAt; }
    public long getExpiresAt() { return expiresAt; }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}
