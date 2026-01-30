package uk.mangostudios.finditemaddon.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import uk.mangostudios.finditemaddon.FindItemAddOn;
import uk.mangostudios.finditemaddon.listener.HeadDatabaseApiListener;

public class ItemUtil {

    public static ItemStack get(String material) {
        if (material.startsWith("hdb-")) {
            try {
                return HeadDatabaseApiListener.getInstance().getApi().getItemHead(material.substring(4));
            } catch (NullPointerException e) {
                FindItemAddOn.getInstance().getLogger().warning("HeadDatabaseAPI is not loaded, cannot get head with id " + material);
                return new ItemStack(Material.BARRIER);
            }
        }

        try {
            return new ItemStack(Material.valueOf(material));
        } catch (IllegalArgumentException e) {
            FindItemAddOn.getInstance().getLogger().warning("Item with id " + material + " does not exist!");
            return new ItemStack(Material.BARRIER);
        }
    }
}
