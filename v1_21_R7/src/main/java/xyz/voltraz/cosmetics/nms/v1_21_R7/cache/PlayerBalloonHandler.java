package xyz.voltraz.cosmetics.nms.v1_21_R7.cache;

import xyz.voltraz.cosmetics.nms.v1_21_R7.ReflectionUtils;

import xyz.voltraz.cosmetics.cache.RotationType;
import xyz.voltraz.cosmetics.nms.balloon.PlayerBalloon;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Rotations;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.fish.Pufferfish;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class PlayerBalloonHandler extends PlayerBalloon {
    private ArmorStand armorStand;
    private net.minecraft.world.entity.LivingEntity leashed;
    private final double distance;
    private final double SQUARED_WALKING;
    private final double SQUARED_DISTANCE;
    private final int armorStandId;
    private final int leashedId;

    public PlayerBalloonHandler(Player p, double space, double distance, boolean bigHead, boolean invisibleLeash) {
        viewers = new CopyOnWriteArrayList<>(new ArrayList<>());
        hideViewers = new CopyOnWriteArrayList<>(new ArrayList<>());
        this.uuid = p.getUniqueId();
        this.distance = distance;
        this.invisibleLeash = invisibleLeash;
        playerBalloons.put(uuid, this);
        Player player = getPlayer();
        ServerLevel world = ReflectionUtils.getHandle(player.getWorld());

        Location location = player.getLocation().clone().add(0, space, 0);
        location = location.clone().add(player.getLocation().clone().getDirection().multiply(-1));
        armorStand = new ArmorStand(EntityType.ARMOR_STAND, world);
        ReflectionUtils.absMoveTo(armorStand, location.getX(), location.getY() - 1.3, location.getZ(), location.getYaw(), location.getPitch());
        armorStand.setInvisible(true);
        armorStand.setInvulnerable(true);
        armorStand.setMarker(true);
        this.bigHead = bigHead;
        if(isBigHead()){
            armorStand.setHeadPose(new Rotations(ReflectionUtils.rotX(armorStand.getHeadPose()), 0, 0));
        }
        leashed = new Pufferfish(EntityType.PUFFERFISH, world);
        leashed.collides = false;
        leashed.setInvisible(true);
        leashed.setInvulnerable(true);
        ReflectionUtils.absMoveTo(leashed, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        this.space = space;
        this.SQUARED_WALKING = 5.5 * space;
        this.SQUARED_DISTANCE = 10 * space;
        this.armorStandId = armorStand.getId();
        this.leashedId = leashed.getId();
    }

    @Override
    public void spawn(Player player) {
        if(armorStand == null || leashed == null) return;
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

        ServerPlayer entityPlayer = ReflectionUtils.getHandle(player);
        entityPlayer.connection.send(new ClientboundAddEntityPacket(armorStand, 0, ReflectionUtils.toBlockPosition(armorStand.getBukkitEntity().getLocation())));
        entityPlayer.connection.send(new ClientboundSetEntityDataPacket(armorStand.getId(), armorStand.getEntityData().getNonDefaultValues()));
        entityPlayer.connection.send(new ClientboundAddEntityPacket(leashed, 0, ReflectionUtils.toBlockPosition(leashed.getBukkitEntity().getLocation())));
        entityPlayer.connection.send(new ClientboundSetEntityDataPacket(leashed.getId(), leashed.getEntityData().getNonDefaultValues()));
        if(!invisibleLeash) {
            entityPlayer.connection.send(new ClientboundSetEntityLinkPacket(leashed, lendEntity == null ? ReflectionUtils.getHandle(owner) : ReflectionUtils.getHandle((Entity)lendEntity)));
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
        armorStand = null;
        leashed = null;
        tempPlayerLoc.setWorld(null);
        tempStandLoc.setWorld(null);
        tempDistLoc1.setWorld(null);
        tempTeleportLoc.setWorld(null);
        lendEntity = null;
    }

    @Override
    public void remove(Player player) {
        ServerGamePacketListenerImpl connection = ReflectionUtils.getHandle(player).connection;
        connection.send(new ClientboundRemoveEntitiesPacket(armorStandId, leashedId));
        viewers.remove(player.getUniqueId());
    }

    @Override
    public void setItem(ItemStack itemStack) {
        if(armorStand == null) return;
        if(isBigHead()) {
            setItemBigHead(itemStack);
            return;
        }
        ArrayList<Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> list = new ArrayList<>();
        list.add(new Pair<>(EquipmentSlot.HEAD, ReflectionUtils.asNMSCopy(itemStack)));
        for (UUID uuid : viewers) {
            Player player = Bukkit.getPlayer(uuid);
            if(player == null) {
                viewers.remove(uuid);
                continue;
            }
            ServerGamePacketListenerImpl connection = ReflectionUtils.getHandle(player).connection;
            connection.send(new ClientboundSetEquipmentPacket(armorStandId, list));
        }
    }

    public void setItemBigHead(ItemStack itemStack) {
        if(armorStand == null) return;
        ArrayList<Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> list = new ArrayList<>();
        list.add(new Pair<>(EquipmentSlot.MAINHAND, ReflectionUtils.asNMSCopy(itemStack)));
        for (UUID uuid : viewers) {
            Player player = Bukkit.getPlayer(uuid);
            if(player == null) {
                viewers.remove(uuid);
                continue;
            }
            ServerGamePacketListenerImpl connection = ReflectionUtils.getHandle(player).connection;
            connection.send(new ClientboundSetEquipmentPacket(armorStandId, list));
        }
    }

    @Override
    public void lookEntity(float yaw, float pitch) {
        if(armorStand == null || leashed == null) return;
        for (UUID uuid : viewers) {
            Player player = Bukkit.getPlayer(uuid);
            if(player == null) {
                viewers.remove(uuid);
                continue;
            }
            ServerGamePacketListenerImpl connection = ReflectionUtils.getHandle(player).connection;
            connection.send(new ClientboundRotateHeadPacket(armorStand, (byte) (yaw * 256 / 360)));
            connection.send(new ClientboundMoveEntityPacket.Rot(armorStandId, (byte) (yaw * 256 / 360), (byte)0, true));
            connection.send(new ClientboundRotateHeadPacket(leashed, (byte) (yaw * 256 / 360)));
            connection.send(new ClientboundMoveEntityPacket.Rot(leashedId, (byte) (yaw * 256 / 360), (byte)0, true));
        }
    }

    protected void teleport(Location location) {
        if(leashed == null || armorStand == null) return;
        ReflectionUtils.absMoveTo(leashed, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        ReflectionUtils.absMoveTo(armorStand, location.getX(), location.getY() - 1.3, location.getZ(), location.getYaw(), location.getPitch());
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
        Location ownerLoc = owner.getLocation();
        tempPlayerLoc.setWorld(ownerLoc.getWorld());
        tempPlayerLoc.setX(ownerLoc.getX());
        tempPlayerLoc.setY(ownerLoc.getY() + space);
        tempPlayerLoc.setZ(ownerLoc.getZ());
        Location standBukkit = leashed.getBukkitEntity().getLocation();
        tempStandLoc.setWorld(standBukkit.getWorld());
        tempStandLoc.setX(standBukkit.getX());
        tempStandLoc.setY(standBukkit.getY());
        tempStandLoc.setZ(standBukkit.getZ());
        Location eyeLoc = owner.getEyeLocation();
        tempStandDir.setX(eyeLoc.getX() - tempStandLoc.getX());
        tempStandDir.setY(eyeLoc.getY() - tempStandLoc.getY());
        tempStandDir.setZ(eyeLoc.getZ() - tempStandLoc.getZ());
        if (!tempStandDir.equals(ZERO_VEC)) {
            tempStandDir.normalize();
        }
        tempStandDir.setY(2);
        tempPlayerLoc.setDirection(tempStandDir);
        if (!floatLoop) {
            y += 0.01;
            tempPlayerLoc.setY(tempPlayerLoc.getY() + 0.01);
            if (y > 0.10) {
                floatLoop = true;
            }
        } else {
            y -= 0.01;
            tempPlayerLoc.setY(tempPlayerLoc.getY() - 0.01);
            if (y < (-0.11 + 0)) {
                floatLoop = false;
                rotate *= -1;
            }
        }
        teleport(tempPlayerLoc);
        if (!rotateLoop) {
            rot += 0.02;
            armorStand.setBodyPose(new Rotations(ReflectionUtils.rotX(armorStand.getBodyPose()) - 0.5f, ReflectionUtils.rotY(armorStand.getBodyPose()), ReflectionUtils.rotZ(armorStand.getBodyPose()) + rotate));
            if (rot > 0.20) {
                rotateLoop = true;
            }
        } else {
            rot -= 0.02;
            armorStand.setBodyPose(new Rotations(ReflectionUtils.rotX(armorStand.getBodyPose()) + 0.5f, ReflectionUtils.rotY(armorStand.getBodyPose()), ReflectionUtils.rotZ(armorStand.getBodyPose()) + rotate));
            if (rot < -0.20) {
                rotateLoop = false;
            }
        }
        if (heightLoop) {
            height -= 0.01;
            armorStand.setBodyPose(new Rotations(ReflectionUtils.rotX(armorStand.getBodyPose()) + 0.8f, ReflectionUtils.rotY(armorStand.getBodyPose()), ReflectionUtils.rotZ(armorStand.getBodyPose())));
            if (height < (-0.10 + 0)) heightLoop = false;
            return;
        }
        boolean sendLeash = lendEntityDirty && !invisibleLeash;
        if (sendLeash) lendEntityDirty = false;
        for(UUID uuid : viewers){
            Player player = Bukkit.getPlayer(uuid);
            if(player == null) {
                viewers.remove(uuid);
                continue;
            }
            ServerPlayer p = ReflectionUtils.getHandle(player);
            if(sendLeash) {
                p.connection.send(new ClientboundSetEntityLinkPacket(leashed, lendEntity == null ? ReflectionUtils.getHandle(owner) : ReflectionUtils.getHandle((Entity)lendEntity)));
            }
            p.connection.send(new ClientboundSetEntityDataPacket(armorStand.getId(), armorStand.getEntityData().getNonDefaultValues()));
            p.connection.send(ReflectionUtils.createTeleportPacket(leashed));
            p.connection.send(ReflectionUtils.createTeleportPacket(armorStand));
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
        Location ownerLoc = owner.getLocation();
        tempPlayerLoc.setWorld(ownerLoc.getWorld());
        tempPlayerLoc.setX(ownerLoc.getX());
        tempPlayerLoc.setY(ownerLoc.getY() + space);
        tempPlayerLoc.setZ(ownerLoc.getZ());
        Location standBukkit = leashed.getBukkitEntity().getLocation();
        tempStandLoc.setWorld(standBukkit.getWorld());
        tempStandLoc.setX(standBukkit.getX());
        tempStandLoc.setY(standBukkit.getY());
        tempStandLoc.setZ(standBukkit.getZ());
        Location eyeLoc = owner.getEyeLocation();
        tempStandDir.setX(eyeLoc.getX() - tempStandLoc.getX());
        tempStandDir.setY(eyeLoc.getY() - tempStandLoc.getY());
        tempStandDir.setZ(eyeLoc.getZ() - tempStandLoc.getZ());
        tempDistLoc1.setWorld(ownerLoc.getWorld());
        tempDistLoc1.setX(ownerLoc.getX());
        tempDistLoc1.setY(ownerLoc.getY());
        tempDistLoc1.setZ(ownerLoc.getZ());
        double distSq = tempDistLoc1.distanceSquared(tempStandLoc);

        if(distSq > SQUARED_WALKING){
            tempLineBetween.setX(tempPlayerLoc.getX() - tempStandLoc.getX());
            tempLineBetween.setY(tempPlayerLoc.getY() - tempStandLoc.getY());
            tempLineBetween.setZ(tempPlayerLoc.getZ() - tempStandLoc.getZ());
            if (!tempStandDir.equals(ZERO_VEC)) {
                tempStandDir.normalize();
            }
            tempDistVec.setX(tempLineBetween.getX());
            tempDistVec.setY(tempLineBetween.getY());
            tempDistVec.setZ(tempLineBetween.getZ());
            tempDistVec.normalize().multiply(CATCH_UP_INCREMENTS_DISTANCE);
            tempStandDir.setY(0);
            tempTeleportLoc.setWorld(tempStandLoc.getWorld());
            tempTeleportLoc.setX(tempStandLoc.getX() + tempDistVec.getX());
            tempTeleportLoc.setY(tempStandLoc.getY() + tempDistVec.getY());
            tempTeleportLoc.setZ(tempStandLoc.getZ() + tempDistVec.getZ());
            tempTeleportLoc.setDirection(tempStandDir);
            teleport(tempTeleportLoc);
        }else {
            tempLineBetween.setX(tempPlayerLoc.getX() - tempStandLoc.getX());
            tempLineBetween.setY(tempPlayerLoc.getY() - tempStandLoc.getY());
            tempLineBetween.setZ(tempPlayerLoc.getZ() - tempStandLoc.getZ());
            if (!tempStandDir.equals(ZERO_VEC)) {
                tempStandDir.normalize();
            }
            tempDistVec.setX(tempLineBetween.getX());
            tempDistVec.setY(tempLineBetween.getY());
            tempDistVec.setZ(tempLineBetween.getZ());
            tempDistVec.normalize().multiply(CATCH_UP_INCREMENTS_DISTANCE);
            double distY = tempDistVec.getY();
            if(owner.isSneaking()){
                distY -= 0.13;
            }
            tempStandDir.setY(0);
            tempTeleportLoc.setWorld(tempStandLoc.getWorld());
            tempTeleportLoc.setX(tempStandLoc.getX());
            tempTeleportLoc.setY(tempStandLoc.getY() + distY);
            tempTeleportLoc.setZ(tempStandLoc.getZ());
            tempTeleportLoc.setDirection(tempStandDir);
            if (!floatLoop) {
                y += 0.01;
                tempTeleportLoc.setY(tempTeleportLoc.getY() + 0.01);
                if (y > 0.10) {
                    floatLoop = true;
                }
            } else {
                y -= 0.01;
                tempTeleportLoc.setY(tempTeleportLoc.getY() - 0.01);
                if (y < (-0.11 + 0)) {
                    floatLoop = false;
                    rotate *= -1;
                }
            }

            if (!rotateLoop) {
                rot += 0.01;
                armorStand.setBodyPose(new Rotations(ReflectionUtils.rotX(armorStand.getBodyPose()) - 0.5f, ReflectionUtils.rotY(armorStand.getBodyPose()), ReflectionUtils.rotZ(armorStand.getBodyPose()) + rotate));
                if (rot > 0.20) {
                    rotateLoop = true;
                }
            } else {
                rot -= 0.01;
                armorStand.setBodyPose(new Rotations(ReflectionUtils.rotX(armorStand.getBodyPose()) + 0.5f, ReflectionUtils.rotY(armorStand.getBodyPose()), ReflectionUtils.rotZ(armorStand.getBodyPose()) + rotate));
                if (rot < -0.20) {
                    rotateLoop = false;
                }
            }
            teleport(tempTeleportLoc);
        }
        boolean sendLeash = lendEntityDirty && !invisibleLeash;
        if (sendLeash) lendEntityDirty = false;
        for(UUID uuid : viewers){
            Player player = Bukkit.getPlayer(uuid);
            if(player == null) {
                viewers.remove(uuid);
                continue;
            }
            ServerPlayer p = ReflectionUtils.getHandle(player);
            if(sendLeash) {
                p.connection.send(new ClientboundSetEntityLinkPacket(leashed, lendEntity == null ? ReflectionUtils.getHandle(owner) : ReflectionUtils.getHandle((Entity)lendEntity)));
            }
            p.connection.send(new ClientboundSetEntityDataPacket(armorStand.getId(), armorStand.getEntityData().getNonDefaultValues()));
            p.connection.send(ReflectionUtils.createTeleportPacket(leashed));
            p.connection.send(ReflectionUtils.createTeleportPacket(armorStand));
        }

        if(distSq > SQUARED_WALKING){
            if(!heightLoop){
                height += 0.01;
                armorStand.setBodyPose(new Rotations(ReflectionUtils.rotX(armorStand.getBodyPose()) - 0.8f, ReflectionUtils.rotY(armorStand.getBodyPose()), ReflectionUtils.rotZ(armorStand.getBodyPose())));
                if(height > 0.10) heightLoop = true;
            }
        }else{
            if (heightLoop) {
                height -= 0.01;
                armorStand.setBodyPose(new Rotations(ReflectionUtils.rotX(armorStand.getBodyPose()) + 0.8f, ReflectionUtils.rotY(armorStand.getBodyPose()), ReflectionUtils.rotZ(armorStand.getBodyPose())));
                if (height < (-0.10 + 0)) heightLoop = false;
                return;
            }

        }
        if(distSq > SQUARED_DISTANCE){
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
        Location ownerLoc = owner.getLocation();
        tempPlayerLoc.setWorld(ownerLoc.getWorld());
        tempPlayerLoc.setX(ownerLoc.getX());
        tempPlayerLoc.setY(ownerLoc.getY() + space);
        tempPlayerLoc.setZ(ownerLoc.getZ());
        Location standBukkit = leashed.getBukkitEntity().getLocation();
        tempStandLoc.setWorld(standBukkit.getWorld());
        tempStandLoc.setX(standBukkit.getX());
        tempStandLoc.setY(standBukkit.getY());
        tempStandLoc.setZ(standBukkit.getZ());
        Location eyeLoc = owner.getEyeLocation();
        tempStandDir.setX(eyeLoc.getX() - tempStandLoc.getX());
        tempStandDir.setY(eyeLoc.getY() - tempStandLoc.getY());
        tempStandDir.setZ(eyeLoc.getZ() - tempStandLoc.getZ());
        tempDistLoc1.setWorld(ownerLoc.getWorld());
        tempDistLoc1.setX(ownerLoc.getX());
        tempDistLoc1.setY(ownerLoc.getY());
        tempDistLoc1.setZ(ownerLoc.getZ());
        double distSq = tempDistLoc1.distanceSquared(tempStandLoc);

        if(distSq > SQUARED_WALKING){
            tempLineBetween.setX(tempPlayerLoc.getX() - tempStandLoc.getX());
            tempLineBetween.setY(tempPlayerLoc.getY() - tempStandLoc.getY());
            tempLineBetween.setZ(tempPlayerLoc.getZ() - tempStandLoc.getZ());
            if (!tempStandDir.equals(ZERO_VEC)) {
                tempStandDir.normalize();
            }
            tempDistVec.setX(tempLineBetween.getX());
            tempDistVec.setY(tempLineBetween.getY());
            tempDistVec.setZ(tempLineBetween.getZ());
            tempDistVec.normalize().multiply(CATCH_UP_INCREMENTS_DISTANCE);
            tempStandDir.setY(0);
            tempTeleportLoc.setWorld(tempStandLoc.getWorld());
            tempTeleportLoc.setX(tempStandLoc.getX() + tempDistVec.getX());
            tempTeleportLoc.setY(tempStandLoc.getY() + tempDistVec.getY());
            tempTeleportLoc.setZ(tempStandLoc.getZ() + tempDistVec.getZ());
            tempTeleportLoc.setDirection(tempStandDir);
            teleport(tempTeleportLoc);
        }else {
            if (!tempStandDir.equals(ZERO_VEC)) {
                tempStandDir.normalize();
            }
            tempStandDir.setY(0);
            tempTeleportLoc.setWorld(tempStandLoc.getWorld());
            tempTeleportLoc.setX(tempStandLoc.getX());
            tempTeleportLoc.setY(tempStandLoc.getY());
            tempTeleportLoc.setZ(tempStandLoc.getZ());
            tempTeleportLoc.setDirection(tempStandDir);
            if (!floatLoop) {
                y += 0.01;
                tempTeleportLoc.setY(tempTeleportLoc.getY() + 0.01);
                if (y > 0.10) {
                    floatLoop = true;
                }
            } else {
                y -= 0.01;
                tempTeleportLoc.setY(tempTeleportLoc.getY() - 0.01);
                if (y < (-0.11 + 0)) {
                    floatLoop = false;
                    rotate *= -1;
                }
            }

            if (!rotateLoop) {
                rot += 0.01;
                armorStand.setHeadPose(new Rotations(ReflectionUtils.rotX(armorStand.getHeadPose()) - 0.5f, ReflectionUtils.rotY(armorStand.getHeadPose()), ReflectionUtils.rotZ(armorStand.getHeadPose()) + rotate));
                if (rot > 0.20) {
                    rotateLoop = true;
                }
            } else {
                rot -= 0.01;
                armorStand.setHeadPose(new Rotations(ReflectionUtils.rotX(armorStand.getHeadPose()) + 0.5f, ReflectionUtils.rotY(armorStand.getHeadPose()), ReflectionUtils.rotZ(armorStand.getHeadPose()) + rotate));
                if (rot < -0.20) {
                    rotateLoop = false;
                }
            }
            teleport(tempTeleportLoc);
        }
        boolean sendLeash = lendEntityDirty && !invisibleLeash;
        if (sendLeash) lendEntityDirty = false;
        for(UUID uuid : viewers){
            Player player = Bukkit.getPlayer(uuid);
            if(player == null) {
                viewers.remove(uuid);
                continue;
            }
            ServerPlayer p = ReflectionUtils.getHandle(player);
            if(sendLeash) {
                p.connection.send(new ClientboundSetEntityLinkPacket(leashed, lendEntity == null ? ReflectionUtils.getHandle(owner) : ReflectionUtils.getHandle((Entity)lendEntity)));
            }
            p.connection.send(new ClientboundSetEntityDataPacket(armorStand.getId(), armorStand.getEntityData().getNonDefaultValues()));
            p.connection.send(ReflectionUtils.createTeleportPacket(leashed));
            p.connection.send(ReflectionUtils.createTeleportPacket(armorStand));
        }

        if(distSq > SQUARED_WALKING){
            if(!heightLoop){
                height += 0.01;
                armorStand.setHeadPose(new Rotations(ReflectionUtils.rotX(armorStand.getHeadPose()) - 0.8f, ReflectionUtils.rotY(armorStand.getHeadPose()), ReflectionUtils.rotZ(armorStand.getHeadPose())));
                if(height > 0.10) heightLoop = true;
            }
        }else{
            if (heightLoop) {
                height -= 0.01;
                armorStand.setHeadPose(new Rotations(ReflectionUtils.rotX(armorStand.getHeadPose()) + 0.8f, ReflectionUtils.rotY(armorStand.getHeadPose()), ReflectionUtils.rotZ(armorStand.getHeadPose())));
                if (height < (-0.10 + 0)) heightLoop = false;
                return;
            }

        }
        if(distSq > SQUARED_DISTANCE){
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
        if(armorStand == null) return;
        switch (rotationType) {
            case RIGHT:
                armorStand.setBodyPose(new Rotations(ReflectionUtils.rotX(armorStand.getBodyPose()), ReflectionUtils.rotY(armorStand.getBodyPose()) + rotate, ReflectionUtils.rotZ(armorStand.getBodyPose())));
                break;
            case UP:
                armorStand.setBodyPose(new Rotations(ReflectionUtils.rotX(armorStand.getBodyPose()) + rotate, ReflectionUtils.rotY(armorStand.getBodyPose()), ReflectionUtils.rotZ(armorStand.getBodyPose())));
                break;
            case ALL:
                armorStand.setBodyPose(new Rotations(ReflectionUtils.rotX(armorStand.getBodyPose()) + rotate, ReflectionUtils.rotY(armorStand.getBodyPose()) + rotate, ReflectionUtils.rotZ(armorStand.getBodyPose())));
                break;
        }
        for(UUID uuid : viewers){
            Player player = Bukkit.getPlayer(uuid);
            if(player == null) {
                viewers.remove(uuid);
                continue;
            }
            ReflectionUtils.getHandle(player).connection.send(new ClientboundSetEntityDataPacket(armorStand.getId(), armorStand.getEntityData().getNonDefaultValues()));
        }
    }

    public void rotateBigHead(boolean rotation, RotationType rotationType, float rotate) {
        if(!rotation) return;
        if(armorStand == null) return;
        switch (rotationType){
            case RIGHT:
                armorStand.setHeadPose(new Rotations(ReflectionUtils.rotX(armorStand.getHeadPose()), ReflectionUtils.rotY(armorStand.getHeadPose()) + rotate, ReflectionUtils.rotZ(armorStand.getHeadPose())));
                break;
            case UP:
                armorStand.setHeadPose(new Rotations(ReflectionUtils.rotX(armorStand.getHeadPose()) + rotate, ReflectionUtils.rotY(armorStand.getHeadPose()), ReflectionUtils.rotZ(armorStand.getHeadPose())));
                break;
            case ALL:
                armorStand.setHeadPose(new Rotations(ReflectionUtils.rotX(armorStand.getHeadPose()) + rotate, ReflectionUtils.rotY(armorStand.getHeadPose()) + rotate, ReflectionUtils.rotZ(armorStand.getHeadPose())));
                break;
        }
        for(UUID uuid : viewers){
            Player player = Bukkit.getPlayer(uuid);
            if(player == null) {
                viewers.remove(uuid);
                continue;
            }
            ReflectionUtils.getHandle(player).connection.send(new ClientboundSetEntityDataPacket(armorStand.getId(), armorStand.getEntityData().getNonDefaultValues()));
        }
    }

    public double getDistance() {
        return distance;
    }
}

