package com.simple.rogueenchants.listeners;

import com.simple.rogueenchants.enchants.EnchantManager;
import com.simple.rogueenchants.enchants.EnchantType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Periodically grants HASTE if player has BUILDER_HASTE. */
public class BuilderHasteFallback implements Listener {
    private final EnchantManager manager;

    public BuilderHasteFallback(EnchantManager manager) {
        this.manager = manager;
        final int period = 20 * 60 * 5; // every 5 minutes
        Bukkit.getScheduler().runTaskTimer(manager.getPlugin(), () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                var data = manager.getCurrent(p);
                if (data == null || data.type() != EnchantType.EFFICIENCY) continue;

                int amplifier = Math.max(0, data.level() - 1);
                p.addPotionEffect(new PotionEffect(
                        PotionEffectType.HASTE,
                        20 * 60 * 6, // 6 min > 5 min period
                        amplifier,
                        true,  // ambient
                        false, // particles off
                        true   // icon on
                ));
            }
        }, 0L, period);
    }
}
