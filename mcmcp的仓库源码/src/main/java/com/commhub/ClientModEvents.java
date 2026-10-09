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

    // 2D 成交动画（TradeEffectRenderer）已停用：
    // 它注册为 GUI 最上层图层，会把 3D 的裂缝 / 锁链 / 包裹整个盖住。
    // 成交动画现在完全由 RiftEntity + RiftRenderer 负责。

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
