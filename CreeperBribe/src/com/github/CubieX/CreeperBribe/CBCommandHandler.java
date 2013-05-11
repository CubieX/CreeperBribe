package com.github.CubieX.CreeperBribe;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CBCommandHandler implements CommandExecutor
{
   private CreeperBribe plugin = null;
   private CBConfigHandler cHandler = null;

   public CBCommandHandler(CreeperBribe plugin, CBConfigHandler cHandler) 
   {
      this.plugin = plugin;
      this.cHandler = cHandler;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
   {
      Player player = null;

      if (sender instanceof Player) 
      {
         player = (Player) sender;
      }

      if (cmd.getName().equalsIgnoreCase("cbribe"))
      {
         if (args.length == 0)
         { //no arguments, so help will be displayed
            return false;
         }
         
         if (args.length==1)
         {
            if (args[0].equalsIgnoreCase("version")) // argument 0 is given and correct
            {            
               sender.sendMessage(ChatColor.GREEN + "This server is running " + plugin.getDescription().getName() + " version " + plugin.getDescription().getVersion());

               return true;
            }
            
            if (args[0].equalsIgnoreCase("reload")) // argument 0 is given and correct
            {            
               if(sender.hasPermission("creeperbribe.admin"))
               {                        
                  cHandler.reloadConfig(sender);
                  return true;
               }
               else
               {
                  sender.sendMessage(ChatColor.RED + "You do not have sufficient permission to reload " + plugin.getDescription().getName() + "!");
               }
            }
           
            if (args[0].equalsIgnoreCase("help")) // argument 0 is given and correct
            {
               sender.sendMessage(ChatColor.YELLOW + CreeperBribe.logPrefix + "Nutze xxx um einen Creeper zu bestechen.");

               return true;
            }
         }
         else
         {
            sender.sendMessage(ChatColor.YELLOW + "Falsche Parameteranzahl.");
         }                

      }         
      return false; // if false is returned, the help for the command stated in the plugin.yml will be displayed to the player
   }
}
