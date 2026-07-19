package xyz.voltraz.cosmetics.cache.inventories.menus;

import xyz.voltraz.cosmetics.cache.PlayerData;
import xyz.voltraz.cosmetics.cache.inventories.ContentMenu;
import xyz.voltraz.cosmetics.cache.inventories.Menu;
import xyz.voltraz.cosmetics.cache.inventories.SlotMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class FreeMenu extends Menu {

    public FreeMenu(String id, ContentMenu contentMenu) {
        super(id, contentMenu);
    }

    public FreeMenu(PlayerData playerData, Menu menu) {
        super(playerData, menu);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        SlotMenu slotMenu = getContentMenu().getSlotMenuBySlot(slot);
        if(slotMenu == null) return;
        slotMenu.action(player);
    }

    @Override
    public void setItems() {
        for(SlotMenu slotMenu : getContentMenu().getSlotMenu().values()){
            slotMenu.getItems().addPlaceHolder(playerData.getOfflinePlayer().getPlayer());
            setItemInMenu(slotMenu);
        }
    }
}
