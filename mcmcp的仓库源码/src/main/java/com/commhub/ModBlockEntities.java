package com.commhub;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CommHub.MODID);

    public static final Supplier<BlockEntityType<WarehouseBlockEntity>> WAREHOUSE =
            BLOCK_ENTITIES.register("warehouse",
                    () -> BlockEntityType.Builder.of(WarehouseBlockEntity::new,
                            ModBlocks.WAREHOUSE_INPUT.get(), ModBlocks.WAREHOUSE_OUTPUT.get()).build(null));

}
