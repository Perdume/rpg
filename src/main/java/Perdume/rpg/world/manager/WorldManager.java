package Perdume.rpg.world.manager;


import Perdume.rpg.Rpg;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * 인스턴스 레이드 월드의 생성, 로드, 언로드, 삭제를 전담하는 유틸리티 클래스입니다.
 * 모든 메소드는 static으로 선언되어, 객체 생성 없이 바로 사용할 수 있습니다.
 */
public class WorldManager {


    /**
     * 지정된 이름의 공허 월드를 생성하고, 즉시 'plugins/Rpg/worlds/' 폴더에 템플릿으로 저장합니다.
     * @param worldName 새로 만들 템플릿 월드의 이름
     * @return 생성이 성공하면 true, 실패하면 false
     */
    public static boolean createVoidTemplate(String worldName) {
        Rpg plugin = Rpg.getInstance();
        File templatesDir = new File(plugin.getDataFolder(), "worlds");
        File targetTemplateFolder = new File(templatesDir, worldName);

        // 이미 최종 목적지에 템플릿이 존재하면, 작업을 수행할 필요 없음
        if (targetTemplateFolder.exists()) {
            return true;
        }

        // 월드 생성
        WorldCreator wc = new WorldCreator(worldName);
        wc.generator("VoidGen");
        World world = wc.createWorld();

        if (world != null) {
            world.save();
            Bukkit.unloadWorld(world, true); // 변경사항을 저장하며 언로드

            // [핵심] 생성된 월드 폴더를 최종 목적지로 이동
            File sourceFolder = new File(Bukkit.getWorldContainer(), worldName);
            if (sourceFolder.exists()) {
                if (!templatesDir.exists()) {
                    templatesDir.mkdirs(); // 'worlds' 폴더가 없으면 생성
                }
                // 폴더 이동 (기존 위치의 폴더를 새 위치로 이름 변경)
                if (sourceFolder.renameTo(targetTemplateFolder)) {
                    Rpg.log.info("'" + worldName + "' 템플릿을 'plugins/Rpg/worlds/' 폴더로 이동했습니다.");
                    return true;
                } else {
                    Rpg.log.severe("'" + worldName + "' 템플릿 폴더 이동에 실패했습니다. 수동으로 옮겨주세요.");
                    // 이동 실패 시, 잘못된 위치에 남은 폴더를 삭제
                    deleteWorldFolder(sourceFolder);
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * 'plugins/Rpg/worlds/' 폴더에 있는 템플릿을 복사하여 레이드 인스턴스를 생성합니다.
     * 이 때, 복사된 월드의 기본 생성기를 공허 생성기로 지정합니다.
     * @param newWorldName 새로 생성될 월드의 이름 (예: "Raid--RUN--1")
     * @param templateName 복사할 원본 템플릿 월드의 이름
     * @param callback 월드 로드가 완료된 후 실행될 작업
     */
    public static void copyAndLoadWorld(String newWorldName, String templateName, Consumer<World> callback) {
        Rpg plugin = Rpg.getInstance();
        new BukkitRunnable() {
            @Override
            public void run() {
                File templateFolder = new File(new File(plugin.getDataFolder(), "worlds"), templateName);
                File instanceFolder = new File(Bukkit.getWorldContainer(), newWorldName);

                if (!templateFolder.exists() || !templateFolder.isDirectory()) {
                    new BukkitRunnable() {
                        @Override
                        public void run() { callback.accept(null); }
                    }.runTask(plugin);
                    return;
                }

                copyWorldFolder(templateFolder, instanceFolder);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // [핵심 수정] WorldCreator에 공허 생성기를 명시적으로 지정합니다.
                        WorldCreator wc = new WorldCreator(newWorldName);
                        wc.generator("VoidGen"); // plugin.yml에 정의된 생성기

                        callback.accept(wc.createWorld());
                    }
                }.runTask(plugin);
            }
        }.runTaskAsynchronously(plugin);
    }

    public static boolean createVoidWorld(String worldName) {
        if (Bukkit.getWorld(worldName) != null) return false;

        WorldCreator wc = new WorldCreator(worldName);
        wc.generator("Rpg:VoidGenerator");
        World world = wc.createWorld();

        if (world != null) {
            // [핵심 수정] config.yml에 월드 이름 추가
            Rpg plugin = Rpg.getInstance();
            FileConfiguration config = plugin.getConfig();
            List<String> worlds = config.getStringList("worlds");
            if (!worlds.contains(worldName)) {
                worlds.add(worldName);
                config.set("worlds", worlds);
                plugin.saveConfig();
            }
            return true;
        }
        return false;
    }

    /**
     * 월드를 언로드하고, 폴더를 삭제하며, config.yml에서도 제거합니다.
     * @param worldName 삭제할 월드의 이름
     */
    public static void unloadAnddeleteWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            world.getPlayers().forEach(p -> p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation()));
            if (!Bukkit.unloadWorld(world, false)) {
                Rpg.log.severe("월드 '" + worldName + "' 언로드에 실패했습니다!");
                return;
            }
        }

