package com.commhub;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 仓库指南。依赖 Patchouli；未安装时这件物品不会被注册（见 ModItems）。
 */
public class GuideBookItem extends Item {
    public static final String BOOK_ID = "commhub_guide";

    public GuideBookItem(Properties properties) {
        super(properties);
    }

    /** 是否装了 Patchouli */
    public static boolean hasPatchouli() {
        return net.neoforged.fml.ModList.get().isLoaded("patchouli");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (!hasPatchouli()) {
                sp.sendSystemMessage(Component.literal("这本书需要安装 Patchouli 模组才能阅读"));
                return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
            }
            vazkii.patchouli.api.PatchouliAPI.get().openBookGUI(sp,
                    ResourceLocation.fromNamespaceAndPath(CommHub.MODID, BOOK_ID));
            sp.sendSystemMessage(Component.literal("已打开 mcmcp的仓库 使用指南"));
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
