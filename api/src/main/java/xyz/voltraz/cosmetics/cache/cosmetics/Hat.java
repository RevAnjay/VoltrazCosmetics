package xyz.voltraz.cosmetics.cache.cosmetics;

import xyz.voltraz.cosmetics.api.Cosmetic;
import org.bukkit.entity.Player;

// API stub for R7 compilation
public class Hat extends Cosmetic {
    private int hatId = -1;
    public int getHatId() { return hatId; }
    public void setHatId(int id) { this.hatId = id; }
    @Override public void hide(Player player) {}
    @Override public void show(Player player) {}
    @Override public void spawn(Player player) {}
    @Override public void despawn(Player player) {}
}
