package com.commhub;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 合成升级卡：<b>拿着它右键空气</b>就能打开配置界面（界面里带玩家背包，
 * 没装 JEI 也可以从背包拿物品点进幽灵格）。配好后把卡放进私人仓库的卡槽，
 * 仓库就会按卡上的配方自动合成。
 */
public class CraftingUpgradeCardItem extends Item {

    public CraftingUpgradeCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            sp.openMenu(new SimpleMenuProvider(
                            (id, inv, p) -> new CraftingCardMenu(id, inv, hand.ordinal()),
                            Component.literal("合成升级卡 · 配置")),
                    buf -> buf.writeInt(hand.ordinal()));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int n = 0;
        try {
            if (context.registries() != null) {
                n = CraftingCard.configuredCount(stack, context.registries());
            }
        } catch (Exception ignored) {
        }
        tooltip.add(Component.literal("右键空气打开配置界面（从背包或 JEI 把材料点进配方格）").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("配好后放进私人仓库的卡槽（最多 3 张），仓库会按配方自动合成").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("已配置合成目标：" + n + " / " + CraftingCard.TARGETS).withStyle(ChatFormatting.AQUA));
    }
}
