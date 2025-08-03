package Perdume.rpg.command;

import Perdume.rpg.gui.SpawnGUI; // 새로운 GUI 클래스
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용 가능합니다.");
            return true;
        }

        // GUI를 열어줌
        SpawnGUI.open(player);
        return true;
    }
}