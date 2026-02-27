package uk.mangostudios.finditemaddon.commands.impl;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import uk.mangostudios.finditemaddon.FindItemAddOn;
import uk.mangostudios.finditemaddon.external.QuickShopHandler;
import uk.mangostudios.finditemaddon.gui.AllShopsGui;
import uk.mangostudios.finditemaddon.gui.impl.ShopItem;
import uk.mangostudios.finditemaddon.util.Colourify;

import java.util.Comparator;
import java.util.List;

public class ViewAllCommand extends AbstractCommand {

    @Command("finditem|shopsearch|searchshop view-all <mode>")
    private void onViewAll(Player player, @Argument("mode") String mode) {
        boolean toBuy;
        String lower = mode.toLowerCase();
        if (lower.equals("to-buy") || lower.equals("tobuy") || lower.equals("buy")) {
            toBuy = true;
        } else if (lower.equals("to-sell") || lower.equals("tosell") || lower.equals("sell")) {
            toBuy = false;
        } else {
            player.sendMessage(Colourify.colour(FindItemAddOn.getConfigProvider().PLUGIN_PREFIX + "<red>Usage: /finditem view-all to-buy|to-sell"));
            return;
        }

        player.sendMessage(Colourify.colour(FindItemAddOn.getConfigProvider().PLUGIN_PREFIX + FindItemAddOn.getConfigProvider().SHOP_SEARCH_LOADING_MSG));

        QuickShopHandler quickShopApi = QuickShopHandler.getInstance();

        // Use cached filtered shops from QuickShopHandler (fast). Caller must still perform per-player checks.
        List<com.ghostchu.quickshop.api.shop.Shop> allShops = quickShopApi.getAllShopsFiltered(toBuy);
        List<ShopItem> shopItems = quickShopApi.getAllShopItems(toBuy);

        // sort alphabetically by item display name
        shopItems.sort(Comparator.comparing(ShopItem::itemName));

        if (shopItems.isEmpty()) {
            if (FindItemAddOn.getConfigProvider().NO_SHOP_FOUND_MSG != null && !FindItemAddOn.getConfigProvider().NO_SHOP_FOUND_MSG.isEmpty()) {
                player.sendMessage(Colourify.colour(FindItemAddOn.getConfigProvider().PLUGIN_PREFIX + FindItemAddOn.getConfigProvider().NO_SHOP_FOUND_MSG));
            }
            return;
        }

        AllShopsGui.open(player, toBuy, shopItems);
    }
}
