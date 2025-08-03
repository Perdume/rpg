package Perdume.rpg.core.player.listener;


import Perdume.rpg.Rpg;
import Perdume.rpg.core.util.EquipmentStats;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class AttributeListener implements Listener {

    private final Rpg plugin;
    private final NamespacedKey reinforceLevelKey;

    // 각 속성마다 고유한 키를 부여하여 관리
    private final NamespacedKey healthModifierKey;
    private final NamespacedKey armorModifierKey;
    private final NamespacedKey attackSpeedModifierKey;
    private final NamespacedKey knockbackMidifierKey;

    public AttributeListener(Rpg plugin) {
        this.plugin = plugin;
        this.reinforceLevelKey = new NamespacedKey(plugin, "reinforce_level");
        this.healthModifierKey = new NamespacedKey(plugin, "raid_health");
        this.armorModifierKey = new NamespacedKey(plugin, "raid_armor");
        this.attackSpeedModifierKey = new NamespacedKey(plugin, "raid_attack_speed");
        this.knockbackMidifierKey= new NamespacedKey(plugin, "raid_knockback");
    }

    /**
     * 플레이어가 레이드에 입장할 때 호출되는 메소드.
     * @param player 대상 플레이어
     */
    public void applyRaidAttributes(Player player) {
        double totalHealthBonus = 0, totalArmorBonus = 0, totalKnockbackRes = 0, totalAtkSpeed = 0;

        // 모든 장비(무기 포함)를 순회하며 Attribute 관련 스탯 합산
        List<ItemStack> equipment = new ArrayList<>(List.of(player.getInventory().getArmorContents()));
        equipment.add(player.getInventory().getItemInMainHand());

        for (ItemStack piece : equipment) {
            int level = getReinforceLevel(piece);
            if (level == 0) continue;

            var type = EquipmentStats.getType(piece);
            var tier = EquipmentStats.getTier(piece);
            var bonuses = EquipmentStats.getBonuses(type, tier);
            if (bonuses == null) continue;

            totalHealthBonus += bonuses.percentHealth() * level;
            totalArmorBonus += bonuses.percentArmor() * level;
            totalKnockbackRes += bonuses.knockRes() * level;
            totalAtkSpeed += bonuses.atkSpeed() * level;
        }

        applyModifier(player, Attribute.MAX_HEALTH, healthModifierKey, totalHealthBonus, AttributeModifier.Operation.ADD_SCALAR);
        applyModifier(player, Attribute.ARMOR, armorModifierKey, totalArmorBonus, AttributeModifier.Operation.ADD_SCALAR);
        applyModifier(player, Attribute.KNOCKBACK_RESISTANCE, knockbackMidifierKey, totalKnockbackRes, AttributeModifier.Operation.ADD_NUMBER);
        applyModifier(player, Attribute.ATTACK_SPEED, attackSpeedModifierKey, totalAtkSpeed, AttributeModifier.Operation.ADD_SCALAR);
    }

    /**
     * 플레이어가 레이드에서 퇴장할 때 호출되는 메소드.
     */
    public void removeRaidAttributes(Player player) {
        removeAttribute(player, Attribute.MAX_HEALTH, healthModifierKey);
        removeAttribute(player, Attribute.ARMOR, armorModifierKey);
        removeAttribute(player, Attribute.ATTACK_SPEED, attackSpeedModifierKey);
    }

    // 플레이어가 서버에 접속하거나, 퇴장 시 혹시 모를 속성 찌꺼기를 제거하는 안전장치
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // 서버 재시작 등으로 인해 레이드 중에 강제 종료되었을 경우를 대비
        removeRaidAttributes(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 레이드 중에 접속 종료 시에도 속성 제거
        if (plugin.getRaidManager() != null && plugin.getRaidManager().isPlayerInRaid(event.getPlayer())) {
            removeRaidAttributes(event.getPlayer());
        }
    }

    // --- 유틸리티 메소드 ---

    private void applyModifier(Player p, Attribute attr, NamespacedKey key, double amount, AttributeModifier.Operation op) {
        AttributeInstance instance = p.getAttribute(attr);
        if (instance == null) return;

        // 기존 모디파이어가 있다면 제거 (중복 방지)
        removeAttribute(p, attr, key);

        if (amount > 0) {
            AttributeModifier modifier = new AttributeModifier(key, amount, op, EquipmentSlotGroup.ANY);
            instance.addModifier(modifier);
        }
    }

    private void removeAttribute(Player p, Attribute attr, NamespacedKey key) {
        AttributeInstance instance = p.getAttribute(attr);
        if (instance == null) return;
        instance.getModifiers().stream()
                .filter(m -> m.getKey().equals(key))
                .findFirst()
                .ifPresent(instance::removeModifier);
    }

    private int getReinforceLevel(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return 0;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(reinforceLevelKey, PersistentDataType.INTEGER, 0);
    }
}