package Perdume.rpg.core.player.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 한 명의 플레이어에 대한 모든 RPG 데이터를 저장하는 클래스입니다.
 * 이 객체는 PlayerDataManager에 의해 관리됩니다.
 */
public class PlayerData {

    // 플레이어가 소유하거나 소속된 섬의 고유 ID
    private String islandId;

    // 보스 ID와 마지막 클리어 시간(Timestamp)을 저장하는 맵
    private final Map<String, Long> bossClearTimestamps = new HashMap<>();

    // --- 섬 관련 데이터 ---

    public String getIslandId() {
        return islandId;
    }

    public void setIslandId(String islandId) {
        this.islandId = islandId;
    }

    public boolean hasIsland() {
        return this.islandId != null && !this.islandId.isEmpty();
    }


    // --- 레이드 클리어 관련 데이터 ---

    public long getLastClearTime(String bossId) {
        return bossClearTimestamps.getOrDefault(bossId, 0L);
    }

    public void setLastClearTime(String bossId, long timestamp) {
        bossClearTimestamps.put(bossId, timestamp);
    }

    public Map<String, Long> getBossClearTimestamps() {
        return bossClearTimestamps;
    }

    // [신규] 플레이어가 해금한 스폰 지점의 ID 목록
    private final List<String> unlockedSpawns = new ArrayList<>();

    // --- 스폰 관련 데이터 ---
    public List<String> getUnlockedSpawns() {
        return unlockedSpawns;
    }

    public void unlockSpawn(String spawnId) {
        if (!unlockedSpawns.contains(spawnId)) {
            unlockedSpawns.add(spawnId);
        }
    }

    public boolean hasUnlockedSpawn(String spawnId) {
        return unlockedSpawns.contains(spawnId);
    }
}