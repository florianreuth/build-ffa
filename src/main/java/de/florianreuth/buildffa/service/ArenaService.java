package de.florianreuth.buildffa.service;

import de.florianreuth.buildffa.BuildFFA;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ArenaService {

    private final BuildFFA plugin;
    private final File file;
    private final List<Location> spawns = new ArrayList<>();

    public ArenaService(BuildFFA plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "arenas.yml");
    }

    public void load() {
        spawns.clear();
        if (!file.exists()) {
            return;
        }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = configuration.getConfigurationSection("spawns");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection spawnSection = section.getConfigurationSection(key);
            if (spawnSection == null) {
                continue;
            }
            Location location = deserializeLocation(spawnSection);
            if (location != null) {
                spawns.add(location);
            }
        }
    }

    public void addSpawn(Location location) {
        spawns.add(location.clone());
        save();
    }

    public Optional<Location> getRandomSpawn() {
        if (spawns.isEmpty()) {
            return Optional.empty();
        }
        int index = ThreadLocalRandom.current().nextInt(spawns.size());
        return Optional.of(spawns.get(index).clone());
    }

    public int getSpawnCount() {
        return spawns.size();
    }

    public boolean isInSpawnProtection(Location location, double radius) {
        if (location == null || location.getWorld() == null || spawns.isEmpty() || radius <= 0.0) {
            return false;
        }

        double radiusSquared = radius * radius;
        for (Location spawn : spawns) {
            if (spawn.getWorld() == null || !spawn.getWorld().equals(location.getWorld())) {
                continue;
            }
            if (spawn.distanceSquared(location) <= radiusSquared) {
                return true;
            }
        }
        return false;
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (int index = 0; index < spawns.size(); index++) {
            Location location = spawns.get(index);
            String path = "spawns." + index;
            serializeLocation(config, path, location);
        }

        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save arenas.yml: " + exception.getMessage());
        }
    }

    private static void serializeLocation(YamlConfiguration config, String path, Location location) {
        config.set(path + ".world", location.getWorld() == null ? null : location.getWorld().getName());
        config.set(path + ".x", location.getX());
        config.set(path + ".y", location.getY());
        config.set(path + ".z", location.getZ());
        config.set(path + ".yaw", location.getYaw());
        config.set(path + ".pitch", location.getPitch());
    }

    private static Location deserializeLocation(ConfigurationSection section) {
        String worldName = section.getString("world");
        if (worldName == null) {
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }

        return new Location(
            world,
            section.getDouble("x"),
            section.getDouble("y"),
            section.getDouble("z"),
            (float) section.getDouble("yaw"),
            (float) section.getDouble("pitch")
        );
    }
}

