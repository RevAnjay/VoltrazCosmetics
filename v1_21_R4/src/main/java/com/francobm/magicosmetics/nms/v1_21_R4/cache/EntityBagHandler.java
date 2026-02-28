package com.francobm.magicosmetics.nms.v1_21_R4.cache;

import com.francobm.magicosmetics.nms.bag.EntityBag;
import com.mojang.datafixers.util.Pair;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_21_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R4.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_21_R4.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_21_R4.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_21_R4.util.CraftLocation;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class EntityBagHandler extends EntityBag {
    private final ArmorStand armorStand;
    private final double distance;

    public EntityBagHandler(Entity entity, double distance) {
        players = new CopyOnWriteArrayList<>(new ArrayList<>());
        this.uuid = entity.getUniqueId();
        this.distance = distance;
        this.entity = entity;
        entityBags.put(uuid, this);
        ServerLevel world = ((CraftWorld) entity.getWorld()).getHandle();

        armorStand = new ArmorStand(EntityType.ARMOR_STAND, world);
        armorStand.setPos(entity.getLocation().getX(), entity.getLocation().getY(), entity.getLocation().getZ()); armorStand.forceSetRotation(entity.getLocation().getYaw(), 0);;
        armorStand.setInvisible(true);
        armorStand.setInvulnerable(true);
        armorStand.setMarker(true);

    }

    @Override
    public void spawnBag(Player player) {
        if(players.contains(player.getUniqueId())) {
            if(!getEntity().getWorld().equals(player.getWorld())) {
                remove(player);
                return;
            }
            if(getEntity().getLocation().distanceSquared(player.getLocation()) > distance) {
                remove(player);
            }
            return;
        }
        if(!getEntity().getWorld().equals(player.getWorld())) return;
        if(getEntity().getLocation().distanceSquared(player.getLocation()) > distance) return;
        armorStand.setInvulnerable(true);
        armorStand.setInvisible(true);
        armorStand.setMarker(true);
        Location location = getEntity().getLocation();
        armorStand.setPos(location.getX(), location.getY(), location.getZ()); armorStand.forceSetRotation(location.getYaw(), 0);;

        ServerPlayer entityPlayer = ((CraftPlayer)player).getHandle();
        entityPlayer.connection.send(new ClientboundAddEntityPacket(armorStand, 0, CraftLocation.toBlockPosition(location)));
        entityPlayer.connection.send(new ClientboundSetEntityDataPacket(armorStand.getId(), armorStand.getEntityData().getNonDefaultValues()));
        addPassenger(player, getEntity(), armorStand.getBukkitEntity());
        players.add(player.getUniqueId());
    }

    @Override
    public void spawnBag() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            spawnBag(player);
        }
    }

    @Override
    public void remove() {
        for(UUID uuid : players){
            Player player = Bukkit.getPlayer(uuid);
            if(player == null) {
                players.remove(uuid);
                continue;
            }
            remove(player);
        }
        entityBags.remove(uuid);
    }

    @Override
    public void addPassenger() {
        for(UUID uuid : players){
            Player player = Bukkit.getPlayer(uuid);
            if(player == null) {
                players.remove(uuid);
                continue;
            }
            ServerPlayer entityPlayer = ((CraftPlayer)player).getHandle();
            net.minecraft.world.entity.Entity e = ((CraftEntity)entity).getHandle();
            ClientboundSetPassengersPacket packetPlayOutMount = this.createDataSerializer(packetDataSerializer -> {
                packetDataSerializer.writeVarInt(e.getId());
                packetDataSerializer.writeVarIntArray(new int[]{armorStand.getId()});
                return ClientboundSetPassengersPacket.STREAM_CODEC.decode(packetDataSerializer);
            });
            entityPlayer.connection.send(packetPlayOutMount);
        }
    }

    @Override
    public void addPassenger(Entity entity, Entity passenger) {
        for(UUID uuid : players){
            Player player = Bukkit.getPlayer(uuid);
            if(player == null) {
                players.remove(uuid);
                continue;
            }
            ServerPlayer entityPlayer = ((CraftPlayer)player).getHandle();
            net.minecraft.world.entity.Entity e = ((CraftEntity)entity).getHandle();
            net.minecraft.world.entity.Entity pass = ((CraftEntity)passenger).getHandle();

            ClientboundSetPassengersPacket packetPlayOutMount = this.createDataSerializer(packetDataSerializer -> {
                packetDataSerializer.writeVarInt(e.getId());
                packetDataSerializer.writeVarIntArray(new int[]{pass.getId()});
                return ClientboundSetPassengersPacket.STREAM_CODEC.decode(packetDataSerializer);
            });
            entityPlayer.connection.send(packetPlayOutMount);
        }
    }

    @Override
    public void addPassenger(Player player, Entity entity, Entity passenger) {
        ServerPlayer entityPlayer = ((CraftPlayer)player).getHandle();
        net.minecraft.world.entity.Entity e = ((CraftEntity)entity).getHandle();
        net.minecraft.world.entity.Entity pass = ((CraftEntity)passenger).getHandle();

        ClientboundSetPassengersPacket packetPlayOutMount = this.createDataSerializer(packetDataSerializer -> {
            packetDataSerializer.writeVarInt(e.getId());
            packetDataSerializer.writeVarIntArray(new int[]{pass.getId()});
            return ClientboundSetPassengersPacket.STREAM_CODEC.decode(packetDataSerializer);
        });
        entityPlayer.connection.send(packetPlayOutMount);
    }

    @Override
    public void remove(Player player) {
        ServerGamePacketListenerImpl connection = ((CraftPlayer)player).getHandle().connection;
        connection.send(new ClientboundRemoveEntitiesPacket(armorStand.getId()));
        players.remove(player.getUniqueId());
    }

    @Override
    public void setItemOnHelmet(ItemStack itemStack) {
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if(player == null) {
                players.remove(uuid);
                continue;
            }
            ServerGamePacketListenerImpl connection = ((CraftPlayer)player).getHandle().connection;
            ArrayList<Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> list = new ArrayList<>();
            list.add(new Pair<>(EquipmentSlot.HEAD, CraftItemStack.asNMSCopy(itemStack)));
            connection.send(new ClientboundSetEquipmentPacket(armorStand.getId(), list));
        }
    }

    @Override
    public void lookEntity() {
        float yaw = getEntity().getLocation().getYaw();
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if(player == null) {
                players.remove(uuid);
                continue;
            }
            ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;
            connection.send(new ClientboundRotateHeadPacket(armorStand, (byte) (yaw * 256 / 360)));
            connection.send(new ClientboundMoveEntityPacket.Rot(armorStand.getId(), (byte) (yaw * 256 / 360), (byte)0, true));
        }
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
}