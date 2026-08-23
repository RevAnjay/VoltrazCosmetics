package xyz.voltraz.cosmetics.cache.cosmetics;

import xyz.voltraz.cosmetics.VoltrazCosmetics;
import xyz.voltraz.cosmetics.api.Cosmetic;
import xyz.voltraz.cosmetics.api.CosmeticType;
import xyz.voltraz.cosmetics.utils.DefaultAttributes;
import xyz.voltraz.cosmetics.utils.FoliaUtil;
import xyz.voltraz.cosmetics.utils.Utils;
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

public class Hat extends Cosmetic implements CosmeticInventory {

    private boolean overlaps;
    private double offSetY;
    private ItemStack currentItemSaved = null;
    private ItemStack combinedItem = null;
    private boolean hasDropped;
    private volatile boolean closing = false;

    public Hat(String id, String name, ItemStack itemStack, int modelData, boolean colored, CosmeticType cosmeticType, Color color, boolean overlaps, String permission, boolean texture, boolean hideMenu, boolean useEmote, double offSetY, NamespacedKey namespacedKey) {
        super(id, name, itemStack, modelData, colored, cosmeticType, color, permission, texture, hideMenu, useEmote, namespacedKey);
        this.overlaps = overlaps;
        this.offSetY = offSetY;
    }

    @Override
    protected void updateCosmetic(Cosmetic cosmetic) {
        super.updateCosmetic(cosmetic);
        Hat hat = (Hat) cosmetic;
        overlaps = hat.overlaps;
        offSetY = hat.offSetY;
    }

    @Override
    public boolean updateProperties() {
        boolean result = super.updateProperties();
        if(result)
            update();
        return result;
    }

    @Override
    public void update() {
        if(player == null || closing){
            return;
        }
        if(isHideCosmetic()){
            return;
        }
        if(lendEntity != null){
            lendToEntity();
            return;
        }
        recoverCurrentItem();
        if(!overlaps) {
            if(currentItemSaved != null) {
                player.getInventory().setHelmet(getItemPlaceholders(player));
                return;
            }
            ItemStack itemStack = player.getInventory().getHelmet();
            if(itemStack == null || itemStack.getType().isAir() || isCosmetic(itemStack)) {
                //Equip Helmet Without combined.
                setAndSaveCurrentItem(null);
                player.getInventory().setHelmet(getItemPlaceholders(player));
                return;
            }
            player.getInventory().setHelmet(null);
            setAndSaveCurrentItem(itemStack.clone());
            player.getInventory().setHelmet(getItemPlaceholders(player));
            player.updateInventory();
            return;
        }
        //Equip hat combined with helmet saved in cache
        if(currentItemSaved != null) {
            combinedItem = combinedItems(currentItemSaved);
            player.getInventory().setHelmet(combinedItem);
            return;
        }
        ItemStack itemStack = player.getInventory().getHelmet();
        if(itemStack == null || itemStack.getType().isAir() || isCosmetic(itemStack)) {
            //Equip Helmet Without combined.
            player.getInventory().setHelmet(getItemPlaceholders(player));
            return;
        }
        player.getInventory().setHelmet(null);
        combinedItem = combinedItems(itemStack);
        player.getInventory().setHelmet(combinedItem);
        player.updateInventory();
    }

    public ItemStack changeItem(ItemStack originalItem) {
        if(isCosmetic(originalItem)) return null;
        if(!overlaps){
            if(originalItem == null) {
                if(currentItemSaved == null || currentItemSaved.getType().isAir()) {
                    setAndSaveCurrentItem(null);
                    player.getInventory().setHelmet(getItemPlaceholders(player));
                    return null;
                }
            }
            ItemStack helmet = currentItemSaved != null ? currentItemSaved.clone() : null;
            setAndSaveCurrentItem(originalItem != null ? originalItem.clone() : null);
            player.getInventory().setHelmet(getItemPlaceholders(player));
            return helmet;
        }
        ItemStack helmet = currentItemSaved != null ? VoltrazCosmetics.getInstance().getVersion().getItemSavedWithNBTsUpdated(combinedItem, currentItemSaved.clone()) : null;
        combinedItem = combinedItems(originalItem);
        player.getInventory().setHelmet(combinedItem);
        return helmet;
    }

