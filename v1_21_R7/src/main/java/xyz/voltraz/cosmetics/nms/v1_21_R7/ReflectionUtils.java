package xyz.voltraz.cosmetics.nms.v1_21_R7;

import net.minecraft.core.Rotations;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

public class ReflectionUtils {

    // Cached Methods
    private static Method compoundTagKeySetMethod;
    private static Method compoundTagGetCompoundMethod;
    private static Method compoundTagGetStringMethod;
    
    private static Method rotationsXMethod;
    private static Method rotationsYMethod;
    private static Method rotationsZMethod;
    
    private static Constructor<?> teleportPacketConstructorWithPmr;
    private static Constructor<?> teleportPacketConstructorFallback;
    private static Class<?> pmrClass;
    private static Method pmrOfMethod;
    
    private static Constructor<?> playerInfoEntryConstructorWithRemoteChat;
    private static Constructor<?> playerInfoEntryConstructorFallback;
    
    private static Method entityAbsMoveToMethod;
    private static Method entitySetPosMethod;
    private static Method entitySetYRotMethod;
    private static Method entitySetXRotMethod;
    private static boolean useFallbackMoveTo = false;
    
    // CraftItemStack (flat package for Paper 1.21.11+)
    private static Class<?> craftItemStackClass;
    private static Method craftItemStackAsNMSCopy;
    private static Method craftItemStackAsBukkitCopy;
    
    // CraftChatMessage
    private static Class<?> craftChatMessageClass;
    private static Method craftChatMessageFromString;
    
