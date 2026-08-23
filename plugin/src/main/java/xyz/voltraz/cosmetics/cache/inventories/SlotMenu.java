package xyz.voltraz.cosmetics.cache.inventories;

import xyz.voltraz.cosmetics.VoltrazCosmetics;
import xyz.voltraz.cosmetics.api.Cosmetic;
import xyz.voltraz.cosmetics.cache.*;
import xyz.voltraz.cosmetics.cache.inventories.menus.FreeColoredMenu;
import xyz.voltraz.cosmetics.cache.items.Items;
import xyz.voltraz.cosmetics.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SlotMenu {
    private final int slot;
    private Items items;
    private final List<ActionType> actionType;
    private final List<String> commands;
    private final String menu;
    private final SlotMenu slotMenu;
    private final Cosmetic cosmetic;
    private final Token token;
    private Sound sound;
    private String permission;
    private boolean exchangeable;
    private Cosmetic tempCosmetic;
    private ItemStack oldToken;
    private long lastClickTime = 0;
    private static final long CLICK_COOLDOWN_MS = 250;

    public SlotMenu(int slot, Items items, List<String> commands, String menu, SlotMenu slotMenu, Cosmetic cosmetic, Token token, Sound sound, ActionType... actionType) {
        this.slot = slot;
        this.items = items;
        this.actionType = Arrays.asList(actionType);
        this.commands = commands;
        this.menu = menu;
        this.slotMenu = slotMenu;
        this.cosmetic = cosmetic;
        this.token = token;
        this.sound = sound;
    }

    public SlotMenu(int slot, Items items, List<String> commands, String menu, ActionType... actionType) {
        this.slot = slot;
        this.items = items;
        this.actionType = Arrays.asList(actionType);
        this.commands = commands;
        this.menu = menu;
        this.slotMenu = null;
        this.cosmetic = null;
        this.token = null;
        this.sound = null;
        this.permission = "";
    }

    public SlotMenu(int slot, Items items, List<String> commands, String menu, Sound sound, String permission, List<ActionType> actionTypes) {
        this.slot = slot;
        this.items = items;
        this.actionType = actionTypes;
        this.commands = commands;
        this.menu = menu;
        this.slotMenu = null;
        this.cosmetic = null;
        this.token = null;
        this.sound = sound;
        this.permission = permission;
    }

    public SlotMenu(int slot, Items items, List<String> commands, ActionType... actionType) {
        this.slot = slot;
        this.items = items;
        this.actionType = Arrays.asList(actionType);
        this.commands = commands;
        this.menu = "";
        this.slotMenu = null;
        this.cosmetic = null;
        this.token = null;
        this.sound = null;
        this.permission = "";
    }

    public SlotMenu(int slot, Items items, String menu, ActionType... actionType) {
        this.slot = slot;
        this.items = items;
        this.actionType = Arrays.asList(actionType);
        this.commands = new ArrayList<>();
        this.menu = menu;
        this.slotMenu = null;
        this.cosmetic = null;
        this.token = null;
        this.sound = null;
        this.permission = "";
    }

    public SlotMenu(int slot, Items items, SlotMenu slotMenu, ActionType... actionType) {
        this.slot = slot;
        this.items = items;
        this.actionType = Arrays.asList(actionType);
        this.commands = new ArrayList<>();
        this.menu = "";
        this.slotMenu = slotMenu;
        this.cosmetic = null;
        this.token = null;
        this.sound = null;
        this.permission = "";
    }

    public SlotMenu(int slot, Items items, Cosmetic cosmetic, ActionType... actionType) {
        this.slot = slot;
        this.items = items;
        this.actionType = Arrays.asList(actionType);
        this.commands = new ArrayList<>();
        this.menu = "";
        this.slotMenu = null;
        this.cosmetic = cosmetic;
        this.token = null;
        this.sound = null;
        this.permission = "";
    }

    public SlotMenu(int slot, Items items, Token token, ItemStack oldToken, ActionType... actionType) {
        this.slot = slot;
        this.items = items;
        this.actionType = Arrays.asList(actionType);
        this.commands = new ArrayList<>();
        this.menu = "";
        this.slotMenu = null;
        this.cosmetic = null;
        this.oldToken = oldToken;
        this.token = token;
        this.sound = null;
        this.permission = "";
    }

    public int getSlot() {
        return slot;
    }

    public Items getItems() {
        return items;
    }

    public List<ActionType> getActionType() {
        return actionType;
    }

    public List<String> getCommands() {
        return commands;
    }

    public void action(Player player){
        VoltrazCosmetics plugin = VoltrazCosmetics.getInstance();
        if(!permission.isEmpty()){
            if(!player.hasPermission(permission)){
                player.sendMessage(plugin.prefix + plugin.getMessages().getString("no-permission"));
                return;
            }
        }
        // Click cooldown to prevent rapid double-click race conditions
        long now = System.currentTimeMillis();
        if(now - lastClickTime < CLICK_COOLDOWN_MS) return;
        lastClickTime = now;
        playSound(player);
        for(ActionType actionType : actionType){
            switch (actionType){
                case OPEN_MENU:
                    openMenu(player);
                    break;
                case CLOSE_MENU:
                    closeMenu(player);
                    break;
                case PLAYER_COMMAND:
                case CONSOLE_COMMAND:
                case COMMAND:
                    runCommands(player);
                    break;
                case ADD_ITEM_MENU:
                    addItemMenu(player);
                    break;
                case PREVIEW_ITEM:
                    previewItem(player);
                    break;
                case REMOVE_TOKEN_ADD_COSMETIC:
                    removeTokenAddCosmetic(player);
                    break;
                case UPDATE_OLD_TOKEN:
                    updateOldToken(player);
                    break;
                case DRAG_AND_DROP:
                    break;
                default:
                    break;
            }
        }
    }

    public boolean action(Player player, ActionType actionType){
        VoltrazCosmetics plugin = VoltrazCosmetics.getInstance();
        if(!permission.isEmpty()) {
            if (!player.hasPermission(permission)) {
                player.sendMessage(plugin.prefix + plugin.getMessages().getString("no-permission"));
                return false;
            }
        }
        switch (actionType){
            case OPEN_MENU:
                openMenu(player);
                break;
            case CLOSE_MENU:
                closeMenu(player);
                break;
            case REFRESH:
                refreshMenu(player);
                break;
            case PLAYER_COMMAND:
            case CONSOLE_COMMAND:
            case COMMAND:
                runCommands(player);
                break;
            case ADD_ITEM_MENU:
                addItemMenu(player);
                break;
            case PREVIEW_ITEM:
                previewItem(player);
                break;
            case REMOVE_TOKEN_ADD_COSMETIC:
                removeTokenAddCosmetic(player);
                break;
            case REMOVE_COSMETIC_ADD_TOKEN:
                boolean allow = removeCosmeticAddToken(player);
                if(allow)
                    playSound(player);
                return allow;
            case UPDATE_OLD_TOKEN:
                updateOldToken(player);
                break;
            case DRAG_AND_DROP:
                break;
            default:
                break;
        }
        playSound(player);
        return true;
    }

    private void runCommands(Player player){
        for(String command : commands){
            command = command.replace("%player%", player.getName());
            for(ActionType actionType : actionType){
                switch (actionType){
                    case COMMAND:
                    case CONSOLE_COMMAND:
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                        if(command.startsWith("magiccos unset ")){
                            refreshMenu(player);
                        }
                        break;
                    case PLAYER_COMMAND:
                        player.performCommand(command);
                        if(command.startsWith("magiccos unset ")){
                            refreshMenu(player);
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void removeTokenAddCosmetic(Player player){
        PlayerData playerData = PlayerData.getPlayer(player);
        if(playerData.removeTokenInPlayer()){
            VoltrazCosmetics.getInstance().getCosmeticsManager().changeCosmetic(player, token.getCosmetic(), token.getTokenType());
        }
        closeMenu(player);
    }

    private void updateOldToken(Player player) {
        for(int i = 0; i < oldToken.getAmount(); i++){
            player.getInventory().addItem(token.getItemStack());
        }
        player.getInventory().removeItem(oldToken);
        closeMenu(player);
    }

    private boolean removeCosmeticAddToken(Player player){
        if(tempCosmetic == null) return false;
        PlayerData playerData = PlayerData.getPlayer(player);
        if(!playerData.hasCosmeticById(tempCosmetic.getId())) return false;
        return VoltrazCosmetics.getInstance().getCosmeticsManager().unUseCosmetic(player, tempCosmetic.getId());
    }

    private void previewItem(Player player){
        VoltrazCosmetics plugin = VoltrazCosmetics.getInstance();
        PlayerData playerData = PlayerData.getPlayer(player);
        if(plugin.isPermissions()){
            if(!cosmetic.hasPermission(player)) {
                if(playerData.isZone()) {
                    VoltrazCosmetics.getInstance().getCosmeticsManager().previewCosmetic(player, cosmetic);
                }
                closeMenu(player);
                return;
            }
            if(playerData.isZone()) {
                VoltrazCosmetics.getInstance().getCosmeticsManager().previewCosmetic(player, cosmetic);
            }
            if(!cosmetic.isColorBlocked()) {
                VoltrazCosmetics.getInstance().getCosmeticsManager().equipCosmetic(player, cosmetic.getId(), null, false);
            }
            closeMenu(player);
            return;
        }
        if(playerData.getCosmeticById(cosmetic.getId()) == null){
            if(playerData.isZone()) {
                VoltrazCosmetics.getInstance().getCosmeticsManager().previewCosmetic(player, cosmetic);
            }
            closeMenu(player);
            return;
        }
        // Use existing cosmetic data from player — do NOT remove+re-add which loses
        // currentItemSaved state (helmet stored in Hat) and causes helmet disappearance.
        if(!cosmetic.isColorBlocked()) {
            VoltrazCosmetics.getInstance().getCosmeticsManager().equipCosmetic(player, cosmetic.getId(), null, false);
        }
        if(playerData.isZone()) {
            VoltrazCosmetics.getInstance().getCosmeticsManager().previewCosmetic(player, cosmetic);
        }
        closeMenu(player);
    }

    private void addItemMenu(Player player){
        InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
        if(holder instanceof Menu){
            Menu menu = (Menu) holder;
            menu.getContentMenu().addSlotMenu(slotMenu);
            menu.setItemInMenu(slotMenu);
        }
    }

    public void playSound(Player player){
        if(sound == null) {
            return;
        }
        Utils.sendSound(player, sound);
    }

    private void refreshMenu(Player player){
        if((player.getOpenInventory().getTopInventory().getHolder() instanceof Menu)) {
            Menu menu = (Menu) player.getOpenInventory().getTopInventory().getHolder();
            menu.setItems();
        }
    }

    private void openMenu(Player player){
        String[] split = menu.split("\\|");
        if(split.length == 3){
            Color color = Color.getColor(split[1]);
            Cosmetic cosmetic = Cosmetic.getCloneCosmetic(split[2]);
            if(color == null){
                VoltrazCosmetics.getInstance().getLogger().info("Color Null");
                return;
            }
            if(cosmetic == null){
                VoltrazCosmetics.getInstance().getLogger().info("Cosmetic Null");
                return;
            }
            VoltrazCosmetics.getInstance().getCosmeticsManager().openMenuColor(player, split[0], color, cosmetic);
            return;
        }
        if(split.length == 2){
            if(!(player.getOpenInventory().getTopInventory().getHolder() instanceof FreeColoredMenu)) return;
            FreeColoredMenu menu = (FreeColoredMenu) player.getOpenInventory().getTopInventory().getHolder();
            Color color = Color.getColor(split[1]);
            if(color == null){
                VoltrazCosmetics.getInstance().getLogger().info("Color Null");
                return;
            }
            menu.setPage(0);
            menu.setColor(color);
            menu.setSecondaryColor((SecondaryColor) null);
            menu.setItems();
            return;
        }
        VoltrazCosmetics.getInstance().getCosmeticsManager().openMenu(player, this.menu);
    }

    public void setSound(Sound sound) {
        this.sound = sound;
    }

    public Sound getSound() {
        return sound;
    }

    private void closeMenu(Player player){
        player.closeInventory();
    }

    public String getMenu() {
        return menu;
    }

    public SlotMenu getSlotMenu() {
        return slotMenu;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public void setItems(Items items) {
        this.items = items;
    }

    public Token getToken() {
        return token;
    }

    public ItemStack getOldToken() {
        return oldToken;
    }

    public void setExchangeable(boolean exchangeable) {
        this.exchangeable = exchangeable;
    }

    public boolean isExchangeable() {
        return exchangeable;
    }

    public void setTempCosmetic(Cosmetic tempCosmetic) {
        this.tempCosmetic = tempCosmetic;
    }

    public Cosmetic getTempCosmetic() {
        return tempCosmetic;
    }
}
