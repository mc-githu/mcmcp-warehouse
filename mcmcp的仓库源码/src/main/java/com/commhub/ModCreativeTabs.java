package com.commhub;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CommHub.MODID);

    public static final Supplier<CreativeModeTab> COMMHUB_TAB = CREATIVE_TABS.register("commhub",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.commhub"))
                    .icon(() -> new ItemStack(ModItems.WAREHOUSE_INPUT.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.WAREHOUSE_INPUT.get());
                        output.accept(ModItems.WAREHOUSE_OUTPUT.get());
                        output.accept(ModItems.MEMBERSHIP_TOKEN.get());
                        output.accept(ModItems.ADD_MEMBER_TOOL.get());
                        output.accept(ModItems.REMOVE_MEMBER_TOOL.get());
                        output.accept(ModItems.TRANSFER_ADMIN_TOOL.get());
                        output.accept(ModItems.AUTOMATION_CORE.get());
                        output.accept(ModItems.CRAFTING_UPGRADE_CARD.get());
                        output.accept(ModItems.DEPLOYER_CARD.get());
                        output.accept(ModItems.SAW_CARD.get());
                        if (ModItems.guideBookAvailable()) {
                            output.accept(ModItems.GUIDE_BOOK.get());
                        }
                    })
                    .build());
}
