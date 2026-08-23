package xyz.voltraz.cosmetics.listeners;

import xyz.voltraz.cosmetics.VoltrazCosmetics;
import xyz.voltraz.cosmetics.cache.PlayerData;
import xyz.voltraz.cosmetics.cache.cosmetics.Hat;
import xyz.voltraz.cosmetics.cache.inventories.Menu;
import xyz.voltraz.cosmetics.cache.inventories.menus.FreeColoredMenu;
import xyz.voltraz.cosmetics.cache.inventories.menus.TokenMenu;
import xyz.voltraz.cosmetics.utils.FoliaUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryHolder;

public class InventoryListener implements Listener {

    @EventHandler
    public void onDrag(InventoryDragEvent event){
        InventoryHolder holder = event.getInventory().getHolder();
        if(holder instanceof FreeColoredMenu){
            event.setCancelled(true);
        }
        if(holder instanceof TokenMenu){
            TokenMenu menu = (TokenMenu) holder;
            if(!menu.isDrag()) return;
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event){
        InventoryHolder holder = event.getInventory().getHolder();
        if(holder instanceof FreeColoredMenu){
            FreeColoredMenu menu = (FreeColoredMenu) holder;
            menu.handleMenu(event);
            return;
        }
        if(holder instanceof TokenMenu){
            TokenMenu menu = (TokenMenu) holder;
            if(menu.isDrag()){
                menu.handleMenu(event);
                return;
            }
        }
        if(holder instanceof Menu){
            event.setCancelled(true);
            if(event.getCurrentItem() == null) return;
            if(event.getClickedInventory() == null) return;
            if(event.getClickedInventory().getType() == InventoryType.PLAYER) return;
            Menu menu = (Menu) holder;
            menu.handleMenu(event);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event){
        InventoryHolder holder = event.getInventory().getHolder();
        if(holder instanceof FreeColoredMenu){
            FreeColoredMenu menu = (FreeColoredMenu) holder;
            menu.returnItem();
        }
        if(holder instanceof TokenMenu){
            TokenMenu menu = (TokenMenu) holder;
            menu.returnItem();
        }
        // Post-close hat state verification: ensure helmet slot is consistent
        // with the hat cosmetic state after menu close (race condition fix).
        // This catches edge cases where rapid equip/unequip + close leaves
        // the hat state desynchronized from the actual helmet slot contents.
        if(holder instanceof Menu){
            Player player = (Player) event.getPlayer();
            PlayerData playerData = PlayerData.getPlayerIfPresent(player);
            if(playerData != null && playerData.getHat() != null) {
                FoliaUtil.runTaskLater(VoltrazCosmetics.getInstance(), () -> {
                    if(!player.isOnline()) return;
                    Hat hat = playerData.getHat();
                    if(hat == null) return;
                    // Reset closing flag since menu close is complete by now,
                    // then force re-sync helmet slot with hat state
                    hat.setClosing(false);
                    hat.update();
                }, 2L);
            }
        }
    }
}
