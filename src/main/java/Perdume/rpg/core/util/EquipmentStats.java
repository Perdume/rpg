package Perdume.rpg.core.util;

import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class EquipmentStats {

    public enum Type { SWORD, AXE, BOW, HELMET, CHESTPLATE, LEGGINGS, BOOTS, OTHER }
    public enum Tier { WOOD_STONE, IRON, DIAMOND, NETHERITE, OTHER }

    public record StatBonuses(double flatAtk, double percentAtk, double critChance, double critDmg,
                              double armorIgnore, double projRes, double dmgReduce,
                              double mobDmg, double knockRes, double atkSpeed,
                              double percentArmor, double percentHealth) {}

    private static final Map<Type, Map<Tier, StatBonuses>> STAT_MAP;

    static {
        STAT_MAP = Map.ofEntries(
                Map.entry(Type.SWORD, Map.of(
                        Tier.WOOD_STONE, new StatBonuses(0.8, 0.08, 0.01, 0.02, 0,0,0,0,0,0,0,0),
                        Tier.IRON,       new StatBonuses(1.0, 0.10, 0.012, 0.025, 0,0,0,0,0,0,0,0),
                        Tier.DIAMOND,    new StatBonuses(1.2, 0.12, 0.015, 0.03, 0,0,0,0,0,0,0,0),
                        Tier.NETHERITE,  new StatBonuses(1.5, 0.15, 0.02, 0.04, 0,0,0,0,0,0,0,0)
                )),
                Map.entry(Type.AXE, Map.of(
                        Tier.WOOD_STONE, new StatBonuses(1.0, 0.09, 0, 0, 0.015,0,0,0,0,0,0,0),
                        Tier.IRON,       new StatBonuses(1.2, 0.11, 0, 0, 0.02,0,0,0,0,0,0,0),
                        Tier.DIAMOND,    new StatBonuses(1.5, 0.13, 0, 0, 0.025,0,0,0,0,0,0,0),
                        Tier.NETHERITE,  new StatBonuses(1.8, 0.15, 0, 0, 0.03,0,0,0,0,0,0,0)
                )),
                Map.entry(Type.BOW, Map.of(
                        Tier.WOOD_STONE, new StatBonuses(0.6, 0.07, 0.012, 0, 0,0,0,0,0,0.02,0,0)
                )),
                Map.entry(Type.HELMET, Map.of(
                        Tier.WOOD_STONE, new StatBonuses(0, 0, 0, 0, 0.005, 0.01, 0, 0, 0, 0, 0.03, 0.015),
                        Tier.IRON,       new StatBonuses(0, 0, 0, 0, 0.008, 0.012, 0, 0, 0, 0, 0.04, 0.02),
                        Tier.DIAMOND,    new StatBonuses(0, 0, 0, 0, 0.01, 0.015, 0, 0, 0, 0, 0.05, 0.025),
                        Tier.NETHERITE,  new StatBonuses(0, 0, 0, 0, 0.015, 0.02, 0, 0, 0, 0, 0.06, 0.03)
                )),
                Map.entry(Type.CHESTPLATE, Map.of(
                        Tier.WOOD_STONE, new StatBonuses(0, 0.015, 0, 0, 0.008, 0, 0, 0, 0, 0, 0.04, 0.02),
                        Tier.IRON,       new StatBonuses(0, 0.02, 0, 0, 0.01, 0, 0, 0, 0, 0, 0.05, 0.025),
                        Tier.DIAMOND,    new StatBonuses(0, 0.025, 0, 0, 0.012, 0, 0, 0, 0, 0, 0.06, 0.03),
                        Tier.NETHERITE,  new StatBonuses(0, 0.03, 0, 0, 0.015, 0, 0, 0, 0, 0, 0.07, 0.04)
                )),
                Map.entry(Type.LEGGINGS, Map.of(
                        Tier.WOOD_STONE, new StatBonuses(0, 0, 0, 0, 0.005, 0, 0.008, 0.01, 0, 0, 0.035, 0),
                        Tier.IRON,       new StatBonuses(0, 0, 0, 0, 0.008, 0, 0.01, 0.015, 0, 0, 0.045, 0),
                        Tier.DIAMOND,    new StatBonuses(0, 0, 0, 0, 0.01, 0, 0.012, 0.02, 0, 0, 0.055, 0),
                        Tier.NETHERITE,  new StatBonuses(0, 0, 0, 0, 0.012, 0, 0.015, 0.025, 0, 0, 0.065, 0)
                )),
                Map.entry(Type.BOOTS, Map.of(
                        Tier.WOOD_STONE, new StatBonuses(0, 0, 0, 0, 0.003, 0, 0, 0, 0.015, 0.01, 0.03, 0),
                        Tier.IRON,       new StatBonuses(0, 0, 0, 0, 0.005, 0, 0, 0, 0.02, 0.015, 0.04, 0),
                        Tier.DIAMOND,    new StatBonuses(0, 0, 0, 0, 0.008, 0, 0, 0, 0.025, 0.02, 0.05, 0),
                        Tier.NETHERITE,  new StatBonuses(0, 0, 0, 0, 0.01, 0, 0, 0, 0.03, 0.025, 0.06, 0)
                ))
        );
    }

    public static StatBonuses getBonuses(Type type, Tier tier) {
        return STAT_MAP.getOrDefault(type, Map.of()).get(tier);
    }

    public static Type getType(ItemStack item) {
        if (item == null) return Type.OTHER;
        String name = item.getType().name();
        if (name.contains("SWORD")) return Type.SWORD;
        if (name.contains("AXE")) return Type.AXE;
        if (name.equals("BOW") || name.equals("CROSSBOW")) return Type.BOW;
        if (name.contains("HELMET")) return Type.HELMET;
        if (name.contains("CHESTPLATE")) return Type.CHESTPLATE;
        if (name.contains("LEGGINGS")) return Type.LEGGINGS;
        if (name.contains("BOOTS")) return Type.BOOTS;
        return Type.OTHER;
    }

    public static Tier getTier(ItemStack item) {
        if (item == null) return Tier.OTHER;
        String name = item.getType().name();
        if (name.startsWith("LEATHER_") || name.startsWith("WOODEN_") || name.startsWith("STONE_") || name.equals("BOW") || name.equals("CROSSBOW")) return Tier.WOOD_STONE;
        if (name.startsWith("IRON_")) return Tier.IRON;
        if (name.startsWith("DIAMOND_")) return Tier.DIAMOND;
        if (name.startsWith("NETHERITE_")) return Tier.NETHERITE;
        return Tier.OTHER;
    }
}