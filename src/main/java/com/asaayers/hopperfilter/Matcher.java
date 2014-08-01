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
    public final short dataId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return toString().equals(o.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

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
            dataId = Short.parseShort(parts[1]);
        } else {
            dataId = 0;
        }
    }

    public Matcher(ItemStack item, short dataId) {
        id = item.getTypeId();
        this.dataId = dataId;
        name = null;
        negative = false;
    }

    public Matcher(ItemStack item) {
        this(item, item.getDurability());
    }

    public String toString() {
        if (id != null) {
            if (dataId > 0) {
                return id.toString() + ":" + dataId;
            }
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
            if (dataId == 0 || dataId == item.getDurability()) {
                return true;
            }
        }

        Material mat = Material.getMaterial(name);
        return (mat != null && mat.getId() == item.getTypeId());
    }

    public ItemStack getItemStack() {
        return new ItemStack(getMaterial(), 1, dataId);
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

