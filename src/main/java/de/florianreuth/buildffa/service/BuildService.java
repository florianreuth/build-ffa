package de.florianreuth.buildffa.service;

import de.florianreuth.buildffa.BuildFFA;
import java.util.ArrayList;
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

    public BuildService(final BuildFFA plugin) {
        this.plugin = plugin;
    }

    public boolean isBuildDisabled() {
        return !plugin.getConfig().getBoolean("build.enabled", true);
    }

    public boolean isBuildMode(final UUID playerId) {
        return buildModePlayers.contains(playerId);
    }

    public void setBuildMode(final UUID playerId, final boolean enabled) {
        if (enabled) {
            buildModePlayers.add(playerId);
        } else {
            buildModePlayers.remove(playerId);
        }
    }

    public boolean toggleBuildMode(final UUID playerId) {
        final boolean enabled = !isBuildMode(playerId);
        setBuildMode(playerId, enabled);
        return enabled;
    }

    public boolean isInSpawnProtection(final Location location) {
        if (location == null || plugin.getArenaService() == null) {
            return false;
        }

        final double radius = Math.max(0.0, plugin.getConfig().getDouble("spawn-protection.radius", 6.0));
        return plugin.getArenaService().isInSpawnProtection(location, radius);
    }

    public void trackPlacement(final Block block, final BlockData originalState) {
        final Location key = toBlockKey(block.getLocation());
        trackedOriginalStates.computeIfAbsent(key, ignored -> originalState.clone());

        final BukkitTask previous = cleanupTasks.remove(key);
        if (previous != null) {
            previous.cancel();
        }

        final long cleanupSeconds = Math.max(1L, plugin.getConfig().getLong("build.block-cleanup-seconds",
            plugin.getConfig().getLong("build.block-stable-seconds", 10L)));
        final long cleanupTicks = cleanupSeconds * 20L;

        cleanupTasks.put(key, plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            cleanupTasks.remove(key);
            restoreTrackedState(key);
        }, cleanupTicks));
    }

    public boolean canBreak(final Block block) {
        return trackedOriginalStates.containsKey(toBlockKey(block.getLocation()));
    }

    public void handleBreak(final Block block) {
        final Location key = toBlockKey(block.getLocation());
        final BukkitTask task = cleanupTasks.remove(key);
        if (task != null) {
            task.cancel();
        }

        restoreTrackedState(key);
    }

    public void clearTracked(final Block block) {
        final Location key = toBlockKey(block.getLocation());
        trackedOriginalStates.remove(key);
        final BukkitTask task = cleanupTasks.remove(key);
        if (task != null) {
            task.cancel();
        }
    }

    public void resetPendingPlayerChanges() {
        cleanupTasks.values().forEach(BukkitTask::cancel);
        cleanupTasks.clear();

        new ArrayList<>(trackedOriginalStates.keySet()).forEach(this::restoreTrackedState);
        buildModePlayers.clear();
    }

    private void restoreTrackedState(final Location key) {
        final BlockData original = trackedOriginalStates.remove(key);
        if (original == null) {
            return;
        }

        final Block block = key.getBlock();
        if (!block.getBlockData().matches(original)) {
            block.setBlockData(original, true);
        }
    }

    private static Location toBlockKey(final Location source) {
        return new Location(source.getWorld(), source.getBlockX(), source.getBlockY(), source.getBlockZ());
    }

}

