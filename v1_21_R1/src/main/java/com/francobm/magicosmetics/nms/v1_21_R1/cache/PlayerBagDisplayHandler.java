package com.francobm.magicosmetics.nms.v1_21_R1.cache;

import com.francobm.magicosmetics.MagicCosmetics;
import com.francobm.magicosmetics.nms.IRangeManager;
import com.francobm.magicosmetics.nms.bag.PlayerBag;
import com.mojang.math.Transformation;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelPipeline;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemDisplayContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class PlayerBagDisplayHandler extends PlayerBag {
    private final Display.ItemDisplay backPack;
    private final double distance;
    private final ServerPlayer entityPlayer;

    public PlayerBagDisplayHandler(Player p, IRangeManager rangeManager, double distance, float height, ItemStack backPackItem, ItemStack backPackItemForMe){
        hideViewers = new CopyOnWriteArrayList<>(new ArrayList<>());
        this.uuid = p.getUniqueId();
        this.distance = distance;
        this.height = height;
        this.ids = new ArrayList<>();
        this.backPackItem = backPackItem;
        this.backPackItemForMe = backPackItemForMe;
        this.rangeManager = rangeManager;
        Player player = getPlayer();
        entityPlayer = ((CraftPlayer) player).getHandle();
        ServerLevel world = ((CraftWorld) player.getWorld()).getHandle();

        backPack = new Display.ItemDisplay(EntityType.ah, world);
        backpackId = backPack.an();
        backPack.a(ItemDisplayContext.f);
        backPack.w(10);
        backPack.x(10);
        backPack.a_(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ());
        backPack.b(0);
        backPack.c(0);
        backPack.ar().a(Display.s, 1);
    }

    @Override
    public void spawn(Player player) {
        if(hideViewers.contains(player.getUniqueId())) return;
        Player owner = getPlayer();
        if(owner == null) return;
        if(player.getUniqueId().equals(owner.getUniqueId())) {
            spawnSelf(owner);
            return;
        }
        Location location = owner.getLocation();
        backPack.a_(location.getX(), location.getY(), location.getZ());

        sendPackets(player, getBackPackSpawn(backPackItem, height));
    }

    @Override
    public void spawnSelf(Player player) {
        Player owner = getPlayer();
        if(owner == null) return;

        Location location = owner.getLocation();
        backPack.a_(location.getX(), location.getY(), location.getZ());

        sendPackets(player, getBackPackSpawn(backPackItemForMe == null ? backPackItem : backPackItemForMe, height));
    }

    @Override
    public void spawn(boolean exception) {
        for (Player player : getPlayersInRange()) {
            if(exception && player.getUniqueId().equals(uuid)) continue;
            spawn(player);
        }
    }

    @Override
    public void remove() {
        for (Player player : getPlayersInRange()) {
            remove(player);
        }
    }

    @Override
    public void remove(Player player) {
        if(player.getUniqueId().equals(uuid)) {
            sendPackets(player, getBackPackDismount());
            ids.clear();
            return;
        }
        sendPackets(player, getBackPackDismount());
    }

    @Override
    public void addPassenger(boolean exception) {
        List<Packet<?>> backPack = getBackPackMountPacket(lendEntityId == -1 ? getPlayer().getEntityId() : lendEntityId, this.backPack.an());
        for(Player player : getPlayersInRange()){
            if(exception && player.getUniqueId().equals(this.uuid)) continue;
            sendPackets(player, backPack);
        }
    }

    @Override
    public void addPassenger(Player player, int entity, int passenger) {
        sendPackets(player, getBackPackMountPacket(entity, passenger));
    }

    @Override
    public void setItemOnHelmet(Player player, ItemStack itemStack) {
        sendPackets(player, getBackPackHelmetPacket(itemStack));
    }

    private List<Packet<?>> getBackPackSpawn(ItemStack backpackItem, float height) {
        //backPack.a(new Transformation(new Vector3f(0, height, -0.3f), new Quaternionf(), new Vector3f(.6f, .6f, .6f), new Quaternionf()));
        backPack.a(new Transformation(new Vector3f(0, height, -0.3f), new Quaternionf(), new Vector3f(), new Quaternionf()));
        backPack.a(CraftItemStack.asNMSCopy(backpackItem));
        ClientboundAddEntityPacket spawnEntity = new ClientboundAddEntityPacket(backPack, 0, CraftLocation.toBlockPosition(entityPlayer.getBukkitEntity().getLocation()));
        ClientboundSetEntityDataPacket entityMetadata = new ClientboundSetEntityDataPacket(backPack.an(), backPack.ar().c());
        ClientboundSetPassengersPacket mountEntity = new ClientboundSetPassengersPacket(entityPlayer);
        return Arrays.asList(spawnEntity, entityMetadata, mountEntity);
    }

    private List<Packet<?>> getBackPackDismount() {
        ClientboundRemoveEntitiesPacket backPackDestroy = new ClientboundRemoveEntitiesPacket(backPack.an());
        return Collections.singletonList(backPackDestroy);
    }

    private List<Packet<?>> getBackPackMountPacket(int entity, int passenger) {
        ClientboundSetPassengersPacket packetPlayOutMount = this.createDataSerializer(packetDataSerializer -> {
            packetDataSerializer.c(entity);
            packetDataSerializer.a(new int[]{passenger});
            return ClientboundSetPassengersPacket.a.decode(packetDataSerializer);
        });
        return Collections.singletonList(packetPlayOutMount);
    }

    private List<Packet<?>> getBackPackHelmetPacket(ItemStack itemStack) {
        backPack.a(CraftItemStack.asNMSCopy(itemStack));
        return Collections.singletonList(new ClientboundSetEntityDataPacket(backPack.an(), backPack.ar().c()));
    }

    @Override
    public void lookEntity(float yaw, float pitch, boolean all) {
        Player owner = getPlayer();
        if(owner == null) return;
        if(all) {
            for (Player player : getPlayersInRange()) {
                sendPackets(player, getBackPackRotationPackets(yaw));
            }
            return;
        }
        sendPackets(owner, getBackPackRotationPackets(yaw));
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

    public double getDistance() {
        return distance;
    }

    @Override
    public Entity getEntity() {
        return backPack.getBukkitEntity();
    }

    private List<Packet<?>> getBackPackRotationPackets(float yaw) {
        Player owner = getPlayer();
        if(owner == null) return null;
        /*
        float rotationY = (float) Math.toRadians(yaw);
        backPack.a(new Transformation(new Vector3f(0,  owner.getUniqueId().equals(player.getUniqueId()) ? height : 0, -0.3f), new Quaternionf().rotateY(-rotationY), new Vector3f(.6f, .6f, .6f), new Quaternionf()));
        ClientboundSetEntityDataPacket entityMetadata = new ClientboundSetEntityDataPacket(backPack.an(), backPack.ar().c());
        return Collections.singletonList(entityMetadata);
        */
        //float rotationY = Location.normalizeYaw(entityPlayer.dF());
        double rotationY = yaw;
        PacketPlayOutEntityHeadRotation packetPlayOutEntityHeadRotation = new PacketPlayOutEntityHeadRotation(backPack, (byte) (rotationY * 256 / 360));
        PacketPlayOutEntity.PacketPlayOutEntityLook packetPlayOutEntityLook = new PacketPlayOutEntity.PacketPlayOutEntityLook(backPack.an(), (byte) (rotationY * 256 / 360), (byte)0, false);
        return Arrays.asList(packetPlayOutEntityLook, packetPlayOutEntityHeadRotation);
    }

    private void sendPackets(Player player, List<Packet<?>> packets) {
        final ChannelPipeline pipeline = getPrivateChannelPipeline(((CraftPlayer) player).getHandle().c);
        if(pipeline == null) return;
        for(Packet<?> packet : packets)
            pipeline.write(packet);
        pipeline.flush();
    }

    private ChannelPipeline getPrivateChannelPipeline(ServerGamePacketListenerImpl playerConnection) {
        MagicCosmetics plugin = MagicCosmetics.getInstance();
        if(plugin.getServer().getPluginManager().isPluginEnabled("Denizen")){
            String className = "com.denizenscript.denizen.nms.v1_21.impl.network.handlers.DenizenNetworkManagerImpl";
            String methodName = "getConnection";
            try {
                Class<?> clazz = Class.forName(className);
                Class<?>[] typeParameters = { ServerPlayer.class };
                Method method = clazz.getMethod(methodName, typeParameters);
                Object[] parameters = { playerConnection.f };
                Connection result = (Connection) method.invoke(null, parameters);
                return result.n.pipeline();
            } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                     IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            Field privateConnection = ServerCommonPacketListenerImpl.class.getDeclaredField("e");
            privateConnection.setAccessible(true);
            Connection networkManager = (Connection) privateConnection.get(playerConnection);
            return networkManager.n.pipeline();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Bukkit.getLogger().severe("Error: Channel Pipeline not found");
            return null;
        }
    }
}
