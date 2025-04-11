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
package uk.mangostudios.finditemaddon.util;

import com.olziedev.playerwarps.api.PlayerWarpsAPI;
import com.olziedev.playerwarps.api.player.WBanned;
import com.olziedev.playerwarps.api.warp.Warp;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import uk.mangostudios.finditemaddon.FindItemAddOn;
import uk.mangostudios.finditemaddon.external.PlayerWarpsHandler;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class PlayerWarpsUtil {

    @Nullable
    public Warp findNearestWarp(Location shopLocation, UUID shopOwner) {
        List<Warp> playersWarps = PlayerWarpsHandler.getAllWarps().stream()
                .filter(warp -> warp.getWarpLocation().getWorld() != null)
                .filter(warp -> warp.getWarpLocation().getWorld().equals(shopLocation.getWorld().getName()))
                .filter(warp -> warp.getWarpPlayer().getUUID().equals(shopOwner))
                .toList();

        if (playersWarps.isEmpty()) {
            return null;
        }

        Map<Double, Warp> warpDistanceMap = new TreeMap<>();
        playersWarps.forEach(warp ->
                warpDistanceMap.put(LocationUtil.calculateDistance3D(
                        shopLocation.getX(),
                        shopLocation.getY(),
                        shopLocation.getZ(),
                        warp.getWarpLocation().getX(),
                        warp.getWarpLocation().getY(),
                        warp.getWarpLocation().getZ()
                ), warp));

        for (Map.Entry<Double, Warp> doubleWarpEntry : warpDistanceMap.entrySet()) {
            // Is the distance less than 200 blocks?
            if (doubleWarpEntry.getKey() > 200) {
                continue;
            }

            // Is the warp locked?
            if (doubleWarpEntry.getValue().isWarpLocked()) {
                continue;
            }

            return doubleWarpEntry.getValue();
        }

        return null;
    }

    public static boolean isPlayerBanned(Warp warp, Player player) {
        AtomicBoolean isBanned = new AtomicBoolean(false);

        if (warp == null) return false;

        PlayerWarpsAPI.getInstance(api -> {
            for (WBanned bannedPlayer : warp.getBanned()) {
                if (bannedPlayer.getUUID().equals(player.getUniqueId())) {
                    player.sendMessage(Colourify.colour(
                            FindItemAddOn.getConfigProvider().PLUGIN_PREFIX + FindItemAddOn.getConfigProvider().SHOP_TP_BANNED_MSG));
                    isBanned.set(true);
                    return;
                }
            }
        });

        return isBanned.get();
    }

    /**
     * Check if a player warp is locked as a player teleports to it
     *
     * @param warp   The warp to check
     * @param player The player who is teleporting (i guess)
     * @return If the warp is locked
     */
    public static boolean isWarpLocked(Warp warp, Player player) {
        if (warp == null || player == null) return false;
        AtomicBoolean result = new AtomicBoolean(false);

        PlayerWarpsAPI.getInstance(api -> {
            Warp provided = api.getPlayerWarp(warp.getWarpName(), player);
            result.set(provided.isWarpLocked());
        });

        return result.get();
    }

}