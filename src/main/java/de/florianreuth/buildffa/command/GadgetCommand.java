package de.florianreuth.buildffa.command;

import de.florianreuth.buildffa.service.GadgetService;
import de.florianreuth.buildffa.service.GadgetService.GadgetDefinition;
import de.florianreuth.buildffa.util.Branding;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class GadgetCommand implements CommandExecutor, TabCompleter {

    private final GadgetService gadgetService;

    public GadgetCommand(GadgetService gadgetService) {
        this.gadgetService = gadgetService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof final Player player)) {
            Branding.send(sender, Component.text("Only players can select gadgets.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0 || "list".equalsIgnoreCase(args[0])) {
            final List<String> names = gadgetService.getAvailable(player).keySet().stream().toList();
            if (names.isEmpty()) {
                Branding.send(player, Component.text("No gadgets available for your permissions.", NamedTextColor.RED));
                return true;
            }

            Branding.send(player, Component.text("Available gadgets: " + String.join(", ", names), NamedTextColor.GOLD));
            return true;
        }

        final String gadgetId = args[0].toLowerCase(Locale.ROOT);
        if (!gadgetService.select(player, gadgetId)) {
            Branding.send(player, Component.text("Unknown or unavailable gadget. Use /gadget list", NamedTextColor.RED));
            return true;
        }

        gadgetService.getSelected(player).ifPresent(selected ->
            Branding.send(player, Component.text("Selected gadget: " + selected.displayName(), NamedTextColor.GREEN))
        );
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }

        final String prefix = args[0].toLowerCase(Locale.ROOT);
        final List<String> suggestions = new ArrayList<>();
        if (!(sender instanceof final Player player)) {
            return Collections.emptyList();
        }

        for (final String name : gadgetService.getAvailable(player).keySet()) {
            if (name.startsWith(prefix)) {
                suggestions.add(name);
            }
        }
        if ("list".startsWith(prefix)) {
            suggestions.add("list");
        }
        return suggestions;
    }
}

