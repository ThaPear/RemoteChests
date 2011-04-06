package rc.rc.ThaPear.RemoteChests;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;

import net.minecraft.server.InventoryLargeChest;
import net.minecraft.server.ItemStack;

public class rcFile
{
	public static rcPlugin plugin;
	public rcFile(rcPlugin instance)
	{
		plugin = instance;
	}

	public static String baseDir = "plugins/RemoteChests/";

	static File AllowedFile =		new File(baseDir + "allowed.txt");
	static File OwnerFile =			new File(baseDir + "owners.txt");
	static File ChestFile =			new File(baseDir + "chests.txt");
	static File iConomyFile =		new File(baseDir + "iconomy.txt");
	static File iConomyFileBackup =	new File(baseDir + "iconomybackup.txt");

	/**
	 * iConomy settings save & base values
	 */
	public void loadIConomy()
	{
		// Make sure the folder exists
		new File(baseDir).mkdir();

		if (!iConomyFile.exists()) // Check if the file exists
		{ // If it doesn't, try to create it.
			try
			{
				iConomyFile.createNewFile();
				printDefaultIConomyValues();
			}
			// If it fails, print why.
			catch (IOException ex)	{	ex.printStackTrace();		}
			// No need to try to read chests from an empty file, so return empty hashmap.
			System.out.println(rcPlugin.messagePrefix + "iconomy.txt not found, created it." );
			return;
		}
		try
		{
			ArrayList<String> listLines = new ArrayList<String>();
			FileReader readFile = new FileReader(iConomyFile);
			BufferedReader readBuffer = new BufferedReader(readFile);
			String line = "";
			while (line != null) {
				line = readBuffer.readLine();
				listLines.add(line);
			}
			String[] lines = (String[])listLines.toArray(new String[0]);
			if(lines.length < 2 || !lines[0].contains(":"))
			{
				System.out.println(rcPlugin.messagePrefix + "iconomy.txt corrupted, regenerating" );
				iConomyFile.renameTo(iConomyFileBackup);
				
				iConomyFile.createNewFile();
				printDefaultIConomyValues();
				
				return;
			}
			rcPlugin.iConChestCreatePrice = Integer.parseInt(lines[0].split(":")[1]);
		}
		catch (IOException ex)
		{
			System.out.println(rcPlugin.messagePrefix + "Could not read iconomy.txt" );
		}
	}
	@SuppressWarnings({ "unused" })
	private void printDefaultIConomyValues()
	{
		FileOutputStream outStream = null;
		PrintStream printStream = null;
		try
		{
			outStream = new FileOutputStream(iConomyFile, true);
			printStream = new PrintStream(outStream, true);
		}
		catch (IOException ex)
		{
			if (printStream != null) printStream.close();
			try						{	if (outStream != null) outStream.close();	}
			catch (IOException ex2)	{}
			System.out.println(rcPlugin.messagePrefix + "Could not save default iConomy values.");
			return;
		}

		printStream.println("chestcreateprice:0");
		
		try	{	outStream.close();	} catch (IOException ex) {}
		printStream.close();
	}
	/**
	 * Allowed users save/load
	 */
	public HashMap<String, ArrayList<String>> loadAllowed()
	{
		HashMap<String, ArrayList<String>> allowed = new HashMap<String, ArrayList<String>>();
		
		new File(baseDir).mkdir();
		try
		{
			if (!AllowedFile.exists()) // Check if the file exists
			{ // If it doesn't, try to create it.
				try						{	AllowedFile.createNewFile();	}
				// If it fails, print why.
				catch (IOException ex)	{	ex.printStackTrace();		}
				// No need to try to read chests from an empty file, so return empty hashmap.
				System.out.println(rcPlugin.messagePrefix + "allowed.txt not found, creating it." );
				return allowed;
			}
	
			ArrayList<String> listLines = new ArrayList<String>();
			FileReader readFile = new FileReader(AllowedFile);
			BufferedReader readBuffer = new BufferedReader(readFile);
			String line = "";
			while (line != null) {
				line = readBuffer.readLine();
				listLines.add(line);
			}
			String[] lines = (String[])listLines.toArray(new String[0]);
			readBuffer.close();
			
			
			
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
			System.out.println(rcPlugin.messagePrefix + "Loaded " + (lines.length-1) + " chests with allowed.");
			readFile.close();
			readBuffer.close();
			return allowed;
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			return allowed;
		}
	}
	@SuppressWarnings("unused")
	public void saveAllowed(HashMap<String, ArrayList<String>> allowed)
	{
		if(allowed.keySet().isEmpty())
			return;

		// Delete the old file.
		if(!AllowedFile.delete())
		{
			System.out.println(rcPlugin.messagePrefix + "Could not delete old allowed.txt");
		}
		// Create an empty file.
		try						{	AllowedFile.createNewFile();	}
		// If it fails, print why.
		catch (IOException e)	{	e.printStackTrace();		}
		
		FileOutputStream outStream = null;
		PrintStream printStream = null;
		try
		{
			outStream = new FileOutputStream(AllowedFile, true);
			printStream = new PrintStream(outStream, true);
		}
		catch (IOException ex)
		{
			if (printStream != null) printStream.close();
			try						{	if (outStream != null) outStream.close();	}
			catch (IOException ex2)	{}
			System.out.println(rcPlugin.messagePrefix + "Could not save allowed.");
			return;
		}
		
		for(String chest : allowed.keySet())
		{
			ArrayList<String> names = allowed.get(chest);
			
			String ln = "";
			ln += chest + " ";
			for(String name : (String[])names.toArray(new String[0]))
			{
				ln += name + " ";
			}
			printStream.println(ln);
		}
		try	{	outStream.close();	} catch (IOException ex) {}
		printStream.close();
	}
	/**
	 * Owners save/load
	 */
	public HashMap<String, String> loadOwners()
	{
		HashMap<String, String> owners = new HashMap<String, String>();
		
		new File(baseDir).mkdir();
		try
		{
			if (!OwnerFile.exists()) // Check if the file exists
			{ // If it doesn't, try to create it.
				try						{	OwnerFile.createNewFile();	}
				// If it fails, print why.
				catch (IOException ex)	{	ex.printStackTrace();		}
				// No need to try to read chests from an empty file, so return empty hashmap.
				System.out.println(rcPlugin.messagePrefix + "owners.txt not found, creating it." );
				return owners;
			}
	
			ArrayList<String> listLines = new ArrayList<String>();
			FileReader readFile = new FileReader(OwnerFile);
			BufferedReader readBuffer = new BufferedReader(readFile);
			String line = "";
			while (line != null) {
				line = readBuffer.readLine();
				listLines.add(line);
			}
			String[] lines = (String[])listLines.toArray(new String[0]);
			readBuffer.close();
			
			
			
			for(String ln : lines)
			{
				if(ln != null && ln.contains(" "))
				{
					String[] split = ln.split(" ");
					owners.put(split[0], split[1]);
				}
			}
			System.out.println(rcPlugin.messagePrefix + "Loaded " + (lines.length-1) + " owners.");
			readFile.close();
			readBuffer.close();
			return owners;
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			return owners;
		}
	}
	@SuppressWarnings({ "unused" })
	public void saveOwners(HashMap<String, String> owners)
	{
		if(owners.keySet().isEmpty())
			return;

		// Delete the old file.
		if(!OwnerFile.delete())
		{
			System.out.println(rcPlugin.messagePrefix + "Could not delete old owners.txt");
		}
		// Create an empty file.
		try						{	OwnerFile.createNewFile();	}
		// If it fails, print why.
		catch (IOException e)	{	e.printStackTrace();		}
		
		FileOutputStream outStream = null;
		PrintStream printStream = null;
		try
		{
			outStream = new FileOutputStream(OwnerFile, true);
			printStream = new PrintStream(outStream, true);
		}
		catch (IOException ex)
		{
			if (printStream != null) printStream.close();
			try						{	if (outStream != null) outStream.close();	}
			catch (IOException ex2)	{}
			System.out.println(rcPlugin.messagePrefix + "Could not save owners.");
			return;
		}

		for(String chestName : owners.keySet())
		{
			String playerName = owners.get(chestName);
			printStream.println(chestName + " " + playerName);
		}
		try	{	outStream.close();	} catch (IOException ex) {}
		printStream.close();
	}
	/**
	 * Chests save/load
	 */
	public HashMap<String, InventoryLargeChest> loadChests()
	{
		HashMap<String, InventoryLargeChest> chests = new HashMap<String, InventoryLargeChest>();

		new File(baseDir).mkdir();
		try
		{
			if (!ChestFile.exists()) // Check if the file exists
			{ // If it doesn't, try to create it.
				try						{	ChestFile.createNewFile();	}
				// If it fails, print why.
				catch (IOException ex)	{	ex.printStackTrace();		}
				// No need to try to read chests from an empty file, so return empty hashmap.
				System.out.println(rcPlugin.messagePrefix + "chests.txt not found, creating it." );
				return chests;
			}
	
			ArrayList<String> listLines = new ArrayList<String>();
			FileReader readFile = new FileReader(ChestFile);
			BufferedReader readBuffer = new BufferedReader(readFile);
			String line = "";
			while (line != null) {
				line = readBuffer.readLine();
				listLines.add(line);
			}
			String[] lines = (String[])listLines.toArray(new String[0]);
			readBuffer.close();
			
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
			readFile.close();
			readBuffer.close();
			return chests;
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			return chests;
		}
	}
	@SuppressWarnings({ "unused" })
	public void saveChests(HashMap<String, InventoryLargeChest> chests)
	{
		if(chests.keySet().isEmpty())
			return;

		// Delete the old file.
		if(!ChestFile.delete())
		{
			System.out.println(rcPlugin.messagePrefix + "Could not delete old chests.txt");
		}
		// Create an empty file.
		try						{	ChestFile.createNewFile();	}
		// If it fails, print why.
		catch (IOException e)	{	e.printStackTrace();		}
		
		FileOutputStream outStream = null;
		PrintStream printStream = null;
		try
		{
			outStream = new FileOutputStream(ChestFile, true);
			printStream = new PrintStream(outStream, true);
		}
		catch (IOException ex)
		{
			if (printStream != null) printStream.close();
			try						{	if (outStream != null) outStream.close();	}
			catch (IOException ex2)	{}
			System.out.println(rcPlugin.messagePrefix + "Could not save chests.");
			return;
		}
		
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
			printStream.println(chestName);
			printStream.println(ids);
			printStream.println(amounts);
		}
		System.out.println(rcPlugin.messagePrefix + "Saved " + numChestsSaved + " chests.");
		try	{	outStream.close();	} catch (IOException ex) {}
		printStream.close();
	}
}
