package com.simple.rogueenchants.ritual;

import com.simple.rogueenchants.RogueEnchantsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

/**
 * Handles Fortified Dragon Egg rituals.
 * When a ritual completes, grants a tagged "Starbound" Fortified Egg
 * and flags that the next Ender Dragon death should drop an extra egg.
 */
public class RitualManager {

    private final RogueEnchantsPlugin plugin;

    /** Number of pending extra eggs to spawn on next dragon death(s). */
    private int extraEggsToSpawn = 0;

    public RitualManager(RogueEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Begin a ritual at the player's current location.
     * Broadcasts to all players, runs a timed effect,
     * and rewards the Fortified Egg when complete.
     */
    public void startRitual(Player p) {
        final int secs = plugin.getConfig().getInt("ritual.duration-seconds", 60);

        // Broadcast ritual start + coords
        var loc = p.getLocation();
        String template = plugin.getConfig().getString(
                "ritual.broadcast",
                "&d[Fortified Ritual]&7 at &b%world% &7x:&b%x% &7y:&b%y% &7z:&b%z%"
        );
        String msg = ChatColor.translateAlternateColorCodes('&',
                template.replace("%world%", loc.getWorld().getName())
                        .replace("%x%", String.valueOf(loc.getBlockX()))
                        .replace("%y%", String.valueOf(loc.getBlockY()))
                        .replace("%z%", String.valueOf(loc.getBlockZ()))
        );
        Bukkit.broadcastMessage(msg);

        // Countdown task
        new BukkitRunnable() {
            int t = secs;

            @Override
            public void run() {
                if (!p.isOnline()) {
                    cancel();
                    return;
                }

                // Ritual particle effect
                loc.getWorld().spawnParticle(Particle.PORTAL, loc, 30, 1.2, 1.0, 1.2, 0.05);

                // Every 10s play sound
                if (t % 10 == 0) {
                    p.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.1f);
                }

                // Ritual complete
                if (t-- <= 0) {
                    giveFortifiedEgg(p);
                    extraEggsToSpawn++;
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Called by DragonDeathListener: if true, spawn extra egg and decrement counter.
     */
    public boolean consumeExtraEggFlag() {
        if (extraEggsToSpawn <= 0) return false;
        extraEggsToSpawn--;
        return true;
    }

    /**
     * Grants the Fortified Dragon Egg (Starbound) item to the player.
     */
    private void giveFortifiedEgg(Player p) {
        ItemStack egg = new ItemStack(Material.DRAGON_EGG);
        ItemMeta meta = egg.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Fortified Dragon Egg §7(Starbound)");
            meta.setLore(List.of(ChatColor.GRAY + "Empowered by a Nether Star."));
            NamespacedKey key = plugin.getFortifiedEggKey();
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
            egg.setItemMeta(meta);
        }
        p.getInventory().addItem(egg);
        p.sendMessage(ChatColor.LIGHT_PURPLE + "The ritual is complete. You received a Fortified Dragon Egg!");
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }
}
