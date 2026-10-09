package com.commhub.jei;

import com.commhub.SimpleCardScreen;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** 机械手 / 动力锯卡片配置界面：支持从 JEI 直接拖物品到两个格子 */
public class SimpleCardGhostHandler implements IGhostIngredientHandler<SimpleCardScreen> {

    @Override
    public <I> List<Target<I>> getTargetsTyped(SimpleCardScreen gui, ITypedIngredient<I> ingredient, boolean doStart) {
        List<Target<I>> targets = new ArrayList<>();
        if (ingredient.getItemStack().isEmpty()) return targets;
        int left = gui.getGuiLeft();
        int top = gui.getGuiTop();
        for (int i = 0; i < 2; i++) {
            final int index = i;
            final Rect2i area = (i == 0)
                    ? new Rect2i(left + SimpleCardScreen.A_X - 2, top + SimpleCardScreen.A_Y - 2, 18, 18)
                    : new Rect2i(left + SimpleCardScreen.B_X - 2, top + SimpleCardScreen.B_Y - 2, 18, 18);
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