    public void leftItem() {
        if(currentItemSaved == null) return;
        if(!overlaps){
            player.setItemOnCursor(currentItemSaved.clone());
            setAndSaveCurrentItem(null);
            player.getInventory().setHelmet(getItemPlaceholders(player));
            return;
        }
        ItemStack itemSavedUpdated = VoltrazCosmetics.getInstance().getVersion().getItemSavedWithNBTsUpdated(combinedItem, currentItemSaved.clone());
        player.setItemOnCursor(itemSavedUpdated);
        setAndSaveCurrentItem(null);
        combinedItem = null;
        player.getInventory().setHelmet(getItemPlaceholders(player));
    }

    @Override
    public ItemStack leftItemAndGet() {
        if(currentItemSaved == null) return null;
        if(!overlaps) {
            ItemStack getItem = currentItemSaved.clone();
            setAndSaveCurrentItem(null);
            player.getInventory().setHelmet(getItemPlaceholders(player));
            return getItem;
        }
        ItemStack getItem = VoltrazCosmetics.getInstance().getVersion().getItemSavedWithNBTsUpdated(combinedItem, currentItemSaved.clone());
        setAndSaveCurrentItem(null);
        combinedItem = null;
        player.getInventory().setHelmet(getItemPlaceholders(player));
        return getItem;
    }

    @Override
    public void dropItem(boolean all) {
        recoverCurrentItem();
        if(currentItemSaved == null) return;
        //Bukkit.getLogger().info("Current Item Saved: " + currentItemSaved.getType().name());
        
        ItemStack getItem = currentItemSaved.clone();
        int amount = getItem.getAmount();
        if (!all) {
            getItem.setAmount(1);
            if(amount <= 1) {
                setAndSaveCurrentItem(null);
            } else {
                currentItemSaved.setAmount(amount - 1);
                setAndSaveCurrentItem(currentItemSaved);
            }
        }else {
            getItem.setAmount(amount);
            setAndSaveCurrentItem(null);
        }
        Location location = player.getEyeLocation();
        location.setY(location.getY() - 0.30000001192092896);
        Item itemEntity = player.getWorld().dropItem(location, getItem);
        itemEntity.setThrower(player.getUniqueId());
        itemEntity.setVelocity(Utils.getItemDropVelocity(player));
        itemEntity.setPickupDelay(40);
        
        if(!overlaps) {
            FoliaUtil.runTask(VoltrazCosmetics.getInstance(), player, () -> {
                player.getInventory().setHelmet(getItemPlaceholders(player));
                player.updateInventory();
            });
        }
    }

    private ItemStack combinedItems(ItemStack originalItem) {
        this.setAndSaveCurrentItem(originalItem != null ? originalItem.clone() : null);
        ItemStack cosmeticItem = getItemPlaceholders(player);
        if(currentItemSaved == null) return cosmeticItem;
        ItemMeta cosmeticMeta = cosmeticItem.getItemMeta();
        ItemMeta itemSaveMeta = (currentItemSaved.hasItemMeta() ? currentItemSaved.getItemMeta() : Bukkit.getItemFactory().getItemMeta(currentItemSaved.getType()));
        if(cosmeticMeta == null || itemSaveMeta == null) return cosmeticItem;
        if(!itemSaveMeta.getItemFlags().isEmpty())
            cosmeticMeta.addItemFlags(itemSaveMeta.getItemFlags().toArray(new ItemFlag[0]));
        itemSaveMeta.getEnchants().forEach((enchantment, level) -> cosmeticMeta.addEnchant(enchantment, level, true));
        List<String> lore = cosmeticMeta.hasLore() ? cosmeticMeta.getLore() : new ArrayList<>();
        if(itemSaveMeta.getLore() != null && !itemSaveMeta.getLore().isEmpty()) {
            lore.add("");
            lore.addAll(itemSaveMeta.getLore());
        }
        cosmeticMeta.setLore(lore);

        Multimap<Attribute, AttributeModifier> attributes = itemSaveMeta.getAttributeModifiers() == null ? DefaultAttributes.defaultsOf(currentItemSaved) : itemSaveMeta.getAttributeModifiers();
        cosmeticMeta.setAttributeModifiers(attributes);
        cosmeticItem.setItemMeta(cosmeticMeta);
        cosmeticItem = VoltrazCosmetics.getInstance().getVersion().getItemWithNBTsCopy(currentItemSaved, cosmeticItem);
        return cosmeticItem;
    }

