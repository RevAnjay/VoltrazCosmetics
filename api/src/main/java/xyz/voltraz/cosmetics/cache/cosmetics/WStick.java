package xyz.voltraz.cosmetics.cache.cosmetics;

import xyz.voltraz.cosmetics.api.Cosmetic;
import org.bukkit.entity.Player;

// API stub for R7 compilation
public class WStick extends Cosmetic {
    private int wStickId = -1;
    public int getWStickId() { return wStickId; }
    public void setWStickId(int id) { this.wStickId = id; }
    @Override public void hide(Player player) {}
    @Override public void show(Player player) {}
    @Override public void spawn(Player player) {}
    @Override public void despawn(Player player) {}
}
