package xyz.voltraz.cosmetics.managers;

import xyz.voltraz.cosmetics.api.*;
import xyz.voltraz.cosmetics.cache.EntityCache;
import xyz.voltraz.cosmetics.VoltrazCosmetics;
import xyz.voltraz.cosmetics.cache.*;
import xyz.voltraz.cosmetics.cache.cosmetics.backpacks.Bag;
import xyz.voltraz.cosmetics.cache.inventories.Menu;
import xyz.voltraz.cosmetics.cache.inventories.PaginatedMenu;
import xyz.voltraz.cosmetics.cache.inventories.menus.*;
import xyz.voltraz.cosmetics.cache.items.Items;
import xyz.voltraz.cosmetics.events.CosmeticChangeEquipEvent;
import xyz.voltraz.cosmetics.events.CosmeticEquipEvent;
import xyz.voltraz.cosmetics.events.CosmeticUnEquipEvent;
import xyz.voltraz.cosmetics.files.FileCreator;
import xyz.voltraz.cosmetics.nms.NPC.NPC;
import xyz.voltraz.cosmetics.nms.bag.EntityBag;
import xyz.voltraz.cosmetics.nms.balloon.EntityBalloon;
import xyz.voltraz.cosmetics.nms.balloon.PlayerBalloon;
import xyz.voltraz.cosmetics.nms.spray.CustomSpray;
import xyz.voltraz.cosmetics.utils.Utils;
import xyz.voltraz.cosmetics.utils.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import xyz.voltraz.cosmetics.utils.FoliaUtil;
import xyz.voltraz.cosmetics.utils.Utils;

import java.util.*;

public class CosmeticsManager {

    private final VoltrazCosmetics plugin;
    private Object balloons;
    private Object otherCosmetics;
    private Object npcTask;
    private Object autoSaveTask;

    public CosmeticsManager() {
        this.plugin = VoltrazCosmetics.getInstance();
        loadNewMessages();
    }
    public void sendMessage(CommandSender sender, String message) {
        Utils.sendMessage(sender, message);
    }

    public void loadNewMessages() {
        FileCreator messages = plugin.getMessages();
        FileCreator config = plugin.getConfig();
        FileCreator zones = plugin.getZones();
        if(!zones.contains("on_enter.commands"))
            zones.set("on_enter.commands", Collections.singletonList("[console] say &aThe %player% has entered the wardrobe"));
        if(!zones.contains("on_exit.commands"))
            zones.set("on_exit.commands", Collections.singletonList("[player] say &cThe %player% has come out of the wardrobe"));
        if(!messages.contains("world-blacklist"))
            messages.set("world-blacklist", "&cYou cant use this command in this world!");
        if(!messages.contains("already-all-unlocked")){
            messages.set("already-all-unlocked", "&cThe player already has all the cosmetics unlocked!");
        }
        if(!messages.contains("already-all-locked")) {
            messages.set("already-all-locked", "&cThe player already has all the cosmetics locked!");
        }
        if(!messages.contains("remove-all-cosmetic")){
            messages.set("remove-all-cosmetic", "&aYou have successfully removed all cosmetics from the player.");
        }
        if(!messages.contains("commands.remove-all-usage")) {
            messages.set("commands.remove-all-usage", "&c/cosmetics removeall <player>");
        }
        if(!messages.contains("spray-cooldown")) {
            messages.set("spray-cooldown", "&cYou must wait &e%time% &cbefore you can spray again!");
        }
        if(!messages.contains("exit-color-without-perm")) {
            messages.set("exit-color-without-perm", "&cOne or more cosmetics have colors that you dont have access to, so they have become unequipped!");
        }
        if(!config.contains("show-all-cosmetics-in-menu"))
            config.set("show-all-cosmetics-in-menu", true);
        if(!config.contains("placeholder-api"))
            config.set("placeholder-api", false);
        if(!config.contains("luckperms-server"))
            config.set("luckperms-server", "");
        if(!config.contains("main-menu"))
            config.set("main-menu", "hat");
        if(!config.contains("save-data-delay"))
            config.set("save-data-delay", 300);
        if(!config.contains("zones-actions"))
            config.set("zones-actions", false);
        if(!config.contains("on_execute_cosmetics"))
            config.set("on_execute_cosmetics", "");
        if(!config.contains("worlds-blacklist"))
            config.set("worlds-blacklist", Arrays.asList("test", "test1"));
        if(!config.contains("proxy")) {
            config.set("proxy", false);
        }
        if(!config.contains("worldguard-support"))
            config.set("worldguard-support", true);
        zones.save();
        config.save();
        messages.save();
    }

