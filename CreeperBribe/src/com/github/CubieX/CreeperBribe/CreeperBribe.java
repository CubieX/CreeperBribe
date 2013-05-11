package com.github.CubieX.CreeperBribe;

import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class CreeperBribe extends JavaPlugin
{
   public static final Logger log = Bukkit.getServer().getLogger();
   static final String logPrefix = "[Blessed] "; // Prefix to go in front of all log entries

   private CreeperBribe plugin = null;
   private CBCommandHandler comHandler = null;
   private CBConfigHandler cHandler = null;
   private CBEntityListener eListener = null;
   //private BSchedulerHandler schedHandler = null;

   static boolean debug = false;
   static int bribeChance = 50;                 // chance to bribe a creeper in percent
   static Material bribeItem = Material.CAKE;   // Item to bribe a creeper with
   static int angryExplosionRadius = 5;         // explosion radius of an angry creeper in meters (blocks)
   static int nauseaDuration = 20;              // duration of nausea (confusion) from an angry creepers explosion (seconds)
   static int blindnessDuration = 15;           // duration of blindness from an angry creepers explosion (seconds)

   //*************************************************
   static String usedConfigVersion = "1"; // Update this every time the config file version changes, so the plugin knows, if there is a suiting config present
   //*************************************************

   @Override
   public void onEnable()
   {
      this.plugin = this;
      cHandler = new CBConfigHandler(this);

      if(!checkConfigFileVersion())
      {
         log.severe(logPrefix + "Outdated or corrupted config file(s). Please delete your config files."); 
         log.severe(logPrefix + "will generate a new config for you.");
         log.severe(logPrefix + "will be disabled now. Config file is outdated or corrupted.");
         getServer().getPluginManager().disablePlugin(this);
         return;
      }

      if (!hookToPermissionSystem())
      {
         log.info(logPrefix + "- Disabled due to no superperms compatible permission system found!");
         getServer().getPluginManager().disablePlugin(this);
         return;
      }

      eListener = new CBEntityListener(this);      
      comHandler = new CBCommandHandler(this, cHandler);      
      getCommand("cbribe").setExecutor(comHandler);

      //schedHandler = new BSchedulerHandler(this);

      readConfigValues();

      log.info(this.getDescription().getName() + " version " + getDescription().getVersion() + " is enabled!");

      //schedHandler.startPlayerInWaterCheckScheduler_SynchRepeating();
   }

   private boolean checkConfigFileVersion()
   {      
      boolean configOK = false;     

      if(cHandler.getConfig().isSet("config_version"))
      {
         String configVersion = cHandler.getConfig().getString("config_version");

         if(configVersion.equals(usedConfigVersion))
         {
            configOK = true;
         }
      }

      return (configOK);
   }

   private boolean hookToPermissionSystem()
   {
      if ((getServer().getPluginManager().getPlugin("PermissionsEx") == null) &&
            (getServer().getPluginManager().getPlugin("bPermissions") == null) &&
            (getServer().getPluginManager().getPlugin("zPermissions") == null) &&
            (getServer().getPluginManager().getPlugin("PermissionsBukkit") == null))
      {
         return false;
      }
      else
      {
         return true;
      }
   }

   public void readConfigValues()
   {
      boolean exceed = false;
      boolean invalid = false;

      debug = cHandler.getConfig().getBoolean("debug");

      bribeChance = cHandler.getConfig().getInt("bribeChance");
      if(bribeChance > 100) {bribeChance = 100; exceed = true;}
      if(bribeChance < 0) {bribeChance = 0; exceed = true;}

      if(null != Material.getMaterial(cHandler.getConfig().getString("bribeItem")))
      {
         bribeItem = Material.getMaterial(cHandler.getConfig().getString("bribeItem"));
      }
      else
      {
         invalid = true;
      }
      
      angryExplosionRadius = cHandler.getConfig().getInt("angryExplosionRadius");
      if(angryExplosionRadius > 10) {angryExplosionRadius = 10; exceed = true;}
      if(angryExplosionRadius < 3) {angryExplosionRadius = 3; exceed = true;}
      
      nauseaDuration = cHandler.getConfig().getInt("nauseaDuration");
      if(nauseaDuration > 60) {nauseaDuration = 60; exceed = true;}
      if(nauseaDuration < 0) {nauseaDuration = 0; exceed = true;}
      
      blindnessDuration = cHandler.getConfig().getInt("blindnessDuration");
      if(blindnessDuration > 60) {blindnessDuration = 60; exceed = true;}
      if(blindnessDuration < 0) {blindnessDuration = 0; exceed = true;}

      if(exceed)
      {
         log.warning("One or more config values are exceeding their allowed range. Please check your config file!");
      }
      
      if(invalid)
      {
         log.warning("One or more config values are invalid. Please check your config file!");
      }
   }

   @Override
   public void onDisable()
   {
      this.getServer().getScheduler().cancelAllTasks();
      cHandler = null;
      eListener = null;
      comHandler = null;
      //schedHandler = null; // TODO ACTIVATE THIS AGAIN IF USED!
      log.info(this.getDescription().getName() + " version " + getDescription().getVersion() + " is disabled!");
   }

   // #########################################################


}


