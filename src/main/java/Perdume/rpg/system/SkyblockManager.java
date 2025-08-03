package Perdume.rpg.system; // 사용하시는 패키지 경로

import Perdume.rpg.Rpg;
import Perdume.rpg.core.player.data.PlayerDataManager;
import Perdume.rpg.gamemode.island.Island;

import Perdume.rpg.world.WorldManager;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SkyblockManager {

    private final Rpg plugin;
    // 현재 메모리에 로드된 섬들의 목록 (ID를 키로 사용)
    private final Map<String, Island> activeIslands = new HashMap<>();
    private final PlayerDataManager playerDataManager;

    public SkyblockManager(Rpg plugin) {
        this.plugin = plugin;
        this.playerDataManager = plugin.getPlayerDataManager();
    }

    /**
     * 플레이어를 자신의 섬으로 이동시킵니다. 섬이 언로드 상태라면 먼저 로드합니다.
     * @param player 대상 플레이어
     */
    public void enterMyIsland(Player player) {
        String islandId = playerDataManager.getPlayerData(player).getIslandId();
        if (islandId == null || islandId.isEmpty()) {
            player.sendMessage("§c아직 당신의 섬이 없습니다. /섬 생성 명령어로 만들어주세요.");
            return;
        }

        Island island = activeIslands.get(islandId);
        if (island != null && island.getWorld() != null) {
            // 이미 로드된 경우
            player.teleport(island.getWorld().getSpawnLocation());
        } else {
            // 언로드 상태인 경우, 로드 후 텔레포트
            player.sendMessage("§e당신의 섬을 불러오는 중입니다...");
            loadIsland(islandId, (loadedIsland) -> {
                if (loadedIsland != null) {
                    player.teleport(loadedIsland.getWorld().getSpawnLocation());
                } else {
                    player.sendMessage("§c섬을 불러오는 데 실패했습니다.");
                }
            });
        }
    }

    /**
     * [핵심] 새로운 섬을 생성하고, 생성 과정을 플레이어에게 안내합니다.
     * @param player 섬을 생성할 플레이어
     */
    public void createIsland(Player player) {
        if (playerDataManager.getPlayerData(player).hasIsland()) {
            player.sendMessage("§c이미 섬을 소유하고 있습니다.");
            return;
        }

        // 1. 새로운 Island 객체를 생성하여 고유 ID를 부여합니다.
        Island newIsland = new Island(player);
        activeIslands.put(newIsland.getId(), newIsland);
        playerDataManager.getPlayerData(player).setIslandId(newIsland.getId());

        player.sendMessage("§a당신만의 새로운 섬을 생성하는 중입니다. 잠시만 기다려주세요...");

        // 2. WorldManager에게 "island" 타입의 "island_template"을 복사하여 월드를 생성하도록 요청합니다.
        WorldManager.copyAndLoadWorld(newIsland.getWorldName(), "island_template", "island", (world) -> {
            if (world != null) {
                // 3. 월드 생성이 성공하면, Island 객체에 월드 정보를 저장하고 플레이어를 텔레포트시킵니다.
                newIsland.setWorld(world);
                player.teleport(world.getSpawnLocation());
                player.sendMessage("§a하늘에 떠 있는 당신의 섬이 생성되었습니다! §e/섬 §a명령어로 언제든지 돌아올 수 있습니다.");
            } else {
                player.sendMessage("§c섬 생성에 실패했습니다. 관리자에게 문의해주세요.");
                // 4. 실패 시, 생성했던 모든 데이터를 되돌립니다 (롤백).
                activeIslands.remove(newIsland.getId());
                playerDataManager.getPlayerData(player).setIslandId(null);
            }
        });
    }

    /**
     * 파일에서 섬 데이터를 로드합니다. (TODO)
     */
    public void loadIsland(String islandId, Consumer<Island> onLoaded) {
        // TODO: island_data 폴더에서 islandId.yml 파일을 읽어 Island 객체를 복원하는 로직
        // Island island = ...
        // activeIslands.put(islandId, island);
        // WorldManager.copyAndLoadWorld(...)
        // onLoaded.accept(island);
    }

    /**
     * 섬 데이터를 파일에 저장하고 언로드합니다. (TODO)
     */
    public void unloadIsland(String islandId) {
        Island island = activeIslands.remove(islandId);
        if (island != null && island.getWorld() != null) {
            // TODO: 월드의 변경사항을 추출하여 islandId.yml 파일에 저장하는 로직
            // WorldManager.unloadAnddeleteWorld(island.getWorldName());
        }
    }
}