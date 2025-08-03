package Perdume.rpg.gamemode.raid.ai.goals.golemking;


import Perdume.rpg.gamemode.raid.ai.EntityGolemKing;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.EnumSet;
import java.util.List;

public class EarthshatterGoal extends Goal {
    private final EntityGolemKing golem;
    private int cooldown = 0;
    private int actionTicks = 0;

    public EarthshatterGoal(EntityGolemKing golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (golem.isInGracePeriod()) return false;
        if (cooldown > 0) return false;
        if (golem.getTarget() == null) return false;
        AABB area = golem.getBoundingBox().inflate(8.0);
        List<Player> playersInArea = golem.level().getEntitiesOfClass(Player.class, area);
        return playersInArea.size() >= 2;
    }

    @Override
    public void start() {
        this.actionTicks = 50; // 2.5초 시전
        this.cooldown = 150; // 7.5초 쿨타임
        this.golem.getNavigation().stop();
    }

    @Override
    public void tick() {
        actionTicks--;
        LivingEntity target = golem.getTarget();
        if (target == null) {
            this.stop();
            return;
        }

        Location bossLoc = golem.getBukkitEntity().getLocation();

        // 2.5초(50틱) 중 앞의 2초(40틱)는 범위 예고
        if (actionTicks > 10) {
            if (actionTicks == 40) {
                bossLoc.getWorld().playSound(bossLoc, Sound.ENTITY_WARDEN_SONIC_CHARGE, 2, 0.5f);
            }

            // --- [핵심] 부채꼴 범위 예고 파티클 ---
            Vector direction = target.getBukkitEntity().getLocation().toVector().subtract(bossLoc.toVector()).setY(0).normalize();

            // 120도 범위를 그리기 위해, 중심 방향에서 -60도 ~ +60도 사이를 반복
            for (int i = -60; i <= 60; i += 10) { // 10도 간격으로 파티클 생성
                Vector particleDir = direction.clone().rotateAroundY(Math.toRadians(i));
                for (int d = 1; d < 12; d++) { // 12블록 길이
                    Location point = bossLoc.clone().add(particleDir.clone().multiply(d));
                    point.getWorld().spawnParticle(Particle.DUST, point, 1, new Particle.DustOptions(Color.RED, 1.0f));
                }
            }

        } else if (actionTicks <= 0) { // 시전 종료 후 공격
            bossLoc.getWorld().playSound(bossLoc, Sound.ENTITY_GENERIC_EXPLODE, 2, 1f);
            // TODO: 실제 피해 적용 로직을 부채꼴 범위에 맞게 수정해야 함
            bossLoc.getNearbyPlayers(12).forEach(p -> {
                p.damage(200, golem.getBukkitEntity());
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 1));
            });
            this.stop();
        }
    }
    
    @Override
    public boolean canContinueToUse() { return actionTicks > 0; }
    @Override
    public void stop() { this.actionTicks = 0; }
}