// gui/SpawnGUI.java
package Perdume.rpg.gui;
import Perdume.rpg.Rpg;
import Perdume.rpg.core.player.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class SpawnGUI {
    public static final String GUI_TITLE = "§8[마을 이동]";
    public static void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, GUI_TITLE);
        ConfigurationSection section = Rpg.getInstance().getConfigManager().getLocationsConfig().getConfigurationSection("spawn-locations");
        PlayerData data = Rpg.getInstance().getPlayerDataManager().getPlayerData(player);

        for (String key : section.getKeys(false)) {
            if (data.hasUnlockedSpawn(key)) {
                // [해금됨] 기존 아이템 표시 로직
            } else {
                // [잠김] 회색 염료와 함께 "미발견 지역" Lore 표시
                ItemStack item = new ItemStack(Material.GRAY_DYE);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName("§8???");
                meta.setLore(List.of("§7아직 발견하지 못한 지역입니다."));
                item.setItemMeta(meta);
                gui.addItem(item);
            }
        }
        player.openInventory(gui);
    }
}

// listener/SpawnGUIListener.java