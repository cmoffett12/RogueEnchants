package com.simple.rogueenchants.listeners;

import com.simple.rogueenchants.enchants.EnchantManager;
import com.simple.rogueenchants.enchants.EnchantType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class DamageReductionListener implements Listener {
    private final EnchantManager manager;
    public DamageReductionListener(EnchantManager manager) { this.manager = manager; }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player player = (Player) e.getEntity();

        var data = manager.getCurrent(player);
        if (data == null || data.type() != EnchantType.FORTIFY) return;

        // Simple percent reduction per level (configurable)
        double perLvl = manager.getPlugin().getConfig()
                .getDouble("enchants.fortify.damage-reduction-per-level", 0.04); // 4% each level
        double mult = Math.max(0.0, 1.0 - (perLvl * data.level()));
        e.setDamage(e.getDamage() * mult);
    }
}
