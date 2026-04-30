package dev.auction.auctionhouse.gui;

import dev.auction.auctionhouse.models.SortType;
import org.bukkit.entity.Player;

import java.util.*;

public class GUITracker {

    public enum GUIType { MAIN, CONFIRM, INBOX }

    private final Map<UUID, PlayerGUIState> states = new HashMap<>();

    public void track(Player player, int page, SortType sort, String search) {
        states.put(player.getUniqueId(), new PlayerGUIState(GUIType.MAIN, page, sort, search, null, null));
    }

    public void trackConfirm(Player player, String listingId) {
        PlayerGUIState current = states.getOrDefault(player.getUniqueId(), new PlayerGUIState(GUIType.MAIN, 0, SortType.NEWEST, null, null, null));
        states.put(player.getUniqueId(), new PlayerGUIState(GUIType.CONFIRM, current.page, current.sort, current.search, listingId, null));
    }

    public void trackInbox(Player player, List<Map<String, Object>> inbox) {
        PlayerGUIState current = states.getOrDefault(player.getUniqueId(), new PlayerGUIState(GUIType.MAIN, 0, SortType.NEWEST, null, null, null));
        states.put(player.getUniqueId(), new PlayerGUIState(GUIType.INBOX, current.page, current.sort, current.search, null, inbox));
    }

    public void remove(Player player) {
        states.remove(player.getUniqueId());
    }

    public PlayerGUIState getState(Player player) {
        return states.get(player.getUniqueId());
    }

    public static class PlayerGUIState {
        public final GUIType type;
        public final int page;
        public final SortType sort;
        public final String search;
        public final String confirmListingId;
        public final List<Map<String, Object>> inboxEntries;

        public PlayerGUIState(GUIType type, int page, SortType sort, String search, String confirmListingId, List<Map<String, Object>> inboxEntries) {
            this.type = type;
            this.page = page;
            this.sort = sort;
            this.search = search;
            this.confirmListingId = confirmListingId;
            this.inboxEntries = inboxEntries;
        }
    }
}
