package com.commhub;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ClientScreens {
    @OnlyIn(Dist.CLIENT)
    public static void openCreateDialog() {
        net.minecraft.client.Minecraft.getInstance().setScreen(new WarehouseCreateScreen());
    }

    @OnlyIn(Dist.CLIENT)
    public static void onPublishResult(boolean ok) {
        if (ok) TradeScreen.clearGhosts();
    }


    @OnlyIn(Dist.CLIENT)
    public static void openWarehouseList(net.minecraft.core.BlockPos pos, java.util.List<String> names) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new WarehouseListScreen(pos, names));
    }
}
