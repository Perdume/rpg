package Perdume.rpg.util;

import Perdume.rpg.Rpg;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * RPG 플러그인에서 사용되는 모든 특별한 아이템을 생성하는 클래스입니다.
 */
public class ItemFactory {

    private static final NamespacedKey RPG_ITEM_KEY = new NamespacedKey(Rpg.getInstance(), "rpg_item");

    /**
     * '보스의 정수' 아이템을 생성하여 반환합니다.
     * @param amount 생성할 아이템의 개수
     * @return 생성된 ItemStack
     */
    public static ItemStack createBossEssence(int amount) {
        ItemStack item = new ItemStack(Material.NETHER_STAR, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item; // 아이템 메타 생성 실패 시 기본 아이템 반환

        meta.setDisplayName("§c보스의 정수");
        meta.setLore(List.of(
                "§7강력한 보스의 힘이 응축된 결정체.",
                "§7장비를 제작하거나 강화하는 데 사용된다."
        ));

        // 아이템에 반짝이는 효과를 추가합니다.
        addGlow(meta);

        meta.getPersistentDataContainer().set(RPG_ITEM_KEY, PersistentDataType.STRING, "boss_essence");

        item.setItemMeta(meta);
        return item;
    }

    /**
     * '강화 보호권' 아이템을 생성하여 반환합니다.
     * @param amount 생성할 아이템의 개수
     * @return 생성된 ItemStack
     */
    public static ItemStack createProtectionScroll(int amount) {
        ItemStack item = new ItemStack(Material.PAPER, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName("§b강화 보호권");
        meta.setLore(List.of(
                "§7강화 실패 시 아이템이 파괴되는 것을",
                "§71회 막아줍니다. (15성 이상에서만 사용 가능)"
        ));

        addGlow(meta);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * 아이템 메타에 반짝이는 효과를 추가하는 private 헬퍼 메소드입니다.
     * @param meta 효과를 추가할 ItemMeta
     */
    private static void addGlow(ItemMeta meta) {
        // 보이지 않는 인챈트를 추가하여 반짝이게 만듭니다.
        meta.addEnchant(Enchantment.LURE, 1, false);
        // "마법이 부여됨" 텍스트를 숨깁니다.
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }

    /**
     * [신규] 해당 아이템이 우리가 만든 특별한 RPG 아이템인지 확인합니다.
     * @param item 확인할 아이템
     * @return 특별한 아이템이면 true
     */
    public static boolean isRpgItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        // 꼬리표(Key)가 붙어있는지 여부만으로 판별합니다.
        return item.getItemMeta().getPersistentDataContainer().has(RPG_ITEM_KEY, PersistentDataType.STRING);
    }
}