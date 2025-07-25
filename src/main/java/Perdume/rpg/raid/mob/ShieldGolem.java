package Perdume.rpg.raid.mob;


import Perdume.rpg.Rpg;
import Perdume.rpg.raid.RaidInstance;
import Perdume.rpg.raid.ai.EntityShieldGolem;
import Perdume.rpg.raid.boss.AbstractBoss;
import Perdume.rpg.raid.boss.GolemKing;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import java.util.Optional;

public class ShieldGolem extends AbstractBoss implements Listener {

    private EntityShieldGolem customEntity;
    private final GolemKing master; // 이 골렘을 소환한 주인(메인 보스)

    public ShieldGolem(Rpg plugin, GolemKing master) {
        super(plugin, "보호막 골렘", 10000.0, 1000.0, "ShieldGolem");
        this.master = master;
    }

    @Override
    public void spawn(RaidInstance raidInstance, Location location) {
        this.raidInstance = raidInstance;
        this.customEntity = EntityShieldGolem.spawn(location, this);
        if (this.customEntity != null) {
            registerSelf(raidInstance);
            // [핵심] 이제 GolemKing이 등록을 책임지므로, 여기서 registerSelf를 호출하지 않습니다.
            // plugin.getServer().getPluginManager().registerEvents(this, plugin); // 피격 감지는 CombatListener가 전담
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
            // 죽었을 때, 주인(GolemKing)에게 알림
            master.onShieldGolemDestroyed(this);
        }
    }

    @Override
    public Optional<LivingEntity> getBukkitEntity() {
        return Optional.ofNullable(this.customEntity).map(EntityShieldGolem::getBukkitLivingEntity);
    }


    
    // --- Boss 인터페이스의 비어있는 메소드들 ---
    @Override
    public void onTick() { /* AI는 NMS에서 모두 처리 */ }
    @Override
    public void onDeath() { /* 사망 처리는 damage 메소드에서 직접 처리 */ }
    @Override
    public void cleanup() {
        HandlerList.unregisterAll(this);
        getBukkitEntity().ifPresent(LivingEntity::remove);
    }
    @Override
    public int getCurrentPhase() { return 0; }
}