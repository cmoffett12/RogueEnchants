package com.simple.rogueenchants.listeners;

import com.simple.rogueenchants.enchants.EnchantManager;
import com.simple.rogueenchants.util.TabColors;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

public class PassiveEffectListener implements Listener {
    public PassiveEffectListener(EnchantManager manager) {
        int period = Math.max(40, manager.getPassiveRefreshTicks());
        Bukkit.getScheduler().runTaskTimer(manager.getPlugin(), () -> {
            Bukkit.getOnlinePlayers().forEach(p ->
                    TabColors.apply(p, manager.getCurrent(p)));
        }, period, period);
    }
}
