package Perdume.rpg.core.player.listener;

import Perdume.rpg.Rpg;
import Perdume.rpg.system.TestDummyManager;
import Perdume.rpg.core.util.EquipmentStats;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class CombatListener implements Listener {

    private final Rpg plugin;
    private final NamespacedKey reinforceLevelKey;

    public CombatListener(Rpg plugin) {
        this.plugin = plugin;
        this.reinforceLevelKey = new NamespacedKey(plugin, "reinforce_level");
    }

    /**
     * [내부 클래스] 대미지 계산 결과와 치명타 여부를 함께 담습니다.
     */
    public record DamageResult(double getDamage, boolean isCritical) {}

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();
        Entity damager = event.getDamager();

        // --- 허수아비 타격 시 대미지 측정 ---
        if (TestDummyManager.isTestDummy(victim.getUniqueId()) && damager instanceof Player attacker) {
            event.setCancelled(true);
            DamageResult result = calculatePlayerAttackDamage(attacker, event.getOriginalDamage(EntityDamageByEntityEvent.DamageModifier.BASE));
            attacker.sendMessage(String.format("§e[대미지 측정] §f최종 대미지: §c%,.2f %s", result.getDamage(), result.isCritical() ? "§4(치명타!)" : ""));
            return;
        }

        // --- 레이드 내의 모든 전투 ---
        boolean isVictimInRaidSystem = plugin.getRaidManager().findRaidByEntityId(victim.getUniqueId()).isPresent();
        boolean isDamagerInRaidSystem = plugin.getRaidManager().findRaidByEntityId(damager.getUniqueId()).isPresent();

        if (isVictimInRaidSystem || isDamagerInRaidSystem) {
            event.setCancelled(true); // 레이드 내 전투는 모두 우리 시스템이 통제

            // 경우 A: 플레이어가 레이드 개체를 공격
            if (damager instanceof Player attacker && isVictimInRaidSystem) {
                plugin.getRaidManager().findRaidByEntityId(victim.getUniqueId()).ifPresent(raidInstance -> {
                    raidInstance.getBossByEntity(victim).ifPresent(bossObject -> {
                        double baseDamage = Math.max(1.0, event.getOriginalDamage(EntityDamageByEntityEvent.DamageModifier.BASE));
                        DamageResult result = calculatePlayerAttackDamage(attacker, baseDamage);
                        double finalDamage = result.getDamage();
                        double armorIgnore = getPlayerArmorIgnore(attacker);

                        // --- 타격감 효과 ---
                        Location particleLoc = victim.getLocation().add(0, victim.getHeight() / 2, 0);
                        victim.getWorld().playSound(particleLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 1.0f);
                        victim.getWorld().spawnParticle(Particle.CRIT, particleLoc, 30, 0.5, 0.5, 0.5, 0.1);

                        if (result.isCritical()) {
                            victim.getWorld().playSound(particleLoc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.5f, 1.0f);
                            victim.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, particleLoc, 10, 0.5, 0.5, 0.5, 0.2);
                        }

                        bossObject.damage(finalDamage, armorIgnore);
                    });
                });
            }
            // 경우 B: 레이드 개체가 플레이어를 공격
            else if (victim instanceof Player playerVictim && isDamagerInRaidSystem) {
                if (plugin.getRaidManager().isPlayerInRaid(playerVictim)) {
                    double reducedDamage = calculatePlayerDefense(playerVictim, event.getDamage(), event.getCause());
                    double finalDamage = reducedDamage * 5.0;
                    playerVictim.damage(finalDamage); // 최종 피해 적용

                    playerVictim.playSound(playerVictim.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
                }
            }
        }
    }

    /**
     * 플레이어의 모든 공격 관련 스탯과 효과를 종합하여 최종 대미지를 계산합니다.
     */
    public DamageResult calculatePlayerAttackDamage(Player player, double baseDamage) {
        double flatAtk = 0, percentAtk = 0, critChance = 0, critDmg = 0, armorIgnore = 0, mobDmg = 0;

        // --- [핵심 수정] NullPointerException을 방지하는 안전한 리스트 생성 ---
        List<ItemStack> equipment = new ArrayList<>();
        // 1. 갑옷을 하나씩 확인하고, null이 아닐 때만 리스트에 추가합니다.
        for (ItemStack armorPiece : player.getInventory().getArmorContents()) {
            if (armorPiece != null && armorPiece.getType() != Material.AIR) {
                equipment.add(armorPiece);
            }
        }
        // 2. 손에 든 아이템도 null이 아닐 때만 추가합니다.
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType() != Material.AIR) {
            equipment.add(itemInHand);
        }

        for (ItemStack piece : equipment) {
            int level = getReinforceLevel(piece);
            if (level == 0) continue;

            var type = EquipmentStats.getType(piece);
            var tier = EquipmentStats.getTier(piece);
            var bonuses = EquipmentStats.getBonuses(type, tier);
            if (bonuses == null) continue;

            flatAtk += bonuses.flatAtk() * level;
            percentAtk += bonuses.percentAtk() * level;
            critChance += bonuses.critChance() * level;
            critDmg += bonuses.critDmg() * level;
            armorIgnore += bonuses.armorIgnore() * level;
            mobDmg += bonuses.mobDmg() * level;
        }

        ItemStack weapon = player.getInventory().getItemInMainHand();
        double sharpnessBonus = 0;
        if (weapon.hasItemMeta() && weapon.getItemMeta().hasEnchant(Enchantment.SHARPNESS)) {
            sharpnessBonus = 0.5 * weapon.getItemMeta().getEnchantLevel(Enchantment.SHARPNESS) + 0.5;
        }

        double strengthMultiplier = 1.0;
        if (player.hasPotionEffect(PotionEffectType.STRENGTH)) {
            int level = player.getPotionEffect(PotionEffectType.STRENGTH).getAmplifier();
            strengthMultiplier = 1.0 + ((level + 1) * 1.3);
        }

        double finalDamage = ((baseDamage + sharpnessBonus) * strengthMultiplier + flatAtk) * (1 + percentAtk) * (1 + mobDmg);

        boolean isCritical = Math.random() < critChance;
        if (isCritical) {
            finalDamage *= (1.5 + critDmg);
        }

        return new DamageResult(finalDamage, isCritical);
    }

    /**
     * 플레이어의 모든 방어 관련 스탯을 종합하여 최종적으로 받는 대미지를 계산합니다.
     */
    private double calculatePlayerDefense(Player victim, double incomingDamage, DamageCause cause) {
        double totalDefensePower = 0, totalDmgReduction = 0, totalProjRes = 0;

        for (ItemStack armorPiece : victim.getInventory().getArmorContents()) {
            int level = getReinforceLevel(armorPiece);
            if (level == 0) continue;

            var type = EquipmentStats.getType(armorPiece);
            var tier = EquipmentStats.getTier(armorPiece);
            var bonuses = EquipmentStats.getBonuses(type, tier);
            if (bonuses == null) continue;

            totalDefensePower += bonuses.percentArmor() * level * 100;
            totalDmgReduction += bonuses.dmgReduce() * level;
            totalProjRes += bonuses.projRes() * level;
        }

        double defenseReduction = totalDefensePower / (totalDefensePower + 100);
        double finalMultiplier = 1.0;
        finalMultiplier *= (1.0 - defenseReduction);
        finalMultiplier *= (1.0 - totalDmgReduction);

        if (cause == DamageCause.PROJECTILE) {
            finalMultiplier *= (1.0 - totalProjRes);
        }

        return Math.max(0, incomingDamage * finalMultiplier);
    }

    /**
     * 플레이어의 모든 '방어 무시' 스탯을 합산하여 반환합니다.
     */
    public double getPlayerArmorIgnore(Player player) {
        double totalArmorIgnore = 0;
        List<ItemStack> equipment = new ArrayList<>(List.of(player.getInventory().getArmorContents()));
        equipment.add(player.getInventory().getItemInMainHand());

        for (ItemStack piece : equipment) {
            int level = getReinforceLevel(piece);
            if (level == 0) continue;
            var type = EquipmentStats.getType(piece);
            var tier = EquipmentStats.getTier(piece);
            var bonuses = EquipmentStats.getBonuses(type, tier);
            if (bonuses == null) continue;
            totalArmorIgnore += bonuses.armorIgnore() * level;
        }
        return totalArmorIgnore;
    }

    private int getReinforceLevel(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return 0;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(reinforceLevelKey, PersistentDataType.INTEGER, 0);
    }
}