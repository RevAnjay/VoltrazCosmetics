package com.francobm.magicosmetics.nms.v1_21_R4.models;

import com.francobm.magicosmetics.MagicCosmetics;
import com.francobm.magicosmetics.models.PacketReader;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_21_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PacketReaderHandler extends PacketReader{

    public void injectPlayer(Player player) {
        ServerPlayer entityPlayer = ((CraftPlayer) player).getHandle();
        MCChannelHandler cdh = new MCChannelHandler(entityPlayer);
        ChannelPipeline pipeline = getPrivateChannelPipeline(entityPlayer.connection);
        if(pipeline == null) return;
        for (String name : pipeline.toMap().keySet()) {
            if (pipeline.get(name) instanceof Connection) {
                pipeline.addBefore(name, "magic_cosmetics_packet_handler", cdh);
                break;
            }
        }
    }

    public void removePlayer(Player player) {
        CraftPlayer craftPlayer = (CraftPlayer) player;
        Channel channel = getPrivateChannel(craftPlayer.getHandle().connection);
        if(channel == null) return;
        channel.eventLoop().submit(() -> {
            channel.pipeline().remove("magic_cosmetics_packet_handler");
        });
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
                Object[] parameters = { playerConnection.player };
                Connection result = (Connection) method.invoke(null, parameters);
                return result.channel.pipeline();
            } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                     IllegalAccessException exception) {
                throw new RuntimeException(exception);
            }
        }
        try {
            Field privateConnection = ServerCommonPacketListenerImpl.class.getDeclaredField("connection");
            privateConnection.setAccessible(true);
            Connection networkManager = (Connection) privateConnection.get(playerConnection);
            return networkManager.channel.pipeline();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Bukkit.getLogger().severe("Error: Channel Pipeline not found");
            return null;
        }
    }

    private Channel getPrivateChannel(ServerGamePacketListenerImpl playerConnection) {
        MagicCosmetics plugin = MagicCosmetics.getInstance();
        if(plugin.getServer().getPluginManager().isPluginEnabled("Denizen")){
            String className = "com.denizenscript.denizen.nms.v1_20.impl.network.handlers.DenizenNetworkManagerImpl";
            String methodName = "getConnection";
            try {
                Class<?> clazz = Class.forName(className);
                Class<?>[] typeParameters = { ServerPlayer.class };
                Method method = clazz.getMethod(methodName, typeParameters);
                Object[] parameters = { playerConnection.player };
                Connection result = (Connection) method.invoke(null, parameters);
                return result.channel;
            } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                     IllegalAccessException exception) {
                throw new RuntimeException(exception);
            }
        }
        try {
            Field privateConnection = ServerCommonPacketListenerImpl.class.getDeclaredField("connection");
            privateConnection.setAccessible(true);
            Connection networkManager = (Connection) privateConnection.get(playerConnection);
            return networkManager.channel;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Bukkit.getLogger().severe("Error: Channel not found");
            return null;
        }
    }
}