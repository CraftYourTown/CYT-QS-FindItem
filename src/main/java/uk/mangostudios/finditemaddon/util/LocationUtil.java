package uk.mangostudios.finditemaddon.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Nullable;
import uk.mangostudios.finditemaddon.FindItemAddOn;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Utility class for handling location-related operations in the FindItemAddOn plugin.
 * Provides methods to find safe teleportation locations around shops and calculate distances.
 */
public class LocationUtil {

    private static final Set<Material> DAMAGING_BLOCKS = new HashSet<>();
    private static final Set<Material> NON_SUFFOCATING_BLOCKS = new HashSet<>();
    private static final int MAX_DOWNWARD_SEARCH_LIMIT = 20;
    private static final int[][] ADJACENT_OFFSETS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    static {
        // Damaging blocks
        DAMAGING_BLOCKS.addAll(Arrays.asList(
                Material.LAVA, Material.CACTUS, Material.CAMPFIRE, Material.SOUL_CAMPFIRE,
                Material.MAGMA_BLOCK, Material.FIRE, Material.SOUL_FIRE, Material.SWEET_BERRY_BUSH,
                Material.WITHER_ROSE, Material.END_PORTAL
        ));

        // Non-suffocating blocks (excluding chests to prevent teleporting into them)
        NON_SUFFOCATING_BLOCKS.add(Material.AIR);

        // Stained Glass
        NON_SUFFOCATING_BLOCKS.addAll(Arrays.asList(
                Material.WHITE_STAINED_GLASS, Material.ORANGE_STAINED_GLASS, Material.MAGENTA_STAINED_GLASS,
                Material.LIGHT_BLUE_STAINED_GLASS, Material.YELLOW_STAINED_GLASS, Material.LIME_STAINED_GLASS,
                Material.PINK_STAINED_GLASS, Material.GRAY_STAINED_GLASS, Material.LIGHT_GRAY_STAINED_GLASS,
                Material.CYAN_STAINED_GLASS, Material.PURPLE_STAINED_GLASS, Material.BLUE_STAINED_GLASS,
                Material.BROWN_STAINED_GLASS, Material.GREEN_STAINED_GLASS, Material.RED_STAINED_GLASS,
                Material.BLACK_STAINED_GLASS
        ));

        // Stained Glass Panes
        NON_SUFFOCATING_BLOCKS.addAll(Arrays.asList(
                Material.WHITE_STAINED_GLASS_PANE, Material.ORANGE_STAINED_GLASS_PANE, Material.MAGENTA_STAINED_GLASS_PANE,
                Material.LIGHT_BLUE_STAINED_GLASS_PANE, Material.YELLOW_STAINED_GLASS_PANE, Material.LIME_STAINED_GLASS_PANE,
                Material.PINK_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE, Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                Material.CYAN_STAINED_GLASS_PANE, Material.PURPLE_STAINED_GLASS_PANE, Material.BLUE_STAINED_GLASS_PANE,
                Material.BROWN_STAINED_GLASS_PANE, Material.GREEN_STAINED_GLASS_PANE, Material.RED_STAINED_GLASS_PANE,
                Material.BLACK_STAINED_GLASS_PANE
        ));

        // Leaves (includes Azalea leaves in 1.21)
        NON_SUFFOCATING_BLOCKS.addAll(Tag.LEAVES.getValues());
        NON_SUFFOCATING_BLOCKS.addAll(Arrays.asList(
                Material.AZALEA_LEAVES, Material.FLOWERING_AZALEA_LEAVES
        ));

        // Slabs
        NON_SUFFOCATING_BLOCKS.addAll(Tag.SLABS.getValues());

        // Wall Signs
        NON_SUFFOCATING_BLOCKS.addAll(Tag.WALL_SIGNS.getValues());

        // Stairs
        NON_SUFFOCATING_BLOCKS.addAll(Tag.STAIRS.getValues());

        // Miscellaneous non-suffocating blocks (excluding chests)
        NON_SUFFOCATING_BLOCKS.addAll(Arrays.asList(
                Material.HONEY_BLOCK, Material.BELL, Material.HOPPER, Material.COMPOSTER,
                Material.GRINDSTONE, Material.STONECUTTER, Material.IRON_BARS,
                Material.END_PORTAL_FRAME, Material.PISTON_HEAD
        ));
    }

    /**
     * Finds a safe location around a shop for player teleportation.
     * Checks adjacent blocks to ensure the location is non-damaging, non-suffocating, and clear.
     *
     * @param shopLocation The location of the shop block.
     * @return A safe Location for teleportation, or null if none is found.
     */
    @Nullable
    public static Location findSafeLocationAroundShop(Location shopLocation) {
        if (!isValidShopLocation(shopLocation)) {
            return null;
        }

        Location roundedShopLocation = getRoundedDestination(shopLocation);

        for (int[] offset : ADJACENT_OFFSETS) {
            Location candidate = new Location(
                    roundedShopLocation.getWorld(),
                    roundedShopLocation.getX() + offset[0],
                    roundedShopLocation.getY(),
                    roundedShopLocation.getZ() + offset[1]
            );

            // Check if the feet position is clear (AIR or WALL_SIGN)
            Material feetMaterial = candidate.getBlock().getType();
            if (feetMaterial != Material.AIR && !Tag.WALL_SIGNS.isTagged(feetMaterial)) {
                continue;
            }

            // Check if the head position is non-suffocating and provides clearance
            Location headPosition = candidate.clone().add(0, 1, 0);
            Block headBlock = headPosition.getBlock();
            if (isBlockSuffocating(headPosition) || !isBlockPassable(headBlock)) {
                continue;
            }

            // Search for a safe block below
            Location safeLocation = findSafeBlockBelow(candidate);
            if (safeLocation == null) {
                continue;
            }

            return orientPlayer(safeLocation, roundedShopLocation);
        }

        return null;
    }

