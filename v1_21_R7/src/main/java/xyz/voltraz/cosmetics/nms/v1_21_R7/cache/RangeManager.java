package xyz.voltraz.cosmetics.nms.v1_21_R7.cache;

import xyz.voltraz.cosmetics.nms.v1_21_R7.ReflectionUtils;
import xyz.voltraz.cosmetics.nms.IRangeManager;
import net.minecraft.server.level.ChunkMap;
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
        tracked.seenBy.add(ReflectionUtils.getHandle(player).connection);
    }

    @Override
    public void removePlayer(Player player) {
        tracked.seenBy.remove(ReflectionUtils.getHandle(player).connection);
    }

    @Override
    public Set<Player> getPlayerInRange() {
        Set<Player> list = new HashSet<>();
        if(tracked == null) return list;
        tracked.seenBy.forEach(c -> {
            org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(c.getPlayer().getUUID());
            if (p != null) list.add(p);
        });
        return list;
    }
}
