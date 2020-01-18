package io.github.tecses1.DisableTeleportNearPlayers;

import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import com.alessiodp.parties.api.Parties;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import java.io.IOException;
import java.util.Iterator;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.Location;
import java.util.ArrayList;
import java.io.File;
import org.bukkit.entity.Player;
import com.alessiodp.parties.api.interfaces.PartiesAPI;
import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin implements Listener, CommandExecutor
{
    FileConfiguration config;
    List<String> blacklistedCommands;
    List<String> exemptWorlds;
    PartiesAPI api;
    List<Player> disabledPlayersT;
    List<Player> disabledPlayersB;
    List<Player> disabledPlayersBH;
    List<Main.treaty> treaties;
    File userYml;
    FileConfiguration userConfig;
    
    public Main() {
        this.disabledPlayersT = new ArrayList<Player>();
        this.disabledPlayersB = new ArrayList<Player>();
        this.disabledPlayersBH = new ArrayList<Player>();
        this.treaties = new ArrayList<Main.treaty>();
    }
    
    public List<Player> getNearbyPlayers(final Location loc, final int distance, final World world, final String party) {
        final int distanceSquared = distance * distance;
        final List<Player> list = new ArrayList<Player>();
        for (final Player p : Bukkit.getOnlinePlayers()) {
            if (p.getWorld() == world && !party.equals(this.api.getPartyPlayer(p.getUniqueId()).getPartyName().toLowerCase()) && p.getLocation() != loc && p.getLocation().distanceSquared(loc) < distanceSquared) {
                list.add(p);
            }
        }
        return list;
    }
    
    public void saveCustomYml(final FileConfiguration ymlConfig, final File ymlFile) {
        try {
            ymlConfig.save(ymlFile);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void onEnable() {
        this.saveDefaultConfig();
        this.config = this.getConfig();
        this.getCommand("dt").setExecutor((CommandExecutor)this);
        this.getCommand("treaty").setExecutor((CommandExecutor)this);
        this.getServer().getPluginManager().registerEvents((Listener)this, (Plugin)this);
        this.blacklistedCommands = (List<String>)this.config.getStringList("disabled-commands.essentials");
        this.exemptWorlds = (List<String>)this.config.getStringList("exempt-worlds");
        this.userYml = new File(this.getDataFolder() + "/users.yml");
        this.userConfig = (FileConfiguration)YamlConfiguration.loadConfiguration(this.userYml);
        if (this.getServer().getPluginManager().getPlugin("Parties") != null && this.getServer().getPluginManager().getPlugin("Parties").isEnabled()) {
            this.api = Parties.getApi();
        }
        new Main.Main$1(this).runTaskTimer((Plugin)this, 0L, (long)this.config.getInt("update-time"));
        this.getLogger().info("Enabled successfully.");
    }
    
    public void onDisable() {
        this.saveCustomYml(this.userConfig, this.userYml);
        this.getLogger().info("Disabled.");
    }
    
    public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        if (cmd.getName().equalsIgnoreCase("dt")) {
            if (args.length != 0) {
                if (args[0].equalsIgnoreCase("reload")) {
                    this.reloadConfig();
                    sender.sendMessage("Configuration reloaded.");
                    return true;
                }
                if (args[0].equalsIgnoreCase("info")) {
                    sender.sendMessage("Info for DisableTeleportNearPlayers:\n    version: 1.0\n    ");
                    return true;
                }
                if (args[0].equalsIgnoreCase("help")) {
                    sender.sendMessage("Valid Commands for DisableTeleportNearPlayers:\n    /dt help - Displays help.\n    /dt info - Displays plugin info.\n    /dt reload - Reloads the plugin.\n    /treaty [player] - Creates a temporary treaty to a player");
                    return true;
                }
            }
        }
        else if (cmd.getName().equalsIgnoreCase("treaty")) {
            if (args.length == 0) {
                return false;
            }
            if (args[0].equalsIgnoreCase("accept")) {
                for (final Main.treaty activeTreaty : this.treaties) {
                    final Player s = (Player)sender;
                    if (activeTreaty.playerTwoID == s.getUniqueId()) {
                        activeTreaty.active = true;
                        final Player targetPlayer = Bukkit.getPlayer(activeTreaty.playerOneID);
                        targetPlayer.sendMessage(String.valueOf(sender.getName()) + " accepted your treaty.");
                        sender.sendMessage("You accepted your treaty with " + targetPlayer.getName());
                        final int treatyIndex = this.treaties.indexOf(activeTreaty);
                        Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)this, (Runnable)new Main.Main$2(this, targetPlayer, sender, treatyIndex), (long)(this.config.getInt("treaty-time") * 20));
                        return true;
                    }
                }
            }
            else {
                final Player target = Bukkit.getPlayerExact(args[0]);
                if (target != null) {
                    target.sendMessage(String.valueOf(sender.getName()) + " wants to make a treaty with you!\n accept with /treaty accept");
                    final Player s2 = (Player)sender;
                    final Player t = target;
                    final Main.treaty newTreaty = new Main.treaty(this, s2.getUniqueId(), t.getUniqueId(), false);
                    sender.sendMessage("Treaty request sent.");
                    this.treaties.add(newTreaty);
                    return true;
                }
                sender.sendMessage("That player is offline or doesn't exist.");
            }
        }
        else if (cmd.getName().equalsIgnoreCase("dthome")) {
            final Player s3 = (Player)sender;
            final Location location = s3.getLocation();
            if (this.exemptWorlds.contains(s3.getWorld().getName())) {
                sender.sendMessage("You can't set home on an exempt world!");
            }
            else {
                this.userConfig.set(s3.getUniqueId().toString(), (Object)"");
                this.userConfig.set(String.valueOf(s3.getUniqueId().toString()) + ".world", (Object)s3.getWorld().getName());
                this.userConfig.set(String.valueOf(s3.getUniqueId().toString()) + ".location", (Object)"");
                this.userConfig.set(String.valueOf(s3.getUniqueId().toString()) + ".location.x", (Object)location.getX());
                this.userConfig.set(String.valueOf(s3.getUniqueId().toString()) + ".location.y", (Object)location.getY());
                this.userConfig.set(String.valueOf(s3.getUniqueId().toString()) + ".location.z", (Object)location.getZ());
                this.saveCustomYml(this.userConfig, this.userYml);
                sender.sendMessage("Home set to your location.");
            }
        }
        return false;
    }
    
    @EventHandler
    public void onEntityDamage(final PlayerTeleportEvent event) {
        final Player player = event.getPlayer();
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL && this.config.getBoolean("disable-enderpearl-damage")) {
            event.setCancelled(true);
            player.teleport(event.getTo());
        }
    }
    
    @EventHandler
    public void commandPreprocessEvent(final PlayerCommandPreprocessEvent event) {
        if (!this.blacklistedCommands.contains(event.getMessage().replaceFirst("/", "").toLowerCase()) && !event.getMessage().replaceFirst("/", "").toLowerCase().startsWith("home")) {
            event.setCancelled(false);
            return;
        }
        if (this.disabledPlayersT.contains(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("You are too close to a player not in your party! Consider a treaty.");
        }
    }
}