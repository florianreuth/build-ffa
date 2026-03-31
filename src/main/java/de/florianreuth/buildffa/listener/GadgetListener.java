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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

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

        final Player player = event.getPlayer();

        final ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem.isEmpty() || handItem.getType().isAir()) {
            return;
        }

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
