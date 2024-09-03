package uk.mangostudios.finditemaddon.commands.impl;

import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Command;
import uk.mangostudios.finditemaddon.cache.HiddenShopsCache;

public class HideAllShopsCommand extends AbstractCommand {

    @Command("finditem|shopsearch|searchshop manage hide-all")
    private void onHide(Player player) {
        HiddenShopsCache.getInstance().hideAllShops(player);
    }

    @Command("finditem|shopsearch|searchshop manage unhide-all")
    private void onUnhide(Player player) {
        HiddenShopsCache.getInstance().unhideAllShops(player);
    }

}
