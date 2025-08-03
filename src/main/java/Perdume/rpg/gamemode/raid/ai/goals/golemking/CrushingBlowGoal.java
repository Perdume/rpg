package Perdume.rpg.gamemode.raid.ai.goals.golemking;


import Perdume.rpg.gamemode.raid.ai.EntityGolemKing;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;

import java.util.Comparator;
import java.util.EnumSet;

public class CrushingBlowGoal extends Goal {
    private final EntityGolemKing golem;
    private LivingEntity target;
    private int actionTicks = 0;

    public CrushingBlowGoal(EntityGolemKing golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (golem.isInGracePeriod() || golem.skillCooldowns.get("CRUSHING_BLOW") > 0) return false;
        this.target = findFarthestPlayer();
        return this.target != null;
    }

    @Override
    public void start() {
        this.actionTicks = 40; // 2초 예고
        golem.getLookControl().setLookAt(target, 30.0F, 30.0F);
        golem.getBukkitEntity().getWorld().playSound(golem.getBukkitEntity().getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 2, 1);
    }

    @Override
    public void tick() {
        actionTicks--;

        // [핵심] 1. 2초간 타겟의 위치에 폭발 범위를 예고합니다.
        if (actionTicks > 0) {
            Location targetLoc = target.getBukkitEntity().getLocation();
            targetLoc.getWorld().spawnParticle(Particle.EXPLOSION, targetLoc, 20, 2.5, 0.5, 2.5, 0.1);
        }
        // [핵심] 2. 예고가 끝나면, '가짜' 투사체 대신 '진짜' 피해를 직접 줍니다.
        else {
            Location targetLoc = target.getBukkitEntity().getLocation();
            
            // 시각/청각 효과
            targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 2, 0.8f);
            targetLoc.getWorld().spawnParticle(Particle.EXPLOSION, targetLoc, 1);

            // 폭발 반경(5블록) 내의 모든 플레이어를 찾아 피해를 입힙니다.
            targetLoc.getNearbyPlayers(5).forEach(p -> {
                // 중앙에 가까울수록 더 큰 피해를 주는 로직
                double distance = p.getLocation().distance(targetLoc);
                double damage = Math.max(0, 60.0 - (distance * 10)); // 중앙은 60, 멀어질수록 감소
                p.damage(damage, golem.getBukkitEntity());
            });
            this.stop();
        }
    }

    @Override
    public boolean canContinueToUse() {
        return actionTicks > 0 && target != null && target.isAlive();
    }

    @Override
    public void stop() {
        golem.skillCooldowns.put("CRUSHING_BLOW", 400); // 20초 쿨타임
        this.actionTicks = 0;
        this.target = null;
    }

    private LivingEntity findFarthestPlayer() {
        return golem.level().getEntitiesOfClass(Player.class, golem.getBoundingBox().inflate(40.0))
                .stream()
                .filter(p -> p.distanceToSqr(golem) > 225) // 15칸 이상
                .max(Comparator.comparingDouble(p -> p.distanceToSqr(golem)))
                .orElse(null);
    }
}