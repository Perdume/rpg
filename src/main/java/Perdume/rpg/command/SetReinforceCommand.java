package Perdume.rpg.command;

import Perdume.rpg.core.util.ReinforceUtil;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SetReinforceCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        if (!player.hasPermission("rpg.admin.setreinforce")) {
            player.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage("§cUsage: /setreinforce <level>");
            return true;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand.getType() == Material.AIR) {
            player.sendMessage("§cYou must be holding an item to reinforce.");
            return true;
        }

        if (!ReinforceUtil.isReinforceable(itemInHand)) {
            player.sendMessage("§cThis item cannot be reinforced.");
            return true;
        }

        int level;
        try {
            level = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cThe level must be a number.");
            return true;
        }
        
        if (level < 0) {
            player.sendMessage("§cThe level cannot be negative.");
            return true;
        }

        ReinforceUtil.setReinforceLevel(itemInHand, level);
        player.sendMessage("§aSuccessfully set the reinforcement level of your item to §e+" + level + "§a.");

        return true;
    }
}