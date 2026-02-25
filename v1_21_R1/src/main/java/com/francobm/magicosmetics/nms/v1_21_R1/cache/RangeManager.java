package com.francobm.magicosmetics.nms.v1_21_R1.cache;

import com.francobm.magicosmetics.nms.IRangeManager;
import net.minecraft.server.level.ChunkMap;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class RangeManager implements IRangeManager {

    private final ChunkMap.EntityTracker tracked;

    public RangeManager(ChunkMap.EntityTracker tracked) {
        this.tracked = tracked;
    }

    @Override
    public void addPlayer(Player player) {
        tracked.f.add(((CraftPlayer) player).getHandle().c);
    }

    @Override
    public void removePlayer(Player player) {
        tracked.f.remove(((CraftPlayer) player).getHandle().c);
    }

    @Override
    public Set<Player> getPlayerInRange() {
        Set<Player> list = new HashSet<>();
        if(tracked == null) return list;
        tracked.f.forEach(serverServerGamePacketListenerImpl -> list.add(serverServerGamePacketListenerImpl.o().getBukkitEntity()));
        return list;
    }
}
