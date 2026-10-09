package com.commhub;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, CommHub.MODID);

    public static final Supplier<Block> WAREHOUSE_INPUT = BLOCKS.register("warehouse_input",
            () -> new WarehouseBlock(BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.METAL)));

    public static final Supplier<Block> WAREHOUSE_OUTPUT = BLOCKS.register("warehouse_output",
            () -> new WarehouseBlock(BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.METAL)));


}
