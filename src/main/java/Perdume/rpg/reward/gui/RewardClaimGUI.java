package Perdume.rpg.reward.gui;


import Perdume.rpg.Rpg;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class RewardClaimGUI {

    public static final String GUI_TITLE = "§8[보상 수령함]";

    public static void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE);
        List<ItemStack> rewards = Rpg.getInstance().getRewardManager().getRewards(player);

        if (rewards.isEmpty()) {
            ItemStack noReward = new ItemStack(Material.BARRIER);
            ItemMeta meta = noReward.getItemMeta();
            meta.setDisplayName("§c받을 보상이 없습니다.");
            noReward.setItemMeta(meta);
            gui.setItem(22, noReward); // 중앙에 표시
        } else {
            for (ItemStack reward : rewards) {
                gui.addItem(reward.clone());
            }
        }
        player.openInventory(gui);
    }
}