package com.commhub.jei;

import com.commhub.CraftingCardScreen;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** 合成升级卡配置界面：支持从 JEI 直接拖物品到配方格/产物格 */
public class CraftingCardGhostHandler implements IGhostIngredientHandler<CraftingCardScreen> {

    @Override
    public <I> List<Target<I>> getTargetsTyped(CraftingCardScreen gui, ITypedIngredient<I> ingredient, boolean doStart) {
        List<Target<I>> targets = new ArrayList<>();
        if (ingredient.getItemStack().isEmpty()) return targets;
        int left = gui.getGuiLeft();
        int top = gui.getGuiTop();
        for (int i = 0; i < 10; i++) {
            final int index = i;
            final Rect2i area;
            if (i < 9) {
                area = new Rect2i(left + CraftingCardScreen.GRID_X + (i % 3) * CraftingCardScreen.SLOT,
                        top + CraftingCardScreen.GRID_Y + (i / 3) * CraftingCardScreen.SLOT, 16, 16);
            } else {
                area = new Rect2i(left + CraftingCardScreen.RESULT_X, top + CraftingCardScreen.RESULT_Y, 16, 16);
            }
            targets.add(new Target<>() {
                @Override
                public Rect2i getArea() {
                    return area;
                }

                @Override
                public void accept(I ing) {
                    if (ing instanceof ItemStack stack) gui.setGhost(index, stack);
                }
            });
        }
        return targets;
    }

    @Override
    public void onComplete() {
    }
}
