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

package de.florianreuth.buildffa.listener;

import de.florianreuth.buildffa.service.BuildService;
import de.florianreuth.buildffa.service.GadgetService;
import de.florianreuth.buildffa.service.GadgetService.GadgetDefinition;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.Event;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class GadgetListener implements Listener {

    private final GadgetService gadgetService;
    private final BuildService buildService;

    public GadgetListener(final GadgetService gadgetService, final BuildService buildService) {
        this.gadgetService = gadgetService;
        this.buildService = buildService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(final PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        final Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        final Player player = event.getPlayer();
        final ItemStack handItem = event.getItem() != null ? event.getItem() : player.getInventory().getItemInMainHand();
        if (handItem.isEmpty() || handItem.getType().isAir()) {
            return;
        }

        if (buildService.isInSpawnProtection(player.getLocation())) {
            event.setCancelled(true);
            player.sendActionBar(Component.text("You cannot use items near spawn.", NamedTextColor.RED));
            return;
        }

        final Optional<GadgetDefinition> selected = gadgetService.getSelected(player);
        if (selected.isEmpty()) {
            return;
        }

        final GadgetDefinition gadget = selected.get();
        if (handItem.getType() != gadget.material()) {
            return;
        }

        if (player.getInventory().getHeldItemSlot() != gadget.slot()) {
            return;
        }

        final boolean handled = gadgetService.useSelectedGadget(player);
        if (handled) {
            // Explicitly deny vanilla item/block interaction while still allowing right-click-air gadget activation.
            event.setUseItemInHand(Event.Result.DENY);
            event.setUseInteractedBlock(Event.Result.DENY);
            event.setCancelled(true);
        }
    }

}
