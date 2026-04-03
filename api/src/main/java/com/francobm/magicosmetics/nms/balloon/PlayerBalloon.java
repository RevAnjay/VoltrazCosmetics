package com.francobm.magicosmetics.nms.balloon;

import com.francobm.magicosmetics.cache.RotationType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class PlayerBalloon {
    // Reusable scratch objects to avoid per-tick GC allocation
    protected static final Vector ZERO_VEC = new Vector(0, 0, 0);
    protected final Location tempPlayerLoc = new Location(null, 0, 0, 0);
    protected final Location tempStandLoc = new Location(null, 0, 0, 0);
    protected final Location tempDistLoc1 = new Location(null, 0, 0, 0);
    protected final Location tempTeleportLoc = new Location(null, 0, 0, 0);
    protected final Vector tempStandDir = new Vector();
    protected final Vector tempLineBetween = new Vector();
    protected final Vector tempDistVec = new Vector();
    public static Map<UUID, PlayerBalloon> playerBalloons = new ConcurrentHashMap<>();
    protected UUID uuid;
    protected List<UUID> viewers;
    protected List<UUID> hideViewers;
    protected LivingEntity lendEntity;
    protected boolean floatLoop = true;
    protected double y = 0;
    protected double height = 0;
    protected boolean heightLoop = true;
    protected float rotate = -0.4f;
    protected double rot = 0;
    protected boolean rotateLoop = true;
    protected double space;
    protected boolean bigHead = false;
    protected boolean invisibleLeash;
    protected boolean lendEntityDirty = false;


    public static void updatePlayerBalloon(Player player){
        for(PlayerBalloon playerBalloon : playerBalloons.values()){
            playerBalloon.remove(player);
            playerBalloon.spawn(player);
        }
    }

    public static void removePlayerBagByPlayer(Player player){
        for(PlayerBalloon playerBalloon : playerBalloons.values()){
            if(player.getUniqueId().equals(playerBalloon.uuid)) continue;
            if(!playerBalloon.viewers.contains(player.getUniqueId())) continue;
            playerBalloon.remove(player);
        }
    }

    public abstract void spawn(Player player);

    public abstract void spawn(boolean exception);

    public abstract void remove();

    public abstract void remove(Player player);

    public abstract void setItem(org.bukkit.inventory.ItemStack itemStack);

    public abstract void lookEntity(float yaw, float pitch);

    protected abstract void teleport(Location location);

    protected abstract void instantUpdate();

    public abstract void update(boolean instantFollow);

    public abstract void rotate(boolean rotation, RotationType rotationType, float rotate);

    public void setLendEntity(LivingEntity lendEntity) {
        this.lendEntity = lendEntity;
        this.lendEntityDirty = true;
    }

    public Player getPlayer(){
        return Bukkit.getPlayer(uuid);
    }

    public UUID getUuid() {
        return uuid;
    }

    public List<UUID> getViewers() {
        return viewers;
    }

    public boolean isBigHead() {
        return bigHead;
    }

    public List<UUID> getHideViewers() {
        return hideViewers;
    }

    public void addHideViewer(Player player) {
        if(hideViewers.contains(player.getUniqueId())) return;
        hideViewers.add(player.getUniqueId());
        remove(player);
    }

    public void removeHideViewer(Player player) {
        hideViewers.remove(player.getUniqueId());
    }
}
