package com.simple.rogueenchants.listeners;

import com.simple.rogueenchants.enchants.EnchantData;
import com.simple.rogueenchants.enchants.EnchantManager;
import com.simple.rogueenchants.enchants.EnchantType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.bukkit.event.block.Action.RIGHT_CLICK_AIR;
import static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;

public class ShadowstepListener implements Listener {
    private final EnchantManager manager;
    private final Map<UUID, Long> firstHitUntil = new HashMap<>();
    private final Map<UUID, Long> smokeArmedAt = new HashMap<>();
    private final Map<UUID, Long> cloneCd = new HashMap<>();
    private final Map<UUID, Long> blinkCd = new HashMap<>();

    public ShadowstepListener(EnchantManager manager) {
        this.manager = manager;
    }

    // --- L1: reduce mob targeting while sneaking
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTarget(EntityTargetEvent e) {
        if (!(e instanceof EntityTargetLivingEntityEvent)) return;
        EntityTargetLivingEntityEvent ev = (EntityTargetLivingEntityEvent) e;
        if (!(ev.getTarget() instanceof Player)) return;
        Player p = (Player) ev.getTarget();

        EnchantData d = manager.getCurrent(p);
        if (d == null || d.type() != EnchantType.SHADOWSTEP || d.level() < 1) return;
        if (!p.isSneaking()) return;

        double cancelChance = manager.getPlugin().getConfig().getDouble("shadowstep.l1.cancel-chance", 0.35);
        if (Math.random() < cancelChance) {
            ev.setTarget(null);
            ev.setCancelled(true);
        }
    }

    // --- L2: arm first-hit window on sneak
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        EnchantData d = manager.getCurrent(p);
        if (d == null || d.type() != EnchantType.SHADOWSTEP || d.level() < 2) return;

