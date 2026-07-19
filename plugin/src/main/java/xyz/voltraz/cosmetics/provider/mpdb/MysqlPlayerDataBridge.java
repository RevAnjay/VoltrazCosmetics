package xyz.voltraz.cosmetics.provider.mpdb;

import xyz.voltraz.cosmetics.VoltrazCosmetics;
import net.craftersland.data.bridge.PD;
import net.craftersland.data.bridge.api.API;
import net.craftersland.data.bridge.api.events.SyncCompleteEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MysqlPlayerDataBridge implements Listener {
    private final VoltrazCosmetics plugin = VoltrazCosmetics.getInstance();
    private API api;

    public MysqlPlayerDataBridge() {
        api = PD.api;
    }

    @EventHandler
    public void onSyncInventory(SyncCompleteEvent event) {
        Player player = event.getPlayer();
        plugin.getSql().loadPlayerAsync(player);
    }
}
