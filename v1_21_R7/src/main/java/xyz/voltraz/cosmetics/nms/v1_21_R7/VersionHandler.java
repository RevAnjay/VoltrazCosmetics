package xyz.voltraz.cosmetics.nms.v1_21_R7;
import xyz.voltraz.cosmetics.nms.v1_21_R7.ReflectionUtils;

import xyz.voltraz.cosmetics.nms.IRangeManager;
import xyz.voltraz.cosmetics.nms.NPC.ItemSlot;
import xyz.voltraz.cosmetics.nms.NPC.NPC;
import xyz.voltraz.cosmetics.nms.bag.EntityBag;
import xyz.voltraz.cosmetics.nms.bag.PlayerBag;
import xyz.voltraz.cosmetics.nms.balloon.EntityBalloon;
import xyz.voltraz.cosmetics.nms.balloon.PlayerBalloon;
import xyz.voltraz.cosmetics.nms.spray.CustomSpray;
import xyz.voltraz.cosmetics.nms.v1_21_R7.cache.*;
import xyz.voltraz.cosmetics.nms.v1_21_R7.models.PacketReaderHandler;
import xyz.voltraz.cosmetics.nms.version.Version;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.fish.Pufferfish;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.map.MapView;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Set;

public class VersionHandler extends Version {

    public VersionHandler() {
        this.packetReader = new PacketReaderHandler();
    }

