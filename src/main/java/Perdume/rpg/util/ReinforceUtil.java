package Perdume.rpg.util;

import Perdume.rpg.Rpg;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ReinforceUtil {

    private static final NamespacedKey REINFORCE_LEVEL_KEY = new NamespacedKey(Rpg.getInstance(), "reinforce_level");

    /**
     * Gets the reinforcement level of an item.
     * @param item The item to check.
     * @return The reinforcement level, or 0 if not reinforced.
     */
    public static int getReinforceLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(REINFORCE_LEVEL_KEY, PersistentDataType.INTEGER, 0);
    }

    /**
     * Sets the reinforcement level and updates the lore of an item.
     * @param item The item to modify.
     * @param level The new reinforcement level.
     */
    public static void setReinforceLevel(ItemStack item, int level) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // Save the level to the item's data
        meta.getPersistentDataContainer().set(REINFORCE_LEVEL_KEY, PersistentDataType.INTEGER, level);

        List<String> newLore = new ArrayList<>();

        // Add the new reinforcement level line
        if (level > 0) {
            newLore.add("§7강화 레벨: §e+" + level);
            newLore.add(" "); // 구분선
        }

        // --- [핵심] 등급과 부위에 맞는 모든 강화 효과를 Lore에 추가 ---
        var type = EquipmentStats.getType(item);
        var tier = EquipmentStats.getTier(item);
        var bonuses = EquipmentStats.getBonuses(type, tier);

        if (level > 0 && bonuses != null) {
            if (bonuses.flatAtk() > 0) newLore.add(String.format("§7공격력: §c+%.2f", level * bonuses.flatAtk()));
            if (bonuses.percentAtk() > 0) newLore.add(String.format("§7공격력: §c+%.1f%%", level * bonuses.percentAtk() * 100));
            if (bonuses.critChance() > 0) newLore.add(String.format("§7치명타 확률: §c+%.2f%%", level * bonuses.critChance() * 100));
            if (bonuses.critDmg() > 0) newLore.add(String.format("§7치명타 피해: §c+%.1f%%", level * bonuses.critDmg() * 100));
            if (bonuses.armorIgnore() > 0) newLore.add(String.format("§7방어 무시: §c+%.2f%%", level * bonuses.armorIgnore() * 100));
            if (bonuses.percentArmor() > 0) newLore.add(String.format("§7방어력: §a+%.1f%%", level * bonuses.percentArmor() * 100));
            if (bonuses.percentHealth() > 0) newLore.add(String.format("§7최대 체력: §a+%.1f%%", level * bonuses.percentHealth() * 100));
            if (bonuses.projRes() > 0) newLore.add(String.format("§7투사체 저항: §a+%.2f%%", level * bonuses.projRes() * 100));
            if (bonuses.dmgReduce() > 0) newLore.add(String.format("§7대미지 감소: §a+%.2f%%", level * bonuses.dmgReduce() * 100));
            if (bonuses.mobDmg() > 0) newLore.add(String.format("§7몬스터 추가 대미지: §a+%.2f%%", level * bonuses.mobDmg() * 100));
            if (bonuses.knockRes() > 0) newLore.add(String.format("§7넉백 저항: §a+%.2f%%", level * bonuses.knockRes() * 100));
            if (bonuses.atkSpeed() > 0) newLore.add(String.format("§7공격 속도: §e+%.2f%%", level * bonuses.atkSpeed() * 100));
        }
        // 3. 기존 Lore가 있다면, 강화 관련 Lore를 제외하고 나머지를 가져와 붙임
        if (meta.hasLore()) {
            for (String line : meta.getLore()) {
                if (!line.contains("강화 레벨:") && !line.matches("§7[가-힣 ]+:.*")) {
                    newLore.add(line);
                }
            }
        }

        meta.setLore(newLore);
        item.setItemMeta(meta);
    }

    /**
     * Checks if an item is of a type that can be reinforced.
     * @param item The item to check.
     * @return True if the item is reinforceable, false otherwise.
     */
    public static boolean isReinforceable(ItemStack item) {
        if (item == null) return false;
        String materialName = item.getType().name();
        return materialName.contains("_SWORD") ||
               materialName.contains("_AXE") ||
               materialName.contains("_HELMET") ||
               materialName.contains("_CHESTPLATE") ||
               materialName.contains("_LEGGINGS") ||
               materialName.contains("_BOOTS");
    }
}