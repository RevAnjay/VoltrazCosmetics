package xyz.voltraz.cosmetics.cache;

import xyz.voltraz.cosmetics.VoltrazCosmetics;
import xyz.voltraz.cosmetics.api.Cosmetic;
import xyz.voltraz.cosmetics.api.CosmeticType;
import xyz.voltraz.cosmetics.cache.cosmetics.backpacks.Bag;
import xyz.voltraz.cosmetics.cache.cosmetics.balloons.Balloon;
import xyz.voltraz.cosmetics.nms.NPC.ItemSlot;
import xyz.voltraz.cosmetics.utils.XMaterial;
import org.bukkit.Color;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import org.bukkit.inventory.ItemStack;

import java.util.*;

public class EntityCache {
    private static final ItemStack AIR_ITEM = XMaterial.AIR.parseItem();
    public static Map<UUID, EntityCache> entities = new java.util.concurrent.ConcurrentHashMap<>();
    private final UUID uniqueId;
    private boolean npc = false;
    private Entity entity;
    private Cosmetic hat;
    private Cosmetic bag;
    private Cosmetic wStick;
    private Cosmetic balloon;

    public EntityCache(UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    public EntityCache(Entity entity) {
        this.uniqueId = entity.getUniqueId();
        this.entity = entity;
    }

    public static EntityCache getEntity(UUID uniqueId) {
        if (!entities.containsKey(uniqueId)) {
            return null;
        }
        return entities.get(uniqueId);
    }

    public static EntityCache getEntityOrCreate(Entity entity) {
        if (!entities.containsKey(entity.getUniqueId())) {
            entities.put(entity.getUniqueId(), new EntityCache(entity));
        }
        return entities.get(entity.getUniqueId());
    }

    public static void removeEntity(UUID uniqueId){
        EntityCache cache = entities.remove(uniqueId);
        if(cache != null) {
            cache.clearCosmeticsInUse();
        }
    }

    public static void cleanupInvalid() {
        entities.entrySet().removeIf(entry -> {
            EntityCache cache = entry.getValue();
            if(cache.entity == null) return true;
            if(cache.entity.isDead() || !cache.entity.isValid()) {
                cache.clearCosmeticsInUse();
                return true;
            }
            return false;
        });
    }

    public static void clearAll() {
        for(EntityCache cache : entities.values()) {
            cache.clearCosmeticsInUse();
        }
        entities.clear();
    }

    public boolean hasEquipped(String cosmeticId){
        if(hat != null){
            if(hat.getId().equals(cosmeticId)) return true;
        }
        if(bag != null){
            if(bag.getId().equals(cosmeticId)) return true;
        }
        if(wStick != null){
            if(wStick.getId().equals(cosmeticId)) return true;
        }
        if(balloon != null){
            return balloon.getId().equals(cosmeticId);
        }
        return false;
    }

    public boolean hasEquipped(Cosmetic cosmetic){
        switch (cosmetic.getCosmeticType()){
            case HAT:
                if(hat == null) return false;
                return hat.getId().equals(cosmetic.getId());
            case BAG:
                if(bag == null) return false;
                return bag.getId().equals(cosmetic.getId());
            case WALKING_STICK:
                if(wStick == null) return false;
                return wStick.getId().equals(cosmetic.getId());
            case BALLOON:
                if(balloon == null) return false;
                return balloon.getId().equals(cosmetic.getId());
        }
        return false;
    }

    public void unSetCosmetic(CosmeticType cosmetic) {
        switch (cosmetic) {
            case HAT:
                clearHat();
                this.hat = null;
                break;
            case BAG:
                clearBag();
                this.bag = null;
                break;
            case WALKING_STICK:
                clearWStick();
                this.wStick = null;
                break;
            case BALLOON:
                clearBalloon();
                this.balloon = null;
                break;
        }
    }

    public void setCosmetic(Cosmetic cosmetic) {
        switch (cosmetic.getCosmeticType()) {
            case HAT:
                clearHat();
                this.hat = cosmetic;
                break;
            case BAG:
                clearBag();
                this.bag = cosmetic;
                break;
            case WALKING_STICK:
                clearWStick();
                this.wStick = cosmetic;
                break;
            case BALLOON:
                clearBalloon();
                this.balloon = cosmetic;
                break;
        }
    }

    public void activeCosmetics(){
        activeHat();
        activeBag();
        activeWStick();
        activeBalloon();
    }

    public void activeCosmeticsInInventory() {
        activeHat();
        activeBag();
        activeWStick();
    }

    public void clearCosmeticsInUse(){
        clearBalloon();
        clearBag();
        clearHat();
        clearWStick();
    }

    public void activeHat(){
        if(hat == null) return;
        if(!(entity instanceof LivingEntity)) return;
        if(!npc){
            LivingEntity livingEntity = (LivingEntity) entity;
            if(livingEntity instanceof Player){
                Player player = (Player) livingEntity;
                VoltrazCosmetics.getInstance().getVersion().equip(livingEntity, ItemSlot.HELMET, hat.getItemColor(player));
                return;
            }
            VoltrazCosmetics.getInstance().getVersion().equip(livingEntity, ItemSlot.HELMET, hat.getItemColor());
            return;
        }
        VoltrazCosmetics.getInstance().getCitizens().EquipmentNPC(ItemSlot.HELMET, getUniqueId(), hat.getItemColor());
    }

    public void activeBag(){
        if(bag == null) return;
        ((Bag)bag).active(getEntityOrCreate());
    }

    public void activeWStick(){
        if(wStick == null) return;
        if(!(entity instanceof LivingEntity)) return;
        if(!npc){
            LivingEntity livingEntity = (LivingEntity) entity;
            if(livingEntity instanceof Player){
                Player player = (Player) livingEntity;
                VoltrazCosmetics.getInstance().getVersion().equip(livingEntity, ItemSlot.OFF_HAND, wStick.getItemColor(player));
                return;
            }
            VoltrazCosmetics.getInstance().getVersion().equip(livingEntity, ItemSlot.OFF_HAND, wStick.getItemColor());
            return;
        }
        VoltrazCosmetics.getInstance().getCitizens().EquipmentNPC(ItemSlot.OFF_HAND, getUniqueId(), wStick.getItemColor());
    }

    public void activeBalloon(){
        if(balloon == null) return;
        ((Balloon)balloon).active(getEntityOrCreate());
    }

    public void clearHat(){
        if(hat == null){
            return;
        }
        if(!(entity instanceof LivingEntity)) return;
        if(!npc){
            LivingEntity livingEntity = (LivingEntity) entity;
            VoltrazCosmetics.getInstance().getVersion().equip(livingEntity, ItemSlot.HELMET, AIR_ITEM);
            return;
        }
        VoltrazCosmetics.getInstance().getCitizens().EquipmentNPC(ItemSlot.HELMET, getUniqueId(), AIR_ITEM);
    }

    public void clearBag(){
        if(bag == null) return;
        bag.remove();
    }

    public void clearWStick(){
        if(wStick == null){
            return;
        }
        if(!(entity instanceof LivingEntity)) return;
        if(!npc){
            LivingEntity livingEntity = (LivingEntity) entity;
            VoltrazCosmetics.getInstance().getVersion().equip(livingEntity, ItemSlot.OFF_HAND, AIR_ITEM);
            return;
        }
        VoltrazCosmetics.getInstance().getCitizens().EquipmentNPC(ItemSlot.OFF_HAND, getUniqueId(), AIR_ITEM);
    }

    public void clearBalloon(){
        if(balloon == null){
            return;
        }
        balloon.remove();
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public Entity getEntityOrCreate(){
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public Cosmetic getHat() {
        return hat;
    }

    public Cosmetic getBag() {
        return bag;
    }

    public Cosmetic getWStick() {
        return wStick;
    }

    public Cosmetic getBalloon() {
        return balloon;
    }

    public void loadCosmetics(String ids){
        if(ids.isEmpty()) return;
        List<String> cosmetics = new ArrayList<>(Arrays.asList(ids.split(",")));
        for(String cosmetic : cosmetics){
            if(cosmetic.isEmpty()) continue;
            String[] color = cosmetic.split("\\|");
            if(color.length > 1){
                Cosmetic cosmetic1 = Cosmetic.getCloneCosmetic(color[0]);
                if(cosmetic1 == null) continue;
                cosmetic1.setColor(Color.fromRGB(Integer.parseInt(color[1])));
                setCosmetic(cosmetic1);
                continue;
            }
            Cosmetic cosmetic1 = Cosmetic.getCloneCosmetic(cosmetic);
            if(cosmetic1 == null) continue;
            setCosmetic(cosmetic1);
        }
    }

    public String saveCosmetics() {
        return saveHat() + "," + saveBag() + "," + saveWStick() + "," + saveBalloon();
    }

    public String saveHat(){
        if(hat == null) return "";
        if(hat.getColor() == null) return hat.getId();
        return hat.getId() + "|" + hat.getColor().asRGB();
    }

    public String saveBag(){
        if(bag == null) return "";
        if(bag.getColor() == null) return bag.getId();
        return bag.getId() + "|" + bag.getColor().asRGB();
    }

    public String saveWStick(){
        if(wStick == null) return "";
        if(wStick.getColor() == null) return wStick.getId();
        return wStick.getId() + "|" + wStick.getColor().asRGB();
    }

    public String saveBalloon(){
        if(balloon == null) return "";
        if(balloon.getColor() == null) return balloon.getId();
        return balloon.getId() + "|" + balloon.getColor().asRGB();
    }

    public boolean isCosmeticUse(){
        return hat != null || bag != null || wStick != null || balloon != null;
    }

    public void setNpc(boolean npc) {
        this.npc = npc;
    }
}
