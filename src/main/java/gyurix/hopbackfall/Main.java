package gyurix.hopbackfall;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gyurix.configfile.ConfigFile;
import gyurix.spigotlib.SU;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;

import static gyurix.hopbackfall.Config.checkInterval;

public class Main extends JavaPlugin implements Listener {
  public static ConfigFile kf;
  public static WorldGuardPlugin wg;
  public static HashMap<UUID, Location> lastLoc = new HashMap<>();

  @Override
  public void onEnable() {
    SU.saveResources(this, "config.yml");
    kf = new ConfigFile(new File(getDataFolder() + File.separator + "config.yml"));
    kf.data.deserialize(Config.class);
    wg = (WorldGuardPlugin) SU.pm.getPlugin("WorldGuard");
    SU.pm.registerEvents(this, this);
    if (wg.getDescription().getVersion().startsWith("6.")) {
      SU.sch.scheduleSyncRepeatingTask(this, () -> {
        for (Player p : Bukkit.getOnlinePlayers()) {
          Location loc = p.getLocation();
          if (shouldTpBackWG16(loc)) {
            Location l = lastLoc.get(p.getUniqueId());
            l.setYaw(loc.getYaw());
            l.setPitch(loc.getPitch());
            if (l != null)
              p.teleport(l);
          } else if (isSafe(loc))
            lastLoc.put(p.getUniqueId(), loc);
        }
      }, checkInterval, checkInterval);
    } else {
      SU.sch.scheduleSyncRepeatingTask(this, () -> {
        for (Player p : Bukkit.getOnlinePlayers()) {
          Location loc = p.getLocation();
          if (shouldTpBackWG17(loc)) {
            Location l = lastLoc.get(p.getUniqueId());
            l.setYaw(loc.getYaw());
            l.setPitch(loc.getPitch());
            if (l != null)
              p.teleport(l);
          } else if (isSafe(loc))
            lastLoc.put(p.getUniqueId(), loc);
        }
      }, checkInterval, checkInterval);
    }
  }

  private boolean shouldTpBackWG16(Location loc) {
    HashMap<String, Double> map = Config.worlds.get(loc.getWorld().getName());
    if (map == null)
      map = Config.worlds.get("others");
    if (map == null)
      return false;
    RegionManager rg = wg.getRegionManager(loc.getWorld());
    ApplicableRegionSet rs = rg.getApplicableRegions(loc);
    for (ProtectedRegion pr : rs.getRegions()) {
      Double d = map.get(pr.getId());
      if (d != null)
        return loc.getY() <= d;
    }
    Double d = map.get("others");
    return d != null && loc.getY() < d;
  }

  private boolean shouldTpBackWG17(Location loc) {
    HashMap<String, Double> map = Config.worlds.get(loc.getWorld().getName());
    if (map == null)
      map = Config.worlds.get("others");
    if (map == null)
      return false;
    /*RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
    RegionManager rg = container.get(BukkitAdapter.adapt(loc.getWorld()));
    Block b = loc.getBlock();
    ApplicableRegionSet rs = rg.getApplicableRegions(BlockVector3.at(b.getX(), b.getY(), b.getZ()));
    for (ProtectedRegion pr : rs.getRegions()) {
      Double d = map.get(pr.getId());
      if (d != null)
        return loc.getY() <= d;
    }*/
    Double d = map.get("others");
    return d != null && loc.getY() < d;
  }

  private boolean isSafe(Location loc) {
    Block b = loc.getBlock().getRelative(BlockFace.DOWN);
    return b.getType().isSolid() || b.getRelative(BlockFace.DOWN).getType().isSolid();
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    lastLoc.remove(e.getPlayer().getUniqueId());
  }

  @Override
  public void onDisable() {

  }
}
