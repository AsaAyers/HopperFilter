/**
 *
 */
package com.asaayers.hopperfilter;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Hopper;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

class Matcher {

    public final boolean negative;
    public final String name;
    public final Integer id;
    public final Integer dataId;

    Matcher(String str) {
        str = str.trim().toUpperCase();

        negative = str.startsWith("-");
        if (negative) {
            str = str.substring(1);
        }

        String[] parts = str.split(":");
        name = parts[0];

        Integer tmpId = null;
        try {
            tmpId = Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {

        }
        id = tmpId;

        if (parts.length == 2) {
            dataId = Integer.parseInt(parts[1]);
        } else {
            dataId = null;
        }
    }

    public Matcher(ItemStack inHand) {
        id = inHand.getTypeId();
        name = null;
        dataId = null;
        negative = false;
    }

    public String toString() {
        if (id != null) {
            return id.toString();
        }
        return name;
    }

    private Integer getId() {
        return id;
    }

    public boolean match(ItemStack item) {
        item.getType().getId();

        if (getId() != null && getId() == item.getTypeId()) {
            if (dataId == null || dataId == item.getData().getData()) {
                return true;
            }
        }

        Material mat = Material.getMaterial(name);
        return (mat != null && mat.getId() == item.getTypeId());
    }
}

/**
 * @author Asa Ayers
 */
public class HopperFilter extends JavaPlugin implements Listener {

    private static HopperFilter instance;

    public static HopperFilter getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
    }

    @EventHandler
    public void onPlayerInteractEvent(PlayerInteractEvent event) {
        Block clickedBlock = event.getClickedBlock();

        // Known issue: This doesn't work for items that can't be placed on walls,
        // like pressure plates, unless there is a floor under the sign.

        if (event.getPlayer().isSneaking()
                && event.getAction() == Action.RIGHT_CLICK_BLOCK
                && clickedBlock.getType() == Material.WALL_SIGN
                && clickedBlock.getState() instanceof Sign) {
            Sign sign = (Sign) clickedBlock.getState();

            List<Matcher> matchers = extractIds(sign);

            if (matchers != null) {
                ItemStack inHand = event.getPlayer().getItemInHand();

                Matcher found = null;
                for (Matcher matcher : matchers) {
                    if (matcher.match(inHand)) {
                        found = matcher;
                        break;
                    }
                }

                if (found != null) {
                    matchers.remove(found);
                } else {
                    matchers.add(new Matcher(inHand));
                }

                if (!writeSign(sign, matchers)) {
                    event.getPlayer().sendMessage(ChatColor.RED + "Unable to add " + inHand.getType().name() + " to filter.");
                }
                event.setCancelled(true);
            }
        }

    }


    private boolean writeSign(Sign sign, List<Matcher> matchers) {
        Integer index = 0;
        boolean needComma = false;
        String[] lines = {"", "", ""};

        for (Matcher matcher : matchers) {
            String tmp;
            if (needComma) {
                tmp = "," + matcher.toString();
            } else {
                tmp = matcher.toString();
            }

            if (lines[index].concat(tmp).length() > 16) {
                index++;
                tmp = matcher.toString();
            } else {
                needComma = true;
            }
            if (index > 2) {
                getLogger().info("Index to high?");
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

    @EventHandler
    public void onInventoryMoveItemEvent(InventoryMoveItemEvent event) {

        if (event.getDestination().getHolder() instanceof Hopper) {
            Hopper hopper = (Hopper) event.getDestination().getHolder();

            List<Matcher> matchers = findSign(hopper.getLocation().getBlock());

            if (matchers != null) {
                ItemStack item = event.getItem();

                for (Matcher matcher : matchers) {
                    if (matcher.match(item)) {
                        return;
                    }
                }
                event.setCancelled(true);
            }
        }
    }

    private static EnumSet<BlockFace> signDirections = EnumSet.of(
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.EAST,
            BlockFace.WEST
    );

    private List<Matcher> extractIds(Sign sign) {
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

    private List<Matcher> findSign(Block block) {

        for (BlockFace direction : signDirections) {
            Block sign = block.getRelative(direction);

            // Find any nearby wall sign
            if (sign.getType() == Material.WALL_SIGN) {

                // So many hoops just to figure out which direction it's facing.
                MaterialData md = sign.getType().getNewData(sign.getData());
                if (md instanceof org.bukkit.material.Sign && sign.getState() instanceof Sign) {
                    org.bukkit.material.Sign s = (org.bukkit.material.Sign) md;

                    // If the WALL_SIGN is NORTH of the hopper and facing NORTH it must
                    // be attached to the hopper.
                    if (direction == s.getFacing()) {

                        // extractIds will make sure it starts with [Filter]
                        List<Matcher> ids = extractIds((Sign) sign.getState());
                        if (ids != null) {
                            return ids;
                        }
                    }
                }
            }
        }
        return null;
    }

}
