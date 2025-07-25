package Perdume.rpg.reward.listener;


import Perdume.rpg.Rpg;
import Perdume.rpg.reward.gui.RewardClaimGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class RewardClaimListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(RewardClaimGUI.GUI_TITLE)) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR || clickedItem.getType() == Material.BARRIER) return;

        // 인벤토리에 공간이 있는지 확인
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage("§c인벤토리에 공간이 부족하여 아이템을 수령할 수 없습니다.");
            return;
        }

        // 보상 수령
        player.getInventory().addItem(clickedItem.clone());
        Rpg.getInstance().getRewardManager().claimReward(player, clickedItem);
        player.sendMessage("§a[알림] §f" + clickedItem.getItemMeta().getDisplayName() + " §f" + clickedItem.getAmount() + "개를 수령했습니다.");

        // GUI 새로고침
        RewardClaimGUI.open(player);
    }
}