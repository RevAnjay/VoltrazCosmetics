package com.francobm.magicosmetics.provider;

import com.francobm.magicosmetics.MagicCosmetics;
import com.francobm.magicosmetics.cache.PlayerData;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class WorldGuard implements Listener {
    private final MagicCosmetics plugin;
    private StateFlag customFlag;
    private RegionQuery cachedQuery;

    public WorldGuard(MagicCosmetics plugin){
        registerFlag();
        this.plugin = plugin;
    }

    public void registerFlag(){
        FlagRegistry registry = com.sk89q.worldguard.WorldGuard.getInstance().getFlagRegistry();
        try {
            // create a flag with the name "my-custom-flag", defaulting to true
            StateFlag flag = new StateFlag("cosmetics", true);
            registry.register(flag);
            customFlag = flag; // only set our field if there was no error
        } catch (FlagConflictException e) {
            // some other plugin registered a flag by the same name already.
            // you can use the existing flag, but this may cause conflicts - be sure to check type
            Flag<?> existing = registry.get("cosmetics");
            if (existing instanceof StateFlag) {
                customFlag = (StateFlag) existing;
            } else {
                // types don't match - this is bad news! some other plugin conflicts with you
                // hopefully this never actually happens
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event){
        // Skip if player hasn't moved to a new block (regions are block-based)
        org.bukkit.Location to = event.getTo();
        if(to == null) return;
        org.bukkit.Location from = event.getFrom();
        if(from.getWorld() == to.getWorld()
            && from.getBlockX() == to.getBlockX()
            && from.getBlockY() == to.getBlockY()
            && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        PlayerData playerData = PlayerData.getPlayer(player);
        if(playerData.isHasInBlackList()) return;

        // Cache the RegionQuery to avoid creating a new instance every call
        if(cachedQuery == null) {
            cachedQuery = com.sk89q.worldguard.WorldGuard.getInstance()
                .getPlatform().getRegionContainer().createQuery();
        }

        Location location = BukkitAdapter.adapt(to);
        ApplicableRegionSet applicableRegionSet = cachedQuery.getApplicableRegions(location);
        StateFlag.State flagState = applicableRegionSet.queryState(null, customFlag);
        if(flagState == null || flagState.equals(StateFlag.State.DENY)){
            playerData.hideAllCosmetics();
            return;
        }
        playerData.showAllCosmetics();
    }
}
