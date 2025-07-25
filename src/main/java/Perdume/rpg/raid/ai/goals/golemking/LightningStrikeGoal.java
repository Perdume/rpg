package Perdume.rpg.raid.ai.goals.golemking;

import Perdume.rpg.raid.ai.EntityGolemKing;
import net.minecraft.world.entity.ai.goal.Goal;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class LightningStrikeGoal extends Goal {
    private final EntityGolemKing golem;
    private int cooldown = 0;
    private int actionTicks = 0;
    private final List<Location> strikeLocations = new ArrayList<>();

    public LightningStrikeGoal(EntityGolemKing golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (golem.isInGracePeriod()) return false;
        if (cooldown > 0) return false;
        // 원거리 플레이어가 다수일 때 (10칸 이상 떨어진 플레이어가 2명 이상)
        long rangedPlayers = golem.level().getEntitiesOfClass(net.minecraft.world.entity.player.Player.class, golem.getBoundingBox().inflate(30.0))
                .stream()
                .filter(p -> p.distanceToSqr(golem) > 100)
                .count();
        return rangedPlayers >= 2;
    }

    @Override
    public void start() {
        this.actionTicks = 80; // 4초 예고
        this.cooldown = 200; // 10초 쿨타임
        this.strikeLocations.clear();
        golem.level().getEntitiesOfClass(net.minecraft.world.entity.player.Player.class, golem.getBoundingBox().inflate(30.0))
                .forEach(p -> strikeLocations.add(p.getBukkitEntity().getLocation()));
    }

    @Override
    public void tick() {
        if (cooldown > 0) cooldown--;
        actionTicks--;

        if (actionTicks > 0) { // 예고
            for (Location loc : strikeLocations) {
                loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 10, 1, 1, 1, 0.1);
            }
        } else {
            for (Location loc : strikeLocations) {
                loc.getWorld().strikeLightning(loc); // 실제 번개
            }
            this.stop();
        }
    }

    @Override
    public boolean canContinueToUse() { return actionTicks > 0; }
    @Override
    public void stop() { this.actionTicks = 0; }
}