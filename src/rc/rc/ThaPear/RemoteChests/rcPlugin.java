package rc.rc.ThaPear.RemoteChests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import net.minecraft.server.EntityPlayer;
import net.minecraft.server.InventoryLargeChest;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;

import rc.rc.ThaPear.RemoteChests.rcBlockListener;
import rc.rc.ThaPear.RemoteChests.rcPlayerListener;
import rc.rc.ThaPear.RemoteChests.rcChest;
import rc.rc.ThaPear.RemoteChests.rcFile;
// Permissions
import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;
import org.bukkit.plugin.Plugin;
// iConomy
import com.nijiko.coelho.iConomy.iConomy;
import com.nijiko.coelho.iConomy.system.Account;

public class rcPlugin extends JavaPlugin
{
	/**
	 * Permissions stuff
	 */
	static boolean permissionsOn = false;
	public  static PermissionHandler Permissions;
	private static String rcadminPerm = "rc.admin";
	public  static String rcfreeCreatePerm = "rc.freecreate";
	public  static String rcfreeLinkPerm = "rc.freelink";
	private static String rcunlimitedAmountPerm = "rc.unlimitedamount";
	public  static String rclinkopenPerm = "rc.linkedopen"; // open linked chests
	public  static String rccreatelinkPerm = "rc.createlink";
	private static String rcopenPerm = "rc.open";
	private static String rccreatePerm = "rc.create";
	private static String rcremovePerm = "rc.remove";
	private static String rcrenamePerm = "rc.rename";
	private static String rcsortPerm = "rc.sort";
	private static String rcstackPerm = "rc.stack";
	private static String rcmergePerm = "rc.merge";
	private static String rclistPerm = "rc.list";

	/**
	 * iConomy stuff
	 */
	static boolean iConomyOn = false;
	private static iConomy iConomyHandle = null;

	/**
	 * Settings
	 */
	public static BlockFace signDir = BlockFace.UP;
	static String tagPlayer = "[player]";
	static int maxChestsPP = 10;
	static int iConChestCreatePrice = 0;
	static int iConChestLinkPrice = 0;

	public  static final Logger log = Logger.getLogger("Minecraft");
	public  static String messagePrefix = "[RemoteChests] ";
	public  static HashMap<String, InventoryLargeChest> chests = new HashMap<String, InventoryLargeChest>();
	private static ArrayList<Sign> signs = new ArrayList<Sign>();
	private static HashMap<String, String> chestOwners = new HashMap<String, String>();
	private static HashMap<String, ArrayList<String>> chestAllowed = new HashMap<String, ArrayList<String>>();
	private static HashMap<String, Integer> numChestsOwned = new HashMap<String, Integer>();

	/**
	 * Handles all player related events.
	 */
	private final rcPlayerListener playerListener = new rcPlayerListener(this);
	/**
	 * Handles all block related events.
	 */
	private final rcBlockListener blockListener = new rcBlockListener(this);
	/**
	 * Handles all world related events.
	 */
	private final rcWorldListener worldListener = new rcWorldListener(this);
	/**
	 * Handles all file interaction.
	 */
	private final rcFile fileIO = new rcFile(this);

	public rcPlugin()
	{
		super();
	}

