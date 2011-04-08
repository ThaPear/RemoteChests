package rc.rc.ThaPear.RemoteChests;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.block.BlockFace;

import net.minecraft.server.InventoryLargeChest;
import net.minecraft.server.ItemStack;

public class rcFile
{
	public static rcPlugin plugin;
	public rcFile(rcPlugin instance)
	{
		plugin = instance;
	}

	public static String baseDir = "plugins/Remote Chests/";

	static File AllowedFile =			new File(baseDir + "allowed.txt");
	static File OwnerFile =				new File(baseDir + "owners.txt");
	static File ChestFile =				new File(baseDir + "chests.txt");
	static File SettingsFile =			new File(baseDir + "settings.txt");
	static File SettingsFileBackup =	new File(baseDir + "settingsbackup.txt");

	private String[] loadFile(File file)
	{
		String[] lines = {""};

		new File(baseDir).mkdir();
		try
		{
			if (!file.exists()) // Check if the file exists
			{ // If it doesn't, try to create it.
				try						{	file.createNewFile();	}
				// If it fails, print why.
				catch (IOException ex)	{	ex.printStackTrace();		}
				// No need to try to read chests from an empty file, so return empty hashmap.
				System.out.println(rcPlugin.messagePrefix + file.getName() + " not found, creating it." );
			}
	
			ArrayList<String> listLines = new ArrayList<String>();
			FileReader readFile = new FileReader(file);
			BufferedReader readBuffer = new BufferedReader(readFile);
			String line = "";
			while (line != null) {
				line = readBuffer.readLine();
				listLines.add(line);
			}
			lines = (String[])listLines.toArray(new String[0]);
			readBuffer.close();
		}
		catch (IOException ex)
		{
			System.out.println(rcPlugin.messagePrefix + "Error while reading " + file.getName() );
			ex.printStackTrace();
			return lines;
		}
		return lines;
	}
	@SuppressWarnings("unused")
	private void saveFile(File file, String[] lines)
	{
		if(file.exists())
		{
			// Delete the old file.
			if(!file.delete())
			{
				System.out.println(rcPlugin.messagePrefix + "Could not delete " + file.getName());
			}
			// Create an empty file.
			try						{	file.createNewFile();	}
			// If it fails, print why.
			catch (IOException e)	{	e.printStackTrace();		}
		}

		FileOutputStream outStream = null;
		PrintStream printStream = null;
		try
		{
			outStream = new FileOutputStream(file, true);
			printStream = new PrintStream(outStream, true);
		}
		catch (IOException ex)
		{
			if (printStream != null) printStream.close();
			try						{	if (outStream != null) outStream.close();	}
			catch (IOException ex2)	{}
			System.out.println(rcPlugin.messagePrefix + "Could not write file " + file.getName());
			return;
		}
		
		for(String line : lines)
		{
			printStream.println(line);
		}
		
		try	{	outStream.close();	} catch (IOException ex) {}
		printStream.close();
	}
	
