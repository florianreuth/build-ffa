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

    public KitService(BuildFFA plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "kits.yml");
    }

    public void load() {
        kits.clear();
        if (!file.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("kits");
        if (section == null) {
            return;
        }

        for (String kitId : section.getKeys(false)) {
            ConfigurationSection kitSection = section.getConfigurationSection(kitId);
            if (kitSection == null) {
                continue;
            }

            List<ItemStack> items = parseItems(kitSection.getStringList("items"));
            List<ItemStack> armor = parseItems(kitSection.getStringList("armor"));
            List<PotionEffect> effects = parseEffects(kitSection.getStringList("effects"));
            String displayName = LEGACY.serialize(LEGACY.deserialize(kitSection.getString("name", kitId)));
            String permission = kitSection.getString("permission", "");

            Kit kit = new Kit(kitId.toLowerCase(Locale.ROOT), displayName, permission, items, armor, effects);
            kits.put(kit.id(), kit);
        }
    }

    public void reload() {
        load();
    }

    public Map<String, Kit> getKits() {
        return Collections.unmodifiableMap(kits);
    }

    public Optional<Kit> getKit(String id) {
        return Optional.ofNullable(kits.get(id.toLowerCase(Locale.ROOT)));
    }

    public Optional<Kit> getDefaultKit() {
        String configured = plugin.getConfig().getString("default-kit", "");
        if (!configured.isBlank()) {
            Kit configuredKit = kits.get(configured.toLowerCase(Locale.ROOT));
            if (configuredKit != null) {
                return Optional.of(configuredKit);
            }
        }

        if (kits.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(kits.values().iterator().next());
    }

    public List<Kit> getAvailableKits(Player player) {
        return kits.values().stream().filter(kit -> kit.canUse(player)).collect(Collectors.toList());
    }

    public void applyKit(Player player, Kit kit) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setArmorContents(new ItemStack[4]);
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));

        for (ItemStack itemStack : kit.items()) {
            inventory.addItem(itemStack);
        }

        List<ItemStack> armor = kit.armor();
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

        for (PotionEffect effect : kit.effects()) {
            player.addPotionEffect(effect);
        }
        player.updateInventory();
    }

    private static List<ItemStack> parseItems(List<String> entries) {
        List<ItemStack> parsed = new ArrayList<>();
        for (String entry : entries) {
            if (entry == null || entry.isBlank()) {
                continue;
            }

            String[] parts = entry.split(":");
            Material material = Material.matchMaterial(parts[0].trim());
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

    private static List<PotionEffect> parseEffects(List<String> entries) {
        List<PotionEffect> parsed = new ArrayList<>();
        for (String entry : entries) {
            String[] parts = entry.split(":");
            if (parts.length < 3) {
                continue;
            }

            PotionEffectType type = PotionEffectType.getByName(parts[0].trim().toUpperCase(Locale.ROOT));
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

