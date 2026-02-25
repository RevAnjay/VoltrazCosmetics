package com.francobm.magicosmetics.nms.v1_21_R1;

import com.francobm.magicosmetics.nms.NPC.ItemSlot;
import com.francobm.magicosmetics.nms.NPC.NPC;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.datafixers.util.Pair;
import io.netty.buffer.Unpooled;
import org.joml.Vector3f;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftArmorStand;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
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
        entityPunch.b(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        float yaw = location.getYaw() * 256.0F / 360.0F;
        ClientboundAddEntityPacket packetPlayOutSpawnEntity = new ClientboundAddEntityPacket(entityPunch.an(), entityPunch.cz(), location.getX(), location.getY(), location.getZ(), location.getPitch(), location.getYaw(), entityPunch.am(), 0, entityPunch.dr(), entityPunch.ct());
        entityPlayer.c.b(packetPlayOutSpawnEntity);
        entityPlayer.c.b(new PacketPlayOutEntityHeadRotation(entityPunch, (byte) yaw));
        entityPlayer.c.b(new ClientboundSetEntityDataPacket(entityPunch.an(), entityPunch.ar().c()));
        entityPlayer.c.b(new PacketPlayOutCamera(entityPunch));
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
        ServerPlayer npc = new EntityPlayer(server, world, gameProfile, ClientInformation.a());

        ArmorStand armorStand = new ArmorStand(EntityType.d, world);
        armorStand.k(true); //Invisible
        armorStand.n(true); //Invulnerable
        armorStand.b(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), player.getLocation().getYaw(), 0);
        npc.b(location.getX(), location.getY(), location.getZ(), location.getYaw(), 0);
        //balloon
        balloon = new ArmorStand(EntityType.d, world);
        balloon.n(true); //invulnerable true
        balloon.k(true); //Invisible true
        ArmorStand entityPunch = new ArmorStand(EntityType.d, world);
        entityPunch.n(true);
        entityPunch.k(true);
        entityPunch.b(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), player.getLocation().getYaw(), player.getLocation().getPitch());
        leashed = new Pufferfish(EntityType.aF, world);
        ((Pufferfish)leashed).b(npc, true);
        leashed.n(true);
        leashed.k(true);
        leashed.d(true); //silent true
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
        ServerGamePacketListenerImpl connection = ((CraftPlayer)player).getHandle().c;
        connection.b(new ClientboundRemoveEntitiesPacket(armorStand.getEntityId(), entity.getEntityId(), punch.getEntityId(), balloon.an(), leashed.an()));
    }

    @Override
    public void removeBalloon(Player player) {
        ServerGamePacketListenerImpl connection = ((CraftPlayer)player).getHandle().c;
        connection.b(new ClientboundRemoveEntitiesPacket(balloon.an(), leashed.an()));
    }

    @Override
    public void spawnNPC(Player player) {
        Location npcLocation = entity.getLocation();
        Location armorStandLocation = armorStand.getLocation();
        ServerPlayer entityPlayer = ((CraftPlayer)player).getHandle();
        ServerPlayer npc = ((CraftPlayer)this.entity).getHandle();
        ArmorStand armorStand = ((CraftArmorStand)this.armorStand).getHandle();
        armorStand.n(true); //invulnerable true
        armorStand.k(true); //Invisible true
        npc.c = entityPlayer.c;
        ClientboundAddEntityPacket npcSpawnPacket = new ClientboundAddEntityPacket(npc.an(), npc.cz(), npcLocation.getX(), npcLocation.getY(), npcLocation.getZ(), npcLocation.getPitch(), npcLocation.getYaw(), npc.am(), 0, npc.dr(), npc.ct());
        ClientboundAddEntityPacket armorStandSpawnPacket = new ClientboundAddEntityPacket(armorStand.an(), armorStand.cz(), armorStandLocation.getX(), armorStandLocation.getY(), armorStandLocation.getZ(), armorStandLocation.getPitch(), armorStandLocation.getYaw(), armorStand.am(), 0, armorStand.dr(), armorStand.ct());
        entityPlayer.c.b(new ClientboundPlayerInfoUpdatePacket(Enum.valueOf(ClientboundPlayerInfoUpdatePacket.a.class, "ADD_PLAYER"), npc));
        entityPlayer.c.b(npcSpawnPacket);
        entityPlayer.c.b(new PacketPlayOutEntityHeadRotation(npc, (byte) (player.getLocation().getYaw() * 256 / 360)));
        entityPlayer.c.b(armorStandSpawnPacket);
        //client settings
        entityPlayer.c.b(new ClientboundSetEntityDataPacket(armorStand.an(), armorStand.ar().c()));
        //
        SynchedEntityData watcher = npc.ar();
        byte bitmask = ((CraftPlayer)player).getHandle().ar().a(new EntityDataAccessor<>(17, EntityDataSerializers.a));
        watcher.a(new EntityDataAccessor<>(17, EntityDataSerializers.a), bitmask);
        entityPlayer.c.b(new ClientboundSetEntityDataPacket(npc.an(), watcher.c()));
        new BukkitRunnable() {
            @Override
            public void run() {
                entityPlayer.c.b(new ClientboundPlayerInfoRemovePacket(Collections.singletonList(npc.getBukkitEntity().getUniqueId())));
            }
        }.runTaskLater(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("MagicCosmetics")), 20L);
        addPassenger(player);
    }

    @Override
    public void lookNPC(Player player, float yaw) {
        ServerPlayer entityPlayer = ((CraftPlayer)this.entity).getHandle();
        ArmorStand armorStand = ((CraftArmorStand)this.armorStand).getHandle();
        armorStand.n(true); //invulnerable true
        armorStand.k(true); //Invisible true
        ServerGamePacketListenerImpl connection = ((CraftPlayer)player).getHandle().c;
        connection.b(new PacketPlayOutEntityHeadRotation(armorStand, (byte)(yaw * 256 / 360)));
        connection.b(new PacketPlayOutEntity.PacketPlayOutEntityLook(armorStand.an(), (byte)(yaw * 256 / 360), (byte)0, true));

        connection.b(new PacketPlayOutEntityHeadRotation(entityPlayer, (byte)(yaw * 256 / 360)));
        connection.b(new PacketPlayOutEntity.PacketPlayOutEntityLook(entityPlayer.an(), (byte)(yaw * 256 / 360), (byte)0, true));
        //connection.b(new PacketPlayOutEntityTeleport(entityPlayer));
    }

    @Override
    public void armorStandSetItem(Player player, ItemStack itemStack) {
        ArmorStand entityPlayer = ((CraftArmorStand)this.armorStand).getHandle();
        ServerGamePacketListenerImpl connection = ((CraftPlayer)player).getHandle().c;
        ArrayList<Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> list = new ArrayList<>();
        list.add(new Pair<>(EquipmentSlot.f, CraftItemStack.asNMSCopy(itemStack)));
        connection.b(new PacketPlayOutEntityEquipment(entityPlayer.an(), list));
    }

    @Override
    public void balloonSetItem(Player player, ItemStack itemStack) {
        ServerGamePacketListenerImpl connection = ((CraftPlayer)player).getHandle().c;
        ArrayList<Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> list = new ArrayList<>();
        if(isBigHead()){
            list.add(new Pair<>(EquipmentSlot.a, CraftItemStack.asNMSCopy(itemStack)));
        }else {
            list.add(new Pair<>(EquipmentSlot.f, CraftItemStack.asNMSCopy(itemStack)));
        }
        connection.b(new PacketPlayOutEntityEquipment(balloon.an(), list));
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
        balloon.b(balloonPos.getX(), balloonPos.getY(), balloonPos.getZ(), balloonPos.getYaw(), balloonPos.getPitch());

        leashed.b(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        this.bigHead = bigHead;
        if(isBigHead()){
            balloon.d(new Vector3f(balloon.C().b(), 0, 0));
        }
        ClientboundAddEntityPacket balloonSpawnPacket = new ClientboundAddEntityPacket(balloon.an(), balloon.cz(), balloonPos.getX(), balloonPos.getY(), balloonPos.getZ(), balloonPos.getPitch(), balloonPos.getYaw(), balloon.am(), 0, balloon.dr(), balloon.ct());
        ClientboundAddEntityPacket leashedSpawnPacket = new ClientboundAddEntityPacket(leashed.an(), leashed.cz(), balloonPosition.getX(), balloonPosition.getY(), balloonPosition.getZ(), balloonPosition.getPitch(), balloonPosition.getYaw(), leashed.am(), 0, leashed.dr(), leashed.ct());
        realPlayer.c.b(balloonSpawnPacket);
        realPlayer.c.b(leashedSpawnPacket);
        realPlayer.c.b(new ClientboundSetEntityDataPacket(balloon.an(), balloon.ar().c()));
        realPlayer.c.b(new ClientboundSetEntityDataPacket(leashed.an(), leashed.ar().c()));
        realPlayer.c.b(new PacketPlayOutAttachEntity(leashed, entityPlayer));
        balloonSetItem(player, itemStack);
    }

    @Override
    public void equipNPC(Player player, ItemSlot itemSlot, ItemStack itemStack) {
        ServerPlayer entityPlayer = ((CraftPlayer)this.entity).getHandle();
        ServerGamePacketListenerImpl connection = ((CraftPlayer)player).getHandle().c;
        ArrayList<Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> list = new ArrayList<>();
        switch (itemSlot){
            case MAIN_HAND:
                list.add(new Pair<>(EquipmentSlot.a, CraftItemStack.asNMSCopy(itemStack)));
                connection.b(new PacketPlayOutEntityEquipment(entityPlayer.an(), list));
                break;
            case OFF_HAND:
                list.add(new Pair<>(EquipmentSlot.b, CraftItemStack.asNMSCopy(itemStack)));
                connection.b(new PacketPlayOutEntityEquipment(entityPlayer.an(), list));
                break;
            case BOOTS:
                list.add(new Pair<>(EquipmentSlot.c, CraftItemStack.asNMSCopy(itemStack)));
                connection.b(new PacketPlayOutEntityEquipment(entityPlayer.an(), list));
                break;
            case LEGGINGS:
                list.add(new Pair<>(EquipmentSlot.d, CraftItemStack.asNMSCopy(itemStack)));
                connection.b(new PacketPlayOutEntityEquipment(entityPlayer.an(), list));
                break;
            case CHESTPLATE:
                list.add(new Pair<>(EquipmentSlot.e, CraftItemStack.asNMSCopy(itemStack)));
                connection.b(new PacketPlayOutEntityEquipment(entityPlayer.an(), list));
                break;
            case HELMET:
                list.add(new Pair<>(EquipmentSlot.f, CraftItemStack.asNMSCopy(itemStack)));
                connection.b(new PacketPlayOutEntityEquipment(entityPlayer.an(), list));
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
            packetDataSerializer.c(entityPlayer.an());
            packetDataSerializer.a(new int[]{armorStand.an()});
            return ClientboundSetPassengersPacket.a.decode(packetDataSerializer);
        });
        p.c.b(packetPlayOutMount);
    }

    public void addPassenger(Player player, net.minecraft.world.entity.Entity entity1, net.minecraft.world.entity.Entity entity2) {
        if(entity1 == null) return;
        if(entity2 == null) return;
        ServerPlayer p = ((CraftPlayer)player).getHandle();
        ClientboundSetPassengersPacket packetPlayOutMount = this.createDataSerializer(packetDataSerializer -> {
            packetDataSerializer.c(entity1.an());
            packetDataSerializer.a(new int[]{entity2.an()});
            return ClientboundSetPassengersPacket.a.decode(packetDataSerializer);
        });
        p.c.b(packetPlayOutMount);
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
            //standToLoc.setYaw(standToLoc.getYaw() - 3F);
            if (y > 0.10) {
                floatLoop = true;
            }
        } else {
            y -= 0.01;
            balloonPosition.subtract(0, 0.01, 0);
            //standToLoc.setYaw(standToLoc.getYaw() + 3F);
            if (y < (-0.11 + 0)) {
                floatLoop = false;
                rotate *= -1;
            }
        }
        if (!rotateLoop) {
            rot += 0.01;
            balloon.a(new Vector3f(balloon.B().b() - 0.5f, balloon.B().c(), balloon.B().d() + rotate));
            //armorStand.setHeadPose(armorStand.getHeadPose().add(0, 0, rotate).subtract(0.008, 0, 0));
            if (rot > 0.20) {
                rotateLoop = true;
            }
        } else {
            rot -= 0.01;
            balloon.a(new Vector3f(balloon.B().b() + 0.5f, balloon.B().c(), balloon.B().d() + rotate));
            //armorStand.setHeadPose(armorStand.getHeadPose().add(0.008, 0, rotate));//.subtract(0.006, 0, 0));
            if (rot < -0.20) {
                rotateLoop = false;
            }
        }
        leashed.a(balloonPosition.getX(), balloonPosition.getY(), balloonPosition.getZ(), balloonPosition.getYaw(), balloonPosition.getPitch());
        balloon.a(balloonPosition.getX(), balloonPosition.getY() - 1.3, balloonPosition.getZ(), balloonPosition.getYaw(), balloonPosition.getPitch());
        p.c.b(new ClientboundSetEntityDataPacket(balloon.an(), balloon.ar().c()));
        p.c.b(new PacketPlayOutEntityTeleport(leashed));
        p.c.b(new PacketPlayOutEntityTeleport(balloon));
    }

    public void animationBigHead(Player player){
        ServerPlayer p = ((CraftPlayer)player).getHandle();
        //
        if(balloonPosition == null) return;
        if (!floatLoop) {
            y += 0.01;
            balloonPosition.add(0, 0.01, 0);
            //standToLoc.setYaw(standToLoc.getYaw() - 3F);
            if (y > 0.10) {
                floatLoop = true;
            }
        } else {
            y -= 0.01;
            balloonPosition.subtract(0, 0.01, 0);
            //standToLoc.setYaw(standToLoc.getYaw() + 3F);
            if (y < (-0.11 + 0)) {
                floatLoop = false;
                rotate *= -1;
            }
        }
        if (!rotateLoop) {
            rot += 0.01;
            balloon.d(new Vector3f(balloon.C().b() - 0.5f, balloon.C().c(), balloon.C().d() + rotate));
            //armorStand.setHeadPose(armorStand.getHeadPose().add(0, 0, rotate).subtract(0.008, 0, 0));
            if (rot > 0.20) {
                rotateLoop = true;
            }
        } else {
            rot -= 0.01;
            balloon.d(new Vector3f(balloon.C().b() + 0.5f, balloon.C().c(), balloon.C().d() + rotate));
            //armorStand.setHeadPose(armorStand.getHeadPose().add(0.008, 0, rotate));//.subtract(0.006, 0, 0));
            if (rot < -0.20) {
                rotateLoop = false;
            }
        }
        leashed.a(balloonPosition.getX(), balloonPosition.getY(), balloonPosition.getZ(), balloonPosition.getYaw(), balloonPosition.getPitch());
        balloon.a(balloonPosition.getX(), balloonPosition.getY() - 1.3, balloonPosition.getZ(), balloonPosition.getYaw(), balloonPosition.getPitch());
        p.c.b(new ClientboundSetEntityDataPacket(balloon.an(), balloon.ar().c()));
        p.c.b(new PacketPlayOutEntityTeleport(leashed));
        p.c.b(new PacketPlayOutEntityTeleport(balloon));
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
