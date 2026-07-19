package xyz.voltraz.cosmetics.listeners;

import xyz.voltraz.cosmetics.VoltrazCosmetics;
import xyz.voltraz.cosmetics.cache.EntityCache;
import xyz.voltraz.cosmetics.cache.NPC;
import xyz.voltraz.cosmetics.cache.NPCType;
import net.citizensnpcs.api.event.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;



public class CitizensListener implements Listener {
    private final VoltrazCosmetics plugin = VoltrazCosmetics.getInstance();
    @EventHandler
    public void onLoad(NPCSpawnEvent event) {
        net.citizensnpcs.api.npc.NPC cNPC = event.getNPC();
        NPC npc = plugin.getNPCsLoader().getNPC(cNPC.getId(), NPCType.CITIZENS);
        if (npc == null) return;
        plugin.getCitizens().loadNPC(cNPC, npc);
    }

    @EventHandler
    public void onRemove(NPCRemoveByCommandSenderEvent event){
        plugin.getNPCsLoader().removeNPC(event.getNPC().getId(), NPCType.CITIZENS);
    }

    @EventHandler
    public void onDespawn(NPCDespawnEvent event){
        NPC npc = plugin.getNPCsLoader().getNPC(event.getNPC().getId(), NPCType.CITIZENS);
        if(npc == null) return;
        EntityCache entityCache = npc.getEntityCache();
        if(entityCache == null) return;
        entityCache.clearCosmeticsInUse();
    }

    @EventHandler
    public void onDeath(NPCDeathEvent event){
        NPC npc = plugin.getNPCsLoader().getNPC(event.getNPC().getId(), NPCType.CITIZENS);
        if(npc == null) return;
        EntityCache entityCache = npc.getEntityCache();
        if(entityCache == null) return;
        entityCache.clearCosmeticsInUse();
    }

    @EventHandler
    public void onTeleport(NPCTeleportEvent event){
        NPC npc = plugin.getNPCsLoader().getNPC(event.getNPC().getId(), NPCType.CITIZENS);
        if(npc == null) return;
        EntityCache entityCache = npc.getEntityCache();
        if(entityCache == null) return;
        entityCache.clearCosmeticsInUse();
    }
}
