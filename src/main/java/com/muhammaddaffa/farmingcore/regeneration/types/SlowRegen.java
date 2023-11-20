package com.muhammaddaffa.farmingcore.regeneration.types;

import com.muhammaddaffa.mdlib.MDLib;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.scheduler.BukkitRunnable;

public class SlowRegen extends BukkitRunnable {

    public static void active(Block block, Material material, BlockData blockData) {
        // create a new instance
        SlowRegen task = new SlowRegen(block, material, blockData);
        // start the task
        task.runTaskTimer(MDLib.getInstance(), 0L, 1L);
    }

    private final Block block;
    private final Material material;
    private final BlockData blockData;

    private boolean first = true;

    public SlowRegen(Block block, Material material, BlockData blockData) {
        this.block = block;
        this.material = material;
        this.blockData = blockData;
    }

    @Override
    public void run() {
        if (this.first) {
            this.block.setType(this.material);
        }
        // if the block is not ageable, cancel it
        if (!(this.block.getBlockData() instanceof Ageable ageable)) {
            this.block.setBlockData(this.blockData);
            this.cancel();
            return;
        }
        // if the age is maximum age, skip it
        if (ageable.getAge() == ageable.getMaximumAge()) {
            this.cancel();
            return;
        }
        // check if it's the first time
        if (this.first) {
            // set the boolean first
            this.first = false;
            // set the age to level 1
            ageable.setAge(1);
        } else {
            // increase age by one
            ageable.setAge(ageable.getAge() + 1);
        }
        this.block.setBlockData(ageable);
    }

}
