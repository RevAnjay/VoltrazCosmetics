package xyz.voltraz.cosmetics.nms.v1_21_R2.models;

import xyz.voltraz.cosmetics.VoltrazCosmetics;
import xyz.voltraz.cosmetics.api.CosmeticType;
import xyz.voltraz.cosmetics.cache.EntityIdCache;
import xyz.voltraz.cosmetics.cache.PlayerData;
import xyz.voltraz.cosmetics.cache.cosmetics.Hat;
import xyz.voltraz.cosmetics.cache.cosmetics.WStick;
import xyz.voltraz.cosmetics.cache.cosmetics.backpacks.Bag;
import xyz.voltraz.cosmetics.events.CosmeticInventoryUpdateEvent;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.LevelEntityGetter;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_21_R2.inventory.CraftItemStack;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Netty channel handler for intercepting packets.
 * Stores only the player UUID (not ServerPlayer) to prevent memory leaks
 * if the handler is not properly removed from the pipeline on disconnect.
 */
public class MCChannelHandler extends ChannelDuplexHandler {

    private static Method entityGetter;

    static {
        for(Method method : ServerLevel.class.getMethods()) {
            if(LevelEntityGetter.class.isAssignableFrom(method.getReturnType()) && method.getReturnType() != LevelEntityGetter.class) {
                entityGetter = method;
                break;
            }
        }
    }

    private final UUID playerUUID;

    public MCChannelHandler(ServerPlayer player){
        this.playerUUID = player.getUUID();
    }

    private Player getBukkitPlayer() {
        return Bukkit.getPlayer(playerUUID);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if(msg instanceof ClientboundContainerSetSlotPacket) {
            ClientboundContainerSetSlotPacket packetPlayOutSetSlot = (ClientboundContainerSetSlotPacket) msg;
            if(packetPlayOutSetSlot.getContainerId() == 0)
                CallUpdateInvEvent(packetPlayOutSetSlot.getSlot(), packetPlayOutSetSlot.getItem());
        }else if(msg instanceof ClientboundBundlePacket) {
            ClientboundBundlePacket packet = (ClientboundBundlePacket) msg;
            for(Packet<?> subPacket : packet.subPackets()){
                if(subPacket instanceof ClientboundAddEntityPacket) {
                    ClientboundAddEntityPacket otherPacket = (ClientboundAddEntityPacket) subPacket;
                    handleEntitySpawn(otherPacket.getId());
                }else if(subPacket instanceof ClientboundRemoveEntitiesPacket) {
                    ClientboundRemoveEntitiesPacket otherPacket = (ClientboundRemoveEntitiesPacket) subPacket;
                    for(int id : otherPacket.getEntityIds()){
                        handleEntityDespawn(id);
                    }
                }
            }
        }else if(msg instanceof ClientboundAddEntityPacket) {
            ClientboundAddEntityPacket otherPacket = (ClientboundAddEntityPacket) msg;
            handleEntitySpawn(otherPacket.getId());
        }else if(msg instanceof ClientboundRemoveEntitiesPacket) {
            ClientboundRemoveEntitiesPacket otherPacket = (ClientboundRemoveEntitiesPacket) msg;
            for(int id : otherPacket.getEntityIds()){
                handleEntityDespawn(id);
            }
        }else if(msg instanceof ClientboundSetPassengersPacket) {
            ClientboundSetPassengersPacket otherPacket = (ClientboundSetPassengersPacket) msg;
            msg = handleEntityMount(otherPacket);
        }
        super.write(ctx, msg, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof ServerboundSwingPacket){
            if(checkInZone()){
                openMenu();
            }
        }
        super.channelRead(ctx, msg);
    }

    private boolean checkInZone(){
        Player player = getBukkitPlayer();
        if(player == null) return false;
        PlayerData playerData = getPlayerDataIfPresent(player);
        if(playerData == null) return false;
        return playerData.isZone();
    }

