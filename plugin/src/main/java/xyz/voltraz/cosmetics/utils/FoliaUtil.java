package xyz.voltraz.cosmetics.utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

public final class FoliaUtil {

    public static Object runTaskTimer(Plugin plugin, Runnable task, long delay, long period) {
        if (isFolia()) {
            return Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, st -> task.run(), Math.max(delay, 1), Math.max(period, 1));
        } else {
            return Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
        }
    }

    public static Object runTaskLaterAsynchronously(Plugin plugin, Runnable task, long delay) {
        if (isFolia()) {
            return Bukkit.getAsyncScheduler().runDelayed(plugin, st -> task.run(), delay, java.util.concurrent.TimeUnit.MILLISECONDS);
        } else {
            return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delay);
        }
    }

    public static Object runTask(Plugin plugin, Runnable task) {
        if (isFolia()) {
            return Bukkit.getGlobalRegionScheduler().run(plugin, st -> task.run());
        } else {
            return Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public static Object runTaskLater(Plugin plugin, Runnable task, long delay) {
        if (isFolia()) {
            return Bukkit.getGlobalRegionScheduler().runDelayed(plugin, st -> task.run(), delay);
        } else {
            return Bukkit.getScheduler().runTaskLater(plugin, task, delay);
        }
    }

    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static void cancel(Object task) {
        if (task instanceof ScheduledTask) {
            ((ScheduledTask) task).cancel();
        } else if (task instanceof org.bukkit.scheduler.BukkitTask) {
            ((org.bukkit.scheduler.BukkitTask) task).cancel();
        }
    }

    public static void cancelTasks(Plugin plugin) {
        if (!isFolia()) {
            Bukkit.getScheduler().cancelTasks(plugin);
        }
    }
}
