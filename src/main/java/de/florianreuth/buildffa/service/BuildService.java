package de.florianreuth.buildffa.service;

import de.florianreuth.buildffa.BuildFFA;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.scheduler.BukkitTask;

public final class BuildService {

    private final BuildFFA plugin;
    private final Map<Location, BlockData> trackedOriginalStates = new ConcurrentHashMap<>();
    private final Set<UUID> buildModePlayers = ConcurrentHashMap.newKeySet();
    private final Map<Location, BukkitTask> cleanupTasks = new ConcurrentHashMap<>();

    public BuildService(BuildFFA plugin) {
        this.plugin = plugin;
    }

    public boolean isBuildEnabled() {
        return plugin.getConfig().getBoolean("build.enabled", true);
    }

    public boolean isBuildMode(UUID playerId) {
        return buildModePlayers.contains(playerId);
    }

    public void setBuildMode(UUID playerId, boolean enabled) {
        if (enabled) {
            buildModePlayers.add(playerId);
            return;
        }
        buildModePlayers.remove(playerId);
    }

    public boolean toggleBuildMode(UUID playerId) {
        boolean enabled = !isBuildMode(playerId);
        setBuildMode(playerId, enabled);
        return enabled;
    }

    public boolean isInSpawnProtection(Location location) {
        if (location == null || plugin.getArenaService() == null) {
            return false;
        }
        double radius = Math.max(0.0, plugin.getConfig().getDouble("spawn-protection.radius", 6.0));
        return plugin.getArenaService().isInSpawnProtection(location, radius);
    }

    public void trackPlacement(Block block, BlockData originalState) {
        Location key = toBlockKey(block.getLocation());
        trackedOriginalStates.computeIfAbsent(key, ignored -> originalState.clone());

        BukkitTask previous = cleanupTasks.remove(key);
        if (previous != null) {
            previous.cancel();
        }

        long cleanupSeconds = Math.max(
            1L,
            plugin.getConfig().getLong("build.block-cleanup-seconds", plugin.getConfig().getLong("build.block-stable-seconds", 10L))
        );
        long cleanupTicks = cleanupSeconds * 20L;

        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            cleanupTasks.remove(key);
            restoreTrackedState(key);
        }, cleanupTicks);

        cleanupTasks.put(key, task);
    }

    public boolean canBreak(Block block) {
        return trackedOriginalStates.containsKey(toBlockKey(block.getLocation()));
    }

    public void handleBreak(Block block) {
        Location key = toBlockKey(block.getLocation());
        BukkitTask task = cleanupTasks.remove(key);
        if (task != null) {
            task.cancel();
        }
        restoreTrackedState(key);
    }

    public void clearTracked(Block block) {
        Location key = toBlockKey(block.getLocation());
        trackedOriginalStates.remove(key);
        BukkitTask task = cleanupTasks.remove(key);
        if (task != null) {
            task.cancel();
        }
    }

    private void restoreTrackedState(Location key) {
        BlockData original = trackedOriginalStates.remove(key);
        if (original == null) {
            return;
        }

        Block block = key.getBlock();
        if (!block.getBlockData().matches(original)) {
            block.setBlockData(original, false);
        }
    }

    private static Location toBlockKey(Location source) {
        org.bukkit.World world = source.getWorld();
        return new Location(world, source.getBlockX(), source.getBlockY(), source.getBlockZ());
    }
}