    @Override
    public void lendToEntity() {
        if(lendEntity.getEquipment() == null) return;
        if(lendEntity.getEquipment().getHelmet() != null && lendEntity.getEquipment().getHelmet().isSimilar(getItemColor(player))) return;
        lendEntity.getEquipment().setHelmet(getItemColor(player));
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
        if(hideCosmetic)
            remove();
        else
            update();
    }

    @Override
    public void remove() {
        recoverCurrentItem();
        if(!overlaps) {
            if(currentItemSaved == null) {
                player.getInventory().setHelmet(null);
            } else {
                player.getInventory().setHelmet(currentItemSaved.clone());
                setAndSaveCurrentItem(null);
            }
            return;
        }
        if(currentItemSaved != null){
            //Clear Hat With helmet save in cache
            player.getInventory().setHelmet(currentItemSaved.clone());
            setAndSaveCurrentItem(null);
            return;
        }
        player.getInventory().setHelmet(null);
    }

    @Override
    public void forceRemove() {
        setAndSaveCurrentItem(null);
    }

    @Override
    public ItemStack getSavedItemForDeath() {
        recoverCurrentItem();
        if(currentItemSaved == null) return null;
        if(overlaps && combinedItem != null) {
            return VoltrazCosmetics.getInstance().getVersion()
                    .getItemSavedWithNBTsUpdated(combinedItem, currentItemSaved.clone());
        }
        return currentItemSaved.clone();
    }

    @Override
    public void clearClose() {
        closing = true;
        recoverCurrentItem();
        if(!overlaps) {
            if(currentItemSaved == null) {
                player.getInventory().setHelmet(null);
            } else {
                player.getInventory().setHelmet(currentItemSaved.clone());
                setAndSaveCurrentItem(null);
            }
            player.updateInventory();
            return;
        }
        if(currentItemSaved != null){
            //Clear Hat With helmet save in cache
            player.getInventory().setHelmet(currentItemSaved.clone());
            setAndSaveCurrentItem(null);
            player.updateInventory();
            return;
        }
        player.getInventory().setHelmet(null);
        player.updateInventory();
    }

    public boolean isOverlaps() {
        return overlaps;
    }

    public double getOffSetY() {
        return isHideCosmetic() ? 0 : offSetY;
    }

    public ItemStack getCurrentItemSaved() {
        return currentItemSaved;
    }

    public void recoverCurrentItem() {
        if (player == null || currentItemSaved != null) return;
        try {
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(VoltrazCosmetics.getInstance(), "original_helmet");
            if (player.getPersistentDataContainer().has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
                String base64 = player.getPersistentDataContainer().get(key, org.bukkit.persistence.PersistentDataType.STRING);
                if (base64 != null && !base64.isEmpty()) {
                    ItemStack recovered = Utils.itemFromBase64(base64);
                    if (recovered != null) {
                        currentItemSaved = recovered;
                    } else {
                        VoltrazCosmetics.getInstance().getLogger().warning(
                            "Failed to deserialize saved helmet for " + player.getName() + " - item data may be corrupted");
                        // Remove corrupted PDC entry to prevent repeated failures
                        player.getPersistentDataContainer().remove(key);
                    }
                }
            }
        } catch (Exception e) {
            VoltrazCosmetics.getInstance().getLogger().warning(
                "Error recovering saved helmet for " + player.getName() + ": " + e.getMessage());
        }
    }

    private void setAndSaveCurrentItem(ItemStack item) {
        this.currentItemSaved = item;
        if (player == null) return;
        try {
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(VoltrazCosmetics.getInstance(), "original_helmet");
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

    public boolean isClosing() {
        return closing;
    }

    public void setClosing(boolean closing) {
        this.closing = closing;
    }

    @Override
    public ItemStack getEquipment() {
        return player.getInventory().getHelmet();
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

