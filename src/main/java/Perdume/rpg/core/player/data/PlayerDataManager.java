package Perdume.rpg.core.player.data;

import Perdume.rpg.Rpg;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.spigotmc.SpigotConfig.config;

public class PlayerDataManager {
    private final Rpg plugin;
    // 동시 접속/종료 시에도 안전하도록 ConcurrentHashMap 사용
    private final Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();
    private final File dataFolder;

    public PlayerDataManager(Rpg plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    /**
     * 플레이어의 데이터를 가져옵니다. 메모리에 없으면 파일에서 로드합니다.
     * @param player 대상 플레이어
     * @return 해당 플레이어의 PlayerData 객체
     */
    public PlayerData getPlayerData(Player player) {
        return playerDataMap.computeIfAbsent(player.getUniqueId(), this::loadPlayerData);
    }

    /**
     * 플레이어가 서버에 접속할 때 데이터를 미리 로드합니다.
     */
    public void loadPlayerDataOnJoin(Player player) {
        getPlayerData(player);
    }

    /**
     * 플레이어가 서버에서 나갈 때 데이터를 파일에 저장하고 메모리에서 제거합니다.
     */
    public void savePlayerDataOnQuit(Player player) {
        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data != null) {
            savePlayerData(player.getUniqueId(), data);
            playerDataMap.remove(player.getUniqueId());
        }
    }

    /**
     * 파일에서 특정 플레이어의 데이터를 읽어와 PlayerData 객체로 변환합니다.
     */
    private PlayerData loadPlayerData(UUID uuid) {
        File playerFile = new File(dataFolder, uuid + ".yml");
        PlayerData data = new PlayerData();
        if (playerFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);

            // 섬 ID 로드
            data.setIslandId(config.getString("island-id", null));

            // 보스 클리어 기록 로드 (항목이 없어도 오류나지 않도록 안전하게)
            ConfigurationSection clearSection = config.getConfigurationSection("boss-clears");
            if (clearSection != null) {
                clearSection.getKeys(false).forEach(bossId -> {
                    data.setLastClearTime(bossId, clearSection.getLong(bossId));
                });
            }
        }
        data.getUnlockedSpawns().addAll(config.getStringList("unlocked-spawns"));
        return data;
    }

    /**
     * PlayerData 객체를 .yml 파일로 저장합니다.
     */
    private void savePlayerData(UUID uuid, PlayerData data) {
        File playerFile = new File(dataFolder, uuid + ".yml");
        FileConfiguration config = new YamlConfiguration();

        // 섬 ID 저장
        config.set("island-id", data.getIslandId());
        config.set("unlocked-spawns", data.getUnlockedSpawns());

        // 보스 클리어 기록 저장
        if (!data.getBossClearTimestamps().isEmpty()) {
            for (Map.Entry<String, Long> entry : data.getBossClearTimestamps().entrySet()) {
                config.set("boss-clears." + entry.getKey(), entry.getValue());
            }
        }

        try {
            config.save(playerFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 플레이어가 특정 보스를 오늘 클리어할 수 있는지 확인합니다.
     */
    public boolean canClearBoss(Player player, String bossId) {
        long lastClear = getPlayerData(player).getLastClearTime(bossId);
        if (lastClear == 0) return true;
        // 한국 시간대(KST, UTC+9)를 기준으로 자정이 지났는지 확인
        long lastClearDay = (lastClear + 32400000) / 86400000;
        long today = (System.currentTimeMillis() + 32400000) / 86400000;
        return lastClearDay != today;
    }

    /**
     * 플레이어의 보스 클리어 기록을 현재 시간으로 저장합니다.
     */
    public void recordBossClear(Player player, String bossId) {
        getPlayerData(player).setLastClearTime(bossId, System.currentTimeMillis());
    }
}