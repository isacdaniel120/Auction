package dev.auction.auctionhouse;

import dev.auction.auctionhouse.commands.AHCommand;
import dev.auction.auctionhouse.gui.AuctionHouseGUI;
import dev.auction.auctionhouse.gui.ConfirmGUI;
import dev.auction.auctionhouse.gui.GUITracker;
import dev.auction.auctionhouse.gui.InboxGUI;
import dev.auction.auctionhouse.listeners.ChatListener;
import dev.auction.auctionhouse.listeners.GUIListener;
import dev.auction.auctionhouse.managers.AuctionManager;
import dev.auction.auctionhouse.managers.ChatSearchTracker;
import dev.auction.auctionhouse.managers.DatabaseManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class AuctionHouse extends JavaPlugin {

    private Economy economy;
    private DatabaseManager databaseManager;
    private AuctionManager auctionManager;
    private GUITracker guiTracker;
    private ChatSearchTracker chatSearchTracker;
    private AuctionHouseGUI ahGui;
    private ConfirmGUI confirmGui;
    private InboxGUI inboxGui;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (!setupEconomy()) {
            getLogger().severe("Vault not found! Disabling AuctionHouse.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getDataFolder().mkdirs();

        databaseManager = new DatabaseManager(this);
        databaseManager.connect();

        auctionManager = new AuctionManager(this, databaseManager, economy);
        guiTracker = new GUITracker();
        chatSearchTracker = new ChatSearchTracker();

        ahGui = new AuctionHouseGUI(this, auctionManager);
        confirmGui = new ConfirmGUI(this);
        inboxGui = new InboxGUI(this, databaseManager, auctionManager);

        // Register listeners
        GUIListener guiListener = new GUIListener(this, auctionManager, databaseManager, ahGui, confirmGui, inboxGui);
        getServer().getPluginManager().registerEvents(guiListener, this);
        getServer().getPluginManager().registerEvents(new ChatListener(this, chatSearchTracker), this);

        // Register commands
        AHCommand ahCommand = new AHCommand(this, auctionManager);
        getCommand("ah").setExecutor(ahCommand);
        getCommand("ah").setTabCompleter(ahCommand);

        getLogger().info("AuctionHouse enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) databaseManager.disconnect();
        getLogger().info("AuctionHouse disabled.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    // Fix purchaseListing reference in GUIListener
    public boolean purchaseItem(dev.auction.auctionhouse.listeners.GUIListener ignored, org.bukkit.entity.Player player, String listingId, int page, dev.auction.auctionhouse.models.SortType sort, String search) {
        String prefix = getConfig().getString("messages.prefix", "");
        dev.auction.auctionhouse.models.AuctionListing listing = auctionManager.getListing(listingId);
        if (listing == null) {
            player.sendMessage(color(prefix + getConfig().getString("messages.listing-gone")));
            ahGui.open(player, page, sort, search);
            return false;
        }
        if (listing.getSellerUUID().equals(player.getUniqueId())) {
            player.sendMessage(color(prefix + getConfig().getString("messages.own-listing")));
            ahGui.open(player, page, sort, search);
            return false;
        }
        if (!economy.has(player, listing.getPrice())) {
            player.sendMessage(color(prefix + getConfig().getString("messages.no-money")));
            ahGui.open(player, page, sort, search);
            return false;
        }
        boolean success = auctionManager.purchaseListing(player, listingId);
        if (success) {
            String symbol = getConfig().getString("settings.currency-symbol", "$");
            player.sendMessage(color(prefix + getConfig().getString("messages.item-purchased", "")
                    .replace("{item}", listing.getItem().getType().name())
                    .replace("{price}", symbol + String.format("%,.2f", listing.getPrice()))));
        }
        ahGui.open(player, page, sort, search);
        return success;
    }

    public Economy getEconomy() { return economy; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public AuctionManager getAuctionManager() { return auctionManager; }
    public GUITracker getGuiTracker() { return guiTracker; }
    public ChatSearchTracker getChatSearchTracker() { return chatSearchTracker; }
    public AuctionHouseGUI getAhGui() { return ahGui; }
    public ConfirmGUI getConfirmGui() { return confirmGui; }
    public InboxGUI getInboxGui() { return inboxGui; }
}
