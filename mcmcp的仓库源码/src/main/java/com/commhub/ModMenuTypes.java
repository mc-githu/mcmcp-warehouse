package com.commhub;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, CommHub.MODID);

    public static final Supplier<MenuType<HubMenu>> HUB_MENU =
            MENU_TYPES.register("hub_menu", () -> IMenuTypeExtension.create(HubMenu::new));

    public static final Supplier<MenuType<TradeMenu>> TRADE_MENU =
            MENU_TYPES.register("trade_menu", () -> IMenuTypeExtension.create(TradeMenu::new));

    public static final Supplier<MenuType<CraftingCardMenu>> CRAFTING_CARD_MENU =
            MENU_TYPES.register("crafting_card_menu", () -> IMenuTypeExtension.create(CraftingCardMenu::new));

    public static final Supplier<MenuType<SimpleCardMenu>> SIMPLE_CARD_MENU =
            MENU_TYPES.register("simple_card_menu", () -> IMenuTypeExtension.create(SimpleCardMenu::new));

    public static final Supplier<MenuType<WarehouseTargetMenu>> WAREHOUSE_TARGET =
            MENU_TYPES.register("warehouse_target", () -> IMenuTypeExtension.create(WarehouseTargetMenu::new));
}
