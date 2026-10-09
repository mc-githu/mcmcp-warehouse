package com.commhub;

import com.commhub.network.ModNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

public class MembershipTokenItem extends Item {
    public MembershipTokenItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            HubSavedData data = HubSavedData.get(sp.getServer());
            data.addAdmin(sp.getUUID());
            // 不在此消耗：创建仓库成功时才消耗凭证
            sp.sendSystemMessage(Component.literal("已获得管理员权限，请给私人仓库起名（创建成功会消耗凭证）"));
            PacketDistributor.sendToPlayer(sp, new ModNetworking.OpenCreateDialog());
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
