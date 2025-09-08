package com.simple.rogueenchants.listeners.smite;

import com.simple.rogueenchants.enchants.EnchantManager;
import com.simple.rogueenchants.enchants.EnchantType;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class SmiteDamageListener implements Listener {
    private final EnchantManager manager;

    public SmiteDamageListener(EnchantManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onHitUndead(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        if (!(e.getEntity() instanceof LivingEntity)) return;

        Player p = (Player) e.getDamager();
        var data = manager.getCurrent(p);
        if (data == null || data.type() != EnchantType.SMITE) return;

        if (!isUndead((LivingEntity) e.getEntity())) return;

        double perLvl = manager.getPlugin().getConfig().getDouble("smite.undead-damage-bonus-per-level", 0.15);
        double mult = 1.0 + (perLvl * data.level());
        e.setDamage(e.getDamage() * mult);
    }

    private boolean isUndead(LivingEntity le) {
        EntityType t = le.getType();
        // Common undead set (safe across Paper versions)
        return t == EntityType.ZOMBIE ||
               t == EntityType.DROWNED ||
               t == EntityType.HUSK ||
               t == EntityType.ZOMBIE_VILLAGER ||
               t == EntityType.SKELETON ||
               t == EntityType.STRAY ||
               t == EntityType.WITHER ||
               t == EntityType.WITHER_SKELETON ||
               t == EntityType.ZOGLIN ||
               t == EntityType.PHANTOM;
    }
}