        if (e.isSneaking()) {
            firstHitUntil.put(p.getUniqueId(), System.currentTimeMillis() + 6000L);
        }
    }

    // --- L2 ambusher + crude crit-weakness
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAmbush(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        Player p = (Player) e.getDamager();

        EnchantData d = manager.getCurrent(p);
        if (d == null || d.type() != EnchantType.SHADOWSTEP || d.level() < 2) return;
        if (!p.isSneaking()) return;

        Long until = firstHitUntil.get(p.getUniqueId());
        if (until == null || System.currentTimeMillis() > until) return;

        double mult = manager.getPlugin().getConfig().getDouble("shadowstep.l2.ambush-multiplier", 1.25);
        e.setDamage(e.getDamage() * mult);
        firstHitUntil.remove(p.getUniqueId());

        if (p.getFallDistance() > 0.0f && !p.isOnGround() && e.getEntity() instanceof LivingEntity) {
            LivingEntity le = (LivingEntity) e.getEntity();
            le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0, false, true, true));
        }
    }

    // --- L3 smoke veil (invis); at L5 invis lasts longer (30s default)
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCrouch(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        EnchantData d = manager.getCurrent(p);
        if (d == null || d.type() != EnchantType.SHADOWSTEP || d.level() < 3) return;

        if (e.isSneaking()) {
            smokeArmedAt.put(p.getUniqueId(), System.currentTimeMillis());
        } else {
            smokeArmedAt.remove(p.getUniqueId());
            p.removePotionEffect(PotionEffectType.INVISIBILITY);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        EnchantData data = manager.getCurrent(p);
        if (data == null || data.type() != EnchantType.SHADOWSTEP || data.level() < 3) return;

        Long armed = smokeArmedAt.get(p.getUniqueId());
        if (armed != null && p.isSneaking()) {
            long need = manager.getPlugin().getConfig().getLong("shadowstep.l3.arm-ms", 3000L);
            if (System.currentTimeMillis() - armed >= need) {
                int baseTicks = manager.getPlugin().getConfig().getInt("shadowstep.l3.invis-ticks", 80);
                int ticks = (data.level() >= 5)
                        ? manager.getPlugin().getConfig().getInt("shadowstep.l5.invis-ticks", 600)
                        : baseTicks;
                p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, ticks, 0, false, false, true));
                p.getWorld().spawnParticle(Particle.LARGE_SMOKE, p.getLocation(), 20, 0.4, 0.1, 0.4, 0.01);
                smokeArmedAt.put(p.getUniqueId(), System.currentTimeMillis() + 9999999L); // one-time
            }
        }
    }

    // --- L4: shadow clone (teleport on hit while sneaking), cooldown
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHitClone(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();

        EnchantData d = manager.getCurrent(p);
        if (d == null || d.type() != EnchantType.SHADOWSTEP || d.level() < 4) return;
        if (!p.isSneaking()) return;

        long now = System.currentTimeMillis();
        long cdMs = manager.getPlugin().getConfig().getLong("shadowstep.l4.cooldown-ms", 30000L);
        long ready = cloneCd.getOrDefault(p.getUniqueId(), 0L);
        if (now < ready) return;

        Location loc = p.getLocation();
        p.getWorld().spawnParticle(Particle.CLOUD, loc, 40, 0.6, 0.2, 0.6, 0.02);
        p.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);

        double r = manager.getPlugin().getConfig().getDouble("shadowstep.l4.teleport-radius", 5.0);
        Vector rnd = new Vector((Math.random()*2-1), 0, (Math.random()*2-1)).normalize().multiply(r);
        Location to = safeTeleport(loc.clone().add(rnd));
        p.teleport(to);

        cloneCd.put(p.getUniqueId(), now + cdMs);
    }

    // --- L5: RIGHT-CLICK BLINK — no block needed. Teleport straight along look dir.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlink(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != RIGHT_CLICK_AIR && e.getAction() != RIGHT_CLICK_BLOCK) return;

        Player p = e.getPlayer();
        EnchantData d = manager.getCurrent(p);
        if (d == null || d.type() != EnchantType.SHADOWSTEP || d.level() < 5) return;

        long now = System.currentTimeMillis();
        long cdMs = manager.getPlugin().getConfig().getLong("shadowstep.l5.blink-cooldown-ms", 15000L);
        long ready = blinkCd.getOrDefault(p.getUniqueId(), 0L);
        if (now < ready) return;

        int maxDist = manager.getPlugin().getConfig().getInt("shadowstep.l5.blink-max-distance", 16);

        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        Location target = eye.clone().add(dir.multiply(maxDist));

        // Find a safe spot without requiring a block hit:
        Location to = findSafeForward(eye, dir, maxDist);
        if (to == null) {
            // fallback: just try highest block around target
            to = target.getWorld().getHighestBlockAt(target).getLocation().add(0.5, 1.0, 0.5);
        }

        p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation(), 20, 0.4, 0.2, 0.4, 0.01);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.4f);
        p.teleport(to);
        p.getWorld().spawnParticle(Particle.CLOUD, to, 20, 0.4, 0.2, 0.4, 0.01);
        p.getWorld().playSound(to, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.6f);

        blinkCd.put(p.getUniqueId(), now + cdMs);
    }

    private Location safeTeleport(Location desired) {
        Location loc = desired.clone();
        for (int i = 0; i < 3; i++) {
            Block b = loc.getBlock();
            Block bUp = b.getLocation().add(0,1,0).getBlock();
            if (b.isPassable() && bUp.isPassable()) return loc;
            loc.add(0, 1, 0);
        }
        return loc.getWorld().getHighestBlockAt(loc).getLocation().add(0.5, 1.0, 0.5);
    }

    // Walk forward from the eye, then backtrack a little and try up/down to find 2-block air.
    private Location findSafeForward(Location eye, Vector dir, int maxDist) {
        World w = eye.getWorld();
        Location best = null;

        // sample points every 0.5 block up to maxDist
        int steps = Math.max(2, maxDist * 2);
        for (int i = 1; i <= steps; i++) {
            Location sample = eye.clone().add(dir.clone().multiply(i * 0.5));
            if (!w.getWorldBorder().isInside(sample)) break;
            if (isTwoBlockAir(sample)) best = sample.clone(); // keep last known good
        }
        if (best == null) return null;

        // Try slight downward & upward nudges to avoid ceilings/floors
        for (int dy = -1; dy <= 1; dy++) {
            Location test = best.clone().add(0, dy, 0);
            if (isTwoBlockAir(test)) {
                return centerOnBlock(test);
            }
        }
        return centerOnBlock(best);
    }

    private boolean isTwoBlockAir(Location loc) {
        Block b = loc.getBlock();
        Block bUp = b.getLocation().add(0,1,0).getBlock();
        return b.isPassable() && bUp.isPassable();
    }

    private Location centerOnBlock(Location loc) {
        return new Location(loc.getWorld(),
                Math.floor(loc.getX()) + 0.5,
                Math.floor(loc.getY()),
                Math.floor(loc.getZ()) + 0.5,
                loc.getYaw(), loc.getPitch());
    }
}
