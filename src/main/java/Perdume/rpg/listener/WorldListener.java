package Perdume.rpg.listener;

import Perdume.rpg.Rpg;
import Perdume.rpg.config.LocationManager;
import Perdume.rpg.core.player.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Map;

public class WorldListener implements Listener {
    private final Rpg plugin;
    public WorldListener(Rpg plugin) { this.plugin = plugin; }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        // 1초마다 한번씩만 체크하여 부하 감소
        if (player.getWorld().getTime() % 20 != 0) return;

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);

        // 모든 스폰 지점을 순회하며
        for (Map.Entry<String, Location> entry : LocationManager.getSpawnLocations().entrySet()) {
            String spawnId = entry.getKey();
            // 아직 해금하지 않은 스폰 지점이고,
            if (!data.hasUnlockedSpawn(spawnId)) {
                Location spawnLoc = entry.getValue();
                // 해당 스폰 지점의 20블록 반경 안에 들어왔다면
                if (player.getWorld().equals(spawnLoc.getWorld()) && player.getLocation().distanceSquared(spawnLoc) < 400) {
                    // 해금!
                    data.unlockSpawn(spawnId);
                    player.sendMessage("§a[알림] §f새로운 스폰 지점 §e'" + spawnId + "'§f을(를) 발견했습니다!");
                }
            }
        }
    }
}