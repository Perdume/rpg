package Perdume.rpg.enhancement.listener;

import Perdume.rpg.Rpg;
import Perdume.rpg.util.EquipmentStats;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReinforceListener implements Listener {
    private final Rpg plugin;
    private final NamespacedKey reinforceLevelKey;
    private record StarForceStats(double success, double destruction) {}
    private static final Map<Integer, StarForceStats> PROBABILITY_MAP;

    // 슬롯 번호를 상수로 정의하여 가독성 및 유지보수 향상
    private static final int ITEM_SLOT = 12;
    private static final int BUTTON_SLOT = 13;
    private static final int INFO_SLOT = 14;

    static {
        // [수정] 메이플스토리 스타포스 원본 기반 30성 최종 확률 테이블
        PROBABILITY_MAP = Stream.of(new Object[][]{
                {0, new StarForceStats(0.95, 0.0)},   // 0 -> 1
                {1, new StarForceStats(0.90, 0.0)},   // 1 -> 2
                {2, new StarForceStats(0.85, 0.0)},   // 2 -> 3
                {3, new StarForceStats(0.85, 0.0)},   // 3 -> 4
                {4, new StarForceStats(0.80, 0.0)},   // 4 -> 5
                {5, new StarForceStats(0.75, 0.0)},   // 5 -> 6
                {6, new StarForceStats(0.70, 0.0)},   // 6 -> 7
                {7, new StarForceStats(0.65, 0.0)},   // 7 -> 8
                {8, new StarForceStats(0.60, 0.0)},   // 8 -> 9
                {9, new StarForceStats(0.55, 0.0)},   // 9 -> 10
                {10, new StarForceStats(0.50, 0.0)},  // 10 -> 11
                {11, new StarForceStats(0.45, 0.0)},  // 11 -> 12
                {12, new StarForceStats(0.40, 0.0)},  // 12 -> 13
                {13, new StarForceStats(0.35, 0.0)},  // 13 -> 14
                {14, new StarForceStats(0.30, 0.0)},  // 14 -> 15
                {15, new StarForceStats(0.30, 0.021)}, // 15 -> 16
                {16, new StarForceStats(0.30, 0.021)}, // 16 -> 17
                {17, new StarForceStats(0.30, 0.021)}, // 17 -> 18
                {18, new StarForceStats(0.30, 0.028)}, // 18 -> 19
                {19, new StarForceStats(0.30, 0.028)}, // 19 -> 20
                {20, new StarForceStats(0.30, 0.07)},  // 20 -> 21
                {21, new StarForceStats(0.30, 0.07)},  // 21 -> 22
                {22, new StarForceStats(0.03, 0.194)}, // 22 -> 23
                {23, new StarForceStats(0.02, 0.294)}, // 23 -> 24
                {24, new StarForceStats(0.01, 0.396)}, // 24 -> 25
                {25, new StarForceStats(0.03, 0.194)}, // 25 -> 26 (22성 확률과 유사)
                {26, new StarForceStats(0.02, 0.294)}, // 26 -> 27 (23성 확률과 유사)
                {27, new StarForceStats(0.02, 0.392)}, // 27 -> 28
                {28, new StarForceStats(0.01, 0.495)}, // 28 -> 29
                {29, new StarForceStats(0.01, 0.594)}  // 29 -> 30
        }).collect(Collectors.toMap(data -> (Integer) data[0], data -> (StarForceStats) data[1]));
    }

    public ReinforceListener(Rpg plugin) {
        this.plugin = plugin;
        this.reinforceLevelKey = new NamespacedKey(plugin, "reinforce_level");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§8강화")) return;
        event.setCancelled(true); // 강화창 내에서는 기본 클릭 행동을 모두 막음

        Player player = (Player) event.getWhoClicked();
        Inventory topInventory = event.getView().getTopInventory();
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) return;

        // 강화 GUI 창 내부를 클릭했을 경우
        if (clickedInventory.equals(topInventory)) {
            // 아이템 회수 (아이템 슬롯 클릭)
            if (event.getSlot() == ITEM_SLOT && topInventory.getItem(ITEM_SLOT) != null) {
                player.getInventory().addItem(topInventory.getItem(ITEM_SLOT).clone());
                topInventory.setItem(ITEM_SLOT, null);
                updateInfoPanel(topInventory, null);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.5f);
            }
            // 강화 버튼 클릭
            else if (event.getSlot() == BUTTON_SLOT) {
                executeReinforcement(player, topInventory);
            }
        }
        // 자신의 인벤토리를 클릭했을 경우 (아이템 등록)
        else {
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                // [핵심] 강화 가능한 아이템인지 확인
                if (!isReinforceable(clickedItem)) {
                    player.sendMessage("§c이 아이템은 강화할 수 없습니다.");
                    return;
                }

                // 강화 슬롯이 비어있을 때만 등록 가능
                if (topInventory.getItem(ITEM_SLOT) == null) {
                    topInventory.setItem(ITEM_SLOT, clickedItem.clone());
                    event.setCurrentItem(null); // 플레이어 인벤토리에서 아이템 제거
                    updateInfoPanel(topInventory, clickedItem);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.5f);
                }
            }
        }
    }

    /**
     * 강화 로직을 실행합니다.
     */
    private void executeReinforcement(Player player, Inventory gui) {
        ItemStack targetItem = gui.getItem(ITEM_SLOT);
        if (targetItem == null || targetItem.getType() == Material.AIR) {
            player.sendMessage("§c강화할 아이템을 왼쪽에 놓아주세요.");
            return;
        }

        int currentLevel = getReinforceLevel(targetItem);
        StarForceStats stats = PROBABILITY_MAP.get(currentLevel);
        if (stats == null) {
            player.sendMessage("§e더 이상 강화할 수 없습니다.");
            return;
        }

        Economy econ = Rpg.econ;
        long finalCost = (long) (1000 + Math.pow(currentLevel + 1, 3) * 150);
        if (econ.getBalance(player) < finalCost) {
            player.sendMessage("§c비용이 부족합니다. (§e" + NumberFormat.getInstance().format(finalCost) + "원§c 필요)");
            return;
        }

        econ.withdrawPlayer(player, finalCost);
        player.sendMessage("§e" + currentLevel + "성 → " + (currentLevel + 1) + "성 강화 시도... (§6비용: " + NumberFormat.getInstance().format(finalCost) + "원§e)");

        // 강화 결과에 따른 분기 처리
        double random = Math.random();
        if (random < stats.success()) {
            setReinforceLevel(targetItem, currentLevel + 1);
            player.sendMessage("§a강화에 성공했습니다!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1.2f);
        } else if (random < stats.success() + stats.destruction()) {
            gui.setItem(ITEM_SLOT, null); // 아이템 파괴
            player.sendMessage("§4강화에 실패하여 아이템이 파괴되었습니다...");
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 1f);
        } else {
            setReinforceLevel(targetItem, currentLevel);
            player.sendMessage("§c강화에 실패했습니다. (레벨 유지)");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1, 1f);
        }

        // 강화 시도 후 정보 패널 즉시 업데이트
        updateInfoPanel(gui, gui.getItem(ITEM_SLOT));
    }

    /**
     * 강화 정보 패널을 실시간으로 업데이트합니다.
     */
    private void updateInfoPanel(Inventory gui, ItemStack item) {
        ItemStack infoPanel = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = infoPanel.getItemMeta();
        infoMeta.setDisplayName("§e[ 강화 정보 ]");

        List<String> lore = new ArrayList<>();
        if (item == null || item.getType() == Material.AIR) {
            lore.add("§7아이템을 왼쪽에 놓아주세요.");
        } else {
            int currentLevel = getReinforceLevel(item);
            StarForceStats stats = PROBABILITY_MAP.get(currentLevel);

            String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ?
                    item.getItemMeta().getDisplayName() : item.getType().name();
            lore.add("§7아이템: " + itemName);
            lore.add(" ");

            if (stats == null) {
                lore.add("§b최고 강화 단계입니다.");
            } else {
                long cost = (long) (1000 + Math.pow(currentLevel + 1, 3) * 150);
                double successRate = stats.success();
                double destructionRate = stats.destruction();
                // [추가] 실패(유지) 확률 계산 (소수점 오차 방지를 위해 0.0001보다 클 때만)
                double maintainRate = 1.0 - successRate - destructionRate;

                lore.add("§f" + currentLevel + "성 §7→ §b" + (currentLevel + 1) + "성");
                lore.add(" ");
                lore.add("§a성공 확률: " + String.format("%.2f", successRate * 100) + "%");
                // [추가] 유지 확률 표시
                if (maintainRate > 0.0001) {
                    lore.add("§7실패 확률: " + String.format("%.2f", maintainRate * 100) + "%");
                }
                if (destructionRate > 0) {
                    lore.add("§c파괴 확률: " + String.format("%.2f", destructionRate * 100) + "%");
                }
                lore.add("§6강화 비용: " + NumberFormat.getInstance().format(cost) + "원");
            }
        }
        infoMeta.setLore(lore);
        infoPanel.setItemMeta(infoMeta);
        gui.setItem(INFO_SLOT, infoPanel);

        ItemStack button = new ItemStack(Material.ANVIL);
        ItemMeta buttonMeta = button.getItemMeta();
        buttonMeta.setDisplayName("§a[ 강화 시작 ]");
        button.setItemMeta(buttonMeta);
        gui.setItem(BUTTON_SLOT, button);
    }

    private int getReinforceLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(reinforceLevelKey, PersistentDataType.INTEGER, 0);
    }

    /**
     * 아이템의 강화 레벨과 그에 따른 효과를 Lore에 설정합니다.
     * @param item 대상 아이템
     * @param level 설정할 레벨
     */
    private void setReinforceLevel(ItemStack item, int level) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(reinforceLevelKey, PersistentDataType.INTEGER, level);

        List<String> newLore = new ArrayList<>();
        if (level > 0) {
            newLore.add("§7강화 레벨: §e+" + level);
            newLore.add(" ");
        }

        var type = EquipmentStats.getType(item);
        var tier = EquipmentStats.getTier(item);
        var bonuses = EquipmentStats.getBonuses(type, tier);

        if (level > 0 && bonuses != null) {
            // 모든 능력치를 bonuses 객체에서 읽어와 Lore에 추가
            if (bonuses.flatAtk() > 0) newLore.add(String.format("§7공격력: §c+%.2f", level * bonuses.flatAtk()));
            if (bonuses.percentAtk() > 0) newLore.add(String.format("§7공격력: §c+%.1f%%", level * bonuses.percentAtk() * 100));
            if (bonuses.critChance() > 0) newLore.add(String.format("§7치명타 확률: §c+%.2f%%", level * bonuses.critChance() * 100));
            if (bonuses.critDmg() > 0) newLore.add(String.format("§7치명타 피해: §c+%.1f%%", level * bonuses.critDmg() * 100));
            if (bonuses.armorIgnore() > 0) newLore.add(String.format("§7방어 무시: §c+%.2f%%", level * bonuses.armorIgnore() * 100));
            if (bonuses.percentArmor() > 0) newLore.add(String.format("§7방어력: §a+%.1f%%", level * bonuses.percentArmor() * 100));
            if (bonuses.percentHealth() > 0) newLore.add(String.format("§7최대 체력: §a+%.1f%%", level * bonuses.percentHealth() * 100));
            if (bonuses.projRes() > 0) newLore.add(String.format("§7투사체 저항: §a+%.2f%%", level * bonuses.projRes() * 100));
            if (bonuses.dmgReduce() > 0) newLore.add(String.format("§7대미지 감소: §a+%.2f%%", level * bonuses.dmgReduce() * 100));
            if (bonuses.mobDmg() > 0) newLore.add(String.format("§7몬스터 추가 대미지: §a+%.2f%%", level * bonuses.mobDmg() * 100));
            if (bonuses.knockRes() > 0) newLore.add(String.format("§7넉백 저항: §a+%.2f%%", level * bonuses.knockRes() * 100));
            if (bonuses.atkSpeed() > 0) newLore.add(String.format("§7공격 속도: §e+%.2f%%", level * bonuses.atkSpeed() * 100));
        }

        meta.setLore(newLore);
        item.setItemMeta(meta);
    }
    /**
     * 강화 GUI가 닫힐 때 아이템을 돌려주는 이벤트 핸들러
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals("§8강화")) return;

        Inventory topInventory = event.getView().getTopInventory();
        ItemStack item = topInventory.getItem(ITEM_SLOT);

        // 아이템 슬롯에 아이템이 남아있다면
        if (item != null && item.getType() != Material.AIR) {
            Player player = (Player) event.getPlayer();
            player.getInventory().addItem(item); // 플레이어 인벤토리로 아이템 반환
            player.sendMessage("§e[알림] 강화 창에 있던 아이템이 인벤토리로 돌아왔습니다.");
        }
    }
    /**
     * 해당 아이템이 강화 가능한 종류인지 확인합니다.
     * @param item 확인할 아이템
     * @return 강화 가능하면 true
     */
    private boolean isReinforceable(ItemStack item) {
        if (item == null) return false;
        String materialName = item.getType().name();
        return materialName.contains("_SWORD") ||
                materialName.contains("_AXE") ||
                materialName.contains("_HELMET") ||
                materialName.contains("_CHESTPLATE") ||
                materialName.contains("_LEGGINGS") ||
                materialName.contains("_BOOTS");
    }
}