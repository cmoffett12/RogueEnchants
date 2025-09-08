package com.simple.rogueenchants.listeners;

import com.simple.rogueenchants.RogueEnchantsPlugin;
import com.simple.rogueenchants.items.Hamburger;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class HamburgerListener implements Listener {
    private final RogueEnchantsPlugin plugin;
    public HamburgerListener(RogueEnchantsPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onEat(PlayerItemConsumeEvent e) {
        if (!Hamburger.isHamburger(e.getItem(), plugin)) return;

        Player p = e.getPlayer();

        // Heal instantly
        p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 8.0)); // 4 hearts

        // Effects like Golden Head
        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 10, 1)); // Regen II, 10s
        p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 120, 0)); // Absorption I, 2min

        p.sendMessage(ChatColor.GOLD + "You feel empowered after eating a Hamburger!");
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_BURP, 1f, 1f);
    }
}