	/**
	 * settings save & base values
	 */
	public void loadSettings()
	{
		String[] lines = loadFile(SettingsFile);
		if(lines.length < 6 || !lines[0].contains(":") || !lines[1].contains(":") || !lines[2].contains(":") || !lines[3].contains(":") || !lines[4].contains(":"))
		{
			System.out.println(rcPlugin.messagePrefix + "Settings file corrupt, backing up and recreating");
			revertSettings();
			
			return;
		}
		HashMap<String, BlockFace> blockFaces = new HashMap<String, BlockFace>();
		blockFaces.put("up", BlockFace.UP);			blockFaces.put("down", BlockFace.DOWN);
		blockFaces.put("north", BlockFace.NORTH);	blockFaces.put("east", BlockFace.EAST);
		blockFaces.put("south", BlockFace.SOUTH);	blockFaces.put("west", BlockFace.WEST);
		
		String signDirS = lines[0].split(":")[1].toLowerCase();
		if(!blockFaces.containsKey(signDirS))
		{
			System.out.println(rcPlugin.messagePrefix + "Invalid signdir specified, valid dirs:");
			System.out.println(rcPlugin.messagePrefix + "up, down, north, east, south, west");
			revertSettings();
			return;
		}
		
		rcPlugin.signDir = blockFaces.get(signDirS);
		String tagPlayer = lines[1].split(":")[1];
		if(tagPlayer.length() < 1)
		{
			System.out.println(rcPlugin.messagePrefix + "Invalid player tag specified.");
			revertSettings();
			return;
		}
		rcPlugin.tagPlayer = tagPlayer;
		rcPlugin.maxChestsPP = Integer.parseInt(lines[2].split(":")[1]);
		rcPlugin.iConChestCreatePrice = Integer.parseInt(lines[3].split(":")[1]);
		rcPlugin.iConChestLinkPrice = Integer.parseInt(lines[4].split(":")[1]);
		
	}
	private void revertSettings()
	{
		SettingsFile.renameTo(SettingsFileBackup);
		
		try	{	SettingsFile.createNewFile();	} catch(IOException e){}
		printDefaultSettings();
	}
	private void printDefaultSettings()
	{
		String[] lines = {"signlocation:up", "tagplayer:[player]", "maxchestsperplayer:10", "chestcreateprice:0", "chestlinkprice:0"};
		saveFile(SettingsFile, lines);
	}
	/**
	 * Allowed users save/load
	 */
	public HashMap<String, ArrayList<String>> loadAllowed()
	{
		HashMap<String, ArrayList<String>> allowed = new HashMap<String, ArrayList<String>>();
		
		String[] lines = loadFile(AllowedFile);
		
		for(String ln : lines)
		{
			if(ln != null && ln.contains(" "))
			{
				String[] split = ln.split(" ");
				ArrayList<String> names = new ArrayList<String>();
				String chestName = "";
				for(String name : split)
				{
					if(chestName.equals(""))
						chestName = name;
					else
						names.add(name);
				}
				allowed.put(chestName, names);
			}
		}
		
		return allowed;
	}
	public void saveAllowed(HashMap<String, ArrayList<String>> allowed)
	{
		if(allowed.keySet().isEmpty())
			return;

		ArrayList<String> lines = new ArrayList<String>();
		for(String chest : allowed.keySet())
		{
			ArrayList<String> names = allowed.get(chest);
			
			String ln = "";
			ln += chest + " ";
			for(String name : (String[])names.toArray(new String[0]))
			{
				ln += name + " ";
			}
			lines.add(ln);
		}
		saveFile(AllowedFile, (String[])lines.toArray(new String[0]));
	}
	/**
	 * Owners save/load
	 */
	public HashMap<String, String> loadOwners()
	{
		HashMap<String, String> owners = new HashMap<String, String>();
		
		String[] lines = loadFile(OwnerFile);
		
		for(String ln : lines)
		{
			if(ln != null && ln.contains(" "))
			{
				String[] split = ln.split(" ");
				String ownerName = split[1];
				owners.put(split[0], ownerName);
			}
		}
		return owners;
	}
	public void saveOwners(HashMap<String, String> owners)
	{
		if(owners.keySet().isEmpty())
			return;

		ArrayList<String> lines = new ArrayList<String>();

		for(String chestName : owners.keySet())
		{
			String playerName = owners.get(chestName);
			lines.add(chestName + " " + playerName);
		}
		saveFile(OwnerFile, (String[])lines.toArray(new String[0]));
	}
	/**
	 * Chests save/load
	 */
	public HashMap<String, InventoryLargeChest> loadChests()
	{
		HashMap<String, InventoryLargeChest> chests = new HashMap<String, InventoryLargeChest>();

		String[] lines = loadFile(ChestFile);
		
		if((lines.length-1) % 3 == 0)
		{
			for(int i = 0; i < (lines.length-1); i += 3)
			{
				if(chests.containsKey(lines[i]))
				{
					System.out.println(rcPlugin.messagePrefix + "Duplicate chestname found: \"" + lines[i] + "\".");
					continue;
				}
				//System.out.println(rcPlugin.messagePrefix + "Loading chest \"" + lines[i] + "\"." );
				InventoryLargeChest lrgchest = new InventoryLargeChest(lines[i], new rcChest(), new rcChest());
				chests.put(lines[i], lrgchest);
				String[] ids = lines[i+1].split(" ");
				String[] amounts = lines[i+2].split(" ");
				if(ids.length != amounts.length || ids.length != 54)
					System.out.println(rcPlugin.messagePrefix + "Found invalid amount of item ids/amounts in chests.txt" );
				for(int j = 0; j < ids.length; j++)
				{
					int id = Integer.parseInt(ids[j]);
					int amount = Integer.parseInt(amounts[j]);
					if(id != 0 && amount != 0)
					{
						//System.out.println(rcPlugin.messagePrefix + "sids[" + j + "] = " + ids[j] + ", amounts[" + j + "] = " + amounts[j] + "."  );
						lrgchest.a(j, new ItemStack(id, amount, 0));
					}
				}
			}
			System.out.println(rcPlugin.messagePrefix + "Loaded " + (lines.length-1)/3 + " chests.");
		}
		else
		{
			System.out.println(rcPlugin.messagePrefix + "Found invalid amount of lines in chests.txt: " + lines.length + ", should be multiple of 3." );
		}
		return chests;
	}
	public void saveChests(HashMap<String, InventoryLargeChest> chests)
	{
		if(chests.keySet().isEmpty())
			return;
		
		ArrayList<String> lines = new ArrayList<String>();
		
		int numChestsSaved = 0;
		for(String chestName : chests.keySet())
		{
			numChestsSaved++;
			//System.out.println(rcPlugin.messagePrefix + "Saving chest \"" + chestName + "\"." );
			InventoryLargeChest lrgchest = chests.get(chestName);
			ItemStack[] chestinv = lrgchest.getContents();
			String ids = "";
			String amounts = "";
			for(ItemStack stack : chestinv)
			{
				if(ids.equals(""))
				{
					if(stack == null || stack.count == 0 || stack.id == 0)
					{	ids += "0";
						amounts += "0";					}
					else
					{	ids += stack.id;
						amounts += stack.count;			}
				}
				else
				{
					if(stack == null || stack.count == 0 || stack.id == 0)
					{	ids += " 0";
						amounts += " 0";				}
					else
					{	ids += " " + stack.id;
						amounts += " " + stack.count;	}
				}
			}
			lines.add(chestName);
			lines.add(ids);
			lines.add(amounts);
		}
		saveFile(ChestFile, (String[])lines.toArray(new String[0]));
		System.out.println(rcPlugin.messagePrefix + "Saved " + numChestsSaved + " chests.");
	}
	/**
	 * Delete files left over by previous versions.
	 */
	public void removeObsolete()
	{
		new File(baseDir + "iconomy.txt").delete();
	}
}
