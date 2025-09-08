package com.simple.rogueenchants.commands;

import com.simple.rogueenchants.RogueEnchantsPlugin;
import com.simple.rogueenchants.enchants.EnchantManager;
import com.simple.rogueenchants.enchants.EnchantType;
import com.simple.rogueenchants.npc.TraderManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReCommand implements CommandExecutor, TabCompleter {

    private final EnchantManager manager;
    private final RogueEnchantsPlugin plugin;

    public ReCommand(EnchantManager manager, RogueEnchantsPlugin plugin) {
        this.manager = manager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("rogueenchants.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /re <list|view|set|level|reroll|steal|reload|scroll|trader> ...");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "list" -> {
                StringBuilder sb = new StringBuilder(ChatColor.GREEN + "Enchants: ");
                for (EnchantType t : EnchantType.values()) sb.append(t.name()).append(" ");
                sender.sendMessage(sb.toString());
                return true;
            }
            case "view" -> {
                if (args.length < 2) { sender.sendMessage(ChatColor.RED + "Usage: /re view <player>"); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage(ChatColor.RED + "Player not found."); return true; }
                var d = manager.getCurrent(target);
                sender.sendMessage(ChatColor.AQUA + target.getName() + ChatColor.GRAY + " -> " + (d == null ? "none" : (d.type() + " " + d.level())));
                return true;
            }
            case "set" -> { // /re set <player> <ENCHANT> <level>
                if (args.length < 4) { sender.sendMessage(ChatColor.RED + "Usage: /re set <player> <ENCHANT> <level>"); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage(ChatColor.RED + "Player not found."); return true; }
                EnchantType type;
                try { type = EnchantType.valueOf(args[2].toUpperCase()); }
                catch (Exception ex) { sender.sendMessage(ChatColor.RED + "Unknown enchant: " + args[2]); return true; }
                int lvl;
                try { lvl = Integer.parseInt(args[3]); }
                catch (Exception ex) { sender.sendMessage(ChatColor.RED + "Bad level."); return true; }

                // 3-arg version
                manager.setCurrent(target, type, lvl);
                sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + " -> " + type + " " + lvl);
                return true;
            }
            case "level" -> { // /re level <player> <ENCHANT> <level>
                if (args.length < 4) { sender.sendMessage(ChatColor.RED + "Usage: /re level <player> <ENCHANT> <level>"); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage(ChatColor.RED + "Player not found."); return true; }
                EnchantType type;
                try { type = EnchantType.valueOf(args[2].toUpperCase()); }
                catch (Exception ex) { sender.sendMessage(ChatColor.RED + "Unknown enchant: " + args[2]); return true; }
                int lvl;
                try { lvl = Integer.parseInt(args[3]); }
                catch (Exception ex) { sender.sendMessage(ChatColor.RED + "Bad level."); return true; }

                // 3-arg version
                manager.setCurrent(target, type, lvl);
                sender.sendMessage(ChatColor.GREEN + "Level set " + target.getName() + " -> " + type + " " + lvl);
                return true;
            }
            case "trader" -> { // spawn OP trader at your feet
                if (!(sender instanceof Player p)) { sender.sendMessage("Run in-game."); return true; }
                TraderManager tm = new TraderManager();
                tm.spawnTrader(p.getLocation());
                p.sendMessage(ChatColor.GOLD + "Spawned Rogue Trader.");
                return true;
            }
            // stubs—keep your old handlers if you had them implemented
            case "reroll", "steal", "reload", "scroll" -> {
                sender.sendMessage(ChatColor.RED + "Subcommand not implemented in this minimal ReCommand. (Use your existing file to keep these.)");
                return true;
            }
            default -> {
                sender.sendMessage(ChatColor.RED + "Unknown: " + sub);
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("rogueenchants.admin")) return List.of();

        if (args.length == 1) {
            return Arrays.asList("list","view","set","level","trader","reroll","steal","reload","scroll");
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("level"))) {
            List<String> out = new ArrayList<>();
            for (EnchantType t : EnchantType.values()) out.add(t.name());
            return out;
        }
        return List.of();
    }
}
