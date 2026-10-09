package com.commhub.jei;

import com.commhub.CraftingCardScreen;
import com.commhub.TradeScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class CommHubJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath("commhub", "jei");
    }

    /** JEI 里给「自动化核心」显示一页「获取方式」（宝箱来源 JEI 自己看不到，所以写在这里） */
    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addItemStackInfo(
                new net.minecraft.world.item.ItemStack(com.commhub.ModItems.AUTOMATION_CORE.get()),
                net.minecraft.network.chat.Component.literal("获取方式：只能在遗迹宝箱里找到"),
                net.minecraft.network.chat.Component.literal("地牢 / 废弃矿井 / 沙漠神殿 / 丛林神庙 / 雪屋"),
                net.minecraft.network.chat.Component.literal("林地府邸 / 掠夺者前哨 / 沉船宝藏 / 埋藏的宝藏"),
                net.minecraft.network.chat.Component.literal("下界要塞 / 要塞 / 远古城市 / 末地城"),
                net.minecraft.network.chat.Component.literal("堡垒遗迹（4 种）/ 试炼密室"),
                net.minecraft.network.chat.Component.literal("每个宝箱约 20% 概率出现 1 个"),
                net.minecraft.network.chat.Component.literal(""),
                net.minecraft.network.chat.Component.literal("复刻（一次出 1 个）："),
                net.minecraft.network.chat.Component.literal("正中放自动化核心，上方放下界合金锭，"),
                net.minecraft.network.chat.Component.literal("其余 7 格放钻石 → 产出 2 个"),
                net.minecraft.network.chat.Component.literal(""),
                net.minecraft.network.chat.Component.literal("游戏里可用 /commhub loottest 自检这条注入"));
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(TradeScreen.class, new TradeScreenGhostHandler());
        registration.addGhostIngredientHandler(CraftingCardScreen.class, new CraftingCardGhostHandler());
        registration.addGhostIngredientHandler(com.commhub.SimpleCardScreen.class, new SimpleCardGhostHandler());
    }
}
