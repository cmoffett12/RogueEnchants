package com.simple.rogueenchants.listeners.smite;

import com.simple.rogueenchants.enchants.EnchantManager;
import com.simple.rogueenchants.enchants.EnchantType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SmiteHeroListener implements Listener {
    public SmiteHeroListener(EnchantManager manager) {
        var cfg = manager.getPlugin().getConfig();
        int refresh = Math.max(40, cfg.getInt("smite.hero.refresh-ticks", 200));
        int seconds = Math.max(2, cfg.getInt("smite.hero.seconds", 90));
        int ampL2 = Math.max(0, cfg.getInt("smite.hero.amplifier-lvl2", 0));
        int ampL4 = Math.max(0, cfg.getInt("smite.hero.amplifier-lvl4", 1));

        Bukkit.getScheduler().runTaskTimer(manager.getPlugin(), () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                var d = manager.getCurrent(p);
                if (d == null || d.type() != EnchantType.SMITE) continue;
                if (d.level() < 2) continue;

                int amp = (d.level() >= 4 ? ampL4 : ampL2);
                // Re-apply with small overlap to keep it stable
                p.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE,
                        (seconds * 20), amp, true, false, true));
            }
        }, 0L, refresh);
    }
}
