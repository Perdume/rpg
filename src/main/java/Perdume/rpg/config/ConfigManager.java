package Perdume.rpg.config;

import Perdume.rpg.Rpg;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final Rpg plugin;
    private FileConfiguration locationsConfig;
    private File locationsFile;

    // 로드된 위치 정보를 메모리에 저장해두어, 필요할 때마다 빠르게 사용
    private final Map<String, Location> spawnLocations = new HashMap<>();

    public ConfigManager(Rpg plugin) {
        this.plugin = plugin;
        loadLocations();
    }

    public void loadLocations() {
        locationsFile = new File(plugin.getDataFolder(), "locations.yml");
        if (!locationsFile.exists()) {
            plugin.saveResource("locations.yml", false); // jar 내의 기본 파일을 복사
        }
        locationsConfig = YamlConfiguration.loadConfiguration(locationsFile);

        // 'spawn-locations' 섹션을 읽어와 메모리에 저장
        ConfigurationSection section = locationsConfig.getConfigurationSection("spawn-locations");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String[] coords = section.getString(key + ".coordinates").split(", ");
                if (coords.length == 6) {
                    Location loc = new Location(
                            Bukkit.getWorld(coords[0]),
                            Double.parseDouble(coords[1]),
                            Double.parseDouble(coords[2]),
                            Double.parseDouble(coords[3]),
                            Float.parseFloat(coords[4]),
                            Float.parseFloat(coords[5])
                    );
                    spawnLocations.put(key, loc);
                }
            }
            Rpg.log.info(spawnLocations.size() + "개의 스폰 위치를 불러왔습니다.");
        }
    }

    public FileConfiguration getLocationsConfig() {
        return locationsConfig;
    }

    public Location getSpawnLocation(String id) {
        return spawnLocations.get(id);
    }
}