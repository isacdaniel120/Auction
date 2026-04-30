package dev.auction.auctionhouse.managers;

import dev.auction.auctionhouse.AuctionHouse;
import dev.auction.auctionhouse.models.AuctionListing;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class DatabaseManager {

    private final AuctionHouse plugin;
    private Connection connection;

    public DatabaseManager(AuctionHouse plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "auction.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            createTables();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to database", e);
        }
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to close database", e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS listings (
                    id TEXT PRIMARY KEY,
                    seller_uuid TEXT NOT NULL,
                    seller_name TEXT NOT NULL,
                    item BLOB NOT NULL,
                    price REAL NOT NULL,
                    listed_at INTEGER NOT NULL,
                    expires_at INTEGER NOT NULL
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS inbox (
                    id TEXT PRIMARY KEY,
                    owner_uuid TEXT NOT NULL,
                    item BLOB,
                    money REAL DEFAULT 0,
                    reason TEXT,
                    created_at INTEGER NOT NULL
                )
            """);
        }
    }

    public void saveListing(AuctionListing listing) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO listings VALUES (?,?,?,?,?,?,?)")) {
            ps.setString(1, listing.getId());
            ps.setString(2, listing.getSellerUUID().toString());
            ps.setString(3, listing.getSellerName());
            ps.setBytes(4, serializeItem(listing.getItem()));
            ps.setDouble(5, listing.getPrice());
            ps.setLong(6, listing.getListedAt());
            ps.setLong(7, listing.getExpiresAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save listing", e);
        }
    }

    public void deleteListing(String id) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM listings WHERE id=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete listing", e);
        }
    }

    public List<AuctionListing> loadAllListings() {
        List<AuctionListing> listings = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM listings")) {
            while (rs.next()) {
                ItemStack item = deserializeItem(rs.getBytes("item"));
                if (item == null) continue;
                listings.add(new AuctionListing(
                        rs.getString("id"),
                        UUID.fromString(rs.getString("seller_uuid")),
                        rs.getString("seller_name"),
                        item,
                        rs.getDouble("price"),
                        rs.getLong("listed_at"),
                        rs.getLong("expires_at")
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load listings", e);
        }
        return listings;
    }

    public void addInboxItem(UUID owner, ItemStack item, double money, String reason) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO inbox VALUES (?,?,?,?,?,?)")) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, owner.toString());
            ps.setBytes(3, item != null ? serializeItem(item) : null);
            ps.setDouble(4, money);
            ps.setString(5, reason);
            ps.setLong(6, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to add inbox item", e);
        }
    }

    public List<Map<String, Object>> getInbox(UUID owner) {
        List<Map<String, Object>> inbox = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM inbox WHERE owner_uuid=? ORDER BY created_at DESC")) {
            ps.setString(1, owner.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("id", rs.getString("id"));
                byte[] itemBytes = rs.getBytes("item");
                entry.put("item", itemBytes != null ? deserializeItem(itemBytes) : null);
                entry.put("money", rs.getDouble("money"));
                entry.put("reason", rs.getString("reason"));
                inbox.add(entry);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get inbox", e);
        }
        return inbox;
    }

    public void clearInboxEntry(String id) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM inbox WHERE id=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to clear inbox entry", e);
        }
    }

    private byte[] serializeItem(ItemStack item) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BukkitObjectOutputStream boos = new BukkitObjectOutputStream(baos)) {
            boos.writeObject(item);
            return baos.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    private ItemStack deserializeItem(byte[] data) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             BukkitObjectInputStream bois = new BukkitObjectInputStream(bais)) {
            return (ItemStack) bois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }
}
