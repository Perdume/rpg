package Perdume.rpg.player.data; // 사용하시는 패키지 경로

import Perdume.rpg.Rpg;
import org.bukkit.configuration.ConfigurationSection; // [핵심] ConfigurationSection import
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {
    private final Rpg plugin;
    private final Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();
    private final File dataFolder;

    public PlayerDataManager(Rpg plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public PlayerData getPlayerData(Player player) {
        return playerDataMap.computeIfAbsent(player.getUniqueId(), this::loadPlayerData);
    }

    public void loadPlayerDataOnJoin(Player player) {
        getPlayerData(player);
    }

    public void savePlayerDataOnQuit(Player player) {
        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data != null) {
            savePlayerData(player.getUniqueId(), data);
            playerDataMap.remove(player.getUniqueId());
        }
    }

    private PlayerData loadPlayerData(UUID uuid) {
        File playerFile = new File(dataFolder, uuid + ".yml");
        PlayerData data = new PlayerData();
        if (playerFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            
            // [핵심 수정] 'boss-clears' 섹션이 존재하는지 먼저 확인합니다.
            ConfigurationSection clearSection = config.getConfigurationSection("boss-clears");
            if (clearSection != null) {
                // 섹션이 존재할 때만 키를 읽어옵니다.
                clearSection.getKeys(false).forEach(bossId -> {
                    data.setLastClearTime(bossId, clearSection.getLong(bossId));
                });
            }
        }
        return data;
    }

    private void savePlayerData(UUID uuid, PlayerData data) {
        File playerFile = new File(dataFolder, uuid + ".yml");
        FileConfiguration config = new YamlConfiguration();
        // 저장할 데이터가 있을 때만 섹션을 만듭니다.
        if (data != null && !data.getBossClearTimestamps().isEmpty()) {
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

    public boolean canClearBoss(Player player, String bossId) {
        long lastClear = getPlayerData(player).getLastClearTime(bossId);
        if (lastClear == 0) return true;
        // 마지막 클리어 날짜와 오늘 날짜가 다른지 확인 (자정을 기준으로)
        // 86400000 = 24 * 60 * 60 * 1000 (하루의 밀리초)
        // 한국 시간대(KST)를 고려하여 9시간(32400000)을 빼줍니다.
        long lastClearDay = (lastClear - 32400000) / 86400000;
        long today = (System.currentTimeMillis() - 32400000) / 86400000;
        return lastClearDay != today;
    }

    public void recordBossClear(Player player, String bossId) {
        getPlayerData(player).setLastClearTime(bossId, System.currentTimeMillis());
    }
}