package com.simple.rogueenchants.listeners;

import com.simple.rogueenchants.enchants.EnchantManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {
    private final EnchantManager manager;
    public JoinListener(EnchantManager manager) { this.manager = manager; }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        manager.ensureCurrentExists(e.getPlayer()); // assigns once if missing; otherwise leaves as-is
    }
}
