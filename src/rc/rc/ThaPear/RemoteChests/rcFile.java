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
    
	static File ChestFile = new File(baseDir + "chests.txt");	
	
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
			FileReader readFile = null;
	
			readFile = new FileReader(ChestFile);
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

	public void saveChests(HashMap<String, InventoryLargeChest> chests)
	{	saveChests(chests, false);	}
	@SuppressWarnings({ "unused" })
	public void saveChests(HashMap<String, InventoryLargeChest> chests, boolean silent)
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
		if(!silent)	System.out.println(rcPlugin.messagePrefix + "Saved " + numChestsSaved + " chests.");
		try	{	outStream.close();	} catch (IOException ex) {}
		printStream.close();
	}
}
