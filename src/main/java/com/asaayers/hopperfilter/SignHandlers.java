package com.asaayers.hopperfilter;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
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

    public static Set<Matcher> extractIds(String[] lines) {
        if (lines[0].trim().equalsIgnoreCase("[Filter]")) {

            Set<Matcher> matchers = new HashSet<>();

            for (int i = 1; i <= 3; i++) {
                for (String str : lines[i].split(",")) {
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

    public static Set<Matcher> extractIds(Sign sign) {
        return extractIds(sign.getLines());
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
    public void onSignChangeEvent(SignChangeEvent event) {
        if (event.getLine(0).trim().equalsIgnoreCase("[filter]")) {

            BlockFace direction = plugin.getSignBack(event.getBlock());

            if (direction == null || event.getBlock().getRelative(direction).getType() != Material.HOPPER) {
                event.getPlayer().sendMessage(ChatColor.RED + "[Filter] signs must be placed against hoppers.");
                event.setCancelled(true);
                return;
            }

            event.setLine(0, "[Filter]");
            clearNearCache(event.getBlock());
            Inventory inventory = getInventoryForSign(event.getBlock(), event.getLines());
            event.getPlayer().openInventory(inventory);
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
        clearNearCache(event.getBlock());
    }

    private void clearNearCache(Block block) {
        String selfMeta = null;
        String neighborMeta = null;

        if (block.getType() == Material.WALL_SIGN) {
            selfMeta = FILTER_INVENTORY;
            neighborMeta = HopperFilter.MATCHERS;
        } else if (block.getType() == Material.HOPPER) {
            selfMeta = HopperFilter.MATCHERS;
            neighborMeta = FILTER_INVENTORY;
        }

        if (selfMeta != null && neighborMeta != null) {
            if (block.hasMetadata(selfMeta)) {
                block.removeMetadata(selfMeta, plugin);
            }

            for (BlockFace direction: HopperFilter.signDirections) {
                Block neighbor = block.getRelative(direction);
                if (neighbor.hasMetadata(neighborMeta)) {
                    neighbor.removeMetadata(neighborMeta, plugin);
                }
            }
        }
    }

    private Inventory getInventoryForSign(Block clickedBlock) {
        Sign sign = (Sign) clickedBlock.getState();

        return getInventoryForSign(clickedBlock, sign.getLines());
    }

    private Inventory getInventoryForSign(Block clickedBlock, String[] lines) {
        Inventory inventory;

        Object foo = plugin.getMeta(clickedBlock, FILTER_INVENTORY);

        if (foo instanceof Inventory) {
            inventory = (Inventory) foo;
        } else {
            inventory = plugin.getServer().createInventory(null, FILTER_SIZE, "[Filter]");
            populate(inventory, extractIds(lines));
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
                Set<Matcher> specifics = new HashSet<>();

                for (Matcher matcher : matchers) {
                    // If the filter already contains the generic form of this
                    // item, remove it and replace it with an exact match.
                    if (matcher.match(item) && matcher.dataId == 0) {
                        generic = matcher;
                    }

                    if (matcher.looseMatch(item)) {
                        specifics.add(matcher);
                    }
                }

                if (!event.isShiftClick()) {
                    // regular click adds that specific item. For example
                    // it adds that exact color of wool.
                    if (generic != null) {
                        matchers.remove(generic);
                    }
                    matchers.add(new Matcher(item));

                } else if (generic == null) {
                    // shift+click to match all wool
                    matchers.add(new Matcher(item, (short) 0));
                    matchers.removeAll(specifics);
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
        clearNearCache(sign.getBlock());
        return true;
    }


}
