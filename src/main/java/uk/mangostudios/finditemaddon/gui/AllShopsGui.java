package uk.mangostudios.finditemaddon.gui;

import com.olziedev.playerwarps.api.warp.WVisit;
import com.olziedev.playerwarps.api.warp.Warp;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.PaginatedGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import uk.mangostudios.finditemaddon.FindItemAddOn;
import uk.mangostudios.finditemaddon.cache.HiddenShopsCache;
import uk.mangostudios.finditemaddon.gui.impl.ShopItem;
import uk.mangostudios.finditemaddon.util.Colourify;
import uk.mangostudios.finditemaddon.util.ItemUtil;
import uk.mangostudios.finditemaddon.util.LocationUtil;
import uk.mangostudios.finditemaddon.util.PlayerWarpsUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AllShopsGui {

    private final PaginatedGui gui = Gui.paginated()
            .title(Component.text(""))
            .rows(6)
            .disableAllInteractions()
            .create();

    public AllShopsGui(Player player, boolean toBuy, List<ShopItem> shopList) {
        // Set the title
        String titleReplacement = toBuy ? "To Buy" : "To Sell";
        String rawTitle = FindItemAddOn.getConfigProvider().SHOP_SEARCH_GUI_TITLE == null ? "" : FindItemAddOn.getConfigProvider().SHOP_SEARCH_GUI_TITLE;
        gui.updateTitle(
                Colourify.colour(rawTitle.replace("<matcher>", titleReplacement)));

        // Add the buttons
        gui.setItem(6, 1,
                ItemBuilder.from(ItemUtil.get(FindItemAddOn.getConfigProvider().SHOP_GUI_BACK_BUTTON_MATERIAL))
                        .name(Colourify.colour(FindItemAddOn.getConfigProvider().SHOP_GUI_BACK_BUTTON_TEXT))
                        .asGuiItem(event -> gui.previous()));
        gui.setItem(6, 9,
                ItemBuilder.from(ItemUtil.get(FindItemAddOn.getConfigProvider().SHOP_GUI_NEXT_BUTTON_MATERIAL))
                        .name(Colourify.colour(FindItemAddOn.getConfigProvider().SHOP_GUI_NEXT_BUTTON_TEXT))
                        .asGuiItem(event -> gui.next()));
        gui.setItem(6, 5,
                ItemBuilder.from(ItemUtil.get(FindItemAddOn.getConfigProvider().SHOP_GUI_CLOSE_BUTTON_MATERIAL))
                        .name(Colourify.colour(FindItemAddOn.getConfigProvider().SHOP_GUI_CLOSE_BUTTON_TEXT))
                        .asGuiItem(event -> gui.close(event.getWhoClicked())));

        gui.setItem(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8), ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).name(Colourify.colour(" ")).asGuiItem());
        gui.setItem(List.of(46, 47, 48, 50, 51, 52), ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).name(Colourify.colour(" ")).asGuiItem());

        // Add the items
        shopList.forEach(shopItem -> {
            ItemStack itemStack = shopItem.item().clone();
            Warp nearestWarp = this.getNearestWarp(shopItem.shopOwner(), shopItem.shopLocation());
            double costToSearch = FindItemAddOn.getConfigProvider().COST_TO_SEARCH;

            if (nearestWarp != null) {
                // Skip if the warp is locked
                if (nearestWarp.isWarpLocked()) return;
            }

            // Is the warp hidden?
            if (HiddenShopsCache.getInstance().isShopHidden(player, shopItem.shopLocation())) {
                return;
            }

            List<String> lore = new ArrayList<>();
            List<String> rawLore = FindItemAddOn.getConfigProvider().SHOP_GUI_ITEM_LORE == null ? new ArrayList<>() : FindItemAddOn.getConfigProvider().SHOP_GUI_ITEM_LORE;
            for (String line : rawLore) {
                if (line == null) continue;
                lore.add(line
                        .replace("<price>", String.valueOf(shopItem.shopPrice()))
                        .replace("<stock>", String.valueOf(shopItem.remainingStockOrSpace()))
                        .replace("<owner>", (Bukkit.getOfflinePlayer(shopItem.shopOwner()).getName() == null ? "Unknown" : Bukkit.getOfflinePlayer(shopItem.shopOwner()).getName()))
                        .replace("<cost>", String.valueOf(costToSearch))
                        .replace("<location>",
                                "X: " + shopItem.shopLocation().getBlockX()
                                        + ", Y: " + shopItem.shopLocation().getBlockY()
                                        + ", Z: " + shopItem.shopLocation().getBlockZ())
                        .replace("<world>", shopItem.shopLocation().getWorld().getName())
                        .replace("<warp>", nearestWarp == null ? "No warp found" : nearestWarp.getWarpDisplayName()));
            }

            gui.addItem(ItemBuilder.from(itemStack)
                    .name(itemStack.displayName().decoration(TextDecoration.ITALIC, false))
                    .lore(Colourify.colour(lore))
                    .asGuiItem(inventoryClickEvent -> {
                        if (costToSearch > 0) {
                            if (!FindItemAddOn.getInstance().getEconomy().withdrawPlayer(player, costToSearch).transactionSuccess()) {
                                player.sendMessage(Colourify.colour(FindItemAddOn.getConfigProvider().NOT_ENOUGH_MONEY_MSG
                                        .replace("<cost>", String.valueOf(costToSearch))));
                                return;
                            }
                        }

                        if (PlayerWarpsUtil.isPlayerBanned(nearestWarp, player)) return;
                        if (PlayerWarpsUtil.isWarpLocked(nearestWarp, player)) return;
                        if (nearestWarp != null) {
                            final WVisit warpVisit = nearestWarp.getWarpVisit();
                            if (!warpVisit.getWarpVisited().containsKey(player.getUniqueId())) {
                                warpVisit.addWarpVisited(player.getUniqueId());
                            }
                        }

                        player.closeInventory();

                        Location safeLocationAroundShop = LocationUtil.findSafeLocationAroundShop(shopItem.shopLocation());
                        Location teleportLocation = safeLocationAroundShop == null ? (nearestWarp == null ? shopItem.shopLocation() : nearestWarp.getWarpLocation().getLocation()) : safeLocationAroundShop;
                        player.teleportAsync(teleportLocation);
                    }));
        });
    }

    public static void open(Player player, boolean toBuy, List<ShopItem> shopList) {
        CompletableFuture.supplyAsync(() -> new AllShopsGui(player, toBuy, shopList)).thenAccept(gui ->
                Bukkit.getScheduler().runTask(FindItemAddOn.getInstance(), () -> gui.gui.open(player))
        ).exceptionally(throwable -> {
            FindItemAddOn.getInstance().getLogger().severe("Failed to open AllShopsGui: " + throwable.getMessage());
            return null;
        });
    }

    private @Nullable Warp getNearestWarp(UUID shopOwner, Location location) {
        return new PlayerWarpsUtil().findNearestWarp(location, shopOwner);
    }

}
