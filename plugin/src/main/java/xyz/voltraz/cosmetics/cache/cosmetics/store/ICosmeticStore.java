package xyz.voltraz.cosmetics.cache.cosmetics.store;

import xyz.voltraz.cosmetics.api.Cosmetic;

import java.util.Map;

public interface ICosmeticStore {

    Map<String, Cosmetic> getCosmetics();

    boolean hasPermission();
}
