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
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public final class MatchService {

    private static final long COMBAT_TAG_MILLIS = 10_000L;

    private final BuildFFA plugin;
    private final KitService kitService;
    private final ArenaService arenaService;
    private final PlayerDataService playerDataService;
    private final Map<UUID, Long> combatTags = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> lastAttackers = new ConcurrentHashMap<>();
    private BukkitTask autosaveTask;

    public MatchService(BuildFFA plugin, KitService kitService, ArenaService arenaService, PlayerDataService playerDataService) {
        this.plugin = plugin;
        this.kitService = kitService;
        this.arenaService = arenaService;
        this.playerDataService = playerDataService;
    }

    public void startAutosaveTask() {
        stopAutosaveTask();
        long intervalTicks = Math.max(60, plugin.getConfig().getLong("autosave-seconds", 120L)) * 20L;
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

    public Location resolveSpawn(Player player) {
        return arenaService
            .getRandomSpawn()
            .orElseGet(() -> player.getWorld().getSpawnLocation().clone());
    }

    public void preparePlayer(Player player, boolean teleport) {
        if (teleport) {
            player.teleportAsync(resolveSpawn(player));
        }

        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFallDistance(0f);
        player.setFireTicks(0);

        PlayerStats stats = playerDataService.get(player.getUniqueId());
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

    public int handleKill(Player killer) {
        PlayerStats stats = playerDataService.get(killer.getUniqueId());
        int currentStreak = stats.recordKill();

        if (plugin.getConfig().getBoolean("rewards.heal-on-kill", true)) {
            killer.setHealth(killer.getMaxHealth());
            killer.setFoodLevel(20);
        }

        int rewardEvery = Math.max(1, plugin.getConfig().getInt("rewards.golden-apple-every-streak", 5));
        if (currentStreak % rewardEvery == 0) {
            killer.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLDEN_APPLE, 1));
            Branding.send(killer, Component.text("Killstreak reward: +1 Golden Apple", NamedTextColor.GOLD));
        }

        int announceEvery = Math.max(1, plugin.getConfig().getInt("killstreak-announce-every", 5));
        if (currentStreak % announceEvery == 0) {
            Bukkit.getServer().broadcast(
                Branding.PREFIX
                    .append(Component.text(killer.getName(), NamedTextColor.RED))
                    .append(Component.text(" is on a ", NamedTextColor.YELLOW))
                    .append(Component.text(currentStreak, NamedTextColor.GOLD))
                    .append(Component.text(" kill streak!", NamedTextColor.YELLOW))
            );
        }

        return currentStreak;
    }

    public void handleDeath(Player victim) {
        playerDataService.get(victim.getUniqueId()).recordDeath();
        combatTags.remove(victim.getUniqueId());
        lastAttackers.remove(victim.getUniqueId());
    }

    public void tagCombat(Player first, Player second) {
        long now = System.currentTimeMillis();
        combatTags.put(first.getUniqueId(), now);
        combatTags.put(second.getUniqueId(), now);
        lastAttackers.put(second.getUniqueId(), first.getUniqueId());
    }

    public Player resolveRecentAttacker(Player victim) {
        UUID attackerId = lastAttackers.get(victim.getUniqueId());
        if (attackerId == null || attackerId.equals(victim.getUniqueId())) {
            return null;
        }

        Long taggedAt = combatTags.get(victim.getUniqueId());
        if (taggedAt == null) {
            lastAttackers.remove(victim.getUniqueId());
            return null;
        }

        long age = System.currentTimeMillis() - taggedAt;
        if (age > COMBAT_TAG_MILLIS) {
            combatTags.remove(victim.getUniqueId());
            lastAttackers.remove(victim.getUniqueId());
            return null;
        }

        Player attacker = Bukkit.getPlayer(attackerId);
        if (attacker == null || !attacker.isOnline()) {
            return null;
        }
        return attacker;
    }

    public boolean isInCombat(Player player) {
        Long taggedAt = combatTags.get(player.getUniqueId());
        if (taggedAt == null) {
            return false;
        }

        long age = System.currentTimeMillis() - taggedAt;
        if (age > COMBAT_TAG_MILLIS) {
            combatTags.remove(player.getUniqueId());
            lastAttackers.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    public long getCombatMillisLeft(Player player) {
        Long taggedAt = combatTags.get(player.getUniqueId());
        if (taggedAt == null) {
            return 0L;
        }
        long left = COMBAT_TAG_MILLIS - (System.currentTimeMillis() - taggedAt);
        return Math.max(0L, left);
    }
}

