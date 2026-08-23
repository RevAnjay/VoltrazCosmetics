package xyz.voltraz.cosmetics.cache.cosmetics.backpacks;
import xyz.voltraz.cosmetics.api.Cosmetic;
import org.bukkit.entity.Player;
public class Bag extends Cosmetic {
    private int backpackId = -1;
    public int getBackpackId() { return backpackId; }
    public void setBackpackId(int id) { this.backpackId = id; }
    @Override public void hide(Player player) {}
    @Override public void show(Player player) {}
    @Override public void spawn(Player player) {}
    @Override public void despawn(Player player) {}
}
