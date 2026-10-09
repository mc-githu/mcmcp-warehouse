package com.commhub;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class TradeMenu extends AbstractContainerMenu {
    public static final int MODE_BOUNTY = 0;
    public static final int MODE_EXCHANGE = 1;
    public static final int TRADE_SLOTS = 10;

    private final int mode;

    public TradeMenu(int id, Inventory inv, int mode) {
        super(ModMenuTypes.TRADE_MENU.get(), id);
        this.mode = mode;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 64 + col * 18, 112 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 64 + col * 18, 170));
        }
    }

    public TradeMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readInt());
    }

    public int getMode() {
        return mode;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return result;
        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index < 27) {
            if (!this.moveItemStackTo(stack, 27, 36, false)) return ItemStack.EMPTY;
        } else {
            if (!this.moveItemStackTo(stack, 0, 27, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }
}
