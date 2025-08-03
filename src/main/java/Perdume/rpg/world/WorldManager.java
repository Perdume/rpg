package Perdume.rpg.world;


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
     * [수정] 지정된 타입의 공허 월드 템플릿을 생성합니다.
     * @param worldName 새로 만들 템플릿 월드의 이름
     * @param type 월드의 종류 (예: "raid", "island")
     * @return 생성이 성공하면 true, 실패하면 false
     */
    public static boolean createVoidTemplate(String worldName, String type) {
        Rpg plugin = Rpg.getInstance();
        File templatesDir = new File(plugin.getDataFolder(), "worlds/" + type);
        File targetTemplateFolder = new File(templatesDir, worldName);

        if (targetTemplateFolder.exists()) return true;
        if (Bukkit.getWorld(worldName) != null) return true;

        WorldCreator wc = new WorldCreator(worldName);
        wc.generator("VoidGen");
        World world = wc.createWorld();

        if (world != null) {
            world.setSpawnLocation(0, 65, 0);
            world.save();
            Bukkit.unloadWorld(world, true);

            File sourceFolder = new File(Bukkit.getWorldContainer(), worldName);
            if (sourceFolder.exists()) {
                if (!templatesDir.exists()) templatesDir.mkdirs();
                if (sourceFolder.renameTo(targetTemplateFolder)) {
                    Rpg.log.info("'" + worldName + "' " + type + " 템플릿을 'plugins/Rpg/worlds/" + type + "/' 폴더로 이동했습니다.");
                    return true;
                } else {
                    Rpg.log.severe("'" + worldName + "' 템플릿 폴더 이동에 실패했습니다.");
                    deleteWorldFolder(sourceFolder);
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * [수정] 지정된 타입의 템플릿을 복사하여 인스턴스 월드를 생성합니다.
     */
    public static void copyAndLoadWorld(String newWorldName, String templateName, String type, Consumer<World> callback) {
        Rpg plugin = Rpg.getInstance();
        new BukkitRunnable() {
            @Override
            public void run() {
                // [핵심] 이제 템플릿을 'worlds/[type]/' 폴더 안에서 찾습니다.
                File templateFolder = new File(new File(plugin.getDataFolder(), "worlds/" + type), templateName);
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
                        WorldCreator wc = new WorldCreator(newWorldName);
                        wc.generator("VoidGen");
                        callback.accept(wc.createWorld());
                    }
                }.runTask(plugin);
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * [수정] 수정용 월드의 변경사항을 원본 템플릿에 덮어씌워 저장합니다.
     */
    public static void saveAndOverwriteTemplate(World editWorld, String templateName, String type, Consumer<Boolean> callback) {
        if (editWorld == null) {
            callback.accept(false);
            return;
        }
        String editWorldName = editWorld.getName();
        File editWorldFolder = editWorld.getWorldFolder();
        File templateFolder = new File(Rpg.getInstance().getDataFolder(), "worlds/" + type + "/" + templateName);

        if (!Bukkit.unloadWorld(editWorld, true)) {
            Rpg.log.severe("맵 저장 실패: 수정용 월드 '" + editWorldName + "' 언로드에 실패했습니다.");
            callback.accept(false);
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                deleteWorldFolder(templateFolder);
                copyWorldFolder(editWorldFolder, templateFolder);
                deleteWorldFolder(editWorldFolder);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        callback.accept(true);
                    }
                }.runTask(Rpg.getInstance());
            }
        }.runTaskAsynchronously(Rpg.getInstance());
    }


    /**
     * 월드 폴더를 복사하고, 복사된 월드를 서버에 로드합니다.
     * @param newWorldName 새로 생성될 월드의 이름
     * @param templateName 복사할 원본 템플릿 월드의 이름
     * @param callback 월드 로드가 완료된 후 실행될 작업
     */
    public static void copyAndLoadWorld(String newWorldName, String templateName, Consumer<World> callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                File templateFolder = new File(Rpg.getInstance().getDataFolder(), "worlds/" + templateName);
                File instanceFolder = new File(Bukkit.getWorldContainer(), newWorldName);

                if (!templateFolder.exists() || !templateFolder.isDirectory()) {
                    new BukkitRunnable() {
                        @Override
                        public void run() { callback.accept(null); }
                    }.runTask(Rpg.getInstance());
                    return;
                }

                copyWorldFolder(templateFolder, instanceFolder);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // [핵심] 복사된 월드를 로드할 때도, 만약의 사태에 대비해 공허 생성기를 지정합니다.
                        WorldCreator wc = new WorldCreator(newWorldName);
                        wc.generator("VoidGen");

                        callback.accept(wc.createWorld());
                    }
                }.runTask(Rpg.getInstance());
            }
        }.runTaskAsynchronously(Rpg.getInstance());
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