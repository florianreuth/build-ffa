package de.florianreuth.buildffa.service;

import de.florianreuth.buildffa.BuildFFA;
import de.florianreuth.buildffa.model.PlayerStats;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public final class GadgetService {

    private final BuildFFA plugin;
    private final PlayerDataService playerDataService;
    private final Map<String, GadgetDefinition> gadgets = new LinkedHashMap<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    public GadgetService(BuildFFA plugin, PlayerDataService playerDataService) {
        this.plugin = plugin;
        this.playerDataService = playerDataService;
    }

    public void load() {
        gadgets.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("gadgets");
        if (section == null) {
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection gadgetSection = section.getConfigurationSection(id);
            if (gadgetSection == null || !gadgetSection.getBoolean("enabled", true)) {
                continue;
            }

            String type = gadgetSection.getString("type", "heal").toUpperCase(Locale.ROOT);
            String itemName = gadgetSection.getString("material", "BLAZE_ROD");
            Material material = Material.matchMaterial(itemName);
            if (material == null || material.isAir()) {
                material = Material.BLAZE_ROD;
            }

            GadgetDefinition definition = new GadgetDefinition(
                id.toLowerCase(Locale.ROOT),
                gadgetSection.getString("display-name", id),
                type,
                material,
                Math.max(0, gadgetSection.getInt("slot", 7)),
                Math.max(1, gadgetSection.getInt("cooldown-seconds", 20)),
                gadgetSection.getDouble("strength", 1.0),
                Math.max(1, gadgetSection.getInt("amount", 1)),
                gadgetSection.getString("permission", "")
            );
            gadgets.put(definition.id(), definition);
        }
    }

    public Map<String, GadgetDefinition> getGadgets() {
        return Collections.unmodifiableMap(gadgets);
    }

    public Map<String, GadgetDefinition> getAvailable(Player player) {
        return gadgets
            .entrySet()
            .stream()
            .filter(entry -> canUse(player, entry.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (first, ignored) -> first, LinkedHashMap::new));
    }

    public Optional<GadgetDefinition> getSelected(Player player) {
        PlayerStats stats = playerDataService.get(player.getUniqueId());
        String selected = stats.getSelectedGadget();
        if (selected != null && !selected.isBlank()) {
            GadgetDefinition configured = gadgets.get(selected.toLowerCase(Locale.ROOT));
            if (configured != null && canUse(player, configured)) {
                return Optional.of(configured);
            }
        }
        return gadgets.values().stream().filter(gadget -> canUse(player, gadget)).findFirst();
    }

    public boolean select(Player player, String id) {
        GadgetDefinition definition = gadgets.get(id.toLowerCase(Locale.ROOT));
        if (definition == null || !canUse(player, definition)) {
            return false;
        }
        playerDataService.get(player.getUniqueId()).setSelectedGadget(definition.id());
        giveSelectedGadget(player);
        return true;
    }

    public void giveSelectedGadget(Player player) {
        GadgetDefinition definition = getSelected(player).orElse(null);
        if (definition == null) {
            return;
        }
        ItemStack itemStack = new ItemStack(definition.material(), definition.amount());
        ItemMeta meta = itemStack.getItemMeta();
        meta.displayName(Component.text(definition.displayName(), NamedTextColor.AQUA));
        itemStack.setItemMeta(meta);
        player.getInventory().setItem(definition.slot(), itemStack);
    }

    public boolean useSelectedGadget(Player player) {
        GadgetDefinition definition = getSelected(player).orElse(null);
        if (definition == null) {
            return false;
        }

        long cooldownLeft = getCooldownMillisLeft(player, definition.id());
        if (cooldownLeft > 0L) {
            long seconds = Math.max(1L, cooldownLeft / 1000L);
            player.sendActionBar(Component.text("Gadget cooldown: " + seconds + "s", NamedTextColor.RED));
            return true;
        }

        switch (definition.type()) {
            case "HEAL" -> {
                double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) == null
                    ? 20.0
                    : player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                player.setHealth(Math.min(maxHealth, player.getHealth() + (4.0 * definition.strength())));
            }
            case "LEAP" -> {
                Vector vector = player.getLocation().getDirection().normalize().multiply(1.1 * definition.strength());
                vector.setY(0.55 * definition.strength());
                player.setVelocity(vector);
            }
            case "FIREBALL" -> {
                Fireball fireball = player.launchProjectile(Fireball.class);
                fireball.setYield(0.0f);
                fireball.setIsIncendiary(false);
                fireball.setShooter(player);
                fireball.setVelocity(player.getLocation().getDirection().normalize().multiply(1.2 * definition.strength()));
            }
            case "DASH" -> {
                Vector vector = player.getLocation().getDirection().normalize().multiply(1.8 * definition.strength());
                vector.setY(Math.max(0.25, 0.35 * definition.strength()));
                player.setVelocity(vector);
            }
            case "BLINK" -> {
                Location eye = player.getEyeLocation();
                Vector direction = eye.getDirection().normalize();
                double maxDistance = Math.max(3.0, 6.0 * definition.strength());

                Location target = player.getLocation().clone().add(direction.clone().multiply(maxDistance));
                RayTraceResult hit = player.getWorld().rayTraceBlocks(eye, direction, maxDistance);
                if (hit != null) {
                    target = hit.getHitPosition().toLocation(player.getWorld()).subtract(direction.clone().multiply(1.2));
                }

                if (target.getBlock().isSolid()) {
                    target.add(0, 1, 0);
                }
                if (target.getBlock().isSolid()) {
                    return false;
                }

                target.setYaw(player.getLocation().getYaw());
                target.setPitch(player.getLocation().getPitch());
                player.teleportAsync(target);
                player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 16, 0.35, 0.4, 0.35, 0.02);
            }
            case "SHOCKWAVE" -> {
                double radius = Math.max(2.0, 3.5 * definition.strength());
                for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                    if (!(entity instanceof Player target) || target.getUniqueId().equals(player.getUniqueId())) {
                        continue;
                    }
                    Vector knockback = target.getLocation().toVector().subtract(player.getLocation().toVector());
                    if (knockback.lengthSquared() < 0.0001) {
                        knockback = new Vector(0, 0, 1);
                    }
                    knockback.normalize().multiply(1.25 * definition.strength()).setY(0.35 * definition.strength());
                    target.setVelocity(knockback);
                }
                player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation().add(0, 1, 0), 3);
            }
            default -> {
                return false;
            }
        }

        cooldowns
            .computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>())
            .put(definition.id(), System.currentTimeMillis());
        player.sendActionBar(Component.text("Used gadget: " + definition.displayName(), NamedTextColor.GREEN));
        return true;
    }

    public long getSelectedCooldownMillisLeft(Player player) {
        GadgetDefinition definition = getSelected(player).orElse(null);
        if (definition == null) {
            return 0L;
        }
        return getCooldownMillisLeft(player, definition.id());
    }

    private static boolean canUse(Player player, GadgetDefinition definition) {
        return definition.permission().isBlank() || player.hasPermission(definition.permission());
    }

    private long getCooldownMillisLeft(Player player, String gadgetId) {
        GadgetDefinition definition = gadgets.get(gadgetId);
        if (definition == null) {
            return 0L;
        }

        Map<String, Long> perPlayer = cooldowns.get(player.getUniqueId());
        if (perPlayer == null) {
            return 0L;
        }

        Long usedAt = perPlayer.get(gadgetId);
        if (usedAt == null) {
            return 0L;
        }

        long cooldownMillis = definition.cooldownSeconds() * 1000L;
        long left = cooldownMillis - (System.currentTimeMillis() - usedAt);
        return Math.max(0L, left);
    }

    public record GadgetDefinition(
        String id,
        String displayName,
        String type,
        Material material,
        int slot,
        int cooldownSeconds,
        double strength,
        int amount,
        String permission
    ) {}
}

