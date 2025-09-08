package com.simple.rogueenchants.listeners;

import com.simple.rogueenchants.enchants.EnchantData;
import com.simple.rogueenchants.enchants.EnchantManager;
import com.simple.rogueenchants.enchants.EnchantType;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

import static org.bukkit.event.block.Action.RIGHT_CLICK_AIR;
import static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;

public class ChannelingListener implements Listener {

    private final EnchantManager manager;

    // L1 dash cooldown
    private final Map<UUID, Long> dashCd = new HashMap<>();
    // L3 storm battery stacks
    private final Map<UUID, Integer> stacks = new HashMap<>();

    // Combat bookkeeping: who I attacked; who attacked me (with expiry)
    private final Map<UUID, Map<UUID, Long>> recentTargets = new HashMap<>();
    private final Map<UUID, Map<UUID, Long>> recentAttackers = new HashMap<>();

    private final Random rng = new Random();

    public ChannelingListener(EnchantManager manager) {
        this.manager = manager;
        Bukkit.getScheduler().runTaskTimer(manager.getPlugin(), this::tickLoop, 40L, 40L);
    }

    /* =========================
       L1: Riptide-like dash
       ========================= */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDash(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != RIGHT_CLICK_AIR && e.getAction() != RIGHT_CLICK_BLOCK) return;

        Player p = e.getPlayer();
        EnchantData d = manager.getCurrent(p);
        if (d == null || d.type() != EnchantType.CHANNELING || d.level() < 1) return;

