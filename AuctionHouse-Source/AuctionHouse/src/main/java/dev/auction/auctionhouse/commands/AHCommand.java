package dev.auction.auctionhouse.commands;

import dev.auction.auctionhouse.AuctionHouse;
import dev.auction.auctionhouse.managers.AuctionManager;
import dev.auction.auctionhouse.models.SortType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class AHCommand implements CommandExecutor, TabCompleter {

    private final AuctionHouse plugin;
    private final AuctionManager manager;

    public AHCommand(AuctionHouse plugin, AuctionManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        String prefix = plugin.getConfig().getString("messages.prefix", "");

        if (!player.hasPermission("ah.use")) {
            player.sendMessage(plugin.color(prefix + plugin.getConfig().getString("messages.no-permission")));
            return true;
        }

        // /ah — open main GUI
        if (args.length == 0) {
            plugin.getAhGui().open(player, 0, SortType.NEWEST, null);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "sell" -> {
                if (!player.hasPermission("ah.sell")) {
                    player.sendMessage(plugin.color(prefix + plugin.getConfig().getString("messages.no-permission")));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(plugin.color(prefix + "&cUsage: /ah sell <price>"));
                    return true;
                }
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand == null || hand.getType().isAir()) {
                    player.sendMessage(plugin.color(prefix + plugin.getConfig().getString("messages.hand-empty")));
                    return true;
                }
                double price;
                try {
                    price = Double.parseDouble(args[1]);
                } catch (NumberFormatException e) {
                    player.sendMessage(plugin.color(prefix + plugin.getConfig().getString("messages.invalid-price")));
                    return true;
                }
                double min = plugin.getConfig().getDouble("limits.min-price", 1);
                double max = plugin.getConfig().getDouble("limits.max-price", 1000000000);
                if (price < min) {
                    player.sendMessage(plugin.color(prefix + plugin.getConfig().getString("messages.min-price", "")
                            .replace("{min}", String.valueOf(min))));
                    return true;
                }
                if (price > max) {
                    player.sendMessage(plugin.color(prefix + plugin.getConfig().getString("messages.max-price", "")
                            .replace("{max}", String.valueOf(max))));
                    return true;
                }
                int activeListings = manager.getActiveListingCount(player.getUniqueId());
                int limit = manager.getListingLimit(player);
                if (activeListings >= limit) {
                    player.sendMessage(plugin.color(prefix + plugin.getConfig().getString("messages.listing-limit")));
                    return true;
                }
                // Take item from hand
                ItemStack toSell = hand.clone();
                toSell.setAmount(hand.getAmount());
                player.getInventory().setItemInMainHand(null);
                manager.createListing(player, toSell, price);
                String symbol = plugin.getConfig().getString("settings.currency-symbol", "$");
                player.sendMessage(plugin.color(prefix + plugin.getConfig().getString("messages.item-listed", "")
                        .replace("{price}", symbol + String.format("%,.2f", price))));
            }

            case "search" -> {
                if (args.length < 2) {
                    player.sendMessage(plugin.color(prefix + "&cUsage: /ah search <query>"));
                    return true;
                }
                String query = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                plugin.getAhGui().open(player, 0, SortType.NEWEST, query);
            }

            case "inbox" -> plugin.getInboxGui().open(player);

            case "reload" -> {
                if (!player.hasPermission("ah.reload")) {
                    player.sendMessage(plugin.color(prefix + plugin.getConfig().getString("messages.no-permission")));
                    return true;
                }
                plugin.reloadConfig();
                player.sendMessage(plugin.color(prefix + plugin.getConfig().getString("messages.reload")));
            }

            default -> {
                // Search by player name: /ah <player>
                String targetName = args[0];
                plugin.getAhGui().open(player, 0, SortType.NEWEST, targetName);
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            return List.of("sell", "search", "inbox", "reload").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
