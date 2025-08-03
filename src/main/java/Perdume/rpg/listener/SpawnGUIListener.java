package Perdume.rpg.listener;
import Perdume.rpg.Rpg;
import Perdume.rpg.gui.SpawnGUI;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.persistence.PersistentDataType;

public class SpawnGUIListener implements Listener {
    @EventHandler
    public void onGUIClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(SpawnGUI.GUI_TITLE)) return;
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        if (event.getCurrentItem() != null) {
            String spawnId = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Rpg.getInstance(), "spawn_id"), PersistentDataType.STRING);
            if (spawnId != null) {
                Location loc = Rpg.getInstance().getConfigManager().getSpawnLocation(spawnId);
                if (loc != null) {
                    player.teleport(loc);
                    player.sendMessage("§a" + event.getCurrentItem().getItemMeta().getDisplayName() + "§a(으)로 이동했습니다.");
                } else {
                    player.sendMessage("§c해당 위치를 찾을 수 없습니다.");
                }
                player.closeInventory();
            }
        }
    }
}