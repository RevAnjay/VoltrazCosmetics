package xyz.voltraz.cosmetics.listeners;

import xyz.voltraz.cosmetics.VoltrazCosmetics;
import xyz.voltraz.cosmetics.cache.PlayerData;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.event.SkinApplyEvent;
import org.bukkit.entity.Player;

public class SkinListener {
    private final VoltrazCosmetics plugin = VoltrazCosmetics.getInstance();

    public SkinListener() {
        SkinsRestorerProvider.get().getEventBus().subscribe(plugin, SkinApplyEvent.class, event -> {
            Player player = event.getPlayer(Player.class);
            PlayerData playerData = PlayerData.getPlayerIfPresent(player);
            if(playerData == null) return;
            plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, playerData::clearBag, 20L);
        });
    }
}
