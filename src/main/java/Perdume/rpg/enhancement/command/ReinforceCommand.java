package Perdume.rpg.enhancement.command;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;

public class ReinforceCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용 가능합니다.");
            return true;
        }

        Inventory gui = Bukkit.createInventory(null, 27, "§8강화");

        ItemStack grayPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta grayMeta = grayPane.getItemMeta();
        grayMeta.setDisplayName(" ");
        grayPane.setItemMeta(grayMeta);

        // 핵심 슬롯(12, 13, 14)을 제외하고 배경 채우기
        for (int i = 0; i < gui.getSize(); i++) {
            if (i != 12 && i != 13 && i != 14) {
                gui.setItem(i, grayPane);
            }
        }

        // 초기 정보 패널 설정
        ItemStack infoPanel = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = infoPanel.getItemMeta();
        infoMeta.setDisplayName("§e[ 강화 정보 ]");
        infoMeta.setLore(Collections.singletonList("§7아이템을 왼쪽에 놓아주세요."));
        infoPanel.setItemMeta(infoMeta);
        gui.setItem(14, infoPanel);

        player.openInventory(gui);
        return true;
    }
}