    private void openMenu() {
        VoltrazCosmetics plugin = VoltrazCosmetics.getInstance();
        Player player = getBukkitPlayer();
        if(player == null) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getCosmeticsManager().openMenu(player, plugin.getMainMenu()));
    }

    private void CallUpdateInvEvent(int slot, ItemStack itemStack) {
        Player player = getBukkitPlayer();
        if(player == null) return;
        VoltrazCosmetics plugin = VoltrazCosmetics.getInstance();
        CosmeticInventoryUpdateEvent event;
        if(slot == 5){
            PlayerData playerData = getPlayerDataIfPresent(player);
            if(playerData == null) return;
            Hat hat = playerData.getHat();
            if(hat == null) return;
            event = new CosmeticInventoryUpdateEvent(player, CosmeticType.HAT, hat, CraftItemStack.asBukkitCopy(itemStack));
        }else if(slot == 45){
            PlayerData playerData = getPlayerDataIfPresent(player);
            if(playerData == null) return;
            WStick wStick = playerData.getWStick();
            if(wStick == null) return;
            event = new CosmeticInventoryUpdateEvent(player, CosmeticType.WALKING_STICK, wStick, CraftItemStack.asBukkitCopy(itemStack));
        }else {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getServer().getPluginManager().callEvent(event));
    }

    private ClientboundSetPassengersPacket handleEntityMount(ClientboundSetPassengersPacket packetPlayOutMount) {
        int id = packetPlayOutMount.getVehicle();
        int[] ids = packetPlayOutMount.getPassengers();
        org.bukkit.entity.Entity entity = this.getEntityAsync(id);
        if(!(entity instanceof Player)) return packetPlayOutMount;
        Player otherPlayer = (Player) entity;
        PlayerData playerData = getPlayerDataIfPresent(otherPlayer);
        if(playerData == null || playerData.getBag() == null) return packetPlayOutMount;

        Bag bag = (Bag) playerData.getBag();
        if(bag.getBackpackId() == -1) return packetPlayOutMount;
        boolean alreadyPresent = false;
        for(int pid : ids) {
            if(pid == bag.getBackpackId()) { alreadyPresent = true; break; }
        }
        int[] newIds;
        if(alreadyPresent) {
            newIds = new int[ids.length];
            newIds[0] = bag.getBackpackId();
            int writeIdx = 1;
            for(int pid : ids) {
                if(pid == bag.getBackpackId()) continue;
                newIds[writeIdx++] = pid;
            }
        } else {
            newIds = new int[ids.length + 1];
            newIds[0] = bag.getBackpackId();
            System.arraycopy(ids, 0, newIds, 1, ids.length);
        }
        FriendlyByteBuf data = new FriendlyByteBuf(Unpooled.buffer());
        try {
            data.writeVarInt(id);
            data.writeVarIntArray(newIds);
            return ClientboundSetPassengersPacket.STREAM_CODEC.decode(data);
        } finally {
            data.release();
        }
    }

    private void handleEntitySpawn(int id) {
        org.bukkit.entity.Entity entity = this.getEntityAsync(id);
        if(!(entity instanceof Player)) return;
        Player otherPlayer = (Player) entity;
        PlayerData playerData = getPlayerDataIfPresent(otherPlayer);
        if(playerData == null || playerData.getBag() == null) return;

        Player viewer = getBukkitPlayer();
        if(viewer == null) return;
        Bukkit.getServer().getScheduler().runTask(VoltrazCosmetics.getInstance(), () -> {
            if(!otherPlayer.isOnline()) return;
            Player currentViewer = getBukkitPlayer();
            if(currentViewer == null) return;
            playerData.getBag().spawn(currentViewer);
        });
    }

    private void handleEntityDespawn(int id) {
        org.bukkit.entity.Entity entity = this.getEntityAsync(id);
        if(!(entity instanceof Player)) return;
        Player otherPlayer = (Player) entity;
        PlayerData playerData = getPlayerDataIfPresent(otherPlayer);
        if(playerData == null || playerData.getBag() == null) return;

        Player viewer = getBukkitPlayer();
        if(viewer == null) return;
        Bukkit.getServer().getScheduler().runTask(VoltrazCosmetics.getInstance(), () -> {
            if(!otherPlayer.isOnline()) return;
            Player currentViewer = getBukkitPlayer();
            if(currentViewer == null) return;
            playerData.getBag().despawn(currentViewer);
        });
    }

    protected org.bukkit.entity.Entity getEntityAsync(int id) {
        return EntityIdCache.getPlayer(id);
    }

    public static LevelEntityGetter<Entity> getEntityGetter(ServerLevel level) {
        if(entityGetter == null)
            return level.getEntities();
        try {
            @SuppressWarnings("unchecked")
            LevelEntityGetter<net.minecraft.world.entity.Entity> result = (LevelEntityGetter<net.minecraft.world.entity.Entity>) entityGetter.invoke(level);
            return result;
        }catch (Throwable ignored) {
            return null;
        }
    }

    private static final java.lang.reflect.Method PLAYER_DATA_METHOD;
    private static final java.lang.reflect.Field PLAYER_DATA_FIELD;

    static {
        java.lang.reflect.Method m = null;
        java.lang.reflect.Field f = null;
        try {
            m = PlayerData.class.getMethod("getPlayerIfPresent", org.bukkit.OfflinePlayer.class);
        } catch (Exception ignored) {
            try {
                f = PlayerData.class.getDeclaredField("players");
                f.setAccessible(true);
            } catch (Exception ignored2) {}
        }
        PLAYER_DATA_METHOD = m;
        PLAYER_DATA_FIELD = f;
    }

    private PlayerData getPlayerDataIfPresent(Player p) {
        if (p == null) return null;
        if (PLAYER_DATA_METHOD != null) {
            try {
                return (PlayerData) PLAYER_DATA_METHOD.invoke(null, p);
            } catch (Exception ignored) {}
        }
        if (PLAYER_DATA_FIELD != null) {
            try {
                @SuppressWarnings("unchecked")
                java.util.Map<java.util.UUID, PlayerData> map = (java.util.Map<java.util.UUID, PlayerData>) PLAYER_DATA_FIELD.get(null);
                return map.get(p.getUniqueId());
            } catch (Exception ignored) {}
        }
        return null;
    }
}
