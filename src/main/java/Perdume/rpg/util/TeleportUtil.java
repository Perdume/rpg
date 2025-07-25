package Perdume.rpg.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class TeleportUtil {

    /**
     * 플레이어를 안전한 귀환 지점으로 텔레포트시킵니다.
     * 우선순위: 1. 원래 있던 위치 -> 2. 마지막으로 잔 침대 -> 3. 서버 기본 스폰
     * @param player 텔레포트시킬 플레이어
     * @param originalLocation 플레이어가 원래 있던 위치 (null 가능)
     */
    public static void returnPlayerToSafety(Player player, Location originalLocation) {
        if (player == null || !player.isOnline()) return;

        // 1순위: 원래 있던 위치가 유효한 경우
        if (originalLocation != null) {
            player.teleport(originalLocation);
            return;
        }

        // 2순위: 마지막으로 잔 침대 위치가 유효한 경우
        Location bedLocation = player.getBedSpawnLocation();
        if (bedLocation != null) {
            player.teleport(bedLocation);
            player.sendMessage("§e[알림] 원래 위치를 찾을 수 없어, 마지막으로 이용한 침대로 돌아왔습니다.");
            return;
        }

        // 3순위: 위 두 가지 모두 실패했을 경우, 서버의 기본 스폰 지점으로 이동
        Location fallbackLocation = Bukkit.getWorlds().get(0).getSpawnLocation();
        player.teleport(fallbackLocation);
        player.sendMessage("§c[알림] 귀환할 위치를 찾을 수 없어, 서버의 기본 스폰 지점으로 이동했습니다.");
    }
}