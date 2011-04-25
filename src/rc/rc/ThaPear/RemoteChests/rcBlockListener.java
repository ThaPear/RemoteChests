package rc.rc.ThaPear.RemoteChests;

import org.bukkit.ChatColor;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.SignChangeEvent;

import com.nijiko.coelho.iConomy.iConomy;
import com.nijiko.coelho.iConomy.system.Account;

public class rcBlockListener extends BlockListener
{
	public static rcPlugin plugin;
	public rcBlockListener(rcPlugin instance)
	{
		plugin = instance;
	}

	@Override
	public void onSignChange(SignChangeEvent event)
	{
		Player player = event.getPlayer();

		String chestName = rcPlugin.getSignName(event.getLines());
		if(!chestName.equals(""))
		{
			chestName = rcPlugin.parseTags(chestName, player);
			if(rcPlugin.Permissions.has(player, rcPlugin.rccreatelinkPerm))
			{
				if(rcPlugin.iConomyOn)
				{
					boolean linkIsFree = (rcPlugin.permissionsOn && rcPlugin.Permissions.has(player, "rc.freelink"));
					boolean chestIsFree = (rcPlugin.permissionsOn && rcPlugin.Permissions.has(player, "rc.freecreate"));
					Account iConAccount = iConomy.getBank().getAccount(player.getName());
					int cost = 0;
					if(!linkIsFree)
						cost += rcPlugin.iConChestLinkPrice;
					if(!chestIsFree && !rcPlugin.chests.containsKey(chestName))
						cost += rcPlugin.iConChestCreatePrice;
					if(!(iConAccount.getBalance() >= cost))
					{
						player.sendMessage(rcPlugin.messagePrefix + ChatColor.RED + "Your do not have enough " + iConomy.getBank().getCurrency() + " to link to this chest");
						player.sendMessage(rcPlugin.messagePrefix + ChatColor.RED + "You need " + iConomy.getBank().format(cost) + ", you have " + iConomy.getBank().format(iConAccount.getBalance()));
						event.setCancelled(true);
					}
					else
					{
						if(!plugin.addChest(player, chestName, event.getBlock().getLocation()))
						{
							plugin.removeSign((Sign) event.getBlock().getState());
							event.setCancelled(true);
						}
						else
						{
							if(!linkIsFree)
							{
								iConAccount.subtract(rcPlugin.iConChestLinkPrice);
								player.sendMessage(rcPlugin.messagePrefix + ChatColor.GREEN + "Linking the sign cost you " + iConomy.getBank().format(rcPlugin.iConChestLinkPrice));
							}
							plugin.addSign((Sign) event.getBlock().getState());
						}
					}
				}
				else
				{
					if(!plugin.addChest(player, chestName, event.getBlock().getLocation()))
					{
						plugin.removeSign((Sign) event.getBlock().getState());
						event.setCancelled(true);
					}
					else
						plugin.addSign((Sign) event.getBlock().getState());
				}
				
			}
			else
			{
				player.sendMessage(rcPlugin.messagePrefix + ChatColor.RED + "You do not have permission to link chests.");
				event.setCancelled(true);
			}
		}
		else
		{
			plugin.removeSign((Sign) event.getBlock().getState());
		}
	}

	public void onBlockBreak(BlockBreakEvent event)
	{
		if(event.getBlock().getState() instanceof Sign)
			plugin.removeSign((Sign) event.getBlock().getState());
	}
}
