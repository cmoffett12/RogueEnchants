package com.simple.rogueenchants.npc;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class TraderManager {

    public Villager spawnTrader(Location where) {
        Villager v = (Villager) where.getWorld().spawnEntity(where, EntityType.VILLAGER);
        v.setVillagerType(Villager.Type.TAIGA);
        v.setProfession(Villager.Profession.WEAPONSMITH);
        v.setVillagerLevel(5);
        v.setAI(true);
        v.setInvulnerable(false);
        v.setCustomName(ChatColor.GOLD + "Rogue Trader");
        v.setCustomNameVisible(true);
        v.setPersistent(true);

        v.setRecipes(buildRecipes());
        return v;
    }

    private List<MerchantRecipe> buildRecipes() {
        List<MerchantRecipe> list = new ArrayList<>();

        // Helper currency
        ItemStack EMERALDS_32 = new ItemStack(Material.EMERALD, 32);
        ItemStack DIAMONDS_8  = new ItemStack(Material.DIAMOND, 8);
        ItemStack NETHERITE_SCRAP_4 = new ItemStack(Material.NETHERITE_SCRAP, 4);

        // 1) Mace (vanilla 1.21)
        ItemStack mace = new ItemStack(Material.MACE);
        mace.editMeta(m -> {
            m.setDisplayName(ChatColor.AQUA + "Storm Mace");
            m.addEnchant(Enchantment.SHARPNESS, 5, true);
            m.addEnchant(Enchantment.KNOCKBACK, 2, true);
        });
        list.add(recipe(mace, 12, EMERALDS_32, NETHERITE_SCRAP_4));

        // 2) Netherite Chestplate
        ItemStack chest = new ItemStack(Material.NETHERITE_CHESTPLATE);
        chest.editMeta(m -> {
            m.setDisplayName(ChatColor.LIGHT_PURPLE + "Bulwark Plate");
            m.addEnchant(Enchantment.PROTECTION, 4, true);
            m.addEnchant(Enchantment.UNBREAKING, 3, true);
            m.addEnchant(Enchantment.MENDING, 1, true);
        });
        list.add(recipe(chest, 8, EMERALDS_32, DIAMONDS_8));

        // 3) Custom OP item example (tweak later)
        ItemStack stormbrand = new ItemStack(Material.NETHERITE_SWORD);
        stormbrand.editMeta((ItemMeta m) -> {
            m.setDisplayName(ChatColor.BLUE + "Stormbrand");
            m.addEnchant(Enchantment.SHARPNESS, 5, true);
            m.addEnchant(Enchantment.SWEEPING_EDGE, 3, true);
            m.addEnchant(Enchantment.UNBREAKING, 3, true);
            m.addEnchant(Enchantment.MENDING, 1, true);
        });
        list.add(recipe(stormbrand, 6, new ItemStack(Material.EMERALD, 48)));

        // 4) Elytra (suggestion)
        ItemStack elytra = new ItemStack(Material.ELYTRA);
        elytra.editMeta(m -> m.setDisplayName(ChatColor.GREEN + "Glider's Gift"));
        list.add(recipe(elytra, 5, new ItemStack(Material.EMERALD, 64), new ItemStack(Material.PHANTOM_MEMBRANE, 10)));

        return list;
    }

    private MerchantRecipe recipe(ItemStack result, int maxUses, ItemStack... costs) {
        MerchantRecipe r = new MerchantRecipe(result, maxUses);
        for (ItemStack c : costs) r.addIngredient(c);
        r.setExperienceReward(true);
        r.setVillagerExperience(10);
        r.setPriceMultiplier(0.0f);
        return r;
    }
}
