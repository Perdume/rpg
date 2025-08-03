package Perdume.rpg.gamemode.raid;

import Perdume.rpg.Rpg;
import Perdume.rpg.core.party.Party;
import Perdume.rpg.core.party.PartyManager;
import Perdume.rpg.core.player.data.PlayerDataManager;
import Perdume.rpg.gamemode.raid.boss.Boss;
import Perdume.rpg.system.RaidErrorHandler;
import Perdume.rpg.system.RaidManager;
import Perdume.rpg.core.util.TeleportUtil;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.stream.Collectors;

public class RaidInstance {

    public enum RaidState { WAITING, STARTING, RUNNING, FINISHED }
    private RaidState currentState;
    private final int initialPartySize; // 시작 인원

    private final Rpg plugin;
    private final RaidManager raidManager;
    private final World raidWorld;
    private final Boss boss;
    private final Map<UUID, Location> participants = new HashMap<>();
    private final Map<UUID, Integer> deathCounts = new HashMap<>();
    private final Map<UUID, Boolean> revivingPlayers = new HashMap<>();

    private final int TIME_LIMIT_SECONDS = 600; // 10분
    private int timeLeft = TIME_LIMIT_SECONDS;
    private int timeElapsed = 0;
    private BossBar bossBar;
    private BukkitTask gameTimer;

    private final Map<UUID, Boss> raidEntities = new HashMap<>();

    public RaidInstance(Rpg plugin, RaidManager raidManager, List<Player> players, World raidWorld, Boss boss) {
        this.plugin = plugin;
        this.raidManager = raidManager;
        this.raidWorld = raidWorld;
        this.boss = boss;
        players.forEach(p -> {
            this.participants.put(p.getUniqueId(), p.getLocation());
            this.deathCounts.put(p.getUniqueId(), 5);
        });
        this.currentState = RaidState.WAITING;
        this.initialPartySize = players.size();
    }

