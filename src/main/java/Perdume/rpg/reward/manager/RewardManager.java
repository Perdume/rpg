package Perdume.rpg.reward.manager;

import Perdume.rpg.Rpg;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RewardManager {

    private final Rpg plugin;
    private final Map<UUID, List<ItemStack>> pendingRewards = new HashMap<>();
    private File rewardsFile;

    public RewardManager(Rpg plugin) {
        this.plugin = plugin;
        loadRewards();
    }

    /**
     * 플레이어에게 보상을 추가합니다. (우편함에 넣기)
     */
    public void addReward(Player player, ItemStack reward) {
        List<ItemStack> playerRewards = pendingRewards.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());
        playerRewards.add(reward);
        player.sendMessage("§a[알림] §e새로운 보상이 도착했습니다! §f/보상 §e명령어로 확인하세요.");
    }

    /**
     * 플레이어의 보상 목록을 가져옵니다.
     */
    public List<ItemStack> getRewards(Player player) {
        return pendingRewards.getOrDefault(player.getUniqueId(), Collections.emptyList());
    }

    /**
     * 플레이어가 보상을 수령했을 때, 목록에서 제거합니다.
     */
    public void claimReward(Player player, ItemStack item) {
        if (!pendingRewards.containsKey(player.getUniqueId())) return;
        pendingRewards.get(player.getUniqueId()).remove(item);
    }

    /**
     * 서버 종료 시, 모든 보상 데이터를 파일에 저장합니다.
     */
    public void saveRewards() {
        if (rewardsFile == null) {
            rewardsFile = new File(plugin.getDataFolder(), "rewards.yml");
        }
        FileConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, List<ItemStack>> entry : pendingRewards.entrySet()) {
            config.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            config.save(rewardsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 서버 시작 시, 파일에서 보상 데이터를 불러옵니다.
     */
    @SuppressWarnings("unchecked")
    public void loadRewards() {
        rewardsFile = new File(plugin.getDataFolder(), "rewards.yml");
        if (!rewardsFile.exists()) return;
        FileConfiguration config = YamlConfiguration.loadConfiguration(rewardsFile);
        for (String key : config.getKeys(false)) {
            UUID uuid = UUID.fromString(key);
            List<ItemStack> items = (List<ItemStack>) config.getList(key);
            if (items != null) {
                pendingRewards.put(uuid, items);
            }
        }
    }
}