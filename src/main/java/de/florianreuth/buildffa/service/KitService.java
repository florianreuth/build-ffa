package de.florianreuth.buildffa.service;

import de.florianreuth.buildffa.BuildFFA;
import de.florianreuth.buildffa.model.Kit;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class KitService {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final BuildFFA plugin;
    private final File file;
    private final Map<String, Kit> kits = new LinkedHashMap<>();

    public KitService(final BuildFFA plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "kits.yml");
    }

    public void load() {
        kits.clear();
        if (!file.exists()) {
            return;
        }

        final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        final ConfigurationSection section = config.getConfigurationSection("kits");
        if (section == null) {
            return;
        }

        for (final String kitId : section.getKeys(false)) {
            final ConfigurationSection kitSection = section.getConfigurationSection(kitId);
            if (kitSection == null) {
                continue;
            }

            final List<ItemStack> items = parseItems(kitSection.getStringList("items"));
            final List<ItemStack> armor = parseItems(kitSection.getStringList("armor"));
            final List<PotionEffect> effects = parseEffects(kitSection.getStringList("effects"));
            final String displayName = LEGACY.serialize(LEGACY.deserialize(kitSection.getString("name", kitId)));
            final String permission = kitSection.getString("permission", "");

            final Kit kit = new Kit(kitId.toLowerCase(Locale.ROOT), displayName, permission, items, armor, effects);
            kits.put(kit.id(), kit);
        }
    }

    public void reload() {
        load();
    }

    public Map<String, Kit> getKits() {
        return Collections.unmodifiableMap(kits);
    }

    public Optional<Kit> getKit(final String id) {
        return Optional.ofNullable(kits.get(id.toLowerCase(Locale.ROOT)));
    }

    public Optional<Kit> getDefaultKit() {
        final String configured = plugin.getConfig().getString("default-kit", "");
        if (!configured.isBlank()) {
            Kit configuredKit = kits.get(configured.toLowerCase(Locale.ROOT));
            if (configuredKit != null) {
                return Optional.of(configuredKit);
            }
        }

        if (kits.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(kits.values().iterator().next());
        }
    }

    public List<Kit> getAvailableKits(final Player player) {
        return kits.values().stream().filter(kit -> kit.canUse(player)).collect(Collectors.toList());
    }

    public void applyKit(final Player player, final Kit kit) {
        final PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setArmorContents(new ItemStack[4]);
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));

        for (final ItemStack itemStack : kit.items()) {
            inventory.addItem(itemStack);
        }

        // ....
        final List<ItemStack> armor = kit.armor();
        if (!armor.isEmpty()) {
            inventory.setHelmet(armor.getFirst());
        }
        if (armor.size() > 1) {
            inventory.setChestplate(armor.get(1));
        }
        if (armor.size() > 2) {
            inventory.setLeggings(armor.get(2));
        }
        if (armor.size() > 3) {
            inventory.setBoots(armor.get(3));
        }

        for (final PotionEffect effect : kit.effects()) {
            player.addPotionEffect(effect);
        }
        player.updateInventory();
    }

    private static List<ItemStack> parseItems(final List<String> entries) {
        final List<ItemStack> parsed = new ArrayList<>();
        for (final String entry : entries) {
            if (entry == null || entry.isBlank()) {
                continue;
            }

            final String[] parts = entry.split(":");
            final Material material = Material.matchMaterial(parts[0].trim());
            if (material == null || material.isAir()) {
                continue;
            }

            int amount = 1;
            if (parts.length > 1) {
                try {
                    amount = Math.max(1, Integer.parseInt(parts[1].trim()));
                } catch (NumberFormatException ignored) {}
            }
            parsed.add(new ItemStack(material, amount));
        }
        return parsed;
    }

    private static List<PotionEffect> parseEffects(final List<String> entries) {
        final List<PotionEffect> parsed = new ArrayList<>();
        for (final String entry : entries) {
            final String[] parts = entry.split(":");
            if (parts.length < 3) {
                continue;
            }

            final PotionEffectType type = PotionEffectType.getByName(parts[0].trim().toUpperCase(Locale.ROOT));
            if (type == null) {
                continue;
            }

            int amplifier;
            int durationTicks;
            try {
                amplifier = Integer.parseInt(parts[1].trim());
                durationTicks = Integer.parseInt(parts[2].trim());
            } catch (NumberFormatException ignored) {
                continue;
            }

            parsed.add(new PotionEffect(type, durationTicks, amplifier, true, false, true));
        }
        return parsed;
    }
}

