package Perdume.rpg.raid.ai.goals.golemking;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB; // [핵심] AABB import

import java.util.Comparator;
import java.util.EnumSet;

public class FindPlayerGoal extends TargetGoal {
    private LivingEntity target;

    public FindPlayerGoal(net.minecraft.world.entity.Mob mob) {
        super(mob, false);
        // 이 AI가 실행되는 동안에는 다른 타겟팅 AI가 작동하지 않도록 설정
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        // 이미 타겟이 있거나, 스턴 상태이면 새로 찾지 않음
        if (this.mob.getTarget() != null && this.mob.isWithinMeleeAttackRange(this.mob.getTarget())) {
            return false;
        }

        AABB searchArea = this.mob.getBoundingBox().inflate(40.0);
        this.target = this.mob.level().getEntitiesOfClass(Player.class, searchArea)
                .stream()
                .min(Comparator.comparingDouble(p -> p.distanceToSqr(this.mob)))
                .orElse(null);

        return this.target != null;
    }

    @Override
    public void start() {
        // [핵심 수정] 불필요한 Bukkit API 정보를 모두 제거하고, NMS 엔티티 타겟만 전달합니다.
        this.mob.setTarget(this.target);
        super.start();
    }
}