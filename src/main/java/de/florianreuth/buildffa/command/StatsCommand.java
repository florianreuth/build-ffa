package de.florianreuth.buildffa.command;

import de.florianreuth.buildffa.model.PlayerStats;
import de.florianreuth.buildffa.service.PlayerDataService;
import de.florianreuth.buildffa.util.Branding;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class StatsCommand implements CommandExecutor, TabCompleter {

    private final PlayerDataService playerDataService;

    public StatsCommand(PlayerDataService playerDataService) {
        this.playerDataService = playerDataService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player target;
        if (args.length == 0) {
            if (!(sender instanceof final Player player)) {
                Branding.send(sender, Component.text("Usage: /ffastats <player>", NamedTextColor.YELLOW));
                return true;
            }
            target = player;
        } else {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                Branding.send(sender, Component.text("Player is not online.", NamedTextColor.RED));
                return true;
            }
        }

        final PlayerStats stats = playerDataService.get(target.getUniqueId());
        Branding.send(
            sender,
            Component.text("Stats for ", NamedTextColor.GOLD)
                .append(Component.text(target.getName(), NamedTextColor.YELLOW))
                .append(Component.text(":", NamedTextColor.GOLD))
        );
        Branding.send(sender, Component.text("Kills: ", NamedTextColor.GRAY).append(Component.text(stats.getKills(), NamedTextColor.GREEN)));
        Branding.send(sender, Component.text("Deaths: ", NamedTextColor.GRAY).append(Component.text(stats.getDeaths(), NamedTextColor.RED)));
        Branding.send(
            sender,
            Component.text("K/D: ", NamedTextColor.GRAY)
                .append(Component.text(String.format(Locale.US, "%.2f", stats.getKdr()), NamedTextColor.YELLOW))
        );
        Branding.send(sender, Component.text("Best Streak: ", NamedTextColor.GRAY).append(Component.text(stats.getBestKillStreak(), NamedTextColor.AQUA)));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }

        final String prefix = args[0].toLowerCase(Locale.ROOT);
        final List<String> suggestions = new ArrayList<>();
        for (final Player online : Bukkit.getOnlinePlayers()) {
            String name = online.getName();
            if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                suggestions.add(name);
            }
        }
        return suggestions;
    }
}

