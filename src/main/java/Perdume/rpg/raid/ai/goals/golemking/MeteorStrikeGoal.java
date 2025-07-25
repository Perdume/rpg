package Perdume.rpg.raid.ai.goals.golemking;


import Perdume.rpg.raid.ai.EntityGolemKing;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.LargeFireball;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class MeteorStrikeGoal extends Goal {
    private final EntityGolemKing golem;
    private int actionTicks = 0;
    private final List<Location> targetLocations = new ArrayList<>();

    public MeteorStrikeGoal(EntityGolemKing golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE)); // 시전 중 움직임만 방해
    }

    @Override
    public boolean canUse() {
        if (golem.isInGracePeriod() || golem.skillCooldowns.get("METEOR") > 0) return false;
        // 플레이어가 3명 이상일 때 발동
        return golem.level().getEntitiesOfClass(net.minecraft.world.entity.player.Player.class, golem.getBoundingBox().inflate(40.0)).size() >= 3;
    }

    @Override
    public void start() {
        this.actionTicks = 30; // 1.5초 예고
        this.targetLocations.clear();
        // 모든 플레이어의 현재 위치를 타겟으로 저장
        golem.level().getEntitiesOfClass(net.minecraft.world.entity.player.Player.class, golem.getBoundingBox().inflate(40.0))
                .forEach(p -> targetLocations.add(p.getBukkitEntity().getLocation()));
    }

    @Override
    public void tick() {
        actionTicks--;

        if (actionTicks > 0) { // 1.5초간 예고
            for (Location loc : targetLocations) {
                // 바닥에 붉은색 원으로 낙하 지점을 예고
                loc.getWorld().spawnParticle(Particle.DUST, loc, 20, 1, 0.1, 1, new Particle.DustOptions(org.bukkit.Color.RED, 1.5f));
            }
        } else {
            // [핵심] 더 이상 LargeFireball을 소환하지 않습니다.
            for (Location loc : targetLocations) {
                // 1. 시각/청각 효과를 직접 만듭니다.
                loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1, 1.2f);
                loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1);

                // 2. 해당 위치 주변의 플레이어에게 '우리가 직접' 대미지를 줍니다.
                loc.getNearbyPlayers(4).forEach(p -> { // 4블록 폭발 범위
                    p.damage(50, golem.getBukkitEntity()); // 기획서에 있던 10의 피해
                });
            }
            this.stop();
        }
    }

    @Override
    public boolean canContinueToUse() { return actionTicks > 0; }
    @Override
    public void stop() { this.actionTicks = 0; }
}