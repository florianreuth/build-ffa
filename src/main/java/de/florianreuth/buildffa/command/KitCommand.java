package de.florianreuth.buildffa.command;

import de.florianreuth.buildffa.model.Kit;
import de.florianreuth.buildffa.model.PlayerStats;
import de.florianreuth.buildffa.service.GadgetService;
import de.florianreuth.buildffa.service.KitService;
import de.florianreuth.buildffa.service.MatchService;
import de.florianreuth.buildffa.service.PlayerDataService;
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

public final class KitCommand implements CommandExecutor, TabCompleter {

    private final KitService kitService;
    private final PlayerDataService playerDataService;
    private final MatchService matchService;
    private final GadgetService gadgetService;

    public KitCommand(
        KitService kitService,
        PlayerDataService playerDataService,
        MatchService matchService,
        GadgetService gadgetService
    ) {
        this.kitService = kitService;
        this.playerDataService = playerDataService;
        this.matchService = matchService;
        this.gadgetService = gadgetService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Branding.send(sender, Component.text("Only players can select kits.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0 || "list".equalsIgnoreCase(args[0])) {
            final List<Kit> kits = kitService.getAvailableKits(player);
            if (kits.isEmpty()) {
                Branding.send(player, Component.text("No kits are available for your permissions.", NamedTextColor.RED));
                return true;
            }

            Component line = Component.text("Available kits: ", NamedTextColor.GOLD);
            for (int i = 0; i < kits.size(); i++) {
                final Kit kit = kits.get(i);
                line = line
                    .append(Component.text(kit.id(), NamedTextColor.YELLOW))
                    .append(Component.text(" (", NamedTextColor.DARK_GRAY))
                    .append(Branding.legacy(kit.displayName()))
                    .append(Component.text(")", NamedTextColor.DARK_GRAY));
                if (i < kits.size() - 1) {
                    line = line.append(Component.text(", ", NamedTextColor.GRAY));
                }
            }
            Branding.send(player, line);
            return true;
        }

        if (matchService.isInCombat(player)) {
            final long secondsLeft = Math.max(1L, matchService.getCombatMillisLeft(player) / 1000L);
            Branding.send(player, Component.text("You cannot switch kits in combat (" + secondsLeft + "s left).", NamedTextColor.RED));
            return true;
        }

        final String kitName = args[0].toLowerCase(Locale.ROOT);
        final Kit kit = kitService.getKit(kitName).orElse(null);
        if (kit == null) {
            Branding.send(player, Component.text("Unknown kit. Use /kit list.", NamedTextColor.RED));
            return true;
        }
        if (!kit.canUse(player)) {
            Branding.send(player, Component.text("You do not have permission for that kit.", NamedTextColor.RED));
            return true;
        }

        final PlayerStats stats = playerDataService.get(player.getUniqueId());
        stats.setSelectedKit(kit.id());
        matchService.preparePlayer(player, false);
        gadgetService.giveSelectedGadget(player);
        Branding.send(
            player,
            Component.text("Selected kit: ", NamedTextColor.GREEN)
                .append(Branding.legacy(kit.displayName()))
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
        for (final String kitName : kitService.getKits().keySet()) {
            if (kitName.startsWith(prefix)) {
                suggestions.add(kitName);
            }
        }
        if ("list".startsWith(prefix)) {
            suggestions.add("list");
        }
        return suggestions;
    }

}