    public void runTasks(){
            if(otherCosmetics == null){
            otherCosmetics = FoliaUtil.runTaskTimer(plugin, () -> {
                for(PlayerData playerData : PlayerData.players.values()){
                    if(playerData.getOfflinePlayer() == null) continue;
                    Player player = playerData.getOfflinePlayer().getPlayer();
                    if(player == null) continue;
                    boolean needsCosmeticUpdate = playerData.hasActiveCosmetics();
                    boolean needsZoneUpdate = playerData.getZone() != null || !Zone.zones.isEmpty();
                    // Skip players that have nothing to update
                    if(!needsCosmeticUpdate && !needsZoneUpdate) continue;
                    if(needsCosmeticUpdate) playerData.activeCosmetics(player);
                    if(needsZoneUpdate) playerData.enterZone(player);
                }
            }, 5L, 4L);
        }
        if(balloons == null) {
            final int[] entityCleanupCounter = {0};
            balloons = FoliaUtil.runTaskTimer(plugin, () -> {
                for(PlayerData playerData : PlayerData.players.values()){
                    if(playerData.getOfflinePlayer() == null) continue;
                    if(playerData.getOfflinePlayer().getPlayer() == null) continue;
                    playerData.activeBalloon();
                }
                for(EntityCache entityCache : EntityCache.entities.values()){
                    entityCache.activeCosmetics();
                }
                if(++entityCleanupCounter[0] >= 50) {
                    entityCleanupCounter[0] = 0;
                    EntityCache.cleanupInvalid();
                    // Periodic cleanup of orphaned PlayerData entries (players no longer online)
                    // Save data before removing to prevent loss if quit handler crashed
                    PlayerData.players.entrySet().removeIf(entry -> {
                        Player p = Bukkit.getPlayer(entry.getKey());
                        if(p == null || !p.isOnline()) {
                            PlayerData orphan = entry.getValue();
                            // Skip entries that have no offlinePlayer set — likely still loading
                            if(orphan.getOfflinePlayer() == null) return false;
                            try {
                                plugin.getSql().savePlayerAsync(orphan);
                            } catch (Exception e) {
                                plugin.getLogger().warning("Failed to save orphaned PlayerData for " + entry.getKey() + ": " + e.getMessage());
                            }
                            return true;
                        }
                        return false;
                    });
                }
            }, 0L, 4L);
        }
        if(npcTask == null && !NPC.npcs.isEmpty()) {
            final int[] npcAngle = {0};
            npcTask = FoliaUtil.runTaskTimer(plugin, () -> {
                if(NPC.npcs.isEmpty()) {
                    FoliaUtil.cancel(npcTask);
                    npcTask = null;
                    return;
                }
                for(Player player : Bukkit.getOnlinePlayers()){
                    NPC npc = plugin.getVersion().getNPC(player);
                    if(npc == null) continue;
                    npc.lookNPC(player, npcAngle[0]);
                }
                npcAngle[0] = npcAngle[0] + 10;
            }, plugin.getConfig().getLong("npc-rotation"), plugin.getConfig().getLong("npc-rotation"));
        }
        if(autoSaveTask == null) {
            // Auto-save dirty player data asynchronously every 5 minutes (6000 ticks)
            autoSaveTask = FoliaUtil.runTaskTimerAsync(plugin, () -> {
                if (plugin.getSql() == null) return;
                for (PlayerData playerData : PlayerData.players.values()) {
                    if (playerData != null && playerData.isDirty() && playerData.getOfflinePlayer() != null) {
                        playerData.setDirty(false);
                        plugin.getSql().savePlayerAsync(playerData);
                    }
                }
            }, 6000L, 6000L);
        }
    }

    public boolean npcTaskStopped() {
        return npcTask == null;
    }

    public void reRunTasks() {
        runTasks();
    }

