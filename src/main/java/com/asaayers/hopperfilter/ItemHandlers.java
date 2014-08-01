package com.asaayers.hopperfilter;

import org.bukkit.block.Hopper;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Set;

/**
 * Created by asa on 7/30/14.
 */
public class ItemHandlers implements Listener {

    private final HopperFilter plugin;

    public ItemHandlers(HopperFilter hopperFilter) {
        plugin = hopperFilter;
    }

    @EventHandler
    public void onInventoryMoveItemEvent(InventoryMoveItemEvent event) {
        if (event.getDestination().getHolder() instanceof Hopper) {
            Hopper hopper = (Hopper) event.getDestination().getHolder();
            hopperHandler(hopper, event.getItem(), event);
        }
    }

    @EventHandler
    public void onInventoryPickupItemEvent(InventoryPickupItemEvent event) {
        if (event.getInventory().getHolder() instanceof Hopper) {
            Hopper hopper = (Hopper) event.getInventory().getHolder();
            hopperHandler(hopper, event.getItem().getItemStack(), event);
        }
    }

    private void hopperHandler(Hopper hopper, ItemStack item, Cancellable event) {
        Set<Matcher> matchers = plugin.findSign(hopper.getLocation().getBlock());

        if (matchers != null) {
            for (Matcher matcher : matchers) {
                if (matcher.match(item)) {
                    return;
                }
            }
            event.setCancelled(true);
        }
    }

}
