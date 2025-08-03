package Perdume.rpg.command;

import Perdume.rpg.Rpg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class IslandCommand implements CommandExecutor {

    private final Rpg plugin;
    public IslandCommand(Rpg plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용 가능합니다.");
            return true;
        }

        if (args.length == 0) {
            // 인자가 없으면 자기 섬으로 이동
            plugin.getSkyblockManager().enterMyIsland(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "생성" -> plugin.getSkyblockManager().createIsland(player);
            // case "초대" -> ...
            // case "수락" -> ...
            // case "나가기" -> ...
            default -> player.sendMessage("§c알 수 없는 명령어입니다. /섬 도움말");
        }
        return true;
    }
}