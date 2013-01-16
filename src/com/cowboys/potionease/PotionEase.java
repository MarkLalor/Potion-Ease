package com.cowboys.potionease;

import java.util.ArrayList;

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


public class PotionEase extends JavaPlugin implements Listener
{
	public P factions=null;
	public boolean hasFactions=false;
	public ArrayList<Material> ingredients;
	
	public void onEnable() 
	{ 
		//Register events
		getServer().getPluginManager().registerEvents(this, this);
		
		//Get factions plugin if it exists.
		factions = getFactions();
		if (factions != null)
			hasFactions = true;
		
		//All the brewing ingredients.
		ingredients = new ArrayList<Material>();
		ingredients.add(Material.NETHER_STALK);
		ingredients.add(Material.BLAZE_POWDER);
		ingredients.add(Material.SUGAR);
		ingredients.add(Material.MAGMA_CREAM);
		ingredients.add(Material.GLOWSTONE_DUST);
		ingredients.add(Material.REDSTONE);
		ingredients.add(Material.SPIDER_EYE);
		ingredients.add(Material.FERMENTED_SPIDER_EYE);
		ingredients.add(Material.GOLDEN_CARROT);
		ingredients.add(Material.GHAST_TEAR);
		ingredients.add(Material.SPECKLED_MELON);
		ingredients.add(Material.SULPHUR);
	}
	private P getFactions() 
	{
		return (P) Bukkit.getServer().getPluginManager().getPlugin("Factions");
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
	public static void transferIngredientToStand(Player p, BrewingStand stand, Material mat)
	{
		ItemStack m = new ItemStack(mat, 1);
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
		//Hit a brewing stand
		if (e.getAction() == Action.LEFT_CLICK_BLOCK && e.getClickedBlock().getType() == Material.BREWING_STAND)
		{
			if (hasFactions)
				if (!FactionsPlayerListener.canPlayerUseBlock(e.getPlayer(), e.getClickedBlock(), true))
					return;
			
			//Get brewing stand
			BrewingStand stand = (BrewingStand) e.getClickedBlock().getState();
			
			//If clicked with a water potion, transfer to stand.
			if (isWaterPotion(e.getPlayer().getItemInHand()))
			{
				transferToStand(e.getPlayer(), stand);
				return;
			}
			
			//If clicked with an ingredient, transfer the ingredient to stand.
			for (Material m : ingredients)
				if (e.getPlayer().getItemInHand().getType() == m)
				{
					transferIngredientToStand(e.getPlayer(), stand, m);
					return;
				}
			
			//If clicked with thier hand, transfer to player.
			if (e.getPlayer().getItemInHand().getType() == Material.AIR)
			{
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
	
	//Misc
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
