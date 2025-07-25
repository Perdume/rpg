package Perdume.rpg.raid.listener;

import Perdume.rpg.Rpg;
import Perdume.rpg.raid.ai.EntityGolemKing;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class BossDeathListener implements Listener {
    private final Rpg plugin;

    public BossDeathListener(Rpg plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity deadEntity = event.getEntity();

        // [핵심] 죽은 엔티티가 우리 시스템에 등록된 '메인 보스'인지 확인
        plugin.getRaidManager().findRaidByEntityId(deadEntity.getUniqueId()).ifPresent(raidInstance -> {
            // 이 죽음이 '메인 보스'의 죽음인지 한번 더 확인
            if (raidInstance.getBoss().getBukkitEntity().isPresent() &&
                    raidInstance.getBoss().getBukkitEntity().get().equals(deadEntity))
            {
                // 레이드를 '클리어' 상태로 종료시킴
                raidInstance.end(true);
            }
        });
    }
}