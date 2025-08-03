package Perdume.rpg.gamemode.raid;

import Perdume.rpg.Rpg;
import Perdume.rpg.gamemode.raid.gui.RaidGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RaidCommand implements CommandExecutor {
    private final Rpg plugin;
    public RaidCommand(Rpg plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용 가능합니다.");
            return true;
        }


        // 이제 이 명령어는 GUI를 열어주는 역할만 합니다.
        // GUI 리스너에서 파티 여부 등을 체크합니다.
        RaidGUI.open(player);
        return true;
    }
}