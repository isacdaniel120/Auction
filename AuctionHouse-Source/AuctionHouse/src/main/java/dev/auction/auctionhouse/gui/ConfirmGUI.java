package dev.auction.auctionhouse.gui;

import dev.auction.auctionhouse.AuctionHouse;
import dev.auction.auctionhouse.models.AuctionListing;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ConfirmGUI {

    private final AuctionHouse plugin;

    public ConfirmGUI(AuctionHouse plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, AuctionListing listing) {
        String symbol = plugin.getConfig().getString("settings.currency-symbol", "$");
        Inventory inv = Bukkit.createInventory(null, 27,
                plugin.color("&8» &6Confirm Purchase"));

        // Fill with black glass
        ItemStack black = makeGlass(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) inv.setItem(i, black);

        // Item being purchased (center)
        ItemStack display = listing.getItem().clone();
        ItemMeta itemMeta = display.getItemMeta();
        List<String> lore = itemMeta.hasLore() ? new java.util.ArrayList<>(itemMeta.getLore()) : new java.util.ArrayList<>();
        lore.add("");
        lore.add(plugin.color("&6Price: &a" + symbol + String.format("%,.2f", listing.getPrice())));
        lore.add(plugin.color("&7Seller: &f" + listing.getSellerName()));
        itemMeta.setLore(lore);
        display.setItemMeta(itemMeta);
        inv.setItem(13, display);

        // Confirm button
        ItemStack confirm = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName(plugin.color("&a&lConfirm Purchase"));
        confirmMeta.setLore(List.of(
                plugin.color("&7Click to confirm buying"),
                plugin.color("&7this item for &a" + symbol + String.format("%,.2f", listing.getPrice()))
        ));
        confirmMeta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        confirmMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        confirm.setItemMeta(confirmMeta);
        for (int slot : new int[]{10, 11, 12}) inv.setItem(slot, confirm);

        // Cancel button
        ItemStack cancel = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName(plugin.color("&c&lCancel"));
        cancelMeta.setLore(List.of(plugin.color("&7Click to go back")));
        cancel.setItemMeta(cancelMeta);
        for (int slot : new int[]{14, 15, 16}) inv.setItem(slot, cancel);

        player.openInventory(inv);
        plugin.getGuiTracker().trackConfirm(player, listing.getId());
    }

    private ItemStack makeGlass(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }
}
