package com.simple.rogueenchants.enchants;

import com.simple.rogueenchants.RogueEnchantsPlugin;
import com.simple.rogueenchants.storage.EnchantStorage;
import com.simple.rogueenchants.util.TabColors;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class EnchantManager {
    private final RogueEnchantsPlugin plugin;
    private final EnchantStorage storage;

    // CURRENT only (persisted)
    private final Map<UUID, EnchantData> current = new HashMap<>();

    public EnchantManager(RogueEnchantsPlugin plugin) {
        this.plugin = plugin;
        this.storage = new EnchantStorage(plugin);
        this.current.putAll(storage.loadCurrent());
    }

    public RogueEnchantsPlugin getPlugin() { return plugin; }

    // ---- caps & helpers ----
    public int baseMax() { return plugin.getConfig().getInt("base-max-level", 3); }
    public int maxLevelFor(Player p) { return baseMax() + plugin.getEggTracker().getBoost(p); }
    public int getPassiveRefreshTicks() { return plugin.getConfig().getInt("passive-refresh-ticks", 200); }

    // ---- accessors ----
    public EnchantData getCurrent(Player p) { return current.get(p.getUniqueId()); }

    /**
     * Ensure player has a CURRENT enchant. If missing, assign a random one (level 1),
     * save it, and set tab color. If present, do nothing (no re-randomize).
     */
    public boolean ensureCurrentExists(Player p) {
        if (current.containsKey(p.getUniqueId())) {
            TabColors.apply(p, current.get(p.getUniqueId()));
            return false;
        }
        setCurrent(p, randomType(), 1);
        p.sendMessage("§aYou received an enchant: §e" + current.get(p.getUniqueId()).type().name() + " §7x1");
        return true;
    }

    /** Set CURRENT (clamped by player cap), persist, update tab color. */
    public void setCurrent(Player p, EnchantType type, int level) {
        int clamped = Math.min(Math.max(1, level), maxLevelFor(p));
        EnchantData data = new EnchantData(type, clamped);
        current.put(p.getUniqueId(), data);
        storage.saveCurrentOne(p.getUniqueId(), data);
        TabColors.apply(p, data);
    }

    /** Reroll CURRENT randomly to given level. */
    public void rerollCurrent(Player p, int level) {
        setCurrent(p, randomType(), level);
    }

    /** Steal: killer gets victim CURRENT; victim rerolls to level 1. */
    public void steal(Player killer, Player victim) {
        EnchantData v = getCurrent(victim);
        if (v != null) setCurrent(killer, v.type(), v.level());
        rerollCurrent(victim, 1);
    }

    private EnchantType randomType() {
        EnchantType[] types = EnchantType.values();
        return types[ThreadLocalRandom.current().nextInt(types.length)];
    }

    // effect helpers (unchanged)
    public double getLifestealPercent(int level) {
        FileConfiguration cfg = plugin.getConfig();
        return cfg.getDouble("enchants.lifesteal.heal-percent-per-level", 0.08) * level;
    }
    public int getHasteAmplifier(int level) {
        return Math.max(0, plugin.getConfig().getInt("enchants.builder_haste.amplifier-per-level", 1) * Math.max(0, level - 1));
    }
    public int getResistanceAmplifier(int level) {
        return Math.max(0, plugin.getConfig().getInt("enchants.fortify.amplifier-per-level", 1) * Math.max(0, level - 1));
    }
}
