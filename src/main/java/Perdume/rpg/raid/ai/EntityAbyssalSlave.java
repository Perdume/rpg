package Perdume.rpg.raid.ai;


import Perdume.rpg.raid.mob.AbyssalSlave;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.bukkit.Location;

import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;
import java.text.NumberFormat;

public class EntityAbyssalSlave extends WitherSkeleton {

    private final AbyssalSlave controller;

    public EntityAbyssalSlave(Level world, AbyssalSlave controller) {
        super(EntityType.WITHER_SKELETON, world);
        this.controller = controller;
        this.setPersistenceRequired(true);
        this.setCustomNameVisible(true);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }


    public void updateHealthBar(double current, double max) {
        if (current <= 0) {
            this.setCustomName(Component.literal("§7쓰러짐"));
            return;
        }
        NumberFormat formatter = NumberFormat.getInstance();
        String name = "§7심연의 노예 §eHP: " + formatter.format(current) + " / " + formatter.format(max);
        this.setCustomName(Component.literal(name));
    }

    public static EntityAbyssalSlave spawn(Location location, AbyssalSlave controller) {
        ServerLevel world = ((CraftWorld) location.getWorld()).getHandle();
        EntityAbyssalSlave minion = new EntityAbyssalSlave(world, controller);
        minion.setPos(location.getX(), location.getY(), location.getZ());
        world.addFreshEntity(minion, CreatureSpawnEvent.SpawnReason.CUSTOM);
        return minion;
    }
}