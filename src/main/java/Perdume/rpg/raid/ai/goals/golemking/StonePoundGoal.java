package Perdume.rpg.raid.ai.goals.golemking;


import Perdume.rpg.raid.ai.EntityGolemKing;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;

import java.util.EnumSet;

public class StonePoundGoal extends Goal {
    private final EntityGolemKing golem;
    private int cooldown = 0;
    private int actionTicks = 0;

    public StonePoundGoal(EntityGolemKing golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (golem.isInGracePeriod()) return false;
        if (cooldown > 0) return false;
        LivingEntity target = golem.getTarget();
        if (target == null) return false;
        // 타겟이 6칸 이내에 있고, 주변에 다른 플레이어가 없을 때 (단일 대상)
        return golem.distanceToSqr(target) < 36 && golem.level().getEntitiesOfClass(net.minecraft.world.entity.player.Player.class, golem.getBoundingBox().inflate(8.0)).size() == 1;
    }

    @Override
    public void start() {
        this.actionTicks = 60; // 3초 시전 (소리 3초, 예고 1초)
        this.cooldown = 80; // 4초 쿨타임
        this.golem.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (cooldown > 0) cooldown--;
        actionTicks--;

        Location targetLoc = golem.getTarget().getBukkitEntity().getLocation();

        if (actionTicks > 20 && actionTicks % 20 == 0) { // 2초간 소리
            targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_GRINDSTONE_USE, 1, 0.5f);
        }
        if (actionTicks <= 20 && actionTicks > 0) { // 1초간 범위 예고
            targetLoc.getWorld().spawnParticle(Particle.EXPLOSION, targetLoc, 20, 2.5, 0.5, 2.5, 0);
        }

        if (actionTicks <= 0) {
            targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 2, 1f);
            targetLoc.getNearbyPlayers(5).forEach(p -> p.damage(80, golem.getBukkitEntity()));
            this.stop();
        }
    }


    
    @Override
    public boolean canContinueToUse() { return actionTicks > 0 && golem.getTarget() != null; }

    @Override
    public void stop() { this.actionTicks = 0; }
}