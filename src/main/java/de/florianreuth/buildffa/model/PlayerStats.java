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

package de.florianreuth.buildffa.model;

public final class PlayerStats {

    private int kills;
    private int deaths;
    private int currentKillStreak;
    private int bestKillStreak;
    private String selectedKit;
    private String selectedGadget;

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public int getCurrentKillStreak() {
        return currentKillStreak;
    }

    public void setCurrentKillStreak(int currentKillStreak) {
        this.currentKillStreak = currentKillStreak;
    }

    public int getBestKillStreak() {
        return bestKillStreak;
    }

    public void setBestKillStreak(int bestKillStreak) {
        this.bestKillStreak = bestKillStreak;
    }

    public String getSelectedKit() {
        return selectedKit;
    }

    public void setSelectedKit(String selectedKit) {
        this.selectedKit = selectedKit;
    }

    public String getSelectedGadget() {
        return selectedGadget;
    }

    public void setSelectedGadget(String selectedGadget) {
        this.selectedGadget = selectedGadget;
    }

    public int recordKill() {
        kills++;
        currentKillStreak++;
        if (currentKillStreak > bestKillStreak) {
            bestKillStreak = currentKillStreak;
        }
        return currentKillStreak;
    }

    public void recordDeath() {
        deaths++;
        currentKillStreak = 0;
    }

    public double getKdr() {
        if (deaths == 0) {
            return kills;
        }
        return (double) kills / deaths;
    }

}

