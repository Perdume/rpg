package Perdume.rpg.raid.boss;

import Perdume.rpg.Rpg;
import Perdume.rpg.raid.RaidInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.text.NumberFormat;
import java.util.Optional;

public abstract class AbstractBoss implements Boss {

    protected final Rpg plugin;
    protected RaidInstance raidInstance;
    protected final String baseName;
    protected final double maxHealth;
    protected double currentHealth;
    protected final double defense;
    protected final String bossId; // [신규] 보스의 고유 ID를 저장할 변수

    // [핵심 수정] 모든 자식 클래스가 공유할 단일 엔티티 참조 변수
    protected Mob nmsEntity;
    protected LivingEntity bukkitEntity;

    public AbstractBoss(Rpg plugin, String baseName, double maxHealth, double defense, String bossId) {
        this.plugin = plugin;
        this.baseName = baseName;
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;
        this.defense = defense;
        this.bossId = bossId;
    }

    @Override
    public void onTick() {
        if (!isDead()) {
            updateHealthBar();
        }
    }
    @Override
    public String getBossId() {
        return this.bossId;
    }

    @Override
    public void damage(double amount, double armorIgnore) {
        if (isDead()) return;
        double effectiveDefense = this.defense * (1.0 - Math.min(1.0, armorIgnore));
        double defenseReduction = effectiveDefense / (effectiveDefense + 1000);
        double finalDamage = amount * (1.0 - defenseReduction);
        this.currentHealth -= finalDamage;
        if (this.currentHealth < 0) {
            this.currentHealth = 0;
        }
    }

    public void updateHealthBar() {
        if (nmsEntity == null) return;
        NumberFormat formatter = NumberFormat.getInstance();
        String name = getBaseName() + " §eHP: " + formatter.format(this.currentHealth) + " / " + formatter.format(this.maxHealth);
        nmsEntity.setCustomName(Component.literal(name));
        nmsEntity.setCustomNameVisible(true);
    }

    // --- 나머지 Getter 및 추상 메서드 ---
    @Override
    public String getBaseName() { return this.baseName; }
    @Override
    public double getCurrentHealth() { return this.currentHealth; }
    @Override
    public double getMaxHealth() { return this.maxHealth; }
    @Override
    public double getHealthPercentage() {
        if (this.maxHealth == 0) return 0;
        return this.currentHealth / this.maxHealth;
    }
    @Override
    public boolean isDead() { return this.currentHealth <= 0; }
    @Override
    public void registerSelf(RaidInstance raidInstance) {
        getBukkitEntity().ifPresent(entity -> {
            Rpg.getInstance().getRaidManager().registerBossEntity(entity.getUniqueId(), raidInstance);
        });
    }

    @Override
    public abstract void spawn(RaidInstance raidInstance, Location location);
    @Override
    public abstract void cleanup();
    @Override
    public abstract Optional<LivingEntity> getBukkitEntity();
    @Override
    public abstract void onDeath();
    @Override
    public abstract int getCurrentPhase();
}