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

