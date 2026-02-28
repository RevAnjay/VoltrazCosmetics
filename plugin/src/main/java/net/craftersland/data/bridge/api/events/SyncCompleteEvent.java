package net.craftersland.data.bridge.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Stub class for MysqlPlayerDataBridge dependency.
 * The actual class is provided at runtime by the MysqlPlayerDataBridge plugin.
 */
public class SyncCompleteEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    public Player getPlayer() {
        return null;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