    public void start() {
        this.currentState = RaidState.STARTING;

        // 1. [핵심] 보스바를 여기서 직접 생성하고 플레이어를 추가합니다.
        this.bossBar = Bukkit.createBossBar("§c[ RAID ] §f레이드 시작 대기 중...", BarColor.RED, BarStyle.SOLID);
        bossBar.setProgress(1.0);
        getOnlinePlayers().forEach(bossBar::addPlayer);

        // 2. 플레이어들을 시작 지점으로 텔레포트
        Location playerSpawn = new Location(raidWorld, 0.5, 65, 0.5);
        getOnlinePlayers().forEach(p -> {
            p.teleport(playerSpawn);
            p.setGameMode(GameMode.ADVENTURE);
        });

        // 3. 15초 카운트다운 시작
        new BukkitRunnable() {
            int countdown = 15;
            @Override
            public void run() {
                // ... (카운트다운 중 플레이어 이탈 시 종료 로직)
                if (countdown > 0) {
                    // [핵심] 보스바의 제목만 변경합니다.
                    bossBar.setTitle("§c[ RAID ] §f전투 시작까지... " + countdown + "초");
                    countdown--;
                } else {
                    this.cancel();
                    startCombatPhase();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * [신규] 이 레이드 인스턴스에 새로운 Boss 개체(쫄몹, 수정 등)를 등록합니다.
     * @param bossObject 등록할 Boss 객체
     */
    public void registerRaidEntity(Boss bossObject) {
        bossObject.getBukkitEntity().ifPresent(entity -> {
            this.raidEntities.put(entity.getUniqueId(), bossObject);
            // RaidManager에게도 동일하게 알려줍니다.
            plugin.getRaidManager().registerBossEntity(entity.getUniqueId(), this);
        });
    }

    /**
     * [신규] 이 레이드 인스턴스에서 Boss 개체를 제거합니다.
     * @param bossObject 제거할 Boss 객체
     */
    public void unregisterRaidEntity(Boss bossObject) {
        bossObject.getBukkitEntity().ifPresent(entity -> {
            this.raidEntities.remove(entity.getUniqueId());
            plugin.getRaidManager().unregisterBossEntity(entity.getUniqueId());
        });
    }

    /**
     * [핵심] 엔티티 객체로 이 레이드에 속한 Boss 객체를 찾아 반환합니다.
     */
    public Optional<Boss> getBossByEntity(Entity entity) {
        if (entity == null) return Optional.empty();

        // 1. 메인 보스인지 확인
        if (boss.getBukkitEntity().isPresent() && boss.getBukkitEntity().get().equals(entity)) {
            return Optional.of(boss);
        }

        // 2. 쫄몹이나 수정 같은 다른 엔티티인지 확인
        return Optional.ofNullable(raidEntities.get(entity.getUniqueId()));
    }

    private void startCombatPhase() {
        this.currentState = RaidState.RUNNING;
        broadcastMessage("§c[알림] §4전투가 시작되었습니다!");

        // 1. 보스 소환
        Location bossSpawn = new Location(raidWorld, 0.5, 65, 0.5);
        this.boss.spawn(this, bossSpawn);

        // 2. 플레이어 스탯 적용
        getOnlinePlayers().forEach(p -> plugin.getAttributeListener().applyRaidAttributes(p));

        // 3. 메인 게임 타이머 시작
        this.gameTimer = new BukkitRunnable() {
            @Override
            public void run() {
                // --- [핵심] 치명적 오류 감지 시스템 ---
                try {
                    if (currentState != RaidState.RUNNING) {
                        this.cancel();
                        return;
                    }
                    timeLeft--;
                    timeElapsed++;

                    // 보스의 AI 로직은 얘기치 못한 오류를 발생시킬 가능성이 가장 높습니다.
                    boss.onTick();

                    // 보스바 업데이트
                    if (!boss.isDead()) {
                        double healthPercent = boss.getCurrentHealth() / boss.getMaxHealth();
                        // RaidInstance가 직접 관리하는 bossBar를 업데이트합니다.
                        bossBar.setProgress(Math.max(0, healthPercent));
                        bossBar.setTitle(String.format("§c%s §e남은 시간: %d분 %d초",
                                boss.getBaseName(), timeLeft / 60, timeLeft % 60));
                    }

                    // 종료 조건 확인
                    if (boss.isDead()) {
                        end(true);
                        return;
                    }
                    if (timeLeft <= 0) {
                        broadcastMessage("§c[알림] §4제한 시간이 초과되어 레이드에 실패했습니다.");
                        end(false);
                        return;
                    }
                    if (getOnlinePlayers().isEmpty()) {
                        end(false);
                    }

                } catch (Exception e) {
                    // [핵심] 어떤 오류든 감지되면, RaidErrorHandler를 통해 안전하게 레이드를 종료합니다.
                    RaidErrorHandler.handle(RaidInstance.this, e);
                    this.cancel(); // 이 타이머를 즉시 중지합니다.
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // 1초마다 반복
    }

    /**
     * [신규] 레이드 도중 접속을 종료한 플레이어에게 페널티를 부여합니다.
     * @param player 접속 종료한 플레이어
     */
    public void handlePlayerQuitPenalty(Player player) {
        broadcastMessage("§c[알림] " + player.getName() + "님이 레이드에서 이탈했습니다.");

        // 1. 데스카운트를 0으로 만듭니다.
        deathCounts.put(player.getUniqueId(), 0);

        // 2. 파티에서 강제로 탈퇴시킵니다.
        PartyManager.leaveParty(player);

        // 3. 레이드 참여자 명단에서는 제거하지 않습니다. (귀환 위치 정보를 보존하기 위해)
        // participants.remove(player.getUniqueId()); -> 이 줄을 주석 처리하거나 삭제

        // 만약 모든 '온라인' 플레이어가 나갔다면, 레이드를 실패 처리하고 즉시 종료
        if (getOnlinePlayers().isEmpty()) {
            end(false);
        }
    }

    public Location getOriginalLocation(Player player) {
        return participants.get(player.getUniqueId());
    }

    public void startRevivalSequence(Player player) {
        UUID uuid = player.getUniqueId();
        if (revivingPlayers.getOrDefault(uuid, false)) return;

        int remainingLives = deathCounts.getOrDefault(uuid, 1) - 1;
        deathCounts.put(uuid, remainingLives);
        revivingPlayers.put(uuid, true);

        // 1. 다른 모든 플레이어에게서 이 플레이어를 숨깁니다.
        List<Player> otherPlayers = getOnlinePlayers().stream().filter(p -> !p.getUniqueId().equals(uuid)).toList();
        for (Player other : otherPlayers) {
            other.hidePlayer(plugin, player);
        }

        // 2. 즉시 무적 및 체력 회복, 보스 타겟 해제
        player.setInvulnerable(true);
        player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
        getBoss().getBukkitEntity().ifPresent(bossEntity -> {
            if (bossEntity instanceof net.minecraft.world.entity.Mob nmsMob) {
                if (nmsMob.getTarget() != null && nmsMob.getTarget().getUUID().equals(player.getUniqueId())) {
                    nmsMob.setTarget(null);
                }
            }
        });

        broadcastMessage("§e[알림] §f" + player.getName() + "님이 쓰러져 5초간 전장에서 이탈합니다. §c(남은 목숨: " + remainingLives + "개)");
        player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1, 1);

        // 3. 5초 후 부활
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.setInvulnerable(false);
                    List<Player> allPlayers = getOnlinePlayers();
                    for (Player other : allPlayers) {
                        other.showPlayer(plugin, player);
                    }
                    player.sendMessage("§a[알림] 전장에 복귀합니다!");
                }
                revivingPlayers.remove(uuid);
            }
        }.runTaskLater(plugin, 100L); // 5초
    }

    /**
     * [수정] 데스카운트가 0인 플레이어가 진짜 죽었을 때 호출됩니다.
     */
    public void handleFinalPlayerDeath(Player player) {
        broadcastMessage("§c[알림] §4" + player.getName() + "님이 모든 목숨을 잃어 관전합니다.");

        // 즉시 관전 모드로 변경하여 리스폰 위치 오류를 원천 차단
        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(getRespawnLocation()); // 관전 위치로 이동

        boolean allDead = getOnlinePlayers().stream().allMatch(p -> deathCounts.getOrDefault(p.getUniqueId(), 0) <= 0);
        if (allDead) {
            handlePartyWipe();
        }
    }

    public boolean isPlayerReviving(Player player) {
        return revivingPlayers.getOrDefault(player.getUniqueId(), false);
    }

    private void handlePartyWipe() {
        broadcastMessage("§c[알림] §4모든 파티원이 사망하여 레이드에 실패했습니다.");
        Location wipeLocation = new Location(raidWorld, 0.5, 65, 0.5);
        for (UUID uuid : participants.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.setGameMode(GameMode.ADVENTURE);
                p.teleport(wipeLocation);
            }
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                end(false);
            }
        }.runTaskLater(plugin, 100L);
    }

    public void onPlayerQuit(Player player) {
        broadcastMessage("§e[알림] " + player.getName() + "님이 레이드에서 나갔습니다.");
        if (getOnlinePlayers().isEmpty()) {
            end(false);
        }
    }

    public void end(boolean isClear) {
        if (this.currentState == RaidState.FINISHED) return;
        this.currentState = RaidState.FINISHED;
        if (gameTimer != null) gameTimer.cancel();

        if (bossBar != null) {
            bossBar.removeAll();
        }

        String endMessage = isClear ? "§a레이드 클리어!" : "§c레이드 실패...";

        // --- [핵심] 새로운 귀환 시퀀스 ---

        // 1. 모든 파티원을 레이드 월드의 중앙 지점으로 이동시킵니다.
        Location centerPoint = new Location(raidWorld, 0.5, 65, 0.5);
        for (UUID uuid : participants.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.setGameMode(GameMode.ADVENTURE); // 관전자도 모드 변경
                p.teleport(centerPoint);
            }
        }

        // 2. 결과 메시지를 전송합니다.
        broadcastMessage(endMessage);

        if (isClear) {
            // 클리어 시에만 클리어 기록 저장
            PlayerDataManager dataManager = plugin.getPlayerDataManager();
            for (UUID uuid : participants.keySet()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) dataManager.recordBossClear(p, boss.getBossId());
            }
        }

