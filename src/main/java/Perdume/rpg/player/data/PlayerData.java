package Perdume.rpg.player.data;

import java.util.HashMap;
import java.util.Map;

public class PlayerData {

    // 보스 ID와 마지막 클리어 시간(Timestamp)을 저장하는 맵
    private final Map<String, Long> bossClearTimestamps = new HashMap<>();

    public long getLastClearTime(String bossId) {
        return bossClearTimestamps.getOrDefault(bossId, 0L);
    }

    public void setLastClearTime(String bossId, long timestamp) {
        bossClearTimestamps.put(bossId, timestamp);
    }

    public Map<String, Long> getBossClearTimestamps() {
        return bossClearTimestamps;
    }
}