package com.muhammaddaffa.farmingcore.hoes.managers;

import com.muhammaddaffa.farmingcore.NextFarming;
import com.muhammaddaffa.farmingcore.hoes.Hoe;
import com.muhammaddaffa.mdlib.utils.Config;
import com.muhammaddaffa.mdlib.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class HoeManager {

    private final Map<Integer, Hoe> hoeMap = new HashMap<>();

    private final NamespacedKey hoe_id;
    private final NamespacedKey tnt_bomb;
    public HoeManager(NextFarming plugin) {
        this.hoe_id = new NamespacedKey(plugin, "farmingcore_hoe_id");
        this.tnt_bomb = new NamespacedKey(plugin, "farmingcore_tnt_bomb");
    }

    @Nullable
    public Hoe getHoe(int tier) {
        return this.hoeMap.get(tier);
    }

    @Nullable
    public Hoe getHoe(ItemStack stack) {
        if (stack == null || stack.getItemMeta() == null) {
            return null;
        }
        // get the persistent data stored on the item
        Integer tier = stack.getItemMeta().getPersistentDataContainer().get(this.hoe_id, PersistentDataType.INTEGER);
        // if there is no data stored on the item, return null
        if (tier == null) {
            return null;
        }
        // get the hoe by using getHoe method
        return this.getHoe(tier);
    }

    public List<Hoe> getHoes() {
        return new ArrayList<>(this.hoeMap.values());
    }

    public void load() {
        // first, clear the hoe map
        this.hoeMap.clear();
        // get all variables we want
        FileConfiguration config = Config.getFileConfiguration(NextFarming.HOES_CONFIG);
        // check if there are any farming hoes configured
        if (!config.isConfigurationSection("farming_hoes")) {
            return;
        }
        // loop through all farming hoes
        for (String key : config.getConfigurationSection("farming_hoes").getKeys(false)) {
            // get all variables we want
            int tier = Integer.parseInt(key);
            List<PotionEffect> effects = new ArrayList<>();
            boolean quickRegrowth = config.getBoolean("farming_hoes." + key + ".abilities.quick-regrowth");
            int fortune = config.getInt("farming_hoes." + key + ".abilities.fortune");
            double upgradeCost = config.getDouble("farming_hoes." + key + ".upgrade-cost");
            ItemStack item = ItemBuilder.fromConfig(config, "farming_hoes." + key + ".item").build();
            // insert persistent data into the item
            ItemMeta meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(this.hoe_id, PersistentDataType.INTEGER, tier);
            meta.setUnbreakable(true);
            meta.addEnchant(Enchantment.DURABILITY, 20, true);
            meta.addItemFlags(ItemFlag.values());
            // set the item meta to the item
            item.setItemMeta(meta);

            // load all the potion effect
            for (String line : config.getStringList("farming_hoes." + key + ".effects")) {
                // scrap the string to get the potion type and amplifier
                String[] split = line.split(";");
                PotionEffectType type = PotionEffectType.getByName(split[0].toUpperCase());
                int amplifier = Integer.parseInt(split[1]) - 1;
                // if there is no potion with that type, skip
                if (type == null) {
                    continue;
                }
                // create the potion effect instance
                // and add it to the list of effects
                effects.add(new PotionEffect(type, (20 * 2), amplifier, false, false, false));
            }

            // load all hoe exp
            Map<String, Double> expMap = new HashMap<>();
            for (String material : config.getConfigurationSection("farming_hoes." + key + ".hoe-xp").getKeys(false)) {
                double xp = config.getDouble("farming_hoes." + key + ".hoe-xp." + material);
                expMap.put(material.toUpperCase(), xp);
            }

            // farm bomb data
            boolean farmBomb = config.getBoolean("farming_hoes." + key + ".abilities.farm-bomb.enabled");
            double timer = config.getDouble("farming_hoes." + key + ".abilities.farm-bomb.time");
            int range = config.getInt("farming_hoes." + key + ".abilities.farm-bomb.range");
            Hoe.FarmBombData farmBombData = new Hoe.FarmBombData(farmBomb, timer, range);

            // finally, create the Hoe object
            // and store it on the map
            this.hoeMap.put(tier, new Hoe(tier, item, effects, quickRegrowth, fortune, upgradeCost, expMap, farmBombData));
        }
    }

    public boolean isFarmBomb(Entity entity) {
        if (!(entity instanceof TNTPrimed tnt)) {
            return false;
        }
        return tnt.getPersistentDataContainer().has(this.tnt_bomb, PersistentDataType.STRING);
    }

    public Player getFarmBomb(Entity entity) {
        if (!(entity instanceof TNTPrimed tnt)) {
            return null;
        }
        String uuidString = tnt.getPersistentDataContainer().get(this.tnt_bomb, PersistentDataType.STRING);
        if (uuidString == null) {
            return null;
        }
        return Bukkit.getPlayer(UUID.fromString(uuidString));
    }

    public void setFarmBomb(Entity entity, Player igniter) {
        entity.getPersistentDataContainer().set(this.tnt_bomb, PersistentDataType.STRING, igniter.getUniqueId().toString());
    }

}
