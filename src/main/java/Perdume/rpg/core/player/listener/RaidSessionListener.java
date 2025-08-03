package Perdume.rpg.core.player.listener;

import Perdume.rpg.Rpg;
import Perdume.rpg.core.util.TeleportUtil;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class RaidSessionListener implements Listener {
    private final Rpg plugin;

    public RaidSessionListener(Rpg plugin) {
        this.plugin = plugin;
    }

    /**
     * 레이드 중인 플레이어가 죽을 만큼의 대미지를 입었을 때, 죽음 대신 '부활 대기' 상태로 만듭니다.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerLethalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        plugin.getRaidManager().findRaidByPlayer(player).ifPresent(raidInstance -> {
            if (raidInstance.isPlayerReviving(player)) {
                event.setCancelled(true);
                return;
            }
            if (player.getHealth() - event.getFinalDamage() <= 0) {
                if (raidInstance.getDeathCount(player) > 0) {
                    event.setCancelled(true);
                    raidInstance.startRevivalSequence(player);
                }
            }
        });
    }

    /**
     * 레이드에 참여 중인 플레이어가 (데스카운트 0으로) 진짜 죽었을 때 처리합니다.
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        plugin.getRaidManager().findRaidByPlayer(player).ifPresent(raidInstance -> {
            event.setKeepInventory(true);
            event.getDrops().clear();
            event.setDroppedExp(0);
            event.setDeathMessage("§e[RAID] §f" + player.getName() + " 이(가) 죽었습니다.");

            // [핵심 3] RaidInstance에 최종 사망 처리를 위임합니다.
            raidInstance.handleFinalPlayerDeath(player);
        });
    }

    /**
     * [핵심] 레이드 중인 플레이어가 부활할 때의 위치를 지정합니다.
     */
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        plugin.getRaidManager().findRaidByPlayer(player).ifPresent(raidInstance -> {
            // 부활 위치를 해당 레이드 월드의 지정된 스폰 장소로 설정합니다.
            event.setRespawnLocation(raidInstance.getRespawnLocation());
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getRaidManager().findRaidByPlayer(player).ifPresent(raidInstance -> {
            // RaidInstance에 이탈 페널티 처리 위임
            raidInstance.handlePlayerQuitPenalty(player);
        });
    }
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // RaidManager를 통해 해당 플레이어가 '이탈했던' 레이드를 찾음
        plugin.getRaidManager().findRaidByPlayer(player).ifPresent(raidInstance -> {
            // 이 플레이어는 이제 레이드 멤버가 아니지만, RaidInstance에는 귀환 위치 정보가 남아있음
            player.sendMessage("§c레이드 도중 이탈하여, 원래 위치로 귀환합니다.");

            // 즉시 원래 위치로 귀환
            plugin.getAttributeListener().removeRaidAttributes(player);

            player.setGameMode(GameMode.SURVIVAL);
            TeleportUtil.returnPlayerToSafety(player, raidInstance.getOriginalLocation(player));
        });
    }
}