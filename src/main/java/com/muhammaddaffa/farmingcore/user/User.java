package com.muhammaddaffa.farmingcore.user;

import com.muhammaddaffa.farmingcore.NextFarming;

import java.util.UUID;

public class User {

    private final UUID uuid;
    private double xp;
    private int tokens;

    public User(UUID uuid) {
        this.uuid = uuid;
    }

    public User(UUID uuid, double xp, int tokens) {
        this.uuid = uuid;
        this.xp = xp;
        this.tokens = tokens;
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public void addXp(double amount) {
        this.xp += amount;
    }

    public void removeXp(double amount) {
        double predict = this.xp - amount;
        if (predict < 0) {
            this.xp = 0;
            return;
        }
        this.xp -= amount;
    }

    public void setXp(double amount) {
        this.xp = amount;
    }

    public double getXp() {
        double collective = 0;
        for (int i = 1; i <= this.getLevel(); i++) {
            collective += i * this.getRequiredXp();
        }
        return this.getTotalXp() - collective;
    }

    public double getTotalXp() {
        return xp;
    }

    public double getTotalNeededXp() {
        return (this.getLevel() + 1) * this.getRequiredXp();
    }

    public int getTokens() {
        return tokens;
    }

    public int getLevel() {

        int level = 0;
        double progress = this.getRequiredXp();
        double current = this.getTotalXp();

        for (int i = 0; i < 100000; i++) {
            if (current <= 0) {
                break;
            }

            if ((current - progress) >= 0) {
                current -= progress;
                progress += this.getRequiredXp();
                level++;
            }
        }

        return level;
    }

    public double getRequiredXp() {
        return NextFarming.getDefaultConfig().getDouble("infinite-levels.xp-required");
    }

}
