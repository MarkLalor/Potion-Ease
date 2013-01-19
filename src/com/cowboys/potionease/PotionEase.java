package com.cowboys.potionease;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import com.massivecraft.factions.P;
import com.massivecraft.factions.listeners.FactionsPlayerListener;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;


public class PotionEase extends JavaPlugin implements Listener
{
	public P factions = null;
	public WorldGuardPlugin worldguard = null;
	public static final int[] ingredients = { 372,377,353,378,348,331,375,376,396,370,382,289 };
	
	public void onEnable() 
	{ 
		
		//Register events
		getServer().getPluginManager().registerEvents(this, this);
		
		//Get factions plugin if it exists.
		factions =  (P) Bukkit.getServer().getPluginManager().getPlugin("Factions");
		worldguard = (WorldGuardPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");
	}
	
	//Item moving, where all the magic happens ;)
	public static void transferToPlayer(Player p, BrewingStand stand)
	{
		BrewerInventory i = stand.getInventory();
		for (int slot = 0;slot<=2;slot++)
			if (i.getItem(slot) != null && firstEmptyFirstSlotLast(p.getInventory()) != -1)
			{
				ItemStack[] inv = p.getInventory().getContents();
				inv[firstEmptyFirstSlotLast(p.getInventory())] = i.getItem(slot);
				p.getInventory().setContents(inv);
				
				i.setItem(slot, null);
			}
		

		if (i.getIngredient() != null)
		{
			if (p.getInventory().firstEmpty() == -1)
			{
				if (p.getInventory().contains(i.getIngredient().getType()))
				{
					if (p.getInventory().getItem(p.getInventory().first(i.getIngredient().getType())).getAmount() < 64)
					{
						p.getInventory().addItem(i.getIngredient());
						i.setIngredient(null);
					}
				}
				return;
			}
			
			p.getInventory().addItem(i.getIngredient());
			i.setIngredient(null);
		}
		
	}
	public static void transferToStand(Player p, BrewingStand stand)
	{
		BrewerInventory i = stand.getInventory();
		
		//Run 3 times
		for (int slot = 0;slot<=2;slot++)
		{
			//Check if slot is empty or not
			if (i.getItem(slot) == null)
			{
				//Make sure there are results
				int potionPosition = firstWaterPotion(p.getInventory());
				if (potionPosition != -1)
				{
					ItemStack item = p.getInventory().getItem(potionPosition);
					
					if (item != null)
					{
						if (isWaterPotion(item))
						{
							ItemStack[] inv = p.getInventory().getContents();
							inv[potionPosition] = new ItemStack(Material.AIR);
							p.getInventory().setContents(inv);
							
							i.setItem(slot, new ItemStack(Material.POTION, 1));
						}
					}
				}
			}
		}
	}
	public static void transferIngredientToStand(Player p, BrewingStand stand, int material)
	{
		ItemStack m = new ItemStack(material, 1);
		BrewerInventory i = stand.getInventory();
		
		if (i.getIngredient() == null && p.getInventory().contains(m.getType()))
		{
			p.getInventory().removeItem(m);
			i.setIngredient(m);
		}
	}
	
	//Events
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e)
	{
		//Hit a brewing stand.
		if (e.getAction() == Action.LEFT_CLICK_BLOCK && e.getClickedBlock().getType() == Material.BREWING_STAND)
		{
			//Factions check.
			if (factions != null)
				if (!FactionsPlayerListener.canPlayerUseBlock(e.getPlayer(), e.getClickedBlock(), true))
					return;
			//WorldGuard check.
			if (worldguard != null)
				if (!worldguard.canBuild(e.getPlayer(), e.getClickedBlock()))
					return;
			//Permission check
			if (!e.getPlayer().hasPermission("potionease.normal"))
				return;
			
			
			//Get brewing stand.
			BrewingStand stand = (BrewingStand) e.getClickedBlock().getState();
			
			//If clicked with a water potion, transfer to stand.
			if (isWaterPotion(e.getPlayer().getItemInHand()))
			{
				//Transfers water bottlse to stand untill full.
				transferToStand(e.getPlayer(), stand);
				return;
			}
			
			//If clicked with an ingredient, transfer the ingredient to stand.
			for (int m : ingredients)
				if (e.getPlayer().getItemInHand().getTypeId() == m)
				{
					//Transfers the ingredient in the players hand into the stand.
					transferIngredientToStand(e.getPlayer(), stand, m);
					return;
				}
			
			//If clicked with thier hand, transfer to player.
			if (e.getPlayer().getItemInHand().getType() == Material.AIR)
			{
				//Transfers items into players inventory until full.
				transferToPlayer(e.getPlayer(), stand);
				return;
			}
		}
	}
	@EventHandler
	public void onBrew(BrewEvent e)
	{
		//Play an effect when brewing finishes.
		e.getBlock().getWorld().playEffect(e.getBlock().getLocation(), Effect.MOBSPAWNER_FLAMES, 1);
	}
	
	//Water potion detection
	public static boolean isWaterPotion(ItemStack item)
	{	
		return item==null?false:(item.getDurability() & 0x3F) == 0?item.getType()==Material.POTION?true:false:false;
	}
	public static int firstWaterPotion(PlayerInventory i)
	{
		for (int c=35;c>=0;c--)
			if (isWaterPotion(i.getItem(c)))
				return c;
		return -1;
	}
	
	//Get's first empty starting from the BACK of the players inventory, allowing them to keep
	//Nothing in thier hand to click more brewing stands.
	public static int firstEmptyFirstSlotLast(PlayerInventory p)
	{
		for (int c = 1;c<=36;c++)
		{
			int i = c != 36 ? c : 0;
			if (p.getItem(i) == null)
				return i;
			else if (p.getItem(i).getType() == Material.AIR)
				return i;
		}
		return -1;
	}
}
