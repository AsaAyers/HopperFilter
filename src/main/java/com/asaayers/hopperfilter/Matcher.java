package com.asaayers.hopperfilter;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Created by asa on 7/30/14.
 */
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

    public Material getMaterial() {
        Material material = null;
        if (id != null) {
            material = Material.getMaterial(id);
        }
        if (material == null && name != null) {
            material = Material.getMaterial(name);
        }

        return material;
    }
}