    @Override
    public void setSpectator(Player player) {
        player.setGameMode(GameMode.SPECTATOR);
        ServerPlayer p = ReflectionUtils.getHandle(player);
        ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE, p);
        try {
            Field packetField = packet.getClass().getDeclaredField("entries");
            packetField.setAccessible(true);
            ArrayList<ClientboundPlayerInfoUpdatePacket.Entry> list = Lists.newArrayList();
            list.add(ReflectionUtils.createPlayerInfoEntry(player.getUniqueId(), p.getBukkitEntity().getProfile(),false, 0, GameType.ADVENTURE, p.getTabListDisplayName()));
            packetField.set(packet, list);
            p.connection.send(packet);
            ClientboundGameEventPacket gameEventPacket = new ClientboundGameEventPacket(ClientboundGameEventPacket.CHANGE_GAME_MODE, 3f);
            p.connection.send(gameEventPacket);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void createNPC(Player player) {
        NPC npc = new NPCHandler();
        npc.addNPC(player);
        npc.spawnNPC(player);
    }

    @Override
    public void createNPC(Player player, Location location) {
        NPC npc = new NPCHandler();
        npc.addNPC(player, location);
        npc.spawnNPC(player);
    }

    @Override
    public NPC getNPC(Player player) {
        return NPC.npcs.get(player.getUniqueId());
    }

    @Override
    public void removeNPC(Player player) {
        NPC npc = NPC.npcs.get(player.getUniqueId());
        if(npc == null) return;
        npc.removeNPC(player);
        NPC.npcs.remove(player.getUniqueId());
    }

    @Override
    public NPC getNPC() {
        return new NPCHandler();
    }

    public PlayerBag createPlayerBag(Player player, double distance, float height, ItemStack backPackItem, ItemStack backPackItemForMe, boolean isDisplay) {
        return isDisplay ? new PlayerBagDisplayHandler(player, createRangeManager(player), distance, height, backPackItem, backPackItemForMe) : new PlayerBagHandler(player, createRangeManager(player), distance, height, backPackItem, backPackItemForMe);
    }

    @Override
    public EntityBag createEntityBag(Entity entity, double distance) {
        return new EntityBagHandler(entity, distance);
    }

    @Override
    public PlayerBalloon createPlayerBalloon(Player player, double space, double distance, boolean bigHead, boolean invisibleLeash) {
        return new PlayerBalloonHandler(player, space, distance, bigHead, invisibleLeash);
    }

    @Override
    public EntityBalloon createEntityBalloon(Entity entity, double space, double distance, boolean bigHead, boolean invisibleLeash) {
        return new EntityBalloonHandler(entity, space, distance, bigHead, invisibleLeash);
    }

    @Override
    public CustomSpray createCustomSpray(Player player, Location location, BlockFace blockFace, ItemStack itemStack, MapView mapView, int rotation) {
        return new CustomSprayHandler(player, location, blockFace, itemStack, mapView, rotation);
    }

    @Override
    public void equip(org.bukkit.entity.LivingEntity livingEntity, ItemSlot itemSlot, ItemStack itemStack) {
        ArrayList<Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> list = new ArrayList<>();
        switch (itemSlot){
            case MAIN_HAND:
                list.add(new Pair<>(EquipmentSlot.MAINHAND, ReflectionUtils.asNMSCopy(itemStack)));
                break;
            case OFF_HAND:
                list.add(new Pair<>(EquipmentSlot.OFFHAND, ReflectionUtils.asNMSCopy(itemStack)));
                break;
            case BOOTS:
                list.add(new Pair<>(EquipmentSlot.FEET, ReflectionUtils.asNMSCopy(itemStack)));
                break;
            case LEGGINGS:
                list.add(new Pair<>(EquipmentSlot.LEGS, ReflectionUtils.asNMSCopy(itemStack)));
                break;
            case CHESTPLATE:
                list.add(new Pair<>(EquipmentSlot.CHEST, ReflectionUtils.asNMSCopy(itemStack)));
                break;
            case HELMET:
                list.add(new Pair<>(EquipmentSlot.HEAD, ReflectionUtils.asNMSCopy(itemStack)));
                break;
        }
        ClientboundSetEquipmentPacket packet = new ClientboundSetEquipmentPacket(livingEntity.getEntityId(), list);
        Location entityLoc = livingEntity.getLocation();
        double trackingRangeSq = 48 * 48; // vanilla entity tracking range squared
        for(Player p : Bukkit.getOnlinePlayers()){
            if(!p.getWorld().equals(entityLoc.getWorld())) continue;
            if(p.getLocation().distanceSquared(entityLoc) > trackingRangeSq) continue;
            ReflectionUtils.getHandle(p).connection.send(packet);
        }
    }

    @Override
    public void updateTitle(Player player, String title) {
        ServerPlayer entityPlayer = ReflectionUtils.getHandle(player);
        if(player.getOpenInventory().getTopInventory().getType() != InventoryType.CHEST) return;
        ClientboundOpenScreenPacket packet = null;
        switch (player.getOpenInventory().getTopInventory().getSize()/9){
            case 1:
                packet = new ClientboundOpenScreenPacket(entityPlayer.containerMenu.containerId, MenuType.GENERIC_9x1, ReflectionUtils.fromStringOrNull(title));
                break;
            case 2:
                packet = new ClientboundOpenScreenPacket(entityPlayer.containerMenu.containerId, MenuType.GENERIC_9x2, ReflectionUtils.fromStringOrNull(title));
                break;
            case 3:
                packet = new ClientboundOpenScreenPacket(entityPlayer.containerMenu.containerId, MenuType.GENERIC_9x3, ReflectionUtils.fromStringOrNull(title));
                break;
            case 4:
                packet = new ClientboundOpenScreenPacket(entityPlayer.containerMenu.containerId, MenuType.GENERIC_9x4, ReflectionUtils.fromStringOrNull(title));
                break;
            case 5:
                packet = new ClientboundOpenScreenPacket(entityPlayer.containerMenu.containerId, MenuType.GENERIC_9x5, ReflectionUtils.fromStringOrNull(title));
                break;
            case 6:
                packet = new ClientboundOpenScreenPacket(entityPlayer.containerMenu.containerId, MenuType.GENERIC_9x6, ReflectionUtils.fromStringOrNull(title));
                break;
        }
        if(packet == null) return;
        entityPlayer.connection.send(packet);
        entityPlayer.containerMenu.sendAllDataToRemote();
    }

    @Override
    public void setCamera(Player player, Entity entity) {
        net.minecraft.world.entity.Entity e = ReflectionUtils.getHandle(entity);
        ServerPlayer entityPlayer = ReflectionUtils.getHandle(player);
        entityPlayer.connection.send(new ClientboundSetCameraPacket(e));
    }

    @Override
    public ItemStack setNBTCosmetic(ItemStack itemStack, String key) {
        if(itemStack == null) return null;
        net.minecraft.world.item.ItemStack itemCosmetic = ReflectionUtils.asNMSCopy(itemStack);
        CustomData.update(DataComponents.CUSTOM_DATA, itemCosmetic, nbtTagCompound -> nbtTagCompound.putString("magic_cosmetic", key));
        return ReflectionUtils.asBukkitCopy(itemCosmetic);
    }

    @Override
    public String isNBTCosmetic(ItemStack itemStack) {
        if(itemStack == null) return null;
        net.minecraft.world.item.ItemStack itemCosmetic = ReflectionUtils.asNMSCopy(itemStack);
        if(!itemCosmetic.has(DataComponents.CUSTOM_DATA)) return "";
        CustomData customData = itemCosmetic.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return "";
        return ReflectionUtils.getString(customData.copyTag(), "magic_cosmetic");
    }

    public PufferFish spawnFakePuffer(Location location) {
        Pufferfish entityPufferFish = new Pufferfish(EntityType.PUFFERFISH, ReflectionUtils.getHandle(location.getWorld()));
        ReflectionUtils.absMoveTo(entityPufferFish, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        return (PufferFish) entityPufferFish.getBukkitEntity();
    }

    @Override
    public org.bukkit.entity.ArmorStand spawnArmorStand(Location location) {
        ArmorStand armorStand = new ArmorStand(EntityType.ARMOR_STAND, ReflectionUtils.getHandle(location.getWorld()));
        ReflectionUtils.absMoveTo(armorStand, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        return (org.bukkit.entity.ArmorStand) armorStand.getBukkitEntity();
    }

    public void showEntity(org.bukkit.entity.LivingEntity entity, Player ...viewers) {
        net.minecraft.world.entity.LivingEntity entityClient = ReflectionUtils.getHandle(entity);
        entityClient.setInvisible(true);
        SynchedEntityData dataWatcher = entityClient.getEntityData();
        ClientboundAddEntityPacket packet = new ClientboundAddEntityPacket(entityClient, 0, ReflectionUtils.toBlockPosition(entity.getLocation()));
        ClientboundSetEntityDataPacket metadata = new ClientboundSetEntityDataPacket(entity.getEntityId(), dataWatcher.getNonDefaultValues());
        for(Player viewer : viewers) {
            ServerPlayer view = ReflectionUtils.getHandle(viewer);
            view.connection.send(packet);
            view.connection.send(metadata);
        }
    }

    public void despawnFakeEntity(Entity entity, Player ...viewers) {
        ClientboundRemoveEntitiesPacket packet = new ClientboundRemoveEntitiesPacket(entity.getEntityId());
        for(Player viewer : viewers) {
            ServerPlayer view = ReflectionUtils.getHandle(viewer);
            view.connection.send(packet);
        }
    }

    public void attachFakeEntity(Entity entity, Entity leashed, Player ...viewers) {
        ServerPlayer entityPlayer = (ServerPlayer) ReflectionUtils.getHandle(entity);
        ClientboundSetEntityLinkPacket packet = new ClientboundSetEntityLinkPacket((net.minecraft.world.entity.Entity) ReflectionUtils.getHandle(leashed), entityPlayer);
        for(Player viewer : viewers) {
            ServerPlayer view = ReflectionUtils.getHandle(viewer);
            view.connection.send(packet);
        }
    }

    public void updatePositionFakeEntity(Entity leashed, Location location) {
        net.minecraft.world.entity.Entity entity = ReflectionUtils.getHandle(leashed);
        ReflectionUtils.absMoveTo(entity, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public void teleportFakeEntity(Entity leashed, Set<Player> viewers) {
        net.minecraft.world.entity.Entity entity = ReflectionUtils.getHandle(leashed);
        ClientboundTeleportEntityPacket packet = ReflectionUtils.createTeleportPacket(entity);
        for(Player viewer : viewers) {
            ServerPlayer view = ReflectionUtils.getHandle(viewer);
            view.connection.send(packet);
        }
    }

    @Override
    public ItemStack getItemWithNBTsCopy(ItemStack itemToCopy, ItemStack cosmetic) {
        net.minecraft.world.item.ItemStack copy = ReflectionUtils.asNMSCopy(itemToCopy);
        if(!copy.has(DataComponents.CUSTOM_DATA)) return cosmetic;
        boolean debug = false;
        net.minecraft.world.item.ItemStack cosmeticItem = ReflectionUtils.asNMSCopy(cosmetic);
        CustomData copyCustomData = copy.get(DataComponents.CUSTOM_DATA);
        if(copyCustomData == null) return cosmetic;
        CustomData cosmeticCustomData = cosmeticItem.get(DataComponents.CUSTOM_DATA);
        CompoundTag copyNBT = copyCustomData.copyTag();
        CompoundTag cosmeticNBT = cosmeticCustomData != null ? cosmeticCustomData.copyTag() : new CompoundTag();
        for(String key : ReflectionUtils.getKeys(copyNBT)){
            if(debug) Bukkit.getLogger().info("Key: " + key);
            if((key.equals("display") || key.equals("minecraft:custom_name")) || (key.equals("CustomModelData") || key.equals("minecraft:custom_model_data"))) continue;
            if(key.equals("PublicBukkitValues")) {
                CompoundTag compound = ReflectionUtils.getCompound(copyNBT, key);
                CompoundTag realCompound = ReflectionUtils.getCompound(cosmeticNBT, key);
                Set<String> keys = ReflectionUtils.getKeys(compound);
                for (String compoundKey : keys){
                    if(debug) Bukkit.getLogger().info("Key of key: " + compoundKey);
                    if(compoundKey.contains("magicosmetics") || compoundKey.contains("cosmetic")) continue;
                    realCompound.put(compoundKey, compound.get(compoundKey));
                }
                cosmeticNBT.put(key, realCompound);
                continue;
            }
            cosmeticNBT.put(key, copyNBT.get(key));
        }
        cosmeticItem.set(DataComponents.CUSTOM_DATA, cosmeticCustomData.update(nbtTagCompound -> nbtTagCompound.merge(cosmeticNBT)));
        return ReflectionUtils.asBukkitCopy(cosmeticItem);
    }

    public ItemStack getItemSavedWithNBTsUpdated(ItemStack itemCombined, ItemStack itemStack) {
        net.minecraft.world.item.ItemStack copy = ReflectionUtils.asNMSCopy(itemCombined);
        if(!copy.has(DataComponents.CUSTOM_DATA)) return itemStack;
        net.minecraft.world.item.ItemStack realItem = ReflectionUtils.asNMSCopy(itemStack);
        if(!realItem.has(DataComponents.CUSTOM_DATA)) return itemStack;
        CustomData copyCustomData = copy.get(DataComponents.CUSTOM_DATA);
        CustomData realCustomData = realItem.get(DataComponents.CUSTOM_DATA);
        if(copyCustomData == null || realCustomData == null) return itemStack;
        CompoundTag copyNBT = copyCustomData.copyTag();
        CompoundTag realNBT = realCustomData.copyTag();
        for(String key : ReflectionUtils.getKeys(copyNBT)){
            if((key.equals("display") || key.equals("minecraft:custom_name")) || (key.equals("CustomModelData") || key.equals("minecraft:custom_model_data"))) continue;
            if(key.equals("PublicBukkitValues")) {
                CompoundTag compound = ReflectionUtils.getCompound(copyNBT, key);
                CompoundTag realCompound = ReflectionUtils.getCompound(realNBT, key);
                Set<String> keys = ReflectionUtils.getKeys(compound);
                for (String compoundKey : keys){
                    if(!realCompound.contains(compoundKey)) continue;
                    realCompound.put(compoundKey, compound.get(compoundKey));
                }
                realNBT.put(key, realCompound);
                continue;
            }
            if(!realNBT.contains(key)) continue;
            realNBT.put(key, copyNBT.get(key));
        }
        realItem.set(DataComponents.CUSTOM_DATA, realCustomData.update(nbtTagCompound -> nbtTagCompound.merge(realNBT)));
        return ReflectionUtils.asBukkitCopy(realItem);
    }

    public ItemStack getCustomHead(ItemStack itemStack, String texture){
        if(itemStack == null) return null;
        if(texture.isEmpty()){
            return itemStack;
        }
        PlayerProfile profile = Bukkit.createPlayerProfile(RANDOM_UUID);
        PlayerTextures textures = profile.getTextures();
        URL urlObject;
        try {
            urlObject = new java.net.URI(texture).toURL();
        } catch (MalformedURLException | java.net.URISyntaxException exception) {
            try {
                urlObject = getUrlFromBase64(texture);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        textures.setSkin(urlObject);
        profile.setTextures(textures);
        SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
        if(skullMeta == null) return itemStack;
        skullMeta.setOwnerProfile(profile);
        itemStack.setItemMeta(skullMeta);
        return itemStack;
    }

    @Override
    public IRangeManager createRangeManager(Entity entity) {
        ServerLevel level = ReflectionUtils.getHandle(entity.getWorld());

        ChunkMap.TrackedEntity trackedEntity;
        try {
            trackedEntity = level.getChunkSource().chunkMap.entityMap.get(entity.getEntityId());
        } catch (NoSuchFieldError var8) {
            net.minecraft.world.entity.Entity nmsEntity = ReflectionUtils.getHandle(entity);

            try {
                Field trackerField = nmsEntity.getClass().getField("tracker");
                trackedEntity = (ChunkMap.TrackedEntity)trackerField.get(nmsEntity);
            } catch (IllegalAccessException | NoSuchFieldException var7) {
                return null;
            }
        }

        return new RangeManager(trackedEntity);
    }
}

