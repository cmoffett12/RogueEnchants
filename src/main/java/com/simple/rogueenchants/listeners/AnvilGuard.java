package com.simple.rogueenchants.listeners;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public class AnvilGuard implements Listener {
    private final Set<Enchantment> allowed = Set.of(Enchantment.MENDING, Enchantment.UNBREAKING);

    @EventHandler
    public void onPrepare(PrepareAnvilEvent e) {
        AnvilInventory inv = e.getInventory();
        ItemStack left = inv.getFirstItem();
        ItemStack right = inv.getSecondItem();
        if (left == null || right == null) return;

        // Only guard when combining with a book
        if (right.getType() != Material.ENCHANTED_BOOK) {
            // allow repairs/renames as normal
            return;
        }

        // If the book has only allowed enchants, let Paper/Bukkit compute normally.
        var ench = right.getEnchantments().keySet();
        if (ench.isEmpty()) return;

        boolean allAllowed = ench.stream().allMatch(allowed::contains);
        if (!allAllowed) {
            e.setResult(null); // block forbidden books
        }
    }
}
