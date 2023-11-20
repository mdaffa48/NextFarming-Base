package com.muhammaddaffa.farmingcore.utils;

import com.muhammaddaffa.farmingcore.NextFarming;
import com.muhammaddaffa.mdlib.utils.Config;
import org.bukkit.entity.Player;

public class Utils {

    public static boolean isInsideFarmingWorld(Player player) {
        String name = player.getWorld().getName();
        return Config.getFileConfiguration(NextFarming.DEFAULT_CONFIG).getStringList("farming-worlds").contains(name);
    }

}
