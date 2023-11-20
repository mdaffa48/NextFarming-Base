package com.muhammaddaffa.farmingcore.commands;

import com.muhammaddaffa.farmingcore.NextFarming;
import com.muhammaddaffa.farmingcore.hoes.Hoe;
import com.muhammaddaffa.farmingcore.hoes.managers.HoeManager;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Config;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record MainCommand(
        HoeManager hoeManager
) implements TabExecutor {

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();

            // add suggestion for admin commands
            if (sender.hasPermission("farmingcore.admin")) {
                suggestions.add("hoe");
                suggestions.add("reload");
            }
            return suggestions;
        }

        if (sender.hasPermission("farmingcore.admin")) {
            // hoe give <player> <tier> [amount]
            List<String> arguments = List.of("hoe");
            if (args.length == 2 && arguments.contains(args[0].toLowerCase())) {
                return null;
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("hoe")) {
                List<String> suggestions = this.hoeManager.getHoes()
                        .stream()
                        .map(Hoe::getTierString)
                        .toList();

                return StringUtil.copyPartialMatches(args[2], suggestions, new ArrayList<>());
            }
        }

        return new ArrayList<>();
    }
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {

        if (args.length == 0) {
            Common.sendMessage(sender, "&cInvalid argument!");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "hoe", "hoes" -> this.hoe(sender, args);
            case "reload" -> this.reload(sender);
            default -> Common.sendMessage(sender, "&cInvalid arguments!");
        }

        return true;
    }

    private void hoe(CommandSender sender, String[] args) {
        if (!sender.hasPermission("farmingcore.admin")) {
            Common.configMessage(NextFarming.DEFAULT_CONFIG, sender, "messages.no-permission");
            return;
        }
        // hoe give <player> <tier> [amount]
        if (args.length < 3) {
            Common.sendMessage(sender, "&cUsage: /farmingcore hoe <player> <tier> [amount]");
            return;
        }
        Player player = Bukkit.getPlayer(args[1]);
        if (player == null) {
            Common.configMessage(NextFarming.DEFAULT_CONFIG, sender, "messages.target-not-found");
            return;
        }
        if (!Common.isInt(args[2])) {
            Common.configMessage(NextFarming.DEFAULT_CONFIG, sender, "messages.not-int");
            return;
        }
        Hoe hoe = this.hoeManager.getHoe(Integer.parseInt(args[2]));
        if (hoe == null) {
            Common.configMessage(NextFarming.DEFAULT_CONFIG, sender, "messages.no-tier");
            return;
        }
        int amount = 1;
        if (args.length >= 4 && Common.isInt(args[3])) {
            amount = Integer.parseInt(args[3]);
        }
        amount = Math.max(1, amount);
        // actually give the item to the player
        for (int i = 0; i < amount; i++) {
            Common.addInventoryItem(player, hoe.item());
        }
        // send message to the sender
        Common.configMessage(NextFarming.DEFAULT_CONFIG, sender, "messages.give-hoe", new Placeholder()
                .add("{amount}", amount)
                .add("{tier}", hoe.tier())
                .add("{player}", player.getName()));
        // send message to the receiver
        Common.configMessage(NextFarming.DEFAULT_CONFIG, player, "messages.receive-hoe", new Placeholder()
                .add("{amount}", amount)
                .add("{tier}", hoe.tier()));
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("farmingcore.admin")) {
            Common.configMessage(NextFarming.DEFAULT_CONFIG, sender, "messages.no-permission");
            return;
        }
        // actually reload the config
        Config.reload();
        // after that, load back the tools
        this.hoeManager.load();
        // send message to the sender
        Common.configMessage(NextFarming.DEFAULT_CONFIG, sender, "messages.reload");
    }

}
