package Perdume.rpg.gamemode.raid.ai.goals.golemking;

import Perdume.rpg.gamemode.raid.ai.EntityGolemKing;
import net.minecraft.world.entity.ai.goal.Goal;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.util.Vector;

import java.util.EnumSet;

public class SpinningLaserGoal extends Goal {
    private final EntityGolemKing golem;
    private int cooldown = 0;
    private int actionTicks = 0;
    private float initialYaw;

    public SpinningLaserGoal(EntityGolemKing golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (golem.isInGracePeriod()) return false;
        if (cooldown > 0) return false;
        // 보스 체력이 75% 이하일 때부터 사용
        return golem.getController().getHealthPercentage() <= 0.75;
    }

    @Override
    public void start() {
        this.actionTicks = 150; // 1.5초 예고 + 6초 시전
        this.cooldown = 600; // 30초 쿨타임
        this.initialYaw = golem.getYHeadRot();
        this.golem.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (cooldown > 0) cooldown--;
        actionTicks--;

        if (actionTicks > 120) {
            Location eyeLoc = golem.getBukkitLivingEntity().getEyeLocation();
            if (actionTicks == 140) { // 시전 시작 시 사운드
                eyeLoc.getWorld().playSound(eyeLoc, Sound.ENTITY_WARDEN_ATTACK_IMPACT, 2, 0.5f);
            }
            // 보스의 눈 주변에 마법 파티클을 소환하여 힘을 모으는 것처럼 보이게 합니다.
            eyeLoc.getWorld().spawnParticle(Particle.ENCHANTED_HIT, eyeLoc, 20, 0.3, 0.3, 0.3, 0.05);
        }
        else if (actionTicks > 0) { // 6초(120틱) 시전
            float currentYaw = initialYaw + ((120 - actionTicks) / 120f) * 360f;
            golem.setYRot(currentYaw);

            Location eyeLoc = golem.getBukkitLivingEntity().getEyeLocation();
            Vector direction = eyeLoc.getDirection();
            for (int i = 1; i < 20; i++) {
                Location point = eyeLoc.clone().add(direction.clone().multiply(i));
                eyeLoc.getWorld().spawnParticle(Particle.DUST, point, 1, new Particle.DustOptions(org.bukkit.Color.RED, 1.0f));
                if (actionTicks % 2 == 0) {
                    point.getNearbyPlayers(1.5).forEach(p -> p.damage(80, golem.getBukkitEntity()));
                }
            }
        } else {
            this.stop();
        }
    }

    @Override
    public boolean canContinueToUse() { return actionTicks > 0; }
    @Override
    public void stop() { this.actionTicks = 0; }
}