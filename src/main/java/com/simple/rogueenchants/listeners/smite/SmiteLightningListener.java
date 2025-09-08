package com.simple.rogueenchants.listeners.smite;

import com.simple.rogueenchants.enchants.EnchantManager;
import com.simple.rogueenchants.enchants.EnchantType;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class SmiteLightningListener implements Listener {
    private final EnchantManager manager;
    private final Map<UUID, Long> cooldownUntil = new HashMap<>();
    private final List<PotionEffectType> pool;

    public SmiteLightningListener(EnchantManager manager) {
        this.manager = manager;
        // Keep the pool to widely supported positive effects in your API build
        pool = List.of(
            PotionEffectType.SPEED,
            PotionEffectType.REGENERATION,
            PotionEffectType.ABSORPTION,
            PotionEffectType.FIRE_RESISTANCE,
            PotionEffectType.NIGHT_VISION,
            PotionEffectType.WATER_BREATHING,
            PotionEffectType.CONDUIT_POWER,
            PotionEffectType.DOLPHINS_GRACE
        );
    }

    @EventHandler
    public void onBigHit(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();

        var d = manager.getCurrent(p);
        if (d == null || d.type() != EnchantType.SMITE) return;
        if (d.level() < 3) return;

        double finalDmg = e.getFinalDamage();
        double th3 = manager.getPlugin().getConfig().getDouble("smite.lightning.threshold-lvl3", 10.0);
        double th4 = manager.getPlugin().getConfig().getDouble("smite.lightning.threshold-lvl4", 5.0);
        double threshold = d.level() >= 4 ? th4 : th3;

        if (finalDmg < threshold) return;

        int cooldownSec = Math.max(0, manager.getPlugin().getConfig().getInt("smite.lightning.cooldown-seconds", 20));
        long now = System.currentTimeMillis();
        Long until = cooldownUntil.get(p.getUniqueId());
        if (until != null && until > now) return; // still on cooldown
        cooldownUntil.put(p.getUniqueId(), now + cooldownSec * 1000L);

        // Lightning for drama
        Location loc = p.getLocation();
        World w = loc.getWorld();
        if (w != null) w.strikeLightning(loc);

        // Random positive effect
        int durSec = Math.max(1, manager.getPlugin().getConfig().getInt("smite.lightning.positive-effect-seconds", 30));
        PotionEffectType type = pool.get(new Random().nextInt(pool.size()));
        p.addPotionEffect(new PotionEffect(type, durSec * 20, 0, true, true, true));
    }
}
