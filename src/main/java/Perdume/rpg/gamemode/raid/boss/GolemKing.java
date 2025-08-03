package Perdume.rpg.gamemode.raid.boss;


import Perdume.rpg.Rpg;
import Perdume.rpg.core.player.listener.CombatListener;
import Perdume.rpg.gamemode.raid.RaidInstance;
import Perdume.rpg.gamemode.raid.ai.EntityGolemKing;
import Perdume.rpg.gamemode.raid.mob.ShieldGolem;
import Perdume.rpg.core.reward.manager.RewardManager;
import Perdume.rpg.core.util.ItemFactory;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GolemKing extends AbstractBoss implements Listener {

    private enum Phase { ONE, TWO, THREE }
    private Phase currentPhase = Phase.ONE;
    private boolean hasDied = false; // [신규] onDeath가 중복 호출되는 것을 방지

    private boolean isRaged = false;

    private EntityGolemKing customEntity;
    private final List<ShieldGolem> shieldGolems = new ArrayList<>();
    private boolean isShieldActive = false;
    private int shieldPhaseTimer = 1800; // 90초
    private BukkitTask lavaFloorTask;
    private int lavaFloorRadius = 32;
    private int fieldPatternTimer = 200; // 10초
    private final Random random = new Random();

    public GolemKing(Rpg plugin, int partySize) {
        super(plugin, "골렘킹", 50000.0 * (1 + (partySize - 1) * 0.7), 300.0 * (1 + (partySize - 1) * 0.5), "GolemKing");
    }

    @Override
    public void spawn(RaidInstance raidInstance, Location location) {
        this.raidInstance = raidInstance;
        this.customEntity = EntityGolemKing.spawn(location, this);
        if (this.customEntity != null) {
            registerSelf(raidInstance);
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
        }
    }

    @Override
    public void onTick() {
        if (isDead() || customEntity == null || !customEntity.isAlive()) return;

        // --- 페이즈 전환 관리 ---
        if (currentPhase == Phase.ONE && getHealthPercentage() <= 0.6) { // 30,000 HP
            enterPhaseTwo();
        } else if (currentPhase == Phase.TWO && getHealthPercentage() <= 0.3) { // 15,000 HP
            enterPhaseThree();
        }

        // --- 보호막 페이즈 관리 ---
        if (!isShieldActive) {
            shieldPhaseTimer--;
            if (shieldPhaseTimer <= 0) {
                startShieldPhase();
                this.shieldPhaseTimer = 1800;
            }
        }
        fieldPatternTimer--;
        if (fieldPatternTimer <= 0) {
            executeFieldPattern();
            this.fieldPatternTimer = 200; // 쿨타임 10초로 초기화
        }
    }

    private void enterPhaseTwo() {
        this.currentPhase = Phase.TWO;
        raidInstance.broadcastMessage("§c2페이즈 돌입! 보스의 패턴이 변화합니다!");
        customEntity.setPhase(2);
        startShieldPhase();
    }

    private void enterPhaseThree() {
        this.currentPhase = Phase.THREE;
        raidInstance.broadcastMessage("§43페이즈 돌입! 골렘킹이 광폭화합니다!");
        customEntity.setPhase(3);
        if (!shieldGolems.isEmpty()) destroyShieldGolems();
        this.isShieldActive = false;

        this.lavaFloorTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::expandLavaFloor, 0L, 200L); // 10초마다
    }

    private void startShieldPhase() {
        this.isShieldActive = true;
        this.shieldPhaseTimer = 1800;
        raidInstance.broadcastMessage("§c골렘킹이 무적 보호막을 활성화하고, 보호막 골렘들을 소환합니다!");

        Location center = customEntity.getBukkitEntity().getLocation();
        Random random = new Random();
        Location spawnLoc = center.clone().add((random.nextDouble() - 0.5) * 20, 0, (random.nextDouble() - 0.5) * 20);
        ShieldGolem golem = new ShieldGolem(plugin, this);
        golem.spawn(raidInstance, spawnLoc);
        shieldGolems.add(golem);

        // [핵심 수정] RaidManager 대신, 자신이 소속된 RaidInstance에 직접 등록
        raidInstance.registerRaidEntity(golem);
    }

    public void onShieldGolemDestroyed(ShieldGolem golem) {
        shieldGolems.remove(golem);
        raidInstance.unregisterRaidEntity(golem); // [핵심] 등록 해제도 RaidInstance를 통해
        if (shieldGolems.isEmpty()) {
            this.isShieldActive = false;
            customEntity.setStunned(200); // 10초 기절
            raidInstance.broadcastMessage("§a보호막이 파괴되었습니다! 보스가 10초간 무력화됩니다!");
        }
    }

    private void destroyShieldGolems() {
        for (ShieldGolem golem : new ArrayList<>(shieldGolems)) {
            golem.cleanup();
        }
        shieldGolems.clear();
    }

    private void expandLavaFloor() {
        if (lavaFloorRadius <= 2) return;
        Location center = new Location(raidInstance.getRaidWorld(), 0, 64, 0);
        for (int x = -lavaFloorRadius; x <= lavaFloorRadius; x++) {
            for (int z = -lavaFloorRadius; z <= lavaFloorRadius; z++) {
                if (Math.abs(x) == lavaFloorRadius || Math.abs(z) == lavaFloorRadius) {
                    center.clone().add(x, 0, z).getBlock().setType(Material.LAVA);
                }
            }
        }
        lavaFloorRadius--;
    }

    @EventHandler
    public void onDamaged(EntityDamageByEntityEvent event) {
        if (customEntity == null || !event.getEntity().getUniqueId().equals(this.customEntity.getUUID())) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        event.setCancelled(true);
        CombatListener combatListener = plugin.getCombatListener();
        if (combatListener != null) {
            CombatListener.DamageResult finalDamage = combatListener.calculatePlayerAttackDamage(attacker, event.getDamage());
            double armorIgnore = combatListener.getPlayerArmorIgnore(attacker);
            this.damage(finalDamage.getDamage(), armorIgnore);
        }
    }

    @Override
    public void damage(double amount, double armorIgnore) {
        if (isShieldActive || isDead()) {
            return;
        }

        super.damage(amount, armorIgnore); // AbstractBoss의 대미지 계산 실행

        if (customEntity != null) {
            customEntity.updateHealthBar(this.currentHealth, this.maxHealth);

            // [핵심] 체력이 0 이하가 되면, '인형사'가 직접 사망 선고를 내립니다.
            if (isDead() && !hasDied) {
                this.hasDied = true; // 중복 실행 방지

                // 1. NMS 엔티티를 먼저 죽여서 더 이상 공격하지 않도록 합니다.
                customEntity.setHealth(0);

                // 2. onDeath()를 호출하여 보상을 지급하고 메시지를 보냅니다.
                onDeath();

                // 3. 마지막으로 RaidInstance에게 '클리어' 상태로 종료하라고 명령합니다.
                if (raidInstance != null) {
                    raidInstance.end(true);
                }
            }
        }
    }

    /**
     * [핵심 수정] 10초마다 무작위 플레이어 위치에 '커스텀 TNT' 패턴을 실행합니다.
     */
    private void executeFieldPattern() {
        if (raidInstance == null) return;
        List<Player> onlinePlayers = raidInstance.getOnlinePlayers();
        if (onlinePlayers.isEmpty()) return;

        Player target = onlinePlayers.get(random.nextInt(onlinePlayers.size()));
        Location targetLocation = target.getLocation();

        raidInstance.broadcastMessage("§c[경고] " + target.getName() + "의 위치에 불안정한 에너지가 감지됩니다!");

        // 1. 시각 효과용 '가짜' TNT를 소환합니다.
        TNTPrimed tnt = (TNTPrimed) targetLocation.getWorld().spawnEntity(targetLocation, EntityType.TNT);
        tnt.setFuseTicks(60); // 3초 후에 터지는 것처럼 보이게 함
        tnt.setIsIncendiary(false);
        // [중요] 실제 폭발력을 0으로 설정하여, 블록을 파괴하거나 피해를 주지 않도록 합니다.
        tnt.setYield(0);

        // 2. 3초 후에 '진짜' 피해를 주는 작업을 예약합니다.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // 레이드가 아직 진행 중일 때만 실행
            if (raidInstance.isFinished()) return;

            // 3. 폭발 이펙트와 사운드를 직접 재생합니다.
            targetLocation.getWorld().playSound(targetLocation, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);
            targetLocation.getWorld().spawnParticle(Particle.EXPLOSION, targetLocation, 1);

            // 4. 폭발 범위(5칸) 내의 플레이어를 찾아 커스텀 대미지를 입힙니다.
            targetLocation.getNearbyPlayers(5).forEach(player -> {
                // 레이드 중인 플레이어에게만 피해
                if (raidInstance.hasPlayer(player.getUniqueId())) {
                    double customDamage = 120.0;
                    player.damage(customDamage, customEntity.getBukkitEntity());
                    player.sendMessage("§c폭발에 휘말렸습니다!");
                }
            });

        }, 60L); // 60틱 = 3초
    }

    @Override
    public void onDeath() {
        if (raidInstance == null) return;

        // --- 보상 지급 로직 ---
        Economy econ = Rpg.econ;
        RewardManager rewardManager = plugin.getRewardManager();
        if (econ == null || rewardManager == null) {
            Rpg.log.severe("보상 지급 실패: Vault 경제 또는 보상 시스템을 찾을 수 없습니다!");
            return;
        }

        int partySize = raidInstance.getInitialPartySize();
        double baseGold = 250000.0;
        double bonusGoldPerMember = 50000.0;
        double goldPerPlayer = baseGold + (bonusGoldPerMember * (partySize - 1));

        int minEssence = 1 + (partySize / 2);
        int maxEssence = 5 + partySize;

        for (Player player : raidInstance.getOnlinePlayers()) {
            econ.depositPlayer(player, goldPerPlayer);
            player.sendMessage(String.format("§a[보상] §f%,.0f 골드를 획득했습니다.", goldPerPlayer));
            int essenceAmount = ThreadLocalRandom.current().nextInt(minEssence, maxEssence + 1);
            ItemStack bossEssence = ItemFactory.createBossEssence(essenceAmount);
            rewardManager.addReward(player, bossEssence);
        }

        // --- 기존 로직 ---
        if (lavaFloorTask != null) lavaFloorTask.cancel();
        destroyShieldGolems();
        raidInstance.broadcastMessage("§6골렘킹을 처치하고 보상을 획득했습니다!");
    }

    @Override
    public int getCurrentPhase() {
        return 0;
    }

    @Override
    public void cleanup() {
        // 1. GolemKing이 사용하던 리스너를 서버에서 안전하게 등록 해제합니다.
        HandlerList.unregisterAll(this);

        // 2. 남아있을 수 있는 모든 스케줄러(용암 바닥 등)를 취소합니다.
        if (lavaFloorTask != null && !lavaFloorTask.isCancelled()) {
            lavaFloorTask.cancel();
        }

        // 3. 자신이 소환했던 모든 보호막 골렘들을 정리합니다.
        destroyShieldGolems();
    }

    @Override
    public Optional<LivingEntity> getBukkitEntity() {
        return Optional.ofNullable(this.customEntity).map(EntityGolemKing::getBukkitLivingEntity);
    }
}