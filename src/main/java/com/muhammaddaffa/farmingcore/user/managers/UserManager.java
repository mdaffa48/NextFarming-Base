package com.muhammaddaffa.farmingcore.user.managers;

import com.muhammaddaffa.farmingcore.NextFarming;
import com.muhammaddaffa.farmingcore.user.User;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Config;
import com.muhammaddaffa.mdlib.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UserManager {

    private final Map<UUID, User> userMap = new HashMap<>();

    @NotNull
    public User getOrCreate(Player player) {
        return this.getOrCreate(player.getUniqueId());
    }

    @NotNull
    public User getOrCreate(UUID uuid) {
        return this.userMap.computeIfAbsent(uuid, e -> new User(uuid));
    }

    public void addXp(Player player, double amount) {
        // get all variables
        FileConfiguration config = NextFarming.getDefaultConfig();
        User user = this.getOrCreate(player);
        int currentLevel = user.getLevel();
        // add the xp to the user
        user.addXp(amount);
        // check if the level is different
        if (currentLevel != user.getLevel() && config.get("infinite-levels.rewards." + user.getLevel()) != null) {
            // send title
            String title = config.getString("infinite-levels.rewards." + user.getLevel() + ".titles.title");
            String subTitle = config.getString("infinite-levels.rewards." + user.getLevel() + ".titles.sub-title");
            player.sendTitle(Common.color(title), Common.color(subTitle), 20, 40, 20);
            // give player reward
            for (String command : config.getStringList("infinite-levels.rewards." + user.getLevel() + ".commands")) {
                // execute console commands
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command
                        .replace("{player}", player.getName()));
            }
        }
    }

    public void removeXp(Player player, double amount) {
        this.getOrCreate(player).removeXp(amount);
    }

    public void setXp(Player player, double amount) {
        this.getOrCreate(player).setXp(amount);
    }

    public int getLevel(Player player) {
        return this.getOrCreate(player).getLevel();
    }

    public void load() {
        FileConfiguration config = NextFarming.getDataConfig();
        // check if there are any data
        if (!config.isConfigurationSection("users")) {
            return;
        }
        // loop through all data
        for (String uuidString : config.getConfigurationSection("users").getKeys(false)) {
            // get all data
            UUID uuid = UUID.fromString(uuidString);
            double xp = config.getDouble("users." + uuidString + ".xp");
            int tokens = config.getInt("users." + uuidString + ".tokens");
            // store it on the cache
            this.userMap.put(uuid, new User(uuid, xp, tokens));
        }
        // send log message
        Logger.info("Successfully loaded " + this.userMap.size() + " users data!");
    }

    public void save() {
        Config data = Config.getConfig(NextFarming.DATA_CONFIG);
        FileConfiguration config = data.getConfig();
        // loop through all users
        for (User user : this.userMap.values()) {
            // save all info
            config.set("users." + user.getUniqueId().toString() + ".xp", user.getTotalXp());
            config.set("users." + user.getUniqueId().toString() + ".tokens", user.getTokens());
        }
        // finally save the data
        data.saveConfig();
        // send log message
        Logger.info("Successfully saved " + this.userMap.size() + " users data!");
    }

}
