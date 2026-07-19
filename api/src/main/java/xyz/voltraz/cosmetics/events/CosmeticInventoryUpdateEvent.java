package xyz.voltraz.cosmetics.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

public class CosmeticInventoryUpdateEvent extends PlayerEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final Object cosmeticType;
    private final Object cosmetic;
    private final ItemStack itemToChange;

    public CosmeticInventoryUpdateEvent(Player player, Object cosmeticType, Object cosmetic, ItemStack itemToChange) {
        super(player);
        this.cosmeticType = cosmeticType;
        this.cosmetic = cosmetic;
        this.itemToChange = itemToChange;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    public Object getCosmetic() {
        return cosmetic;
    }

    public Object getCosmeticType() {
        return cosmeticType;
    }

    public ItemStack getItemToChange() {
        return itemToChange;
    }
}