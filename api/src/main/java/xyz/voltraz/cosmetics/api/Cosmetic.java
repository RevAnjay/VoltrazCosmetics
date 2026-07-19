package xyz.voltraz.cosmetics.api;

import org.bukkit.entity.Player;

public abstract class Cosmetic {
    public abstract void hide(Player player);
    public abstract void show(Player player);
    public abstract void spawn(Player player);
    public abstract void despawn(Player player);
}
