package com.simple.rogueenchants.items;

import com.simple.rogueenchants.enchants.EnchantType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class EnchantBook {
    private EnchantBook() {}

    public static ItemStack create(NamespacedKey keyType, NamespacedKey keyLevel, EnchantType type, int level) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK, 1);
        ItemMeta meta = book.getItemMeta();
        String roman = roman(level);
        meta.setDisplayName(ChatColor.AQUA + "[Enchant Book] " + ChatColor.GOLD + type.name() + ChatColor.GRAY + " " + roman);
        meta.setLore(List.of(
                ChatColor.DARK_GRAY + "Right-click to equip this enchant.",
                ChatColor.DARK_GRAY + "Your current enchant will become a book."
        ));
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyType, PersistentDataType.STRING, type.name());
        pdc.set(keyLevel, PersistentDataType.INTEGER, level);
        book.setItemMeta(meta);
        return book;
    }

    public static EnchantType getType(NamespacedKey keyType, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String s = item.getItemMeta().getPersistentDataContainer().get(keyType, PersistentDataType.STRING);
        if (s == null) return null;
        try { return EnchantType.valueOf(s); } catch (Exception ignored) { return null; }
    }

    public static Integer getLevel(NamespacedKey keyLevel, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(keyLevel, PersistentDataType.INTEGER);
    }

    private static String roman(int n) {
        String[] r = {"","I","II","III","IV","V","VI","VII","VIII","IX","X"};
        if (n >= 0 && n < r.length) return r[n];
        return String.valueOf(n);
    }
}
