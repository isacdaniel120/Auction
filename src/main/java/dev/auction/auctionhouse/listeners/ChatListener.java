package dev.auction.auctionhouse.listeners;

import dev.auction.auctionhouse.AuctionHouse;
import dev.auction.auctionhouse.managers.ChatSearchTracker;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final AuctionHouse plugin;
    private final ChatSearchTracker tracker;

    public ChatListener(AuctionHouse plugin, ChatSearchTracker tracker) {
        this.plugin = plugin;
        this.tracker = tracker;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        if (!tracker.isSearching(player)) return;

        e.setCancelled(true);
        String query = e.getMessage().trim();
        var sort = tracker.getSort(player);
        tracker.stop(player);

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (query.equalsIgnoreCase("cancel")) {
                player.sendMessage(plugin.color("&cSearch cancelled."));
                plugin.getAhGui().open(player, 0, sort, null);
            } else {
                plugin.getAhGui().open(player, 0, sort, query);
            }
        });
    }
}
