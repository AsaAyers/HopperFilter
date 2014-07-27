/**
 *
 */
package com.asaayers.hopperfilter;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Hopper;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

class Matcher {

    public final boolean negative;
    public final String name;
    public final Integer dataId;

    Matcher(String str) {
        str = str.trim().toUpperCase();
        negative = str.startsWith("-");
        if (negative) {
            str = str.substring(1);
        }

        String[] parts = str.split(":");
        name = parts[0];

        if (parts.length == 2) {
            dataId = Integer.parseInt(parts[1]);
        } else {
            dataId = null;
        }
    }

    private Integer getId() {
        try {
            return Integer.parseInt(name);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    Matcher(boolean negative, String name, int dataId) {
        this.negative = negative;
        this.name = name;
        this.dataId = dataId;
    }

    public boolean match(ItemStack item) {
        item.getType().getId();

        if (getId() !=null && getId() == item.getTypeId()) {
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
    public void onInventoryMoveItemEvent(InventoryMoveItemEvent event) {

        if (event.getDestination().getHolder() instanceof Hopper) {
            Hopper hopper = (Hopper) event.getDestination().getHolder();

            List<Matcher> matchers = findSign(hopper.getLocation().getBlock());

            if (matchers != null) {
                ItemStack item = event.getItem();

                for (Matcher matcher: matchers) {
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
            for (String str : sign.getLine(1).split(",") ) {
                matchers.add(new Matcher(str));
            }
            for (String str : sign.getLine(2).split(",") ) {
                matchers.add(new Matcher(str));
            }
            for (String str : sign.getLine(3).split(",") ) {
                matchers.add(new Matcher(str));
            }
            return matchers;
        }

        return null;
    }

    private List<Matcher> findSign(Block block) {

        for (BlockFace direction: signDirections) {
            Block sign = block.getRelative(direction);

            // Find any nearby wall sign
            if (sign.getType() == Material.WALL_SIGN) {

                // So many hoops just to figure out which direction it's facing.
                MaterialData md = sign.getType().getNewData(sign.getData());
                if (md instanceof org.bukkit.material.Sign && sign.getState() instanceof Sign) {
                    org.bukkit.material.Sign s = (org.bukkit.material.Sign)md;

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