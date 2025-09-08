package com.simple.rogueenchants.listeners.smite;

import com.simple.rogueenchants.enchants.EnchantManager;
import com.simple.rogueenchants.enchants.EnchantType;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

import java.lang.reflect.Method;

public class SmiteFearListener implements Listener {
    private final EnchantManager manager;

    // How far around the player we “pacify” mobs each tick cycle
    private static final double RADIUS = 24.0;

    public SmiteFearListener(EnchantManager manager) {
        this.manager = manager;

        // Periodic sweeper: every 2 seconds, clear targets & (if supported) anger near Smite-5 players
        long period = 40L; // 2s
        Bukkit.getScheduler().runTaskTimer(manager.getPlugin(), () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                var d = manager.getCurrent(p);
                if (d == null || d.type() != EnchantType.SMITE || d.level() < 5) continue;

                for (Entity e : p.getNearbyEntities(RADIUS, RADIUS, RADIUS)) {
                    if (!(e instanceof Mob mob)) continue;
                    if (!isHostile(mob)) continue;

                    LivingEntity tgt = mob.getTarget();
                    if (tgt != null && tgt.getUniqueId().equals(p.getUniqueId())) {
                        mob.setTarget(null);
                        clearAngerIfPresent(mob, p);
                    }
                }
            }
        }, period, period);
    }

    /** Cancel any attempt to target a Smite-5 player (covers most AI goal changes). */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTarget(EntityTargetEvent e) {
        if (!(e instanceof EntityTargetLivingEntityEvent ev)) return;
        if (!(ev.getTarget() instanceof Player p)) return;

        var d = manager.getCurrent(p);
        if (d == null || d.type() != EnchantType.SMITE || d.level() < 5) return;
        if (!(ev.getEntity() instanceof Mob mob) || !isHostile(mob)) return;

        ev.setTarget(null);
        ev.setCancelled(true);
        clearAngerIfPresent(mob, p);
    }

    /** If a hit still sneaks through, drop target immediately after the strike. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        var d = manager.getCurrent(p);
        if (d == null || d.type() != EnchantType.SMITE || d.level() < 5) return;

        if (e.getDamager() instanceof Mob mob && isHostile(mob)) {
            mob.setTarget(null);
            clearAngerIfPresent(mob, p);
        }
    }

    /** Heuristic for hostiles; broadened to catch more edge cases. */
    private boolean isHostile(Mob mob) {
        if (mob instanceof Monster) return true;
        return (mob instanceof Slime) ||
               (mob instanceof MagmaCube) ||
               (mob instanceof Hoglin) ||
               (mob instanceof Piglin) ||
               (mob instanceof PiglinBrute) ||
               (mob instanceof Guardian) ||
               (mob instanceof Phantom) ||
               (mob instanceof Endermite) ||
               (mob instanceof Silverfish) ||
               (mob instanceof Ravager) ||
               (mob instanceof Shulker);
    }

    /**
     * Clear “anger” if the mob class supports it in this API/build.
     * We avoid importing Angerable and instead probe typical methods via reflection.
     */
    private void clearAngerIfPresent(Mob mob, Player p) {
        // Always ensure target cleared
        if (mob.getTarget() != null && mob.getTarget().getUniqueId().equals(p.getUniqueId())) {
            mob.setTarget(null);
        }

        Class<?> c = mob.getClass();
        tryInvoke(c, mob, "setAnger", new Class[]{int.class}, new Object[]{0});
        tryInvoke(c, mob, "setRemainingAngerTime", new Class[]{int.class}, new Object[]{0});
        tryInvoke(c, mob, "setAngry", new Class[]{boolean.class}, new Object[]{false});
        tryInvoke(c, mob, "stopAnger", new Class[]{}, new Object[]{});
        tryInvoke(c, mob, "clearTarget", new Class[]{}, new Object[]{}); // some builds expose this
    }

    private void tryInvoke(Class<?> cls, Object obj, String name, Class<?>[] sig, Object[] args) {
        try {
            Method m = cls.getMethod(name, sig);
            m.setAccessible(true);
            m.invoke(obj, args);
        } catch (Exception ignored) {
            // Method not present in this API/build; safe to ignore.
        }
    }
}
