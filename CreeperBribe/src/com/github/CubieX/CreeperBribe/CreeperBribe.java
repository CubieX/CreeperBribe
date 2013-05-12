package com.github.CubieX.CreeperBribe;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import sun.io.Converters;

public class CreeperBribe extends JavaPlugin
{
   public static final Logger log = Bukkit.getServer().getLogger();
   static final String logPrefix = "[CreeperBribe] "; // Prefix to go in front of all log entries

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

   // ##### These methods to get Entities in the Field of View FOV are from kumpelblase2
   // ##### See: http://forums.bukkit.org/threads/how-can-i-check-if-a-player-can-visually-see-another.81134/

   /**
    * Gets entities inside a cone.
    * @see Utilities#getPlayersInCone(List, Location, int, int, int)
    *
    * @param entities - {@code List<Entity>}, list of nearby entities to check if in FOV
    * @param startpoint - {@code Location}, center point, usually players location. His Jaw will also be used. Pitch is ignored. (2D)
    * @param radius - {@code int}, radius of the cone (depth)
    * @param degrees - {@code int}, angle of the cone (width of checked FOV in degree with yaw as center vector)
    * @return {@code List<Entity>} - entities in the cone
    */
   // TODO check fails for certain YAW values of the player. Fix that!
   public static List<Entity> getEntitiesInCone(List<Entity> entities, Location startpoint, int radius, int degrees)
   {
      List<Entity> newEntities = new ArrayList<Entity>();

      int[] startPos = new int[] { (int)startpoint.getX(), (int)startpoint.getZ() };

      int[] endA = new int[] { (int)(radius * Math.cos(((int)startpoint.getYaw()) - (degrees / 2))), (int)(radius * Math.sin(((int)startpoint.getYaw()) - (degrees / 2))) };

      for(Entity e : entities)
      {
         Location l = e.getLocation();       
         int[] entityVector = getVectorForPoints(startPos[0], startPos[1], l.getBlockX(), l.getBlockY());

         double angle = getAngleBetweenVectors(endA, entityVector);
         if(Math.toDegrees(angle) < degrees && Math.toDegrees(angle) > 0)
         {
            newEntities.add(e);
            if(CreeperBribe.debug){log.info("Entity in field of view: " + e.getType().toString());}
         }
      }      
      return newEntities;
   }

   // check for a single Entity (modified by CubieX)
   // TODO check fails for certain YAW values of the player. Fix that!
   public static boolean getEntitiesInCone(Entity entity, Location startpoint, int radius, int degrees)
   {
      boolean isInCone = false;

      int[] startPos = new int[] { (int)startpoint.getX(), (int)startpoint.getZ() };
      int[] endA = new int[] { (int)(radius * Math.cos(((int)startpoint.getYaw()) - (degrees / 2))), (int)(radius * Math.sin(((int)startpoint.getYaw()) - (degrees / 2))) };
      Location l = entity.getLocation();       
      int[] entityVector = getVectorForPoints(startPos[0], startPos[1], l.getBlockX(), l.getBlockY());
      double angle = getAngleBetweenVectors(endA, entityVector);

      if(Math.toDegrees(angle) < degrees && Math.toDegrees(angle) > 0)
      {
         isInCone = true;
         if(CreeperBribe.debug){log.info("Entity in field of view: " + entity.getType().toString());}
      }

      return isInCone;
   }

   /**
    * Created an integer vector in 2d between two points
    *
    * @param x1 - {@code int}, X pos 1
    * @param y1 - {@code int}, Y pos 1
    * @param x2 - {@code int}, X pos 2
    * @param y2 - {@code int}, Y pos 2
    * @return {@code int[]} - vector
    */
   public static int[] getVectorForPoints(int x1, int y1, int x2, int y2)
   {
      return new int[] { x2 - x1, y2 - y1 };
   }
   /**
    * Get the angle between two vectors.
    *
    * @param vector1 - {@code int[]}, vector 1
    * @param vector2 - {@code int[]}, vector 2
    * @return {@code double} - angle
    */
   public static double getAngleBetweenVectors(int[] vector1, int[] vector2)
   {
      return Math.atan2(vector2[1], vector2[0]) - Math.atan2(vector1[1], vector1[0]);
   }

   // ### END of kumpelblase2's methods ###########
}


