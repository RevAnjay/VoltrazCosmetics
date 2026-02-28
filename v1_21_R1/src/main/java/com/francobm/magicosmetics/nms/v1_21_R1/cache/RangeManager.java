package com.francobm.magicosmetics.nms.v1_21_R1.cache;

import com.francobm.magicosmetics.nms.IRangeManager;
import net.minecraft.server.level.ChunkMap;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class RangeManager implements IRangeManager {

    private final ChunkMap.TrackedEntity tracked;

    public RangeManager(ChunkMap.TrackedEntity tracked) {
        this.tracked = tracked;
    }

    @Override
    public void addPlayer(Player player) {
        tracked.seenBy.add(((CraftPlayer) player).getHandle().connection);
    }

    @Override
    public void removePlayer(Player player) {
        tracked.seenBy.remove(((CraftPlayer) player).getHandle().connection);
    }

    @Override
    public Set<Player> getPlayerInRange() {
        Set<Player> list = new HashSet<>();
        if(tracked == null) return list;
        tracked.seenBy.forEach(serverServerGamePacketListenerImpl -> list.add(serverServerGamePacketListenerImpl.getPlayer().getBukkitEntity()));
        return list;
    }
}
