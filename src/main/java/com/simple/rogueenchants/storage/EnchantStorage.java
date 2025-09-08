package com.simple.rogueenchants.storage;

import com.simple.rogueenchants.RogueEnchantsPlugin;
import com.simple.rogueenchants.enchants.EnchantData;
import com.simple.rogueenchants.enchants.EnchantType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class EnchantStorage {
    private final RogueEnchantsPlugin plugin;
    private final File file;
    private final FileConfiguration yml;

    public EnchantStorage(RogueEnchantsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        this.yml = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
    }

    /** Load CURRENT enchants only */
    public Map<UUID, EnchantData> loadCurrent() {
        Map<UUID, EnchantData> map = new HashMap<>();
        String section = "current";
        if (!yml.isConfigurationSection(section)) return map;
        for (String key : Objects.requireNonNull(yml.getConfigurationSection(section)).getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String typeStr = yml.getString(section + "." + key + ".type");
                int level = yml.getInt(section + "." + key + ".level", 1);
                if (typeStr == null) continue;
                EnchantType type = EnchantType.valueOf(typeStr);
                map.put(uuid, new EnchantData(type, Math.max(1, level)));
            } catch (Exception ignored) {}
        }
        return map;
    }

    /** Save all CURRENT enchants */
    public void saveCurrent(Map<UUID, EnchantData> data) {
        String section = "current";
        yml.set(section, null);
        for (Map.Entry<UUID, EnchantData> e : data.entrySet()) {
            String base = section + "." + e.getKey();
            yml.set(base + ".type", e.getValue().type().name());
            yml.set(base + ".level", e.getValue().level());
        }
        try { yml.save(file); } catch (IOException ex) {
            plugin.getLogger().warning("Failed to save data.yml (current): " + ex.getMessage());
        }
    }

    /** Save one CURRENT enchant */
    public void saveCurrentOne(UUID uuid, EnchantData d) {
        String base = "current." + uuid;
        yml.set(base + ".type", d.type().name());
        yml.set(base + ".level", d.level());
        try { yml.save(file); } catch (IOException ex) {
            plugin.getLogger().warning("Failed to save data.yml (current one): " + ex.getMessage());
        }
    }
}
