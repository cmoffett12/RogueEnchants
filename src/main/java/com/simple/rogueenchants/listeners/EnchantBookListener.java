package com.simple.rogueenchants.listeners;

import com.simple.rogueenchants.RogueEnchantsPlugin;
import com.simple.rogueenchants.enchants.EnchantData;
import com.simple.rogueenchants.enchants.EnchantManager;
import com.simple.rogueenchants.enchants.EnchantType;
import com.simple.rogueenchants.items.EnchantBook;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class EnchantBookListener implements Listener {
    private final EnchantManager manager;
    private final NamespacedKey keyType;
    private final NamespacedKey keyLevel;

    public EnchantBookListener(EnchantManager manager, RogueEnchantsPlugin plugin) {
        this.manager = manager;
        this.keyType = new NamespacedKey(plugin, "book_type");
        this.keyLevel = new NamespacedKey(plugin, "book_level");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onUse(PlayerInteractEvent e) {
        // main hand only to avoid double fires
        if (e.getHand() != EquipmentSlot.HAND) return;

        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();

        EnchantType bookType = EnchantBook.getType(keyType, item);
        Integer bookLevel = EnchantBook.getLevel(keyLevel, item);
        if (bookType == null || bookLevel == null) return; // not our book

        e.setCancelled(true); // consume interaction for clarity

        // Make the swap:
        // 1) Turn player's current enchant (if any) into a book (same level)
        var current = manager.getCurrent(p);
        if (current != null) {
            ItemStack refund = EnchantBook.create(keyType, keyLevel, current.type(), current.level());
            // give back, or drop if full
            var left = p.getInventory().addItem(refund);
            if (!left.isEmpty()) {
                left.values().forEach(stk -> p.getWorld().dropItemNaturally(p.getLocation(), stk));
            }
        }

        // 2) Equip the book’s enchant (respect max level cap)
        int clamped = Math.min(bookLevel, manager.maxLevelFor(p));
        manager.setCurrent(p, bookType, clamped);

        // 3) Consume the used book (reduce stack by 1)
        ItemStack hand = p.getInventory().getItemInMainHand();
        hand.setAmount(hand.getAmount() - 1);

        p.sendMessage(ChatColor.GREEN + "Equipped " + ChatColor.GOLD + bookType.name() + ChatColor.GRAY + " x" + clamped
                + ChatColor.GREEN + " and received a book of your previous enchant (if any).");
    }

    /* exposed for other classes (e.g., CombatListener) */
    public NamespacedKey getKeyType() { return keyType; }
    public NamespacedKey getKeyLevel() { return keyLevel; }
}
