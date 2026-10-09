package com.commhub.jei;

import com.commhub.TradeMenu;
import com.commhub.TradeScreen;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class TradeScreenGhostHandler implements IGhostIngredientHandler<TradeScreen> {
    @Override
    public <I> List<Target<I>> getTargetsTyped(TradeScreen gui, ITypedIngredient<I> ingredient, boolean doStart) {
        List<Target<I>> targets = new ArrayList<>();
        // 只在「发布悬赏」页有幽灵格，「换物」页不接受拖拽
        if (gui.getMenu().getMode() != TradeMenu.MODE_BOUNTY) return targets;
        // 只接受物品（从 JEI 物品列表拖来的）
        if (ingredient.getItemStack().isEmpty()) return targets;

        int left = gui.getGuiLeft();
        int top = gui.getGuiTop();
        for (int i = 0; i < TradeMenu.TRADE_SLOTS; i++) {
            final int index = i;
            // 「我给」格
            Rect2i offerArea = new Rect2i(left + 110 + i * 18, top + 22, 16, 16);
            targets.add(new Target<>() {
                @Override
                public Rect2i getArea() {
                    return offerArea;
                }

                @Override
                public void accept(I ingredient) {
                    if (ingredient instanceof ItemStack stack) {
                        gui.setOfferGhost(index, stack);
                    }
                }
            });
            // 「我要」格
            Rect2i wantArea = new Rect2i(left + 110 + i * 18, top + 58, 16, 16);
            targets.add(new Target<>() {
                @Override
                public Rect2i getArea() {
                    return wantArea;
                }

                @Override
                public void accept(I ingredient) {
                    if (ingredient instanceof ItemStack stack) {
                        gui.setWantGhost(index, stack);
                    }
                }
            });
        }
        return targets;
    }

    @Override
    public void onComplete() {
    }
}
