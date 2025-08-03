package Perdume.rpg.gamemode.raid.ai.goals.shieldgolem;


import Perdume.rpg.gamemode.raid.ai.EntityShieldGolem;
import net.minecraft.world.entity.ai.goal.Goal;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.util.Vector;

import java.util.EnumSet;

public class WhirlwindGoal extends Goal {
    private final EntityShieldGolem golem;
    private int actionTicks = 0;
    private int cooldown = 0;

    public WhirlwindGoal(EntityShieldGolem golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) return false;
        return golem.getTarget() != null && golem.distanceToSqr(golem.getTarget()) < 100; // 10칸 이내
    }

    @Override
    public void start() {
        this.actionTicks = 100; // 5초간 시전
        this.cooldown = 300; // 15초 쿨타임
        golem.getBukkitEntity().getWorld().playSound(golem.getBukkitEntity().getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 2, 0.5f);
    }

    @Override
    public void tick() {
        if (cooldown > 0) cooldown--;
        actionTicks--;
        if (actionTicks <= 0) {
            this.stop();
            return;
        }

        // 주변을 돌며 파티클 생성
        Location loc = golem.getBukkitEntity().getLocation();
        loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc.add(0, 1, 0), 5, 1, 0, 1);
        
        // 10틱마다 주변 플레이어에게 피해 및 넉백
        if (actionTicks % 10 == 0) {
            loc.getNearbyPlayers(5).forEach(p -> {
                p.damage(40, golem.getBukkitEntity());
                Vector knockback = p.getLocation().toVector().subtract(loc.toVector()).normalize().setY(0.3);
                p.setVelocity(knockback);
            });
        }
    }
    
    @Override
    public void stop() { this.actionTicks = 0; }
}