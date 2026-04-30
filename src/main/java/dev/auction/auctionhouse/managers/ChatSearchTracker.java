package dev.auction.auctionhouse.managers;

import dev.auction.auctionhouse.models.SortType;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatSearchTracker {

    private final Map<UUID, SortType> searching = new HashMap<>();

    public void startSearch(Player player, SortType sort) {
        searching.put(player.getUniqueId(), sort);
    }

    public boolean isSearching(Player player) {
        return searching.containsKey(player.getUniqueId());
    }

    public SortType getSort(Player player) {
        return searching.getOrDefault(player.getUniqueId(), SortType.NEWEST);
    }

    public void stop(Player player) {
        searching.remove(player.getUniqueId());
    }
}
