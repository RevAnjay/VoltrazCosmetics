package com.francobm.magicosmetics.cache;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe cache mapping entity IDs to Player objects.
 * Used by MCChannelHandler to avoid O(n) iteration over online players
 * when resolving entity IDs from packets.
 */
public class EntityIdCache {
    private static final Map<Integer, Player> cache = new ConcurrentHashMap<>();

    public static void register(Player player) {
        cache.put(player.getEntityId(), player);
    }

    public static void unregister(Player player) {
        cache.remove(player.getEntityId());
    }

    public static Player getPlayer(int entityId) {
        return cache.get(entityId);
    }
}
