package com.muhammaddaffa.farmingcore.hoes.gui;

import com.muhammaddaffa.farmingcore.NextFarming;
import com.muhammaddaffa.farmingcore.hoes.Hoe;
import com.muhammaddaffa.farmingcore.hoes.managers.HoeManager;
import com.muhammaddaffa.mdlib.MDLib;
import com.muhammaddaffa.mdlib.gui.SimpleInventory;
import com.muhammaddaffa.mdlib.hooks.VaultEconomy;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Config;
import com.muhammaddaffa.mdlib.utils.ItemBuilder;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class HoeUpgradeInventory extends SimpleInventory {

    public static void openInventory(Player player, Hoe hoe, HoeManager hoeManager) {
        // get the config
        FileConfiguration config = Config.getFileConfiguration(NextFarming.HOES_CONFIG);
        // get all variables we want
        String title = config.getString("gui.title");
        int size = config.getInt("gui.size");
        // create a new inventory instance and open it to the player
        new HoeUpgradeInventory(size, title, player, hoe, hoeManager).open(player);
    }


    private final Player player;
    private Hoe hoe;
    private final HoeManager hoeManager;

    public HoeUpgradeInventory(int size, String title, Player player, Hoe hoe, HoeManager hoeManager) {
        super(size, Common.color(title));
        this.player = player;
        this.hoe = hoe;
        this.hoeManager = hoeManager;

        this.setAllItems();
    }

    private void setAllItems() {
        // clear the inventory first
        for (int i = 0; i < this.getInventory().getSize(); i++) {
            this.setItem(i, new ItemStack(Material.AIR));
        }
        // fill the inventory
        this.fillInventory();
        // get the next hoe
        Hoe nextHoe = this.hoeManager.getHoe(this.hoe.tier() + 1);
        // check if the hoe is on last tier
        if (nextHoe == null) {
            this.setItem(13, new ItemBuilder(Material.RED_DYE)
                    .name("&cYou're on the last tier")
                    .lore("&7You are not able to upgrade your hoe", "&7anymore, it has reached the last tier.")
                    .build());
            return;
        }
        // set the current hoe
        this.setItem(11, this.hoe.item());
        // set the next hoe
        this.setItem(15, nextHoe.item());
        // set the upgrade button
        String displayName = "&aUpgrade your hoe &7(&e{current} &f-> &e{next}&7)"
                .replace("{current}", this.hoe.getTierString())
                .replace("{next}", nextHoe.getTierString());

        double balance = VaultEconomy.getBalance(this.player);
        Material material;
        String cost;
        String footer;
        String remaining;
        if (balance >= this.hoe.upgradeCost()) {
            material = Material.LIME_DYE;
            cost = "&a$" + Common.digits(this.hoe.upgradeCost());
            footer = "&aClick to upgrade!";
            remaining = "&7You are able to purchase this upgrade!";
        } else {
            material = Material.RED_DYE;
            cost = "&c$" + Common.digits(this.hoe.upgradeCost());
            footer = "&c> Insufficient Funds <";
            remaining = "&cYou need &a$" + Common.digits(this.hoe.upgradeCost() - balance) + " &cmore!";
        }

        Placeholder placeholder = new Placeholder()
                .add("{cost}", cost)
                .add("{footer}", footer)
                .add("{remaining}", remaining);

        List<String> lore = placeholder.translate(List.of(
                "&8Farming",
                "&f",
                "&fUpgrade Cost:",
                "{cost}",
                "&7",
                "{remaining}",
                "{footer}"
        ));

        this.setItem(13, new ItemBuilder(material)
                .name(displayName)
                .lore(lore)
                .build(), event -> {
            // if player has enough money, upgrade the hoe
            if (balance < this.hoe.upgradeCost()) {
                Common.configMessage(NextFarming.DEFAULT_CONFIG, this.player, "messages.not-enough-money", new Placeholder()
                        .add("{money}", Common.digits(VaultEconomy.getBalance(this.player)))
                        .add("{upgradecost}", Common.digits(this.hoe.upgradeCost()))
                        .add("{remaining}", Common.digits(balance - this.hoe.upgradeCost())));
                return;
            }
            // upgrade the current hoe
            // remove the item in main hand first
            this.player.getInventory().setItemInMainHand(null);
            // set it back a tick later
            Bukkit.getScheduler().runTask(MDLib.getInstance(), () -> {
                this.player.getInventory().setItemInMainHand(nextHoe.item());
            });
            // actually withdraw the money
            VaultEconomy.withdraw(this.player, this.hoe.upgradeCost());
            // send message to the player
            Common.configMessage(NextFarming.DEFAULT_CONFIG, this.player, "messages.hoe-upgrade", new Placeholder()
                    .add("{tier}", nextHoe.getTierString())
                    .add("{from}", hoe.getTierString())
                    .add("{to}", nextHoe.getTierString()));
            // refresh the gui
            this.hoe = nextHoe;
            this.setAllItems();
            // play sound
            this.player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
        });
    }

    private void fillInventory() {
        // get the config
        FileConfiguration config = Config.getFileConfiguration(NextFarming.HOES_CONFIG);
        // get all variables we want
        boolean enabled = config.getBoolean("gui.fill_item.enabled");
        ItemStack stack = ItemBuilder.fromConfig(config, "gui.fill_item").build();
        // if the fill item is not enabled, just return
        if (!enabled) {
            return;
        }
        // proceeds to fill the inventory
        for (int i = 0; i < this.getInventory().getSize(); i++) {
            // proceeds to place the filler item
            ItemStack item = this.getInventory().getItem(i);
            if (item == null || item.getType() == Material.AIR) {
                this.setItem(i, stack);
            }
        }
    }

}
