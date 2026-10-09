package com.commhub;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 合成升级卡配置界面的容器。
 * 只装玩家背包 —— 这样**没装 JEI 也能从背包里拿物品**，再点进配方幽灵格。
 */
public class SimpleCardMenu extends AbstractContainerMenu {

    /** 背包在界面里的位置（相对 leftPos/topPos），要和 CraftingCardScreen 的布局一致 */
    public static final int INV_X = 69;
    public static final int INV_Y = 118;

    private final int hand;

    public SimpleCardMenu(int id, Inventory inv, int hand) {
        super(ModMenuTypes.SIMPLE_CARD_MENU.get(), id);
        this.hand = hand;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, INV_X + col * 18, INV_Y + 58));
        }
    }

    public SimpleCardMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        // buf 可能为 null（服务端打开菜单时没写额外数据），必须保护
        this(id, inv, buf == null ? 0 : buf.readInt());
    }

    /** 打开界面时手里拿的是哪只手（0 主手 / 1 副手） */
    public int getHand() {
        return hand;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    /** 配置界面不需要 shift-click 搬运 */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
