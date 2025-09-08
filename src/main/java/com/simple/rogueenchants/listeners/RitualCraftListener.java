package com.simple.rogueenchants.listeners;

import com.simple.rogueenchants.RogueEnchantsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class RitualCraftListener implements Listener {
    private final RogueEnchantsPlugin plugin;
    private final NamespacedKey tokenKey;

    public RitualCraftListener(RogueEnchantsPlugin plugin) {
        this.plugin = plugin;
        this.tokenKey = new NamespacedKey(plugin, "fortified_egg_ritual_token");
        registerRitualRecipe();
    }

    public static ItemStack makeRitualToken(RogueEnchantsPlugin plugin) {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Fortified Egg Ritual");
        meta.setLore(List.of(ChatColor.GRAY + "Click the result to begin the ritual."));
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "fortified_egg_ritual_token"),
                PersistentDataType.BYTE, (byte) 1
        );
        paper.setItemMeta(meta);
        return paper;
    }

    private void registerRitualRecipe() {
        NamespacedKey key = new NamespacedKey(plugin, "fortified_egg_ritual");
        ItemStack token = makeRitualToken(plugin);

        ShapedRecipe recipe = new ShapedRecipe(key, token);
        recipe.shape(" N ", "TET", "D G");
        recipe.setIngredient('N', Material.NETHER_STAR);
        recipe.setIngredient('T', Material.NETHERITE_BLOCK);
        recipe.setIngredient('E', Material.DRAGON_EGG);
        recipe.setIngredient('D', Material.DIAMOND_BLOCK);
        recipe.setIngredient('G', Material.GOLD_BLOCK);

        try {
            plugin.getServer().addRecipe(recipe);
        } catch (IllegalStateException ignored) {}
    }

    @EventHandler
    public void onCraftToken(CraftItemEvent e) {
        if (e.getRecipe() == null) return;
        ItemStack result = e.getRecipe().getResult();
        if (result == null || !result.hasItemMeta()) return;
        ItemMeta rMeta = result.getItemMeta();
        if (rMeta == null || !rMeta.getPersistentDataContainer().has(tokenKey, PersistentDataType.BYTE)) return;

        // Java 17 style: instanceof + explicit cast
        if (!(e.getInventory() instanceof CraftingInventory)) return;
        CraftingInventory ci = (CraftingInventory) e.getInventory();

        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();

        // Cancel vanilla craft; consume ingredients; start ritual
        e.setCancelled(true);

        // Verify exact matrix (slots 0..8)
        ItemStack[] m = ci.getMatrix();
        if (!is(m[0], Material.AIR) ||
            !is(m[1], Material.NETHER_STAR) ||
            !is(m[2], Material.AIR) ||
            !is(m[3], Material.NETHERITE_BLOCK) ||
            !is(m[4], Material.DRAGON_EGG) ||
            !is(m[5], Material.NETHERITE_BLOCK) ||
            !is(m[6], Material.DIAMOND_BLOCK) ||
            !is(m[7], Material.AIR) ||
            !is(m[8], Material.GOLD_BLOCK)) {
            p.sendMessage(ChatColor.RED + "The ritual pattern is invalid.");
            return;
        }

        // Consume one of each used slot
        for (int i = 0; i < m.length; i++) {
            if (m[i] == null || m[i].getType() == Material.AIR) continue;
            int amt = m[i].getAmount();
            m[i] = (amt <= 1) ? null : new ItemStack(m[i].getType(), amt - 1);
        }
        ci.setMatrix(m);
        ci.setResult(null);
        p.updateInventory();

        plugin.getRitualManager().startRitual(p);
        p.sendMessage(ChatColor.DARK_AQUA + "The ritual begins...");
    }

    private boolean is(ItemStack it, Material mat) {
        if (mat == Material.AIR) return it == null || it.getType() == Material.AIR;
        return it != null && it.getType() == mat;
    }
}
