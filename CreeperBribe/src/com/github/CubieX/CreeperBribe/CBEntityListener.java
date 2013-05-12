package com.github.CubieX.CreeperBribe;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class CBEntityListener implements Listener
{
   private CreeperBribe plugin = null;
   private HashMap<String, String> neutralizedCreepers = new HashMap<String, String>();
   private HashSet<String> angryCreepers = new HashSet<String>();
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
         if(neutralizedCreepers.containsKey(event.getEntity().getUniqueId().toString()) &&
               (event.getTarget() instanceof Player))
         {
            Player targetedPlayer = (Player) event.getTarget();

            // check if the creepers briber is the target, or if the briber is in the direct vicinity of the targeted player (= within explosion radius)
            if((targetedPlayer.getName().equals(neutralizedCreepers.get(event.getEntity().getUniqueId().toString()))) ||
                  (targetedPlayerIsNearBriber(event.getEntity().getUniqueId().toString(), (Entity)event.getTarget(), 3))) // Creepers explosion radius is 3 (MC default)
            {  // neutralized creeper has tried to target his briber, or a player within the explosion radius while the briber is present in the radius
               // so cancel targeting to not hurt the briber
               event.setTarget(null); // BUG: This only works if the entity has never before set a target! (bug since new pathfinding AI of MC)  
            }
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
         for(Entity eNearPrimingCreeper : event.getEntity().getNearbyEntities(event.getRadius(), event.getRadius(), event.getRadius()))
         {
            if(CreeperBribe.debug){CreeperBribe.log.info("Entity in explosion radius found: " + eNearPrimingCreeper.getType().toString());}

            Player pNearPrimingCreeper = null;

            if(eNearPrimingCreeper instanceof Player)
            {
               pNearPrimingCreeper = (Player) eNearPrimingCreeper;

               if((neutralizedCreepers.containsKey(event.getEntity().getUniqueId().toString())) &&
                     (pNearPrimingCreeper.getName().equals(neutralizedCreepers.get(event.getEntity().getUniqueId().toString()))))
               { // this Creeper is already bribed, so cancel the priming if the briber is within the explosion radius                  
                  event.setCancelled(true);
                  if(CreeperBribe.debug){pNearPrimingCreeper.sendMessage("Dieser Creeper wurde von dir bestochen und ist dein Freund.");}
                  return;
               }
               else
               { // this creeper has not been bribed until now or is angry
                  if(angryCreepers.contains(event.getEntity().getUniqueId().toString()))
                  { // he is angry, so apply special effects fefore explosion goes off
                     event.setRadius(CreeperBribe.angryExplosionRadius);

                     if(CreeperBribe.nauseaDuration > 0)
                     {
                        // give all players in explosion range a wobble effect
                        pNearPrimingCreeper.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 20 * CreeperBribe.nauseaDuration, 1));
                     }

                     if(CreeperBribe.blindnessDuration > 0)
                     {
                        // give all players in explosion range a blindness effect
                        pNearPrimingCreeper.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * CreeperBribe.blindnessDuration, 1));
                     }
                  }
                  else
                  { // he has never been bribed                     
                     if((pNearPrimingCreeper.hasPermission("creeperbribe.bribe")) || (pNearPrimingCreeper.hasPermission("creeperbribe.admin")))
                     {
                        if(pNearPrimingCreeper.getItemInHand().getType() == Material.CAKE)
                        {
                           // TODO check fails for certain YAW values of the player. Fix that!
                           /*if(CreeperBribe.getEntitiesInCone(event.getEntity(), pNearPrimingCreeper.getLocation(), (int)(2 * event.getRadius()), 120)) // radius must be > 1
                           { // player sees the creeper in front of him
                              if(CreeperBribe.debug){pNearPrimingCreeper.sendMessage("Creeper gesichtet der dich angreift. Versuche Bestechung...");}
                            */
                           Random rand = new Random(System.currentTimeMillis());
                           int bribe = rand.nextInt(RAND_UPPER_LIMIT); // will generate a value between 0 and upper limit - 1

                           if(bribe == 0)
                           {  // minimum of 1 is needed to make sure the chance is really 100% when using a configured bribeChance of 100%
                              bribe = 1;
                           }

                           if(CreeperBribe.debug){CreeperBribe.log.info("bribeFactor: " + bribe + " % ( >= " + CreeperBribe.bribeChance + " ) needed for Success.");}                  
                           if(CreeperBribe.debug){pNearPrimingCreeper.sendMessage(ChatColor.WHITE + "bribeFactor: " + ChatColor.GREEN + bribe + ChatColor.WHITE + " % ( >= " + CreeperBribe.bribeChance + " ) needed for Success.");}

                           if(bribe >= (RAND_UPPER_LIMIT - CreeperBribe.bribeChance))
                           {
                              // bribe was successful. Creeper will no longer attack this player. (clone him, spawn a new one and block his targeting of players)
                              Creeper original = (Creeper) event.getEntity();
                              Creeper clone = pNearPrimingCreeper.getWorld().spawn(original.getLocation(), original.getClass());
                              clone.setCustomName(ChatColor.RED + "♥ ♥ ♥");
                              original.remove();

                              if(null != clone)
                              {
                                 clone.setTarget(null);
                                 event.setCancelled(true);

                                 // consume the bribe item
                                 if(pNearPrimingCreeper.getItemInHand().getAmount() > 1)
                                 {
                                    pNearPrimingCreeper.getItemInHand().setAmount(pNearPrimingCreeper.getItemInHand().getAmount() - 1);
                                    pNearPrimingCreeper.updateInventory();
                                 }
                                 else
                                 {
                                    pNearPrimingCreeper.setItemInHand(null);
                                    pNearPrimingCreeper.updateInventory();
                                 }

                                 // remember that this creeper has been bribed and is now neutralized
                                 neutralizedCreepers.put(clone.getUniqueId().toString(), pNearPrimingCreeper.getName()); // the creeper is the key, because a creeper can only be bribed by one player

                                 if(CreeperBribe.debug){CreeperBribe.log.info("Creeper " + clone.getUniqueId().toString() + " has been bribed and is now friendly.");}

                                 pNearPrimingCreeper.sendMessage(ChatColor.GREEN + "Du hast diesen Creeper bestochen. Er ist nun friedlich!");
                              }
                           }
                           /*}
                           else
                           {
                              if(CreeperBribe.debug){pNearPrimingCreeper.sendMessage("Du hast den Creeper nicht kommen sehen!");}
                           }*/
                        }
                     }                    
                  }
               }
            }
         }
      }        
   }

   //================================================================================================
   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onEntityDamageByEntity(EntityDamageByEntityEvent event)
   {
      if(neutralizedCreepers.containsKey(event.getEntity().getUniqueId().toString()) &&
            (event.getDamager() instanceof Player))
      {
         // delete the bribed creeper that has been it from the list, to reactivate his killer instincts
         neutralizedCreepers.remove(event.getEntity().getUniqueId().toString());
         angryCreepers.add(event.getEntity().getUniqueId().toString()); // creeper is now angry!

         Player damager = (Player) event.getDamager();
         damager.sendMessage(ChatColor.GOLD + "Du hast den Creeper wuetend gemacht!");
      }
   }

   //================================================================================================
   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onEntityDeath(EntityDeathEvent event)
   {      
      if(neutralizedCreepers.containsKey(event.getEntity().getUniqueId().toString()))
      {
         // delete the bribed creeper from the list to tidy up
         neutralizedCreepers.remove(event.getEntity().getUniqueId().toString());
      }

      if(angryCreepers.contains(event.getEntity().getUniqueId().toString()))
      {
         // delete the angry creeper from the list to tidy up         
         angryCreepers.remove(event.getEntity().getUniqueId().toString());
      }
   }

   boolean targetedPlayerIsNearBriber(String targetingCreeperUUID, Entity target, int explosionRadius)
   {
      boolean res = false;

      for(Entity eNearTarget : target.getNearbyEntities(explosionRadius, explosionRadius, explosionRadius))
      {
         if(eNearTarget instanceof Player)
         {
            Player pNearTarget = (Player) eNearTarget;

            if(pNearTarget.getName().equals(neutralizedCreepers.get(targetingCreeperUUID)))
            { // briber is near targeted player (within explosion radius)
               res = true;
            }
         }
      }

      return res;
   }
}
