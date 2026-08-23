package xyz.voltraz.cosmetics.commands;

import xyz.voltraz.cosmetics.VoltrazCosmetics;
import xyz.voltraz.cosmetics.api.Cosmetic;
import xyz.voltraz.cosmetics.api.CosmeticType;
import xyz.voltraz.cosmetics.cache.*;
import xyz.voltraz.cosmetics.cache.inventories.Menu;
import xyz.voltraz.cosmetics.files.FileCreator;
import xyz.voltraz.cosmetics.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.*;
import java.util.Collections;

import java.util.ArrayList;
import java.util.List;

public class Command implements CommandExecutor, TabCompleter {

    private final VoltrazCosmetics plugin = VoltrazCosmetics.getInstance();

    public static boolean hasPerm(CommandSender sender, String perm) {
        if (sender == null) return false;
        if (sender.isOp()) return true;
        return sender.hasPermission("voltrazcosmetics." + perm) 
            || sender.hasPermission("cosmetics." + perm) 
            || sender.hasPermission("magicosmetics." + perm)
            || sender.hasPermission("voltrazcosmetics.admin")
            || sender.hasPermission("cosmetics.admin")
            || sender.hasPermission("magicosmetics.admin");
    }
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        FileCreator messages = plugin.getMessages();
        if(sender instanceof ConsoleCommandSender){
            if(args.length >= 1){
                Player target;
                switch (args[0].toLowerCase()){
                    case "addall":
                        if(args.length < 2){
                            Utils.sendMessage(sender,plugin.prefix + messages.getString("commands.add-all-usage"));
                            return true;
                        }
                        target = Bukkit.getPlayer(args[1]);
                        if(target == null){
                            Utils.sendMessage(sender,plugin.prefix + messages.getString("offline-player"));
                            return true;
                        }
                        if(plugin.getWorldsBlacklist().contains(target.getWorld().getName())) {
                            Utils.sendMessage(sender,plugin.prefix + plugin.getMessages().getString("world-blacklist"));
                            return true;
                        }
                        plugin.getCosmeticsManager().addAllCosmetics(sender, target);
                        return true;
                    case "add":
                        //cosmetics add <player> <id>
                        if(args.length < 3){
                            Utils.sendMessage(sender,plugin.prefix + messages.getString("commands.add-usage"));
                            return true;
                        }
                        target = Bukkit.getPlayer(args[1]);
                        if(target == null){
                            Utils.sendMessage(sender,plugin.prefix + messages.getString("offline-player"));
                            return true;
                        }
                        if(plugin.getWorldsBlacklist().contains(target.getWorld().getName())) {
                            Utils.sendMessage(sender,plugin.prefix + plugin.getMessages().getString("world-blacklist"));
                            return true;
                        }
                        plugin.getCosmeticsManager().addCosmetic(sender, target, args[2].trim());
                        return true;
                    case "remove":
                        //cosmetics remove <player> <id>
                        if(args.length < 3){
                            Utils.sendMessage(sender,plugin.prefix + messages.getString("commands.remove-usage"));
                            return true;
                        }
                        target = Bukkit.getPlayer(args[1]);
                        if(target == null){
                            Utils.sendMessage(sender,plugin.prefix + messages.getString("offline-player"));
                            return true;
                        }
                        plugin.getCosmeticsManager().removeCosmetic(sender, target, args[2].trim());
                        return true;
                    case "removeall":
                        if(args.length < 2){
                            Utils.sendMessage(sender,plugin.prefix + messages.getString("commands.remove-all-usage"));
                            return true;
                        }
                        target = Bukkit.getPlayer(args[1]);
                        if(target == null){
                            Utils.sendMessage(sender,plugin.prefix + messages.getString("offline-player"));
                            return true;
                        }
                        plugin.getCosmeticsManager().removeAllCosmetics(sender, target);
                        return true;
                    case "reload":
                        plugin.getCosmeticsManager().reload(sender);
                        return true;
                    case "toggle":
                        if(args.length < 2){
                            return true;
                        }
                        target = Bukkit.getPlayer(args[1]);
                        if(target == null){
                            Utils.sendMessage(sender,plugin.prefix + messages.getString("offline-player"));
                            return true;
                        }
                        PlayerData playerData = PlayerData.getPlayer(target);
                        playerData.toggleHiddeCosmetics();
                        return true;
                    case "equip":
                        //cosmetics equip <player> <id> <color> <force>
                        if(args.length < 3){
                            Utils.sendMessage(sender,plugin.prefix + messages.getString("commands.equip-usage"));
                            return true;
                        }
                        target = Bukkit.getPlayer(args[1]);
                        if(target == null){
                            Utils.sendMessage(sender,plugin.prefix + messages.getString("offline-player"));
                            return true;
                        }
                        if(plugin.getWorldsBlacklist().contains(target.getWorld().getName())) {
                            Utils.sendMessage(sender,plugin.prefix + plugin.getMessages().getString("world-blacklist"));
                            return true;
                        }
                        String cosmeticId = args[2].trim();
                        if(args.length == 4) {
                            String colorArg = args[3].trim();
                            if(!colorArg.startsWith("#")) {
                                plugin.getCosmeticsManager().equipCosmetic(target, cosmeticId, null, false);
                            } else {
                                plugin.getCosmeticsManager().equipCosmetic(target, cosmeticId, colorArg, false);
                            }
                            return true;
                        }
                        if(args.length == 5) {
                            String colorArg = args[3].trim();
                            plugin.getCosmeticsManager().equipCosmetic(target, cosmeticId, colorArg.startsWith("#") ? colorArg : null, Boolean.parseBoolean(args[4].trim()));
                            return true;
                        }
                        plugin.getCosmeticsManager().equipCosmetic(target, cosmeticId, null, false);
                        return true;
                    case "unequip":
                        // /cosmetics unequip <player> <id>
                        if(args.length < 3){
                            return true;
                        }
                        target = Bukkit.getPlayer(args[1]);
                        if(target == null){
                            Utils.sendMessage(sender,plugin.prefix + messages.getString("offline-player"));
                            return true;
                        }
                        if(args[2].equalsIgnoreCase("all")){
                            plugin.getCosmeticsManager().unEquipAll(sender, target);
                            return true;
                        }
                        plugin.getCosmeticsManager().unSetCosmetic(target, args[2]);
                        return true;
                    case "open":
                        //cosmetics open <menu-id> <player>
                        if(args.length < 3){
                            Utils.sendMessage(sender,plugin.prefix + messages.getString("commands.menu-usage"));
                            return true;
                        }
                        target = Bukkit.getPlayer(args[2]);
                        if(target == null){
                            Utils.sendMessage(sender,plugin.prefix + messages.getString("offline-player"));
                            return true;
                        }
                        if(plugin.getWorldsBlacklist().contains(target.getWorld().getName())) {
                            Utils.sendMessage(sender,plugin.prefix + plugin.getMessages().getString("world-blacklist"));
                            return true;
                        }
                        plugin.getCosmeticsManager().openMenu(target, args[1].trim());
                        return true;
                    case "token":
                        //cosmetics token give <player> <name>
                        if(args.length < 3){
                            Utils.sendMessage(sender,plugin.prefix + plugin.getMessages().getString("commands.token-usage"));
                            return true;
                        }
                        if(args[1].equalsIgnoreCase("give")){
                            target = Bukkit.getPlayer(args[2]);
                            if(target == null){
                                Utils.sendMessage(sender,plugin.prefix + messages.getString("offline-player"));
                                return true;
                            }
                            if(plugin.getWorldsBlacklist().contains(target.getWorld().getName())) {
                                Utils.sendMessage(sender,plugin.prefix + plugin.getMessages().getString("world-blacklist"));
                                return true;
                            }
                            plugin.getCosmeticsManager().giveToken(sender, target, args[3].trim());
                            return true;
                        }
                        return true;
                    default:
                        Utils.sendMessage(sender,plugin.prefix + plugin.getMessages().getString("commands.not-found"));
                        return true;
                }
            }
            return true;
        }
        if(sender instanceof Player){
            Player player = (Player) sender;
            if(plugin.getWorldsBlacklist().contains(player.getWorld().getName())) {
                Utils.sendMessage(player,plugin.prefix + plugin.getMessages().getString("world-blacklist"));
                return true;
            }
            Player target;
            if(args.length >= 1){
                switch (args[0].toLowerCase()){
                    case "test":
                        Entity entity = player.getWorld().spawnEntity(player.getLocation(), EntityType.ARMOR_STAND);
                        player.addPassenger(entity);
                        return true;
                    case "unlock":
                        if(!hasPerm(player, "admin")){
                            Utils.sendMessage(player, plugin.prefix + messages.getString("no-permission"));
                            return true;
                        }
                        if(args.length < 2){
                            return true;
                        }
                        Player p = Bukkit.getPlayer(args[1]);
                        if(p == null) return true;
                        PlayerData pData = PlayerData.getPlayer(p);
                        pData.setZone(false);
                        return true;
                    case "addall":
                        if(!hasPerm(player, "cosmetics") && !hasPerm(player, "admin")){
                            Utils.sendMessage(player, plugin.prefix + messages.getString("no-permission"));
                            return true;
                        }
                        if(args.length < 2){
                            Utils.sendMessage(player,plugin.prefix + messages.getString("commands.add-all-usage"));
                            return true;
                        }
                        target = Bukkit.getPlayer(args[1]);
                        if(target == null){
                            Utils.sendMessage(player,plugin.prefix + messages.getString("offline-player"));
                            return true;
                        }
                        if(plugin.getWorldsBlacklist().contains(target.getWorld().getName())) {
                            Utils.sendMessage(player,plugin.prefix + plugin.getMessages().getString("world-blacklist"));
                            return true;
                        }
                        plugin.getCosmeticsManager().addAllCosmetics(player, target);
                        return true;
                    case "add":
                        //cosmetics add <player> <id>
                        if(!hasPerm(player, "cosmetics") && !hasPerm(player, "admin")){
                            Utils.sendMessage(player, plugin.prefix + messages.getString("no-permission"));
                            return true;
                        }
                        if(args.length < 3){
                            Utils.sendMessage(player,plugin.prefix + messages.getString("commands.add-usage"));
                            return true;
                        }
                        target = Bukkit.getPlayer(args[1]);
                        if(target == null){
                            Utils.sendMessage(player,plugin.prefix + messages.getString("offline-player"));
                            return true;
                        }
                        if(plugin.getWorldsBlacklist().contains(target.getWorld().getName())) {
                            Utils.sendMessage(player,plugin.prefix + plugin.getMessages().getString("world-blacklist"));
                            return true;
                        }
                        plugin.getCosmeticsManager().addCosmetic(player, target, args[2].trim());
                        return true;
                    case "remove":
                        //cosmetics remove <player> <id>
                        if(!hasPerm(player, "cosmetics") && !hasPerm(player, "admin")){
                            Utils.sendMessage(player, plugin.prefix + messages.getString("no-permission"));
                            return true;
                        }
                        if(args.length < 3){
                            Utils.sendMessage(player,plugin.prefix + messages.getString("commands.remove-usage"));
                            return true;
                        }
                        target = Bukkit.getPlayer(args[1]);
                        if(target == null){
                            Utils.sendMessage(player,plugin.prefix + messages.getString("offline-player"));
                            return true;
                        }
                        plugin.getCosmeticsManager().removeCosmetic(player, target, args[2].trim());
                        return true;
                    case "removeall":
                        if(!hasPerm(player, "cosmetics") && !hasPerm(player, "admin")){
                            Utils.sendMessage(player, plugin.prefix + messages.getString("no-permission"));
                            return true;
                        }
                        if(args.length < 2){
                            Utils.sendMessage(player,plugin.prefix + messages.getString("commands.remove-all-usage"));
                            return true;
                        }
                        target = Bukkit.getPlayer(args[1]);
                        if(target == null){
                            Utils.sendMessage(player,plugin.prefix + messages.getString("offline-player"));
                            return true;
                        }
                        plugin.getCosmeticsManager().removeAllCosmetics(player, target);
                        return true;
                    case "reload":
                        if(!hasPerm(player, "reload") && !hasPerm(player, "admin")){
                            Utils.sendMessage(player, plugin.prefix + messages.getString("no-permission"));
                            return true;
                        }
                        plugin.getCosmeticsManager().reload(sender);
                        return true;
                    case "use":
                        //cosmetics use <id> <color>
                        if(args.length < 2){
                            Utils.sendMessage(player,plugin.prefix + messages.getString("commands.use-usage"));
                            return true;
                        }
                        if(args.length == 3) {
                            plugin.getCosmeticsManager().equipCosmetic(player, args[1], args[2], false);
                            return true;
                        }
                        plugin.getCosmeticsManager().equipCosmetic(player, args[1], null, false);
                        return true;
                    case "preview":
                        if(args.length < 2){
                            Utils.sendMessage(player,plugin.prefix + messages.getString("commands.use-usage"));
                            return true;
                        }
                        plugin.getCosmeticsManager().previewCosmetic(player, args[1]);
                        return true;
                    case "unuse":
                        if(args.length < 2){
                            Utils.sendMessage(player,plugin.prefix + messages.getString("commands.use-usage"));
                            return true;
                        }
                        plugin.getCosmeticsManager().unUseCosmetic(player, args[1]);
                        return true;
                    case "unset":
                        if(args.length < 2){
                            Utils.sendMessage(player,plugin.prefix + messages.getString("commands.use-usage"));
                            return true;
                        }
                        plugin.getCosmeticsManager().unSetCosmetic(player, args[1]);
                        return true;
                    case "unequip":
                        if(args.length < 2){
                            return true;
                        }
                        if(args[1].equalsIgnoreCase("all")){
                            plugin.getCosmeticsManager().unEquipAll(player);
                            return true;
                        }
                        plugin.getCosmeticsManager().unSetCosmetic(player, args[1]);
                        return true;
                    case "open":
                        //cosmetics open <menu-id>
                        if(!hasPerm(player, "menus") && !hasPerm(player, "admin")){
                            Utils.sendMessage(player, plugin.prefix + messages.getString("no-permission"));
                            return true;
                        }
                        if(args.length < 2){
                            Utils.sendMessage(player,plugin.prefix + messages.getString("commands.menu-usage"));
                            return true;
                        }
                        plugin.getCosmeticsManager().openMenu(player, args[1].trim());
                        return true;
                    case "spec":
                        if(!hasPerm(player, "admin")){
                            Utils.sendMessage(player, plugin.prefix + messages.getString("no-permission"));
                            return true;
                        }
                        plugin.getVersion().setSpectator(player);
                        return true;
                    case "spawn":
                        if(!hasPerm(player, "admin")){
                            Utils.sendMessage(player, plugin.prefix + messages.getString("no-permission"));
                            return true;
                        }
                        if(plugin.getVersion().getNPC(player) == null){
                            plugin.getVersion().createNPC(player);
                            return true;
                        }
                        plugin.getVersion().removeNPC(player);
                        return true;
                    case "hide":
                        if(!hasPerm(player, "hide") && !hasPerm(player, "admin")){
                            Utils.sendMessage(player, plugin.prefix + messages.getString("no-permission"));
                            return true;
                        }
                        plugin.getCosmeticsManager().hideSelfCosmetic(player, CosmeticType.BAG);
                        return true;
                    case "toggle":
                        if(!hasPerm(player, "toggle") && !hasPerm(player, "admin")){
                            Utils.sendMessage(player, plugin.prefix + messages.getString("no-permission"));
                            return true;
                        }
                        PlayerData playerData = PlayerData.getPlayer(player);
                        playerData.toggleHiddeCosmetics();
                        return true;
                    case "zones":
                        //cosmetics zones add <name>
                        if(!hasPerm(player, "zones") && !hasPerm(player, "admin")){
                            Utils.sendMessage(player, plugin.prefix + messages.getString("no-permission"));
                            return true;
                        }
                        if(args.length < 2){
                            for(String msg : plugin.getMessages().getStringList("commands.zones-usage")){
                                Utils.sendMessage(player,msg);
                            }
                            return true;
                        }
                        if(args[1].equalsIgnoreCase("add")){
                            if(args.length < 3){
                                for(String msg : plugin.getMessages().getStringList("commands.zones-usage")){
                                    Utils.sendMessage(player,msg);
                                }
                                return true;
                            }
                            plugin.getZonesManager().addZone(player, args[2]);
                            return true;
                        }
                        if(args[1].equalsIgnoreCase("remove")){
                            if(args.length < 3){
                                for(String msg : plugin.getMessages().getStringList("commands.zones-usage")){
                                    Utils.sendMessage(player,msg);
                                }
                                return true;
                            }
                            plugin.getZonesManager().removeZone(player, args[2]);
                            return true;
                        }
                        if(args[1].equalsIgnoreCase("setnpc")){
                            if(args.length < 3){
                                for(String msg : plugin.getMessages().getStringList("commands.zones-usage")){
                                    Utils.sendMessage(player,msg);
                                }
                                return true;
                            }
                            plugin.getZonesManager().setZoneNPC(player, args[2]);
                            return true;
                        }
                        if(args[1].equalsIgnoreCase("setballoon")){
                            if(args.length < 3){
                                for(String msg : plugin.getMessages().getStringList("commands.zones-usage")){
                                    Utils.sendMessage(player,msg);
                                }
                                return true;
                            }
                            plugin.getZonesManager().setBalloonNPC(player, args[2]);
                            return true;
                        }
                        if(args[1].equalsIgnoreCase("setspray")){
                            if(args.length < 3){
                                for(String msg : plugin.getMessages().getStringList("commands.zones-usage")){
                                    Utils.sendMessage(player,msg);
                                }
                                return true;
                            }
                            plugin.getZonesManager().setSpray(player, args[2]);
                            return true;
                        }
                        if(args[1].equalsIgnoreCase("setenter")){
                            if(args.length < 3){
                                for(String msg : plugin.getMessages().getStringList("commands.zones-usage")){
                                    Utils.sendMessage(player,msg);
                                }
                                return true;
                            }
                            plugin.getZonesManager().setZoneEnter(player, args[2]);
                            return true;
                        }
                        if(args[1].equalsIgnoreCase("setexit")){
                            if(args.length < 3){
                                for(String msg : plugin.getMessages().getStringList("commands.zones-usage")){
                                    Utils.sendMessage(player,msg);
                                }
                                return true;
                            }
                            plugin.getZonesManager().setZoneExit(player, args[2]);
                            return true;
                        }
                        if(args[1].equalsIgnoreCase("givecorns")){
                            if(args.length < 3){
                                for(String msg : plugin.getMessages().getStringList("commands.zones-usage")){
                                    Utils.sendMessage(player,msg);
                                }
                                return true;
                            }
                            plugin.getZonesManager().giveCorn(player, args[2]);
                            return true;
                        }
                        if(args[1].equalsIgnoreCase("enable")){
                            if(args.length < 3){
                                for(String msg : plugin.getMessages().getStringList("commands.zones-usage")){
                                    Utils.sendMessage(player,msg);
                                }
                                return true;
                            }
                            plugin.getZonesManager().enableZone(player, args[2]);
                            return true;
                        }
                        if(args[1].equalsIgnoreCase("disable")){
                            if(args.length < 3){
                                for(String msg : plugin.getMessages().getStringList("commands.zones-usage")){
                                    Utils.sendMessage(player,msg);
                                }
                                return true;
                            }
                            plugin.getZonesManager().disableZone(player, args[2]);
                            return true;
                        }
                        if(args[1].equalsIgnoreCase("save")){
                            if(args.length < 3){
                                for(String msg : plugin.getMessages().getStringList("commands.zones-usage")){
                                    Utils.sendMessage(player,msg);
                                }
                                return true;
                            }
                            plugin.getZonesManager().saveZone(player, args[2]);
                            return true;
                        }
                        return true;
                    case "token":
                        //cosmetics token give <player> <name>
                        if(!hasPerm(player, "tokens") && !hasPerm(player, "admin")){
                            Utils.sendMessage(player, plugin.prefix + messages.getString("no-permission"));
                            return true;
                        }
                        if(args.length < 4){
                            Utils.sendMessage(player,plugin.prefix + plugin.getMessages().getString("commands.token-usage"));
                            return true;
                        }
                        if(args[1].equalsIgnoreCase("give")){
                            target = Bukkit.getPlayer(args[2]);
                            if(target == null){
                                Utils.sendMessage(sender,plugin.prefix + messages.getString("offline-player"));
                                return true;
                            }
                            plugin.getCosmeticsManager().giveToken(player, target, args[3]);
                            return true;
                        }
                        return true;
                    case "check":
                        if(!hasPerm(player, "admin")){
                            Utils.sendMessage(player, plugin.prefix + messages.getString("no-permission"));
                            return true;
                        }
                        plugin.getCosmeticsManager().sendCheck(player);
                        return true;
                    case "npc":
                        if(!hasPerm(player, "cosmetics") && !hasPerm(player, "admin")){
                            Utils.sendMessage(player, plugin.prefix + messages.getString("no-permission"));
                            return true;
                        }
                        if(!plugin.isCitizens()){
                            Utils.sendMessage(player, plugin.prefix + "&cCitizens is not installed!");
                            return true;
                        }
                        if(args.length == 2 && args[1].equalsIgnoreCase("save")){
                            plugin.getNPCsLoader().save();
                            return true;
                        }
                        if(args.length < 3){
                            Utils.sendMessage(player,plugin.prefix + plugin.getMessages().getString("commands.npc-usage"));
                            return true;
                        }
                        try{
                            plugin.getCitizens().equipCosmetic(player, args[1], args[2], args[3]);
                        }catch (ArrayIndexOutOfBoundsException exception){
                            plugin.getCitizens().equipCosmetic(player, args[1], args[2], null);
                        }
                        return true;
                    case "tint":
                        //cosmetics tint <color>
                        if(!hasPerm(player, "tint") && !hasPerm(player, "admin")){
                            Utils.sendMessage(player, plugin.prefix + messages.getString("no-permission"));
                            return true;
                        }
                        if(args.length < 2){
                            Utils.sendMessage(player,plugin.prefix + plugin.getMessages().getString("commands.tint-usage"));
                            return true;
                        }
                        return true;
                    default:
                        Utils.sendMessage(player,plugin.prefix + plugin.getMessages().getString("commands.not-found"));
                        return true;
                }
            }
            if(hasPerm(player, "cosmetics.use") || hasPerm(player, "use") || hasPerm(player, "admin")) {
                plugin.getCosmeticsManager().openMenu(player, plugin.getMainMenu());
                if(plugin.getOnExecuteCosmetics().isEmpty()) return true;
                player.performCommand(plugin.getOnExecuteCosmetics());
            }
            return true;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        List<String> arguments = new ArrayList<>();
        if(hasPerm(sender, "cosmetics") || hasPerm(sender, "admin")) {
            arguments.add("add");
            arguments.add("remove");
            arguments.add("addAll");
            arguments.add("removeAll");
            if(plugin.isCitizens()) {
                arguments.add("npc");
            }
        }
        if(hasPerm(sender, "menus") || hasPerm(sender, "admin")) {
            arguments.add("open");
        }
        if(hasPerm(sender, "zones") || hasPerm(sender, "admin")) {
            arguments.add("zones");
        }
        if(hasPerm(sender, "tokens") || hasPerm(sender, "admin")) {
            arguments.add("token");
        }
        if(hasPerm(sender, "reload") || hasPerm(sender, "admin")) {
            arguments.add("reload");
        }
        if(hasPerm(sender, "hide") || hasPerm(sender, "admin")) {
            arguments.add("hide");
        }
        if(hasPerm(sender, "toggle") || hasPerm(sender, "admin")){
            arguments.add("toggle");
        }
        if(hasPerm(sender, "equip") || hasPerm(sender, "use") || hasPerm(sender, "admin")){
            arguments.add("use");
            arguments.add("equip");
            arguments.add("unequip");
        }
        if(hasPerm(sender, "tint") || hasPerm(sender, "admin")){
            arguments.add("tint");
        }
        if(arguments.isEmpty()) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        switch (args.length){
            case 1:
                for(String a : arguments){
                    if(a.toLowerCase().startsWith(args[0].toLowerCase()))
                        result.add(a);
                }
                return result;
            case 2:
                switch (args[0].toLowerCase()){
                    case "hide":
                    case "toggle":
                    case "add":
                    case "addall":
                    case "remove":
                    case "removeall":
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                                result.add(p.getName());
                            }
                        }
                        return result;
                    case "npc":
                        if(!plugin.isCitizens()) return Collections.emptyList();
                        result.add("save");
                        result.addAll(plugin.getCitizens().getNPCs());
                        break;
                    case "unequip":
                    case "use":
                    case "equip":
                        if(!hasPerm(sender, "equip") && !hasPerm(sender, "use") && !hasPerm(sender, "admin")) return Collections.emptyList();
                        result.add("all");
                        result.addAll(Cosmetic.cosmetics.keySet());
                        break;
                    case "open":
                        if(!hasPerm(sender, "menus") && !hasPerm(sender, "admin")) return Collections.emptyList();
                        result.addAll(Menu.inventories.keySet());
                        break;
                    case "zones":
                        if(!hasPerm(sender, "zones") && !hasPerm(sender, "admin")) return Collections.emptyList();
                        result.add("add");
                        result.add("remove");
                        result.add("setNPC");
                        result.add("setBalloon");
                        result.add("setSpray");
                        result.add("setEnter");
                        result.add("setExit");
                        result.add("giveCorns");
                        result.add("enable");
                        result.add("disable");
                        result.add("save");
                        break;
                    case "token":
                        if(!hasPerm(sender, "tokens") && !hasPerm(sender, "admin")) return Collections.emptyList();
                        result.add("give");
                        break;
                    case "tint":
                        if(!hasPerm(sender, "tint") && !hasPerm(sender, "admin")) return Collections.emptyList();
                        result.add("#FFFFFF");
                        result.add("#FF0000");
                        result.add("#00FF00");
                        result.add("#0000FF");
                        result.add("#FFFF00");
                        break;
                }
                List<String> filtered2 = new ArrayList<>();
                for(String s : result){
                    if(s.toLowerCase().startsWith(args[1].toLowerCase())) filtered2.add(s);
                }
                return filtered2;
            case 3:
                switch (args[0].toLowerCase()){
                    case "add":
                    case "remove":
                        if(!hasPerm(sender, "cosmetics") && !hasPerm(sender, "admin")) return Collections.emptyList();
                        result.addAll(Cosmetic.cosmetics.keySet());
                        break;
                    case "npc":
                        if(!hasPerm(sender, "cosmetics") && !hasPerm(sender, "admin")) return Collections.emptyList();
                        result.addAll(Cosmetic.cosmetics.keySet());
                        break;
                    case "use":
                    case "equip":
                        if(!hasPerm(sender, "equip") && !hasPerm(sender, "use") && !hasPerm(sender, "admin")) return Collections.emptyList();
                        result.add("#FFFFFF");
                        result.add("null");
                        break;
                    case "zones":
                        if(!hasPerm(sender, "zones") && !hasPerm(sender, "admin")) return Collections.emptyList();
                        if(!args[1].equalsIgnoreCase("add")) {
                            result.addAll(Zone.zones.keySet());
                        }
                        break;
                    case "token":
                        if(args[1].equalsIgnoreCase("give")){
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                if (p.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                                    result.add(p.getName());
                                }
                            }
                            return result;
                        }
                        break;
                }
                List<String> filtered3 = new ArrayList<>();
                for(String s : result){
                    if(s.toLowerCase().startsWith(args[2].toLowerCase())) filtered3.add(s);
                }
                return filtered3;
            case 4:
                if(args[0].equalsIgnoreCase("token") && args[1].equalsIgnoreCase("give")){
                    if(!hasPerm(sender, "tokens") && !hasPerm(sender, "admin")) return Collections.emptyList();
                    for(String t : Token.tokens.keySet()){
                        if(t.toLowerCase().startsWith(args[3].toLowerCase())) result.add(t);
                    }
                    return result;
                }
                if(args[0].equalsIgnoreCase("npc")){
                    if(!plugin.isCitizens()) return Collections.emptyList();
                    if(!hasPerm(sender, "cosmetics") && !hasPerm(sender, "admin")) return Collections.emptyList();
                    result.add("#FFFFFF");
                    return result;
                }
                break;
        }
        return Collections.emptyList();
    }
}
