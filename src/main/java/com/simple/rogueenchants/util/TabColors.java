package com.simple.rogueenchants.util;

import com.simple.rogueenchants.enchants.EnchantData;
import com.simple.rogueenchants.enchants.EnchantType;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public final class TabColors {

    private TabColors() {}

    /** Returns the tab color for an enchant type. Covers ALL enum values. */
    public static ChatColor getColor(EnchantType type) {
        if (type == null) return ChatColor.WHITE;
        return switch (type) {
            case LIFESTEAL       -> ChatColor.DARK_RED;
            case EFFICIENCY      -> ChatColor.GOLD;
            case FORTIFY         -> ChatColor.DARK_PURPLE;
            case AQUA_AFFINITY   -> ChatColor.AQUA;
            case SMITE           -> ChatColor.YELLOW;
            case CHANNELING      -> ChatColor.BLUE;
            case FEATHER_FALLING -> ChatColor.GREEN;
            case SHADOWSTEP      -> ChatColor.GRAY;
        };
    }

    /**
     * Applies the tab list name color to a player based on current enchant.
     * Safe to call with null data; resets to white.
     */
    public static void apply(Player player, EnchantData data) {
        if (player == null) return;
        ChatColor color = (data == null) ? ChatColor.WHITE : getColor(data.type());
        // Keep just color + plain name to avoid long components in tab
        player.setPlayerListName(color + player.getName() + ChatColor.RESET);
    }
}
