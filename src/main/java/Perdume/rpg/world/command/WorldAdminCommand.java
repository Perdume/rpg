package Perdume.rpg.world.command; // 사용하시는 패키지 경로

import Perdume.rpg.Rpg;

import Perdume.rpg.util.TeleportUtil;
import Perdume.rpg.world.manager.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.StringUtil;

import java.util.*;
import java.util.function.Consumer;

// [핵심] CommandExecutor와 TabCompleter를 모두 구현(implements)합니다.
public class WorldAdminCommand implements CommandExecutor, TabCompleter {

    private final Rpg plugin;
    public static final Map<UUID, EditSession> editingPlayers = new HashMap<>();
    private int nextEditId = 1;

    public record EditSession(String worldName, String templateName, Location originalLocation) {}

    public WorldAdminCommand(Rpg plugin) {
        this.plugin = plugin;
    }

    /**
     * '/rpworld' 명령어가 실행되었을 때의 로직을 처리합니다.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("rpg.admin")) {
            sender.sendMessage("§c권한이 없습니다.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        Player player = (sender instanceof Player) ? (Player) sender : null;

        switch (subCommand) {
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "list" -> handleList(sender);
            case "edit", "tp" -> {
                if (player == null) { sender.sendMessage("§c이 명령어는 플레이어만 사용 가능합니다."); return true; }
                handleTeleportOrEdit(player, args, subCommand.equals("edit"));
            }
            case "save", "leave" -> {
                if (player == null) { sender.sendMessage("§c플레이어만 사용 가능합니다."); return true; }
                if (subCommand.equals("save")) {
                    handleSave(player, (success) -> {});
                } else {
                    handleLeave(player);
                }
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    /**
     * '/rpworld' 명령어 입력 시 Tab 키를 눌렀을 때, 자동 완성 목록을 보여줍니다.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("rpg.admin")) return Collections.emptyList();

        List<String> completions = new ArrayList<>();
        // 첫 번째 인자 (서브 명령어) 자동 완성
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], Arrays.asList("create", "delete", "edit", "save", "leave", "list", "tp"), completions);
        }
        // 두 번째 인자 (월드 이름) 자동 완성
        else if (args.length == 2) {
            if (Arrays.asList("delete", "edit", "tp").contains(args[0].toLowerCase())) {
                StringUtil.copyPartialMatches(args[1], plugin.getConfig().getStringList("raid-bosses"), completions);
            }
        }
        // 세 번째 인자 (삭제 확인) 자동 완성
        else if (args.length == 3 && args[0].equalsIgnoreCase("delete")) {
             StringUtil.copyPartialMatches(args[2], List.of("confirm"), completions);
        }
        return completions;
    }

    // --- 각 서브 명령어 처리 메소드 ---

    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage("§c사용법: /wa create <월드이름>"); return; }
        String worldName = args[1];

        sender.sendMessage("§e공허 월드 '" + worldName + "' 생성을 시작합니다...");
        if (WorldManager.createVoidTemplate(worldName)) {
            sender.sendMessage("§a성공적으로 생성되었습니다! /wa edit " + worldName + " 명령어로 수정하세요.");
        } else {
            sender.sendMessage("§c월드 생성에 실패했습니다. 이미 존재하거나 이름이 잘못되었을 수 있습니다.");
        }
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 3 || !args[2].equalsIgnoreCase("confirm")) {
            sender.sendMessage("§c정말로 삭제하시려면 /wa delete " + (args.length > 1 ? args[1] : "<월드이름>") + " confirm 을 입력하세요.");
            return;
        }
        String worldName = args[1];
        sender.sendMessage("§e월드 '" + worldName + "' 삭제를 시작합니다...");
        WorldManager.unloadAnddeleteWorld(worldName);
        sender.sendMessage("§a삭제 작업이 완료되었습니다.");
    }

    private void handleList(CommandSender sender) {
        List<String> worlds = plugin.getConfig().getStringList("raid-bosses");
        if (worlds.isEmpty()) {
            sender.sendMessage("§e등록된 RPG 템플릿 월드가 없습니다.");
            return;
        }
        sender.sendMessage("§6--- 등록된 RPG 템플릿 월드 목록 ---");
        worlds.forEach(world -> sender.sendMessage("§a- " + world));
    }

    private void handleTeleportOrEdit(Player player, String[] args, boolean isEditMode) {
        if (args.length < 2) {
            player.sendMessage("§c사용법: /wa " + (isEditMode ? "edit" : "tp") + " <템플릿_맵이름>");
            return;
        }
        String templateName = args[1];

        if (isEditMode) {
            if (editingPlayers.containsKey(player.getUniqueId())) {
                player.sendMessage("§c이미 수정 중인 맵이 있습니다. 먼저 종료해주세요.");
                return;
            }
            String editWorldName = "Raid--EDIT--" + Integer.hashCode(nextEditId++);
            player.sendMessage("§e맵 '" + templateName + "'의 수정용 복사본(" + editWorldName + ")을 생성합니다...");
            WorldManager.copyAndLoadWorld(editWorldName, templateName, (newWorld) -> {
                if (newWorld != null) {
                    editingPlayers.put(player.getUniqueId(), new EditSession(editWorldName, templateName, player.getLocation()));
                    player.teleport(new Location(newWorld, 0.5, 65, 0.5));
                    player.setGameMode(GameMode.CREATIVE);
                    player.sendMessage("§a맵 수정 모드로 진입했습니다. 끝나면 /wa save 또는 /wa leave를 사용해주세요.");
                } else {
                    player.sendMessage("§c맵 생성에 실패했습니다. 템플릿 맵 이름이 정확한지 확인해주세요.");
                }
            });
        } else { // 단순 텔레포트
            World world = Bukkit.getWorld(templateName);
            if (world != null) {
                player.teleport(new Location(world, 0.5, 65, 0.5));
                player.sendMessage("§a월드 '" + templateName + "'으로 이동했습니다.");
            } else {
                player.sendMessage("§c월드가 로드되어 있지 않거나, 존재하지 않습니다.");
            }
        }
    }

    public void handleSave(Player player, Consumer<Boolean> afterSaveAction) {
        EditSession session = editingPlayers.get(player.getUniqueId());
        if (session == null) { /* ... */ return; }

        World editWorld = Bukkit.getWorld(session.worldName());
        if (editWorld == null) { /* ... */ return; }

        if (player.isOnline()) player.sendMessage("§e변경사항을 저장하고 원래 위치로 돌아갑니다...");

        // 1. 플레이어를 먼저 안전하게 귀환
        TeleportUtil.returnPlayerToSafety(player, session.originalLocation());
        editingPlayers.remove(player.getUniqueId());

        // 2. 5틱 후에 월드 저장 작업을 시작
        new BukkitRunnable() {
            @Override
            public void run() {
                if (editWorld.getPlayers().isEmpty()) {
                    WorldManager.saveAndOverwriteTemplate(editWorld, session.templateName(), (success) -> {
                        if (player.isOnline()) {
                            if (success) player.sendMessage("§a성공적으로 저장되었습니다!");
                            else player.sendMessage("§c저장에 실패했습니다.");
                        }
                        afterSaveAction.accept(success);
                    });
                } else {
                    Rpg.log.severe("맵 저장 실패: 월드 '" + session.worldName() + "'에 아직 플레이어가 남아있습니다.");
                    if (player.isOnline()) player.sendMessage("§c오류가 발생하여 맵을 저장하지 못했습니다.");
                    afterSaveAction.accept(false);
                }
            }
        }.runTaskLater(plugin, 5L);
    }

    /**
     * 현재 수정 중인 맵의 변경사항을 저장하지 않고 떠납니다.
     * @param player 대상 플레이어
     */
    private void handleLeave(Player player) {
        // 1. 플레이어의 수정 세션 정보를 가져옵니다.
        EditSession session = editingPlayers.get(player.getUniqueId());
        if (session == null) {
            player.sendMessage("§c수정 중인 맵이 없습니다.");
            return;
        }

        player.sendMessage("§e수정 사항을 저장하지 않고 맵을 떠납니다...");

        // [핵심] 1. 플레이어를 먼저 안전하게 귀환시킵니다.
        TeleportUtil.returnPlayerToSafety(player, session.originalLocation());
        editingPlayers.remove(player.getUniqueId());

        // [핵심] 2. '5틱 후에' 월드 삭제 작업을 시작하여 서버 안정성을 확보합니다.
        new BukkitRunnable() {
            @Override
            public void run() {
                WorldManager.unloadAnddeleteWorld(session.worldName());
            }
        }.runTaskLater(plugin, 5L);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6--- RPG World Admin Commands (/wa) ---");
        sender.sendMessage("§e/wa list §7- List registered template worlds.");
        sender.sendMessage("§e/wa create <name> §7- Create and register a new void world.");
        sender.sendMessage("§e/wa delete <name> confirm §7- Delete a template world.");
        sender.sendMessage("§e/wa edit <name> §7- Enter edit mode for a world.");
        sender.sendMessage("§e/wa tp <name> §7- Teleport to a loaded world.");
        sender.sendMessage("§e/wa save §7- Save changes and leave edit mode.");
        sender.sendMessage("§e/wa leave §7- Discard changes and leave edit mode.");
    }
}