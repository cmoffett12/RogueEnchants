package com.simple.rogueenchants.listeners;

import com.simple.rogueenchants.enchants.EnchantManager;
import com.simple.rogueenchants.enchants.EnchantType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class WaterAffinityListener implements Listener {
    private final EnchantManager manager;

    public WaterAffinityListener(EnchantManager manager) {
        this.manager = manager;

        int period = manager.getPlugin().getConfig().getInt("water_affinity.check-period-ticks", 40);
        int conduitSec = manager.getPlugin().getConfig().getInt("water_affinity.conduit-seconds", 8);
        int dolphinSec = manager.getPlugin().getConfig().getInt("water_affinity.dolphins-seconds", 8);
        int dolphinAmp = manager.getPlugin().getConfig().getInt("water_affinity.dolphins-amplifier", 0);

        Bukkit.getScheduler().runTaskTimer(manager.getPlugin(), () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                var d = manager.getCurrent(p);
                if (d == null || d.type() != EnchantType.AQUA_AFFINITY) continue;

                if (isInWater(p)) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, conduitSec * 20, 0, true, false, true));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, dolphinSec * 20, dolphinAmp, true, false, true));
                }
            }
        }, 0L, period);
    }

    private boolean isInWater(Player p) {
        Block b = p.getLocation().getBlock();
        if (b.getType() == Material.WATER) return true;
        return b.isLiquid();
    }
}
