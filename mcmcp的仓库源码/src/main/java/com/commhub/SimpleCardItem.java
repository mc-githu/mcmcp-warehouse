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
 * 机械手卡片 / 动力锯卡片：<b>拿着右键空气</b>打开配置界面（界面里带背包）。
 * 配好后放进私人仓库的卡槽，仓库就会自动干活。
 */
public class SimpleCardItem extends Item {

    private final SimpleCard.Kind kind;

    public SimpleCardItem(SimpleCard.Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public SimpleCard.Kind kind() {
        return kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            sp.openMenu(new SimpleMenuProvider(
                            (id, inv, p) -> new SimpleCardMenu(id, inv, hand.ordinal()),
                            Component.literal(kind == SimpleCard.Kind.DEPLOYER ? "机械手卡片 · 配置" : "动力锯卡片 · 配置")),
                    buf -> buf.writeInt(hand.ordinal()));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (kind == SimpleCard.Kind.DEPLOYER) {
            tooltip.add(Component.literal("右键空气打开配置：选「右键 / 左键」+ 用哪个物品").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("（不包含攻击，只做使用 / 破坏）").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.literal("右键空气打开配置：放输入物品 + 选定产物").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("按切割配方加工，一个输入有多个产物时也不会选错").withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.literal("配好后放进私人仓库的卡槽").withStyle(ChatFormatting.GRAY));
    }
}