    // CraftEntity/CraftPlayer/CraftWorld helpers (flat package for Canvas/Folia 1.21.11+)
    private static Class<?> craftEntityClass;
    private static Class<?> craftPlayerClass;
    private static Class<?> craftWorldClass;
    private static Class<?> craftArmorStandClass;
    private static Class<?> craftLivingEntityClass;
    private static Class<?> craftServerClass;
    private static Class<?> craftLocationClass;
    private static Method craftEntityGetHandle;
    private static Method craftPlayerGetHandle;
    private static Method craftWorldGetHandle;
    private static Method craftArmorStandGetHandle;
    private static Method craftLivingEntityGetHandle;
    private static Method craftServerGetServer;
    private static Method craftLocationToBlockPosition;
    static {
        // CompoundTag
        try {
            compoundTagKeySetMethod = CompoundTag.class.getMethod("keySet");
        } catch (Exception e) {
            try {
                compoundTagKeySetMethod = CompoundTag.class.getMethod("getAllKeys");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        try {
            compoundTagGetCompoundMethod = CompoundTag.class.getMethod("getCompoundOrEmpty", String.class);
        } catch (Exception e) {
            try {
                compoundTagGetCompoundMethod = CompoundTag.class.getMethod("getCompound", String.class);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        try {
            compoundTagGetStringMethod = CompoundTag.class.getMethod("getStringOr", String.class, String.class);
        } catch (Exception e) {
            try {
                compoundTagGetStringMethod = CompoundTag.class.getMethod("getString", String.class);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        // Rotations
        try {
            rotationsXMethod = Rotations.class.getMethod("x");
        } catch (Exception e) {
            try {
                rotationsXMethod = Rotations.class.getMethod("getX");
            } catch (Exception ex) {}
        }
        try {
            rotationsYMethod = Rotations.class.getMethod("y");
        } catch (Exception e) {
            try {
                rotationsYMethod = Rotations.class.getMethod("getY");
            } catch (Exception ex) {}
        }
        try {
            rotationsZMethod = Rotations.class.getMethod("z");
        } catch (Exception e) {
            try {
                rotationsZMethod = Rotations.class.getMethod("getZ");
            } catch (Exception ex) {}
        }

        // ClientboundTeleportEntityPacket
        try {
            pmrClass = Class.forName("net.minecraft.world.entity.PositionMoveRotation");
            pmrOfMethod = pmrClass.getMethod("of", Entity.class);
            teleportPacketConstructorWithPmr = ClientboundTeleportEntityPacket.class.getConstructor(int.class, pmrClass, Set.class, boolean.class);
        } catch (Exception e) {
            try {
                teleportPacketConstructorFallback = ClientboundTeleportEntityPacket.class.getConstructor(Entity.class);
            } catch (Exception ex) {}
        }

        // ClientboundPlayerInfoUpdatePacket.Entry
        try {
            playerInfoEntryConstructorWithRemoteChat = ClientboundPlayerInfoUpdatePacket.Entry.class.getConstructor(
                    java.util.UUID.class, com.mojang.authlib.GameProfile.class, boolean.class, int.class, net.minecraft.world.level.GameType.class, net.minecraft.network.chat.Component.class, boolean.class, int.class, Class.forName("net.minecraft.network.chat.RemoteChatSession$Data"));
        } catch (Exception e) {
            try {
                playerInfoEntryConstructorFallback = ClientboundPlayerInfoUpdatePacket.Entry.class.getConstructor(
                        java.util.UUID.class, com.mojang.authlib.GameProfile.class, boolean.class, int.class, net.minecraft.world.level.GameType.class, net.minecraft.network.chat.Component.class, Class.forName("net.minecraft.network.chat.RemoteChatSession$Data"));
            } catch (Exception ex) {}
        }

        // Entity absMoveTo fallback
        try {
            entityAbsMoveToMethod = Entity.class.getMethod("absMoveTo", double.class, double.class, double.class, float.class, float.class);
        } catch (Exception e) {
            try {
                for (Method m : Entity.class.getMethods()) {
                    if (m.getName().equals("absMoveTo") && m.getParameterCount() == 5 && m.getParameterTypes()[0] == double.class) {
                        entityAbsMoveToMethod = m;
                        break;
                    }
                }
                if (entityAbsMoveToMethod == null) {
                    for (Method m : Entity.class.getMethods()) {
                        if ((m.getName().equals("moveTo") || m.getName().equals("teleportTo")) && m.getParameterCount() == 5 && m.getParameterTypes()[0] == double.class) {
                            entityAbsMoveToMethod = m;
                            break;
                        }
                    }
                }
                if (entityAbsMoveToMethod == null) {
                    useFallbackMoveTo = true;
                    entitySetPosMethod = Entity.class.getMethod("setPos", double.class, double.class, double.class);
                    entitySetYRotMethod = Entity.class.getMethod("setYRot", float.class);
                    entitySetXRotMethod = Entity.class.getMethod("setXRot", float.class);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        // CraftItemStack - try flat package first (Paper 1.21.11+), then versioned
        try {
            craftItemStackClass = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack");
        } catch (ClassNotFoundException e) {
            try {
                craftItemStackClass = Class.forName("org.bukkit.craftbukkit.v1_21_R7.inventory.CraftItemStack");
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        }
        if (craftItemStackClass != null) {
            try {
                craftItemStackAsNMSCopy = craftItemStackClass.getMethod("asNMSCopy", org.bukkit.inventory.ItemStack.class);
                craftItemStackAsBukkitCopy = craftItemStackClass.getMethod("asBukkitCopy", net.minecraft.world.item.ItemStack.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // CraftChatMessage - flat package first
        try {
            craftChatMessageClass = Class.forName("org.bukkit.craftbukkit.util.CraftChatMessage");
        } catch (ClassNotFoundException e) {
            try {
                craftChatMessageClass = Class.forName("org.bukkit.craftbukkit.v1_21_R7.util.CraftChatMessage");
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        }
        if (craftChatMessageClass != null) {
            try {
                craftChatMessageFromString = craftChatMessageClass.getMethod("fromStringOrNull", String.class);
            } catch (Exception e) {
                try {
                    craftChatMessageFromString = craftChatMessageClass.getMethod("fromString", String.class);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        // CraftEntity / CraftPlayer / CraftWorld - flat package for Canvas/Folia 1.21.11+
        try {
            craftEntityClass = Class.forName("org.bukkit.craftbukkit.entity.CraftEntity");
        } catch (ClassNotFoundException e) {
            try {
                craftEntityClass = Class.forName("org.bukkit.craftbukkit.v1_21_R7.entity.CraftEntity");
            } catch (ClassNotFoundException ex) { ex.printStackTrace(); }
        }
        try {
            craftPlayerClass = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer");
        } catch (ClassNotFoundException e) {
            try {
                craftPlayerClass = Class.forName("org.bukkit.craftbukkit.v1_21_R7.entity.CraftPlayer");
            } catch (ClassNotFoundException ex) { ex.printStackTrace(); }
        }
        try {
            craftWorldClass = Class.forName("org.bukkit.craftbukkit.CraftWorld");
        } catch (ClassNotFoundException e) {
            try {
                craftWorldClass = Class.forName("org.bukkit.craftbukkit.v1_21_R7.CraftWorld");
            } catch (ClassNotFoundException ex) { ex.printStackTrace(); }
        }
        if (craftEntityClass != null && craftPlayerClass != null && craftWorldClass != null) {
            try {
                craftEntityGetHandle = craftEntityClass.getMethod("getHandle");
                craftPlayerGetHandle = craftPlayerClass.getMethod("getHandle");
                craftWorldGetHandle = craftWorldClass.getMethod("getHandle");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // CraftArmorStand - flat package for Canvas/Folia 1.21.11+
        try {
            craftArmorStandClass = Class.forName("org.bukkit.craftbukkit.entity.CraftArmorStand");
        } catch (ClassNotFoundException e) {
            try {
                craftArmorStandClass = Class.forName("org.bukkit.craftbukkit.v1_21_R7.entity.CraftArmorStand");
            } catch (ClassNotFoundException ex) { ex.printStackTrace(); }
        }
        // CraftLivingEntity - flat package
        try {
            craftLivingEntityClass = Class.forName("org.bukkit.craftbukkit.entity.CraftLivingEntity");
        } catch (ClassNotFoundException e) {
            try {
                craftLivingEntityClass = Class.forName("org.bukkit.craftbukkit.v1_21_R7.entity.CraftLivingEntity");
            } catch (ClassNotFoundException ex) { ex.printStackTrace(); }
        }
        // CraftServer - flat package
        try {
            craftServerClass = Class.forName("org.bukkit.craftbukkit.CraftServer");
        } catch (ClassNotFoundException e) {
            try {
                craftServerClass = Class.forName("org.bukkit.craftbukkit.v1_21_R7.CraftServer");
            } catch (ClassNotFoundException ex) { ex.printStackTrace(); }
        }
        // CraftLocation - flat package
        try {
            craftLocationClass = Class.forName("org.bukkit.craftbukkit.util.CraftLocation");
        } catch (ClassNotFoundException e) {
            try {
                craftLocationClass = Class.forName("org.bukkit.craftbukkit.v1_21_R7.util.CraftLocation");
            } catch (ClassNotFoundException ex) { ex.printStackTrace(); }
        }
        if (craftArmorStandClass != null && craftLivingEntityClass != null && craftServerClass != null && craftLocationClass != null) {
            try {
                craftArmorStandGetHandle = craftArmorStandClass.getMethod("getHandle");
                craftLivingEntityGetHandle = craftLivingEntityClass.getMethod("getHandle");
                craftServerGetServer = craftServerClass.getMethod("getServer");
                craftLocationToBlockPosition = craftLocationClass.getMethod("toBlockPosition", org.bukkit.Location.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // CompoundTag
    public static Set<String> getKeys(CompoundTag tag) {
        if (compoundTagKeySetMethod != null) {
            try {
                return (Set<String>) compoundTagKeySetMethod.invoke(tag);
            } catch (Exception e) { e.printStackTrace(); }
        }
        return Collections.emptySet();
    }

    public static CompoundTag getCompound(CompoundTag tag, String key) {
        if (compoundTagGetCompoundMethod != null) {
            try {
                return (CompoundTag) compoundTagGetCompoundMethod.invoke(tag, key);
            } catch (Exception e) { e.printStackTrace(); }
        }
        return new CompoundTag();
    }

    public static String getString(CompoundTag tag, String key) {
        if (compoundTagGetStringMethod != null) {
            try {
                if (compoundTagGetStringMethod.getParameterCount() == 2) {
                    return (String) compoundTagGetStringMethod.invoke(tag, key, "");
                } else {
                    return (String) compoundTagGetStringMethod.invoke(tag, key);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return "";
    }

    // Rotations
    public static float rotX(Rotations rot) {
        if (rotationsXMethod != null) {
            try { return (float) rotationsXMethod.invoke(rot); } catch (Exception e) { e.printStackTrace(); }
        }
        return 0f;
    }

    public static float rotY(Rotations rot) {
        if (rotationsYMethod != null) {
            try { return (float) rotationsYMethod.invoke(rot); } catch (Exception e) { e.printStackTrace(); }
        }
        return 0f;
    }

    public static float rotZ(Rotations rot) {
        if (rotationsZMethod != null) {
            try { return (float) rotationsZMethod.invoke(rot); } catch (Exception e) { e.printStackTrace(); }
        }
        return 0f;
    }

    // ClientboundTeleportEntityPacket
    public static ClientboundTeleportEntityPacket createTeleportPacket(Entity entity) {
        if (teleportPacketConstructorWithPmr != null && pmrOfMethod != null) {
            try {
                Object pmr = pmrOfMethod.invoke(null, entity);
                return (ClientboundTeleportEntityPacket) teleportPacketConstructorWithPmr.newInstance(entity.getId(), pmr, Collections.emptySet(), false);
            } catch (Exception e) { e.printStackTrace(); }
        } else if (teleportPacketConstructorFallback != null) {
            try {
                return (ClientboundTeleportEntityPacket) teleportPacketConstructorFallback.newInstance(entity);
            } catch (Exception e) { e.printStackTrace(); }
        }
        return null;
    }

    // ClientboundPlayerInfoUpdatePacket.Entry
    public static ClientboundPlayerInfoUpdatePacket.Entry createPlayerInfoEntry(
            java.util.UUID uuid, com.mojang.authlib.GameProfile profile, boolean listed, int latency, net.minecraft.world.level.GameType gameMode, net.minecraft.network.chat.Component displayName) {
        if (playerInfoEntryConstructorWithRemoteChat != null) {
            try {
                return (ClientboundPlayerInfoUpdatePacket.Entry) playerInfoEntryConstructorWithRemoteChat.newInstance(uuid, profile, listed, latency, gameMode, displayName, true, 0, null);
            } catch (Exception e) { e.printStackTrace(); }
        } else if (playerInfoEntryConstructorFallback != null) {
            try {
                return (ClientboundPlayerInfoUpdatePacket.Entry) playerInfoEntryConstructorFallback.newInstance(uuid, profile, listed, latency, gameMode, displayName, null);
            } catch (Exception e) { e.printStackTrace(); }
        }
        return null;
    }

    // Entity absMoveTo fallback
    public static void absMoveTo(Entity entity, double x, double y, double z, float yRot, float xRot) {
        if (entityAbsMoveToMethod != null) {
            try {
                entityAbsMoveToMethod.invoke(entity, x, y, z, yRot, xRot);
            } catch (Exception e) { e.printStackTrace(); }
        } else if (useFallbackMoveTo && entitySetPosMethod != null && entitySetYRotMethod != null && entitySetXRotMethod != null) {
            try {
                entitySetPosMethod.invoke(entity, x, y, z);
                entitySetYRotMethod.invoke(entity, yRot);
                entitySetXRotMethod.invoke(entity, xRot);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // CraftItemStack helpers
    public static net.minecraft.world.item.ItemStack asNMSCopy(org.bukkit.inventory.ItemStack bukkitStack) {
        if (craftItemStackAsNMSCopy != null) {
            try {
                return (net.minecraft.world.item.ItemStack) craftItemStackAsNMSCopy.invoke(null, bukkitStack);
            } catch (Exception e) { e.printStackTrace(); }
        }
        return null;
    }

    public static org.bukkit.inventory.ItemStack asBukkitCopy(net.minecraft.world.item.ItemStack nmsStack) {
        if (craftItemStackAsBukkitCopy != null) {
            try {
                return (org.bukkit.inventory.ItemStack) craftItemStackAsBukkitCopy.invoke(null, nmsStack);
            } catch (Exception e) { e.printStackTrace(); }
        }
        return null;
    }

    // CraftChatMessage helper
    public static net.minecraft.network.chat.Component fromStringOrNull(String text) {
        if (craftChatMessageFromString != null) {
            try {
                return (net.minecraft.network.chat.Component) craftChatMessageFromString.invoke(null, text);
            } catch (Exception e) { e.printStackTrace(); }
        }
        return null;
    }

    // CraftEntity/CraftPlayer/CraftWorld helpers
    public static ServerPlayer getHandle(org.bukkit.entity.Player player) {
        if (craftPlayerGetHandle != null && craftPlayerClass != null) {
            try {
                if (craftPlayerClass.isInstance(player)) {
                    return (ServerPlayer) craftPlayerGetHandle.invoke(player);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return null;
    }

    public static net.minecraft.world.entity.Entity getHandle(org.bukkit.entity.Entity entity) {
        if (craftEntityGetHandle != null && craftEntityClass != null) {
            try {
                if (craftEntityClass.isInstance(entity)) {
                    return (net.minecraft.world.entity.Entity) craftEntityGetHandle.invoke(entity);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return null;
    }

    public static net.minecraft.server.level.ServerLevel getHandle(org.bukkit.World world) {
        if (craftWorldGetHandle != null && craftWorldClass != null) {
            try {
                if (craftWorldClass.isInstance(world)) {
                    return (net.minecraft.server.level.ServerLevel) craftWorldGetHandle.invoke(world);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return null;
    }

    // CraftArmorStand / CraftLivingEntity helpers
    public static net.minecraft.world.entity.decoration.ArmorStand getHandle(org.bukkit.entity.ArmorStand armorStand) {
        if (craftArmorStandGetHandle != null && craftArmorStandClass != null) {
            try {
                if (craftArmorStandClass.isInstance(armorStand)) {
                    return (net.minecraft.world.entity.decoration.ArmorStand) craftArmorStandGetHandle.invoke(armorStand);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return null;
    }

    public static net.minecraft.world.entity.LivingEntity getHandle(org.bukkit.entity.LivingEntity entity) {
        if (craftLivingEntityGetHandle != null && craftLivingEntityClass != null) {
            try {
                if (craftLivingEntityClass.isInstance(entity)) {
                    return (net.minecraft.world.entity.LivingEntity) craftLivingEntityGetHandle.invoke(entity);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return null;
    }

    // CraftServer helper
    public static MinecraftServer getServer() {
        if (craftServerGetServer != null && craftServerClass != null) {
            try {
                Object server = Bukkit.getServer();
                if (craftServerClass.isInstance(server)) {
                    return (MinecraftServer) craftServerGetServer.invoke(server);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return null;
    }

    // CraftLocation helper
    public static net.minecraft.core.BlockPos toBlockPosition(org.bukkit.Location location) {
        if (craftLocationToBlockPosition != null && craftLocationClass != null) {
            try {
                return (net.minecraft.core.BlockPos) craftLocationToBlockPosition.invoke(null, location);
            } catch (Exception e) { e.printStackTrace(); }
        }
        return null;
    }
}
