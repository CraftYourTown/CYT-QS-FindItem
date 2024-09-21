package uk.mangostudios.finditemaddon.commands.impl;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Command;
import uk.mangostudios.finditemaddon.FindItemAddOn;
import uk.mangostudios.finditemaddon.commands.CmdExecutorHandler;
import uk.mangostudios.finditemaddon.util.Colourify;

public class HandCommand extends AbstractCommand {

    private final CmdExecutorHandler cmdExecutor;

    public HandCommand(CmdExecutorHandler cmdExecutor) {
        this.cmdExecutor = cmdExecutor;
    }

    @Command("finditem|shopsearch|searchshop hand to-buy")
    private void onBuy(Player player) {
        if (player.getInventory().getItemInMainHand().getType().isAir()) {
            player.sendMessage(Colourify.colour(FindItemAddOn.getConfigProvider().HAND_EMPTY_MSG));
            return;
        }

        var item = player.getInventory().getItemInMainHand();
        String search = PlainTextComponentSerializer.plainText().serialize(item.clone().displayName());

        if (search.length() <= 3)
            player.sendMessage(Colourify.colour(FindItemAddOn.getConfigProvider().QUERY_TOO_SHORT_MSG));

        this.cmdExecutor.handleShopSearch(true, player, search);
    }

    @Command("finditem|shopsearch|searchshop hand to-sell")
    private void onSell(Player player) {
        if (player.getInventory().getItemInMainHand().getType().isAir()) {
            player.sendMessage(Colourify.colour(FindItemAddOn.getConfigProvider().HAND_EMPTY_MSG));
            return;
        }

        var item = player.getInventory().getItemInMainHand();
        String search = PlainTextComponentSerializer.plainText().serialize(item.clone().displayName());

        if (search.length() <= 3)
            player.sendMessage(Colourify.colour(FindItemAddOn.getConfigProvider().QUERY_TOO_SHORT_MSG));

        this.cmdExecutor.handleShopSearch(false, player, search);
    }

}
