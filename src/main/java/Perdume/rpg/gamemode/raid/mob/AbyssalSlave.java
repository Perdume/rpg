package Perdume.rpg.gamemode.raid.mob;


import Perdume.rpg.Rpg;
import Perdume.rpg.gamemode.raid.RaidInstance;
import Perdume.rpg.gamemode.raid.ai.EntityAbyssalSlave;
import Perdume.rpg.gamemode.raid.boss.AbstractBoss;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import java.util.Optional;

public class AbyssalSlave extends AbstractBoss {

    private EntityAbyssalSlave customEntity;

    public AbyssalSlave(Rpg plugin) {
        super(plugin, "심연의 노예", 500.0, 50.0, "AbyssalSlave");
    }

    @Override
    public void spawn(RaidInstance raidInstance, Location location) {
        this.raidInstance = raidInstance;
        this.customEntity = EntityAbyssalSlave.spawn(location, this);
        if (this.customEntity != null) {
            registerSelf(raidInstance);
            updateHealthBar();
        }
    }

    public void updateHealthBar() {
        if (customEntity != null) {
            customEntity.updateHealthBar(this.currentHealth, this.maxHealth);
        }
    }

    @Override
    public void damage(double amount, double armorIgnore) {
        super.damage(amount, armorIgnore);
        updateHealthBar();
        if (isDead() && customEntity != null && customEntity.isAlive()) {
            customEntity.setHealth(0);
        }
    }

    @Override
    public Optional<LivingEntity> getBukkitEntity() {
        return Optional.ofNullable(this.customEntity).map(EntityAbyssalSlave::getBukkitLivingEntity);
    }

    // --- 사용하지 않는 메소드들 ---
    @Override
    public void onTick() {}
    @Override
    public void onDeath() {}
    @Override
    public void cleanup() {}
    @Override
    public int getCurrentPhase() { return 0; }
}