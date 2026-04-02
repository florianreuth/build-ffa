package de.florianreuth.buildffa.service;

import de.florianreuth.buildffa.BuildFFA;
import de.florianreuth.buildffa.model.Kit;
import de.florianreuth.buildffa.model.PlayerStats;
import de.florianreuth.buildffa.util.Branding;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

public final class MatchService {

    private static final long COMBAT_TAG_MILLIS = 10_000L;

    private final BuildFFA plugin;
    private final KitService kitService;
    private final ArenaService arenaService;
    private final PlayerDataService playerDataService;
    private final Map<UUID, Long> combatTags = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> recentOpponents = new ConcurrentHashMap<>();
    private BukkitTask autosaveTask;

    public MatchService(final BuildFFA plugin, final KitService kitService, final ArenaService arenaService, final PlayerDataService playerDataService) {
        this.plugin = plugin;
        this.kitService = kitService;
        this.arenaService = arenaService;
        this.playerDataService = playerDataService;
    }

    public void startAutosaveTask() {
        stopAutosaveTask();

        final long intervalTicks = Math.max(60, plugin.getConfig().getLong("autosave-seconds", 120L)) * 20L;
        autosaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
            plugin,
            playerDataService::save,
            intervalTicks,
            intervalTicks
        );
    }

    public void stopAutosaveTask() {
        if (autosaveTask != null) {
            autosaveTask.cancel();
            autosaveTask = null;
        }
    }

    public Location resolveSpawn(final Player player) {
        return arenaService.getRandomSpawn().orElseGet(() -> player.getWorld().getSpawnLocation().clone());
    }

    public void preparePlayer(final Player player, final boolean teleport) {
        if (teleport) {
            player.teleportAsync(resolveSpawn(player));
        }

        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20F);
        player.setFallDistance(0F);
        player.setFireTicks(0);

        final PlayerStats stats = playerDataService.get(player.getUniqueId());
        Kit selected = null;
        if (stats.getSelectedKit() != null) {
            selected = kitService.getKit(stats.getSelectedKit()).orElse(null);
        }
        if (selected == null || !selected.canUse(player)) {
            selected = kitService.getDefaultKit().orElse(null);
        }

        if (selected != null) {
            kitService.applyKit(player, selected);
            stats.setSelectedKit(selected.id());
        }
    }

    public int handleKill(final Player killer) {
        final PlayerStats stats = playerDataService.get(killer.getUniqueId());
        final int currentStreak = stats.recordKill();

        if (plugin.getConfig().getBoolean("rewards.heal-on-kill", true)) {
            killer.setHealth(killer.getMaxHealth());
            killer.setFoodLevel(20);
        }

        final int rewardEvery = Math.max(1, plugin.getConfig().getInt("rewards.golden-apple-every-streak", 5));
        if (currentStreak % rewardEvery == 0) {
            killer.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 1));
            Branding.send(killer, Component.text("Killstreak reward: +1 Golden Apple", NamedTextColor.GOLD));
        }

        final int announceEvery = Math.max(1, plugin.getConfig().getInt("killstreak-announce-every", 5));
        if (currentStreak % announceEvery == 0) {
            Bukkit.getServer().broadcast(Branding.PREFIX.append(Component.text(killer.getName(), NamedTextColor.RED))
                .append(Component.text(" is on a ", NamedTextColor.YELLOW))
                .append(Component.text(currentStreak, NamedTextColor.GOLD))
                .append(Component.text(" kill streak!", NamedTextColor.YELLOW)));
        }

        return currentStreak;
    }

    public void handleDeath(final Player victim, final Player killer) {
        playerDataService.get(victim.getUniqueId()).recordDeath();
        clearCombatState(victim.getUniqueId());
        if (killer != null && !killer.getUniqueId().equals(victim.getUniqueId())) {
            clearCombatStateIfLinked(killer.getUniqueId(), victim.getUniqueId());
        }
    }

    public void tagCombat(final Player first, final Player second) {
        final long now = System.currentTimeMillis();
        combatTags.put(first.getUniqueId(), now);
        combatTags.put(second.getUniqueId(), now);
        recentOpponents.put(first.getUniqueId(), second.getUniqueId());
        recentOpponents.put(second.getUniqueId(), first.getUniqueId());
    }

    public Player resolveRecentAttacker(final Player victim) {
        final UUID opponentId = recentOpponents.get(victim.getUniqueId());
        if (opponentId == null || opponentId.equals(victim.getUniqueId())) {
            return null;
        }

        final Long taggedAt = combatTags.get(victim.getUniqueId());
        if (taggedAt == null) {
            clearCombatState(victim.getUniqueId());
            return null;
        }

        final long age = System.currentTimeMillis() - taggedAt;
        if (age > COMBAT_TAG_MILLIS) {
            clearCombatState(victim.getUniqueId());
            return null;
        }

        final Player attacker = Bukkit.getPlayer(opponentId);
        if (attacker == null || !attacker.isOnline()) {
            return null;
        } else {
            return attacker;
        }
    }

    public boolean isInCombat(final Player player) {
        final Long taggedAt = combatTags.get(player.getUniqueId());
        if (taggedAt == null) {
            return false;
        }

        final long age = System.currentTimeMillis() - taggedAt;
        if (age > COMBAT_TAG_MILLIS) {
            clearCombatState(player.getUniqueId());
            return false;
        }

        return true;
    }

    public void clearCombatState(final Player player) {
        clearCombatState(player.getUniqueId());
    }

    public long getCombatMillisLeft(final Player player) {
        final Long taggedAt = combatTags.get(player.getUniqueId());
        if (taggedAt == null) {
            return 0L;
        }

        final long left = COMBAT_TAG_MILLIS - (System.currentTimeMillis() - taggedAt);
        return Math.max(0L, left);
    }

    private void clearCombatState(final UUID playerId) {
        combatTags.remove(playerId);

        final UUID opponentId = recentOpponents.remove(playerId);
        if (opponentId == null) {
            return;
        }

        if (playerId.equals(recentOpponents.get(opponentId))) {
            recentOpponents.remove(opponentId);
        }
    }

    private void clearCombatStateIfLinked(final UUID playerId, final UUID expectedOpponentId) {
        if (expectedOpponentId.equals(recentOpponents.get(playerId))) {
            clearCombatState(playerId);
        }
    }

}

