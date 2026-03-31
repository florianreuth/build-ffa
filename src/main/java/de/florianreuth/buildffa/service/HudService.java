package de.florianreuth.buildffa.service;

import de.florianreuth.buildffa.BuildFFA;
import de.florianreuth.buildffa.model.PlayerStats;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public final class HudService {

    private final BuildFFA plugin;
    private final PlayerDataService playerDataService;
    private final MatchService matchService;
    private final GadgetService gadgetService;
    private BukkitTask task;

    public HudService(final BuildFFA plugin, final PlayerDataService playerDataService, final MatchService matchService, final GadgetService gadgetService) {
        this.plugin = plugin;
        this.playerDataService = playerDataService;
        this.matchService = matchService;
        this.gadgetService = gadgetService;
    }

    public void start() {
        stop();

        final long intervalTicks = Math.max(10L, plugin.getConfig().getLong("hud.update-ticks", 20L));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshOnlinePlayers, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void refreshPlayer(final Player player) {
        updateScoreboard(player);
        updateTab(player);
        updateActionBar(player);
    }

    private void refreshOnlinePlayers() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            refreshPlayer(player);
        }
    }

    private void updateScoreboard(final Player player) {
        if (!plugin.getConfig().getBoolean("hud.scoreboard-enabled", true)) {
            return;
        }

        final PlayerStats stats = playerDataService.get(player.getUniqueId());
        final Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        final Objective objective = scoreboard.registerNewObjective("buildffa", Criteria.DUMMY, Component.text("BuildFFA", NamedTextColor.GOLD));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        objective.getScore("§8 ").setScore(8);
        objective.getScore("§7Kills: §a" + stats.getKills()).setScore(7);
        objective.getScore("§7Deaths: §c" + stats.getDeaths()).setScore(6);
        objective.getScore("§7K/D: §e" + String.format(Locale.US, "%.2f", stats.getKdr())).setScore(5);
        objective.getScore("§7Streak: §b" + stats.getCurrentKillStreak()).setScore(4);
        objective.getScore("§7Best: §d" + stats.getBestKillStreak()).setScore(3);
        objective.getScore("§8  ").setScore(2);
        objective.getScore("§7Kit: §f" + valueOrNone(stats.getSelectedKit())).setScore(1);

        player.setScoreboard(scoreboard);
    }

    private void updateTab(final Player player) {
        if (!plugin.getConfig().getBoolean("hud.tab-enabled", true)) {
            return;
        }

        final PlayerStats stats = playerDataService.get(player.getUniqueId());
        final List<Map.Entry<UUID, PlayerStats>> top = playerDataService.getTopByKills(3);

        final Component header = Component
            .text("BuildFFA", NamedTextColor.GOLD)
            .append(Component.newline())
            .append(Component.text("Online: " + Bukkit.getOnlinePlayers().size(), NamedTextColor.GRAY));

        Component footer = Component.text("K: " + stats.getKills() + " D: " + stats.getDeaths(), NamedTextColor.GREEN);
        if (!top.isEmpty()) {
            final Map.Entry<UUID, PlayerStats> first = top.getFirst();
            String topName = Bukkit.getOfflinePlayer(first.getKey()).getName();
            if (topName == null) {
                topName = "unknown";
            }
            footer = footer
                .append(Component.newline())
                .append(Component.text("Top: " + topName + " (" + first.getValue().getKills() + " kills)", NamedTextColor.YELLOW));
        }

        player.sendPlayerListHeaderAndFooter(header, footer);
    }

    private void updateActionBar(Player player) {
        if (!plugin.getConfig().getBoolean("hud.actionbar-enabled", true)) {
            return;
        }

        final PlayerStats stats = playerDataService.get(player.getUniqueId());
        final long combatLeft = matchService.getCombatMillisLeft(player);
        final long gadgetCooldown = gadgetService.getSelectedCooldownMillisLeft(player);

        final String combatText = combatLeft > 0L ? (combatLeft / 1000L) + "s" : "ready";
        final String gadgetText = gadgetCooldown > 0L ? (gadgetCooldown / 1000L) + "s" : "ready";

        player.sendActionBar(
            Component.text(
                "Streak " + stats.getCurrentKillStreak() +
                    " | Combat " + combatText +
                    " | Gadget " + gadgetText,
                NamedTextColor.AQUA
            )
        );
    }

    private static String valueOrNone(final String value) {
        return value == null || value.isBlank() ? "none" : value;
    }

}

