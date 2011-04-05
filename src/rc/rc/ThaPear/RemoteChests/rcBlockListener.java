package rc.rc.ThaPear.RemoteChests;

import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.SignChangeEvent;

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

		String chestName = plugin.getSignName(event.getLines());
		if(!chestName.equals(""))
		{
			plugin.addChest(chestName, player, event.getBlock().getLocation());
			plugin.addSign((Sign) event.getBlock().getState());
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
