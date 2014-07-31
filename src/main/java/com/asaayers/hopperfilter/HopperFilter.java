/**
 *
 */
package com.asaayers.hopperfilter;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.event.Listener;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.List;


/**
 * @author Asa Ayers
 */
public class HopperFilter extends JavaPlugin implements Listener {

    private static EnumSet<BlockFace> signDirections = EnumSet.of(
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.EAST,
            BlockFace.WEST
    );

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new ItemHandlers(this), this);
        getServer().getPluginManager().registerEvents(new SignHandlers(this), this);
    }

    @Override
    public void onDisable() {
    }

    public List<Matcher> findSign(Block block) {

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
                        List<Matcher> ids = SignHandlers.extractIds((Sign) sign.getState());
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
