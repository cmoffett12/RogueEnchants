package com.simple.rogueenchants.commands;

import com.simple.rogueenchants.RogueEnchantsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class WithdrawXpCommand implements CommandExecutor {
    private final RogueEnchantsPlugin plugin;
    private final NamespacedKey key;

    public WithdrawXpCommand(RogueEnchantsPlugin plugin){
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "xp_voucher");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
        if (!p.hasPermission("rogueenchants.withdraw")) { p.sendMessage(ChatColor.RED+"No permission."); return true; }
        if (args.length < 1) { p.sendMessage(ChatColor.YELLOW+"Usage: /withdrawxp <amount>"); return true; }

        int amount;
        try { amount = Math.max(1, Integer.parseInt(args[0])); } catch (Exception ex) {
            p.sendMessage(ChatColor.RED+"Amount must be a number."); return true;
        }

        int total = getPlayerTotalExp(p);
        if (amount > total) { p.sendMessage(ChatColor.RED+"You don't have that much XP."); return true; }

        addPlayerTotalExp(p, -amount); // remove XP

        ItemStack note = new ItemStack(Material.PAPER);
        ItemMeta meta = note.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("xp.voucher-name", "&aXP Voucher")));
        List<String> lore = plugin.getConfig().getStringList("xp.voucher-lore");
        lore.replaceAll(s -> ChatColor.translateAlternateColorCodes('&', s.replace("%amount%", String.valueOf(amount))));
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, amount);
        note.setItemMeta(meta);
        p.getInventory().addItem(note);
        p.sendMessage(ChatColor.GREEN + "Created XP voucher for " + amount + " XP.");
        return true;
    }

    // ===== XP math helpers (works with modern Bukkit) =====
    private int getPlayerTotalExp(Player player) {
        int level = player.getLevel();
        float progress = player.getExp();
        int expAtLevel = getExpAtLevel(level);
        int expToNext = getExpToNext(level);
        return expAtLevel + Math.round(progress * expToNext);
    }

    private void addPlayerTotalExp(Player player, int amount) {
        int total = Math.max(0, getPlayerTotalExp(player) + amount);
        player.setExp(0); player.setLevel(0);
        player.giveExp(total);
    }

    private int getExpToNext(int level) {
        if (level >= 30) return 112 + (level - 30) * 9;
        if (level >= 15) return 37 + (level - 15) * 5;
        return 7 + level * 2;
    }

    private int getExpAtLevel(int level) {
        int exp = 0;
        for (int i = 0; i < level; i++) exp += getExpToNext(i);
        return exp;
    }

    // ===== listener hook to redeem (place in a separate Listener or inline here) =====
    public NamespacedKey key() { return key; }
}
