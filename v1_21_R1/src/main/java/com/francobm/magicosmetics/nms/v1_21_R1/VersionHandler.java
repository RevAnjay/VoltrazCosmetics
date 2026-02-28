package com.francobm.magicosmetics.nms.v1_21_R1;

import com.francobm.magicosmetics.nms.IRangeManager;
import com.francobm.magicosmetics.nms.NPC.ItemSlot;
import com.francobm.magicosmetics.nms.NPC.NPC;
import com.francobm.magicosmetics.nms.bag.EntityBag;
import com.francobm.magicosmetics.nms.bag.PlayerBag;
import com.francobm.magicosmetics.nms.balloon.EntityBalloon;
import com.francobm.magicosmetics.nms.balloon.PlayerBalloon;
import com.francobm.magicosmetics.nms.spray.CustomSpray;
import com.francobm.magicosmetics.nms.v1_21_R1.cache.*;
import com.francobm.magicosmetics.nms.v1_21_R1.models.PacketReaderHandler;
import com.francobm.magicosmetics.nms.version.Version;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.craftbukkit.util.CraftLocation;
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
        ServerPlayer p = ((CraftPlayer)player).getHandle();
        ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE, p);
        try {
            Field packetField = packet.getClass().getDeclaredField("entries");
            packetField.setAccessible(true);
            ArrayList<ClientboundPlayerInfoUpdatePacket.Entry> list = Lists.newArrayList();
            list.add(new ClientboundPlayerInfoUpdatePacket.Entry(player.getUniqueId(), p.getBukkitEntity().getProfile(),false, 0, GameType.ADVENTURE, p.getTabListDisplayName(), null));
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
                list.add(new Pair<>(EquipmentSlot.MAINHAND, CraftItemStack.asNMSCopy(itemStack)));
                break;
            case OFF_HAND:
                list.add(new Pair<>(EquipmentSlot.OFFHAND, CraftItemStack.asNMSCopy(itemStack)));
                break;
            case BOOTS:
                list.add(new Pair<>(EquipmentSlot.FEET, CraftItemStack.asNMSCopy(itemStack)));
                break;
            case LEGGINGS:
                list.add(new Pair<>(EquipmentSlot.LEGS, CraftItemStack.asNMSCopy(itemStack)));
                break;
            case CHESTPLATE:
                list.add(new Pair<>(EquipmentSlot.CHEST, CraftItemStack.asNMSCopy(itemStack)));
                break;
            case HELMET:
                list.add(new Pair<>(EquipmentSlot.HEAD, CraftItemStack.asNMSCopy(itemStack)));
                break;
        }
        for(Player p : Bukkit.getOnlinePlayers()){
            ServerGamePacketListenerImpl connection = ((CraftPlayer)p).getHandle().connection;
            connection.send(new ClientboundSetEquipmentPacket(livingEntity.getEntityId(), list));
        }
    }

    @Override
    public void updateTitle(Player player, String title) {
        ServerPlayer entityPlayer = ((CraftPlayer)player).getHandle();
        if(player.getOpenInventory().getTopInventory().getType() != InventoryType.CHEST) return;
        ClientboundOpenScreenPacket packet = null;
        switch (player.getOpenInventory().getTopInventory().getSize()/9){
            case 1:
                packet = new ClientboundOpenScreenPacket(entityPlayer.containerMenu.containerId, MenuType.GENERIC_9x1, CraftChatMessage.fromStringOrNull(title));
                break;
            case 2:
                packet = new ClientboundOpenScreenPacket(entityPlayer.containerMenu.containerId, MenuType.GENERIC_9x2, CraftChatMessage.fromStringOrNull(title));
                break;
            case 3:
                packet = new ClientboundOpenScreenPacket(entityPlayer.containerMenu.containerId, MenuType.GENERIC_9x3, CraftChatMessage.fromStringOrNull(title));
                break;
            case 4:
                packet = new ClientboundOpenScreenPacket(entityPlayer.containerMenu.containerId, MenuType.GENERIC_9x4, CraftChatMessage.fromStringOrNull(title));
                break;
            case 5:
                packet = new ClientboundOpenScreenPacket(entityPlayer.containerMenu.containerId, MenuType.GENERIC_9x5, CraftChatMessage.fromStringOrNull(title));
                break;
            case 6:
                packet = new ClientboundOpenScreenPacket(entityPlayer.containerMenu.containerId, MenuType.GENERIC_9x6, CraftChatMessage.fromStringOrNull(title));
                break;
        }
        if(packet == null) return;
        entityPlayer.connection.send(packet);
        entityPlayer.containerMenu.sendAllDataToRemote();
    }

    @Override
    public void setCamera(Player player, Entity entity) {
        net.minecraft.world.entity.Entity e = ((CraftEntity)entity).getHandle();
        ServerPlayer entityPlayer = ((CraftPlayer)player).getHandle();
        entityPlayer.connection.send(new ClientboundSetCameraPacket(e));
    }

    @Override
    public ItemStack setNBTCosmetic(ItemStack itemStack, String key) {
        if(itemStack == null) return null;
        net.minecraft.world.item.ItemStack itemCosmetic = CraftItemStack.asNMSCopy(itemStack);
        CustomData.update(DataComponents.CUSTOM_DATA, itemCosmetic, nbtTagCompound -> nbtTagCompound.putString("magic_cosmetic", key));
        return CraftItemStack.asBukkitCopy(itemCosmetic);
    }

    @Override
    public String isNBTCosmetic(ItemStack itemStack) {
        if(itemStack == null) return null;
        net.minecraft.world.item.ItemStack itemCosmetic = CraftItemStack.asNMSCopy(itemStack);
        if(!itemCosmetic.has(DataComponents.CUSTOM_DATA)) return "";
        CustomData customData = itemCosmetic.get(DataComponents.CUSTOM_DATA);
        return customData.copyTag().getString("magic_cosmetic");
    }

    public PufferFish spawnFakePuffer(Location location) {
        Pufferfish entityPufferFish = new Pufferfish(EntityType.PUFFERFISH, ((CraftWorld)location.getWorld()).getHandle());
        entityPufferFish.absMoveTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        return (PufferFish) entityPufferFish.getBukkitEntity();
    }

    @Override
    public org.bukkit.entity.ArmorStand spawnArmorStand(Location location) {
        ArmorStand armorStand = new ArmorStand(EntityType.ARMOR_STAND, ((CraftWorld)location.getWorld()).getHandle());
        armorStand.absMoveTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        return (org.bukkit.entity.ArmorStand) armorStand.getBukkitEntity();
    }

    public void showEntity(org.bukkit.entity.LivingEntity entity, Player ...viewers) {
        net.minecraft.world.entity.LivingEntity entityClient = ((CraftLivingEntity) entity).getHandle();
        entityClient.setInvisible(true);
        SynchedEntityData dataWatcher = entityClient.getEntityData();
        ClientboundAddEntityPacket packet = new ClientboundAddEntityPacket(entityClient, 0, CraftLocation.toBlockPosition(entity.getLocation()));
        ClientboundSetEntityDataPacket metadata = new ClientboundSetEntityDataPacket(entity.getEntityId(), dataWatcher.getNonDefaultValues());
        for(Player viewer : viewers) {
            ServerPlayer view = ((CraftPlayer)viewer).getHandle();
            view.connection.send(packet);
            view.connection.send(metadata);
        }
    }

    public void despawnFakeEntity(Entity entity, Player ...viewers) {
        ClientboundRemoveEntitiesPacket packet = new ClientboundRemoveEntitiesPacket(entity.getEntityId());
        for(Player viewer : viewers) {
            ServerPlayer view = ((CraftPlayer)viewer).getHandle();
            view.connection.send(packet);
        }
    }

    public void attachFakeEntity(Entity entity, Entity leashed, Player ...viewers) {
        ServerPlayer entityPlayer = ((CraftPlayer) entity).getHandle();
        ClientboundSetEntityLinkPacket packet = new ClientboundSetEntityLinkPacket(((CraftEntity)leashed).getHandle(), entityPlayer);
        for(Player viewer : viewers) {
            ServerPlayer view = ((CraftPlayer)viewer).getHandle();
            view.connection.send(packet);
        }
    }

    public void updatePositionFakeEntity(Entity leashed, Location location) {
        net.minecraft.world.entity.Entity entity = ((CraftEntity)leashed).getHandle();
        entity.absMoveTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public void teleportFakeEntity(Entity leashed, Set<Player> viewers) {
        net.minecraft.world.entity.Entity entity = ((CraftEntity)leashed).getHandle();
        ClientboundTeleportEntityPacket packet = new ClientboundTeleportEntityPacket(entity);
        for(Player viewer : viewers) {
            ServerPlayer view = ((CraftPlayer)viewer).getHandle();
            view.connection.send(packet);
        }
    }

    @Override
    public ItemStack getItemWithNBTsCopy(ItemStack itemToCopy, ItemStack cosmetic) {
        net.minecraft.world.item.ItemStack copy = CraftItemStack.asNMSCopy(itemToCopy);
        if(!copy.has(DataComponents.CUSTOM_DATA)) return cosmetic;
        net.minecraft.world.item.ItemStack cosmeticItem = CraftItemStack.asNMSCopy(cosmetic);
        CustomData copyCustomData = copy.get(DataComponents.CUSTOM_DATA);
        CustomData cosmeticCustomData = cosmeticItem.get(DataComponents.CUSTOM_DATA);
        CompoundTag copyNBT = copyCustomData.copyTag();
        CompoundTag cosmeticNBT = cosmeticCustomData.copyTag();
        for(String key : copyNBT.getAllKeys()){
            Bukkit.getLogger().info("Key: " + key);
            if((key.equals("display") || key.equals("minecraft:custom_name")) || (key.equals("CustomModelData") || key.equals("minecraft:custom_model_data"))) continue;
            if(key.equals("PublicBukkitValues")) {
                CompoundTag compound = copyNBT.getCompound(key);
                CompoundTag realCompound = cosmeticNBT.getCompound(key);
                Set<String> keys = compound.getAllKeys();
                for (String compoundKey : keys){
                    Bukkit.getLogger().info("Key of key: " + compoundKey);
                    realCompound.put(compoundKey, compound.get(compoundKey));
                }
                cosmeticNBT.put(key, realCompound);
                continue;
            }
            cosmeticNBT.put(key, copyNBT.get(key));
        }
        cosmeticItem.set(DataComponents.CUSTOM_DATA, cosmeticCustomData.update(nbtTagCompound -> nbtTagCompound.merge(cosmeticNBT)));
        return CraftItemStack.asBukkitCopy(cosmeticItem);
    }

    public ItemStack getItemSavedWithNBTsUpdated(ItemStack itemCombined, ItemStack itemStack) {
        net.minecraft.world.item.ItemStack copy = CraftItemStack.asNMSCopy(itemCombined);
        if(!copy.has(DataComponents.CUSTOM_DATA)) return itemStack;
        net.minecraft.world.item.ItemStack realItem = CraftItemStack.asNMSCopy(itemStack);
        if(!realItem.has(DataComponents.CUSTOM_DATA)) return itemStack;
        CustomData copyCustomData = copy.get(DataComponents.CUSTOM_DATA);
        CustomData realCustomData = realItem.get(DataComponents.CUSTOM_DATA);
        CompoundTag copyNBT = copyCustomData.copyTag();
        CompoundTag realNBT = realCustomData.copyTag();
        for(String key : copyNBT.getAllKeys()){
            if((key.equals("display") || key.equals("minecraft:custom_name")) || (key.equals("CustomModelData") || key.equals("minecraft:custom_model_data"))) continue;
            if(key.equals("PublicBukkitValues")) {
                CompoundTag compound = copyNBT.getCompound(key);
                CompoundTag realCompound = realNBT.getCompound(key);
                Set<String> keys = compound.getAllKeys();
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
        return CraftItemStack.asBukkitCopy(realItem);
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
            urlObject = new URL(texture);
        } catch (MalformedURLException exception) {
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
        ServerLevel level = ((CraftWorld)entity.getWorld()).getHandle();

        ChunkMap.TrackedEntity trackedEntity;
        try {
            trackedEntity = level.getChunkSource().chunkMap.entityMap.get(entity.getEntityId());
        } catch (NoSuchFieldError var8) {
            net.minecraft.world.entity.Entity nmsEntity = ((CraftEntity)entity).getHandle();

            try {
                Field trackerField = nmsEntity.getClass().getField("tracker");
                trackedEntity = (ChunkMap.TrackedEntity)trackerField.get(nmsEntity);
            } catch (IllegalAccessException | NoSuchFieldException var7) {
                throw new RuntimeException(var7);
            }
        }

        return new RangeManager(trackedEntity);
    }
}
