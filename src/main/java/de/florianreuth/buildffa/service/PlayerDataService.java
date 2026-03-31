package de.florianreuth.buildffa.service;

import de.florianreuth.buildffa.BuildFFA;
import de.florianreuth.buildffa.model.PlayerStats;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.file.YamlConfiguration;

public final class PlayerDataService {

    private final BuildFFA plugin;
    private final File file;
    private final Map<UUID, PlayerStats> statsByPlayer = new ConcurrentHashMap<>();

    public PlayerDataService(BuildFFA plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
    }

    public void load() {
        statsByPlayer.clear();
        if (!file.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            PlayerStats stats = new PlayerStats();
            stats.setKills(config.getInt(key + ".kills", 0));
            stats.setDeaths(config.getInt(key + ".deaths", 0));
            stats.setCurrentKillStreak(config.getInt(key + ".currentKillStreak", 0));
            stats.setBestKillStreak(config.getInt(key + ".bestKillStreak", 0));
            stats.setSelectedKit(config.getString(key + ".selectedKit"));
            stats.setSelectedGadget(config.getString(key + ".selectedGadget"));
            statsByPlayer.put(uuid, stats);
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerStats> entry : statsByPlayer.entrySet()) {
            String key = entry.getKey().toString();
            PlayerStats stats = entry.getValue();
            config.set(key + ".kills", stats.getKills());
            config.set(key + ".deaths", stats.getDeaths());
            config.set(key + ".currentKillStreak", stats.getCurrentKillStreak());
            config.set(key + ".bestKillStreak", stats.getBestKillStreak());
            config.set(key + ".selectedKit", stats.getSelectedKit());
            config.set(key + ".selectedGadget", stats.getSelectedGadget());
        }

        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save stats.yml: " + exception.getMessage());
        }
    }

    public PlayerStats get(UUID uuid) {
        return statsByPlayer.computeIfAbsent(uuid, ignored -> new PlayerStats());
    }

    public List<Map.Entry<UUID, PlayerStats>> getTopByKills(int limit) {
        return statsByPlayer
            .entrySet()
            .stream()
            .sorted(Comparator.comparingInt(entry -> -entry.getValue().getKills()))
            .limit(Math.max(1, limit))
            .toList();
    }
}

