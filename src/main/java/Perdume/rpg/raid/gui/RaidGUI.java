package Perdume.rpg.raid.gui;

import Perdume.rpg.Rpg;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.List;

public class RaidGUI {

    public static final String GUI_TITLE = "§8[레이드 선택]";

    public static void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, GUI_TITLE);

        // Rpg.java에 정의된 보스 목록을 가져와 GUI에 표시
        for (String bossId : Rpg.BOSS_LIST) {
            ItemStack bossItem = new ItemStack(Material.DRAGON_HEAD); // 예시 아이콘
            ItemMeta meta = bossItem.getItemMeta();
            meta.setDisplayName("§c" + bossId);
            meta.setLore(List.of(
                    "§7클릭하여 레이드를 시작합니다.",
                    "§7(파티장만 가능)"
            ));
            bossItem.setItemMeta(meta);
            gui.addItem(bossItem);
        }

        player.openInventory(gui);
    }
}