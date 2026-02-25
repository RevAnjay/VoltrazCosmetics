package com.francobm.magicosmetics.nms.v1_21_R1.cache;

import com.francobm.magicosmetics.MagicCosmetics;
import com.francobm.magicosmetics.nms.spray.CustomSpray;
import io.netty.channel.ChannelPipeline;
import net.minecraft.core.Direction;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapView;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class CustomSprayHandler extends CustomSpray {
    private final ItemFrame itemFrame;
    private final Location location;
    private final net.minecraft.world.item.ItemStack itemStack;
    private final Direction enumDirection;
    private final MapView mapView;
    private final int rotation;

    public CustomSprayHandler(Player player, Location location, BlockFace blockFace, ItemStack itemStack, MapView mapView, int rotation) {
        players = new CopyOnWriteArrayList<>(new ArrayList<>());
        this.uuid = player.getUniqueId();
        customSprays.put(uuid, this);
        ServerLevel world = ((CraftWorld)player.getWorld()).getHandle();
        this.enumDirection = getDirection(blockFace);
        itemFrame = new ItemFrame(EntityType.ai, world);
        this.entity = (ItemFrame) itemFrame.getBukkitEntity();
        this.location = location;
        this.itemStack = CraftItemStack.asNMSCopy(itemStack);
        this.mapView = mapView;
        this.rotation = rotation;
        itemFrame.a(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    @Override
    public void spawn(Player player) {
        if(players.contains(player.getUniqueId())) {
            if(!player.getWorld().equals(location.getWorld())) {
                remove(player);
            }
            return;
        }
        if(!player.getWorld().equals(location.getWorld())) return;
        itemFrame.k(true); //Invisible
        itemFrame.n(true); //Invulnerable
        itemFrame.setItem(itemStack, true, false);

        itemFrame.a(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        itemFrame.a(enumDirection);
        itemFrame.b(rotation);
        sendPackets(player, spawnItemFrame());
        if(mapView != null) {
            player.sendMap(mapView);
        }
        players.add(player.getUniqueId());
    }

    @Override
    public void spawn(boolean exception) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if(exception && player.getUniqueId().equals(uuid)) continue;
            spawn(player);
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
        customSprays.remove(uuid);
    }

    @Override
    public void remove(Player player) {
        sendPackets(player, Collections.singletonList(destroyItemFrame()));
        players.remove(player.getUniqueId());
    }

    private Direction getDirection(BlockFace facing){
        switch (facing){
            case NORTH:
                return Direction.c;
            case SOUTH:
                return Direction.d;
            case WEST:
                return Direction.e;
            case EAST:
                return Direction.f;
            case DOWN:
                return Direction.a;
            default:
                return Direction.b;
        }
    }

    private List<Packet<?>> spawnItemFrame() {
        ClientboundAddEntityPacket spawnEntity = new ClientboundAddEntityPacket(itemFrame, enumDirection.d(), CraftLocation.toBlockPosition(location));
        ClientboundSetEntityDataPacket entityMetadata = new ClientboundSetEntityDataPacket(itemFrame.an(), itemFrame.ar().c());
        return Arrays.asList(spawnEntity, entityMetadata);
    }

    private Packet<?> destroyItemFrame() {
        return new ClientboundRemoveEntitiesPacket(itemFrame.an());
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
            String className = "com.denizenscript.denizen.nms.v1_20.impl.network.handlers.DenizenNetworkManagerImpl";
            String methodName = "getConnection";
            try {
                Class<?> clazz = Class.forName(className);
                Class<?>[] typeParameters = { ServerPlayer.class };
                Method method = clazz.getMethod(methodName, typeParameters);
                Object[] parameters = { playerConnection.f };
                Connection result = (Connection) method.invoke(null, parameters);
                return result.n.pipeline();
            } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                return null;
            }
        }
        try {
            Field privateConnection = ServerCommonPacketListenerImpl.class.getDeclaredField("e");
            privateConnection.setAccessible(true);
            Connection networkManager = (Connection) privateConnection.get(playerConnection);
            return networkManager.n.pipeline();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }
}
