package me.kevy007.factiongolem;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.FixedMetadataValue;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.massivecraft.factions.Rel;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.util.IdUtil;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.apache.commons.lang3.StringUtils;

public final class FactionGolem extends JavaPlugin implements Listener
{

    public static String[] strExplode(String stringToExplode,String separator)
    {
        return  StringUtils.splitPreserveAllTokens(stringToExplode, separator);
    }

    Logger logger = Bukkit.getLogger();

    FileManager fileManager = new FileManager(this);

    private static Economy econ = null;

    long omilsec = 0;
    boolean milsecd = false;

    public void onEnable()
    {
        getServer().getPluginManager().registerEvents(this, this);
        if (!setupEconomy() )
        {
            logger.severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        fileManager.getConfig("config.yml").copyDefaults(true).save();
        boolean set = fileManager.getConfig("config.yml").get().getBoolean("DontEditThis");
        if(set == false)
        {
            logger.info("[FactionGolems] Config YML was unset - setting it with default values");
            fileManager.getConfig("config.yml").get().set("DontEditThis", true);
            fileManager.getConfig("config.yml").get().set("MaxGolemsPerFaction", 10);
            fileManager.getConfig("config.yml").get().set("GolemConvertPrice", 500.0);
            fileManager.getConfig("config.yml").get().set("GolemPlayerSearchRadius", 25.0D);
            fileManager.getConfig("config.yml").get().set("GolemSearchEnemyGolemRadius", 40.0D);
            fileManager.getConfig("config.yml").get().set("BannedFactions", "§2Wilderness|§4Warzone");
            fileManager.getConfig("config.yml").get().set("UnlimitedGolems", "§6Safezone|§2Wilderness");
            fileManager.getConfig("config.yml").save();
        }
    }

    private boolean setupEconomy()
    {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args)
    {
        Player player = (Player) sender;

        return false;
    }

    public void onDisable()
    {

    }

    public String getConfigString(String confign)
    {
        return fileManager.getConfig("config.yml").get().getString(confign);
    }

    public double getConfigDouble(String confign)
    {
        return fileManager.getConfig("config.yml").get().getDouble(confign);
    }

    public double getConfigInt(String confign)
    {
        return fileManager.getConfig("config.yml").get().getInt(confign);
    }

    public boolean isFactionBanned(String fName)
    {
        String fList = getConfigString("BannedFactions");
        String[] split = strExplode(fList,"|");
        String tmp = "NaN";
        boolean result = false;
        for(int x = 0; x < split.length; x++)
        {
            if(fName.equals(split[x]))
            {
                result = true;
                break;
            }
        }
        return result;
    }

    public boolean factionHasUnlimitedGolems(String fName)
    {
        String fList = getConfigString("UnlimitedGolems");
        String[] split = strExplode(fList,"|");
        String tmp = "NaN";
        boolean result = false;
        for(int x = 0; x < split.length; x++)
        {
            if(fName.equals(split[x]))
            {
                result = true;
                break;
            }
        }
        return result;
    }

    @EventHandler
    public void onPlayerWalk(PlayerMoveEvent event)
    {
        long milsec = System.currentTimeMillis();
        if(milsecd == false)
        {
            omilsec = milsec;
            milsecd = true;
        }
        long cmilsec = milsec - omilsec;
        if(cmilsec < 200)
        {
            //logger.info("[FactionGolem] Debug Timer - Not fired");
            return;
        }
        else
        {
            omilsec = milsec;
            //logger.info("[FactionGolem] Debug Timer - Now fired");
        }
        Player player = event.getPlayer();
        double searchradius = getConfigDouble("GolemPlayerSearchRadius");
        List<Entity> nearby = player.getNearbyEntities(searchradius, searchradius, searchradius);

        MPlayer mover = null;
        Faction moverfac = null;
        mover = MPlayer.get(player);
        moverfac = mover.getFaction();
        if (isFactionBanned(moverfac.getName()))
        {
            return;
        }
        List<Entity> nearby2ex2 = player.getNearbyEntities(256.0D, 256.0D, 256.0D);
        for (Faction faction : FactionColl.get().getAll())
        {
            int count = 0;
            int countkilled = 0;
            boolean didExceed = false;
            for (Entity entityx : nearby2ex2)
            {
                if (entityx instanceof IronGolem)
                {
                    IronGolem iGolem = (IronGolem) entityx;
                    LivingEntity iGolemEnt = (LivingEntity) entityx;
                    if(iGolemEnt.isCustomNameVisible() == true)
                    {
                        if(faction.getName().equals(iGolemEnt.getCustomName()))
                        {
                            count ++;
                            if(count > getConfigInt("MaxGolemsPerFaction"))
                            {
                                if (factionHasUnlimitedGolems(faction.getName()))
                                {

                                }
                                else
                                {
                                    iGolemEnt.remove();
                                    countkilled++;
                                    count--;
                                    didExceed = true;
                                }
                            }
                        }
                    }
                }
            }
            if(didExceed == true && countkilled > 0)
            {
                for (Player playerx : faction.getOnlinePlayers())
                {
                    playerx.sendMessage(ChatColor.GREEN + "[" + faction.getName() + ChatColor.GREEN + "]: " + ChatColor.RED + countkilled
                            + ChatColor.RED + " of your Faction Golems died, because this faction exceeded the limit of"
                            + " golems alive per faction " + ChatColor.GOLD + "(" + getConfigInt("MaxGolemsPerFaction") + ")"
                            + ChatColor.RED + " at the same time");
                }
            }
        }
        for (Entity entityx : nearby)
        {
            if (entityx instanceof IronGolem)
            {
                IronGolem iGolem = (IronGolem) entityx;
				/*if (iGolem.getTarget() == null || iGolem.getTarget() == player
						|| iGolem.getTarget() == (LivingEntity) player)
				{*/
                LivingEntity iGolemEnt = (LivingEntity) entityx;
                if (iGolemEnt.isCustomNameVisible() /*&& iGolem.hasMetadata("HasOwner")*/)
                {
                    Faction ownerfac = null;
                    ownerfac = FactionColl.get().getByName(iGolemEnt.getCustomName());
                    if (ownerfac == null)
                    {
                        //iGolem.removeMetadata("HasOwner", this);
                        iGolemEnt.setCustomName("");
                        iGolemEnt.setCustomNameVisible(false);
                    }
                    else
                    {
                        double searchradiusgolem = getConfigDouble("GolemSearchEnemyGolemRadius");
                        List<Entity> nearby2 = iGolem.getNearbyEntities(searchradiusgolem, searchradiusgolem, searchradiusgolem);
                        for (Entity entityx2 : nearby2)
                        {
                            if (entityx2 instanceof IronGolem)
                            {
                                IronGolem iGolem2 = (IronGolem) entityx2;
                                LivingEntity iGolemEnt2 = (LivingEntity) entityx2;
									/*if (iGolem2.getTarget() == null || iGolem.getTarget() == iGolemEnt2
											|| iGolem2.getTarget() == iGolemEnt)
									{*/
                                if (iGolemEnt2.isCustomNameVisible() /*&& iGolem2.hasMetadata("HasOwner")*/)
                                {
                                    Faction ownerfac2 = null;
                                    ownerfac2 = FactionColl.get().getByName(iGolemEnt2.getCustomName());
                                    if (ownerfac2 == null)
                                    {
                                        //iGolem2.removeMetadata("HasOwner", this);
                                        iGolemEnt2.setCustomName("");
                                        iGolemEnt2.setCustomNameVisible(false);
                                    }
                                    else
                                    {
                                        if (ownerfac2.getRelationTo(ownerfac) == Rel.ENEMY
                                                || ownerfac.getRelationTo(ownerfac2) == Rel.ENEMY)
                                        {
                                            iGolem.setTarget(iGolemEnt2);
                                            iGolem2.setTarget(iGolemEnt);
                                        }
                                        else
                                        {
                                            if (ownerfac2.getName().equals(ownerfac.getName())
                                                    || ownerfac.getRelationTo(ownerfac2) == Rel.ALLY)
                                            {
                                                if (iGolem.getTarget() == iGolemEnt2 || iGolem.getTarget() == iGolem2)
                                                {
                                                    iGolem.setTarget(null);
                                                    iGolem.setTarget(null);
                                                }
                                                if (iGolem2.getTarget() == iGolemEnt || iGolem2.getTarget() == iGolem)
                                                {
                                                    iGolem2.setTarget(null);
                                                    iGolem2.setTarget(null);
                                                }
                                            }
                                        }
                                        //}
                                    }
                                }
                            }
                        }
                        if (moverfac.getRelationTo(ownerfac) == Rel.ENEMY
                                || ownerfac.getRelationTo(moverfac) == Rel.ENEMY)
                        {
                            if(player.getGameMode() == GameMode.SURVIVAL)
                            {
                                iGolem.setTarget((LivingEntity) player);
                                iGolem.setTarget((LivingEntity) player);
                            }
                        }
                        else
                        {
                            if (moverfac.getName().equals(ownerfac.getName())
                                    || moverfac.getRelationTo(ownerfac) == Rel.ALLY)
                            {
                                if (iGolem.getTarget() == player || iGolem.getTarget() == (LivingEntity) player)
                                {
                                    iGolem.setTarget(null);
                                    iGolem.setTarget(null);
                                }
                            }
                        }
                        //}
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event)
    {
        if (event.getEntity() instanceof IronGolem)
        {
            IronGolem iGolem = (IronGolem) event.getEntity();
            LivingEntity iGolemEnt = (LivingEntity) event.getEntity();
            if (iGolemEnt.isCustomNameVisible() /*&& iGolem.hasMetadata("HasOwner")*/)
            {
                Faction ownerfac = null;
                ownerfac = FactionColl.get().getByName(iGolemEnt.getCustomName());
                if (ownerfac == null)
                {
                    //iGolem.removeMetadata("HasOwner", this);
                    iGolemEnt.setCustomName("");
                    iGolemEnt.setCustomNameVisible(false);
                }
                else
                {
                    if (event.getTarget() instanceof IronGolem)
                    {
                        IronGolem target = (IronGolem) event.getTarget();
                        LivingEntity targetEnt = (LivingEntity) event.getEntity();
                        if (targetEnt.isCustomNameVisible() /*&& target.hasMetadata("HasOwner")*/)
                        {
                            Faction targetfac = null;
                            targetfac = FactionColl.get().getByName(targetEnt.getCustomName());
                            if (targetfac == null)
                            {
                                //iGolem.removeMetadata("HasOwner", this);
                                iGolemEnt.setCustomName("");
                                iGolemEnt.setCustomNameVisible(false);
                            }
                            else
                            {
                                if (isFactionBanned(targetfac.getName()))
                                {
                                    return;
                                }
                                if (targetfac.getName().equals(ownerfac.getName())
                                        || targetfac.getRelationTo(ownerfac) == Rel.ALLY)
                                {
                                    iGolem.setTarget(null);
                                    event.setCancelled(true);
                                }
                            }
                        }
                    }
                    if (event.getTarget() instanceof Player)
                    {
                        Player player = (Player) event.getTarget();
                        MPlayer playerf = MPlayer.get(player);
                        Faction targetfac = null;
                        targetfac = playerf.getFaction();
                        if (targetfac != null) {
                            if (isFactionBanned(targetfac.getName()))
                            {
                                return;
                            }
                            if (targetfac.getName().equals(ownerfac.getName())
                                    || targetfac.getRelationTo(ownerfac) == Rel.ALLY)
                            {
                                iGolem.setTarget(null);
                                event.setCancelled(true);
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onPlayerLeashEntity(PlayerLeashEntityEvent event)
    {
        Player player = event.getPlayer();
        Entity entity = event.getEntity();
        LivingEntity living = (LivingEntity) entity;
        MPlayer playerm = null;
        playerm = MPlayer.get(player);
        Faction playerfac = null;
        playerfac = playerm.getFaction();
        if (entity instanceof IronGolem)
        {
            IronGolem iGolem = (IronGolem) entity;
            if(living.isCustomNameVisible() == true)
            {
                Faction igfac = null;
                igfac = FactionColl.get().getByName(living.getCustomName());
                if (igfac == null)
                {
                    living.setCustomName("");
                    living.setCustomNameVisible(false);
                    player.sendMessage(ChatColor.GREEN + "This faction golem's faction was disbanded. Reverting!");
                    event.setCancelled(true);
                    player.updateInventory();
                }
                else
                {
                    if (igfac.getName().equals(playerfac.getName()))
                    {
                        player.sendMessage(ChatColor.GREEN + "You have leashed your faction golem.");
                        player.updateInventory();
                    }
                    else
                    {
                        player.sendMessage(ChatColor.RED + "This faction golem does not belong to your faction!");
                        player.updateInventory();
                        event.setCancelled(true);
                    }
                }
            }
            else
            {
                if (isFactionBanned(playerfac.getName()))
                {
                    return;
                }
                else
                {
                    if(econ.getBalance(player.getName()) >= fileManager.getConfig("config.yml").get().getDouble("GolemConvertPrice"))
                    {
                        EconomyResponse r = econ.withdrawPlayer(player.getName(), fileManager.getConfig("config.yml").get().getDouble("GolemConvertPrice"));
                        if(r.transactionSuccess())
                        {
                            Title title = new Title("Congratulations","Successfully converted!",1,2,1);
                            title.setTitleColor(ChatColor.GREEN);
                            title.setSubtitleColor(ChatColor.GREEN);

                            title.send(player);
                            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 0.75F, 0.75F);
                            player.sendMessage(ChatColor.GREEN + "You have successfully converted this golem into faction golem!");
                            player.sendMessage(ChatColor.BLUE + "Price: " + ChatColor.GOLD + "1x Leash + " + fileManager.getConfig("config.yml").get().getDouble("GolemConvertPrice"));
                            living.setCustomName(playerfac.getName());
                            living.setCustomNameVisible(true);
                            ItemStack leash = new ItemStack(Material.LEAD, 1);
                            player.getInventory().removeItem(leash);
                            player.updateInventory();
                        }
                        else
                        {
                            player.sendMessage(String.format("An error occured while making faction golem: %s", r.errorMessage));
                        }
                    }
                    else
                    {
                        Title title = new Title("ERROR","Not enough money!",1,2,1);
                        title.setTitleColor(ChatColor.DARK_RED);
                        title.setSubtitleColor(ChatColor.RED);

                        title.send(player);
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 0.75F, 0.75F);
                        player.sendMessage(ChatColor.RED + "You need " + ChatColor.GOLD + fileManager.getConfig("config.yml").get().getDouble("GolemConvertPrice") +
                                ChatColor.RED + " to convert this golem to faction golem!");
                    }
                    event.setCancelled(true);
                    player.updateInventory();
                }
            }
        }
    }
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event)
    {
        if(event.getRightClicked() instanceof IronGolem)
        {
            Player pl = event.getPlayer();
            ItemStack mainHand = pl.getInventory().getItemInMainHand();
            ItemStack offHand = pl.getInventory().getItemInOffHand();
            if(mainHand.getType() == Material.NAME_TAG || offHand.getType() == Material.NAME_TAG)
            {
                pl.getWorld().playSound(pl.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.75F, 0.75F);
                Title title = new Title("ERROR:","You can't use that on Iron Golems!",1,2,1);
                title.setTitleColor(ChatColor.DARK_RED);
                title.setSubtitleColor(ChatColor.RED);

                title.send(pl);
                //pl.sendMessage(ChatColor.RED + "This server has disabled setting nametag's for Iron Golems!");
                event.setCancelled(true);
            }
        }
    }
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event)
    {
        if(event.getEntity() instanceof IronGolem)
        {
            int randomNum = ThreadLocalRandom.current().nextInt(1, 2 + 1);
            int amount = 1;
            if(randomNum > 1)
            {
                amount += 1;
            }
            ItemStack stack = new ItemStack(Material.IRON_INGOT, amount);
            event.getDrops().clear();
            event.getDrops().add(stack);
        }
    }
    public static Economy getEcononomy()
    {
        return econ;
    }
}