	/**
	 * Gets called when the plugin is enabled
	 * (so server start/reload)
	 */
	@Override
	public void onEnable()
	{
		// Register our events
		PluginManager pm = getServer().getPluginManager();
		//pm.registerEvent(Event.Type.BLOCK_PLACE, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.SIGN_CHANGE, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.WORLD_SAVE, worldListener, Event.Priority.Normal, this);
	
		
		// Permissions support
		setupPermissions();
		// iConomy support
		setupiConomy();
		
		fileIO.removeObsolete();
		
		chests = fileIO.loadChests();
		chestAllowed = fileIO.loadAllowed();
		chestOwners = fileIO.loadOwners();
		fileIO.loadSettings();
		initNumChestsOwned();
		
		PluginDescriptionFile pdfFile = this.getDescription();
		System.out.println( messagePrefix + pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!" );
	}
	private void initNumChestsOwned()
	{
		for(String chestName : chestOwners.keySet())
		{
			String owner = chestOwners.get(chestName);
			int numOwned = 0;
			if(rcPlugin.numChestsOwned.containsKey(owner))
			{
				numOwned = numChestsOwned.get(owner);
			}
			numChestsOwned.put(owner, numOwned+1);
		}
	}

	/**
	 * Gets called when the plugin is disabled
	 * (so server stop/reload)
	 */
	@Override
	public void onDisable()
	{
		// Save everything
		saveStuff();

		PluginDescriptionFile pdfFile = this.getDescription();
		System.out.println( messagePrefix + pdfFile.getName() + " version " + pdfFile.getVersion() + " is disabled!" );
	}
	/**
	 * Save the chests without outputting anything to the console.
	 */
	public void saveStuff()
	{
		fileIO.saveChests(chests);
		fileIO.saveOwners(chestOwners);
		fileIO.saveAllowed(chestAllowed);
	}
	/**
	 * Processes all commands sent from in-game
	 * New commands need to be added to plugin.yml as well
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
	{
		if(!(sender instanceof Player))
		{
			sender.sendMessage( messagePrefix + ChatColor.RED + "Only players can use this command!");
			return true;
		}
		Player player = (Player)sender;

		if ( cmd.getName().equalsIgnoreCase("rcopen") || cmd.getName().equalsIgnoreCase("rcopenchest") )
		{
			if(permissionsOn && !rcPlugin.Permissions.has(player, rcopenPerm))
				player.sendMessage(messagePrefix + ChatColor.RED + "You do not have permission to use " + cmd.getName() + ".");
			else
				openChest(player, args[0]);
			return true;
		}
		else if ( cmd.getName().equalsIgnoreCase("rccreate") || cmd.getName().equalsIgnoreCase("rccreatechest") )
		{
			if(permissionsOn && !rcPlugin.Permissions.has(player, rccreatePerm))
				player.sendMessage(messagePrefix + ChatColor.RED + "You do not have permission to use " + cmd.getName() + ".");
			else
				createChest(player, args[0]);
			return true;
		}
		else if ( cmd.getName().equalsIgnoreCase("rcremove") )
		{
			if(permissionsOn && !rcPlugin.Permissions.has(player, rcremovePerm))
				player.sendMessage(messagePrefix + ChatColor.RED + "You do not have permission to use " + cmd.getName() + ".");
			else
				removeChest(player, args[0]);
			return true;
		}
		else if ( cmd.getName().equalsIgnoreCase("rcmerge") )
		{
			if(args[0].contains("?"))
				dispMergeHelp(player);
			else
				if(permissionsOn && !rcPlugin.Permissions.has(player, rcmergePerm))
					player.sendMessage(messagePrefix + ChatColor.RED + "You do not have permission to use " + cmd.getName() + ".");
				else
					mergeChests(player, args);
			return true;
		}
		else if ( cmd.getName().equalsIgnoreCase("rcrename") )
		{
			if(permissionsOn && !rcPlugin.Permissions.has(player, rcrenamePerm))
				player.sendMessage(messagePrefix + ChatColor.RED + "You do not have permission to use " + cmd.getName() + ".");
			else 
				renameChest(player, args);
			return true;
		}
		else if ( cmd.getName().equalsIgnoreCase("rcstack") )
		{
			if(permissionsOn && !rcPlugin.Permissions.has(player, rcstackPerm))
				player.sendMessage(messagePrefix + ChatColor.RED + "You do not have permission to use " + cmd.getName() + ".");
			else if(stackChest(player, args[0]))
				player.sendMessage(messagePrefix + ChatColor.GREEN + "Chest \"" + args[0] + "\" stacked succesfully.");
			return true;
		}
		else if ( cmd.getName().equalsIgnoreCase("rcsort") )
		{
			if(permissionsOn && !rcPlugin.Permissions.has(player, rcsortPerm))
				player.sendMessage(messagePrefix + ChatColor.RED + "You do not have permission to use " + cmd.getName() + ".");
			else if(sortChest(player, args[0]))
				player.sendMessage(messagePrefix + ChatColor.GREEN + "Chest \"" + args[0] + "\" sorted succesfully.");
			return true;
		}
		else if ( cmd.getName().equalsIgnoreCase("rclist") )
		{
			if(permissionsOn && !rcPlugin.Permissions.has(player, rclistPerm))
				player.sendMessage(messagePrefix + ChatColor.RED + "You do not have permission to use " + cmd.getName() + ".");
			else
				listChests(player, args);
			return true;
		}
		else if ( cmd.getName().equalsIgnoreCase("rcallow") )
		{
			if(args.length > 1)
				addChestAllowance(player, args[0], args[1]);
			else
				player.sendMessage(messagePrefix + ChatColor.RED + "Please specify a player name and a chest name");
			return true;
		}
		else if ( cmd.getName().equalsIgnoreCase("rcdisallow") )
		{
			if(args.length > 1)
				removeChestAllowance(player, args[0], args[1]);
			else
				player.sendMessage(messagePrefix + ChatColor.RED + "Please specify a player name and a chest name");
			return true;
		}
		else if ( cmd.getName().equalsIgnoreCase("rcsetowner") )
		{
			if(args.length > 1)
				setChestOwner(player, args[0], args[1]);
			else
				player.sendMessage(messagePrefix + ChatColor.RED + "Please specify a player name and a chest name");
			return true;
		}
		else if ( cmd.getName().equalsIgnoreCase("rchelp"))
		{
			ChatColor clr = ChatColor.WHITE;
			player.sendMessage(ChatColor.YELLOW + messagePrefix + "---------------------------------------");
			
			player.sendMessage(ChatColor.GREEN + "Possible commands:");
			if(permissionsOn && !rcPlugin.Permissions.has(player, rcopenPerm))	clr = ChatColor.RED;
			else																clr = ChatColor.WHITE;
			player.sendMessage(clr + "/rcopen <chestname> - Open a chest remotely");
			if(permissionsOn && !rcPlugin.Permissions.has(player, rccreatePerm))clr = ChatColor.RED;
			else																clr = ChatColor.WHITE;
			player.sendMessage(clr + "/rccreate <chestname> - Create a virtual chest manually");
			if(iConomyOn)
				player.sendMessage(ChatColor.RED + "This will cost you " + iConomy.getBank().format(iConChestCreatePrice));
			if(permissionsOn && !rcPlugin.Permissions.has(player, rcremovePerm))clr = ChatColor.RED;
			else																clr = ChatColor.WHITE;
			player.sendMessage(clr + "/rcremove <chest1name> - Remove specified chest");
			if(permissionsOn && !rcPlugin.Permissions.has(player, rcrenamePerm))clr = ChatColor.RED;
			else																clr = ChatColor.WHITE;
			player.sendMessage(clr + "/rcrename <oldname> <newname> - Rename specified chest");
			if(permissionsOn && !rcPlugin.Permissions.has(player, rcstackPerm))	clr = ChatColor.RED;
			else																clr = ChatColor.WHITE;
			player.sendMessage(clr + "/rcstack <chest1name> - Stack the contents of a chest");
			if(permissionsOn && !rcPlugin.Permissions.has(player, rcsortPerm))	clr = ChatColor.RED;
			else																clr = ChatColor.WHITE;
			player.sendMessage(clr + "/rcsort <chest1name> - Sort the contents of a chest");
			if(permissionsOn && !rcPlugin.Permissions.has(player, rclistPerm))	clr = ChatColor.RED;
			else																clr = ChatColor.WHITE;
			player.sendMessage(clr + "/rclist - List all existing chests");
			if(permissionsOn && !rcPlugin.Permissions.has(player, rcmergePerm))	clr = ChatColor.RED;
			else																clr = ChatColor.WHITE;
			player.sendMessage(clr + "/rcmerge <chest1name> <chest2name> [<newname>] [<flags>]");
			player.sendMessage("Merge 2 chests, Type /rcmerge ? for more info");


			player.sendMessage("Allowing players to access your chest");
			player.sendMessage("/rc(dis)allow <playername> <chestname>");
			player.sendMessage("/rcsetowner <playername> <chestname>");
			if(permissionsOn && !rcPlugin.Permissions.has(player, rclinkopenPerm))	clr = ChatColor.RED;
			else																	clr = ChatColor.GREEN;
			player.sendMessage(clr + "Chest linking:");
			player.sendMessage("Place a sign above a chest to link it to a virtual chest");
			player.sendMessage("Specify its target with [rc] <chestname> on any of the lines");
			
			player.sendMessage(ChatColor.YELLOW + messagePrefix + "---------------------------------------");
			return true;
		}
		return false;
	}

	/**
	 * Gets all player names containing specified part.
	 */
	public String[] getFullPlayerNames(String part)
	{
		ArrayList<String> nameList = new ArrayList<String>();
		for(Player player : getServer().getOnlinePlayers())
		{
			String playerName = player.getName();
			if(playerName.toLowerCase().contains(part.toLowerCase()))
				nameList.add(playerName);
		}

		String[] names = (String[])nameList.toArray(new String[0]);
		
		return names;
	}

	private void setChestOwner(Player sender, String namePart, String chestName)
	{
		if(!chests.containsKey(chestName))	
		{	sender.sendMessage(messagePrefix + ChatColor.RED + "Chest \"" + chestName + "\" doesn't exist");			return;	}
		if(isChestOwner(sender, chestName) || isAdmin(sender))
		{
			String[] possPlayers = getFullPlayerNames(namePart);
			
			if(possPlayers.length > 1)
			{
				sender.sendMessage(messagePrefix + ChatColor.RED + "Multiple players match \"" + namePart + "\":");
				for(String plrName : possPlayers)
					sender.sendMessage(plrName);
				sender.sendMessage(ChatColor.RED + "Please be more specific");
				return;
			}
			if(possPlayers.length < 1)
			{
				sender.sendMessage(messagePrefix + ChatColor.RED + "No online players match \"" + namePart + "\"");
				return;
			}
			if(chestOwners.containsKey(chestName))
			{
				int numOwned = 0;
				String oldOwner = chestOwners.get(chestName);
				if(numChestsOwned.containsKey(oldOwner))
				{
					numOwned = numChestsOwned.get(oldOwner);
				}
				numChestsOwned.put(oldOwner, numOwned);
			}

			String newOwner = possPlayers[0].toLowerCase();
			int numOwned = 0;
			if(numChestsOwned.containsKey(newOwner))
			{
				numOwned = numChestsOwned.get(newOwner);
			}
			numChestsOwned.put(newOwner, numOwned+1);
			
			
			chestOwners.put(chestName, newOwner);
			getServer().getPlayer(newOwner).sendMessage(messagePrefix + ChatColor.GREEN + "You are now the owner of chest " + chestName);
		}
	}
	/**
	 * Set the owner of a chest.
	 */
	private void setChestOwner(Player player, String chestName)
	{
		chestOwners.put(chestName, player.getName().toLowerCase());
		player.sendMessage(messagePrefix + ChatColor.GREEN + "You are now the owner of chest " + chestName);
	}
	/**
	 * Checks if specified player is owner of specified chest.
	 */
	private boolean isChestOwner(Player player, String chestName)
	{
		if(isAdmin(player) || chestOwners.containsKey(chestName) && chestOwners.get(chestName).equalsIgnoreCase(player.getName()))
			return true;
		return false;
	}
	/**
	 * Checks if specified player is admin
	 */
	private boolean isAdmin(Player player)
	{
		if(permissionsOn)
			return rcPlugin.Permissions.has(player, rcadminPerm);
		else
			return player.isOp();
	}
	/**
	 * Add a player to the list of allowed openers.
	 */
	private void addChestAllowance(Player sender, String namePart, String chestName)
	{
		if(!chests.containsKey(chestName))	
			{	sender.sendMessage(messagePrefix + ChatColor.RED + "Chest \"" + chestName + "\" doesn't exist");			return;	}
		if(!isChestOwner(sender, chestName))
			{	sender.sendMessage(messagePrefix + ChatColor.RED + "You are not the owner of chest \"" + chestName + "\"");	return;	}
		String[] possPlayers = getFullPlayerNames(namePart);
		
		if(possPlayers.length > 1)
		{
			sender.sendMessage(messagePrefix + ChatColor.RED + "Multiple players match \"" + namePart + "\":");
			for(String plrName : possPlayers)
				sender.sendMessage(plrName);
			sender.sendMessage(ChatColor.RED + "Please be more specific");
			return;
		}
		if(possPlayers.length < 1)
		{
			sender.sendMessage(messagePrefix + ChatColor.RED + "No online players match \"" + namePart + "\"");
			return;
		}
		
		String playerName = possPlayers[0];
		
		if(chestAllowed.containsKey(chestName))
		{
			chestAllowed.get(chestName).add(playerName);
		}
		else
		{
			ArrayList<String> nameList = new ArrayList<String>();
			nameList.add(playerName);
			chestAllowed.put(chestName, nameList);
		}
		sender.sendMessage(messagePrefix + ChatColor.GREEN + "Added player " + playerName + " to allowed players");
	}
	/**
	 * Remove a player from the list of allowed openers.
	 */
	private void removeChestAllowance(Player sender, String namePart, String chestName)
	{
		if(!chests.containsKey(chestName))	
			{	sender.sendMessage(messagePrefix + ChatColor.RED + "Chest \"" + chestName + "\" doesn't exist");			return;	}
		if(!isChestOwner(sender, chestName))
			{	sender.sendMessage(messagePrefix + ChatColor.RED + "You are not the owner of chest \"" + chestName + "\"");	return;	}
		String[] possPlayers = getFullPlayerNames(namePart);
		
		if(possPlayers.length > 1)
		{
			sender.sendMessage(messagePrefix + ChatColor.RED + "Multiple players match \"" + namePart + "\":");
			for(String plrName : possPlayers)
				sender.sendMessage(plrName);
			sender.sendMessage(ChatColor.RED + "Please be more specific");
			return;
		}
		if(possPlayers.length < 1)
		{
			sender.sendMessage(messagePrefix + ChatColor.RED + "No online players match \"" + namePart + "\"");
			return;
		}
		
		String playerName = possPlayers[0];
		
		if(chestAllowed.containsKey(chestName) && chestAllowed.get(chestName).remove(playerName))
			sender.sendMessage(messagePrefix + ChatColor.GREEN + "Removed player " + playerName + " from allowed players");
		else
			sender.sendMessage(messagePrefix + ChatColor.RED + "Player " + playerName + " was not allowed");
	}
	/**
	 * Checks if specified player is allowed to open specified chest.
	 */
	private boolean hasChestAllowance(Player player, String chestName)
	{
		if(!chestAllowed.containsKey(chestName))
		{
			return false;
		}
		ArrayList<String> allowedList = chestAllowed.get(chestName);
		
		String[] allowed = (String[])allowedList.toArray(new String[0]);
		
		for(String name : allowed)
		{
			if(name.toLowerCase().equals(player.getName().toLowerCase()))
				return true;
		}
		return false;
		
	}

	/**
	 * Lists all chests in existence
	 */
	private void listChests(Player player, String[] args)
	{
		int page = 0;
		if(args.length > 0)
			page = Integer.parseInt(args[0])-1; // 0 based in code, 1 based for users
		int numChests = chests.keySet().size();
		int startPos = 0;
		if(numChests > 18)
			startPos = Math.min(numChests - 18, page*18);
		int curPos = 0;
		
		player.sendMessage(messagePrefix + " Listing " + Math.min(18, numChests) + " out of " + numChests + " chests");

		ChatColor clr = ChatColor.RED;
		for(String name : chests.keySet())
		{
			curPos++;
			if(curPos <= startPos)
			{
				continue;
			}
			if((chestOwners.containsKey(name) && chestOwners.get(name).toLowerCase() == player.getName().toLowerCase()) || isAdmin(player))
				clr = ChatColor.GREEN;
			else
			{
				if(chestAllowed.containsKey(name))
					for(String plrName : (String[])chestAllowed.get(name).toArray(new String[0]))
					{
						if(plrName.toLowerCase().equals(player.getName().toLowerCase()))
						{
							clr = ChatColor.WHITE;
							break;
						}
						else
							clr = ChatColor.RED;
					}
			}
			player.sendMessage(clr + name);
			if(curPos >= startPos + 18)
			{
				if(numChests > 18)
					player.sendMessage(messagePrefix + " And " + (numChests-18) + " more, specify page nr to see more");
				break;
			}
		}
	}
	/**
	 * Sorts the contents of a chest based on ID code
	 */
	private boolean sortChest(Player player, String chestName)
	{
		if(chestName.equals(""))			{	player.sendMessage(messagePrefix + ChatColor.RED + "Please specify the name of the chest.");			return false;	}
		chestName = parseTags(chestName, player);
		if(!chests.containsKey(chestName))	{	player.sendMessage(messagePrefix + ChatColor.RED + "Chest \"" + chestName + "\" doesn't exist.");		return false;	}
		if(!isChestOwner(player, chestName)){	player.sendMessage(messagePrefix + ChatColor.RED + "You are not the owner of \"" + chestName + "\"");	return false;	}
//		System.out.println( messagePrefix + "Sorting chest \"" + chestName + "\"." );
		InventoryLargeChest chest = chests.get(chestName);

		int swapStackI = 0;
		net.minecraft.server.ItemStack swapStack;
		
		for(int index = 0; index < 54; index++)
		{
			net.minecraft.server.ItemStack stack = chest.c_(index);
			swapStack = stack;
			swapStackI = index;
			for(int i = index+1; i < 54; i++)
			{
				net.minecraft.server.ItemStack stack2 = chest.c_(i);
				if((stack2 != null && stack2.count != 0 && stack2.id != 0) && 
						(swapStack == null || stack2.id < swapStack.id))
				{
					swapStackI = i;
					swapStack = stack2;
				}
			}
			
			if(swapStack != null)
			{
				if(swapStack != stack)
				{
//					if(stack != null)
//						System.out.println( "Swapping, ids: " + stack.id + "/" + swapStack.id + " at indexes " + index + "/" + swapStackI + "." );
//					else
//						System.out.println( "Swapping, ids: null/" + swapStack.id + " at indexes " + index + "/" + swapStackI + "." );
					chest.a(index, swapStack);
					chest.a(swapStackI, stack);
				}
//				else
//				{
//					System.out.println( "Not swapping, ids: " + stack.id + "/" + swapStack.id + " at indexes " + index + "/" + swapStackI + "." );
//				}
			}
			else
			{
//				System.out.println( messagePrefix + "Sorting done after " + index + " elements." );
				break;
			}
		}
		//chest.h();
		return true;
	}
	/**
	 * Stacks the contents of a chest.
	 */
	private boolean stackChest(Player player, String chestName)
	{
		if(chestName.equals(""))			{	player.sendMessage(messagePrefix + ChatColor.RED + "Please specify the name of the chest.");			return false;	}
		chestName = parseTags(chestName, player);
		if(!chests.containsKey(chestName))	{	player.sendMessage(messagePrefix + ChatColor.RED + "Chest \"" + chestName + "\" doesn't exist.");		return false; }
		if(!isChestOwner(player, chestName)){	player.sendMessage(messagePrefix + ChatColor.RED + "You are not the owner of \"" + chestName + "\"");	return false;	}
		InventoryLargeChest chest = chests.get(chestName);
		for(int index = 0; index < 54; index++)
		{
			net.minecraft.server.ItemStack stack = chest.c_(index);
			if(stack != null && stack.count != 0 && stack.id != 0)
			{
				//player.sendMessage("Trying to merge stack: " + index);
				int i = 0;
				for(net.minecraft.server.ItemStack stack2 : chest.getContents())
				{
					// stack2.b() gets max stack size
					if(stack2 != null && i != index && stack2.count != 0 && stack2.count < stack2.b() && stack2.id == stack.id)
					{
						//player.sendMessage("Found viable target stack: " + i + ", sizes: " + stack.count + ", " + stack2.count);
						stack.count = Math.min(stack2.b(), stack.count + stack2.count );
						chest.a(index, stack);
						stack2.count = Math.max(0,	stack.count + stack2.count - stack2.b());
						//player.sendMessage("New Sizes: " + stack.count + ", " + stack2.count);
						if(stack2.count > 0)
						{
							chest.a(i, stack2);
							break;
						}
						else
							chest.a(i, null);
					}
					i++;
				}
			}
			index++;
		}
		//chest.h();
		return true;
	}
	/**
	 * Displays help with chest merging.
	 */
	private void dispMergeHelp(Player player)
	{
		player.sendMessage("/rcmerge <chest1name> <chest2name> [<newname>] [<flags>]");
		player.sendMessage("Merge 2 chests, [arg] = Optional argument");
		player.sendMessage("When no new name is specified, <chest1name> is used");
		player.sendMessage("Flags:");
		player.sendMessage("I	 - Ignore overfilled chests");
		player.sendMessage("SWF - Auto-stack chests if overfilled");
		player.sendMessage("O	 - Auto-overwrite target chest");
		player.sendMessage("AM	- Auto-merge with target chest");
		player.sendMessage("S	 - Auto-stack chests before merging");
		player.sendMessage("SS	- Auto-stack chests before & after merging");
	}
	/**
	 * Merges 2 chests, not ignoring help messages.
	 */
	private boolean mergeChests(Player player, String[] args)
	{
		return mergeChests(player, args, false);
	}
	/**
	 * Merges 2 chests.
	 * Flag I causes it to ignore if the total amount of items in the 2 chests exceeds 54
	 * Flag SWF causes it to attempt to fix the over filled issue by pre-stacking all items in the 2 chests.
	 * Flag O makes it remove the target chest first
	 * Flag AM makes it merge 1 of the chests with the target chest first
	 * Flag S stacks the chests before merging
	 * Flag SS stacks the chests after merging as well.
	 * 
	 * ignoreHelp makes it not print any help when errors occur.
	 */
	private boolean mergeChests(Player player, String[] args, boolean ignoreHelp)
	{
		String newName;
		String name1;
		String name2;
		boolean ignoreOverfilled = false;
		boolean autoMergeTarget = false;
		boolean autoOverwriteTarget = false;
		boolean autoStackBefore = false;
		boolean autoStackAfter = false;
		boolean stackWhenOverfilled = false;
		
		if(args.length < 2 || args[0].equals("") || args[1].equals(""))
		{
			player.sendMessage(ChatColor.RED + "Incorrect usage of /rcmerge, it is used like this:");
			dispMergeHelp(player);
				return false;
		}
		name1 = parseTags(args[0], player);
		name2 = parseTags(args[1], player);
		if(!chests.containsKey(name1))	{	player.sendMessage(messagePrefix + ChatColor.RED + "Chest \"" + name1 + "\" doesn't exist.");		return false;	}
		if(!chests.containsKey(name2))	{	player.sendMessage(messagePrefix + ChatColor.RED + "Chest \"" + name2 + "\" doesn't exist.");		return false;	}
		
		if(!isChestOwner(player, name1)){	player.sendMessage(messagePrefix + ChatColor.RED + "You are not the owner of \"" + name1 + "\"");	return false;	}
		if(!isChestOwner(player, name2)){	player.sendMessage(messagePrefix + ChatColor.RED + "You are not the owner of \"" + name2 + "\"");	return false;	}
		
		if(args.length > 3)
		{
			stackWhenOverfilled = args[3].toLowerCase().contains("swf" );
			args[3].replaceFirst("swf", "	 ");
			ignoreOverfilled 	= args[3].toLowerCase().contains("i" );
			autoMergeTarget 	= args[3].toLowerCase().contains("am");
			autoOverwriteTarget = args[3].toLowerCase().contains("o" );
			autoStackAfter 		= args[3].toLowerCase().contains("ss");
			autoStackBefore 	= args[3].toLowerCase().contains("s" );
			
		}
		
		if(args.length > 2 && !args[2].equals(""))
		{
			newName = parseTags(args[2], player);
			if(newName != name1 && newName != name2 && chests.containsKey(newName))
			{
				player.sendMessage(messagePrefix + ChatColor.RED + "Chest \"" + newName + "\" already exists.");
				if(!isChestOwner(player, newName)){	player.sendMessage(messagePrefix + ChatColor.RED + "You are not the owner of \"" + newName + "\"");	return false;	}
				if(!(autoMergeTarget || autoOverwriteTarget))
				{
					player.sendMessage(messagePrefix + "Premerge it with " + name1 + "/" + name2 + ".");
					player.sendMessage(messagePrefix + "Remove it with the /rcremove <name> command.");
					player.sendMessage(messagePrefix + "Or specify flag O to overwrite.");

					return false;
				}
				else if(autoMergeTarget)
				{
					player.sendMessage(messagePrefix + "Premerging chests \"" + name1 + "\" and \"" + name2 + "\".");
					String[] fakeArgs = {name1, newName, "", args[3]};
					if(!mergeChests(player, fakeArgs))
					{
						player.sendMessage(messagePrefix + "Retrying premerge with pre-stack.");
						fakeArgs[2] = "";
						fakeArgs[3] = "swf" + args[3];
						if(!mergeChests(player, fakeArgs))
						{
							player.sendMessage(messagePrefix + ChatColor.RED + "Error: \"" + name1 + "\" and \"" + name2 + "\" could not be merged.");
							return false;
						}
					}
				}
				else if(autoOverwriteTarget)
				{
					chests.remove(newName);
				}
			}
		}
		else
		{	newName = name1;	}
		
		InventoryLargeChest newChest = new InventoryLargeChest(newName, new rcChest(), new rcChest());
		setChestOwner(player, newName);
		InventoryLargeChest chest1 = chests.get(name1);
		InventoryLargeChest chest2 = chests.get(name2);
		
		if(autoStackBefore)
		{
			stackChest(player, name1);
			stackChest(player, name2);
		}
		
		if(!ignoreOverfilled)
		{
			int numItems = 0;
			// count the amount of items
			for(net.minecraft.server.ItemStack stack : chest1.getContents())
				if(stack != null && stack.count != 0 && stack.id != 0)
					numItems++;

			for(net.minecraft.server.ItemStack stack : chest2.getContents())
				if(stack != null && stack.count != 0 && stack.id != 0)
					numItems++;
			
			if(numItems > 54)
			{
				if(stackWhenOverfilled && !autoStackBefore)
				{
					player.sendMessage(messagePrefix + "Total amount of items exceeds 54, prestacking chests");
					numItems = 0;
					stackChest(player, name1);
					stackChest(player, name2);
					for(net.minecraft.server.ItemStack stack : chest1.getContents())
						if(stack != null && stack.count != 0 && stack.id != 0)
							numItems++;

					for(net.minecraft.server.ItemStack stack : chest2.getContents())
						if(stack != null && stack.count != 0 && stack.id != 0)
							numItems++;
				}
				if(numItems > 54)
				{
					player.sendMessage(messagePrefix + ChatColor.RED + "Error: Total amount of items exceeds 54");
					if(!ignoreHelp)
					{
						if(!stackWhenOverfilled)
							player.sendMessage(messagePrefix + "Specify flag SWF to stack contents");
						player.sendMessage(messagePrefix + "Specify flag I to ignore this error");
					}
					// no dropping (yet)
					//player.sendMessage(messagePrefix + "Excess items will be dropped");
					return false;
				}
			}
		}

		int index = 0;
		// Copy the contents of the 1st chest to the new chest, preserving locations
		for(net.minecraft.server.ItemStack stack : chest1.getContents())
		{
			if(stack != null && stack.count != 0 && stack.id != 0)
				newChest.a(index, new net.minecraft.server.ItemStack(stack.id, stack.count, 0));
			index++;
		}

		index = 0;
		// Merge the contents of the newChest and the 2nd chest, try to preserve locations
		for(net.minecraft.server.ItemStack stack : chest2.getContents())
		{
			if(stack != null && stack.count != 0 && stack.id != 0)
			{	
				net.minecraft.server.ItemStack trgStack = newChest.c_(index);
				if(trgStack == null || trgStack.count == 0 || trgStack.id == 0)
					newChest.a(index, new net.minecraft.server.ItemStack(stack.id, stack.count, 0));
				else
				{
					int i = 0;
					for(net.minecraft.server.ItemStack targetStack : newChest.getContents())
					{
						if(targetStack == null || targetStack.count == 0 || targetStack.id == 0)
						{
							newChest.a(i, new net.minecraft.server.ItemStack(stack.id, stack.count, 0));
							break;
						}
						i++;
					}
				}
			}
			index++;
		}


		chests.remove(name1);
		chests.remove(name2);
		chests.put(newName, newChest);
		
		if(autoStackAfter)	stackChest(player, newName);
		
		player.sendMessage(messagePrefix + ChatColor.GREEN + "Merged chests \"" + name1 + "\" and \"" + name2 + "\" into \"" + newName + "\".");
		return true;
	}
	/**
	 * Removes a chest.
	 */
	private void removeChest(Player player, String chestName)
	{
		if(chestName.equals(""))				{	player.sendMessage(messagePrefix + ChatColor.RED + "Please specify a chest name");					return;	}
		chestName = parseTags(chestName, player);
		if(!chests.containsKey(chestName))		{	player.sendMessage(messagePrefix + ChatColor.RED + "Chest \"" + chestName + "\" doesn't exist.");		return;	}
		if(!isChestOwner(player, chestName))	{	player.sendMessage(messagePrefix + ChatColor.RED + "You are not the owner of \"" + chestName + "\"");	return;	}
		
		chests.remove(chestName);
		player.sendMessage(messagePrefix + ChatColor.GREEN + "Chest \"" + chestName + "\" succesfully removed.");
	}
	/**
	 * Renames a chest
	 */
	private void renameChest(Player player, String[] args)
	{
		if(args.length < 1 || args[0].equals(""))
		{
			player.sendMessage(messagePrefix + ChatColor.RED + "Please specify a source chest name");
			if(args.length < 2 || args[1].equals(""))
				player.sendMessage(messagePrefix + ChatColor.RED + "Please specify a target chest name");
			return;
		}
		if(args.length < 2 || args[1].equals(""))
		{	player.sendMessage(messagePrefix + ChatColor.RED + "Please specify a target chest name");				return;	}
		String oldName = parseTags(args[0], player);
		String newName = parseTags(args[1], player);
		if(!chests.containsKey(oldName))	{	player.sendMessage(messagePrefix + ChatColor.RED + "Chest \"" + oldName + "\" doesn't exist");		 	return;	}
		if(!isChestOwner(player, oldName))	{	player.sendMessage(messagePrefix + ChatColor.RED + "You are not the owner of \"" + oldName + "\"");		return;	}
		if(chests.containsKey(newName))		{	player.sendMessage(messagePrefix + ChatColor.RED + "Target name: \"" + newName + "\" already exist");	return;	}
		
		InventoryLargeChest chest = chests.get(oldName);
		chests.remove(oldName);
		chests.put(newName, chest);
		
		player.sendMessage(messagePrefix + ChatColor.GREEN + "Renamed " + renameSigns(oldName, newName) + " signs.");
		player.sendMessage(messagePrefix + ChatColor.GREEN + "Renamed chest \"" + oldName + "\" to \"" + newName + ".");
	}
	/**
	 * Creates a chest with the specified name, used for the rcCreate command.
	 */
	public boolean createChest(Player player, String chestName)
	{
		if(permissionsOn && !rcPlugin.Permissions.has(player, rccreatePerm))
			player.sendMessage(messagePrefix + ChatColor.RED + "You do not have permission create chests.");
		debugPrint("Creating Chest");
		chestName = parseTags(chestName, player);
		if(chestName.equals(""))
		{
			player.sendMessage(messagePrefix + ChatColor.RED + "Please specify a chest name");
			return false;
		}
		if(chests.containsKey(chestName))
		{
			player.sendMessage(messagePrefix + ChatColor.RED + "Chest \"" + chestName + "\" already exists.");
			return false;
		}
		if(numChestsOwned.containsKey(player.getName().toLowerCase()) && numChestsOwned.get(player.getName().toLowerCase()) > rcPlugin.maxChestsPP && 
				!(permissionsOn && rcPlugin.Permissions.has(player, rcunlimitedAmountPerm)))
		{
			player.sendMessage(messagePrefix + ChatColor.RED + "You already own too many chests, max = " + rcPlugin.maxChestsPP);
			return false;
		}
		if(iConomyOn)
		{
			boolean chestIsFree = (rcPlugin.permissionsOn && rcPlugin.Permissions.has(player, rcPlugin.rcfreeCreatePerm));
			if(!chestIsFree)
			{
				Account iConAccount = iConomy.getBank().getAccount(player.getName());
				if(!(iConAccount.getBalance() >= iConChestCreatePrice))
				{
					player.sendMessage(messagePrefix + ChatColor.RED + "Your bank balance is too low to create a chest");
					player.sendMessage(messagePrefix + ChatColor.RED + "You need " + iConomy.getBank().format(iConChestCreatePrice) + ", you have " + iConomy.getBank().format(iConAccount.getBalance()));
					return false;
				}
				else
				{
					iConAccount.subtract(iConChestCreatePrice);
					player.sendMessage(messagePrefix + ChatColor.GREEN + "Creating the chest cost you " + iConomy.getBank().format(iConChestCreatePrice));
				}
			}
		}
		// Create the chest
		chests.put(chestName, new InventoryLargeChest(chestName, new rcChest(), new rcChest()));
		
		player.sendMessage(messagePrefix + ChatColor.GREEN + "Chest \"" + chestName + "\" succesfully created.");
		setChestOwner(player, chestName);
		openChest(player, chestName);
		return true;
	}
	/**
	 * Adds a chest at the specified location, used when placing signs.
	 * Transfers all items from the existing chest to the new chest.
	 */
	public boolean addChest( Player player, String chestName, Location location)
	{
		debugPrint("Adding Chest");
		Block block = location.getBlock().getRelative(signDir.getOppositeFace());
		chestName = parseTags(chestName, player);
		if(!(block.getState() instanceof Chest))
		{
			player.sendMessage(messagePrefix + ChatColor.RED + "Sign not placed above chest, try again.");
			return false;
		}
		if(chests.containsKey(chestName))
		{
			transferItems((Chest)block.getState(), chests.get(chestName));
			player.sendMessage(messagePrefix + ChatColor.GREEN + "Linked to chest \"" + chestName + "\".");
			return true;
		}
		if(createChest(player, chestName))
		{
			transferItems((Chest)block.getState(), chests.get(chestName));
			return true;
		}
		else
			return false;
	}
	/**
	 * Transfers the items from a chest to a virtual chest.
	 */
	private void transferItems(Chest chest, InventoryLargeChest lrgchest)
	{
		boolean full = false;
		for (int index = 0; index < 27; index++)
		{
			org.bukkit.inventory.ItemStack stack = chest.getInventory().getItem(index);

			if ((!full) && (stack.getTypeId() > 0) && (stack.getAmount() > 0))
			{
				boolean found = false;
				for (int i = 0; i < 54; i++)
				{
					net.minecraft.server.ItemStack stack2 = lrgchest.c_(i);

					if ((stack2 != null) && (stack2.id == stack.getTypeId()) && (stack.getMaxStackSize() > 1))
					{
						debugPrint("Merging itemstacks");
						stack2.count = Math.min(stack2.b(), stack.getAmount() + stack2.count);
						lrgchest.a(i, stack2);
						int newcount = Math.max(0, stack.getAmount() + stack2.count - stack2.b());
						if (newcount > 0)
						{
							stack = new org.bukkit.inventory.ItemStack(stack.getTypeId(), newcount);
						}
						else
						{
							found = true;
							break;
						}
					}
					else
					{
						if ((stack2 != null) && (stack2.id != 0) && (stack2.count != 0))
							continue;
						debugPrint("Creating new itemstack for transfer");
						lrgchest.a(i, new net.minecraft.server.ItemStack(stack.getTypeId(), stack.getAmount(), 0));
						found = true;
						break;
					}
				}
				if (!found)
				{
					debugPrint("Did not find clear spot, dropping item");
					chest.getBlock().getWorld().dropItem(chest.getBlock().getLocation(), stack);
					full = true;
				}
			}

			if ((!full) || (stack.getTypeId() == 0) || (stack.getAmount() == 0))
				continue;
			debugPrint("Dropping item");
			chest.getBlock().getWorld().dropItem(chest.getBlock().getLocation(), stack);
		}

		chest.getInventory().clear();
		debugPrint("Done");
	}
	/**
	 * Opens a chest without suppressing warning when new chest should be created.
	 */
	private boolean openChest(Player player, String chestName)
	{	return openChest(player, chestName, false);	}
	/**
	 * Opens a chest.
	 * Returns whether chest creation is needed
	 */
	public boolean openChest(Player player, String chestName, boolean supressWarning)
	{
		debugPrint(player.getName() + " is trying to open chest \"" + chestName + "\".");
		chestName = parseTags(chestName, player);
		if(!(chests.containsKey(chestName)))
		{
			if(!supressWarning)
			{
				debugPrint("Chest \"" + chestName + "\" doesn't exist");
				player.sendMessage(messagePrefix + ChatColor.RED + "Chest \"" + chestName + "\" does not exist.");
				player.sendMessage(messagePrefix + "Type /rccreate " + chestName + " to create it");
			}
			return false;
		}
		if(!isChestOwner(player, chestName) && !hasChestAllowance(player, chestName))
		{
			player.sendMessage(messagePrefix + ChatColor.RED + "You are not allowed to open chest \"" + chestName + "\"");
			player.sendMessage(messagePrefix + "Ask player " + chestOwners.get(chestName) + " for permission");
			return true;
		}
		//player.sendMessage(messagePrefix + "Opening chest \"" + chestName + "\".");
		debugPrint("The chest \"" + chestName + "\" exists");
		InventoryLargeChest chest = chests.get(chestName);
		EntityPlayer cPlayer = ((CraftPlayer)player).getHandle();

		debugPrint("Presenting chest \"" + chestName + "\".");
		cPlayer.a(chest);
		return true;
	}

	/**
	 * Parses all tags such as [player]
	 */
	public static String parseTags(String chestName, Player player)
	{
		chestName = chestName.replace(rcPlugin.tagPlayer, player.getName());
		return chestName;
	}
	/**
	 * Checks if a sign contains a chest name, and returns the name if it does.
	 */
	public String getSignName(String[] lines)
	{
		String name = "";
		for(int i = 0; i < 3 && name.equals(""); i++)
		{
			if (lines[i].contains(" "))
			{
				String[] split = lines[i].split(" ");
				if(split[0].equalsIgnoreCase("[rc]"))
					name = split[1];
			}
			else if(lines[i].equalsIgnoreCase("[rc]"))
			{
				if(lines[i+1].length() > 0)
					// ignore everything after spaces
					name = lines[i+1].split(" ")[0];
			}
		}
		if(name.equals(""))
			debugPrint("No sign name specified");
		return name;
	}

	public void debugPrint(String msg)
	{
	//	getServer().broadcastMessage(messagePrefix + msg);
	//	log.info(messagePrefix + msg);
	}

	/**
	 * Add a sign to the list.
	 */
	public void addSign(Sign sign)
	{	signs.add(sign);	}
	/**
	 * Remove a sign from the list.
	 */
	public void removeSign(Sign a_sign)
	{
		for(Sign sign : (Sign[])signs.toArray(new Sign[0]))
		{
			if(		sign.getBlock().getLocation().getBlockX() == a_sign.getBlock().getLocation().getBlockX() &&
					sign.getBlock().getLocation().getBlockY() == a_sign.getBlock().getLocation().getBlockY() &&
					sign.getBlock().getLocation().getBlockZ() == a_sign.getBlock().getLocation().getBlockZ())
			{
				signs.remove(sign);
			}
		}
	}
	/**
	 * Rename all signs containing specified name
	 */
	public int renameSigns(String oldName, String newName)
	{
		int amount = 0;
		for(Sign sign : (Sign[])signs.toArray(new Sign[0]))
		{
			String[] lines = sign.getLines();
			for(int i = 0; i < 4; i++)
			{
				if(lines[i].contains("[rc]"))
				{
					if(lines[i].contains(" "))
					{
						if(lines[i].contains(oldName))
						{
							lines[i] = lines[i].replace(oldName, newName);
							amount++;
						}
					}
					else if(i < 3)
						if(lines[i+1].contains(oldName))
						{
							lines[i+1] = lines[i+1].replace(oldName, newName);
							amount++;
						}
					break;
				}
			}
			for(int i = 0; i < 4; i++)
			{
				sign.setLine(i, lines[i]);
			}
			sign.update();
		}
		return amount;
	}

	/**
	 * Set up the permissions system
	 */
	private void setupPermissions()
	{
		Plugin perm = this.getServer().getPluginManager().getPlugin("Permissions");
		if (rcPlugin.Permissions == null)
			if (perm != null)
			{
				log.info(messagePrefix + "Permissions enabled");
				rcPlugin.Permissions = ((Permissions)perm).getHandler();
				permissionsOn = true;
			}
			else
				log.info(messagePrefix + "Permissions not found, everyone can use chests");
	}
	/**
	 * Set up the iConomy system
	 */
	private void setupiConomy()
	{
		Plugin iCon = this.getServer().getPluginManager().getPlugin("iConomy");
		if (rcPlugin.iConomyHandle == null)
			if (iCon != null)
			{
				log.info(messagePrefix + "iConomy enabled");
				rcPlugin.iConomyHandle = ((iConomy)iCon);
				iConomyOn = true;
			}
			else
				log.info(messagePrefix + "iConomy not found");
	}
}