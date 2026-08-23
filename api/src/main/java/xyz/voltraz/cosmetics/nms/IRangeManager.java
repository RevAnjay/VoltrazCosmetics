package xyz.voltraz.cosmetics.nms;

import org.bukkit.entity.Player;

import java.util.Set;

public interface IRangeManager {
    void addPlayer(Player player);
    void removePlayer(Player player);

    Set<Player> getPlayerInRange();
}
