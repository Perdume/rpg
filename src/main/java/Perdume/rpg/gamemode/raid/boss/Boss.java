package Perdume.rpg.gamemode.raid.boss;

import Perdume.rpg.gamemode.raid.RaidInstance;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.Optional;

/**
 * 모든 보스 클래스가 반드시 구현해야 하는 규칙을 정의하는 인터페이스입니다.
 */
public interface Boss {

    // --- 생명 주기 (Lifecycle) ---
    void spawn(RaidInstance raidInstance, Location location);
    void onTick();
    void onDeath();
    void cleanup();

    // --- 상태 조회 (Getters) ---
    Optional<LivingEntity> getBukkitEntity();
    String getBaseName();
    double getCurrentHealth();
    double getMaxHealth();
    double getHealthPercentage();
    boolean isDead();
    String getBossId();

    // --- 행동 (Actions) ---
    void damage(double amount, double armorIgnore);

    void registerSelf(RaidInstance raidInstance);
    int getCurrentPhase();
}