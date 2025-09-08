package com.simple.rogueenchants.listeners;

import com.simple.rogueenchants.enchants.EnchantData;
import com.simple.rogueenchants.enchants.EnchantManager;
import com.simple.rogueenchants.enchants.EnchantType;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FeatherFallingListener implements Listener {
    private final EnchantManager manager;

    private final Map<UUID, Long> gliderCooldown = new HashMap<>();
    private final Map<UUID, Integer> jumpsUsed = new HashMap<>();
    private final Map<UUID, Long> lastJumpAt = new HashMap<>(); // tiny anti-spam

    public FeatherFallingListener(EnchantManager manager) {
        this.manager = manager;
    }

    /* L1 reduction + L4 immunity */
    @EventHandler
    public void onFall(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        EnchantData data = manager.getCurrent(p);
        if (data == null || data.type() != EnchantType.FEATHER_FALLING) return;

        int lvl = data.level();
        if (lvl >= 4) { // immunity
            e.setCancelled(true);
            return;
        }
        if (lvl >= 1) {
            double reduce = manager.getPlugin().getConfig().getDouble("feather.l1.reduce", 0.40);
            e.setDamage(Math.max(0, e.getDamage() * (1.0 - reduce)));
        }
    }

    /* Handle glide + double/triple jump via flight toggle */
    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent e) {
        Player p = e.getPlayer();
        EnchantData data = manager.getCurrent(p);
        if (data == null || data.type() != EnchantType.FEATHER_FALLING) return;
        int lvl = data.level();

        // We always intercept vanilla flight toggle
        e.setCancelled(true);
        // We'll re-arm allowFlight manually after we do our ability
        p.setAllowFlight(false);

        long now = System.currentTimeMillis();

        // ---- L2 Glider (30s), runs only if off cooldown and actually midair
        boolean midair = !p.isOnGround() && !p.isFlying() && !p.isGliding() && p.getVehicle() == null;
        if (lvl >= 2 && midair) {
            long readyAt = gliderCooldown.getOrDefault(p.getUniqueId(), 0L);
            if (now >= readyAt) {
                int durSec = manager.getPlugin().getConfig().getInt("feather.l2.duration-sec", 30);
                long cdMs  = manager.getPlugin().getConfig().getLong("feather.l2.cooldown-ms", 30000L);
                gliderCooldown.put(p.getUniqueId(), now + cdMs + durSec * 1000L);

                // Glide needs a one-tick delay on many servers; also nudge forward
                p.getWorld().playSound(p.getLocation(), Sound.ITEM_ELYTRA_FLYING, 0.9f, 1.5f);
                Vector nudge = p.getLocation().getDirection().multiply(0.25).setY(Math.max(0.12, p.getVelocity().getY()));
                p.setVelocity(p.getVelocity().add(nudge));

                Bukkit.getScheduler().runTaskLater(manager.getPlugin(), () -> {
                    if (p.isOnline() && !p.isOnGround() && !p.isGliding()) {
                        p.setGliding(true);
                    }
                }, 1L);

                // Stop gliding after duration
                Bukkit.getScheduler().runTaskLater(manager.getPlugin(), () -> {
                    if (p.isOnline() && p.isGliding()) p.setGliding(false);
                }, durSec * 20L);

                // After using glider, we do NOT re-arm allowFlight this tick (avoid instant re-trigger)
                return;
            }
        }

        // ---- L3/L4: Double / Triple Jump
        int maxJumps = (lvl >= 4 ? 2 : (lvl >= 3 ? 1 : 0));
        if (maxJumps > 0 && midair) {
            int used = jumpsUsed.getOrDefault(p.getUniqueId(), 0);
            long last = lastJumpAt.getOrDefault(p.getUniqueId(), 0L);
            long gapMs = 200L; // prevent spam double-activations

            if (used < maxJumps && now - last >= gapMs) {
                jumpsUsed.put(p.getUniqueId(), used + 1);
                lastJumpAt.put(p.getUniqueId(), now);

                Vector vel = p.getLocation().getDirection().multiply(0.15);
                vel.setY(0.9);
                p.setVelocity(p.getVelocity().add(vel));

                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.8f, 1.2f);
                p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation(), 10, 0.3, 0.05, 0.3, 0.01);

                // Re-arm allowFlight so the player can trigger the next midair jump (if any)
                Bukkit.getScheduler().runTaskLater(manager.getPlugin(), () -> {
                    if (p.isOnline() && !p.isOnGround() && !p.isGliding() && p.getGameMode() == GameMode.SURVIVAL) {
                        p.setAllowFlight(true);
                    }
                }, 1L);
                return;
            }
        }

        // If we reached here, nothing triggered; keep things tidy
        if (p.getGameMode() == GameMode.SURVIVAL) {
            // Re-arm in case we pressed too early; onMove will also arm as needed
            p.setAllowFlight(true);
        }
    }

    /* Arm/Reset state while moving */
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        EnchantData data = manager.getCurrent(p);
        if (data == null || data.type() != EnchantType.FEATHER_FALLING) return;
        int lvl = data.level();

        if (p.isOnGround()) {
            // Reset jump counters on landing
            jumpsUsed.put(p.getUniqueId(), 0);
            lastJumpAt.remove(p.getUniqueId());

            // Convenience: allow pressing jump to prepare for midair actions
            if (p.getGameMode() == GameMode.SURVIVAL) {
                p.setAllowFlight(true);
            }
        } else {
            // Midair: arm allowFlight if the player can use glider or has jumps left
            boolean canGlide = lvl >= 2 && System.currentTimeMillis() >= gliderCooldown.getOrDefault(p.getUniqueId(), 0L);
            boolean hasJumps = lvl >= 3 && jumpsUsed.getOrDefault(p.getUniqueId(), 0) < (lvl >= 4 ? 2 : 1);

            if ((canGlide || hasJumps) && !p.getAllowFlight() && !p.isGliding() && p.getGameMode() == GameMode.SURVIVAL) {
                p.setAllowFlight(true);
            }
        }
    }

    /* L5 shockwave on larger falls (after damage is applied/cancelled) */
    @EventHandler
    public void onBigFall(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        EnchantData data = manager.getCurrent(p);
        if (data == null || data.type() != EnchantType.FEATHER_FALLING || data.level() < 5) return;

        // Only trigger for larger falls (uses pre-reduction damage value)
        if (e.getFinalDamage() > 4.0) {
            double radius = manager.getPlugin().getConfig().getDouble("feather.l5.radius", 5.0);
            double force  = manager.getPlugin().getConfig().getDouble("feather.l5.knockback", 1.2);

            for (Entity ent : p.getNearbyEntities(radius, radius, radius)) {
                if (ent instanceof LivingEntity le && !le.getUniqueId().equals(p.getUniqueId())) {
                    Vector away = le.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(force);
                    away.setY(0.4);
                    le.setVelocity(away);
                }
            }

            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
            p.getWorld().spawnParticle(Particle.EXPLOSION, p.getLocation(), 20, 1, 0.5, 1, 0.1);
        }
    }
}
