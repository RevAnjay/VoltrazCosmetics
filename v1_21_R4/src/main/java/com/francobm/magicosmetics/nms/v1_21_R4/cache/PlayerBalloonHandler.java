package com.francobm.magicosmetics.nms.v1_21_R4.cache;

import com.francobm.magicosmetics.cache.RotationType;
import com.francobm.magicosmetics.nms.balloon.PlayerBalloon;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Rotations;
import net.minecraft.network.protocol.game.*;
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
import org.bukkit.craftbukkit.v1_21_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R4.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_21_R4.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_21_R4.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_21_R4.util.CraftLocation;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class PlayerBalloonHandler extends PlayerBalloon {
    private final ArmorStand armorStand;
    private final net.minecraft.world.entity.LivingEntity leashed;
    private final double distance;
    private final double SQUARED_WALKING;
    private final double SQUARED_DISTANCE;

    public PlayerBalloonHandler(Player p, double space, double distance, boolean bigHead, boolean invisibleLeash) {
        viewers = new CopyOnWriteArrayList<>(new ArrayList<>());
        hideViewers = new CopyOnWriteArrayList<>(new ArrayList<>());
        this.uuid = p.getUniqueId();
        this.distance = distance;
        this.invisibleLeash = invisibleLeash;
        playerBalloons.put(uuid, this);
        Player player = getPlayer();
        ServerLevel world = ((CraftWorld)player.getWorld()).getHandle();

        Location location = player.getLocation().clone().add(0, space, 0);
        location = location.clone().add(player.getLocation().clone().getDirection().multiply(-1));
        armorStand = new ArmorStand(EntityType.ARMOR_STAND, world);
        armorStand.setPos(location.getX(), location.getY() - 1.3, location.getZ()); armorStand.forceSetRotation(location.getYaw(), location.getPitch());;
        armorStand.setInvisible(true);
        armorStand.setInvulnerable(true);
        armorStand.setMarker(true);
        this.bigHead = bigHead;
        if(isBigHead()){
            armorStand.setHeadPose(new Rotations(armorStand.getHeadPose().x(), 0, 0));
        }
        leashed = new Pufferfish(EntityType.PUFFERFISH, world);
        leashed.collides = false;
        leashed.setInvisible(true);
        leashed.setInvulnerable(true);
        leashed.setPos(location.getX(), location.getY(), location.getZ()); leashed.forceSetRotation(location.getYaw(), location.getPitch());;
        this.space = space;
        this.SQUARED_WALKING = 5.5 * space;
        this.SQUARED_DISTANCE = 10 * space;
    }

    @Override
    public void spawn(Player player) {
        if(hideViewers.contains(player.getUniqueId())) return;
        Player owner = getPlayer();
        if(owner == null) return;
        if(viewers.contains(player.getUniqueId())) {
            if(!owner.getWorld().equals(player.getWorld())) {
                remove(player);
                return;
            }
            if(owner.getLocation().distanceSquared(player.getLocation()) > distance) {
                remove(player);
            }
            return;
        }
        if(!owner.getWorld().equals(player.getWorld())) return;
        if(owner.getLocation().distanceSquared(player.getLocation()) > distance) return;

        ServerPlayer entityPlayer = ((CraftPlayer)player).getHandle();
        entityPlayer.connection.send(new ClientboundAddEntityPacket(armorStand, 0, CraftLocation.toBlockPosition(armorStand.getBukkitEntity().getLocation())));
        entityPlayer.connection.send(new ClientboundSetEntityDataPacket(armorStand.getId(), armorStand.getEntityData().getNonDefaultValues()));
        entityPlayer.connection.send(new ClientboundAddEntityPacket(leashed, 0, CraftLocation.toBlockPosition(leashed.getBukkitEntity().getLocation())));
        entityPlayer.connection.send(new ClientboundSetEntityDataPacket(leashed.getId(), leashed.getEntityData().getNonDefaultValues()));
        if(!invisibleLeash) {
            entityPlayer.connection.send(new ClientboundSetEntityLinkPacket(leashed, lendEntity == null ? ((CraftPlayer) owner).getHandle() : ((CraftLivingEntity)lendEntity).getHandle()));
        }
        viewers.add(player.getUniqueId());
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
        for(UUID uuid : viewers){
            Player player = Bukkit.getPlayer(uuid);
            if(player == null) {
                viewers.remove(uuid);
                continue;
            }
            remove(player);
        }
        playerBalloons.remove(uuid);
    }

    @Override
    public void remove(Player player) {
        ServerGamePacketListenerImpl connection = ((CraftPlayer)player).getHandle().connection;
        connection.send(new ClientboundRemoveEntitiesPacket(armorStand.getId()));
        connection.send(new ClientboundRemoveEntitiesPacket(leashed.getId()));
        connection.send(new ClientboundRemoveEntitiesPacket(leashed.getId()));
        viewers.remove(player.getUniqueId());
    }

    @Override
    public void setItem(ItemStack itemStack) {
        if(isBigHead()) {
            setItemBigHead(itemStack);
            return;
        }
        ArrayList<Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> list = new ArrayList<>();
        list.add(new Pair<>(EquipmentSlot.HEAD, CraftItemStack.asNMSCopy(itemStack)));
        for (UUID uuid : viewers) {
            Player player = Bukkit.getPlayer(uuid);
            if(player == null) {
                viewers.remove(uuid);
                continue;
            }
            ServerGamePacketListenerImpl connection = ((CraftPlayer)player).getHandle().connection;
            connection.send(new ClientboundSetEquipmentPacket(armorStand.getId(), list));
        }
    }

    public void setItemBigHead(ItemStack itemStack) {
        ArrayList<Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> list = new ArrayList<>();
        list.add(new Pair<>(EquipmentSlot.MAINHAND, CraftItemStack.asNMSCopy(itemStack)));
        for (UUID uuid : viewers) {
            Player player = Bukkit.getPlayer(uuid);
            if(player == null) {
                viewers.remove(uuid);
                continue;
            }
            ServerGamePacketListenerImpl connection = ((CraftPlayer)player).getHandle().connection;
            connection.send(new ClientboundSetEquipmentPacket(armorStand.getId(), list));
        }
    }

    @Override
    public void lookEntity(float yaw, float pitch) {
        for (UUID uuid : viewers) {
            Player player = Bukkit.getPlayer(uuid);
            if(player == null) {
                viewers.remove(uuid);
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
        leashed.setPos(location.getX(), location.getY(), location.getZ()); leashed.forceSetRotation(location.getYaw(), location.getPitch());;
        armorStand.setPos(location.getX(), location.getY() - 1.3, location.getZ()); armorStand.forceSetRotation(location.getYaw(), location.getPitch());;
    }

    protected void instantUpdate() {
        Player owner = getPlayer();
        if(owner == null) return;
        if(armorStand == null) return;
        if(leashed == null) return;
        if(!owner.getWorld().equals(leashed.getBukkitEntity().getWorld())) {
            spawn(false);
            return;
        }
        Location playerLoc = owner.getLocation().clone().add(0, space, 0);
        Location stand = leashed.getBukkitEntity().getLocation().clone();
        Vector standDir = owner.getEyeLocation().clone().subtract(stand).toVector();
        if (!standDir.equals(new Vector())) {
            standDir.normalize();
        }
        Location standToLoc = playerLoc.setDirection(standDir.setY(2));
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
        teleport(standToLoc);
        if (!rotateLoop) {
            rot += 0.02;
            armorStand.setBodyPose(new Rotations(armorStand.getBodyPose().x() - 0.5f, armorStand.getBodyPose().y(), armorStand.getBodyPose().z() + rotate));
            if (rot > 0.20) {
                rotateLoop = true;
            }
        } else {
            rot -= 0.02;
            armorStand.setBodyPose(new Rotations(armorStand.getBodyPose().x() + 0.5f, armorStand.getBodyPose().y(), armorStand.getBodyPose().z() + rotate));
            if (rot < -0.20) {
                rotateLoop = false;
            }
        }
        if (heightLoop) {
            height -= 0.01;
            armorStand.setBodyPose(new Rotations(armorStand.getBodyPose().x() + 0.8f, armorStand.getBodyPose().y(), armorStand.getBodyPose().z()));
            if (height < (-0.10 + 0)) heightLoop = false;
            return;
        }
        for(UUID uuid : viewers){
            Player player = Bukkit.getPlayer(uuid);
            if(player == null) {
                viewers.remove(uuid);
                continue;
            }
            ServerPlayer p = ((CraftPlayer)player).getHandle();
            if(!invisibleLeash) {
                p.connection.send(new ClientboundSetEntityLinkPacket(leashed, lendEntity == null ? ((CraftPlayer) owner).getHandle() : ((CraftLivingEntity)lendEntity).getHandle()));
            }
            p.connection.send(new ClientboundSetEntityDataPacket(armorStand.getId(), armorStand.getEntityData().getNonDefaultValues()));
            p.connection.send(new ClientboundTeleportEntityPacket(leashed.getId(), PositionMoveRotation.of(leashed), java.util.Set.of(), false));
            p.connection.send(new ClientboundTeleportEntityPacket(armorStand.getId(), PositionMoveRotation.of(armorStand), java.util.Set.of(), false));
        }
    }

    private final double CATCH_UP_INCREMENTS = .27;
    private double CATCH_UP_INCREMENTS_DISTANCE = CATCH_UP_INCREMENTS;
    @Override
    public void update(boolean instantFollow){
        if(isBigHead()) {
            updateBigHead();
            return;
        }
        if(instantFollow){
            instantUpdate();
            return;
        }
        Player owner = getPlayer();
        if(owner == null) return;
        if(armorStand == null) return;
        if(leashed == null) return;
        if(!owner.getWorld().equals(leashed.getBukkitEntity().getWorld())) {
            spawn(false);
            return;
        }
        Location playerLoc = owner.getLocation().clone().add(0, space, 0);
        Location stand = leashed.getBukkitEntity().getLocation().clone();
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
            Vector lineBetween = playerLoc.clone().subtract(stand).toVector();
            if (!standDir.equals(new Vector())) {
                standDir.normalize();
            }
            Vector distVec = lineBetween.clone().normalize().multiply(CATCH_UP_INCREMENTS_DISTANCE);
            double distY = distVec.getY();
            if(owner.isSneaking()){
                distY -= 0.13;
            }
            Location standToLoc = stand.clone().setDirection(standDir.setY(0)).add(0, distY, 0);
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
                armorStand.setBodyPose(new Rotations(armorStand.getBodyPose().x() - 0.5f, armorStand.getBodyPose().y(), armorStand.getBodyPose().z() + rotate));
                if (rot > 0.20) {
                    rotateLoop = true;
                }
            } else {
                rot -= 0.01;
                armorStand.setBodyPose(new Rotations(armorStand.getBodyPose().x() + 0.5f, armorStand.getBodyPose().y(), armorStand.getBodyPose().z() + rotate));
                if (rot < -0.20) {
                    rotateLoop = false;
                }
            }
            Location newLocation = standToLoc.clone();
            teleport(newLocation);
        }
        for(UUID uuid : viewers){
            Player player = Bukkit.getPlayer(uuid);
            if(player == null) {
                viewers.remove(uuid);
                continue;
            }
            ServerPlayer p = ((CraftPlayer)player).getHandle();
            if(!invisibleLeash) {
                p.connection.send(new ClientboundSetEntityLinkPacket(leashed, lendEntity == null ? ((CraftPlayer) owner).getHandle() : ((CraftLivingEntity)lendEntity).getHandle()));
            }
            p.connection.send(new ClientboundSetEntityDataPacket(armorStand.getId(), armorStand.getEntityData().getNonDefaultValues()));
            p.connection.send(new ClientboundTeleportEntityPacket(leashed.getId(), PositionMoveRotation.of(leashed), java.util.Set.of(), false));
            p.connection.send(new ClientboundTeleportEntityPacket(armorStand.getId(), PositionMoveRotation.of(armorStand), java.util.Set.of(), false));
        }

        if(distance1.distanceSquared(distance2) > SQUARED_WALKING){
            if(!heightLoop){
                height += 0.01;
                armorStand.setBodyPose(new Rotations(armorStand.getBodyPose().x() - 0.8f, armorStand.getBodyPose().y(), armorStand.getBodyPose().z()));
                if(height > 0.10) heightLoop = true;
            }
        }else{
            if (heightLoop) {
                height -= 0.01;
                armorStand.setBodyPose(new Rotations(armorStand.getBodyPose().x() + 0.8f, armorStand.getBodyPose().y(), armorStand.getBodyPose().z()));
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
        Player owner = getPlayer();
        if(owner == null) return;
        if(armorStand == null) return;
        if(leashed == null) return;
        if(!owner.getWorld().equals(leashed.getBukkitEntity().getWorld())) {
            spawn(false);
            return;
        }
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
                armorStand.setHeadPose(new Rotations(armorStand.getHeadPose().x() - 0.5f, armorStand.getHeadPose().y(), armorStand.getHeadPose().z() + rotate));
                if (rot > 0.20) {
                    rotateLoop = true;
                }
            } else {
                rot -= 0.01;
                armorStand.setHeadPose(new Rotations(armorStand.getHeadPose().x() + 0.5f, armorStand.getHeadPose().y(), armorStand.getHeadPose().z() + rotate));
                if (rot < -0.20) {
                    rotateLoop = false;
                }
            }
            Location newLocation = standToLoc.clone();
            teleport(newLocation);
        }
        for(UUID uuid : viewers){
            Player player = Bukkit.getPlayer(uuid);
            if(player == null) {
                viewers.remove(uuid);
                continue;
            }
            ServerPlayer p = ((CraftPlayer)player).getHandle();
            if(!invisibleLeash) {
                p.connection.send(new ClientboundSetEntityLinkPacket(leashed, lendEntity == null ? ((CraftPlayer) owner).getHandle() : ((CraftLivingEntity)lendEntity).getHandle()));
            }
            p.connection.send(new ClientboundSetEntityDataPacket(armorStand.getId(), armorStand.getEntityData().getNonDefaultValues()));
            p.connection.send(new ClientboundTeleportEntityPacket(leashed.getId(), PositionMoveRotation.of(leashed), java.util.Set.of(), false));
            p.connection.send(new ClientboundTeleportEntityPacket(armorStand.getId(), PositionMoveRotation.of(armorStand), java.util.Set.of(), false));
        }

        if(distance1.distanceSquared(distance2) > SQUARED_WALKING){
            if(!heightLoop){
                height += 0.01;
                armorStand.setHeadPose(new Rotations(armorStand.getHeadPose().x() - 0.8f, armorStand.getHeadPose().y(), armorStand.getHeadPose().z()));
                if(height > 0.10) heightLoop = true;
            }
        }else{
            if (heightLoop) {
                height -= 0.01;
                armorStand.setHeadPose(new Rotations(armorStand.getHeadPose().x() + 0.8f, armorStand.getHeadPose().y(), armorStand.getHeadPose().z()));
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
        switch (rotationType) {
            case RIGHT:
                armorStand.setBodyPose(new Rotations(armorStand.getBodyPose().x(), armorStand.getBodyPose().y() + rotate, armorStand.getBodyPose().z()));
                break;
            case UP:
                armorStand.setBodyPose(new Rotations(armorStand.getBodyPose().x() + rotate, armorStand.getBodyPose().y(), armorStand.getBodyPose().z()));
                break;
            case ALL:
                armorStand.setBodyPose(new Rotations(armorStand.getBodyPose().x() + rotate, armorStand.getBodyPose().y() + rotate, armorStand.getBodyPose().z()));
                break;
        }
        for(UUID uuid : viewers){
            Player player = Bukkit.getPlayer(uuid);
            if(player == null) {
                viewers.remove(uuid);
                continue;
            }
            ((CraftPlayer)player).getHandle().connection.send(new ClientboundSetEntityDataPacket(armorStand.getId(), armorStand.getEntityData().getNonDefaultValues()));
        }
    }

    public void rotateBigHead(boolean rotation, RotationType rotationType, float rotate) {
        if(!rotation) return;
        switch (rotationType){
            case RIGHT:
                armorStand.setHeadPose(new Rotations(armorStand.getHeadPose().x(), armorStand.getHeadPose().y() + rotate, armorStand.getHeadPose().z()));
                break;
            case UP:
                armorStand.setHeadPose(new Rotations(armorStand.getHeadPose().x() + rotate, armorStand.getHeadPose().y(), armorStand.getHeadPose().z()));
                break;
            case ALL:
                armorStand.setHeadPose(new Rotations(armorStand.getHeadPose().x() + rotate, armorStand.getHeadPose().y() + rotate, armorStand.getHeadPose().z()));
                break;
        }
        for(UUID uuid : viewers){
            Player player = Bukkit.getPlayer(uuid);
            if(player == null) {
                viewers.remove(uuid);
                continue;
            }
            ((CraftPlayer)player).getHandle().connection.send(new ClientboundSetEntityDataPacket(armorStand.getId(), armorStand.getEntityData().getNonDefaultValues()));
        }
    }

    public double getDistance() {
        return distance;
    }
}