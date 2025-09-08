package com.simple.rogueenchants.listeners;

import com.simple.rogueenchants.RogueEnchantsPlugin;
import com.simple.rogueenchants.enchants.EnchantData;
import com.simple.rogueenchants.enchants.EnchantManager;
import com.simple.rogueenchants.enchants.EnchantType;
import com.simple.rogueenchants.items.EnchantBook;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class CombatListener implements Listener {
    private final EnchantManager manager;
    private final RogueEnchantsPlugin plugin;
    // same keys used by EnchantBookListener so books are compatible
    private final NamespacedKey keyType;
    private final NamespacedKey keyLevel;

    // Track who needs a reroll applied on respawn
    private final Set<UUID> needsReroll = new HashSet<>();
    private final Random rng = new Random();

    public CombatListener(EnchantManager manager, RogueEnchantsPlugin plugin) {
        this.manager = manager;
        this.plugin = plugin;
        this.keyType = new NamespacedKey(plugin, "book_type");
        this.keyLevel = new NamespacedKey(plugin, "book_level");
    }

    /** On death: turn victim's CURRENT enchant into a book and give to killer (or drop at killer). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        EnchantData cur = manager.getCurrent(victim);
        if (cur == null) return; // nothing to drop or reroll

        // Create the book FIRST from the victim's current enchant
        ItemStack book = EnchantBook.create(keyType, keyLevel, cur.type(), cur.level());

        Player killer = victim.getKiller();
        if (killer != null && killer.getType() == EntityType.PLAYER) {
            // Try to add to killer's inventory; if full, drop at killer's location
            var leftovers = killer.getInventory().addItem(book);
            if (!leftovers.isEmpty()) {
                for (ItemStack left : leftovers.values()) {
                    killer.getWorld().dropItemNaturally(killer.getLocation(), left);
                }
            }
            killer.sendMessage(ChatColor.YELLOW + "You gained an enchant book from " + victim.getName() + ".");
        } else {
            // No killer; drop at victim location as fallback
            victim.getWorld().dropItemNaturally(victim.getLocation(), book);
        }

        // Mark victim to be rerolled on respawn; they "lose" their old enchant now
        needsReroll.add(victim.getUniqueId());
        // Optionally clear their current immediately so effects stop during death screen
        // (safe; we'll set a new one on respawn)
        manager.setCurrent(victim, null, 0);

        victim.sendMessage(ChatColor.GRAY + "Your enchant was released as a book and will be rerolled when you respawn.");
    }

    /** On respawn: assign a fresh random CURRENT enchant at level 1 (respecting caps). */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        if (!needsReroll.remove(p.getUniqueId())) return;

        // Choose a random EnchantType; you can customize the pool here
        EnchantType newType = pickRandomType();
        int lvl = Math.min(1, manager.maxLevelFor(p)); // start at 1, clamped to any cap

        // If somehow cap is 0, just pick level 1 anyway
        if (lvl <= 0) lvl = 1;

        manager.setCurrent(p, newType, lvl);

        p.sendMessage(ChatColor.GREEN + "You were rerolled to " + ChatColor.GOLD + newType.name()
                + ChatColor.GRAY + " x" + lvl + ChatColor.GREEN + ".");
    }

    /** Random type helper. Adjust this pool if you want to exclude anything. */
    private EnchantType pickRandomType() {
        // Build a selectable pool; exclude FORTIFY if that is a meta/egg-only feature
        EnumSet<EnchantType> pool = EnumSet.noneOf(EnchantType.class);
        for (EnchantType t : EnchantType.values()) {
            // Example exclusion rule; remove if you want it included:
            if (t.name().equalsIgnoreCase("FORTIFY")) continue;
            pool.add(t);
        }
        if (pool.isEmpty()) {
            // Fallback: just return first enum constant to avoid NPEs in edge setups
            return EnchantType.values()[0];
        }
        int idx = rng.nextInt(pool.size());
        int i = 0;
        for (EnchantType t : pool) {
            if (i++ == idx) return t;
        }
        // Should never reach
        return pool.iterator().next();
    }
}
