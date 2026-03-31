package de.florianreuth.buildffa.listener;

import de.florianreuth.buildffa.service.GadgetService;
import de.florianreuth.buildffa.service.GadgetService.GadgetDefinition;
import de.florianreuth.buildffa.service.BuildService;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class GadgetListener implements Listener {

    private final GadgetService gadgetService;
    private final BuildService buildService;

    public GadgetListener(GadgetService gadgetService, BuildService buildService) {
        this.gadgetService = gadgetService;
        this.buildService = buildService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (buildService.isInSpawnProtection(player.getLocation())) {
            event.setCancelled(true);
            player.sendActionBar(Component.text("You cannot use items near spawn.", NamedTextColor.RED));
            return;
        }

        Optional<GadgetDefinition> selected = gadgetService.getSelected(player);
        if (selected.isEmpty()) {
            return;
        }

        GadgetDefinition gadget = selected.get();
        if (event.getItem() == null || event.getItem().getType() != gadget.material()) {
            return;
        }

        if (player.getInventory().getHeldItemSlot() != gadget.slot()) {
            return;
        }

        boolean handled = gadgetService.useSelectedGadget(player);
        if (handled) {
            event.setCancelled(true);
        }
    }
}

