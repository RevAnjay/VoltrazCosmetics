package xyz.voltraz.cosmetics.listeners;

import xyz.voltraz.cosmetics.VoltrazCosmetics;
import xyz.voltraz.cosmetics.cache.ZoneAction;
import xyz.voltraz.cosmetics.events.ZoneEnterEvent;
import xyz.voltraz.cosmetics.events.ZoneExitEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ZoneListener implements Listener {
    private final VoltrazCosmetics plugin = VoltrazCosmetics.getInstance();
    @EventHandler
    public void onEnterZone(ZoneEnterEvent event) {
        Player player = event.getPlayer();
        ZoneAction onEnterAction = plugin.getZoneActions().getOnEnter();
        if(onEnterAction == null) return;
        onEnterAction.executeCommands(player, event.getZone().getId());
    }

    @EventHandler
    public void onExitZone(ZoneExitEvent event) {
        Player player = event.getPlayer();
        ZoneAction onExitAction = plugin.getZoneActions().getOnExit();
        if(onExitAction == null) return;
        onExitAction.executeCommands(player, event.getZone().getId());
    }
}
