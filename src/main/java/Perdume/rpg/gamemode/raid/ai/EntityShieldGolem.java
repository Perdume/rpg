package Perdume.rpg.gamemode.raid.ai;


import Perdume.rpg.gamemode.raid.ai.goals.shieldgolem.ChargeGoal;
import Perdume.rpg.gamemode.raid.ai.goals.shieldgolem.WhirlwindGoal;
import Perdume.rpg.gamemode.raid.mob.ShieldGolem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;
import net.minecraft.network.chat.Component;
import java.text.NumberFormat;


public class EntityShieldGolem extends IronGolem {

    private final ShieldGolem controller;

    public EntityShieldGolem(Level world, ShieldGolem controller) {
        super(EntityType.IRON_GOLEM, world);
        this.controller = controller;
        this.setPersistenceRequired(true);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new WhirlwindGoal(this));
        this.goalSelector.addGoal(2, new ChargeGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }
    
    public void updateHealthBar(double current, double max) {
        NumberFormat formatter = NumberFormat.getInstance();
        String name = "§a보호막 골렘 §eHP: " + formatter.format(current) + " / " + formatter.format(max);
        this.setCustomName(Component.literal(name));
        this.setCustomNameVisible(true);
    }

    public static EntityShieldGolem spawn(Location location, ShieldGolem controller) {
        ServerLevel world = ((CraftWorld) location.getWorld()).getHandle();
        EntityShieldGolem minion = new EntityShieldGolem(world, controller);
        minion.setPos(location.getX(), location.getY(), location.getZ());
        world.addFreshEntity(minion, CreatureSpawnEvent.SpawnReason.CUSTOM);
        return minion;
    }
}