package com.commhub;

import com.commhub.network.ModNetworking;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(CommHub.MODID)
public class CommHub {
    public static final String MODID = "commhub";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CommHub(IEventBus modEventBus, ModContainer container) {
        ModItems.ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        ModNetworking.register(modEventBus);
        container.registerConfig(
                net.neoforged.fml.config.ModConfig.Type.SERVER,
                com.commhub.ModConfig.SPEC);
        container.registerConfig(
                net.neoforged.fml.config.ModConfig.Type.CLIENT,
                ModConfig.CLIENT_SPEC);
    }


}
