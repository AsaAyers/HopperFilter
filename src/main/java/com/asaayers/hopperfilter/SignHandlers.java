package com.asaayers.hopperfilter;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.Metadatable;

import java.util.*;

public class SignHandlers implements Listener {
    public static final int FILTER_SIZE = 54;
    public static final String FILTER_INVENTORY = "HopperFilterInventory";
    private final HopperFilter plugin;

    Map<Block, Inventory> inventories;

    public SignHandlers(HopperFilter hopperFilter) {
        plugin = hopperFilter;
        inventories = new HashMap<>();
    }

    public static Set<Matcher> extractIds(Sign sign) {
        if (sign.getLine(0).trim().equalsIgnoreCase("[Filter]")) {

            Set<Matcher> matchers = new HashSet<>();

            for (int i = 1; i <= 3; i++) {
                for (String str : sign.getLine(i).split(",")) {
                    if (str.trim().length() > 0) {
                        Matcher m = new Matcher(str);

                        // Verify that it produces a valid item.
                        if (m.getItemStack() != null) {
                            matchers.add(m);
                        }
                    }
                }
            }
            return matchers;
        }

        return null;
    }

    @EventHandler
    public void onBlockPlaceEvent(BlockPlaceEvent event) {
        Block clickedBlock = event.getBlockAgainst();

        if (clickedBlock.getType() == Material.WALL_SIGN
                && clickedBlock.getState() instanceof Sign) {
            Inventory inventory = getInventoryForSign(clickedBlock);
            if (inventory != null) {
                event.getPlayer().openInventory(inventory);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEvent(PlayerInteractEvent event) {
        Block clickedBlock = event.getClickedBlock();

        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)
                && clickedBlock.getType() == Material.WALL_SIGN
                && clickedBlock.getState() instanceof Sign) {
            Inventory inventory = getInventoryForSign(clickedBlock);
            if (inventory != null) {
                event.getPlayer().openInventory(inventory);
                event.setCancelled(true);
            }

        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().hasMetadata(FILTER_INVENTORY)) {
            event.getBlock().removeMetadata(FILTER_INVENTORY, plugin);
        }
    }

    private Object getMeta(Metadatable obj, String key) {
        List<MetadataValue> meta = obj.getMetadata(key);

        for (MetadataValue item : meta) {
            if (item.getOwningPlugin() == plugin) {
                return item.value();
            }
        }
        return null;
    }

    private Inventory getInventoryForSign(Block clickedBlock) {
        Inventory inventory;

        Object foo = getMeta(clickedBlock, FILTER_INVENTORY);

        if (foo instanceof Inventory) {
            inventory = (Inventory) foo;
        } else {
            inventory = plugin.getServer().createInventory(null, FILTER_SIZE, "[Filter]");
            Sign sign = (Sign) clickedBlock.getState();
            populate(inventory, extractIds(sign));
            inventories.put(clickedBlock, inventory);
            clickedBlock.setMetadata(FILTER_INVENTORY, new FixedMetadataValue(plugin, inventory));
        }

        return inventory;
    }

    private void populate(Inventory inventory, Set<Matcher> matchers) {
        inventory.clear();
        for (Matcher matcher : matchers) {
            inventory.addItem(matcher.getItemStack());
        }
    }

    @EventHandler
    public void onInventoryClickEvent(InventoryClickEvent event) {


        Sign sign = findSign(event.getInventory());
        if (sign != null) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType() == Material.AIR) {
                return;
            }
            Set<Matcher> matchers = extractIds(sign);


            // Remove the item from the sign

            if (event.getRawSlot() >= FILTER_SIZE) {
                // Add the item to the sign

                Matcher generic = null;

                for (Matcher matcher : matchers) {
                    // If the filter already contains the generic form of this
                    // item, remove it and replace it with an exact match.
                    if (matcher.match(item) && matcher.dataId == 0) {
                        generic = matcher;
                    }
                }
                if (event.isShiftClick()) {
                    if (generic != null) {
                        matchers.remove(generic);
                    }
                    matchers.add(new Matcher(item));
                } else if (generic == null) {
                    // Force the generic to go into the filter first
                    matchers.add(new Matcher(item, (short) 0));
                }
            } else {
                Matcher found = null;
                for (Matcher matcher : matchers) {
                    if (matcher.match(item)) {
                        found = matcher;
                    }
                }

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

    private boolean writeSign(Sign sign, Set<Matcher> matchers) {
        int index = 0;
        boolean needComma = false;
        String[] lines = {"", "", ""};

        for (Matcher matcher : new HashSet<>(matchers)) {
            String tmp = needComma ? "," : "";
            tmp += matcher.toString();

            if (lines[index].length() + tmp.length() >= 16) {
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
