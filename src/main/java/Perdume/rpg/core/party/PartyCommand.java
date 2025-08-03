package Perdume.rpg.core.party;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.stream.Collectors;

public class PartyCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용 가능합니다.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create", "생성" -> PartyManager.createParty(player);
            case "invite", "초대" -> {
                if (args.length < 2) { player.sendMessage("§c사용법: /파티 초대 <플레이어이름>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                PartyManager.invitePlayer(player, target);
            }
            case "accept", "수락" -> {
                if (args.length < 2) { player.sendMessage("§c사용법: /파티 수락 <파티장이름>"); return true; }
                Player leader = Bukkit.getPlayer(args[1]);
                PartyManager.acceptInvite(player, leader);
            }
            case "leave", "탈퇴" -> PartyManager.leaveParty(player);
            case "disband", "해산" -> PartyManager.disbandParty(player);
            case "info", "정보" -> {
                Party party = PartyManager.getParty(player);
                if (party == null) {
                    player.sendMessage("§c소속된 파티가 없습니다.");
                    return true;
                }
                String members = party.getMembers().stream().map(Player::getName).collect(Collectors.joining(", "));
                player.sendMessage("§6--- 파티 정보 ---");
                player.sendMessage("§e파티장: §f" + party.getLeader().getName());
                player.sendMessage("§e파티원: §f" + members);
            }
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6--- 파티 명령어 (/파티 또는 /p) ---");
        player.sendMessage("§e/파티 생성 §7- 새로운 파티를 생성합니다.");
        player.sendMessage("§e/파티 초대 <이름> §7- 플레이어를 파티에 초대합니다.");
        player.sendMessage("§e/파티 수락 <이름> §7- 받은 초대를 수락합니다.");
        player.sendMessage("§e/파티 탈퇴 §7- 현재 파티에서 탈퇴합니다.");
        player.sendMessage("§e/파티 해산 §7- 파티를 해산합니다. (파티장 전용)");
        player.sendMessage("§e/파티 정보 §7- 현재 파티 정보를 봅니다.");
    }
}