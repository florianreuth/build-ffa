/*
 * This file is part of build-ffa - https://github.com/florianreuth/build-ffa
 * Copyright (C) 2026 Florian Reuth <git@florianreuth.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.florianreuth.buildffa.command;

import de.florianreuth.buildffa.service.GadgetService;
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

        if (!(sender instanceof final Player player)) {
            return Collections.emptyList();
        }

        final List<String> suggestions = new ArrayList<>(gadgetService.getAvailable(player).keySet());
        suggestions.add("list");
        return suggestions;
    }

}

