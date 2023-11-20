package com.muhammaddaffa.farmingcore.hoes.managers;

import com.muhammaddaffa.farmingcore.NextFarming;
import com.muhammaddaffa.farmingcore.hoes.Hoe;
import com.muhammaddaffa.farmingcore.hoes.gui.HoeUpgradeInventory;
import com.muhammaddaffa.farmingcore.regeneration.BlockRegen;
import com.muhammaddaffa.farmingcore.utils.Utils;
import com.muhammaddaffa.mdlib.MDLib;
import com.muhammaddaffa.mdlib.utils.*;
import com.muhammaddaffa.mdlib.xseries.particles.ParticleDisplay;
import net.zerotoil.dev.cyberlevels.objects.levels.PlayerData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public record HoeListener(
        HoeManager hoeManager,
        FarmBomb farmBomb
) implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void wheatDestroy(EntityExplodeEvent event) {
        // get all variables we want
        FileConfiguration config = Config.getFileConfiguration(NextFarming.DEFAULT_CONFIG);
        Entity entity = event.getEntity();
        // check if the entity is a farm bomb
        if (this.hoeManager.isFarmBomb(entity)) {
            // get the player object
            Player player = this.hoeManager.getFarmBomb(entity);
            // check if inside farming worlds
            if (!Utils.isInsideFarmingWorld(player)) {
                event.blockList().clear();
                event.setCancelled(true);
                return;
            }
            // loop through all blocks
            event.blockList().forEach(block -> {
                // we need to check if the block is fully grown wheat
                if (!(block.getBlockData() instanceof Ageable ageable) ||
                        ageable.getAge() != ageable.getMaximumAge()) {
                    return;
                }
                // if the block is not available, return it
                Material material = ageable.getMaterial();
                if (config.get("farming-levels." + material.name()) == null) {
                    return;
                }
                // get the level required and default regeneration time
                int levelRequired = config.getInt("farming-levels." + material.name() + ".level-required");
                int regenerationTime = config.getInt("farming-levels." + material.name() + ".regeneration-time");
                ItemStack item = ItemBuilder.fromConfig(config, "farming-levels." + material.name() + ".item").build();
                // check if player is existed
                if (player != null) {
                    // get the hoe that the player holds
                    Hoe hoe = this.hoeManager.getHoe(player.getInventory().getItemInMainHand());
                    // check if player has enough level
                    PlayerData data = Utils.getCyberLevels().levelCache().playerLevels().get(player);
                    if (data == null || data.getLevel() < levelRequired) {
                        return;
                    }
                    int amount = 1;
                    // check if the hoe is not null
                    if (hoe != null) {
                        // get the new amount
                        amount = this.getAmountFromFortune(hoe.fortune());
                        // and check for quick regrowth ability
                        if (hoe.quickRegrowth()) {
                            regenerationTime = regenerationTime / 2;
                        }
                        // give player exp
                        Double xp = hoe.getXp(material.name());
                        if (xp != null) {
                            Utils.sendExp(player, xp);
                        }
                    }
                    // add the item to the player's inventory
                    item.setAmount(amount);
                    Common.addInventoryItem(player, item);
                    // finally, start the block regen
                    BlockRegen regen = new BlockRegen(block);
                    regen.execute(20L * regenerationTime);
                    // increase the statistic to player
                    player.incrementStatistic(Statistic.MINE_BLOCK, material);
                    // spawn effect on the block
                    Location particleLocation = block.getLocation().clone().add(0.5, 0, 0.5);
                    Executor.async(() -> {
                        ParticleDisplay.of(Particle.REDSTONE)
                                .withColor(Color.RED, 20)
                                .withLocation(particleLocation)
                                .withCount(3)
                                .offset(0.65, 0.75, 0.65)
                                .spawn();
                    });
                }
            });
            // clear all the blocks
            event.blockList().clear();
            // cancel the event
            event.setCancelled(true);
            // play the explosion sound
            player.getWorld().playSound(event.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void wheatDestroy(BlockBreakEvent event) {
        // get all variables we want
        Player player = event.getPlayer();
        Block block = event.getBlock();
        ItemStack stack = player.getInventory().getItemInMainHand();
        FileConfiguration config = Config.getFileConfiguration(NextFarming.DEFAULT_CONFIG);
        // check if inside farming worlds
        if (!Utils.isInsideFarmingWorld(player)) {
            return;
        }
        // check if player is holding a hoe
        Hoe hoe = this.hoeManager.getHoe(stack);
        // check if the block is a crop
        if (!(block.getBlockData() instanceof Ageable ageable) ||
                ageable.getAge() != ageable.getMaximumAge()) {
            return;
        }
        // if the block is not available, return it
        Material material = ageable.getMaterial();
        if (config.get("farming-levels." + material.name()) == null) {
            return;
        }
        // get the level required and default regeneration time
        int levelRequired = config.getInt("farming-levels." + material.name() + ".level-required");
        int regenerationTime = config.getInt("farming-levels." + material.name() + ".regeneration-time");
        ItemStack item = ItemBuilder.fromConfig(config, "farming-levels." + material.name() + ".item").build();
        // check if player isn't breaking the wheat with hoe
        if (hoe == null) {
            Common.configMessage(NextFarming.DEFAULT_CONFIG, player, "messages.break-fail");
            event.setCancelled(true);
            return;
        }
        // check if player has enough level
        PlayerData data = Utils.getCyberLevels().levelCache().playerLevels().get(player);
        if (data == null || data.getLevel() < levelRequired) {
            Common.configMessage(NextFarming.DEFAULT_CONFIG, player, "messages.level-not-enough", new Placeholder()
                    .add("{level}", levelRequired));
            event.setCancelled(true);
            return;
        }
        // cancel the event
        event.setCancelled(true);
        // divide it by 2 if the hoe has quick regrowth ability
        if (hoe.quickRegrowth()) {
            regenerationTime = regenerationTime / 2;
        }
        // finally, start the block regen
        BlockRegen regen = new BlockRegen(block);
        regen.execute(20L * regenerationTime);
        // after checking if the wheat is fully grown
        // we need to calculate the amount of item player going to receive
        // while considering fortune level of the hoe
        int amount = this.getAmountFromFortune(hoe.fortune());
        // add the item to the player's inventory
        item.setAmount(amount);
        Common.addInventoryItem(player, item);
        // increase the statistic to player
        player.incrementStatistic(Statistic.MINE_BLOCK, material);
        // give player exp
        Double xp = hoe.getXp(material.name());
        if (xp != null) {
            Utils.sendExp(player, xp);
        }
        // spawn effect on the block
        Location particleLocation = block.getLocation().clone().add(0.5, 0, 0.5);
        Executor.async(() -> {
            ParticleDisplay.of(Particle.VILLAGER_HAPPY)
                    .withLocation(particleLocation)
                    .withCount(15)
                    .offset(0.65, 0.75, 0.65)
                    .spawn();
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void hoeUpgrade(PlayerInteractEvent event) {
        // get all variables we want
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        Hoe hoe = this.hoeManager.getHoe(hand);
        // if player is not interacting with right hand and not sneaking
        // also, check if player holding a hoe or not return the code
        if (event.getHand() != EquipmentSlot.HAND ||
                !player.isSneaking() ||
                hoe == null) {
            return;
        }
        // check for the action type
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK &&
                event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }
        // open the upgrade inventory
        HoeUpgradeInventory.openInventory(player, hoe, this.hoeManager);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL && Utils.isInsideFarmingWorld(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onDurabilityLoss(PlayerItemDamageEvent event) {
        if (this.hoeManager.getHoe(event.getItem()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void tntDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof TNTPrimed tnt &&
                this.hoeManager.getFarmBomb(tnt) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPhantomSpawn(CreatureSpawnEvent event) {
        if (event.getEntity().getType() == EntityType.PHANTOM) {
            event.setCancelled(true);
        }
    }

    private int getAmountFromFortune(int level) {
        int finalAmount = 1;
        switch (level) {
            case 0 -> finalAmount = 1;
            case 1 -> finalAmount = 3;
            case 2 -> finalAmount = 5;
            case 3 -> finalAmount = 6;
            case 4 -> finalAmount = 7;
            case 5 -> finalAmount = 9;
        }
        return Math.max(1, ThreadLocalRandom.current().nextInt(finalAmount));
    }

    public static class FarmBomb extends BukkitRunnable {

        private final Map<UUID, Integer> active = new HashMap<>();
        private final HoeManager hoeManager;

        public FarmBomb(HoeManager hoeManager) {
            this.hoeManager = hoeManager;
        }

        @Override
        public void run() {
            // loop through all registered players
            for (UUID uuid : this.active.keySet()) {
                // get all variables we want
                Player player = Bukkit.getPlayer(uuid);
                Integer timer = this.active.get(uuid);
                // if the player is not online, just skip
                if (player == null) {
                    continue;
                }
                // get player hold item
                ItemStack hand = player.getInventory().getItemInMainHand();
                Hoe hoe = this.hoeManager.getHoe(hand);
                // if player is not holding a hoe, then skip and set the timer back to 0
                if (hoe == null || !hoe.farmBomb().enabled() || !Utils.isInsideFarmingWorld(player)) {
                    this.active.put(player.getUniqueId(), 0);
                    continue;
                }
                Hoe.FarmBombData farmBomb = hoe.farmBomb();
                // check if the timer more than or equals to 30
                if (timer >= farmBomb.timer()) {
                    // activate the farm bomb
                    // by spawning a primed tnt
                    TNTPrimed tnt = player.getWorld().spawn(player.getLocation().add(0, 15, 0), TNTPrimed.class);
                    // mark the tnt as farm bomb
                    this.hoeManager.setFarmBomb(tnt, player);
                    // set the fuse ticks and the source of the tnt
                    // also make the power of the tnt much bigger
                    tnt.setFuseTicks(50);
                    tnt.setSource(player);
                    tnt.setYield(farmBomb.yield());
                    // send message to the player
                    Common.configMessage(NextFarming.DEFAULT_CONFIG, player, "messages.farm-bomb-active");
                    // set the timer back to 0
                    this.active.put(player.getUniqueId(), 0);
                    continue;
                }
                // increase the timer for player
                this.active.put(uuid, timer + 1);
            }
        }

        public void register(Player player) {
            this.active.putIfAbsent(player.getUniqueId(), 0);
        }

        public void start() {
            this.runTaskTimer(MDLib.getInstance(), 20L, 20L);
        }

    }

}
