package com.simple.rogueenchants.listeners;

import com.simple.rogueenchants.RogueEnchantsPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderDragon;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class DragonDeathListener implements Listener {

    private final RogueEnchantsPlugin plugin;

    public DragonDeathListener(RogueEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDragonDeath(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof EnderDragon)) return;

        // Only act if a ritual completed earlier
        if (!plugin.getRitualManager().consumeExtraEggFlag()) return;

        World world = e.getEntity().getWorld();

        // Try to place an extra dragon egg near the portal center (0,0) in The End,
        // falling back to the death location area if needed.
        Location center = new Location(world, 0, world.getHighestBlockYAt(0, 0) + 1, 0);
        if (!placeEggAtOrNearby(center)) {
            // Fallback to death location area
            Location death = e.getEntity().getLocation();
            Location fallback = new Location(world,
                    death.getBlockX(), world.getHighestBlockYAt(death) + 1, death.getBlockZ());
            placeEggAtOrNearby(fallback); // best-effort; ignore if still can’t place
        }
    }

    /** Attempts to place a DRAGON_EGG at the given location or the nearest valid spot. */
    private boolean placeEggAtOrNearby(Location base) {
        World world = base.getWorld();
        if (world == null) return false;

        // Small search cube around base to find a solid block with air above
        int bx = base.getBlockX();
        int by = Math.max(5, base.getBlockY()); // sanity floor
        int bz = base.getBlockZ();

        for (int dy = 0; dy <= 6; dy++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    Block below = world.getBlockAt(bx + dx, by + dy - 1, bz + dz);
                    Block spot  = world.getBlockAt(bx + dx, by + dy,     bz + dz);
                    if (!below.getType().isSolid()) continue;
                    if (!spot.isEmpty()) continue;

                    // Place the egg
                    spot.setType(Material.DRAGON_EGG, false);
                    return true;
                }
            }
        }
        return false;
    }
}
