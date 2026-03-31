package de.florianreuth.buildffa.listener;

import de.florianreuth.buildffa.BuildFFA;
import de.florianreuth.buildffa.service.GadgetService;
import de.florianreuth.buildffa.service.HudService;
import de.florianreuth.buildffa.service.MatchService;
import de.florianreuth.buildffa.service.PlayerDataService;
import de.florianreuth.buildffa.util.Branding;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class PlayerLifecycleListener implements Listener {

    private final BuildFFA plugin;
    private final MatchService matchService;
    private final PlayerDataService playerDataService;
    private final GadgetService gadgetService;
    private final HudService hudService;

    public PlayerLifecycleListener(
        final BuildFFA plugin,
        final MatchService matchService,
        final PlayerDataService playerDataService,
        final GadgetService gadgetService,
        final HudService hudService
    ) {
        this.plugin = plugin;
        this.matchService = matchService;
        this.playerDataService = playerDataService;
        this.gadgetService = gadgetService;
        this.hudService = hudService;
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        playerDataService.get(player.getUniqueId());
        matchService.preparePlayer(player, true);
        gadgetService.giveSelectedGadget(player);
        hudService.refreshPlayer(player);
        Branding.send(player, Component.text("Use ", NamedTextColor.GRAY).append(Component.text("/kit",
            NamedTextColor.YELLOW)).append(Component.text(" to switch kits.", NamedTextColor.GRAY)));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(final PlayerDeathEvent event) {
        final Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) {
            killer = matchService.resolveRecentAttacker(victim);
        }

        matchService.handleDeath(victim);
        if (killer != null && !killer.getUniqueId().equals(victim.getUniqueId())) {
            final int streak = matchService.handleKill(killer);
            Branding.send(killer, Component.text("Kill +1 | Streak: " + streak, NamedTextColor.GREEN));
            Bukkit.getServer().broadcast(Branding.PREFIX.append(Component.text(killer.getName(), NamedTextColor.RED))
                .append(Component.text(" killed ", NamedTextColor.GRAY))
                .append(Component.text(victim.getName(), NamedTextColor.YELLOW)));
            hudService.refreshPlayer(killer);
        }
        hudService.refreshPlayer(victim);

        event.getDrops().clear();
        event.setDroppedExp(0);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVoidDamage(final EntityDamageEvent event) {
        if (!(event.getEntity() instanceof final Player victim)) {
            return;
        }

        if (event.getCause() != EntityDamageEvent.DamageCause.VOID) {
            return;
        }

        event.setCancelled(true);

        final Player killer = matchService.resolveRecentAttacker(victim);
        matchService.handleDeath(victim);

        if (killer != null && !killer.getUniqueId().equals(victim.getUniqueId())) {
            final int streak = matchService.handleKill(killer);
            Branding.send(killer, Component.text("Void kill +1 | Streak: " + streak, NamedTextColor.GREEN));
            Bukkit.getServer().broadcast(Branding.PREFIX.append(Component.text(killer.getName(), NamedTextColor.RED))
                .append(Component.text(" knocked ", NamedTextColor.GRAY))
                .append(Component.text(victim.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" into the void", NamedTextColor.GRAY)));
            hudService.refreshPlayer(killer);
        }

        matchService.preparePlayer(victim, true);
        gadgetService.giveSelectedGadget(victim);
        hudService.refreshPlayer(victim);
    }

    @EventHandler
    public void onRespawn(final PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        event.setRespawnLocation(matchService.resolveSpawn(player));

        // Delayed re-gear avoids clashes with other respawn handlers.
        player.getServer().getScheduler().runTaskLater(plugin, () -> {
            matchService.preparePlayer(player, false);
            gadgetService.giveSelectedGadget(player);
            hudService.refreshPlayer(player);
        }, 1L);
    }
}
