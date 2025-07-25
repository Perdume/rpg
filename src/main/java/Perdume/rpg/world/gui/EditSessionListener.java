package Perdume.rpg.world.gui;


import Perdume.rpg.Rpg;
import Perdume.rpg.world.command.WorldAdminCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class EditSessionListener implements Listener {

    private final Rpg plugin;
    private final WorldAdminCommand worldAdminCommand; // [수정] WorldAdminCommand를 참조

    public EditSessionListener(Rpg plugin, WorldAdminCommand worldAdminCommand) {
        this.plugin = plugin;
        this.worldAdminCommand = worldAdminCommand;
    }

    /**
     * 플레이어가 서버에서 나갈 때, 수정 중인 맵이 있는지 확인하고 자동으로 저장합니다.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // [수정] WorldAdminCommand의 editingPlayers 맵을 참조
        if (WorldAdminCommand.editingPlayers.containsKey(player.getUniqueId())) {
            Rpg.log.info(player.getName() + "님이 맵 수정 중 접속을 종료하여, 작업을 자동으로 저장합니다.");
            
            // [수정] WorldAdminCommand의 저장 메소드를 호출하여 자동 저장 실행
            // [핵심 수정] 두 번째 인자로, 저장 후 실행될 간단한 콜백(결과 로그 출력)을 추가합니다.
            worldAdminCommand.handleSave(player, (success) -> {
                if (success) {
                    Rpg.log.info(player.getName() + "님의 맵 수정 작업이 성공적으로 자동 저장되었습니다.");
                } else {
                    Rpg.log.severe(player.getName() + "님의 맵 수정 작업 자동 저장에 실패했습니다.");
                }
            });
        }
    }
}