package de.florianreuth.buildffa.listener;

import de.florianreuth.buildffa.service.BuildService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import java.util.List;

public final class BuildListener implements Listener {

    private final BuildService buildService;

    public BuildListener(final BuildService buildService) {
        this.buildService = buildService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(final BlockPlaceEvent event) {
        if (buildService.isInSpawnProtection(event.getBlockPlaced().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar(Component.text("Spawn area is protected.", NamedTextColor.RED));
            return;
        }

        if (buildService.isBuildMode(event.getPlayer().getUniqueId())) {
            return;
        }

        if (buildService.isBuildDisabled()) {
            event.setCancelled(true);
            return;
        }

        buildService.trackPlacement(event.getBlockPlaced(), event.getBlockReplacedState().getBlockData());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(final BlockBreakEvent event) {
        if (buildService.isInSpawnProtection(event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar(Component.text("Spawn area is protected.", NamedTextColor.RED));
            return;
        }

        if (buildService.isBuildMode(event.getPlayer().getUniqueId())) {
            buildService.handleBreak(event.getBlock());
            return;
        }

        if (!buildService.canBreak(event.getBlock())) {
            event.setCancelled(true);
            event.getPlayer()
                    .sendActionBar(Component.text("You can only break player placed blocks.", NamedTextColor.RED));
            return;
        }

        buildService.handleBreak(event.getBlock());
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(final EntityExplodeEvent event) {
        handleBlocks(event.blockList());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(final BlockExplodeEvent event) {
        handleBlocks(event.blockList());
    }

    private void handleBlocks(final List<Block> blocks) {
        blocks.removeIf(block -> {
            if (!buildService.canBreak(block)) {
                return true;
            }
            buildService.handleBreak(block);
            return false;
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketEmpty(final PlayerBucketEmptyEvent event) {
        final Block clicked = event.getBlockClicked();
        final Block target = clicked.getRelative(event.getBlockFace());
        if (buildService.isInSpawnProtection(target.getLocation())
                || buildService.isInSpawnProtection(clicked.getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar(Component.text("Spawn area is protected.", NamedTextColor.RED));
            return;
        }

        if (buildService.isBuildMode(event.getPlayer().getUniqueId())) {
            return;
        }
        if (buildService.isBuildDisabled()) {
            event.setCancelled(true);
            return;
        }

        buildService.trackPlacement(target, target.getBlockData());
        if (event.getBucket() == Material.WATER_BUCKET
                && clicked.getBlockData() instanceof final Waterlogged waterlogged && !waterlogged.isWaterlogged()) {
            buildService.trackPlacement(clicked, clicked.getBlockData());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketFill(final PlayerBucketFillEvent event) {
        if (buildService.isInSpawnProtection(event.getBlockClicked().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar(Component.text("Spawn area is protected.", NamedTextColor.RED));
            return;
        }

        if (buildService.isBuildMode(event.getPlayer().getUniqueId())) {
            return;
        }

        if (!buildService.canBreak(event.getBlockClicked())) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar(Component.text("You cannot remove map liquids.", NamedTextColor.RED));
            return;
        }

        buildService.clearTracked(event.getBlockClicked());
    }

    @EventHandler(ignoreCancelled = true)
    public void onFluidSpread(final BlockFromToEvent event) {
        if (!event.getBlock().isLiquid()) {
            return;
        }

        if (buildService.isInSpawnProtection(event.getToBlock().getLocation())) {
            event.setCancelled(true);
            return;
        }

        if (!buildService.canBreak(event.getToBlock()) && event.getToBlock().getType() != org.bukkit.Material.AIR) {
            event.setCancelled(true);
            return;
        }

        if (buildService.canBreak(event.getToBlock())) {
            buildService.handleBreak(event.getToBlock());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onUseItems(final PlayerInteractEvent event) {
        final var clickedBlock = event.getClickedBlock();

        if (clickedBlock != null &&
                clickedBlock.getType().name().contains("TRAPDOOR") &&
                !buildService.isBuildDisabled()) {
            event.getPlayer().sendRichMessage("<red>You cannot open trapdoors!");
            return;
        }

        if (!buildService.isInSpawnProtection(event.getPlayer().getLocation())) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (!event.hasItem() && clickedBlock == null) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().sendActionBar(Component.text("You cannot use items near spawn.", NamedTextColor.RED));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConsume(final PlayerItemConsumeEvent event) {
        if (!buildService.isInSpawnProtection(event.getPlayer().getLocation())) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().sendActionBar(Component.text("You cannot use items near spawn.", NamedTextColor.RED));
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        buildService.setBuildMode(event.getPlayer().getUniqueId(), false);
    }

}
