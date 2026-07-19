package xyz.voltraz.cosmetics.cache;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe cache mapping entity IDs to Player UUIDs.
 * Uses UUID instead of Player references to prevent memory leaks
 * if unregister() fails to fire on disconnect.
 */
public class EntityIdCache {
    private static final Map<Integer, UUID> cache = new ConcurrentHashMap<>();

    public static void register(Player player) {
        cache.put(player.getEntityId(), player.getUniqueId());
    }

    public static void unregister(Player player) {
        cache.remove(player.getEntityId());
    }

    public static Player getPlayer(int entityId) {
        UUID uuid = cache.get(entityId);
        if(uuid == null) return null;
        return Bukkit.getPlayer(uuid);
    }
}
