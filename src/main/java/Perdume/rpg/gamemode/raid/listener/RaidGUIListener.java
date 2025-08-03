package Perdume.rpg.gamemode.raid.listener;

import Perdume.rpg.Rpg;
import Perdume.rpg.core.party.Party;
import Perdume.rpg.core.party.PartyManager;
import Perdume.rpg.core.player.data.PlayerDataManager;
import Perdume.rpg.gamemode.raid.gui.RaidGUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class RaidGUIListener implements Listener {

    private final Rpg plugin;

    public RaidGUIListener(Rpg plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(RaidGUI.GUI_TITLE)) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        Party party = PartyManager.getParty(player);
        if (party == null) {
            player.sendMessage("§c파티에 소속되어 있어야 레이드를 시작할 수 있습니다.");
            player.closeInventory();
            return;
        }
        if (!party.isLeader(player)) {
            player.sendMessage("§c파티장만 레이드를 시작할 수 있습니다.");
            player.closeInventory();
            return;
        }

        String bossId = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
        // --- 2. [핵심] 1일 1클리어 제한 확인 ---
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        for (Player member : party.getMembers()) {
            if (!dataManager.canClearBoss(member, bossId)) {
                // 파티원 전체에게 누가 제한에 걸렸는지 알려줌
                party.broadcast("§c파티원 " + member.getName() + "님은 오늘 이미 '" + bossId + "' 레이드를 클리어하여 입장할 수 없습니다.");
                player.closeInventory();
                return;
            }
        }
        
        player.closeInventory();
        party.broadcast("파티장이 '" + bossId + "' 레이드를 시작합니다!");
        plugin.getRaidManager().createAndStartRaid(party.getMembers(), bossId);
    }
}