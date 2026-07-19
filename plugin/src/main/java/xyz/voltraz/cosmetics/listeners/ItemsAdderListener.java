package xyz.voltraz.cosmetics.listeners;

import xyz.voltraz.cosmetics.VoltrazCosmetics;
import xyz.voltraz.cosmetics.api.Cosmetic;
import xyz.voltraz.cosmetics.cache.*;
import xyz.voltraz.cosmetics.cache.inventories.Menu;
import xyz.voltraz.cosmetics.cache.items.Items;
import dev.lone.itemsadder.api.Events.CustomBlockInteractEvent;
import dev.lone.itemsadder.api.Events.CustomBlockPlaceEvent;
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class ItemsAdderListener implements Listener {
    private final VoltrazCosmetics plugin = VoltrazCosmetics.getInstance();

    @EventHandler
    public void onIALoadEvent(ItemsAdderLoadDataEvent event){
        if(event.getCause() != ItemsAdderLoadDataEvent.Cause.FIRST_LOAD) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.ava = plugin.getResourcePlugin().replaceFontImages(plugin.ava);
            plugin.unAva = plugin.getResourcePlugin().replaceFontImages(plugin.unAva);
            plugin.equip = plugin.getResourcePlugin().replaceFontImages(plugin.equip);
            plugin.getBossBar().clear();
            for(String lines : plugin.getMessages().getStringList("bossbar")){
                lines = plugin.getResourcePlugin().replaceFontImages(lines);
                BossBar boss = plugin.getServer().createBossBar(lines, plugin.bossBarColor, BarStyle.SOLID);
                boss.setVisible(true);
                plugin.getBossBar().add(boss);
            }
            Cosmetic.loadCosmetics();
            Color.loadColors();
            Items.loadItems();
            Zone.loadZones();
            Token.loadTokens();
            Sound.loadSounds();
            Menu.loadMenus();
        });
    }

    @EventHandler
    public void onPlaceBlocks(CustomBlockPlaceEvent event) {
        Player player = event.getPlayer();
        PlayerData playerData = PlayerData.getPlayerIfPresent(player);
        if(playerData == null || playerData.getWStick() == null) return;
        if(!playerData.getWStick().isCosmetic(event.getItemInHand())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onInteractBlocks(CustomBlockInteractEvent event) {
        if(event.getHand() != EquipmentSlot.OFF_HAND) return;
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        PlayerData playerData = PlayerData.getPlayerIfPresent(player);
        if(playerData == null || playerData.getWStick() == null) return;
        if(!playerData.getWStick().isCosmetic(item)) return;
        event.setCancelled(true);
    }
}