        tryDash(p);
    }

    // Optional: press F to dash
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        EnchantData d = manager.getCurrent(p);
        if (d == null || d.type() != EnchantType.CHANNELING || d.level() < 1) return;
        if (tryDash(p)) e.setCancelled(true);
    }

    private boolean tryDash(Player p) {
        int cdTicks = manager.getPlugin().getConfig().getInt("channeling.riptide.cooldown-ticks", 20);
        long now = System.currentTimeMillis();
        long readyAt = dashCd.getOrDefault(p.getUniqueId(), 0L);
        if (now < readyAt) return false;

        double speed = manager.getPlugin().getConfig().getDouble("channeling.riptide.speed", 1.4);
        double vy    = manager.getPlugin().getConfig().getDouble("channeling.riptide.vertical-boost", 0.8);

        Vector dir = p.getLocation().getDirection().normalize();
        Vector vel = dir.multiply(speed);
        vel.setY(Math.max(vel.getY() + vy, vy));
        p.setVelocity(vel);

        p.getWorld().playSound(p.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 1f, 1f);
        p.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, p.getLocation(), 10, 0.3, 0.2, 0.3, 0.02);

        dashCd.put(p.getUniqueId(), now + cdTicks * 50L);

        // L3: gain a stack each dash
        EnchantData d = manager.getCurrent(p);
        if (d != null && d.level() >= 3) {
            int have = stacks.getOrDefault(p.getUniqueId(), 0);
            int need = manager.getPlugin().getConfig().getInt("channeling.l3.stacks-need", 3);
            have = Math.min(need, have + 1);
            stacks.put(p.getUniqueId(), have);
        }
        return true;
    }

    /* =========================================================
       Combat bookkeeping + L2/L3 effects filtered to combatants
       ========================================================= */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMelee(EntityDamageByEntityEvent e) {
        // record combat both ways
        if (e.getDamager() instanceof Player a) {
            // a attacked target
            Entity tgt = e.getEntity();
            if (tgt instanceof LivingEntity le) markTarget(a.getUniqueId(), le.getUniqueId());
        }
        if (e.getEntity() instanceof Player victim && e.getDamager() instanceof LivingEntity atk) {
            // attacker hit victim
            markAttacker(victim.getUniqueId(), atk.getUniqueId());
        }

        // channeling effects
        if (!(e.getDamager() instanceof Player p)) return;
        EnchantData d = manager.getCurrent(p);
        if (d == null || d.type() != EnchantType.CHANNELING) return;

        // L2: chain zap ONLY eligible combatants
        if (d.level() >= 2) {
            double chance = manager.getPlugin().getConfig().getDouble("channeling.l2.chain-chance", 0.15);
            if (rng.nextDouble() < chance) {
                int radius = manager.getPlugin().getConfig().getInt("channeling.l2.chain-radius", 4);
                double dmg = manager.getPlugin().getConfig().getDouble("channeling.l2.chain-damage", 2.0);

                Set<UUID> elig = eligibleSet(p);
                for (Entity ent : p.getNearbyEntities(radius, radius, radius)) {
                    if (ent instanceof LivingEntity le && elig.contains(le.getUniqueId())) {
                        le.getWorld().strikeLightningEffect(le.getLocation());
                        le.damage(dmg, p);
                    }
                }
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.6f, 1.6f);
            }
        }

        // L3: burst after stacks, ONLY against combatants
        if (d.level() >= 3) {
            int have = stacks.getOrDefault(p.getUniqueId(), 0);
            int need = manager.getPlugin().getConfig().getInt("channeling.l3.stacks-need", 3);
            if (have >= need) {
                stacks.put(p.getUniqueId(), 0);
                int radius = manager.getPlugin().getConfig().getInt("channeling.l3.burst-radius", 5);
                double dmg = manager.getPlugin().getConfig().getDouble("channeling.l3.burst-damage", 4.0);

                Set<UUID> elig = eligibleSet(p);
                for (Entity ent : p.getNearbyEntities(radius, radius, radius)) {
                    if (ent instanceof LivingEntity le && elig.contains(le.getUniqueId())) {
                        le.getWorld().strikeLightningEffect(le.getLocation());
                        le.damage(dmg, p);
                    }
                }
                p.getWorld().spawnParticle(Particle.CRIT, p.getLocation(), 60, 0.8, 0.4, 0.8, 0.1);
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.4f);
            }
        }
    }

    private void tickLoop() {
        // L4/L5 periodic + prune old combat entries
        long window = manager.getPlugin().getConfig().getLong("channeling.combat-window-ms", 10000L);
        long now = System.currentTimeMillis();

        // prune
        pruneMap(recentTargets, now, window);
        pruneMap(recentAttackers, now, window);

        // L4/L5
        for (Player p : Bukkit.getOnlinePlayers()) {
            EnchantData d = manager.getCurrent(p);
            if (d == null || d.type() != EnchantType.CHANNELING) continue;

            // L4 buffs in storms
            if (d.level() >= 4) {
                World w = p.getWorld();
                if (w.hasStorm() || w.isThundering()) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1, true, false, true));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 0, true, false, true));
                }
            }

            // L5 smite only current combatants nearby
            if (d.level() >= 5) {
                int rad = manager.getPlugin().getConfig().getInt("channeling.l5.smite-radius", 8);
                int max = manager.getPlugin().getConfig().getInt("channeling.l5.max-per-tick", 2);
                double dmg = manager.getPlugin().getConfig().getDouble("channeling.l5.smite-damage", 3.0);

                Set<UUID> elig = eligibleSet(p);
                int hits = 0;
                for (Entity ent : p.getNearbyEntities(rad, rad, rad)) {
                    if (ent instanceof LivingEntity le && elig.contains(le.getUniqueId())) {
                        le.getWorld().strikeLightningEffect(le.getLocation());
                        le.damage(dmg, p);
                        if (++hits >= max) break;
                    }
                }
            }
        }
    }

    private void markTarget(UUID player, UUID target) {
        long until = System.currentTimeMillis() + manager.getPlugin().getConfig().getLong("channeling.combat-window-ms", 10000L);
        recentTargets.computeIfAbsent(player, k -> new HashMap<>()).put(target, until);
    }

    private void markAttacker(UUID player, UUID attacker) {
        long until = System.currentTimeMillis() + manager.getPlugin().getConfig().getLong("channeling.combat-window-ms", 10000L);
        recentAttackers.computeIfAbsent(player, k -> new HashMap<>()).put(attacker, until);
    }

    private Set<UUID> eligibleSet(Player p) {
        Set<UUID> s = new HashSet<>();
        Map<UUID, Long> t = recentTargets.get(p.getUniqueId());
        Map<UUID, Long> a = recentAttackers.get(p.getUniqueId());
        long now = System.currentTimeMillis();
        if (t != null) t.forEach((id, exp) -> { if (exp >= now) s.add(id); });
        if (a != null) a.forEach((id, exp) -> { if (exp >= now) s.add(id); });
        return s;
    }

    private void pruneMap(Map<UUID, Map<UUID, Long>> map, long now, long window) {
        for (var entry : map.entrySet()) {
            Map<UUID, Long> inner = entry.getValue();
            inner.entrySet().removeIf(e -> e.getValue() < now);
        }
    }
}
