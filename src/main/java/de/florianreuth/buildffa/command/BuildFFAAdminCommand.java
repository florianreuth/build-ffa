package de.florianreuth.buildffa.command;

import de.florianreuth.buildffa.BuildFFA;
import de.florianreuth.buildffa.util.Branding;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class BuildFFAAdminCommand implements CommandExecutor, TabCompleter {

    private final BuildFFA plugin;

    public BuildFFAAdminCommand(BuildFFA plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("buildffa.admin")) {
            Branding.send(sender, Component.text("You do not have permission.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            Branding.send(sender, Component.text("Usage: /buildffa <reload|setspawn|info|buildmode>", NamedTextColor.YELLOW));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadRuntimeConfig();
                Branding.send(sender, Component.text("Configuration reloaded.", NamedTextColor.GREEN));
            }
            case "setspawn" -> {
                if (!(sender instanceof final Player player)) {
                    Branding.send(sender, Component.text("Only players can use this sub command.", NamedTextColor.RED));
                    return true;
                }
                plugin.getArenaService().addSpawn(player.getLocation());
                Branding.send(sender, Component.text("Added spawn point #" + plugin.getArenaService().getSpawnCount(), NamedTextColor.GREEN));
            }
            case "info" -> Branding.send(
                sender,
                Component.text("Spawns: ", NamedTextColor.GRAY)
                    .append(Component.text(plugin.getArenaService().getSpawnCount(), NamedTextColor.GOLD))
                    .append(Component.text(" | Kits: ", NamedTextColor.GRAY))
                    .append(Component.text(plugin.getKitService().getKits().size(), NamedTextColor.GOLD))
            );
            case "buildmode" -> {
                if (!(sender instanceof final Player player)) {
                    Branding.send(sender, Component.text("Only players can use this sub command.", NamedTextColor.RED));
                    return true;
                }

                boolean enabled;
                if (args.length >= 2) {
                    final String mode = args[1].toLowerCase();
                    switch (mode) {
                        case "on" -> enabled = true;
                        case "off" -> enabled = false;
                        case "toggle" -> {
                            enabled = plugin.getBuildService().toggleBuildMode(player.getUniqueId());
                            Branding.send(
                                player,
                                Component.text("Build mode ", NamedTextColor.GRAY)
                                    .append(Component.text(enabled ? "enabled" : "disabled", enabled ? NamedTextColor.GREEN : NamedTextColor.RED))
                                    .append(Component.text(".", NamedTextColor.GRAY))
                            );
                            return true;
                        }
                        case "status" -> {
                            enabled = plugin.getBuildService().isBuildMode(player.getUniqueId());
                            Branding.send(
                                player,
                                Component.text("Build mode is currently ", NamedTextColor.GRAY)
                                    .append(Component.text(enabled ? "ON" : "OFF", enabled ? NamedTextColor.GREEN : NamedTextColor.RED))
                                    .append(Component.text(".", NamedTextColor.GRAY))
                            );
                            return true;
                        }
                        default -> {
                            Branding.send(sender, Component.text("Usage: /buildffa buildmode [on|off|toggle|status]", NamedTextColor.YELLOW));
                            return true;
                        }
                    }
                    plugin.getBuildService().setBuildMode(player.getUniqueId(), enabled);
                } else {
                    enabled = plugin.getBuildService().toggleBuildMode(player.getUniqueId());
                }

                Branding.send(
                    player,
                    Component.text("Build mode ", NamedTextColor.GRAY)
                        .append(Component.text(enabled ? "enabled" : "disabled", enabled ? NamedTextColor.GREEN : NamedTextColor.RED))
                        .append(Component.text(".", NamedTextColor.GRAY))
                );
            }
            default -> Branding.send(sender, Component.text("Usage: /buildffa <reload|setspawn|info|buildmode>", NamedTextColor.YELLOW));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("buildffa.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            final String prefix = args[0].toLowerCase();
            final List<String> suggestions = new ArrayList<>();
            for (final String option : List.of("reload", "setspawn", "info", "buildmode")) {
                if (option.startsWith(prefix)) {
                    suggestions.add(option);
                }
            }
            return suggestions;
        }

        if (args.length == 2 && "buildmode".equalsIgnoreCase(args[0])) {
            final String prefix = args[1].toLowerCase();
            final List<String> suggestions = new ArrayList<>();
            for (final String option : List.of("on", "off", "toggle", "status")) {
                if (option.startsWith(prefix)) {
                    suggestions.add(option);
                }
            }
            return suggestions;
        }

        return Collections.emptyList();
    }

}

