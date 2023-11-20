package com.muhammaddaffa.farmingcore.hoes;

import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record Hoe(
        int tier,
        ItemStack item,
        List<PotionEffect> effects,
        boolean quickRegrowth,
        int fortune,
        double upgradeCost,
        Map<String, Double> expData,
        FarmBombData farmBomb
) {

    public String getTierString() {
        return tier + "";
    }

    @Nullable
    public Double getXp(String material) {
        return this.expData.get(material);
    }

    public record FarmBombData(
            boolean enabled,
            double timer,
            int yield
    ) {

    }

}
