package xyz.voltraz.cosmetics;

import xyz.voltraz.cosmetics.managers.CosmeticsManager;
import org.bukkit.plugin.java.JavaPlugin;

public class VoltrazCosmetics extends JavaPlugin {
    private static VoltrazCosmetics instance;
    private CosmeticsManager cosmeticsManager;
    private String mainMenu = "hat";

    public static VoltrazCosmetics getInstance() {
        if (instance == null) {
            try {
                org.bukkit.plugin.Plugin p = org.bukkit.Bukkit.getPluginManager().getPlugin("VoltrazCosmetics");
                if (p instanceof VoltrazCosmetics) {
                    instance = (VoltrazCosmetics) p;
                }
            } catch (Throwable ignored) {}
        }
        return instance;
    }

    public static void setInstance(VoltrazCosmetics inst) {
        instance = inst;
    }

    public CosmeticsManager getCosmeticsManager() {
        return cosmeticsManager;
    }

    public void setCosmeticsManager(CosmeticsManager cosmeticsManager) {
        this.cosmeticsManager = cosmeticsManager;
    }

    public String getMainMenu() {
        return mainMenu;
    }

    public void setMainMenu(String mainMenu) {
        this.mainMenu = mainMenu;
    }
}