        // [핵심 수정] config.yml에서 월드 이름 제거
        Rpg plugin = Rpg.getInstance();
        FileConfiguration config = plugin.getConfig();
        List<String> worlds = config.getStringList("worlds");
        if (worlds.contains(worldName)) {
            worlds.remove(worldName);
            config.set("worlds", worlds);
            plugin.saveConfig();
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                deleteWorldFolder(new File(Bukkit.getWorldContainer(), worldName));
            }
        }.runTaskAsynchronously(Rpg.getInstance());
    }

    /**
     * [신규] 수정용 월드의 변경사항을 원본 템플릿에 덮어씌워 저장합니다.
     * @param editWorld 수정이 완료된 월드
     * @param templateName 덮어쓸 원본 템플릿의 이름
     * @param callback 저장이 완료된 후 실행될 작업 (성공 여부를 boolean으로 받음)
     */
    public static void saveAndOverwriteTemplate(World editWorld, String templateName, Consumer<Boolean> callback) {

        String editWorldName = editWorld.getName();
        File editWorldFolder = editWorld.getWorldFolder();
        File templateFolder = new File(Rpg.getInstance().getDataFolder(), "worlds/" + templateName);

        editWorld.save();

        // true: 변경사항을 저장하며 언로드
        if (!Bukkit.unloadWorld(editWorld, false)) {
            Rpg.log.severe("맵 저장 실패: 수정용 월드 '" + editWorldName + "' 언로드에 실패했습니다.");
            callback.accept(false);
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                // --- [핵심] 덮어쓰기 로직 ---

                // 1. 기존 원본 템플릿 폴더를 완전히 삭제합니다.
                deleteWorldFolder(templateFolder);

                // 2. 수정이 완료되어 언로드된 월드 폴더를 원본 템플릿 이름으로 복사합니다.
                copyWorldFolder(editWorldFolder, templateFolder);

                // 3. 임시로 사용했던 수정용 월드 폴더를 최종적으로 삭제하여 흔적을 지웁니다.
                deleteWorldFolder(editWorldFolder);

                // 모든 작업이 완료되었음을 메인 스레드에 알립니다.
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        callback.accept(true);
                    }
                }.runTask(Rpg.getInstance());
            }
        }.runTaskAsynchronously(Rpg.getInstance());
    }

    // --- 내부 유틸리티 메소드 (폴더 복사 및 삭제) ---
    private static void copyWorldFolder(File source, File target) {
        try {
            if (source.isDirectory()) {
                if (!target.exists()) target.mkdirs();
                String[] files = source.list();
                if (files == null) return;
                for (String file : files) {
                    if (file.equals("uid.dat") || file.equals("session.lock")) continue;
                    copyWorldFolder(new File(source, file), new File(target, file));
                }
            } else {
                try (InputStream in = new FileInputStream(source);
                     OutputStream out = new FileOutputStream(target)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = in.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteWorldFolder(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteWorldFolder(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        path.delete();
    }
}