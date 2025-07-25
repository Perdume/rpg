package Perdume.rpg.world.task;


import Perdume.rpg.Rpg;
import Perdume.rpg.world.command.WorldAdminCommand;
import Perdume.rpg.world.manager.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;
import java.util.UUID;

public class EditWorldCleanupTask extends BukkitRunnable {

    private final Rpg plugin;
    private final WorldAdminCommand worldAdminCommand; // [수정] WorldGUIListener 대신 WorldAdminCommand를 참조

    public EditWorldCleanupTask(Rpg plugin, WorldAdminCommand worldAdminCommand) {
        this.plugin = plugin;
        this.worldAdminCommand = worldAdminCommand;
    }

    @Override
    public void run() {
        // [수정] WorldAdminCommand의 editingPlayers 맵을 참조
        if (WorldAdminCommand.editingPlayers.isEmpty()) {
            return;
        }

        // 현재 수정 중인 세션의 복사본을 만들어 순회 (원본을 직접 수정하는 도중의 오류 방지)
        for (UUID playerUuid : Set.copyOf(WorldAdminCommand.editingPlayers.keySet())) {
            WorldAdminCommand.EditSession session = WorldAdminCommand.editingPlayers.get(playerUuid);
            if (session == null) continue;

            World editWorld = Bukkit.getWorld(session.worldName());

            // 월드가 존재하는데, 그 안에 플레이어가 아무도 없다면?
            if (editWorld != null && editWorld.getPlayers().isEmpty()) {
                Player owner = Bukkit.getPlayer(playerUuid);

                // 플레이어가 온라인 상태인데 월드에만 없는 경우 (예: /spawn으로 이동)
                if (owner != null && owner.isOnline()) {
                    Rpg.log.info("수정 월드(" + session.worldName() + ")에 플레이어가 없어 자동으로 저장합니다.");
                    // [수정] WorldAdminCommand의 저장 메소드를 호출하여 자동 저장 실행
                    worldAdminCommand.handleSave(owner, (success) -> {});
                } else { // 플레이어가 오프라인 상태인 경우
                    Rpg.log.warning("수정 월드의 오프라인 주인(" + playerUuid + ")을 발견하여 월드를 강제 정리합니다.");
                    WorldManager.unloadAnddeleteWorld(session.worldName());
                    // [수정] WorldAdminCommand의 editingPlayers 맵에서 제거
                    WorldAdminCommand.editingPlayers.remove(playerUuid);
                }
            }
        }
    }
}