    /**
     * Searches for a safe block below the given location within a limit.
     *
     * @param startLoc The starting location to check below.
     * @return A safe Location above a solid, non-damaging block, or null if none found.
     */
    @Nullable
    private static Location findSafeBlockBelow(Location startLoc) {
        Location currentLoc = startLoc.clone();
        for (int i = 1; i <= MAX_DOWNWARD_SEARCH_LIMIT; i++) {
            currentLoc.setY(startLoc.getY() - i);
            Block blockBelow = currentLoc.getBlock();
            Material blockType = blockBelow.getType();

            // Skip air or shop sign blocks
            if (isAirOrShopSign(blockType)) {
                continue;
            }

            // Ensure the block is non-damaging and solid
            if (!DAMAGING_BLOCKS.contains(blockType) && blockBelow.isSolid()) {
                currentLoc.setY(currentLoc.getY() + 1);
                return getRoundedDestination(currentLoc);
            }

            break;
        }
        return null;
    }

    /**
     * Checks if the block type is air or a shop sign.
     *
     * @param type The material type to check.
     * @return True if the block is air or a shop sign.
     */
    private static boolean isAirOrShopSign(Material type) {
        return type == Material.AIR ||
                type == Material.CAVE_AIR ||
                type == Material.VOID_AIR ||
                type == FindItemAddOn.getQsApiInstance().getShopSignMaterial();
    }

    /**
     * Checks if a block at the given location is suffocating.
     *
     * @param loc The location to check.
     * @return True if the block is suffocating (not in non-suffocating blocks list).
     */
    private static boolean isBlockSuffocating(Location loc) {
        return !NON_SUFFOCATING_BLOCKS.contains(loc.getBlock().getType());
    }

    /**
     * Checks if a block is passable and provides enough clearance for the player's head.
     *
     * @param block The block to check.
     * @return True if the block is passable (e.g., not a bottom slab).
     */
    private static boolean isBlockPassable(Block block) {
        Material type = block.getType();
        // Check for bottom slabs (slabs in the lower half of the block space)
        if (Tag.SLABS.isTagged(type)) {
            org.bukkit.block.data.type.Slab slabData = (org.bukkit.block.data.type.Slab) block.getBlockData();
            return slabData.getType() != org.bukkit.block.data.type.Slab.Type.BOTTOM;
        }
        return block.isPassable();
    }

    /**
     * Validates if the shop location is valid and its chunk is loaded.
     *
     * @param location The location to validate.
     * @return True if the location is valid and the chunk is loaded.
     */
    private static boolean isValidShopLocation(Location location) {
        return location != null && location.getWorld() != null &&
                location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    /**
     * Orients the player to face the target location.
     *
     * @param playerLoc The player's current location.
     * @param targetLoc The location to face.
     * @return The updated location with adjusted yaw and pitch.
     */
    private static Location orientPlayer(Location playerLoc, Location targetLoc) {
        playerLoc = playerLoc.clone();
        double deltaX = targetLoc.getX() - playerLoc.getX();
        double deltaZ = targetLoc.getZ() - playerLoc.getZ();
        double deltaY = targetLoc.getY() - playerLoc.getY();

        double distanceXZ = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float yaw = (float) (Math.atan2(deltaZ, deltaX) * 180 / Math.PI - 90);
        float pitch = (float) (-Math.atan2(deltaY, distanceXZ) * 180 / Math.PI);

        playerLoc.setYaw(yaw);
        playerLoc.setPitch(pitch);
        return playerLoc;
    }

    /**
     * Rounds the location to the center of the block.
     *
     * @param loc The location to round.
     * @return A new Location centered on the block.
     */
    private static Location getRoundedDestination(Location loc) {
        World world = loc.getWorld();
        int x = loc.getBlockX();
        int y = (int) Math.round(loc.getY());
        int z = loc.getBlockZ();
        return new Location(world, x + 0.5, y, z + 0.5, loc.getYaw(), loc.getPitch());
    }

    /**
     * Calculates the 3D Euclidean distance between two points specified by their coordinates.
     *
     * @param x1 X-coordinate of the first point.
     * @param y1 Y-coordinate of the first point.
     * @param z1 Z-coordinate of the first point.
     * @param x2 X-coordinate of the second point.
     * @param y2 Y-coordinate of the second point.
     * @param z2 Z-coordinate of the second point.
     * @return The 3D Euclidean distance between the two points.
     */
    public static double calculateDistance3D(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2) + Math.pow(z2 - z1, 2));
    }
}