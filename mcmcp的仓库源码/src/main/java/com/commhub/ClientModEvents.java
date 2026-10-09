package com.commhub;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = CommHub.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ModKeyBindings.OPEN_HUB);
    }

    /** 把成交动画注册成「画在最上层」的 GUI 图层 */
    @SubscribeEvent
    public static void registerGuiLayers(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(CommHub.MODID, "trade_effect"),
                TradeEffectRenderer::render);
    }

    /** 空间裂缝的 3D 渲染器 */
    @SubscribeEvent
    public static void registerEntityRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.RIFT.get(), RiftRenderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.HUB_MENU.get(), HubScreen::new);
        event.register(ModMenuTypes.WAREHOUSE_TARGET.get(), WarehouseTargetScreen::new);
        event.register(ModMenuTypes.TRADE_MENU.get(), TradeScreen::new);
        event.register(ModMenuTypes.CRAFTING_CARD_MENU.get(), CraftingCardScreen::new);
        event.register(ModMenuTypes.SIMPLE_CARD_MENU.get(), SimpleCardScreen::new);
    }
}
