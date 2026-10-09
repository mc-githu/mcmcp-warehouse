package com.commhub;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 权限管理工具（针对管理员拥有的私人仓库）。
 */
public class PermissionToolItem extends Item {
    public enum Kind { ADD, REMOVE, ADMIN }

    private final Kind kind;

    public PermissionToolItem(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        net.minecraft.nbt.CompoundTag tag = stack.getOrDefault(
                net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        String wh = tag.contains("whname") ? tag.getString("whname") : "";
        if (kind == Kind.ADMIN) {
            // 管理员工具：NBT 里的 isGrant 标记（生成时写入）决定显示给予还是转让
            boolean isGrant = tag.contains("isgrant") && tag.getBoolean("isgrant");
            String whName = wh.isEmpty() ? "某仓库" : wh;
            tooltip.add(Component.literal(isGrant
                    ? "给予管理员：把「" + whName + "」的管理员交给对方"
                    : "转让管理员：把「" + whName + "」的管理员转给对方"));
            tooltip.add(Component.literal("归于 " + whName + "，仅主人/管理员可使用"));
        } else {
            String owner = wh.isEmpty() ? "某仓库" : wh;
            tooltip.add(Component.literal("归于 " + owner + "，仅主人/管理员可使用"));
        }
    }

    /** 读工具 NBT 里的归属仓库名 */
    private static String warehouseOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        net.minecraft.nbt.CompoundTag tag = stack.getOrDefault(
                net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        return tag.contains("whname") ? tag.getString("whname") : "";
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer self) || !(target instanceof ServerPlayer targetPlayer)) return InteractionResult.PASS;

        HubSavedData data = HubSavedData.get(self.getServer());
        String targetName = targetPlayer.getGameProfile().getName();
        switch (kind) {
            case ADD -> {
                String wh = warehouseOf(stack);
                int n = data.addMemberTo(wh, self.getUUID(), targetPlayer.getUUID());
                if (n < 0) {
                    self.sendSystemMessage(Component.literal("你不是「" + wh + "」的管理员，不能用这把工具"));
                } else if (n > 0) {
                    self.sendSystemMessage(Component.literal("已把 " + targetName + " 加为「" + wh + "」的成员"));
                    targetPlayer.sendSystemMessage(Component.literal(self.getGameProfile().getName() + " 把你加为了「" + wh + "」的成员"));
                } else {
                    self.sendSystemMessage(Component.literal(targetName + " 已经是「" + wh + "」的成员了"));
                }
            }
            case REMOVE -> {
                String wh = warehouseOf(stack);
                int n = data.removeMemberFrom(wh, self.getUUID(), targetPlayer.getUUID());
                if (n < 0) {
                    self.sendSystemMessage(Component.literal("你不是「" + wh + "」的管理员，不能用这把工具"));
                } else {
                    self.sendSystemMessage(n > 0
                            ? Component.literal("已从「" + wh + "」移除 " + targetName)
                            : Component.literal(targetName + " 不是「" + wh + "」的成员"));
                }
            }
            case ADMIN -> {
                String wh = warehouseOf(stack);
                int r = data.grantOrTransferAdmin(wh, self.getUUID(), targetPlayer.getUUID());
                if (r < 0) {
                    self.sendSystemMessage(Component.literal("你没有「" + wh + "」的权限，不能用这把管理员工具"));
                    return InteractionResult.SUCCESS;
                }
                // 给予/转让成功：收回自己手里这把管理员工具（已失去管理员身份，防止越权）
                if (wh != null && !wh.isEmpty()) {
                    stack.shrink(1);
                    if (stack.isEmpty() && self.getMainHandItem() == stack) {
                        self.setItemInHand(hand, ItemStack.EMPTY);
                    }
                }
                self.sendSystemMessage(Component.literal("已把「" + wh + "」的管理员给了 " + targetName + "（你自己不再担任管理员，主人身份保留）"));
                targetPlayer.sendSystemMessage(Component.literal(self.getGameProfile().getName() + " 让你成为了「" + wh + "」的管理员"));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
