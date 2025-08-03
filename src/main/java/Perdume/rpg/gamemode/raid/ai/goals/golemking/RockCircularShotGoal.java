package Perdume.rpg.gamemode.raid.ai.goals.golemking;


import Perdume.rpg.gamemode.raid.ai.EntityGolemKing;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal; // [핵심] NMS의 Goal 클래스를 상속
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.util.Vector;

import java.util.EnumSet;

public class RockCircularShotGoal extends Goal {
    private final EntityGolemKing golem;
    private int chargeTicks = 0;
    private int fireTicks = 0;
    private int projectilesFired = 0;

    public RockCircularShotGoal(EntityGolemKing golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    /**
     * [핵심 수정] canUseSkill() -> canUse() 로 변경
     */
    @Override
    public boolean canUse() {
        if (golem.isInGracePeriod()) return false;
        // 쿨타임이 0이고, 타겟이 8칸 '이상' 떨어져 있을 때만 사용 가능
        if (golem.skillCooldowns.get("CIRCULAR_SHOT") > 0) return false;
        LivingEntity target = golem.getTarget();
        return target != null && golem.distanceToSqr(target) >= 64;
    }

    @Override
    public void start() {
        this.chargeTicks = 20; // 2초 충전
        this.fireTicks = 0;
        this.projectilesFired = 0;
        this.golem.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (chargeTicks > 0) {
            chargeTicks--;
            Location loc = golem.getBukkitEntity().getLocation();
            loc.getWorld().spawnParticle(Particle.SMOKE, loc.add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0);
            if (chargeTicks == 20) {
                loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1, 1.2f);
            }
            return;
        }

        fireTicks++;
        if (fireTicks % 4 == 0 && projectilesFired < 16) {
            org.bukkit.entity.LivingEntity bukkitEntity = golem.getBukkitLivingEntity();
            Location eyeLoc = bukkitEntity.getEyeLocation();
            Level level = golem.level();

            eyeLoc.getWorld().playSound(eyeLoc, Sound.ENTITY_WITHER_SHOOT, 1, 1.5f);
            double angle = (projectilesFired / 8.0) * 2 * Math.PI;
            Vector direction = new Vector(Math.cos(angle), 0, Math.sin(angle)).normalize();

            WitherSkull skull = new WitherSkull(EntityType.WITHER_SKULL, level);
            skull.setOwner(golem);
            skull.setPos(eyeLoc.getX(), eyeLoc.getY(), eyeLoc.getZ());
            skull.shoot(direction.getX(), direction.getY(), direction.getZ(), 1.5F, 0.5F);
            level.addFreshEntity(skull);
            projectilesFired++;
        }

        if (projectilesFired >= 16) {
            this.stop();
        }
    }

    @Override
    public boolean canContinueToUse() {
        return chargeTicks > 0 || projectilesFired < 8;
    }

    @Override
    public void stop() {
        golem.skillCooldowns.put("CIRCULAR_SHOT", 120); // 12초
        this.chargeTicks = 0;
        this.fireTicks = 0;
        this.projectilesFired = 0;
    }
}