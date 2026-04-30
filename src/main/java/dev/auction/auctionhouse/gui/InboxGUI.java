package dev.auction.auctionhouse.gui;

import dev.auction.auctionhouse.AuctionHouse;
import dev.auction.auctionhouse.managers.AuctionManager;
import dev.auction.auctionhouse.managers.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InboxGUI {

    private final AuctionHouse plugin;
    private final DatabaseManager db;
    private final AuctionManager manager;

    public InboxGUI(AuctionHouse plugin, DatabaseManager db, AuctionManager manager) {
        this.plugin = plugin;
        this.db = db;
        this.manager = manager;
    }

    public void open(Player player) {
        List<Map<String, Object>> inbox = db.getInbox(player.getUniqueId());
        String symbol = plugin.getConfig().getString("settings.currency-symbol", "$");

        Inventory inv = Bukkit.createInventory(null, 54,
                plugin.color("&8» &e&lInbox &8| &7" + inbox.size() + " items"));

        // Border
        ItemStack glass = makeGlass();
        for (int i = 45; i < 54; i++) inv.setItem(i, glass);
        for (int i = 0; i < 9; i++) inv.setItem(i, glass);

        // Inbox entries
        int slot = 9;
        for (Map<String, Object> entry : inbox) {
            if (slot >= 45) break;
            String id = (String) entry.get("id");
            ItemStack item = (ItemStack) entry.get("item");
            double money = (double) entry.get("money");
            String reason = (String) entry.get("reason");

            ItemStack display;
            if (item != null) {
                display = item.clone();
                ItemMeta meta = display.getItemMeta();
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add("");
                lore.add(plugin.color("&7Reason: &f" + reason));
                lore.add(plugin.color("&eClick to claim!"));
                meta.setLore(lore);
                display.setItemMeta(meta);
            } else {
                // Money entry
                display = new ItemStack(Material.GOLD_NUGGET);
                ItemMeta meta = display.getItemMeta();
                meta.setDisplayName(plugin.color("&6Payment Received!"));
                meta.setLore(List.of(
                        plugin.color("&7Amount: &a" + symbol + String.format("%,.2f", money)),
                        plugin.color("&7Reason: &fItem sold"),
                        plugin.color(""),
                        plugin.color("&eClick to claim!")
                ));
                display.setItemMeta(meta);
            }
            inv.setItem(slot, display);
            slot++;
        }

        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(plugin.color("&7← &eBack to Auction House"));
        back.setItemMeta(backMeta);
        inv.setItem(49, back);

        // Claim all button
        ItemStack claimAll = new ItemStack(Material.HOPPER);
        ItemMeta claimMeta = claimAll.getItemMeta();
        claimMeta.setDisplayName(plugin.color("&a&lClaim All"));
        claimMeta.setLore(List.of(plugin.color("&7Click to claim everything!")));
        claimAll.setItemMeta(claimMeta);
        inv.setItem(47, claimAll);

        player.openInventory(inv);
        plugin.getGuiTracker().trackInbox(player, inbox);
    }

    private ItemStack makeGlass() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }
}
