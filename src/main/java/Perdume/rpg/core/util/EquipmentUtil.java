package Perdume.rpg.core.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class EquipmentUtil {

    // 아이템 등급을 나타내는 열거형(enum)
    public enum Tier {
        LEATHER_WOOD_STONE,
        IRON,
        DIAMOND,
        NETHERITE,
        OTHER
    }

    /**
     * 아이템의 등급을 판별하여 반환합니다.
     * @param item 판별할 아이템
     * @return 아이템의 등급 (Tier)
     */
    public static Tier getTier(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return Tier.OTHER;
        }
        String name = item.getType().name();

        if (name.startsWith("LEATHER_") || name.startsWith("WOODEN_") || name.startsWith("STONE_")) {
            return Tier.LEATHER_WOOD_STONE;
        } else if (name.startsWith("IRON_")) {
            return Tier.IRON;
        } else if (name.startsWith("DIAMOND_")) {
            return Tier.DIAMOND;
        } else if (name.startsWith("NETHERITE_")) {
            return Tier.NETHERITE;
        } else {
            return Tier.OTHER;
        }
    }
}