package de.florianreuth.buildffa.listener;

import de.florianreuth.buildffa.service.MatchService;
import de.florianreuth.buildffa.service.BuildService;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class CombatListener implements Listener {

    private final MatchService matchService;
    private final BuildService buildService;

    public CombatListener(final MatchService matchService, final BuildService buildService) {
        this.matchService = matchService;
        this.buildService = buildService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(final EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof final Player victim)) {
            return;
        }

        final Player attacker = resolveDamager(event.getDamager());
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        if (buildService.isInSpawnProtection(attacker.getLocation()) || buildService.isInSpawnProtection(victim.getLocation())) {
            event.setCancelled(true);
            return;
        }

        matchService.tagCombat(attacker, victim);
    }

    private static Player resolveDamager(final Entity damager) {
        if (damager instanceof final Player player) {
            return player;
        }
        if (damager instanceof final Projectile projectile && projectile.getShooter() instanceof final Player shooter) {
            return shooter;
        }
        return null;
    }

}
