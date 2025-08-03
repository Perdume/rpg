package Perdume.rpg.system;

import Perdume.rpg.Rpg;
import Perdume.rpg.gamemode.raid.RaidInstance;
import Perdume.rpg.gamemode.raid.boss.Boss;
import Perdume.rpg.gamemode.raid.boss.BossFactory;
import Perdume.rpg.world.WorldManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

public class RaidManager {


    private final Rpg plugin;
    private final List<RaidInstance> activeRaids = new ArrayList<>();
    // [핵심] NMS 엔티티의 UUID와 해당 레이드 인스턴스를 직접 연결하는 맵
    private final Map<UUID, RaidInstance> entityToRaidMap = new HashMap<>();
    private int nextRaidId = 1;

    public RaidManager(Rpg plugin) {
        this.plugin = plugin;
    }

    /**
     * 새로운 레이드를 생성하고 시작합니다.
     * @param players 참여할 플레이어 목록
     * @param bossId 생성할 보스의 고유 ID (이 ID가 템플릿 맵 이름이 됩니다)
     */
    public void createAndStartRaid(List<Player> players, String bossId) {
        String templateWorldName = bossId;
        File templateWorldFolder = new File(plugin.getDataFolder(), "worlds/" + templateWorldName);

        if (!templateWorldFolder.exists()) {
            Rpg.log.severe("치명적 오류: 레이드 템플릿 월드 '" + templateWorldName + "'를 찾을 수 없습니다!");
            players.forEach(p -> p.sendMessage("§c레이드 입장에 실패했습니다. (맵 정보 오류)"));
            return;
        }

        String instanceWorldName = "Raid--RUN--" + Integer.hashCode(nextRaidId++);

        WorldManager.copyAndLoadWorld(instanceWorldName, templateWorldName, "raid", (newWorld) -> {
            if (newWorld == null) {
                players.forEach(p -> p.sendMessage("§c레이드 입장에 실패했습니다. (월드 복사 오류)"));
                WorldManager.unloadAnddeleteWorld(instanceWorldName);
                return;
            }

            // [핵심 수정] BossFactory에 파티 인원 수(players.size())를 함께 전달합니다.
            Boss boss = BossFactory.createBoss(bossId, plugin, players.size());

            if (boss == null) {
                players.forEach(p -> p.sendMessage("§c알 수 없는 보스 ID입니다: " + bossId));
                WorldManager.unloadAnddeleteWorld(instanceWorldName);
                return;
            }

            RaidInstance raidInstance = new RaidInstance(plugin, this, players, newWorld, boss);
            activeRaids.add(raidInstance);
            raidInstance.start();
        });
    }

    /**
     * 보스 객체가 스스로를 등록하기 위해 호출하는 메소드입니다.
     * @param entityId 등록할 보스/수정/쫄몹 엔티티의 UUID
     * @param raidInstance 해당 엔티티가 속한 레이드 인스턴스
     */
    public void registerBossEntity(UUID entityId, RaidInstance raidInstance) {
        entityToRaidMap.put(entityId, raidInstance);
    }

    /**
     * [신규] RaidManager의 관리 목록에서 엔티티 등록을 해제합니다.
     * @param entityId 등록 해제할 엔티티의 UUID
     */
    public void unregisterBossEntity(UUID entityId) {
        entityToRaidMap.remove(entityId);
    }

    public void endRaid(RaidInstance raidInstance) {
        if (raidInstance == null) return;

        raidInstance.getBoss().getBukkitEntity().ifPresent(entity -> {
            entityToRaidMap.remove(entity.getUniqueId());
        });

        String worldNameToDelete = raidInstance.getRaidWorld().getName();
        activeRaids.remove(raidInstance);
        WorldManager.unloadAnddeleteWorld(worldNameToDelete);
    }

    /**
     * 특정 플레이어가 현재 레이드에 참여 중인지 확인합니다.
     * @param player 확인할 플레이어
     * @return 레이드 참여 중이면 true
     */
    public boolean isPlayerInRaid(Player player) {
        if (player == null) return false;
        return activeRaids.stream().anyMatch(raid -> raid.hasPlayer(player.getUniqueId()));
    }

    /**
     * [핵심] 엔티티의 UUID를 기반으로 레이드 인스턴스를 찾습니다.
     * @param entityId 찾을 엔티티의 UUID
     * @return 해당 엔티티가 보스인 RaidInstance (Optional)
     */
    public Optional<RaidInstance> findRaidByEntityId(UUID entityId) {
        return Optional.ofNullable(entityToRaidMap.get(entityId));
    }

    /**
     * 특정 플레이어가 참여 중인 레이드 인스턴스를 찾습니다.
     * @param player 찾을 플레이어
     * @return 해당 플레이어가 참여 중인 RaidInstance (Optional)
     */
    public Optional<RaidInstance> findRaidByPlayer(Player player) {
        if (player == null) return Optional.empty();
        return activeRaids.stream()
                .filter(raid -> raid.hasPlayer(player.getUniqueId()))
                .findFirst();
    }

    /**
     * 현재 진행 중인 레이드 목록의 수정 불가능한 복사본을 반환합니다.
     * (onDisable에서 안전하게 사용하기 위함)
     * @return 활성화된 레이드 목록
     */
    public List<RaidInstance> getActiveRaids() {
        return Collections.unmodifiableList(activeRaids);
    }


    /**
     * [신규] 특정 엔티티가 현재 활성화된 레이드 중 하나에라도 속해 있는지 확인합니다.
     * @param entity 확인할 엔티티
     * @return 레이드에 속해 있으면 true, 아니면 false
     */
    public boolean isEntityInRaid(Entity entity) {
        if (entity == null) {
            return false;
        }
        return entityToRaidMap.containsKey(entity.getUniqueId());
    }

}