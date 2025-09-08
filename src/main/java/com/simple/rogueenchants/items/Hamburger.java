package com.simple.rogueenchants.items;

import com.simple.rogueenchants.RogueEnchantsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class Hamburger {
    private static final String DISPLAY = ChatColor.GOLD + "Hamburger";

    public static ItemStack create(RogueEnchantsPlugin plugin) {
        ItemStack burger = new ItemStack(Material.COOKED_BEEF);
        ItemMeta meta = burger.getItemMeta();
        meta.setDisplayName(DISPLAY);
        meta.setLore(List.of(ChatColor.GRAY + "Tastes better than a Golden Head!"));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        // Persistent tag to recognize it
        NamespacedKey key = new NamespacedKey(plugin, "hamburger");
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte)1);

        burger.setItemMeta(meta);
        return burger;
    }

    public static boolean isHamburger(ItemStack stack, RogueEnchantsPlugin plugin) {
        if (stack == null || !stack.hasItemMeta()) return false;
        NamespacedKey key = new NamespacedKey(plugin, "hamburger");
        return stack.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
}
