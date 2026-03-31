package de.florianreuth.buildffa;

import de.florianreuth.buildffa.command.BuildFFAAdminCommand;
import de.florianreuth.buildffa.command.GadgetCommand;
import de.florianreuth.buildffa.command.KitCommand;
import de.florianreuth.buildffa.command.StatsCommand;
import de.florianreuth.buildffa.listener.BuildListener;
import de.florianreuth.buildffa.listener.CombatListener;
import de.florianreuth.buildffa.listener.GadgetListener;
import de.florianreuth.buildffa.listener.GameplayRulesListener;
import de.florianreuth.buildffa.listener.PlayerLifecycleListener;
import de.florianreuth.buildffa.service.ArenaService;
import de.florianreuth.buildffa.service.BuildService;
import de.florianreuth.buildffa.service.GadgetService;
import de.florianreuth.buildffa.service.HudService;
import de.florianreuth.buildffa.service.KitService;
import de.florianreuth.buildffa.service.MatchService;
import de.florianreuth.buildffa.service.PlayerDataService;
import org.bukkit.GameRules;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public final class BuildFFA extends JavaPlugin {

    private KitService kitService;
    private ArenaService arenaService;
    private PlayerDataService playerDataService;
    private MatchService matchService;
    private BuildService buildService;
    private GadgetService gadgetService;
    private HudService hudService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("kits.yml", false);
        saveResource("arenas.yml", false);

        this.playerDataService = new PlayerDataService(this);
        this.arenaService = new ArenaService(this);
        this.kitService = new KitService(this);
        this.matchService = new MatchService(this, kitService, arenaService, playerDataService);
        this.buildService = new BuildService(this);
        this.gadgetService = new GadgetService(this, playerDataService);
        this.hudService = new HudService(this, playerDataService, matchService, gadgetService);

        playerDataService.load();
        arenaService.load();
        kitService.load();
        gadgetService.load();
        applyGameplayGameRules();

        getServer().getPluginManager().registerEvents(new PlayerLifecycleListener(this, matchService, playerDataService, gadgetService, hudService), this);
        getServer().getPluginManager().registerEvents(new CombatListener(matchService, buildService), this);
        getServer().getPluginManager().registerEvents(new BuildListener(buildService), this);
        getServer().getPluginManager().registerEvents(new GadgetListener(gadgetService, buildService), this);
        getServer().getPluginManager().registerEvents(new GameplayRulesListener(), this);

        final KitCommand kitCommand = new KitCommand(kitService, playerDataService, matchService, gadgetService);
        registerCommand("kit", kitCommand, kitCommand);

        final BuildFFAAdminCommand buildFFAAdminCommand = new BuildFFAAdminCommand(this);
        registerCommand("buildffa", buildFFAAdminCommand, buildFFAAdminCommand);

        final StatsCommand statsCommand = new StatsCommand(playerDataService);
        registerCommand("stats", statsCommand, statsCommand);

        final GadgetCommand gadgetCommand = new GadgetCommand(gadgetService);
        registerCommand("gadget", gadgetCommand, gadgetCommand);

        matchService.startAutosaveTask();
        hudService.start();
        getLogger().info("BuildFFA enabled with " + kitService.getKits().size() + " kits.");
    }

    @Override
    public void onDisable() {
        if (matchService != null) {
            matchService.stopAutosaveTask();
        }
        if (hudService != null) {
            hudService.stop();
        }
        if (playerDataService != null) {
            playerDataService.save();
        }
    }

    public void reloadRuntimeConfig() {
        reloadConfig();
        kitService.reload();
        arenaService.load();
        gadgetService.load();
        applyGameplayGameRules();
        hudService.start();

        getServer().getOnlinePlayers().forEach(player -> {
            matchService.preparePlayer(player, false);
            gadgetService.giveSelectedGadget(player);
            hudService.refreshPlayer(player);
        });
    }

    public KitService getKitService() {
        return kitService;
    }

    public ArenaService getArenaService() {
        return arenaService;
    }

    public BuildService getBuildService() {
        return buildService;
    }

    private void registerCommand(final String commandName, final CommandExecutor executor, final TabCompleter tabCompleter) {
        final PluginCommand command = getCommand(commandName);
        if (command == null) {
            getLogger().warning("Could not register command '/" + commandName + "' (missing in plugin.yml).");
            return;
        }

        command.setExecutor(executor);
        if (tabCompleter != null) {
            command.setTabCompleter(tabCompleter);
        }
    }

    private void applyGameplayGameRules() {
        getServer().getWorlds().forEach(world -> {
            world.setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false);
            world.setGameRule(GameRules.SHOW_DEATH_MESSAGES, false);
            world.setGameRule(GameRules.RANDOM_TICK_SPEED, 0);
        });
    }

}
