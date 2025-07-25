package Perdume.rpg;


import Perdume.rpg.party.PartyCommand;
import Perdume.rpg.player.data.PlayerDataManager;
import Perdume.rpg.player.listener.CombatListener;
import Perdume.rpg.raid.RaidCommand;
import Perdume.rpg.command.SetReinforceCommand;
import Perdume.rpg.command.TestCommand;
import Perdume.rpg.enhancement.command.ReinforceCommand;
import Perdume.rpg.enhancement.listener.ReinforceListener;
import Perdume.rpg.player.listener.AttributeListener;
import Perdume.rpg.player.listener.CraftingListener;
import Perdume.rpg.player.listener.RaidSessionListener;
import Perdume.rpg.raid.boss.BossFactory;
import Perdume.rpg.raid.listener.BossDeathListener;
import Perdume.rpg.raid.listener.RaidGUIListener;
import Perdume.rpg.reward.RewardCommand;
import Perdume.rpg.reward.listener.RewardClaimListener;
import Perdume.rpg.reward.manager.RewardManager;
import Perdume.rpg.system.RaidManager;
import Perdume.rpg.world.command.WorldAdminCommand;
import Perdume.rpg.world.gui.EditSessionListener;
import Perdume.rpg.world.manager.WorldManager;
import Perdume.rpg.world.task.EditWorldCleanupTask;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

public final class Rpg extends JavaPlugin implements Listener {

    public static final List<String> BOSS_LIST = List.of(
            "GolemKing",
            "FireDragon" // 나중에 새로운 보스를 추가할 때 이 리스트에 ID만 추가하면 됩니다.
    );

    private static Rpg instance;
    public static Logger log;

    // --- 핵심 시스템 ---
    private RaidManager raidManager;
    private AttributeListener attributeListener;
    private CombatListener combatListener;
    private RewardManager rewardManager;
    private PlayerDataManager playerDataManager;

    public static Economy econ = null;

    @Override
    public void onEnable() {
        instance = this;
        log = this.getLogger();

        // 1. 설정 파일 로드 및 템플릿 월드 준비 (가장 먼저)
        this.saveDefaultConfig();
        initializeTemplateWorlds();

        // 2. 외부 API 연동 (Vault)
        if (!setupEconomy()) {
            log.severe("Vault 플러그인이 없어 비활성화됩니다!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.playerDataManager = new PlayerDataManager(this);
        getServer().getPluginManager().registerEvents(this, this);

        // 3. 내부 핵심 시스템 초기화 (리스너보다 먼저!)
        initializeSystems();

        // 4. 모든 명령어 및 리스너 등록
        registerCommandsAndSystems();

        log.info("RPG Plugin이 성공적으로 활성화되었습니다.");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            for (Player player : getServer().getOnlinePlayers()) {
                playerDataManager.savePlayerDataOnQuit(player);
            }
        }
        log.info("RPG Plugin이 비활성화되었습니다.");
    }

    // --- 플레이어 데이터 관리 이벤트 ---
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        playerDataManager.loadPlayerDataOnJoin(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerDataManager.savePlayerDataOnQuit(event.getPlayer());
    }

    // Getter 추가
    public PlayerDataManager getPlayerDataManager() {
        return this.playerDataManager;
    }

    private void initializeTemplateWorlds() {
        log.info("레이드 템플릿 월드 초기화를 시작합니다...");
        for (String bossId : BOSS_LIST) {
            if (BossFactory.isValidBossId(bossId, this)) {
                if (WorldManager.createVoidTemplate(bossId)) {
                    log.info("- '" + bossId + "' 템플릿 월드 확인/생성 완료.");
                } else {
                    log.severe("- '" + bossId + "' 템플릿 월드 생성 실패!");
                }
            } else {
                log.warning("- 보스 ID '" + bossId + "'는 존재하지만, BossFactory에 등록되지 않았습니다.");
            }
        }
        log.info("레이드 템플릿 월드 초기화 완료.");
    }

    private void initializeSystems() {
        this.raidManager = new RaidManager(this);
        this.attributeListener = new AttributeListener(this);
        this.combatListener = new CombatListener(this);
        this.rewardManager = new RewardManager(this);
    }

    private void cleanupTemporaryWorlds() {
        log.info("임시 월드 청소를 시작합니다...");
        File worldContainer = getServer().getWorldContainer();
        if (worldContainer == null || !worldContainer.isDirectory()) return;

        File[] worldFolders = worldContainer.listFiles();
        if (worldFolders == null) return;

        int count = 0;
        for (File worldFolder : worldFolders) {
            String folderName = worldFolder.getName();
            if (worldFolder.isDirectory() && (folderName.startsWith("Raid--RUN--") || folderName.startsWith("Raid--EDIT--"))) {
                if (getServer().getWorld(folderName) != null) {
                    getServer().unloadWorld(folderName, false);
                }
                WorldManager.deleteWorldFolder(worldFolder);
                count++;
            }
        }
        if (count > 0) {
            log.info(count + "개의 임시 월드를 성공적으로 삭제했습니다.");
        }
    }
    /**
     * 플러그인의 모든 명령어 실행기와 이벤트 리스너를 서버에 등록합니다.
     * 각 시스템별로 구역을 나누어 가독성과 유지보수성을 높였습니다.
     */
    private void registerCommandsAndSystems() {
        // --- 강화 시스템 ---
        getCommand("강화").setExecutor(new ReinforceCommand());
        getCommand("setreinforce").setExecutor(new SetReinforceCommand());
        getServer().getPluginManager().registerEvents(new ReinforceListener(this), this);

        // --- 파티 및 레이드 시스템 ---
        getCommand("party").setExecutor(new PartyCommand());
        getCommand("raid").setExecutor(new RaidCommand(this));
        getServer().getPluginManager().registerEvents(new RaidGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new RaidSessionListener(this), this);
        getServer().getPluginManager().registerEvents(new BossDeathListener(this), this);

        // --- 보상 시스템 ---
        getCommand("보상").setExecutor(new RewardCommand());
        getServer().getPluginManager().registerEvents(new RewardClaimListener(), this);

        // --- 월드 관리 시스템 ---
        WorldAdminCommand worldAdminCommand = new WorldAdminCommand(this);
        getCommand("rpworld").setExecutor(worldAdminCommand);
        getCommand("rpworld").setTabCompleter(worldAdminCommand);
        getServer().getPluginManager().registerEvents(new EditSessionListener(this, worldAdminCommand), this);
        new EditWorldCleanupTask(this, worldAdminCommand).runTaskTimer(this, 0L, 6000L);

        // --- 테스트 시스템 ---
        getCommand("rpgtest").setExecutor(new TestCommand(this));

        // --- 핵심 리스너 등록 ---
        getServer().getPluginManager().registerEvents(this.attributeListener, this);
        getServer().getPluginManager().registerEvents(this.combatListener, this);
        getServer().getPluginManager().registerEvents(new CraftingListener(), this);
        // getServer().getPluginManager().registerEvents(new GlobalRespawnListener(this), this); // 필요 시 활성화
    }
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        return econ != null;
    }

    // --- Getter 메소드 ---
    public static Rpg getInstance() { return instance; }
    public RaidManager getRaidManager() { return raidManager; }
    public AttributeListener getAttributeListener() { return attributeListener; }
    public CombatListener getCombatListener() {return combatListener;}
    public RewardManager getRewardManager() {return rewardManager;}
}
