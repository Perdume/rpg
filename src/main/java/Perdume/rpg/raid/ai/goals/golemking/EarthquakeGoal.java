package Perdume.rpg.raid.ai.goals.golemking;


import Perdume.rpg.raid.ai.EntityGolemKing;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class EarthquakeGoal extends Goal {
    private final EntityGolemKing golem;
    private int actionTicks = 0;
    private final List<Vector> fissureDirections = new ArrayList<>();

    public EarthquakeGoal(EntityGolemKing golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (golem.isInGracePeriod()) return false;
        if (golem.skillCooldowns.get("EARTHQUAKE") > 0) return false;
        // 플레이어들이 한 방향에 뭉쳐있는지 확인하는 로직 (예시)
        // 여기서는 단순하게 타겟이 있을 때 확률적으로 사용하도록 구현
        return golem.getTarget() != null && golem.getRandom().nextFloat() < 0.25f;
    }

    @Override
    public void start() {
        this.actionTicks = 30; // 4초 예고
        this.fissureDirections.clear();
        this.golem.getNavigation().stop();
        this.golem.getBukkitEntity().getWorld().playSound(golem.getBukkitEntity().getLocation(), Sound.ENTITY_WARDEN_DIG, 2, 0.5f);

        LivingEntity target = this.golem.getTarget();
        if (target != null) {
            Vector initialDir = target.getBukkitEntity().getLocation().toVector()
                    .subtract(golem.getBukkitEntity().getLocation().toVector()).setY(0).normalize();
            fissureDirections.add(initialDir);
            fissureDirections.add(initialDir.clone().rotateAroundY(Math.toRadians(120)));
            fissureDirections.add(initialDir.clone().rotateAroundY(Math.toRadians(-120)));
        }
    }

    @Override
    public void tick() {
        actionTicks--;
        Location start = golem.getBukkitEntity().getLocation();

        if (actionTicks > 0) { // 예고
            for (Vector dir : fissureDirections) {
                for (int i = 1; i < 15; i++) { // 15칸 길이의 균열
                    Location point = start.clone().add(dir.clone().multiply(i));
                    start.getWorld().spawnParticle(Particle.CRIT, point, 1, 0, 0, 0, 0);
                }
            }
        } else { // 폭발
            start.getWorld().playSound(start, Sound.ENTITY_GENERIC_EXPLODE, 2, 1);
            for (Vector dir : fissureDirections) {
                for (int i = 1; i < 15; i++) {
                    Location point = start.clone().add(dir.clone().multiply(i));
                    start.getWorld().spawnParticle(Particle.EXPLOSION, point, 1);
                    point.getNearbyPlayers(1.5).forEach(p -> p.damage(160, golem.getBukkitEntity()));
                }
            }
            this.stop();
        }
    }

    @Override
    public boolean canContinueToUse() {
        return actionTicks > 0;
    }

    @Override
    public void stop() {
        golem.skillCooldowns.put("EARTHQUAKE", 160); // 8초
        this.actionTicks = 0;
    }
}