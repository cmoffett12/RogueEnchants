package com.simple.rogueenchants.listeners;

import com.simple.rogueenchants.commands.WithdrawXpCommand;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class XpVoucherListener implements Listener {
    private final WithdrawXpCommand withdraw;
    public XpVoucherListener(WithdrawXpCommand withdraw){ this.withdraw = withdraw; }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        ItemStack it = e.getItem();
        if (it == null || it.getType() != Material.PAPER || !it.hasItemMeta()) return;

        var c = it.getItemMeta().getPersistentDataContainer();
        Integer amt = c.get(withdraw.key(), PersistentDataType.INTEGER);
        if (amt == null) return;

        e.setCancelled(true);
        // redeem
        e.getPlayer().giveExp(amt);
        e.getPlayer().sendMessage(ChatColor.GREEN + "Redeemed " + amt + " XP.");

        // consume one
        if (it.getAmount() <= 1) e.getPlayer().getInventory().setItemInMainHand(null);
        else it.setAmount(it.getAmount()-1);
        e.getPlayer().updateInventory();
    }
}
