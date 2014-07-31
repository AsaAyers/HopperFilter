package com.asaayers.hopperfilter;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Created by asa on 7/30/14.
 */
public class SignHandlers implements Listener {
    public static final int FILTER_SIZE = 54;
    private final HopperFilter plugin;

    Map<Block, Inventory> inventories;

    public SignHandlers(HopperFilter hopperFilter) {
        plugin = hopperFilter;
        inventories = new HashMap<>();
    }

    public static List<Matcher> extractIds(Sign sign) {
        if (sign.getLine(0).trim().equalsIgnoreCase("[Filter]")) {
            List<Matcher> matchers = new ArrayList<>();
            for (String str : sign.getLine(1).split(",")) {
                if (str.trim().length() > 0) {
                    matchers.add(new Matcher(str));
                }
            }
            for (String str : sign.getLine(2).split(",")) {
                if (str.trim().length() > 0) {
                    matchers.add(new Matcher(str));
                }
            }
            for (String str : sign.getLine(3).split(",")) {
                if (str.trim().length() > 0) {
                    matchers.add(new Matcher(str));
                }
            }
            return matchers;
        }

        return null;
    }

    @EventHandler
    public void onPlayerInteractEvent(PlayerInteractEvent event) {
        Block clickedBlock = event.getClickedBlock();

        if (event.getPlayer().isSneaking()
                && event.getAction() == Action.RIGHT_CLICK_BLOCK
                && clickedBlock.getType() == Material.WALL_SIGN
                && clickedBlock.getState() instanceof Sign) {
            Inventory inventory = getInventoryForSign(clickedBlock);
            if (inventory != null) {
                event.getPlayer().openInventory(inventory);
            }

        }
    }

    private Inventory getInventoryForSign(Block clickedBlock) {
        Inventory inventory;

        if (!inventories.containsKey(clickedBlock)) {
            inventory = plugin.getServer().createInventory(null, FILTER_SIZE, "[Filter]");
            Sign sign = (Sign) clickedBlock.getState();
            populate(inventory, extractIds(sign));
            inventories.put(clickedBlock, inventory);
        }

        return inventories.get(clickedBlock);
    }

    private void populate(Inventory inventory, List<Matcher> matchers) {
        inventory.clear();
        for (Matcher matcher : matchers) {
            ItemStack stack = new ItemStack(matcher.getMaterial());
            inventory.addItem(stack);
        }
    }

    @EventHandler
    public void onInventoryClickEvent(InventoryClickEvent event) {
        plugin.getLogger().info("click " + event.getSlot() + " " + event.getSlotType()
                + " " + event.getRawSlot());

        Sign sign = findSign(event.getInventory());
        if (sign != null) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            List<Matcher> matchers = extractIds(sign);

            if (event.getRawSlot() >= FILTER_SIZE) {
                // Add the item to the sign
                matchers.add(new Matcher(item));
            } else {
                // Remove the item from the sign
                Matcher found = null;

                for (Matcher matcher : matchers) {
                    if (matcher.match(item)) {
                        found = matcher;
                    }
                }

                plugin.getLogger().info("remove " + found);

                if (found != null) {
                    matchers.remove(found);
                }
            }
            writeSign(sign, matchers);
            populate(event.getInventory(), matchers);
        }
    }

    private Sign findSign(Inventory inventory) {
        if (inventories.containsValue(inventory)) {
            for (Map.Entry<Block, Inventory> entry : inventories.entrySet()) {
                if (entry.getValue().equals(inventory)) {
                    Block block = entry.getKey();
                    return (Sign) block.getState();
                }
            }
        }
        return null;
    }

    private boolean writeSign(Sign sign, List<Matcher> matchers) {
        int index = 0;
        boolean needComma = false;
        String[] lines = {"", "", ""};

        for (Matcher matcher : matchers) {
            String tmp = needComma ? "," : "";
            tmp += matcher.toString();

            if (lines[index].length() + tmp.length() > 16) {
                index++;
                tmp = matcher.toString();
            } else {
                needComma = true;
            }
            if (index > 2) {
                return false;
            }

            lines[index] = lines[index].concat(tmp);
        }

        sign.setLine(1, lines[0]);
        sign.setLine(2, lines[1]);
        sign.setLine(3, lines[2]);
        sign.update();
        return true;
    }


}
