package uk.mangostudios.finditemaddon.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.jetbrains.annotations.Nullable;
import uk.mangostudios.finditemaddon.FindItemAddOn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LocationUtil {

    private static final List<Material> DAMAGING_BLOCKS = new ArrayList<>();
    private static final List<Material> NON_SUFFOCATING_BLOCKS = new ArrayList<>();
    private static final int MAX_SEARCH_RADIUS = 3; // Search within a 3-block radius
    private static final int MAX_HEIGHT_CHECK = 5; // Check up to 5 blocks above/below
    private static final int BELOW_SAFE_BLOCK_CHECK_LIMIT = 20; // Max depth to check for safe ground

    static {
        // Initialize damaging blocks
        DAMAGING_BLOCKS.addAll(Arrays.asList(
                Material.LAVA, Material.CACTUS, Material.CAMPFIRE, Material.SOUL_CAMPFIRE,
                Material.MAGMA_BLOCK, Material.FIRE, Material.SOUL_FIRE,
                Material.SWEET_BERRY_BUSH, Material.WITHER_ROSE, Material.END_PORTAL
        ));

        // Initialize non-suffocating blocks
        NON_SUFFOCATING_BLOCKS.add(Material.AIR);
        NON_SUFFOCATING_BLOCKS.addAll(Arrays.stream(Material.values())
                .filter(m -> m.toString().toLowerCase().contains("_glass") ||
                        m.toString().toLowerCase().contains("glass_pane") ||
                        m.toString().toLowerCase().contains("_slab") ||
                        m.toString().toLowerCase().contains("_sign") ||
                        m.toString().toLowerCase().contains("_stairs")).toList());
        NON_SUFFOCATING_BLOCKS.addAll(Arrays.asList(
                Material.ACACIA_LEAVES, Material.BIRCH_LEAVES, Material.DARK_OAK_LEAVES,
                Material.JUNGLE_LEAVES, Material.OAK_LEAVES, Material.SPRUCE_LEAVES,
                Material.HONEY_BLOCK, Material.BELL, Material.CHEST, Material.TRAPPED_CHEST,
                Material.HOPPER, Material.COMPOSTER, Material.GRINDSTONE, Material.STONECUTTER,
                Material.IRON_BARS, Material.END_PORTAL_FRAME, Material.PISTON_HEAD
        ));
        NON_SUFFOCATING_BLOCKS.addAll(Arrays.asList(
                Material.AZALEA_LEAVES, Material.FLOWERING_AZALEA_LEAVES
        ));
    }

    /**
     * Finds a safe location around a shop where a player can be teleported.
     * A safe location is one with a solid, non-damaging block below, and two blocks of non-suffocating space above.
     * Searches within a 3D radius around the shop and adjusts the player's facing direction to look at the shop.
     *
     * @param shopLocation The location of the shop.
     * @return A safe Location facing the shop, or null if no safe location is found.
     */
    @Nullable
    public static Location findSafeLocationAroundShop(Location shopLocation) {
        if (shopLocation == null || shopLocation.getWorld() == null) {
            return null; // Invalid location or world
        }

        Location roundedShopLoc = getRoundedDestination(shopLocation);
        World world = roundedShopLoc.getWorld();
        int centerX = roundedShopLoc.getBlockX();
        int centerY = roundedShopLoc.getBlockY();
        int centerZ = roundedShopLoc.getBlockZ();

        // Search in a 3D radius around the shop
        for (int dy = -MAX_HEIGHT_CHECK; dy <= MAX_HEIGHT_CHECK; dy++) {
            for (int dx = -MAX_SEARCH_RADIUS; dx <= MAX_SEARCH_RADIUS; dx++) {
                for (int dz = -MAX_SEARCH_RADIUS; dz <= MAX_SEARCH_RADIUS; dz++) {
                    // Skip the shop's own location
                    if (dx == 0 && dy == 0 && dz == 0) continue;

                    int x = centerX + dx;
                    int y = centerY + dy;
                    int z = centerZ + dz;

                    // Ensure the location is within world borders
                    if (!isWithinWorldBorders(world, x, z)) continue;

                    Location candidateLoc = new Location(world, x + 0.5, y, z + 0.5);
                    if (isSafeLocation(candidateLoc)) {
                        // Adjust the location to face the shop
                        return lookAt(candidateLoc, roundedShopLoc);
                    }
                }
            }
        }

        // Fallback: Check directly above the shop
        for (int dy = 1; dy <= MAX_HEIGHT_CHECK; dy++) {
            Location candidateLoc = new Location(world, centerX + 0.5, centerY + dy, centerZ + 0.5);
            if (isSafeLocation(candidateLoc)) {
                return lookAt(candidateLoc, roundedShopLoc);
            }
        }

        return null; // No safe location found
    }

    /**
     * Checks if a location is safe for a player to teleport to.
     * A safe location has a non-damaging, solid block below and two blocks of non-suffocating space above.
     *
     * @param loc The location to check.
     * @return True if the location is safe, false otherwise.
     */
    private static boolean isSafeLocation(Location loc) {
        World world = loc.getWorld();
        int x = loc.getBlockX();
        int z = loc.getBlockZ();

        // Check the two blocks where the player will stand (feet and head)
        Location feetLoc = new Location(world, x, loc.getBlockY(), z);
        Location headLoc = new Location(world, x, loc.getBlockY() + 1, z);

        if (isBlockSuffocating(feetLoc) || isBlockSuffocating(headLoc)) {
            return false; // Player would suffocate
        }

        // Check for a solid, non-damaging block below
        Location belowLoc = null;
        for (int i = 1; i <= BELOW_SAFE_BLOCK_CHECK_LIMIT; i++) {
            belowLoc = new Location(world, x, loc.getBlockY() - i, z);
            Material belowType = belowLoc.getBlock().getType();
            if (belowType.isAir() || belowType == FindItemAddOn.getQsApiInstance().getShopSignMaterial()) {
                continue; // Skip air or shop signs
            }
            if (isBlockDamaging(belowLoc)) {
                return false; // Damaging block found
            }
            // Solid block found, check if it's safe
            loc.setY(belowLoc.getY() + 1); // Adjust to stand on the block
            return true;
        }

        return false; // No solid ground found within limit
    }

    /**
     * Checks if a coordinate is within the world's borders.
     *
     * @param world The world to check.
     * @param x     The X coordinate.
     * @param z     The Z coordinate.
     * @return True if within borders, false otherwise.
     */
    private static boolean isWithinWorldBorders(World world, int x, int z) {
        WorldBorder border = world.getWorldBorder();
        double size = border.getSize() / 2;
        Location center = border.getCenter();
        double minX = center.getX() - size;
        double maxX = center.getX() + size;
        double minZ = center.getZ() - size;
        double maxZ = center.getZ() + size;
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    /**
     * Adjusts a location's yaw and pitch to face another location.
     *
     * @param loc    The location to adjust.
     * @param lookat The target location to face.
     * @return The adjusted location.
     */
    private static Location lookAt(Location loc, Location lookat) {
        loc = loc.clone();
        double dx = lookat.getX() - loc.getX();
        double dy = lookat.getY() - loc.getY();
        double dz = lookat.getZ() - loc.getZ();

        if (dx != 0) {
            loc.setYaw((float) (dx < 0 ? 1.5 * Math.PI : 0.5 * Math.PI));
            loc.setYaw(loc.getYaw() - (float) Math.atan(dz / dx));
        } else if (dz < 0) {
            loc.setYaw((float) Math.PI);
        }

        double dxz = Math.sqrt(dx * dx + dz * dz);
        loc.setPitch((float) -Math.atan(dy / dxz));

        loc.setYaw(-loc.getYaw() * 180f / (float) Math.PI);
        loc.setPitch(loc.getPitch() * 180f / (float) Math.PI);
        return loc;
    }

    private static boolean isBlockDamaging(Location loc) {
        return DAMAGING_BLOCKS.contains(loc.getBlock().getType());
    }

    private static boolean isBlockSuffocating(Location loc) {
        return !NON_SUFFOCATING_BLOCKS.contains(loc.getBlock().getType());
    }

    private static Location getRoundedDestination(Location loc) {
        World world = loc.getWorld();
        int x = loc.getBlockX();
        int y = (int) Math.round(loc.getY());
        int z = loc.getBlockZ();
        return new Location(world, x + 0.5, y, z + 0.5, loc.getYaw(), loc.getPitch());
    }

    public static double calculateDistance3D(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2) + Math.pow(z2 - z1, 2));
    }
}