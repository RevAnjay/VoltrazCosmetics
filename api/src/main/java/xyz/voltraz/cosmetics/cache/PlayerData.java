package xyz.voltraz.cosmetics.cache;

import xyz.voltraz.cosmetics.api.Cosmetic;
import xyz.voltraz.cosmetics.cache.cosmetics.Hat;
import xyz.voltraz.cosmetics.cache.cosmetics.WStick;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// API stub for R7 NMS module compilation. The real implementation lives in the plugin module.
// R7 only calls: getHat(), getWStick(), getBag(), isZone(), getPlayerIfPresent(), getCosmetic(), setCosmetic().
public class PlayerData {
    private static final ConcurrentHashMap<UUID, PlayerData> players = new ConcurrentHashMap<>();

    private final UUID uuid;
    private String cosmetic;
    private Hat hat;
    private WStick wStick;
    private Cosmetic bag;
    private boolean zone;

    public PlayerData(Player player, String userId) {
        this.uuid = player.getUniqueId();
        players.put(uuid, this);
    }

    public static PlayerData getPlayerIfPresent(OfflinePlayer player) {
        return players.get(player.getUniqueId());
    }

    public Hat getHat() { return hat; }
    public WStick getWStick() { return wStick; }
    public Cosmetic getBag() { return bag; }
    public boolean isZone() { return zone; }
    public String getCosmetic() { return cosmetic; }
    public void setCosmetic(String cosmetic) { this.cosmetic = cosmetic; }
    public void setHat(Hat hat) { this.hat = hat; }
    public void setWStick(WStick wStick) { this.wStick = wStick; }
    public void setBag(Cosmetic bag) { this.bag = bag; }
    public void setZone(boolean zone) { this.zone = zone; }
    public UUID getUuid() { return uuid; }
}
