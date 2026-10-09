package com.commhub;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = CommHub.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModCapabilities {
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.WAREHOUSE.get(),
                (blockEntity, side) -> blockEntity.getHandler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.WAREHOUSE.get(),
                (blockEntity, side) -> blockEntity.getFluidHandler());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.WAREHOUSE.get(),
                (blockEntity, side) -> blockEntity.getEnergyStorage());
    }
}
