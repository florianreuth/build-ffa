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

package de.florianreuth.buildffa.service;

import de.florianreuth.buildffa.BuildFFA;
import de.florianreuth.buildffa.model.PlayerStats;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.file.YamlConfiguration;

public final class PlayerDataService {

    private final BuildFFA plugin;
    private final File file;
    private final Map<UUID, PlayerStats> statsByPlayer = new ConcurrentHashMap<>();

    public PlayerDataService(final BuildFFA plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
    }

    public void load() {
        statsByPlayer.clear();
        if (!file.exists()) {
            return;
        }

        final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (final String key : config.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            final PlayerStats stats = new PlayerStats();
            stats.setKills(config.getInt(key + ".kills", 0));
            stats.setDeaths(config.getInt(key + ".deaths", 0));
            stats.setCurrentKillStreak(config.getInt(key + ".currentKillStreak", 0));
            stats.setBestKillStreak(config.getInt(key + ".bestKillStreak", 0));
            stats.setSelectedKit(config.getString(key + ".selectedKit"));
            stats.setSelectedGadget(config.getString(key + ".selectedGadget"));
            statsByPlayer.put(uuid, stats);
        }
    }

    public void save() {
        final YamlConfiguration config = new YamlConfiguration();
        for (final Map.Entry<UUID, PlayerStats> entry : statsByPlayer.entrySet()) {
            final String key = entry.getKey().toString();
            final PlayerStats stats = entry.getValue();
            config.set(key + ".kills", stats.getKills());
            config.set(key + ".deaths", stats.getDeaths());
            config.set(key + ".currentKillStreak", stats.getCurrentKillStreak());
            config.set(key + ".bestKillStreak", stats.getBestKillStreak());
            config.set(key + ".selectedKit", stats.getSelectedKit());
            config.set(key + ".selectedGadget", stats.getSelectedGadget());
        }

        try {
            config.save(file);
        } catch (final IOException exception) {
            plugin.getLogger().severe("Could not save stats.yml: " + exception.getMessage());
        }
    }

    public PlayerStats get(final UUID uuid) {
        return statsByPlayer.computeIfAbsent(uuid, ignored -> new PlayerStats());
    }

    public List<Map.Entry<UUID, PlayerStats>> getTopByKills(final int limit) {
        return statsByPlayer
            .entrySet()
            .stream()
            .sorted(Comparator.comparingInt(entry -> -entry.getValue().getKills()))
            .limit(Math.max(1, limit))
            .toList();
    }

}

