package com.francobm.magicosmetics.cache.cosmetics;

import com.francobm.magicosmetics.MagicCosmetics;
import com.francobm.magicosmetics.api.Cosmetic;
import com.francobm.magicosmetics.api.CosmeticType;
import com.francobm.magicosmetics.utils.DefaultAttributes;
import com.francobm.magicosmetics.utils.Utils;
import com.google.common.collect.Multimap;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class WStick extends Cosmetic implements CosmeticInventory {

    private boolean overlaps;
    private ItemStack currentItemSaved = null;
    private ItemStack combinedItem = null;
    private boolean hasDropped;

    public WStick(String id, String name, ItemStack itemStack, int modelData, boolean colored,
            CosmeticType cosmeticType, Color color, String permission, boolean texture, boolean overlaps,
            boolean hideMenu, boolean useEmote, NamespacedKey namespacedKey) {
        super(id, name, itemStack, modelData, colored, cosmeticType, color, permission, texture, hideMenu, useEmote,
                namespacedKey);
        this.overlaps = overlaps;
    }

    @Override
    protected void updateCosmetic(Cosmetic cosmetic) {
        super.updateCosmetic(cosmetic);
        WStick wStick = (WStick) cosmetic;
        overlaps = wStick.overlaps;
    }

    @Override
    public boolean updateProperties() {
        boolean result = super.updateProperties();
        if (result)
            update();
        return result;
    }

    @Override
    public void update() {
        if (isHideCosmetic()) {
            return;
        }
        if (lendEntity != null) {
            lendToEntity();
            return;
        }
        recoverCurrentItem();
        if (!overlaps) {
            if (currentItemSaved != null) {
                player.getInventory().setItemInOffHand(getItemPlaceholders(player));
                return;
            }
            ItemStack itemStack = player.getInventory().getItemInOffHand();
            if (itemStack.getType().isAir() || isCosmetic(itemStack)) {
                // Equip offhand Without combined.
                setAndSaveCurrentItem(null);
                player.getInventory().setItemInOffHand(getItemPlaceholders(player));
                return;
            }
            // Atomically remove from slot FIRST, then store in memory
            player.getInventory().setItemInOffHand(null);
            setAndSaveCurrentItem(itemStack.clone());
            player.getInventory().setItemInOffHand(getItemPlaceholders(player));
            player.updateInventory();
            return;
        }
        // Equip offhand combined with offhand item saved in cache
        if (currentItemSaved != null) {
            combinedItem = combinedItems(currentItemSaved);
            player.getInventory().setItemInOffHand(combinedItem);
            return;
        }
        ItemStack itemStack = player.getInventory().getItemInOffHand();
        if (itemStack.getType().isAir() || isCosmetic(itemStack)) {
            // Equip Helmet Without combined.
            player.getInventory().setItemInOffHand(getItemPlaceholders(player));
            return;
        }
        // Atomically remove from slot FIRST, then store in memory via combinedItems
        player.getInventory().setItemInOffHand(null);
        ItemStack offHand = itemStack;
        combinedItem = combinedItems(offHand);
        player.getInventory().setItemInOffHand(combinedItem);
        player.updateInventory();
    }

    public ItemStack changeItem(ItemStack originalItem) {
        if (isCosmetic(originalItem))
            return null;
        if (!overlaps) {
            if ((originalItem == null)) {
                if (currentItemSaved == null || currentItemSaved.getType().isAir()) {
                    setAndSaveCurrentItem(null);
                    player.getInventory().setItemInOffHand(getItemPlaceholders(player));
                    return null;
                }
            }
            // Always use currentItemSaved as source of truth, never read back from slot
            ItemStack offhand = currentItemSaved != null ? currentItemSaved.clone() : null;
            setAndSaveCurrentItem(originalItem != null ? originalItem.clone() : null);
            player.getInventory().setItemInOffHand(getItemPlaceholders(player));
            return offhand;
        }
        ItemStack offhand = currentItemSaved != null
                ? MagicCosmetics.getInstance().getVersion().getItemSavedWithNBTsUpdated(combinedItem,
                        currentItemSaved.clone())
                : null;
        combinedItem = combinedItems(originalItem);
        player.getInventory().setItemInOffHand(combinedItem);
        return offhand;
    }

    public void leftItem() {
        if (currentItemSaved == null)
            return;
        if (!overlaps) {
            player.setItemOnCursor(currentItemSaved.clone());
            setAndSaveCurrentItem(null);
            player.getInventory().setItemInOffHand(getItemPlaceholders(player));
            return;
        }
        ItemStack itemSavedUpdated = MagicCosmetics.getInstance().getVersion().getItemSavedWithNBTsUpdated(combinedItem,
                currentItemSaved.clone());
        player.setItemOnCursor(itemSavedUpdated);
        setAndSaveCurrentItem(null);
        player.getInventory().setItemInOffHand(getItemPlaceholders(player));
    }

    @Override
    public ItemStack leftItemAndGet() {
        if (currentItemSaved == null)
            return null;
        if (!overlaps) {
            ItemStack getItem = currentItemSaved.clone();
            setAndSaveCurrentItem(null);
            player.getInventory().setItemInOffHand(getItemPlaceholders(player));
            return getItem;
        }
        ItemStack getItem = MagicCosmetics.getInstance().getVersion().getItemSavedWithNBTsUpdated(combinedItem,
                currentItemSaved.clone());
        ;
        setAndSaveCurrentItem(null);
        player.getInventory().setItemInOffHand(getItemPlaceholders(player));
        return getItem;
    }

    @Override
    public void dropItem(boolean all) {
        recoverCurrentItem();
        if (currentItemSaved == null)
            return;

        ItemStack getItem = currentItemSaved.clone();
        int amount = getItem.getAmount();
        if (!all) {
            getItem.setAmount(1);
            if (amount <= 1) {
                setAndSaveCurrentItem(null);
            } else {
                currentItemSaved.setAmount(amount - 1);
                setAndSaveCurrentItem(currentItemSaved);
            }
        } else {
            getItem.setAmount(amount);
            setAndSaveCurrentItem(null);
        }
        Location location = player.getEyeLocation();
        location.setY(location.getY() - 0.30000001192092896);
        Item itemEntity = player.getWorld().dropItem(location, getItem);
        itemEntity.setThrower(player.getUniqueId());
        itemEntity.setVelocity(Utils.getItemDropVelocity(player));
        itemEntity.setPickupDelay(40);

        if (!overlaps) {
            MagicCosmetics.getInstance().getServer().getScheduler().runTask(MagicCosmetics.getInstance(), () -> {
                player.getInventory().setItemInOffHand(getItemPlaceholders(player));
                player.updateInventory();
            });
        }
    }

    private ItemStack combinedItems(ItemStack originalItem) {
        setAndSaveCurrentItem(originalItem);
        ItemStack cosmeticItem = getItemPlaceholders(player);
        if (currentItemSaved == null)
            return cosmeticItem;
        ItemMeta cosmeticMeta = cosmeticItem.getItemMeta();
        ItemMeta itemSaveMeta = currentItemSaved.hasItemMeta() ? currentItemSaved.getItemMeta()
                : Bukkit.getItemFactory().getItemMeta(currentItemSaved.getType());
        if (cosmeticMeta == null || itemSaveMeta == null)
            return cosmeticItem;
        if (!itemSaveMeta.getItemFlags().isEmpty())
            cosmeticMeta.addItemFlags(itemSaveMeta.getItemFlags().toArray(new ItemFlag[0]));
        List<String> lore = cosmeticMeta.hasLore() ? cosmeticMeta.getLore() : new ArrayList<>();
        if (itemSaveMeta.getLore() != null && !itemSaveMeta.getLore().isEmpty()) {
            lore.add("");
            lore.addAll(itemSaveMeta.getLore());
        }
        cosmeticMeta.setLore(lore);

        Multimap<Attribute, AttributeModifier> attributes = itemSaveMeta.getAttributeModifiers() == null
                ? DefaultAttributes.defaultsOf(currentItemSaved)
                : itemSaveMeta.getAttributeModifiers();
        cosmeticMeta.setAttributeModifiers(attributes);
        cosmeticItem.setItemMeta(cosmeticMeta);
        cosmeticItem = MagicCosmetics.getInstance().getVersion().getItemWithNBTsCopy(currentItemSaved, cosmeticItem);
        return cosmeticItem;
    }

    @Override
    public void lendToEntity() {
        if (lendEntity.getEquipment() == null)
            return;
        if (!lendEntity.getEquipment().getItemInOffHand().getType().isAir()
                && lendEntity.getEquipment().getItemInOffHand().isSimilar(getItemColor(player)))
            return;
        lendEntity.getEquipment().setItemInOffHand(getItemColor(player));
    }

    @Override
    public void hide(Player player) {

    }

    @Override
    public void show(Player player) {

    }

    @Override
    public void setHideCosmetic(boolean hideCosmetic) {
        super.setHideCosmetic(hideCosmetic);
        if (hideCosmetic)
            remove();
        else
            update();
    }

    @Override
    public void remove() {
        recoverCurrentItem();
        if (!overlaps) {
            if (currentItemSaved == null) {
                player.getInventory().setItemInOffHand(null);
            } else {
                player.getInventory().setItemInOffHand(currentItemSaved.clone());
                setAndSaveCurrentItem(null);
            }
            return;
        }
        if (currentItemSaved != null) {
            // Clear offhand With offhand item save in cache
            player.getInventory().setItemInOffHand(currentItemSaved.clone());
            setAndSaveCurrentItem(null);
            return;
        }
        player.getInventory().setItemInOffHand(null);
    }

    @Override
    public void forceRemove() {
        setAndSaveCurrentItem(null);
    }

    @Override
    public ItemStack getSavedItemForDeath() {
        recoverCurrentItem();
        if (currentItemSaved == null)
            return null;
        if (overlaps && combinedItem != null) {
            return MagicCosmetics.getInstance().getVersion()
                    .getItemSavedWithNBTsUpdated(combinedItem, currentItemSaved.clone());
        }
        return currentItemSaved.clone();
    }

    @Override
    public void clearClose() {
        recoverCurrentItem();
        if (!overlaps) {
            if (currentItemSaved == null) {
                player.getInventory().setItemInOffHand(null);
            } else {
                player.getInventory().setItemInOffHand(currentItemSaved.clone());
                setAndSaveCurrentItem(null);
            }
            player.updateInventory();
            return;
        }
        if (currentItemSaved != null) {
            // Clear offhand With offhand item save in cache
            player.getInventory().setItemInOffHand(currentItemSaved.clone());
            setAndSaveCurrentItem(null);
            player.updateInventory();
            return;
        }
        player.getInventory().setItemInOffHand(null);
        player.updateInventory();
    }

    public boolean isOverlaps() {
        return overlaps;
    }

    @Override
    public ItemStack getCurrentItemSaved() {
        return currentItemSaved;
    }

    public void recoverCurrentItem() {
        if (player == null || currentItemSaved != null) return;
        try {
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(MagicCosmetics.getInstance(), "original_wstick");
            if (player.getPersistentDataContainer().has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
                String base64 = player.getPersistentDataContainer().get(key, org.bukkit.persistence.PersistentDataType.STRING);
                if (base64 != null && !base64.isEmpty()) {
                    currentItemSaved = Utils.itemFromBase64(base64);
                }
            }
        } catch (Exception e) {}
    }

    private void setAndSaveCurrentItem(ItemStack item) {
        this.currentItemSaved = item;
        if (player == null) return;
        try {
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(MagicCosmetics.getInstance(), "original_wstick");
            if (item == null || item.getType().isAir()) {
                player.getPersistentDataContainer().remove(key);
            } else {
                player.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.STRING, Utils.itemToBase64(item));
            }
        } catch (Exception e) {}
    }

    public void setCurrentItemSaved(ItemStack currentItemSaved) {
        setAndSaveCurrentItem(currentItemSaved);
    }

    public boolean isHasDropped() {
        return hasDropped;
    }

    public void setHasDropped(boolean hasDropped) {
        this.hasDropped = hasDropped;
    }

    @Override
    public ItemStack getEquipment() {
        return player.getInventory().getItemInOffHand();
    }

    @Override
    public void spawn(Player player) {
        // Nothing to do
    }

    @Override
    public void despawn(Player player) {
        // Nothing to do
    }
}
