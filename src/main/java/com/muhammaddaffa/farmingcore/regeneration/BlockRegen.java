package com.muhammaddaffa.farmingcore.regeneration;

import com.muhammaddaffa.farmingcore.regeneration.types.SlowRegen;
import com.muhammaddaffa.mdlib.utils.Config;
import com.muhammaddaffa.mdlib.utils.Executor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.List;

public class BlockRegen {

    private static final List<BlockRegen> blockRegens = new ArrayList<>();

    public static void register(BlockRegen regen) {
        blockRegens.add(regen);
    }

    public static void stopAll() {
        blockRegens.forEach(BlockRegen::replenish);
    }

    // --------------------------------------------

    private final Block block;
    private final Material material;
    private final BlockData blockData;

    public BlockRegen(Block block) {
        this.block = block;
        this.material = block.getType();
        this.blockData = block.getBlockData();
        BlockRegen.register(this);
    }

    public void replenish() {
        this.block.setType(this.material);
        this.block.setBlockData(this.blockData.clone());
    }

    public void execute(long delay) {
        // set the block to air
        this.block.setType(Material.AIR);
        // start the regeneration task
        Executor.syncLater(delay, () -> {
            /*if (Config.getFileConfiguration("config.yml").getBoolean("instant-regeneration")) {
                this.replenish();
                return;
            }*/
            // start the slow regen
            SlowRegen.active(this.block, this.material, this.blockData);
        });

    }

}
