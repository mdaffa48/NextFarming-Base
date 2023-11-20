package com.muhammaddaffa.farmingcore.hoes.runnable;

import com.muhammaddaffa.farmingcore.hoes.Hoe;
import com.muhammaddaffa.farmingcore.hoes.managers.HoeListener;
import com.muhammaddaffa.farmingcore.hoes.managers.HoeManager;
import com.muhammaddaffa.farmingcore.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HoeTask extends BukkitRunnable {

    private final Map<UUID, Hoe> cacheMap = new HashMap<>();

    private final HoeManager hoeManager;
    private final HoeListener.FarmBomb farmBomb;
    public HoeTask(HoeManager hoeManager, HoeListener.FarmBomb farmBomb) {
        this.hoeManager = hoeManager;
        this.farmBomb = farmBomb;
    }

    @Override
    public void run() {
        // loop through all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            // get all variables we want
            ItemStack hand = player.getInventory().getItemInMainHand();
            Hoe hoe = this.hoeManager.getHoe(hand);
            // check if player holding a hoe or not
            if (hoe == null) {
                // if player is not holding hoe, remove effects
                this.removeEffects(player);
            } else {
                // if player outside farming world, continue
                if (!Utils.isInsideFarmingWorld(player)) {
                    continue;
                }
                // if player holding a hoe, apply effects
                this.applyEffects(player, hoe);
                // if the hoe is a farm bomb
                // register the player
                if (hoe.farmBomb().enabled()) {
                    this.farmBomb.register(player);
                }
            }
        }
    }

    private void applyEffects(Player player, Hoe hoe) {
        // loop through all potion effects that the hoe has
        for (PotionEffect effect : hoe.effects()) {
            // apply the potion effect to the player
            player.addPotionEffect(effect);
        }
        // if the player is not cached yet
        if (!this.cacheMap.containsKey(player.getUniqueId())) {
            // cache the player with the hoe
            this.cacheMap.put(player.getUniqueId(), hoe);
        }
    }

    private void removeEffects(Player player) {
        // get hoe that player holds
        Hoe hoe = this.cacheMap.remove(player.getUniqueId());
        // if the player is not cached, return
        if (hoe == null) {
            return;
        }
        // loop through all potion effects
        for (PotionEffect effect : hoe.effects()) {
            // remove the potion effect from player
            player.removePotionEffect(effect.getType());
        }
    }

}
