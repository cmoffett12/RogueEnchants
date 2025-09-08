package com.simple.rogueenchants.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class EggTracker implements Listener {
    private final Map<UUID, Integer> boostCache = new ConcurrentHashMap<>();
    private final NamespacedKey fortifiedKey;

    public EggTracker(NamespacedKey fortifiedKey) { this.fortifiedKey = fortifiedKey; }

    public int getBoost(Player p) { return boostCache.getOrDefault(p.getUniqueId(), 0); }

    public void prime(Player p) { boostCache.put(p.getUniqueId(), scan(p)); }
    public void clear(Player p) { boostCache.remove(p.getUniqueId()); }

    private int scan(Player p) {
        int boost = 0;
        int plain = p.getServer().getPluginManager().getPlugin("RogueEnchants")
                .getConfig().getInt("egg.plain-boost", 1);
        int fortified = p.getServer().getPluginManager().getPlugin("RogueEnchants")
                .getConfig().getInt("egg.fortified-boost", 2);

        for (ItemStack it : p.getInventory().getContents()) {
            if (it == null || it.getType() != Material.DRAGON_EGG) continue;
            ItemMeta meta = it.getItemMeta();
            if (meta != null && meta.getPersistentDataContainer()
                    .has(fortifiedKey, PersistentDataType.BYTE)) {
                boost = Math.max(boost, fortified);
            } else {
                boost = Math.max(boost, plain);
            }
        }
        return boost;
    }

    private void rescan(Player p) { boostCache.put(p.getUniqueId(), scan(p)); }

    @EventHandler public void onJoin(PlayerJoinEvent e){ prime(e.getPlayer()); }
    @EventHandler public void onQuit(PlayerQuitEvent e){ clear(e.getPlayer()); }
    @EventHandler public void onClick(InventoryClickEvent e){ if (e.getWhoClicked() instanceof Player p) rescan(p); }
    @EventHandler public void onPickup(EntityPickupItemEvent e){ if (e.getEntity() instanceof Player p) rescan(p); }
    @EventHandler public void onDrop(PlayerDropItemEvent e){ rescan(e.getPlayer()); }
}
