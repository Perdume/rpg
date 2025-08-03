package Perdume.rpg.gamemode.island;

import Perdume.rpg.Rpg;
import Perdume.rpg.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

/**
 * 하나의 스카이블럭 섬 인스턴스를 관리하는 클래스입니다.
 * 이 객체는 SkyblockManager에 의해 관리됩니다.
 */
public class IslandInstance {

    private final Rpg plugin;
    private final Island islandData; // 섬의 모든 영구 데이터를 담는 객체
    private World world; // 현재 메모리에 로드된 월드 객체

    public IslandInstance(Rpg plugin, Island islandData) {
        this.plugin = plugin;
        this.islandData = islandData;
    }

    /**
     * 섬 월드를 메모리에 로드합니다.
     * @param onLoaded 월드 로드가 완료된 후 실행될 작업
     */
    public void load(Consumer<Boolean> onLoaded) {
        // 이미 로드되어 있다면 즉시 성공을 알림
        if (isLoaded()) {
            onLoaded.accept(true);
            return;
        }

        // WorldManager에게 "island" 타입의 템플릿을 복사하여 월드를 생성하도록 요청합니다.
        // 이때, 템플릿 이름은 각 섬의 데이터 파일이 됩니다. (영구 저장)
        WorldManager.copyAndLoadWorld(islandData.getWorldName(), islandData.getTemplateFolderName(), "island", (loadedWorld) -> {
            if (loadedWorld != null) {
                this.world = loadedWorld;
                // TODO: 섬 보호 플러그인(WorldGuard 등) 연동하여 섬 멤버 외의 접근을 막는 로직 추가
                onLoaded.accept(true);
            } else {
                Rpg.log.severe(islandData.getOwner() + "의 섬 월드 로드에 실패했습니다.");
                onLoaded.accept(false);
            }
        });
    }

    /**
     * 섬 월드의 변경사항을 영구적으로 저장하고 메모리에서 언로드합니다.
     * @param onUnloaded 작업 완료 후 실행할 내용
     */
    public void unload(Consumer<Boolean> onUnloaded) {
        if (!isLoaded()) {
            onUnloaded.accept(true);
            return;
        }
        
        // WorldManager를 통해 현재 월드의 상태를 템플릿 폴더에 덮어씌워 저장합니다.
        WorldManager.saveAndOverwriteTemplate(this.world, islandData.getTemplateFolderName(), "island", (success) -> {
            this.world = null; // 메모리에서 월드 객체 참조 제거
            onUnloaded.accept(success);
        });
    }

    /**
     * 플레이어를 섬의 스폰 위치로 텔레포트시킵니다.
     * @param player 대상 플레이어
     */
    public void teleport(Player player) {
        if (isLoaded()) {
            // TODO: 나중에 /섬 스폰설정 같은 명령어를 위해 스폰 위치를 Island 데이터에 저장하도록 수정
            player.teleport(new Location(this.world, 0.5, 65, 0.5));
        } else {
            // 이 경우는 SkyblockManager에서 처리하므로, 여기서는 로그만 남깁니다.
            Rpg.log.warning(player.getName() + "을(를) 로드되지 않은 섬(" + islandData.getId() + ")으로 텔레포트하려고 시도했습니다.");
        }
    }

    public boolean isLoaded() {
        return this.world != null && Bukkit.getWorld(islandData.getWorldName()) != null;
    }

    public Island getIslandData() {
        return islandData;
    }
}