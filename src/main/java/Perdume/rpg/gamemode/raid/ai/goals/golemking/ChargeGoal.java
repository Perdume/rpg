package Perdume.rpg.gamemode.raid.ai.goals.golemking;


import Perdume.rpg.gamemode.raid.ai.EntityGolemKing;
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
    private final EntityGolemKing golem;
    private int cooldownTicks = 0;
    private final Set<UUID> hitPlayers = new HashSet<>();
    private int actionTicks = 0;

    public ChargeGoal(EntityGolemKing golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (golem.isInGracePeriod()) return false;
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return false;
        }
        LivingEntity target = this.golem.getTarget();
        return target != null && golem.distanceToSqr(target) > 36 && golem.getRandom().nextFloat() < 0.3f; // 6칸 이상
    }

    @Override
    public void start() {
        this.actionTicks = 30; // 1.5초간 공중 체공 및 타격 판정
        this.hitPlayers.clear();
        this.golem.getNavigation().stop(); // 내장 AI의 움직임은 완전히 정지

        LivingEntity target = this.golem.getTarget();
        if (target != null) {
            // [핵심] 1. 타겟을 향하는 방향 벡터를 계산합니다.
            Vector direction = target.getBukkitEntity().getLocation().toVector()
                    .subtract(golem.getBukkitEntity().getLocation().toVector())
                    .normalize();

            // [핵심] 2. Y축(높이) 방향으로 힘을 주어 '도약'하는 느낌을 만듭니다.
            direction.setY(0.6).multiply(1.8); // 위로 0.6, 앞으로 1.8의 힘으로 몸을 날림

            // [핵심] 3. 계산된 힘을 골렘의 속도(Velocity)에 직접 적용합니다.
            golem.getBukkitEntity().setVelocity(direction);

            golem.getBukkitEntity().getWorld().playSound(golem.getBukkitEntity().getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 2, 1.2f);
        }
    }

    @Override
    public boolean canContinueToUse() {
        // 지정된 시간 동안 또는 땅에 닿을 때까지 행동을 계속합니다.
        return actionTicks > 0 && !golem.onGround();
    }

    @Override
    public void tick() {
        actionTicks--;

        Location currentLocation = golem.getBukkitEntity().getLocation();
        AABB hitBox = golem.getBoundingBox().inflate(1.0); // 히트박스 확장

        golem.level().getEntitiesOfClass(net.minecraft.world.entity.player.Player.class, hitBox).forEach(nmsPlayer -> {
            if (!hitPlayers.contains(nmsPlayer.getUUID())) {
                nmsPlayer.getBukkitEntity().damage(100, golem.getBukkitEntity());
                hitPlayers.add(nmsPlayer.getUUID());
            }
        });

        golem.getBukkitEntity().getWorld().spawnParticle(Particle.CRIT, currentLocation.add(0, 1.5, 0), 10, 0.5, 0.5, 0.5, 0.1);
    }

    @Override
    public void stop() {
        this.cooldownTicks = 200; // 10초 쿨타임
        this.actionTicks = 0;
    }
}