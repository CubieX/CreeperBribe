package com.github.CubieX.CreeperBribe;

public class CBSchedulerHandler
{
   private CreeperBribe plugin = null;

   public CBSchedulerHandler(CreeperBribe plugin)
   {
      this.plugin = plugin;
   }

   public void startPlayerInWaterCheckScheduler_SynchRepeating()
   {
      plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable()
      {
         public void run()
         {
                    
         }
      }, 10 * 20L, 1 * 20L); // 10 seconds delay, 1 second cycle
   }
}
