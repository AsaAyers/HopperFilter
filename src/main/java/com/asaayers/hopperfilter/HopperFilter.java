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
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.Metadatable;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;


/**
 * @author Asa Ayers
 */
public class HopperFilter extends JavaPlugin implements Listener {

    public static final String MATCHERS = "HopperFilterMatchers";
    public static final EnumSet<BlockFace> signDirections = EnumSet.of(
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

    protected Object getMeta(Metadatable obj, String key) {
        List<MetadataValue> meta = obj.getMetadata(key);

        for (MetadataValue item : meta) {
            if (item.getOwningPlugin() == this) {
                return item.value();
            }
        }
        return null;
    }

    public Set<Matcher> findSign(Block block) {
        Object metaMatcher = getMeta(block, MATCHERS);

        if (metaMatcher instanceof MetaMatcher) {
            return ((MetaMatcher) metaMatcher).getMatchers();
        }

        Set<Matcher> matchers = noCacheFindSign(block);

        block.setMetadata(MATCHERS, new FixedMetadataValue(this, new MetaMatcher(matchers)));

        return matchers;
    }

    public Set<Matcher> noCacheFindSign(Block block) {

        for (BlockFace direction : signDirections) {
            Block sign = block.getRelative(direction);

            // If the WALL_SIGN is NORTH of the hopper and facing NORTH it must
            // be attached to the hopper.
            if (direction == getSignDirection(sign)) {

                // extractIds will make sure it starts with [Filter]
                Set<Matcher> ids = SignHandlers.extractIds((Sign) sign.getState());
                if (ids != null) {
                    return ids;
                }
            }
        }
        return null;
    }

    public BlockFace getSignBack(Block sign) {
        BlockFace front = getSignDirection(sign);

        if (front != null) {
            return front.getOppositeFace();
        }
        return null;
    }

    public BlockFace getSignDirection(Block sign) {
        // Find any nearby wall sign
        if (sign.getType() == Material.WALL_SIGN) {

            // So many hoops just to figure out which direction it's facing.
            MaterialData md = sign.getType().getNewData(sign.getData());
            if (md instanceof org.bukkit.material.Sign && sign.getState() instanceof Sign) {
                org.bukkit.material.Sign s = (org.bukkit.material.Sign) md;

                return s.getFacing();
            }
        }
        return null;
    }

}
