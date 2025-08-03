package Perdume.rpg.gamemode.island;

import Perdume.rpg.Rpg;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 모든 섬의 영구 데이터를 파일(.yml)로 저장하고 불러오는 클래스입니다.
 */
public class IslandDataManager {

    private final Rpg plugin;
    private final File dataFolder;

    public IslandDataManager(Rpg plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "island_data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    /**
     * Island 객체의 모든 데이터를 파일에 저장합니다.
     * @param island 저장할 Island 객체
     */
    public void saveIsland(Island island) {
        if (island == null) return;
        File islandFile = new File(dataFolder, island.getId() + ".yml");
        FileConfiguration config = new YamlConfiguration();

        // 섬의 핵심 데이터 저장
        config.set("owner", island.getOwner().toString());
        // UUID 리스트를 String 리스트로 변환하여 저장
        config.set("members", island.getMembers().stream().map(UUID::toString).collect(Collectors.toList()));
        // TODO: 섬 레벨, 스폰 위치 등 다른 데이터도 여기에 저장

        try {
            config.save(islandFile);
        } catch (IOException e) {
            Rpg.log.severe("섬 데이터 저장 실패: " + island.getId());
            e.printStackTrace();
        }
    }

    /**
     * 파일에서 섬 ID를 기반으로 Island 객체를 불러옵니다.
     * @param islandId 불러올 섬의 고유 ID
     * @return 복원된 Island 객체, 파일이 없으면 null
     */
    public Island loadIsland(String islandId) {
        File islandFile = new File(dataFolder, islandId + ".yml");
        if (!islandFile.exists()) {
            return null;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(islandFile);

        try {
            UUID owner = UUID.fromString(config.getString("owner"));
            List<UUID> members = config.getStringList("members").stream()
                                     .map(UUID::fromString)
                                     .collect(Collectors.toList());

            return new Island(islandId, owner, members);
        } catch (Exception e) {
            Rpg.log.severe("섬 데이터 불러오기 실패: " + islandId);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 섬 데이터를 삭제합니다.
     * @param islandId 삭제할 섬의 ID
     */
    public void deleteIslandData(String islandId) {
        File islandFile = new File(dataFolder, islandId + ".yml");
        if (islandFile.exists()) {
            islandFile.delete();
        }
    }
}