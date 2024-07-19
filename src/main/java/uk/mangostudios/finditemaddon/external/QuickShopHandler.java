/**
 * QSFindItemAddOn: An Minecraft add-on plugin for the QuickShop Hikari
 * and Reremake Shop plugins for Spigot server platform.
 * Copyright (C) 2021  myzticbean
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see https://www.gnu.org/licenses/.
 */
package uk.mangostudios.finditemaddon.external;

import com.ghostchu.quickshop.api.QuickShopAPI;
import com.ghostchu.quickshop.api.shop.Shop;
import com.ghostchu.quickshop.api.shop.permission.BuiltInShopPermission;
import com.ghostchu.quickshop.util.Util;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import uk.mangostudios.finditemaddon.FindItemAddOn;
import uk.mangostudios.finditemaddon.gui.impl.ShopItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class QuickShopHandler {

    private final QuickShopAPI api;
    private final Cache<ItemStack, List<ShopItem>> searchedItemStacksToBuy = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();
    private final Cache<String, List<ShopItem>> searchedStringsToBuy = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();
    private final Cache<ItemStack, List<ShopItem>> searchedItemStacksToSell = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();
    private final Cache<String, List<ShopItem>> searchedStringsToSell = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    private static QuickShopHandler instance;

    public QuickShopHandler() {
        api = QuickShopAPI.getInstance();
        instance = this;
    }

    public List<ShopItem> findItemBasedOnTypeFromAllShops(ItemStack item, boolean toBuy, Player searchingPlayer) {
        if (toBuy) {
            if (searchedItemStacksToBuy.getIfPresent(item) != null) {
                return searchedItemStacksToBuy.getIfPresent(item);
            }
        } else {
            if (searchedItemStacksToSell.getIfPresent(item) != null) {
                return searchedItemStacksToSell.getIfPresent(item);
            }
        }

        List<ShopItem> shopsFoundList = new ArrayList<>();
        List<com.ghostchu.quickshop.api.shop.Shop> allShops = fetchAllShopsFromQS();
        for (com.ghostchu.quickshop.api.shop.Shop shopIterator : allShops) {
            if (shopIterator.playerAuthorize(searchingPlayer.getUniqueId(), BuiltInShopPermission.SEARCH)
                    && (!FindItemAddOn.getConfigProvider().getBlacklistedWorlds().contains(shopIterator.getLocation().getWorld())
                    && shopIterator.getItem().getType().equals(item.getType())
                    && (toBuy ? shopIterator.isSelling() : shopIterator.isBuying()))
            ) {
                int stockOrSpace = (toBuy ? getRemainingStockOrSpaceFromShopCache(shopIterator, true) : getRemainingStockOrSpaceFromShopCache(shopIterator, false));
                if (stockOrSpace <= 0) {
                    continue;
                }

                // ensure shop owner has enough balance to sell the item (if selling)
                OfflinePlayer shopOwner = Bukkit.getOfflinePlayer(shopIterator.getOwner().getUniqueId());
                if (!toBuy && !FindItemAddOn.getInstance().getEconomy().has(shopOwner, shopIterator.getPrice())) {
                    continue;
                }

                shopsFoundList.add(new ShopItem(
                        shopIterator.getPrice(),
                        QuickShopHandler.processStockOrSpace(stockOrSpace),
                        shopIterator.getOwner().getUniqueIdOptional().orElse(new UUID(0, 0)),
                        shopIterator.getLocation(),
                        shopIterator.getItem(),
                        toBuy
                ));
            }
        }

        if (toBuy) {
            this.searchedItemStacksToBuy.put(item, shopsFoundList);
        } else {
            this.searchedItemStacksToSell.put(item, shopsFoundList);
        }
        return handleShopSorting(toBuy, shopsFoundList);
    }

    public List<ShopItem> findItemBasedOnDisplayNameFromAllShops(String matcher, boolean toBuy, Player searchingPlayer) {
        if (toBuy) {
            if (searchedStringsToBuy.getIfPresent(matcher) != null) {
                return searchedStringsToBuy.getIfPresent(matcher);
            }
        } else {
            if (searchedStringsToSell.getIfPresent(matcher) != null) {
                return searchedStringsToSell.getIfPresent(matcher);
            }
        }

        List<ShopItem> shopsFoundList = new ArrayList<>();
        List<com.ghostchu.quickshop.api.shop.Shop> allShops = fetchAllShopsFromQS();
        for (com.ghostchu.quickshop.api.shop.Shop shopIterator : allShops) {
            if (shopIterator.playerAuthorize(searchingPlayer.getUniqueId(), BuiltInShopPermission.SEARCH)
                    && !FindItemAddOn.getConfigProvider().getBlacklistedWorlds().contains(shopIterator.getLocation().getWorld())
                    && PlainTextComponentSerializer.plainText().serialize(shopIterator.getItem().displayName()).toLowerCase().contains(matcher.replace("_", " ").toLowerCase())
                    && (toBuy ? shopIterator.isSelling() : shopIterator.isBuying())
            ) {
                int stockOrSpace = (toBuy ? getRemainingStockOrSpaceFromShopCache(shopIterator, true) : getRemainingStockOrSpaceFromShopCache(shopIterator, false));
                if (stockOrSpace <= 0) {
                    continue;
                }

                // ensure shop owner has enough balance to sell the item (if selling)
                OfflinePlayer shopOwner = Bukkit.getOfflinePlayer(shopIterator.getOwner().getUniqueId());
                if (!toBuy && !FindItemAddOn.getInstance().getEconomy().has(shopOwner, shopIterator.getPrice())) {
                    continue;
                }

                shopsFoundList.add(new ShopItem(
                        shopIterator.getPrice(),
                        QuickShopHandler.processStockOrSpace(stockOrSpace),
                        shopIterator.getOwner().getUniqueIdOptional().orElse(new UUID(0, 0)),
                        shopIterator.getLocation(),
                        shopIterator.getItem(),
                        toBuy
                ));
            }
        }

        if (toBuy) {
            this.searchedStringsToBuy.put(matcher, shopsFoundList);
        } else {
            this.searchedStringsToSell.put(matcher, shopsFoundList);
        }
        return handleShopSorting(toBuy, shopsFoundList);
    }

    @NotNull
    static List<ShopItem> handleShopSorting(boolean toBuy, List<ShopItem> shopsFoundList) {
        if (!shopsFoundList.isEmpty()) {
            int sortingMethod = 2;
            try {
                sortingMethod = FindItemAddOn.getConfigProvider().SHOP_SORTING_METHOD;
            } catch (Exception ignored) {
            }
            return QuickShopHandler.sortShops(sortingMethod, shopsFoundList, toBuy);
        }
        return shopsFoundList;
    }

    private List<com.ghostchu.quickshop.api.shop.Shop> fetchAllShopsFromQS() {
        List<com.ghostchu.quickshop.api.shop.Shop> allShops;
        if (FindItemAddOn.getConfigProvider().SEARCH_LOADED_SHOPS_ONLY) {
            allShops = new ArrayList<>(api.getShopManager().getLoadedShops());
        } else {
            allShops = getAllShops();
        }
        return allShops;
    }

    public List<Shop> getAllShopsFor(Player player) {
        return api.getShopManager().getAllShops(player.getUniqueId());
    }

    public Material getShopSignMaterial() {
        return com.ghostchu.quickshop.util.Util.getSignMaterial();
    }

    public com.ghostchu.quickshop.api.shop.Shop findShopAtLocation(Location loc) {
        return api.getShopManager().getShopIncludeAttached(loc);
    }

    public boolean isShopOwnerCommandRunner(Player player, com.ghostchu.quickshop.api.shop.Shop shop) {
        return shop.getOwner().getUniqueId() == player.getUniqueId();
    }

    public List<com.ghostchu.quickshop.api.shop.Shop> getAllShops() {
        return api.getShopManager().getAllShops();
    }

    public UUID convertNameToUuid(String playerName) {
        return api.getPlayerFinder().name2Uuid(playerName);
    }

    private int getRemainingStockOrSpaceFromShopCache(com.ghostchu.quickshop.api.shop.Shop shop, boolean fetchRemainingStock) {
        Util.ensureThread(true);
        return (fetchRemainingStock ? shop.getRemainingStock() : shop.getRemainingSpace());
    }

    static List<ShopItem> sortShops(int sortingMethod, List<ShopItem> shopsFoundList, boolean toBuy) {
        switch (sortingMethod) {
            // Random
            case 1 -> Collections.shuffle(shopsFoundList);
            // Based on prices (lower to higher)
            case 2 -> shopsFoundList.sort(Comparator.comparing(ShopItem::shopPrice));
            // Based on stocks (higher to lower)
            case 3 -> {
                shopsFoundList.sort(Comparator.comparing(ShopItem::remainingStockOrSpace));
                Collections.reverse(shopsFoundList);
            }
            default -> {
                shopsFoundList.sort(Comparator.comparing(ShopItem::shopPrice));
            }
        }
        return shopsFoundList;
    }

    static int processStockOrSpace(int stockOrSpace) {
        if (stockOrSpace == -1)
            return Integer.MAX_VALUE;
        return stockOrSpace;
    }

    public static QuickShopHandler getInstance() {
        return instance;
    }

}