        // 3. 5초 후 최종 귀환 및 정리를 시작합니다.
        broadcastMessage("§e5초 후 원래 위치로 돌아갑니다...");
        new BukkitRunnable() {
            @Override
            public void run() {
                // a. 플레이어 퇴장 처리 (스탯 원상복구, 최종 귀환)
                for (UUID uuid : participants.keySet()) {
                    Player p = plugin.getServer().getPlayer(uuid);
                    if (p != null) {
                        plugin.getAttributeListener().removeRaidAttributes(p);
                        p.setGameMode(GameMode.SURVIVAL);
                        TeleportUtil.returnPlayerToSafety(p, participants.get(uuid));
                    }
                }

                // b. 클리어 시에만 파티 자동 해산
                if (isClear) {
                    Player anyMember = getOnlinePlayers().stream().findFirst().orElse(null);
                    Party party = (anyMember != null) ? PartyManager.getParty(anyMember) : null;
                    if (party != null) {
                        PartyManager.disbandParty(party.getLeader());
                    }
                }

                // c. 보스 및 월드 정리
                if (boss != null) {
                    boss.cleanup();
                }

                raidManager.endRaid(RaidInstance.this);
            }
        }.runTaskLater(plugin, 100L); // 5초 (20틱 * 5)
    }

    // --- 유틸리티 및 Getter 메소드 ---

    /**
     * [신규] 특정 위치에서 가장 가까운 플레이어를 찾습니다.
     * @param location 기준 위치
     * @return 가장 가까운 플레이어 Optional
     */
    public Optional<Player> getClosestPlayer(Location location) {
        return getOnlinePlayers().stream()
                .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(location)));
    }

    /**
     * [신규] 특정 위치에서 가장 먼 플레이어를 찾습니다.
     * @param location 기준 위치
     * @return 가장 먼 플레이어 Optional
     */
    public Optional<Player> getFarthestPlayer(Location location) {
        return getOnlinePlayers().stream()
                .max(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(location)));
    }

    public Location getRespawnLocation() {
        return new Location(raidWorld, 0.5, 65, 0.5);
    }
    public int getDeathCount(Player player) {
        return deathCounts.getOrDefault(player.getUniqueId(), 0);
    }
    public Boss getBoss() {
        return this.boss;
    }
    public World getRaidWorld() {
        return this.raidWorld;
    }
    public int getTimeElapsed() {
        return this.timeElapsed;
    }
    public int getInitialPartySize() {
        return this.initialPartySize;
    }
    public void broadcastMessage(String message) {
        getOnlinePlayers().forEach(p -> p.sendMessage(message));
    }
    public boolean hasPlayer(UUID uuid) {
        return participants.containsKey(uuid);
    }
    public List<Player> getOnlinePlayers() {
        return participants.keySet().stream()
                .map(plugin.getServer()::getPlayer)
                .filter(p -> p != null && p.isOnline()) // 관전자는 제외
                .collect(Collectors.toList());
    }

    public void registerRaidEntity(UUID entityId, Boss bossObject) {
        raidEntities.put(entityId, bossObject);
        plugin.getRaidManager().registerBossEntity(entityId, this);
    }

    public void unregisterRaidEntity(UUID entityId) {
        raidEntities.remove(entityId);
        plugin.getRaidManager().unregisterBossEntity(entityId);
    }

    public boolean isFinished() {
        return this.currentState == RaidState.FINISHED;
    }

    /**
     * [신규] 이 인스턴스를 관리하는 메인 플러그인 객체를 반환합니다.
     * @return Rpg 플러그인 인스턴스
     */
    public Rpg getPlugin() {
        return this.plugin;
    }

    /**
     * [신규] 특정 위치에서 가장 가까운 NMS 플레이어(EntityPlayer)를 찾습니다.
     * @param location 기준 위치
     * @return 가장 가까운 NMS 플레이어 Optional
     */
    public Optional<net.minecraft.world.entity.player.Player> getClosestNmsPlayer(Location location) {
        return getOnlinePlayers().stream()
                .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(location)))
                .map(p -> ((CraftPlayer) p).getHandle());
    }

    /**
     * [신규] 특정 위치에서 가장 먼 NMS 플레이어(EntityPlayer)를 찾습니다.
     * @param location 기준 위치
     * @return 가장 먼 NMS 플레이어 Optional
     */
    public Optional<net.minecraft.world.entity.player.Player> getFarthestNmsPlayer(Location location) {
        return getOnlinePlayers().stream()
                .max(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(location)))
                .map(p -> ((CraftPlayer) p).getHandle());
    }
}