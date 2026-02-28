package com.francobm.magicosmetics.nms.v1_21_R1.cache;

import com.francobm.magicosmetics.cache.RotationType;
import com.francobm.magicosmetics.nms.balloon.EntityBalloon;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Rotations;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.decoration.ArmorStand;
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
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class EntityBalloonHandler extends EntityBalloon {
    private final ArmorStand armorStand;
    private final net.minecraft.world.entity.LivingEntity leashed;
    private final double distance;
    private final double SQUARED_WALKING;
    private final double SQUARED_DISTANCE;

    public EntityBalloonHandler(Entity entity, double space, double distance, boolean bigHead, boolean invisibleLeash) {
        players = new CopyOnWriteArrayList<>(new ArrayList<>());
        this.uuid = entity.getUniqueId();
        this.distance = distance;
        this.invisibleLeash = invisibleLeash;
        entitiesBalloon.put(uuid, this);
        this.entity = (org.bukkit.entity.LivingEntity) entity;
        ServerLevel world = ((CraftWorld)entity.getWorld()).getHandle();

        Location location = entity.getLocation().clone().add(0, space, 0);
        location = location.clone().add(entity.getLocation().clone().getDirection().multiply(-1));
        armorStand = new ArmorStand(EntityType.ARMOR_STAND, world);
        armorStand.absMoveTo(location.getX(), location.getY() - 1.3, location.getZ(), location.getYaw(), location.getPitch());
        armorStand.setInvisible(true);
        armorStand.setInvulnerable(true);
        armorStand.setMarker(true);
        this.bigHead = bigHead;
        if(isBigHead()){
            armorStand.setHeadPose(new Rotations(armorStand.getHeadPose().getX(), 0, 0));
        }
        leashed = new Pufferfish(EntityType.PUFFERFISH, world);
        leashed.collides = false;
        leashed.setInvisible(true);
        leashed.setInvulnerable(true);
        leashed.absMoveTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        this.space = space;
        this.SQUARED_WALKING = 5.5 * space;
        this.SQUARED_DISTANCE = 10 * space;
    }

    @Override
    public void spawn(Player player) {
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

        ServerPlayer entityPlayer = ((CraftPlayer)player).getHandle();
        entityPlayer.connection.send(new ClientboundAddEntityPacket(armorStand, 0, CraftLocation.toBlockPosition(armorStand.getBukkitEntity().getLocation())));
        entityPlayer.connection.send(new ClientboundSetEntityDataPacket(armorStand.getId(), armorStand.getEntityData().getNonDefaultValues()));

        entityPlayer.connection.send(new ClientboundAddEntityPacket(leashed, 0, CraftLocation.toBlockPosition(leashed.getBukkitEntity().getLocation())));
        entityPlayer.connection.send(new ClientboundSetEntityDataPacket(leashed.getId(), leashed.getEntityData().getNonDefaultValues()));
        if(!invisibleLeash) {
            entityPlayer.connection.send(new ClientboundSetEntityLinkPacket(leashed, ((CraftEntity) getEntity()).getHandle()));
        }
        //client settings
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
        entitiesBalloon.remove(uuid);
    }

    @Override
    public void remove(Player player) {
        ServerGamePacketListenerImpl connection = ((CraftPlayer)player).getHandle().connection;
        connection.send(new ClientboundRemoveEntitiesPacket(armorStand.getId(), leashed.getId()));
        players.remove(player.getUniqueId());
    }

    @Override
    public void setItem(ItemStack itemStack) {
        if(isBigHead()){
            setItemBigHead(itemStack);
            return;
        }
        for (UUID uuid : players) {
            ArrayList<Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> list = new ArrayList<>();
            list.add(new Pair<>(EquipmentSlot.HEAD, CraftItemStack.asNMSCopy(itemStack)));
            Player player = Bukkit.getPlayer(uuid);
            if(player == null) {
                players.remove(uuid);
                continue;
            }
            ServerGamePacketListenerImpl connection = ((CraftPlayer)player).getHandle().connection;
            connection.send(new ClientboundSetEquipmentPacket(armorStand.getId(), list));
        }
    }

    public void setItemBigHead(ItemStack itemStack) {
        ArrayList<Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> list = new ArrayList<>();
        list.add(new Pair<>(EquipmentSlot.MAINHAND, CraftItemStack.asNMSCopy(itemStack)));
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if(player == null) {
                players.remove(uuid);
                continue;
            }
            ServerGamePacketListenerImpl connection = ((CraftPlayer)player).getHandle().connection;
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
            connection.send(new ClientboundRotateHeadPacket(leashed, (byte) (yaw * 256 / 360)));
            connection.send(new ClientboundMoveEntityPacket.Rot(leashed.getId(), (byte) (yaw * 256 / 360), (byte)0, true));
        }
    }

    protected void teleport(Location location) {
        leashed.absMoveTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        armorStand.absMoveTo(location.getX(), location.getY() - 1.3, location.getZ(), location.getYaw(), location.getPitch());
    }

    private final double CATCH_UP_INCREMENTS = .27; //.25
    private double CATCH_UP_INCREMENTS_DISTANCE = CATCH_UP_INCREMENTS;
    @Override
    public void update(){
        if(isBigHead()){
            updateBigHead();
            return;
        }
        org.bukkit.entity.LivingEntity owner = getEntity();
        if(armorStand == null) return;
        if(leashed == null) return;
        Location playerLoc = owner.getLocation().clone().add(0, space, 0);
        Location stand = leashed.getBukkitEntity().getLocation();
        Vector standDir = owner.getEyeLocation().clone().subtract(stand).toVector();
        Location distance2 = stand.clone();
        Location distance1 = owner.getLocation().clone();

        if(distance1.distanceSquared(distance2) > SQUARED_WALKING){
            Vector lineBetween = playerLoc.clone().subtract(stand).toVector();
            if (!standDir.equals(new Vector())) {
                standDir.normalize();
            }
            Vector distVec = lineBetween.clone().normalize().multiply(CATCH_UP_INCREMENTS_DISTANCE);
            Location standTo = stand.clone().setDirection(standDir.setY(0)).add(distVec.clone());
            Location newLocation = standTo.clone();
            teleport(newLocation);
        }else {
            if (!standDir.equals(new Vector())) {
                standDir.normalize();
            }
            Location standToLoc = stand.clone().setDirection(standDir.setY(0));
            if (!floatLoop) {
                y += 0.01;
                standToLoc.add(0, 0.01, 0);
                if (y > 0.10) {
                    floatLoop = true;
                }
            } else {
                y -= 0.01;
                standToLoc.subtract(0, 0.01, 0);
                if (y < (-0.11 + 0)) {
                    floatLoop = false;
                    rotate *= -1;
                }
            }

            if (!rotateLoop) {
                rot += 0.01;
                armorStand.setBodyPose(new Rotations(armorStand.getBodyPose().getX() - 0.5f, armorStand.getBodyPose().getY(), armorStand.getBodyPose().getZ() + rotate));
                if (rot > 0.20) {
                    rotateLoop = true;
                }
            } else {
                rot -= 0.01;
                armorStand.setBodyPose(new Rotations(armorStand.getBodyPose().getX() + 0.5f, armorStand.getBodyPose().getY(), armorStand.getBodyPose().getZ() + rotate));
                if (rot < -0.20) {
                    rotateLoop = false;
                }
            }
            Location newLocation = standToLoc.clone();
            teleport(newLocation);
        }
        for(UUID uuid : players){
            Player player = Bukkit.getPlayer(uuid);
            if(player == null) {
                players.remove(uuid);
                continue;
            }
            ServerPlayer p = ((CraftPlayer)player).getHandle();
            if(!invisibleLeash) {
                p.connection.send(new ClientboundSetEntityLinkPacket(leashed, ((CraftEntity) getEntity()).getHandle()));
            }
            p.connection.send(new ClientboundSetEntityDataPacket(armorStand.getId(), armorStand.getEntityData().getNonDefaultValues()));
            p.connection.send(new ClientboundTeleportEntityPacket(leashed));
            p.connection.send(new ClientboundTeleportEntityPacket(armorStand));
        }

        if(distance1.distanceSquared(distance2) > SQUARED_WALKING){
            if(!heightLoop){
                height += 0.01;
                armorStand.setBodyPose(new Rotations(armorStand.getBodyPose().getX() - 0.8f, armorStand.getBodyPose().getY(), armorStand.getBodyPose().getZ()));
                if(height > 0.10) heightLoop = true;
            }
        }else{
            if (heightLoop) {
                height -= 0.01;
                armorStand.setBodyPose(new Rotations(armorStand.getBodyPose().getX() + 0.8f, armorStand.getBodyPose().getY(), armorStand.getBodyPose().getZ()));
                if (height < (-0.10 + 0)) heightLoop = false;
                return;
            }

        }
        if(distance1.distanceSquared(distance2) > SQUARED_DISTANCE){
            CATCH_UP_INCREMENTS_DISTANCE += 0.01;
        }else{
            CATCH_UP_INCREMENTS_DISTANCE = CATCH_UP_INCREMENTS;
        }
    }

    public void updateBigHead(){
        org.bukkit.entity.LivingEntity owner = getEntity();
        if(armorStand == null) return;
        if(leashed == null) return;
        Location playerLoc = owner.getLocation().clone().add(0, space, 0);
        Location stand = leashed.getBukkitEntity().getLocation();
        Vector standDir = owner.getEyeLocation().clone().subtract(stand).toVector();
        Location distance2 = stand.clone();
        Location distance1 = owner.getLocation().clone();

        if(distance1.distanceSquared(distance2) > SQUARED_WALKING){
            Vector lineBetween = playerLoc.clone().subtract(stand).toVector();
            if (!standDir.equals(new Vector())) {
                standDir.normalize();
            }
            Vector distVec = lineBetween.clone().normalize().multiply(CATCH_UP_INCREMENTS_DISTANCE);
            Location standTo = stand.clone().setDirection(standDir.setY(0)).add(distVec.clone());
            Location newLocation = standTo.clone();
            leashed.absMoveTo(newLocation.getX(), newLocation.getY(), newLocation.getZ(), newLocation.getYaw(), newLocation.getPitch());
            armorStand.absMoveTo(newLocation.getX(), newLocation.getY() - 1.3, newLocation.getZ(), newLocation.getYaw(), newLocation.getPitch());
        }else {
            if (!standDir.equals(new Vector())) {
                standDir.normalize();
            }
            Location standToLoc = stand.clone().setDirection(standDir.setY(0));
            if (!floatLoop) {
                y += 0.01;
                standToLoc.add(0, 0.01, 0);
                if (y > 0.10) {
                    floatLoop = true;
                }
            } else {
                y -= 0.01;
                standToLoc.subtract(0, 0.01, 0);
                if (y < (-0.11 + 0)) {
                    floatLoop = false;
                    rotate *= -1;
                }
            }

            if (!rotateLoop) {
                rot += 0.01;
                armorStand.setHeadPose(new Rotations(armorStand.getHeadPose().getX() - 0.5f, armorStand.getHeadPose().getY(), armorStand.getHeadPose().getZ() + rotate));
                if (rot > 0.20) {
                    rotateLoop = true;
                }
            } else {
                rot -= 0.01;
                armorStand.setHeadPose(new Rotations(armorStand.getHeadPose().getX() + 0.5f, armorStand.getHeadPose().getY(), armorStand.getHeadPose().getZ() + rotate));
                if (rot < -0.20) {
                    rotateLoop = false;
                }
            }
            Location newLocation = standToLoc.clone();
            leashed.absMoveTo(newLocation.getX(), newLocation.getY(), newLocation.getZ(), newLocation.getYaw(), newLocation.getPitch());
            armorStand.absMoveTo(newLocation.getX(), newLocation.getY() - 1.3, newLocation.getZ(), newLocation.getYaw(), newLocation.getPitch());
        }
        for(UUID uuid : players){
            Player player = Bukkit.getPlayer(uuid);
            if(player == null) {
                players.remove(uuid);
                continue;
            }
            ServerPlayer p = ((CraftPlayer)player).getHandle();
            if(!invisibleLeash) {
                p.connection.send(new ClientboundSetEntityLinkPacket(leashed, ((CraftEntity) getEntity()).getHandle()));
            }
            p.connection.send(new ClientboundSetEntityDataPacket(armorStand.getId(), armorStand.getEntityData().getNonDefaultValues()));
            p.connection.send(new ClientboundTeleportEntityPacket(leashed));
            p.connection.send(new ClientboundTeleportEntityPacket(armorStand));
        }

        if(distance1.distanceSquared(distance2) > SQUARED_WALKING){
            if(!heightLoop){
                height += 0.01;
                armorStand.setHeadPose(new Rotations(armorStand.getHeadPose().getX() - 0.8f, armorStand.getHeadPose().getY(), armorStand.getHeadPose().getZ()));
                if(height > 0.10) heightLoop = true;
            }
        }else{
            if (heightLoop) {
                height -= 0.01;
                armorStand.setHeadPose(new Rotations(armorStand.getHeadPose().getX() + 0.8f, armorStand.getHeadPose().getY(), armorStand.getHeadPose().getZ()));
                if (height < (-0.10 + 0)) heightLoop = false;
                return;
            }

        }
        if(distance1.distanceSquared(distance2) > SQUARED_DISTANCE){
            CATCH_UP_INCREMENTS_DISTANCE += 0.01;
        }else{
            CATCH_UP_INCREMENTS_DISTANCE = CATCH_UP_INCREMENTS;
        }
    }

    @Override
    public void rotate(boolean rotation, RotationType rotationType, float rotate) {
        if(isBigHead()){
            rotateBigHead(rotation, rotationType, rotate);
            return;
        }
        if(!rotation) return;
        switch (rotationType){
            case RIGHT:
                armorStand.setBodyPose(new Rotations(armorStand.getBodyPose().getX(), armorStand.getBodyPose().getY() + rotate, armorStand.getBodyPose().getZ()));
                break;
            case UP:
                armorStand.setBodyPose(new Rotations(armorStand.getBodyPose().getX() + rotate, armorStand.getBodyPose().getY(), armorStand.getBodyPose().getZ()));
                break;
            case ALL:
                armorStand.setBodyPose(new Rotations(armorStand.getBodyPose().getX() + rotate, armorStand.getBodyPose().getY() + rotate, armorStand.getBodyPose().getZ()));
                break;
        }
        for(UUID uuid : players){
            Player player = Bukkit.getPlayer(uuid);
            if(player == null) {
                players.remove(uuid);
                continue;
            }
            ((CraftPlayer)player).getHandle().connection.send(new ClientboundSetEntityDataPacket(armorStand.getId(), armorStand.getEntityData().getNonDefaultValues()));
        }
    }

    public void rotateBigHead(boolean rotation, RotationType rotationType, float rotate) {
        if(!rotation) return;
        switch (rotationType){
            case RIGHT:
                armorStand.setHeadPose(new Rotations(armorStand.getHeadPose().getX(), armorStand.getHeadPose().getY() + rotate, armorStand.getHeadPose().getZ()));
                break;
            case UP:
                armorStand.setHeadPose(new Rotations(armorStand.getHeadPose().getX() + rotate, armorStand.getHeadPose().getY(), armorStand.getHeadPose().getZ()));
                break;
            case ALL:
                armorStand.setHeadPose(new Rotations(armorStand.getHeadPose().getX() + rotate, armorStand.getHeadPose().getY() + rotate, armorStand.getHeadPose().getZ()));
                break;
        }
        for(UUID uuid : players){
            Player player = Bukkit.getPlayer(uuid);
            if(player == null) {
                players.remove(uuid);
                continue;
            }
            ((CraftPlayer)player).getHandle().connection.send(new ClientboundSetEntityDataPacket(armorStand.getId(), armorStand.getEntityData().getNonDefaultValues()));
        }
    }

    public double getDistance() {
        return distance;
    }
}
