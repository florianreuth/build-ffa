package de.florianreuth.buildffa.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public record Kit(String id, String displayName, String permission, List<ItemStack> items, List<ItemStack> armor,
                  List<PotionEffect> effects) {

    public Kit(
        final String id,
        final String displayName,
        final String permission,
        final List<ItemStack> items,
        final List<ItemStack> armor,
        final List<PotionEffect> effects
    ) {
        this.id = id;
        this.displayName = displayName;
        this.permission = permission;
        this.items = copyItems(items);
        this.armor = copyItems(armor);
        this.effects = new ArrayList<>(effects);
    }

    @Override
    public List<ItemStack> items() {
        return copyItems(items);
    }

    @Override
    public List<ItemStack> armor() {
        return copyItems(armor);
    }

    @Override
    public List<PotionEffect> effects() {
        return Collections.unmodifiableList(effects);
    }

    public boolean canUse(final CommandSender sender) {
        return permission == null || permission.isBlank() || sender.hasPermission(permission);
    }

    private static List<ItemStack> copyItems(final List<ItemStack> source) {
        final List<ItemStack> clone = new ArrayList<>(source.size());
        for (final ItemStack itemStack : source) {
            clone.add(itemStack.clone());
        }
        return clone;
    }
}

