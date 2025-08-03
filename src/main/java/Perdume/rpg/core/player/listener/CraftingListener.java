package Perdume.rpg.core.player.listener;


import Perdume.rpg.core.util.ItemFactory;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

public class CraftingListener implements Listener {

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory inventory = event.getInventory();
        // 제작 재료 목록을 가져옵니다.
        ItemStack[] ingredients = inventory.getMatrix();

        // [핵심] 재료들을 하나씩 확인합니다.
        for (ItemStack ingredient : ingredients) {
            // 재료 슬롯이 비어있지 않고, 해당 아이템이 우리가 만든 RPG 아이템이라면
            if (ingredient != null && ItemFactory.isRpgItem(ingredient)) {
                
                // 1. 제작 결과물 슬롯을 즉시 비워버려 제작을 막습니다.
                inventory.setResult(new ItemStack(Material.AIR));

                // 2. 제작을 시도한 플레이어에게 경고 메시지를 보냅니다.
                if (event.getView().getPlayer() instanceof Player player) {
                    player.sendMessage("§c[알림] 그 아이템은 특별한 아이템이라 제작 재료로 사용할 수 없습니다.");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1, 0.5f);
                    
                    // 플레이어의 인벤토리를 업데이트하여, 결과물 슬롯에 남아있을 수 있는
                    // 잘못된 아이템 '잔상'을 제거해주는 것이 좋습니다. (안정성)
                    player.updateInventory();
                }
                
                // RPG 아이템을 하나라도 발견했으면, 더 이상 검사할 필요 없이 즉시 종료
                return;
            }
        }
    }
}