    public void sendCheck(Player player){
        if(!player.hasPermission("cosmetics.admin")){
            sendMessage(player, plugin.prefix + plugin.getMessages().getString("no-permission"));
            return;
        }
        sendMessage(player, plugin.prefix + "&aVoltrazCosmetics v" + plugin.getDescription().getVersion() + " (Status: OK)");
    }

    public void cancelTasks(){
        if(otherCosmetics != null) { FoliaUtil.cancel(otherCosmetics); otherCosmetics = null; }
        if(balloons != null) { FoliaUtil.cancel(balloons); balloons = null; }
        if(npcTask != null) { FoliaUtil.cancel(npcTask); npcTask = null; }

        EntityCache.clearAll();
        NPC.npcs.clear();
        // Clear NMS static maps to prevent entity reference leaks
        // Each map cleanup is wrapped in try-catch so one failure doesn't prevent the rest
        try {
            PlayerBalloon.playerBalloons.values().forEach(balloon -> {
                try { balloon.remove(); } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {} finally {
            PlayerBalloon.playerBalloons.clear();
        }
        try {
            EntityBag.entityBags.values().forEach(bag -> {
                try { bag.remove(); } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {} finally {
            EntityBag.entityBags.clear();
        }
        try {
            EntityBalloon.entitiesBalloon.values().forEach(balloon -> {
                try { balloon.remove(); } catch (Exception ignored) {}
            });
            if(autoSaveTask != null) {
                FoliaUtil.cancel(autoSaveTask);
                autoSaveTask = null;
            }
        } catch (Exception ignored) {} finally {
            EntityBalloon.entitiesBalloon.clear();
        }
        try {
            CustomSpray.customSprays.values().forEach(spray -> {
                try { spray.remove(); } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {} finally {
            CustomSpray.customSprays.clear();
        }
    }

    public void reload(CommandSender sender){
        if(sender != null) {
            if (!sender.hasPermission("magicosmetics.reload")) {
                if (sender instanceof Player) {
                    sendMessage(sender, plugin.prefix + plugin.getMessages().getString("no-permission"));
                    return;
                }
                sender.sendMessage(plugin.prefix + plugin.getMessages().getString("no-permission"));
                return;
            }
        }
        plugin.getCosmeticsManager().cancelTasks();
        plugin.getConfig().reload();
        plugin.getCosmetics().reloadFiles();
        plugin.getMessages().reload();
        plugin.getSounds().reload();
        plugin.getMenus().reload();
        plugin.getTokens().reload();
        plugin.getZones().reload();
        plugin.getNPCs().reload();
        plugin.registerData();

        Cosmetic.loadCosmetics();
        Color.loadColors();
        Items.loadItems();
        Token.loadTokens();
        Sound.loadSounds();
        Menu.loadMenus();
        Zone.loadZones();
        PlayerData.reload();
        plugin.getNPCsLoader().load();
        plugin.getCosmeticsManager().runTasks();
        if(sender == null) return;
        if(sender instanceof Player) {
            sendMessage(sender, plugin.prefix + plugin.getMessages().getString("reload"));
            return;
        }
        sender.sendMessage(plugin.prefix + plugin.getMessages().getString("reload"));
    }

    public void changeCosmetic(Player player, String cosmeticId, TokenType tokenType){
        if(tokenType != null) {
            List<Cosmetic> cosmetics = new ArrayList<>();
            PlayerData playerData = PlayerData.getPlayer(player);
            if(tokenType.getCosmeticType() == null){
                for(Cosmetic cosmetic : Cosmetic.cosmetics.values()){
                    if(!playerData.hasCosmeticById(cosmetic.getId()))
                        cosmetics.add(cosmetic);
                }
            }else{
                for(Cosmetic cosmetic : Cosmetic.getCosmeticsByType(tokenType.getCosmeticType())){
                    if(!playerData.hasCosmeticById(cosmetic.getId()))
                        cosmetics.add(cosmetic);
                }
            }
            if(cosmetics.isEmpty()) return;
            Cosmetic newCosmetic = cosmetics.get(new Random().nextInt(cosmetics.size()));
            playerData.addCosmetic(newCosmetic);
            for(String msg : plugin.getMessages().getStringList("change-token-to-cosmetic")){
                sendMessage(player, msg);
            }
            playerData.sendSavePlayerData();
            return;
        }
        Cosmetic cosmetic = Cosmetic.getCloneCosmetic(cosmeticId);
        if(cosmetic == null) return;
        PlayerData playerData = PlayerData.getPlayer(player);
        if(playerData.hasCosmeticById(cosmeticId)) return;
        playerData.addCosmetic(cosmetic);
        for(String msg : plugin.getMessages().getStringList("change-token-to-cosmetic")){
            sendMessage(player, msg);
        }
        playerData.sendSavePlayerData();
    }

    public void addAllCosmetics(CommandSender sender, Player target){
        if(!sender.hasPermission("magicosmetics.cosmetics")){
            sendMessage(sender, plugin.prefix + plugin.getMessages().getString("no-permission"));
            return;
        }
        PlayerData playerData = PlayerData.getPlayer(target);
        if(plugin.isPermissions()){
            if(playerData.getCosmeticsPerm().size() == Cosmetic.cosmetics.size()) {
                sendMessage(sender, plugin.prefix + plugin.getMessages().getString("already-all-unlocked"));
                return;
            }
        }else {
            if (playerData.getCosmetics().size() == Cosmetic.cosmetics.size()) {
                sendMessage(sender, plugin.prefix + plugin.getMessages().getString("already-all-unlocked"));
                return;
            }
        }
        for(String id : Cosmetic.cosmetics.keySet()){
            Cosmetic cosmetic = Cosmetic.getCloneCosmetic(id);
            if(cosmetic == null) continue;
            if(playerData.hasCosmeticById(id)) continue;
            playerData.addCosmetic(cosmetic);
        }
        playerData.sendSavePlayerData();
        sendMessage(sender, plugin.prefix + plugin.getMessages().getString("add-all-cosmetic"));
    }

    public void addCosmetic(CommandSender sender, Player target, String cosmeticId){
        if(!sender.hasPermission("magicosmetics.cosmetics")){
            sendMessage(sender, plugin.prefix + plugin.getMessages().getString("no-permission"));
            return;
        }
        Cosmetic cosmetic = Cosmetic.getCloneCosmetic(cosmeticId);
        if(cosmetic == null) {
            sendMessage(sender, plugin.prefix + plugin.getMessages().getString("cosmetic-notfound"));
            return;
        }
        PlayerData playerData = PlayerData.getPlayer(target);
        if(playerData.hasCosmeticById(cosmeticId)){
            sendMessage(sender, plugin.prefix + plugin.getMessages().getString("already-cosmetic"));
            return;
        }
        playerData.addCosmetic(cosmetic);
        playerData.sendSavePlayerData();
        sendMessage(sender, plugin.prefix + plugin.getMessages().getString("add-cosmetic"));
    }

    public void removeCosmetic(CommandSender sender, Player target, String cosmeticId){
        if(!sender.hasPermission("magicosmetics.cosmetics")){
            sendMessage(sender, plugin.prefix + plugin.getMessages().getString("no-permission"));
            return;
        }
        Cosmetic cosmetic = Cosmetic.getCosmetic(cosmeticId);
        if(cosmetic == null) {
            sendMessage(sender, plugin.prefix + plugin.getMessages().getString("cosmetic-notfound"));
            return;
        }
        PlayerData playerData = PlayerData.getPlayer(target);
        if(!playerData.hasCosmeticById(cosmeticId)){
            for(String msg : plugin.getMessages().getStringList("not-have-cosmetic")) {
                sender.sendMessage(msg);
            }
            //sendMessage(sender, plugin.prefix + plugin.getMessages().getString("not-have-cosmetic"));
            return;
        }
        playerData.removeCosmetic(cosmeticId);
        playerData.sendSavePlayerData();
        sendMessage(sender, plugin.prefix + plugin.getMessages().getString("remove-cosmetic"));
    }

    public void removeAllCosmetics(CommandSender sender, Player target){
        if(!sender.hasPermission("magicosmetics.cosmetics")){
            sendMessage(sender, plugin.prefix + plugin.getMessages().getString("no-permission"));
            return;
        }
        PlayerData playerData = PlayerData.getPlayer(target);
        if(plugin.isPermissions()){
            if(playerData.getCosmeticsPerm().size() == 0) {
                sendMessage(sender, plugin.prefix + plugin.getMessages().getString("already-all-locked"));
                return;
            }
        }else {
            if (playerData.getCosmetics().size() == 0) {
                sendMessage(sender, plugin.prefix + plugin.getMessages().getString("already-all-locked"));
                return;
            }
        }
        for(String id : Cosmetic.cosmetics.keySet()){
            Cosmetic cosmetic = Cosmetic.getCloneCosmetic(id);
            if(cosmetic == null) continue;
            if(!playerData.hasCosmeticById(id)) continue;
            playerData.removeCosmetic(cosmetic.getId());
        }
        playerData.sendSavePlayerData();
        sendMessage(sender, plugin.prefix + plugin.getMessages().getString("remove-all-cosmetic"));
    }

    public void giveToken(CommandSender sender, Player target, String tokenId){
        if(!sender.hasPermission("magicosmetics.tokens")){
            sendMessage(sender, plugin.prefix + plugin.getMessages().getString("no-permission"));
            return;
        }
        Token token = Token.getToken(tokenId);
        if(token == null) {
            sendMessage(sender, plugin.prefix + plugin.getMessages().getString("not-exist-token").replace("%id%", tokenId));
            return;
        }
        java.util.HashMap<Integer, ItemStack> leftover = target.getInventory().addItem(token.getItemStack().clone());
        if(!leftover.isEmpty()){
            for(ItemStack item : leftover.values()){
                target.getWorld().dropItemNaturally(target.getLocation(), item);
            }
        }
        sendMessage(sender, plugin.prefix + plugin.getMessages().getString("add-token"));
    }

    public boolean tintItem(ItemStack itemStack, String colorHex){
        if(itemStack.getType() == XMaterial.AIR.parseMaterial() || !Utils.isDyeable(itemStack)){
            return false;
        }
        if(colorHex == null) {
            return false;
        }
        org.bukkit.Color color = Utils.hex2Rgb(colorHex);
        Items item = new Items(itemStack);
        item.coloredItem(color);
        return true;
    }

    public void tintItem(Player player, String colorHex){
        if(!player.hasPermission("magicosmetics.tint")){
            sendMessage(player, plugin.prefix + plugin.getMessages().getString("no-permission"));
            return;
        }
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        if(itemStack.getType() == XMaterial.AIR.parseMaterial() || !Utils.isDyeable(itemStack)){
            sendMessage(player, plugin.prefix + plugin.getMessages().getString("not-tint-item"));
            return;
        }
        if(colorHex == null) return;
        org.bukkit.Color color = Utils.hex2Rgb(colorHex);
        Items item = new Items(itemStack);
        item.coloredItem(color);
        sendMessage(player, plugin.prefix + plugin.getMessages().getString("tint-item").replace("%color%", Utils.ChatColor(colorHex)));
    }

    public void equipCosmetic(Player player, Cosmetic cosmetic, String colorHex){
        PlayerData playerData = PlayerData.getPlayer(player);
        if(!playerData.hasCosmeticById(cosmetic.getId())){
            for(String msg : plugin.getMessages().getStringList("not-have-cosmetic")) {
                player.sendMessage(msg);
            }
            return;
        }
        Cosmetic equip = playerData.getEquip(cosmetic.getCosmeticType());
        if(equip == null){
            CosmeticEquipEvent event = new CosmeticEquipEvent(player, cosmetic);
            VoltrazCosmetics.getInstance().getServer().getPluginManager().callEvent(event);
            if(event.isCancelled()) return;
        }else{
            CosmeticChangeEquipEvent event = new CosmeticChangeEquipEvent(player, equip, cosmetic);
            VoltrazCosmetics.getInstance().getServer().getPluginManager().callEvent(event);
            if(event.isCancelled()) return;
        }
        if(colorHex != null){
            org.bukkit.Color color = Utils.hex2Rgb(colorHex);
            cosmetic.setColor(color);
        }
        playerData.setCosmetic(cosmetic);
        if(plugin.equipMessage) {
            for(String msg : plugin.getMessages().getStringList("use-cosmetic")) {
                player.sendMessage(msg.replace("%id%", cosmetic.getId()).replace("%name%", cosmetic.getName()));
            }
            //sendMessage(player, plugin.prefix + plugin.getMessages().getString("use-cosmetic").replace("%id%", cosmetic.getId()).replace("%name%", cosmetic.getName()));
        }
        playerData.sendSavePlayerData();
        //sendMessage(player, plugin.prefix + plugin.getMessages().getString("not-have-cosmetic"));
    }

    public void equipCosmetic(Player player, String id, String colorHex, boolean force){
        if(plugin.getWorldsBlacklist().contains(player.getWorld().getName())) {
            Utils.sendMessage(player,plugin.prefix + plugin.getMessages().getString("world-blacklist"));
            return;
        }
        PlayerData playerData = PlayerData.getPlayer(player);
        if(force){
            Cosmetic cosmetic = Cosmetic.getCloneCosmetic(id);
            Cosmetic equip = playerData.getEquip(cosmetic.getCosmeticType());
            if(equip == null){
                CosmeticEquipEvent event = new CosmeticEquipEvent(player, cosmetic);
                VoltrazCosmetics.getInstance().getServer().getPluginManager().callEvent(event);
                if(event.isCancelled()) return;
            }else{
                CosmeticChangeEquipEvent event = new CosmeticChangeEquipEvent(player, equip, cosmetic);
                VoltrazCosmetics.getInstance().getServer().getPluginManager().callEvent(event);
                if(event.isCancelled()) return;
            }
            if(colorHex != null){
                org.bukkit.Color color = Utils.hex2Rgb(colorHex);
                cosmetic.setColor(color);
            }
            playerData.setCosmetic(cosmetic);
            if(plugin.equipMessage) {
                sendMessage(player, plugin.prefix + plugin.getMessages().getString("use-cosmetic").replace("%id%", id).replace("%name%", cosmetic.getName()));
            }
        }
        if(!playerData.hasCosmeticById(id) && !force) {
            for(String msg : plugin.getMessages().getStringList("not-have-cosmetic")) {
                player.sendMessage(msg);
            }
            return;
        }
        Cosmetic cosmetic = plugin.isPermissions() || force ? Cosmetic.getCloneCosmetic(id) : playerData.getCosmeticById(id);
        if(cosmetic == null) {
            for(String msg : plugin.getMessages().getStringList("cosmetic-notfound")) {
                player.sendMessage(msg);
            }
            //sendMessage(player, plugin.prefix + plugin.getMessages().getString("cosmetic-notfound"));
            return;
        }
        Cosmetic equip = playerData.getEquip(cosmetic.getCosmeticType());
        if(equip == null){
            CosmeticEquipEvent event = new CosmeticEquipEvent(player, cosmetic);
            VoltrazCosmetics.getInstance().getServer().getPluginManager().callEvent(event);
            if(event.isCancelled()) return;
        }else{
            CosmeticChangeEquipEvent event = new CosmeticChangeEquipEvent(player, equip, cosmetic);
            VoltrazCosmetics.getInstance().getServer().getPluginManager().callEvent(event);
            if(event.isCancelled()) return;
        }
        if(colorHex != null){
            org.bukkit.Color color = Utils.hex2Rgb(colorHex);
            cosmetic.setColor(color);
        }
        playerData.setCosmetic(cosmetic);
        if(plugin.equipMessage) {
            sendMessage(player, plugin.prefix + plugin.getMessages().getString("use-cosmetic").replace("%id%", id).replace("%name%", cosmetic.getName()));
        }
        playerData.sendSavePlayerData();
        //sendMessage(player, plugin.prefix + plugin.getMessages().getString("not-have-cosmetic"));
    }

    public void previewCosmetic(Player player, String id){
        PlayerData playerData = PlayerData.getPlayer(player);
        Cosmetic cosmetic = Cosmetic.getCosmetic(id);
        if(cosmetic == null){
            for(String msg : plugin.getMessages().getStringList("not-have-cosmetic")) {
                player.sendMessage(msg);
            }
            //sendMessage(player, plugin.prefix + plugin.getMessages().getString("not-have-cosmetic"));
            return;
        }
        playerData.setPreviewCosmetic(cosmetic);
    }

    public void previewCosmetic(Player player, Cosmetic cosmetic){
        PlayerData playerData = PlayerData.getPlayer(player);
        if(cosmetic == null){
            for(String msg : plugin.getMessages().getStringList("not-have-cosmetic")) {
                player.sendMessage(msg);
            }
            //sendMessage(player, plugin.prefix + plugin.getMessages().getString("not-have-cosmetic"));
            return;
        }
        playerData.setPreviewCosmetic(cosmetic);
    }

    public void openMenu(Player player, String id){
        if(plugin.getWorldsBlacklist().contains(player.getWorld().getName())) {
            Utils.sendMessage(player,plugin.prefix + plugin.getMessages().getString("world-blacklist"));
            return;
        }
        PlayerData playerData = PlayerData.getPlayer(player);
        Menu menu = Menu.inventories.get(id);
        if(menu == null){
            sendMessage(player, plugin.prefix + plugin.getMessages().getString("not-exist-menu").replace("%id%", id));
            return;
        }
        if(!menu.getPermission().isEmpty()){
            if(!player.hasPermission(menu.getPermission())){
                sendMessage(player, plugin.prefix + plugin.getMessages().getString("no-permission"));
                return;
            }
        }
        PaginatedMenu paginatedMenu = null;
        switch (menu.getContentMenu().getInventoryType()){
            case HAT:
                paginatedMenu = new HatMenu(playerData, menu);
                break;
            case BAG:
                paginatedMenu = new BagMenu(playerData, menu);
                break;
            case WALKING_STICK:
                paginatedMenu = new WStickMenu(playerData, menu);
                break;
            case BALLOON:
                paginatedMenu = new BalloonMenu(playerData, menu);
                break;
            case SPRAY:
                paginatedMenu = new SprayMenu(playerData, menu);
                break;
            case FREE:
                new FreeMenu(playerData, menu).open();
                break;
            case COLORED:
            case FREE_COLORED:
                openFreeMenuColor(player, id, Color.getColor("color1"));
                break;
            case TOKEN:
                ((TokenMenu)menu).getClone(playerData).open();
                break;
            case ITEM_SKIN:
                break;
        }
        if(paginatedMenu == null) return;
        paginatedMenu.setShowAllCosmeticsInMenu(plugin.isShowAllCosmeticsInMenu());
        paginatedMenu.open();
    }

    public void openMenuColor(Player player, String id, Color color, Cosmetic cosmetic){
        PlayerData playerData = PlayerData.getPlayer(player);
        Menu menu = Menu.inventories.get(id);
        if(menu == null){
            sendMessage(player, plugin.prefix + plugin.getMessages().getString("not-exist-menu").replace("%id%", id));
            return;
        }
        if(!(menu instanceof ColoredMenu)) return;
        ColoredMenu coloredMenu = (ColoredMenu) menu;
        switch (menu.getContentMenu().getInventoryType()){
            case HAT:
            case BAG:
            case WALKING_STICK:
            case FREE:
            case TOKEN:
            case BALLOON:
            case SPRAY:
            case FREE_COLORED:
            case ITEM_SKIN:
                break;
            case COLORED:
                coloredMenu.getClone(playerData, color, cosmetic).open();
                break;
        }
    }

    public void openFreeMenuColor(Player player, String id, Color color){
        PlayerData playerData = PlayerData.getPlayer(player);
        Menu menu = Menu.inventories.get(id);
        if(menu == null){
            sendMessage(player, plugin.prefix + plugin.getMessages().getString("not-exist-menu").replace("%id%", id));
            return;
        }
        FreeColoredMenu freeColoredMenu = (FreeColoredMenu) menu;
        switch (menu.getContentMenu().getInventoryType()){
            case HAT:
            case BAG:
            case WALKING_STICK:
            case FREE:
            case TOKEN:
            case BALLOON:
            case COLORED:
            case SPRAY:
            case ITEM_SKIN:
                break;
            case FREE_COLORED:
                freeColoredMenu.getClone(playerData, color).open();
                break;
        }
    }

    public void unSetCosmetic(Player player, CosmeticType cosmeticType){
        PlayerData playerData = PlayerData.getPlayer(player);
        Cosmetic equip = playerData.getEquip(cosmeticType);
        if(equip == null) return;
        CosmeticUnEquipEvent event = new CosmeticUnEquipEvent(player, equip);
        VoltrazCosmetics.getInstance().getServer().getPluginManager().callEvent(event);
        if(event.isCancelled()) return;
        playerData.removePreviewEquip(equip.getId());
        playerData.removeEquip(equip.getId());
        playerData.sendSavePlayerData();
    }

    public void unSetCosmetic(Player player, String cosmeticId){
        PlayerData playerData = PlayerData.getPlayer(player);
        Cosmetic equip = playerData.getEquip(cosmeticId);
        if(equip == null) return;
        CosmeticUnEquipEvent event = new CosmeticUnEquipEvent(player, equip);
        VoltrazCosmetics.getInstance().getServer().getPluginManager().callEvent(event);
        if(event.isCancelled()) return;
        playerData.removePreviewEquip(cosmeticId);
        playerData.removeEquip(cosmeticId);
        playerData.sendSavePlayerData();
    }

    public boolean unUseCosmetic(Player player, String cosmeticId){
        PlayerData playerData = PlayerData.getPlayer(player);
        Token token = Token.getTokenByCosmetic(cosmeticId);
        if(token == null) return false;
        if(!token.isExchangeable()) {
            return false;
        }
        if(!playerData.hasCosmeticById(cosmeticId)) return false;
        int freeSlot = playerData.getFreeSlotInventory();
        if(freeSlot == -1) return false;
        playerData.removeCosmetic(cosmeticId);
        if(playerData.isZone()) {
            playerData.getInventory().put(freeSlot, token.getItemStack().clone());
        }else{
            java.util.HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(token.getItemStack().clone());
            if(!leftover.isEmpty()){
                for(ItemStack item : leftover.values()){
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }
            }
        }
        for(String msg : plugin.getMessages().getStringList("change-cosmetic-to-token")){
            sendMessage(player, msg);
        }
        playerData.sendSavePlayerData();
        return true;
    }

    public void unEquipAll(CommandSender sender, Player player){
        if(!sender.hasPermission("magicosmetics.equip")){
            sendMessage(player, plugin.prefix + plugin.getMessages().getString("no-permission"));
            return;
        }
        PlayerData playerData = PlayerData.getPlayer(player);
        for(Cosmetic cosmetic : playerData.cosmeticsInUse()){
            if(cosmetic == null) continue;
            CosmeticUnEquipEvent event = new CosmeticUnEquipEvent(player, cosmetic);
            VoltrazCosmetics.getInstance().getServer().getPluginManager().callEvent(event);
            if(event.isCancelled()) continue;
            playerData.removePreviewEquip(cosmetic.getId());
            playerData.removeEquip(cosmetic.getId());
        }
        playerData.sendSavePlayerData();
    }

    public void unEquipAll(Player player){
        if(!player.hasPermission("magicosmetics.equip")){
            sendMessage(player, plugin.prefix + plugin.getMessages().getString("no-permission"));
            return;
        }
        PlayerData playerData = PlayerData.getPlayer(player);
        for(Cosmetic cosmetic : playerData.cosmeticsInUse()){
            if(cosmetic == null) continue;
            CosmeticUnEquipEvent event = new CosmeticUnEquipEvent(player, cosmetic);
            VoltrazCosmetics.getInstance().getServer().getPluginManager().callEvent(event);
            if(event.isCancelled()) continue;
            playerData.removePreviewEquip(cosmetic.getId());
            playerData.removeEquip(cosmetic.getId());
        }
        playerData.sendSavePlayerData();
    }

    public void hideSelfCosmetic(Player player, CosmeticType cosmeticType){
        PlayerData playerData = PlayerData.getPlayer(player);
        if(cosmeticType != CosmeticType.BAG) return;
        Cosmetic equip = playerData.getEquip(cosmeticType);
        if(!(equip instanceof Bag)) return;
        Bag bag = (Bag) equip;
        bag.hideSelf(true);
        if(bag.isHide()){
            sendMessage(player, plugin.prefix + plugin.getMessages().getString("hide-backpack"));
            return;
        }
        sendMessage(player, plugin.prefix + plugin.getMessages().getString("show-backpack"));
    }

    public boolean hasPermission(CommandSender sender, String permission){
        return sender.hasPermission("magicosmetics.*") || sender.hasPermission(permission);
    }

}