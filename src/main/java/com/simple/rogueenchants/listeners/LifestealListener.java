package com.simple.rogueenchants.listeners;

import com.simple.rogueenchants.enchants.EnchantManager;
import com.simple.rogueenchants.enchants.EnchantType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class LifestealListener implements Listener {
    private final EnchantManager manager;
    public LifestealListener(EnchantManager manager) { this.manager = manager; }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        Player p = (Player) e.getDamager();

        var data = manager.getCurrent(p);
        if (data == null || data.type() != EnchantType.LIFESTEAL) return;

        double healPct = manager.getLifestealPercent(data.level()); // e.g. 0.08 per level
        double heal = Math.max(0.0, e.getFinalDamage() * healPct);

        double max = p.getMaxHealth();
        p.setHealth(Math.min(max, p.getHealth() + heal));
    }
}
