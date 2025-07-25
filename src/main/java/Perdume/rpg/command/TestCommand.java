package Perdume.rpg.command;

import Perdume.rpg.Rpg;
import Perdume.rpg.system.TestDummyManager;
import Perdume.rpg.world.manager.WorldManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TestCommand implements CommandExecutor {

    private final Rpg plugin;
    private final Map<UUID, Location> originalLocations = new HashMap<>();

    public TestCommand(Rpg plugin) {
        this.plugin = plugin;
    }

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

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> handleStart(player, args);
            case "leave" -> handleLeave(player);
            default -> sendHelp(player);
        }

        return true;
    }

    private void handleStart(Player player, String[] args) {
        if (originalLocations.containsKey(player.getUniqueId())) {
            player.sendMessage("§c이미 테스트 세션을 진행 중입니다. /rt leave로 먼저 종료해주세요.");
            return;
        }

        EntityType entityType;
        try {
            entityType = (args.length > 1) ? EntityType.valueOf(args[1].toUpperCase()) : EntityType.ZOMBIE;
        } catch (IllegalArgumentException e) {
            player.sendMessage("§c알 수 없는 엔티티 타입입니다. (예: ZOMBIE, IRON_GOLEM)");
            return;
        }

        String worldName = "Raid--TEST--" + player.getName();
        player.sendMessage("§e대미지 테스트용 월드를 생성합니다...");

        // 1. 임시 템플릿을 만들고
        WorldManager.createVoidTemplate(worldName);
        // 2. 그 템플릿을 기반으로 테스트 월드를 로드
        WorldManager.copyAndLoadWorld(worldName, worldName, (newWorld) -> {
            if (newWorld == null) {
                player.sendMessage("§c테스트 월드 생성에 실패했습니다.");
                return;
            }

            // --- [핵심 수정] 30x30 발판 생성 로직 ---
            player.sendMessage("§e테스트용 발판을 생성합니다...");
            Location center = new Location(newWorld, 0, 64, 0);
            for (int x = -15; x < 15; x++) {
                for (int z = -15; z < 15; z++) {
                    center.clone().add(x, 0, z).getBlock().setType(Material.STONE_BRICKS); // 석재 벽돌로 생성
                }
            }

            originalLocations.put(player.getUniqueId(), player.getLocation());
            Location spawnPoint = new Location(newWorld, 0.5, 65, 0.5);
            player.teleport(spawnPoint);
            player.setGameMode(GameMode.CREATIVE);

            player.sendMessage("§a테스트 월드로 이동했습니다. 허수아비를 소환합니다.");

            // 허수아비 소환
            Location dummyLocation = spawnPoint.clone().add(0, 0, 5);
            LivingEntity dummy = (LivingEntity) newWorld.spawnEntity(dummyLocation, entityType);
            dummy.setAI(false);
            dummy.setGravity(false);
            dummy.setInvulnerable(true);

            TestDummyManager.addDummy(dummy.getUniqueId());
        });
    }

    private void handleLeave(Player player) {
        if (!originalLocations.containsKey(player.getUniqueId())) {
            player.sendMessage("§c진행 중인 테스트 세션이 없습니다.");
            return;
        }

        String worldName = "Raid--TEST--" + player.getName();
        WorldManager.unloadAnddeleteWorld(worldName);

        player.teleport(originalLocations.remove(player.getUniqueId()));
        player.sendMessage("§a테스트 세션을 종료하고 원래 위치로 돌아왔습니다.");
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6--- RPG 테스트 명령어 (/rt) ---");
        player.sendMessage("§e/rt start [엔티티타입] §7- 테스트 세션을 시작합니다.");
        player.sendMessage("§e/rt leave §7- 테스트 세션을 종료하고 돌아옵니다.");
    }
}