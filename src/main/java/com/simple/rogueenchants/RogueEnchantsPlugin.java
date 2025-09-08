package com.simple.rogueenchants;

import com.simple.rogueenchants.commands.ReCommand;
import com.simple.rogueenchants.commands.WithdrawXpCommand;
import com.simple.rogueenchants.enchants.EnchantManager;
import com.simple.rogueenchants.listeners.AnvilGuard;
import com.simple.rogueenchants.listeners.BuilderHasteFallback;
import com.simple.rogueenchants.listeners.DamageReductionListener;
import com.simple.rogueenchants.listeners.DragonDeathListener;
import com.simple.rogueenchants.listeners.JoinListener;
import com.simple.rogueenchants.listeners.LifestealListener;
import com.simple.rogueenchants.listeners.RitualCraftListener;
import com.simple.rogueenchants.listeners.WaterAffinityListener;
import com.simple.rogueenchants.listeners.XpVoucherListener;
import com.simple.rogueenchants.ritual.RitualManager;
import com.simple.rogueenchants.util.EggTracker;
import com.simple.rogueenchants.util.TabColors;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class RogueEnchantsPlugin extends JavaPlugin {

    private static RogueEnchantsPlugin instance;

    // Core managers
    private EnchantManager enchantManager;
    private EggTracker eggTracker;
    private RitualManager ritualManager;

    // Persistent Data Keys
    private NamespacedKey fortifiedEggKey;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // --- PDC keys ---
        fortifiedEggKey = new NamespacedKey(this, "fortified_egg");

        // --- core managers ---
        enchantManager = new EnchantManager(this);
        eggTracker = new EggTracker(fortifiedEggKey);
        ritualManager = new RitualManager(this);

        // --- commands ---
        // /re (admin/testing)
        ReCommand reCmd = new ReCommand(enchantManager, this);
        if (getCommand("re") != null) {
            getCommand("re").setExecutor(reCmd);
            getCommand("re").setTabCompleter(reCmd);
        } else {
            getLogger().warning("Command 're' not found in plugin.yml");
        }

        // /withdrawxp
        WithdrawXpCommand withdraw = new WithdrawXpCommand(this);
        if (getCommand("withdrawxp") != null) {
            getCommand("withdrawxp").setExecutor(withdraw);
        } else {
            getLogger().warning("Command 'withdrawxp' not found in plugin.yml");
        }

        // --- event listeners ---
        var pm = Bukkit.getPluginManager();

        // Channeling (unified)
        pm.registerEvents(new com.simple.rogueenchants.listeners.ChannelingListener(enchantManager), this);
        pm.registerEvents(new com.simple.rogueenchants.listeners.FeatherFallingListener(enchantManager), this);
        pm.registerEvents(new com.simple.rogueenchants.listeners.ShadowstepListener(enchantManager), this);


        // Smite
        pm.registerEvents(new com.simple.rogueenchants.listeners.smite.SmiteDamageListener(enchantManager), this);
        pm.registerEvents(new com.simple.rogueenchants.listeners.smite.SmiteHeroListener(enchantManager), this);
        pm.registerEvents(new com.simple.rogueenchants.listeners.smite.SmiteLightningListener(enchantManager), this);
        pm.registerEvents(new com.simple.rogueenchants.listeners.smite.SmiteFearListener(enchantManager), this);

        // Inventory/Egg tracking (cached boost for caps)
        pm.registerEvents(eggTracker, this);

        // Gameplay & join initialization
        pm.registerEvents(new JoinListener(enchantManager), this);
        pm.registerEvents(new com.simple.rogueenchants.listeners.CombatListener(enchantManager, this), this);

        // Enchant effects
        pm.registerEvents(new LifestealListener(enchantManager), this);
        pm.registerEvents(new DamageReductionListener(enchantManager), this);
        pm.registerEvents(new WaterAffinityListener(enchantManager), this);
        pm.registerEvents(new BuilderHasteFallback(enchantManager), this);

        // Ritual crafting & extra-egg on next dragon kill
        pm.registerEvents(new RitualCraftListener(this), this);
        pm.registerEvents(new DragonDeathListener(this), this);

        // XP voucher redemption
        pm.registerEvents(new XpVoucherListener(withdraw), this);

        // Allow only Mending/Unbreaking books on anvil
        pm.registerEvents(new AnvilGuard(), this);

        pm.registerEvents(new com.simple.rogueenchants.listeners.EnchantBookListener(enchantManager, this), this);

        // --- recipes (Hamburger) ---
        registerHamburgerRecipe();

        // Refresh tab colors after reload for all online players
        getServer().getScheduler().runTask(this, () ->
                getServer().getOnlinePlayers().forEach(p ->
                        TabColors.apply(p, enchantManager.getCurrent(p))));

        getLogger().info("RogueEnchants enabled!");
    }

    @Override
    public void onDisable() {
        // If you later add async persistence, flush here.
        getLogger().info("RogueEnchants disabled.");
    }

    private void registerHamburgerRecipe() {
        try {
            NamespacedKey key = new NamespacedKey(this, "hamburger");
            // Result item is your custom Hamburger (named, lore, PDC)
            ItemStack result = com.simple.rogueenchants.items.Hamburger.create(this);

            ShapelessRecipe recipe = new ShapelessRecipe(key, result);
            recipe.addIngredient(Material.BREAD);
            recipe.addIngredient(Material.COOKED_BEEF);
            recipe.addIngredient(Material.APPLE);

            // addRecipe will replace identical keys on modern Paper; if it throws, ignore once.
            getServer().addRecipe(recipe);
        } catch (Throwable t) {
            getLogger().warning("Failed to register Hamburger recipe: " + t.getMessage());
        }
    }

    // --- getters ---
    public static RogueEnchantsPlugin getInstance() { return instance; }
    public EnchantManager getEnchantManager() { return enchantManager; }
    public EggTracker getEggTracker() { return eggTracker; }
    public RitualManager getRitualManager() { return ritualManager; }
    public NamespacedKey getFortifiedEggKey() { return fortifiedEggKey; }
}
