package Perdume.rpg.gamemode.raid.ai;

import Perdume.rpg.gamemode.raid.ai.goals.golemking.*;
import Perdume.rpg.gamemode.raid.boss.GolemKing;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

public class EntityGolemKing extends IronGolem {

    private final GolemKing controller;
    public final Map<String, Integer> skillCooldowns = new HashMap<>();
    private int currentPhase = 1;
    private boolean isEnraged = false;
    private int stunTicks = 0;
    private int gracePeriodTicks;

    public EntityGolemKing(Level world, GolemKing controller) {
        super(EntityType.IRON_GOLEM, world);
        this.controller = controller;
        this.setPersistenceRequired(true);
        this.setNoAi(false);
        this.setCustomNameVisible(true);

        // 모든 스킬 쿨타임 초기화
        skillCooldowns.put("EARTHSHATTER", 0);
        skillCooldowns.put("CRUSHING_BLOW", 0);
        skillCooldowns.put("CHARGE", 0);
        skillCooldowns.put("STONE_POUND", 0);
        skillCooldowns.put("METEOR", 0);
        skillCooldowns.put("CIRCULAR_SHOT", 0);
        skillCooldowns.put("LIGHTNING", 0);
        skillCooldowns.put("SPIN_LASER", 0);
        skillCooldowns.put("EARTHQUAKE", 0);

        this.gracePeriodTicks = 100; // 5초 (20틱 * 5)의 예열 시간을 줍니다.
    }

    @Override
    protected void registerGoals() {

        this.goalSelector.getAvailableGoals().clear();
        this.targetSelector.getAvailableGoals().clear();

        // --- 타겟 설정 AI ---
        // [핵심] 1순위: 가장 가까운 플레이어를 공격 대상으로 삼는, 마인크래프트 순정 AI를 사용합니다.
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));

        // --- 스킬 및 행동 AI (우선순위가 낮을수록 먼저 실행) ---
        // 기획서의 우선순위에 따라 모든 스킬 Goal을 등록합니다.
        this.goalSelector.addGoal(2, new EarthshatterGoal(this));       // 2순위: 근접 광역기
        this.goalSelector.addGoal(3, new CrushingBlowGoal(this));      // 3순위: 원거리 저격기
        this.goalSelector.addGoal(4, new ChargeGoal(this));            // 4순위: 돌진 추격기
        this.goalSelector.addGoal(5, new StonePoundGoal(this));        // 기타 스킬
        this.goalSelector.addGoal(5, new MeteorStrikeGoal(this));
        this.goalSelector.addGoal(5, new RockCircularShotGoal(this));
        this.goalSelector.addGoal(6, new LightningStrikeGoal(this));
        this.goalSelector.addGoal(6, new SpinningLaserGoal(this));
        this.goalSelector.addGoal(6, new EarthquakeGoal(this));

        // 7순위: 위의 어떤 스킬도 사용할 수 없을 때 최후의 수단으로 사용합니다.
        this.goalSelector.addGoal(7, new MeleeAttackGoal(this, 1.0D, true));

        // 8순위: 주변 플레이어 쳐다보기
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 15.0F));
        // 9순위: 할 일이 없을 때 주변 둘러보기
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // 예열 시간이 아직 남았다면, 스킬 쿨타임을 줄이지 않고 즉시 종료
        if (gracePeriodTicks > 0) {
            gracePeriodTicks--;
            return;
        }

        if (stunTicks > 0) {
            this.getNavigation().stop();
            stunTicks--;
            return;
        }
        skillCooldowns.replaceAll((skill, cooldown) -> cooldown > 0 ? cooldown - 1 : 0);

        if (stunTicks > 0) {
            this.getNavigation().stop();
            stunTicks--;
            return;
        }
        skillCooldowns.replaceAll((skill, cooldown) -> cooldown > 0 ? cooldown - 1 : 0);
    }

    /**
     * [신규] 현재 예열 시간 중인지 확인하는 메소드
     * @return 예열 시간이면 true
     */
    public boolean isInGracePeriod() {
        return this.gracePeriodTicks > 0;
    }

    public void setPhase(int phase) {
        this.currentPhase = phase;
    }

    public void setEnraged(boolean enraged) {
        if(this.isEnraged == enraged) return;
        this.isEnraged = enraged;
        if (enraged) {
            // 모든 스킬 쿨타임 50% 감소
            skillCooldowns.replaceAll((skill, cooldown) -> cooldown / 2);
        }
    }

    public void setStunned(int ticks) {
        this.stunTicks = ticks;
    }

    public GolemKing getController() {
        return this.controller;
    }

    public void updateHealthBar(double current, double max) {
        NumberFormat formatter = NumberFormat.getInstance();
        String name = "§c[보스] 골렘킹 §eHP: " + formatter.format(current) + " / " + formatter.format(max);
        this.setCustomName(Component.literal(name));
    }

    public void enterRageMode() {
        this.getBukkitEntity().getWorld().playSound(this.getBukkitEntity().getLocation(), Sound.ENTITY_WITHER_SPAWN, 2, 0.8f);
    }

    public static EntityGolemKing spawn(Location location, GolemKing controller) {
        ServerLevel world = ((CraftWorld) location.getWorld()).getHandle();
        EntityGolemKing golem = new EntityGolemKing(world, controller);
        golem.setPos(location.getX(), location.getY(), location.getZ());
        world.addFreshEntity(golem, CreatureSpawnEvent.SpawnReason.CUSTOM);
        return golem;
    }
}