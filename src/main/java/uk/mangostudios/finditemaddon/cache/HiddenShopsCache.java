package uk.mangostudios.finditemaddon.cache;

import com.ghostchu.quickshop.api.shop.Shop;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;
import uk.mangostudios.finditemaddon.FindItemAddOn;
import uk.mangostudios.finditemaddon.external.QuickShopHandler;
import uk.mangostudios.finditemaddon.storage.HiddenShopsStorage;
import uk.mangostudios.finditemaddon.storage.impl.FinePosition;
import uk.mangostudios.finditemaddon.util.Colourify;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HiddenShopsCache {

    private static HiddenShopsCache instance;

    private final Map<UUID, List<FinePosition>> hiddenShops = new ConcurrentHashMap<>();
    private final HiddenShopsStorage hiddenShopsStorage;

    public HiddenShopsCache(FindItemAddOn plugin) {
        this.hiddenShopsStorage = new HiddenShopsStorage(plugin);
        hiddenShopsStorage.load().thenAccept(hiddenShops::putAll);
        instance = this;
    }

    public void shutdown() {
        hiddenShopsStorage.saveAll(hiddenShops);
    }

    public void hideAllShops(Player player) {
        QuickShopHandler.getInstance().getAllShopsFor(player).forEach(shop -> {
            if (!(shop.getOwner().getUniqueId() == player.getUniqueId())) return;
            hiddenShops.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(new FinePosition(shop.getLocation().getX(), shop.getLocation().getY(), shop.getLocation().getZ(), shop.getLocation().getWorld().getName()));
        });
        player.sendMessage(Colourify.colour(FindItemAddOn.getConfigProvider().PLUGIN_PREFIX + FindItemAddOn.getConfigProvider().HIDDEN_ALL_SHOPS_MSG));
    }

    public void unhideAllShops(Player player) {
        hiddenShops.remove(player.getUniqueId());
        player.sendMessage(Colourify.colour(FindItemAddOn.getConfigProvider().PLUGIN_PREFIX + FindItemAddOn.getConfigProvider().UNHIDDEN_ALL_SHOPS_MSG));
    }

    public void hideShop(Player player, @Nullable Location shopLocation) {
        if (shopLocation == null) shopLocation = player.getTargetBlock(null, 5).getLocation();
        FinePosition finePosition = new FinePosition(shopLocation.getX(), shopLocation.getY(), shopLocation.getZ(), shopLocation.getWorld().getName());
        Shop shop = QuickShopHandler.getInstance().findShopAtLocation(shopLocation);

        if (shop == null) {
            player.sendMessage(Colourify.colour(FindItemAddOn.getConfigProvider().PLUGIN_PREFIX + FindItemAddOn.getConfigProvider().NOT_LOOKING_AT_SHOP_MSG));
            return;
        }

        if (!(shop.getOwner().getUniqueId() == player.getUniqueId())) {
            player.sendMessage(Colourify.colour(FindItemAddOn.getConfigProvider().PLUGIN_PREFIX + FindItemAddOn.getConfigProvider().NOT_YOUR_SHOP_MSG));
            return;
        }

        hiddenShops.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(finePosition);
        player.sendMessage(Colourify.colour(FindItemAddOn.getConfigProvider().PLUGIN_PREFIX + FindItemAddOn.getConfigProvider().HIDDEN_SHOP_MSG));
    }

    public void unhideShop(Player player, @Nullable Location shopLocation) {
        if (shopLocation == null) shopLocation = player.getTargetBlock(null, 5).getLocation();
        FinePosition finePosition = new FinePosition(shopLocation.getX(), shopLocation.getY(), shopLocation.getZ(), shopLocation.getWorld().getName());
        Shop shop = QuickShopHandler.getInstance().findShopAtLocation(shopLocation);

        if (shop == null) {
            player.sendMessage(Colourify.colour(FindItemAddOn.getConfigProvider().PLUGIN_PREFIX + FindItemAddOn.getConfigProvider().NOT_LOOKING_AT_SHOP_MSG));
            return;
        }

        if (!(shop.getOwner().getUniqueId() == player.getUniqueId())) {
            player.sendMessage(Colourify.colour(FindItemAddOn.getConfigProvider().PLUGIN_PREFIX + FindItemAddOn.getConfigProvider().NOT_YOUR_SHOP_MSG));
            return;
        }

        hiddenShops.get(player.getUniqueId()).remove(finePosition);
        player.sendMessage(Colourify.colour(FindItemAddOn.getConfigProvider().PLUGIN_PREFIX + FindItemAddOn.getConfigProvider().UNHIDDEN_SHOP_MSG));
    }

    public boolean isShopHidden(Player player, Location shopLocation) {
        if (!hiddenShops.containsKey(player.getUniqueId())) return false;
        for (FinePosition finePosition : hiddenShops.get(player.getUniqueId())) {
            return finePosition.equals(new FinePosition(shopLocation.getX(), shopLocation.getY(), shopLocation.getZ(), shopLocation.getWorld().getName()));
        }
        return false;
    }

    public static HiddenShopsCache getInstance() {
        return instance;
    }

}
