package com.muhammaddaffa.farmingcore;

import com.muhammaddaffa.farmingcore.commands.MainCommand;
import com.muhammaddaffa.farmingcore.hoes.managers.HoeListener;
import com.muhammaddaffa.farmingcore.hoes.managers.HoeManager;
import com.muhammaddaffa.farmingcore.hoes.runnable.HoeTask;
import com.muhammaddaffa.farmingcore.regeneration.BlockRegen;
import com.muhammaddaffa.mdlib.MDLib;
import com.muhammaddaffa.mdlib.utils.Config;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

public final class NextFarming extends JavaPlugin {

    public static final String DEFAULT_CONFIG = "config.yml";
    public static final String HOES_CONFIG = "hoes.yml";
    public static final String DATA_CONFIG = "data.yml";

    private HoeManager hoeManager;

    @Override
    public void onLoad() {
        MDLib.inject(this);
    }

    @Override
    public void onEnable() {
        MDLib.onEnable(this);
        // register configs
        Config.registerConfig(new Config(DEFAULT_CONFIG, null, true));
        Config.registerConfig(new Config(HOES_CONFIG, null, true));
        Config.registerConfig(new Config(DATA_CONFIG, null, false));

        // initialize the hoe manager, and load all the important stuff
        this.hoeManager = new HoeManager(this);
        this.hoeManager.load();

        // register listeners & commands
        this.registerListeners();
        this.registerCommands();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        MDLib.shutdown();
        BlockRegen.stopAll();
        // remove all effects from players
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
        }
    }

    private void registerCommands() {
        MainCommand command = new MainCommand(this.hoeManager);
        this.getCommand("farmingcore").setExecutor(command);
        this.getCommand("farmingcore").setTabCompleter(command);
    }

    private void registerListeners() {
        PluginManager pm = Bukkit.getPluginManager();

        // -------------------------------------------------------
        // hoe stuff
        // create farmbomb instance
        HoeListener.FarmBomb farmBomb = new HoeListener.FarmBomb(this.hoeManager);
        // start the farmbomb task
        farmBomb.start();
        // finally, register the listeners
        pm.registerEvents(new HoeListener(this.hoeManager, farmBomb), this);
        // start the hoe task
        new HoeTask(this.hoeManager, farmBomb).runTaskTimer(this, 20L, 5L);
        // -------------------------------------------------------
    }

    public static FileConfiguration getDataConfig() {
        return Config.getFileConfiguration(DATA_CONFIG);
    }

    public static FileConfiguration getDefaultConfig() {
        return Config.getFileConfiguration(DEFAULT_CONFIG);
    }

}
