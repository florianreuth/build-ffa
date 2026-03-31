/*
 * This file is part of build-ffa - https://github.com/florianreuth/build-ffa
 * Copyright (C) 2026 Florian Reuth <git@florianreuth.de> and contributors
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

package de.florianreuth.buildffa.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public final class Branding {

    public static final Component PREFIX = Component
        .text("[", NamedTextColor.DARK_GRAY)
        .append(Component.text("BuildFFA", NamedTextColor.GOLD))
        .append(Component.text("] ", NamedTextColor.DARK_GRAY));

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private Branding() {}

    public static void send(CommandSender sender, Component message) {
        sender.sendMessage(PREFIX.append(message));
    }

    public static Component legacy(String text) {
        return LEGACY.deserialize(text == null ? "" : text);
    }
}

