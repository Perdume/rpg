package Perdume.rpg.gamemode.raid.ai.goals.shieldgolem;


import Perdume.rpg.gamemode.raid.ai.EntityShieldGolem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.util.Vector;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ChargeGoal extends Goal {
    private final EntityShieldGolem golem;
    private int cooldownTicks = 0;
    private LivingEntity target; // 돌진 목표
    private int chargeTimeoutTicks = 0; // 무한 돌진 방지용 타임아웃
    private final Set<UUID> hitPlayers = new HashSet<>();

    public ChargeGoal(EntityShieldGolem golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return false;
        }
        this.target = this.golem.getTarget();
        // 타겟이 있고, 5칸 이상 떨어져 있을 때 30% 확률로 사용 시도
        return this.target != null && golem.distanceToSqr(target) > 25 && golem.getRandom().nextFloat() < 0.3f;
    }

    @Override
    public void start() {
        this.chargeTimeoutTicks = 100; // 최대 5초간 돌진 (벽에 막히는 경우 대비)
        this.hitPlayers.clear();
        
        // [핵심] 목표의 현재 위치를 기억하고, NMS의 내장 경로탐색 AI를 사용하여 이동을 '시작'합니다.
        // 속도는 1.5배로 설정합니다.
        this.golem.getNavigation().moveTo(this.target, 1.5D);

        golem.getBukkitEntity().getWorld().playSound(golem.getBukkitEntity().getLocation(), Sound.ENTITY_VINDICATOR_HURT, 2, 0.5f);
    }

    /**
     * [핵심] 이 행동을 '계속'할 조건이 맞는가?
     */
    @Override
    public boolean canContinueToUse() {
        // 경로 탐색이 아직 진행 중이고, 타임아웃이 지나지 않았을 때만 계속합니다.
        return !this.golem.getNavigation().isDone() && this.chargeTimeoutTicks > 0;
    }

    @Override
    public void tick() {
        chargeTimeoutTicks--;
        
        // 보스의 현재 위치
        Location currentLocation = golem.getBukkitEntity().getLocation();
        
        // AABB 히트박스 생성 (보스의 몸 전체를 감싸도록)
        AABB hitBox = golem.getBoundingBox().inflate(0.5); // 0.5칸 확장

        // 히트박스 내의 플레이어를 찾아 대미지 적용
        golem.level().getEntitiesOfClass(net.minecraft.world.entity.player.Player.class, hitBox).forEach(nmsPlayer -> {
            if (!hitPlayers.contains(nmsPlayer.getUUID())) {
                nmsPlayer.getBukkitEntity().damage(80, golem.getBukkitEntity());
                // 맞은 플레이어에게 넉백 효과
                Vector knockback = nmsPlayer.getBukkitEntity().getLocation().toVector()
                                    .subtract(currentLocation.toVector()).normalize().multiply(1.2).setY(0.4);
                nmsPlayer.getBukkitEntity().setVelocity(knockback);
                hitPlayers.add(nmsPlayer.getUUID());
            }
        });

        // 돌진 경로에 파티클 효과
        golem.getBukkitEntity().getWorld().spawnParticle(Particle.CRIT, currentLocation.add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0.1);
    }

    @Override
    public void stop() {
        this.cooldownTicks = 200; // 10초 쿨타임
        this.chargeTimeoutTicks = 0;
        this.target = null;
        // 경로 탐색 중지
        this.golem.getNavigation().stop();
    }
}