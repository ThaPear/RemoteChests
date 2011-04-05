package rc.rc.ThaPear.RemoteChests;

import org.bukkit.event.world.WorldListener;
import org.bukkit.event.world.WorldSaveEvent;

public class rcWorldListener extends WorldListener
{
	public static rcPlugin plugin;
	public rcWorldListener(rcPlugin instance)
	{
		plugin = instance;
	}
	@Override
	public void onWorldSave(WorldSaveEvent event)
	{
		plugin.saveChests();
	}
	
}
