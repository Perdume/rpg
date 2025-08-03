package Perdume.rpg.command.admin;

import Perdume.rpg.config.LocationManager; // 새로운 LocationManager import
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnAdminCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용 가능합니다.");
            return true;
        }
        if (!player.hasPermission("rpg.admin")) {
            player.sendMessage("§c권한이 없습니다.");
            return true;
        }

        if (args.length < 2) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String spawnId = args[1];

        switch (subCommand) {
            case "생성" -> {
                LocationManager.setSpawnLocation(spawnId, player.getLocation());
                player.sendMessage("§a스폰 지점 '" + spawnId + "'을(를) 현재 위치에 설정했습니다.");
            }
            case "삭제" -> {
                if (LocationManager.removeSpawnLocation(spawnId)) {
                    player.sendMessage("§a스폰 지점 '" + spawnId + "'을(를) 삭제했습니다.");
                } else {
                    player.sendMessage("§c존재하지 않는 스폰 지점입니다.");
                }
            }
            case "목록" -> LocationManager.sendSpawnList(player);
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6--- 스폰 지점 관리 명령어 ---");
        player.sendMessage("§e/스폰설정 생성 <ID> §7- 현재 위치를 새 스폰 지점으로 등록합니다.");
        player.sendMessage("§e/스폰설정 삭제 <ID> §7- 등록된 스폰 지점을 삭제합니다.");
        player.sendMessage("§e/스폰설정 목록 §7- 모든 스폰 지점 목록을 봅니다.");
    }
}