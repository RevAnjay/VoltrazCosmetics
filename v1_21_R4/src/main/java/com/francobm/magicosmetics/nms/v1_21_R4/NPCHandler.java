package com.francobm.magicosmetics.nms.v1_21_R4;

import com.francobm.magicosmetics.nms.NPC.ItemSlot;
import com.francobm.magicosmetics.nms.NPC.NPC;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.datafixers.util.Pair;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Rotations;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_21_R4.CraftServer;
import org.bukkit.craftbukkit.v1_21_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R4.entity.CraftArmorStand;
import org.bukkit.craftbukkit.v1_21_R4.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_21_R4.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_21_R4.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class NPCHandler extends NPC {
    private ArmorStand balloon;
    private net.minecraft.world.entity.LivingEntity leashed;

    @Override
    public void spawnPunch(Player player, Location location) {
        ServerPlayer entityPlayer = ((CraftPlayer)player).getHandle();
        net.minecraft.world.entity.LivingEntity entityPunch = ((CraftLivingEntity)this.punch).getHandle();
        entityPunch.setPos(location.getX(), location.getY(), location.getZ()); entityPunch.forceSetRotation(location.getYaw(), location.getPitch());;
        float yaw = location.getYaw() * 256.0F / 360.0F;
        ClientboundAddEntityPacket packetPlayOutSpawnEntity = new ClientboundAddEntityPacket(entityPunch.getId(), entityPunch.getUUID(), location.getX(), location.getY(), location.getZ(), location.getPitch(), location.getYaw(), entityPunch.getType(), 0, entityPunch.getDeltaMovement(), entityPunch.getYRot());
        entityPlayer.connection.send(packetPlayOutSpawnEntity);
        entityPlayer.connection.send(new ClientboundRotateHeadPacket(entityPunch, (byte) yaw));
        entityPlayer.connection.send(new ClientboundSetEntityDataPacket(entityPunch.getId(), entityPunch.getEntityData().getNonDefaultValues()));
        entityPlayer.connection.send(new ClientboundSetCameraPacket(entityPunch));
    }

    @Override
    public void addNPC(Player player) {
        addNPC(player, player.getLocation());
    }

    @Override
    public void addNPC(Player player, Location location) {
        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        ServerLevel world = ((CraftWorld) player.getWorld()).getHandle();
        GameProfile gameProfile = new GameProfile(UUID.randomUUID(), player.getName());
        ServerPlayer npc = new ServerPlayer(server, world, gameProfile, ClientInformation.createDefault());

        ArmorStand armorStand = new ArmorStand(EntityType.ARMOR_STAND, world);
        armorStand.setInvisible(true);
        armorStand.setInvulnerable(true);
        armorStand.setPos(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ()); armorStand.forceSetRotation(player.getLocation().getYaw(), 0);;
        npc.setPos(location.getX(), location.getY(), location.getZ()); npc.forceSetRotation(location.getYaw(), 0);;
        //balloon
        balloon = new ArmorStand(EntityType.ARMOR_STAND, world);
        balloon.setInvulnerable(true);
        balloon.setInvisible(true);
        ArmorStand entityPunch = new ArmorStand(EntityType.ARMOR_STAND, world);
        entityPunch.setInvulnerable(true);
        entityPunch.setInvisible(true);
        entityPunch.setPos(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ()); entityPunch.forceSetRotation(player.getLocation().getYaw(), player.getLocation().getPitch());;
        leashed = new Pufferfish(EntityType.PUFFERFISH, world);
        ((Pufferfish)leashed).setLeashData(null);
        leashed.setInvulnerable(true);
        leashed.setInvisible(true);
        leashed.setSilent(true);
        //balloon
        //skin
        try {
            String[] skin = getFromPlayer(player);
            npc.getBukkitEntity().getProfile().getProperties().put("textures", new Property("textures", skin[0], skin[1]));
        }catch (NoSuchElementException ignored){

        }
        //skin
        //
        this.entity = npc.getBukkitEntity();
        this.punch = entityPunch.getBukkitEntity();
        this.armorStand = armorStand.getBukkitEntity();

        addNPC(this, player);
    }

    @Override
    public void removeNPC(Player player) {
        ServerGamePacketListenerImpl connection = ((CraftPlayer)player).getHandle().connection;
        connection.send(new ClientboundRemoveEntitiesPacket(armorStand.getEntityId(), entity.getEntityId(), punch.getEntityId(), balloon.getId(), leashed.getId()));
    }

    @Override
    public void removeBalloon(Player player) {
        ServerGamePacketListenerImpl connection = ((CraftPlayer)player).getHandle().connection;
        connection.send(new ClientboundRemoveEntitiesPacket(balloon.getId(), leashed.getId()));
    }

    @Override
    public void spawnNPC(Player player) {
        Location npcLocation = entity.getLocation();
        Location armorStandLocation = armorStand.getLocation();
        ServerPlayer entityPlayer = ((CraftPlayer)player).getHandle();
        ServerPlayer npc = ((CraftPlayer)this.entity).getHandle();
        ArmorStand armorStand = ((CraftArmorStand)this.armorStand).getHandle();
        armorStand.setInvulnerable(true);
        armorStand.setInvisible(true);
        npc.connection = entityPlayer.connection;
        ClientboundAddEntityPacket npcSpawnPacket = new ClientboundAddEntityPacket(npc.getId(), npc.getUUID(), npcLocation.getX(), npcLocation.getY(), npcLocation.getZ(), npcLocation.getPitch(), npcLocation.getYaw(), npc.getType(), 0, npc.getDeltaMovement(), npc.getYRot());
        ClientboundAddEntityPacket armorStandSpawnPacket = new ClientboundAddEntityPacket(armorStand.getId(), armorStand.getUUID(), armorStandLocation.getX(), armorStandLocation.getY(), armorStandLocation.getZ(), armorStandLocation.getPitch(), armorStandLocation.getYaw(), armorStand.getType(), 0, armorStand.getDeltaMovement(), armorStand.getYRot());
        entityPlayer.connection.send(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, npc));
        entityPlayer.connection.send(npcSpawnPacket);
        entityPlayer.connection.send(new ClientboundRotateHeadPacket(npc, (byte) (player.getLocation().getYaw() * 256 / 360)));
        entityPlayer.connection.send(armorStandSpawnPacket);
        //client settings
        entityPlayer.connection.send(new ClientboundSetEntityDataPacket(armorStand.getId(), armorStand.getEntityData().getNonDefaultValues()));
        //
        SynchedEntityData watcher = npc.getEntityData();
        byte bitmask = ((CraftPlayer)player).getHandle().getEntityData().get(new EntityDataAccessor<>(17, EntityDataSerializers.BYTE));
        watcher.set(new EntityDataAccessor<>(17, EntityDataSerializers.BYTE), bitmask);
        entityPlayer.connection.send(new ClientboundSetEntityDataPacket(npc.getId(), watcher.getNonDefaultValues()));
        new BukkitRunnable() {
            @Override
            public void run() {
                entityPlayer.connection.send(new ClientboundPlayerInfoRemovePacket(Collections.singletonList(npc.getBukkitEntity().getUniqueId())));
            }
        }.runTaskLater(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("MagicCosmetics")), 20L);
        addPassenger(player);
    }

    @Override
    public void lookNPC(Player player, float yaw) {
        ServerPlayer entityPlayer = ((CraftPlayer)this.entity).getHandle();
        ArmorStand armorStand = ((CraftArmorStand)this.armorStand).getHandle();
        armorStand.setInvulnerable(true);
        armorStand.setInvisible(true);
        ServerGamePacketListenerImpl connection = ((CraftPlayer)player).getHandle().connection;
        connection.send(new ClientboundRotateHeadPacket(armorStand, (byte)(yaw * 256 / 360)));
        connection.send(new ClientboundMoveEntityPacket.Rot(armorStand.getId(), (byte)(yaw * 256 / 360), (byte)0, true));

        connection.send(new ClientboundRotateHeadPacket(entityPlayer, (byte)(yaw * 256 / 360)));
        connection.send(new ClientboundMoveEntityPacket.Rot(entityPlayer.getId(), (byte)(yaw * 256 / 360), (byte)0, true));
    }

    @Override
    public void armorStandSetItem(Player player, ItemStack itemStack) {
        ArmorStand entityPlayer = ((CraftArmorStand)this.armorStand).getHandle();
        ServerGamePacketListenerImpl connection = ((CraftPlayer)player).getHandle().connection;
        ArrayList<Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> list = new ArrayList<>();
        list.add(new Pair<>(EquipmentSlot.HEAD, CraftItemStack.asNMSCopy(itemStack)));
        connection.send(new ClientboundSetEquipmentPacket(entityPlayer.getId(), list));
    }

    @Override
    public void balloonSetItem(Player player, ItemStack itemStack) {
        ServerGamePacketListenerImpl connection = ((CraftPlayer)player).getHandle().connection;
        ArrayList<Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> list = new ArrayList<>();
        if(isBigHead()){
            list.add(new Pair<>(EquipmentSlot.MAINHAND, CraftItemStack.asNMSCopy(itemStack)));
        }else {
            list.add(new Pair<>(EquipmentSlot.HEAD, CraftItemStack.asNMSCopy(itemStack)));
        }
        connection.send(new ClientboundSetEquipmentPacket(balloon.getId(), list));
    }

    @Override
    public void balloonNPC(Player player, Location location, ItemStack itemStack, boolean bigHead){
        removeBalloon(player);
        //balloon
        ServerPlayer entityPlayer = ((CraftPlayer)this.entity).getHandle();
        ServerPlayer realPlayer = ((CraftPlayer)player).getHandle();
        balloonPosition = location.clone();
        Location balloonPos = location.clone();
        balloonPos.setX(balloonPos.getY()-1.3);
        balloon.setPos(balloonPos.getX(), balloonPos.getY(), balloonPos.getZ()); balloon.forceSetRotation(balloonPos.getYaw(), balloonPos.getPitch());;

        leashed.setPos(location.getX(), location.getY(), location.getZ()); leashed.forceSetRotation(location.getYaw(), location.getPitch());;
        this.bigHead = bigHead;
        if(isBigHead()){
            balloon.setHeadPose(new Rotations(balloon.getHeadPose().x(), 0, 0));
        }
        ClientboundAddEntityPacket balloonSpawnPacket = new ClientboundAddEntityPacket(balloon.getId(), balloon.getUUID(), balloonPos.getX(), balloonPos.getY(), balloonPos.getZ(), balloonPos.getPitch(), balloonPos.getYaw(), balloon.getType(), 0, balloon.getDeltaMovement(), balloon.getYRot());
        ClientboundAddEntityPacket leashedSpawnPacket = new ClientboundAddEntityPacket(leashed.getId(), leashed.getUUID(), balloonPosition.getX(), balloonPosition.getY(), balloonPosition.getZ(), balloonPosition.getPitch(), balloonPosition.getYaw(), leashed.getType(), 0, leashed.getDeltaMovement(), leashed.getYRot());
        realPlayer.connection.send(balloonSpawnPacket);
        realPlayer.connection.send(leashedSpawnPacket);
        realPlayer.connection.send(new ClientboundSetEntityDataPacket(balloon.getId(), balloon.getEntityData().getNonDefaultValues()));
        realPlayer.connection.send(new ClientboundSetEntityDataPacket(leashed.getId(), leashed.getEntityData().getNonDefaultValues()));
        realPlayer.connection.send(new ClientboundSetEntityLinkPacket(leashed, entityPlayer));
        balloonSetItem(player, itemStack);
    }

    @Override
    public void equipNPC(Player player, ItemSlot itemSlot, ItemStack itemStack) {
        ServerPlayer entityPlayer = ((CraftPlayer)this.entity).getHandle();
        ServerGamePacketListenerImpl connection = ((CraftPlayer)player).getHandle().connection;
        ArrayList<Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> list = new ArrayList<>();
        switch (itemSlot){
            case MAIN_HAND:
                list.add(new Pair<>(EquipmentSlot.MAINHAND, CraftItemStack.asNMSCopy(itemStack)));
                connection.send(new ClientboundSetEquipmentPacket(entityPlayer.getId(), list));
                break;
            case OFF_HAND:
                list.add(new Pair<>(EquipmentSlot.OFFHAND, CraftItemStack.asNMSCopy(itemStack)));
                connection.send(new ClientboundSetEquipmentPacket(entityPlayer.getId(), list));
                break;
            case BOOTS:
                list.add(new Pair<>(EquipmentSlot.FEET, CraftItemStack.asNMSCopy(itemStack)));
                connection.send(new ClientboundSetEquipmentPacket(entityPlayer.getId(), list));
                break;
            case LEGGINGS:
                list.add(new Pair<>(EquipmentSlot.LEGS, CraftItemStack.asNMSCopy(itemStack)));
                connection.send(new ClientboundSetEquipmentPacket(entityPlayer.getId(), list));
                break;
            case CHESTPLATE:
                list.add(new Pair<>(EquipmentSlot.CHEST, CraftItemStack.asNMSCopy(itemStack)));
                connection.send(new ClientboundSetEquipmentPacket(entityPlayer.getId(), list));
                break;
            case HELMET:
                list.add(new Pair<>(EquipmentSlot.HEAD, CraftItemStack.asNMSCopy(itemStack)));
                connection.send(new ClientboundSetEquipmentPacket(entityPlayer.getId(), list));
                break;
        }
    }

    @Override
    public void addPassenger(Player player) {
        if(entity == null) return;
        ArmorStand armorStand = ((CraftArmorStand)this.armorStand).getHandle();
        ServerPlayer p = ((CraftPlayer)player).getHandle();
        ServerPlayer entityPlayer = ((CraftPlayer)this.entity).getHandle();
        ClientboundSetPassengersPacket packetPlayOutMount = this.createDataSerializer(packetDataSerializer -> {
            packetDataSerializer.writeVarInt(entityPlayer.getId());
            packetDataSerializer.writeVarIntArray(new int[]{armorStand.getId()});
            return ClientboundSetPassengersPacket.STREAM_CODEC.decode(packetDataSerializer);
        });
        p.connection.send(packetPlayOutMount);
    }

    public void addPassenger(Player player, net.minecraft.world.entity.Entity entity1, net.minecraft.world.entity.Entity entity2) {
        if(entity1 == null) return;
        if(entity2 == null) return;
        ServerPlayer p = ((CraftPlayer)player).getHandle();
        ClientboundSetPassengersPacket packetPlayOutMount = this.createDataSerializer(packetDataSerializer -> {
            packetDataSerializer.writeVarInt(entity1.getId());
            packetDataSerializer.writeVarIntArray(new int[]{entity2.getId()});
            return ClientboundSetPassengersPacket.STREAM_CODEC.decode(packetDataSerializer);
        });
        p.connection.send(packetPlayOutMount);
    }

    public void animation(Player player){
        if(isBigHead()) {
            animationBigHead(player);
            return;
        }
        ServerPlayer p = ((CraftPlayer)player).getHandle();
        //
        if(balloonPosition == null) return;
        if (!floatLoop) {
            y += 0.01;
            balloonPosition.add(0, 0.01, 0);
            if (y > 0.10) {
                floatLoop = true;
            }
        } else {
            y -= 0.01;
            balloonPosition.subtract(0, 0.01, 0);
            if (y < (-0.11 + 0)) {
                floatLoop = false;
                rotate *= -1;
            }
        }
        if (!rotateLoop) {
            rot += 0.01;
            balloon.setBodyPose(new Rotations(balloon.getBodyPose().x() - 0.5f, balloon.getBodyPose().y(), balloon.getBodyPose().z() + rotate));
            if (rot > 0.20) {
                rotateLoop = true;
            }
        } else {
            rot -= 0.01;
            balloon.setBodyPose(new Rotations(balloon.getBodyPose().x() + 0.5f, balloon.getBodyPose().y(), balloon.getBodyPose().z() + rotate));
            if (rot < -0.20) {
                rotateLoop = false;
            }
        }
        leashed.setPos(balloonPosition.getX(), balloonPosition.getY(), balloonPosition.getZ()); leashed.forceSetRotation(balloonPosition.getYaw(), balloonPosition.getPitch());;
        balloon.setPos(balloonPosition.getX(), balloonPosition.getY() - 1.3, balloonPosition.getZ()); balloon.forceSetRotation(balloonPosition.getYaw(), balloonPosition.getPitch());;
        p.connection.send(new ClientboundSetEntityDataPacket(balloon.getId(), balloon.getEntityData().getNonDefaultValues()));
        p.connection.send(new ClientboundTeleportEntityPacket(leashed.getId(), PositionMoveRotation.of(leashed), java.util.Set.of(), false));
        p.connection.send(new ClientboundTeleportEntityPacket(balloon.getId(), PositionMoveRotation.of(balloon), java.util.Set.of(), false));
    }

    public void animationBigHead(Player player){
        ServerPlayer p = ((CraftPlayer)player).getHandle();
        //
        if(balloonPosition == null) return;
        if (!floatLoop) {
            y += 0.01;
            balloonPosition.add(0, 0.01, 0);
            if (y > 0.10) {
                floatLoop = true;
            }
        } else {
            y -= 0.01;
            balloonPosition.subtract(0, 0.01, 0);
            if (y < (-0.11 + 0)) {
                floatLoop = false;
                rotate *= -1;
            }
        }
        if (!rotateLoop) {
            rot += 0.01;
            balloon.setHeadPose(new Rotations(balloon.getHeadPose().x() - 0.5f, balloon.getHeadPose().y(), balloon.getHeadPose().z() + rotate));
            if (rot > 0.20) {
                rotateLoop = true;
            }
        } else {
            rot -= 0.01;
            balloon.setHeadPose(new Rotations(balloon.getHeadPose().x() + 0.5f, balloon.getHeadPose().y(), balloon.getHeadPose().z() + rotate));
            if (rot < -0.20) {
                rotateLoop = false;
            }
        }
        leashed.setPos(balloonPosition.getX(), balloonPosition.getY(), balloonPosition.getZ()); leashed.forceSetRotation(balloonPosition.getYaw(), balloonPosition.getPitch());;
        balloon.setPos(balloonPosition.getX(), balloonPosition.getY() - 1.3, balloonPosition.getZ()); balloon.forceSetRotation(balloonPosition.getYaw(), balloonPosition.getPitch());;
        p.connection.send(new ClientboundSetEntityDataPacket(balloon.getId(), balloon.getEntityData().getNonDefaultValues()));
        p.connection.send(new ClientboundTeleportEntityPacket(leashed.getId(), PositionMoveRotation.of(leashed), java.util.Set.of(), false));
        p.connection.send(new ClientboundTeleportEntityPacket(balloon.getId(), PositionMoveRotation.of(balloon), java.util.Set.of(), false));
    }

    @Override
    public NPC getNPC(Player player) {
        return npcs.get(player.getUniqueId());
    }

    private <T> T createDataSerializer(UnsafeFunction<FriendlyByteBuf, T> callback) {
        FriendlyByteBuf data = new FriendlyByteBuf(Unpooled.buffer());
        T result = null;
        try {
            result = callback.apply(data);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data.release();
        }
        return result;
    }

    @FunctionalInterface
    private interface UnsafeFunction<K, T> {
        T apply(K k) throws Exception;
    }

    public String[] getFromPlayer(Player playerBukkit) throws NoSuchElementException{
        ServerPlayer playerNMS = ((CraftPlayer) playerBukkit).getHandle();
        GameProfile profile = playerNMS.getBukkitEntity().getProfile();

        Property property = profile.getProperties().get("textures").iterator().next();
        String texture = property.value();
        String signature = property.signature();
        return new String[] {texture, signature};
    }
}