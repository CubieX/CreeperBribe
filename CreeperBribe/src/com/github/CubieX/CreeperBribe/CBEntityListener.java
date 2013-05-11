package com.github.CubieX.CreeperBribe;

import java.util.HashSet;
import java.util.Random;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;

public class CBEntityListener implements Listener
{
   private CreeperBribe plugin = null;
   private HashSet<String> neutralizedCreepers = new HashSet<String>();
   private final int RAND_UPPER_LIMIT = 101; 

   public CBEntityListener(CreeperBribe plugin)
   {        
      this.plugin = plugin;

      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   // TODO: unneeded event?
   //================================================================================================
   /*@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onPlayerInteractEntity(PlayerInteractEntityEvent event)
   {
      if(CreeperBribe.bribeChance == 0) 
      {
         return;
      }

      if(event.getRightClicked().getType() == EntityType.CREEPER)
      {
         neutralizedCreepers.add(event.getRightClicked().getUniqueId().toString());
         event.getPlayer().sendMessage("Der Creeper " + event.getRightClicked().getUniqueId().toString() + " wurde bestochen und ist nun friedlich!");
      }      
   }*/

   //================================================================================================
   @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
   public void onEntityTargetLivingEntity(EntityTargetLivingEntityEvent event)
   {
      if(CreeperBribe.bribeChance == 0) 
      {
         return;
      }

      if(null != event.getTarget())
      {
         if(neutralizedCreepers.contains(event.getEntity().getUniqueId().toString()) &&
               (event.getTarget().getType() == EntityType.PLAYER))
         {
            // TODO add distinction between "known" players and foreign ones.
            // So Creeper only ignores players who bribed him in the past
            event.setTarget(null); // BUG: This only works if the entity has never before set a target! (bug since new pathfinding AI of MC)
         }
      }
   }

   //================================================================================================
   @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
   public void onExplosionPrime(ExplosionPrimeEvent event)
   {
      if(CreeperBribe.bribeChance == 0) 
      {
         return;
      }

      if(event.getEntityType() == EntityType.CREEPER)
      {
         // get all entities in the explosion radius
         for(Entity ent : event.getEntity().getNearbyEntities(2 * event.getRadius(), 2 * event.getRadius(), 2 * event.getRadius()))
         {
            if(CreeperBribe.debug){CreeperBribe.log.info("Entity in explosion radius found: " + ent.getType().toString());}

            if(ent instanceof Player)
            {
               if(neutralizedCreepers.contains(event.getEntity().getUniqueId().toString()))
               {
                  // if this Creeper is already bribed, cancel the priming 
                  event.setCancelled(true);
                  if(CreeperBribe.debug){((Player) ent).sendMessage("This creeper has already been bribed.");}
                  return;
               }

               Player p = (Player) ent;

               if((p.hasPermission("creeperbribe.bribe")) || (p.hasPermission("creeperbribe.admin")))
               {
                  if(p.getItemInHand().getType() == Material.CAKE)
                  {
                     Random rand = new Random(System.currentTimeMillis());
                     int bribe = rand.nextInt(RAND_UPPER_LIMIT); // will generate a value between 0 and upper limit - 1

                     if(bribe == 0)
                     {  // minimum of 1 is needed to make sure the chance is really 100% when using a configured bribeChance of 100%
                        bribe = 1;
                     }

                     if(CreeperBribe.debug){CreeperBribe.log.info("bribeFactor: " + bribe + " % (>= " + CreeperBribe.bribeChance + " needed for Success.");}                  
                     if(CreeperBribe.debug){p.sendMessage("bribeFactor: " + bribe + " % (>= " + CreeperBribe.bribeChance + " needed for Success.");}

                     if(bribe >= (RAND_UPPER_LIMIT - CreeperBribe.bribeChance))
                     {
                        // bribe was successful. Creeper will no longer attack this player. (clone him, spawn a new one and block his targeting of players)
                        Creeper original = (Creeper) event.getEntity();
                        Creeper clone = p.getWorld().spawn(original.getLocation(), original.getClass());
                        clone.setCustomName(ChatColor.RED + "♥");
                        original.remove();

                        if(null != clone)
                        {
                           clone.setTarget(null);
                           event.setCancelled(true);

                           // consume the bribe item
                           if(p.getItemInHand().getAmount() > 1)
                           {
                              p.getItemInHand().setAmount(p.getItemInHand().getAmount() - 1);
                              p.updateInventory();
                           }
                           else
                           {
                              p.setItemInHand(null);
                              p.updateInventory();
                           }

                           // TODO so machen, dass Ceeper "Wissen" von wem sie bestochen wurden. 
                           // Sie sollen dann nur diese(n) Spieler nicht angreifen.
                           // bzw. nicht explodieren, wenn neben anderen Spielern auch dieser im Explosionsradius ist.

                           // remember that this creeper has been bribed and is now neutralized
                           neutralizedCreepers.add(clone.getUniqueId().toString());

                           if(CreeperBribe.debug){CreeperBribe.log.info("Creeper " + clone.getUniqueId().toString() + "has been bribed and is now friendly.");}

                           p.sendMessage(ChatColor.GREEN + "Du hast diesen Creeper bestochen. Er ist nun friedlich!");
                        }
                     }
                  }
               }
            }
         }
      }        
   }
}
