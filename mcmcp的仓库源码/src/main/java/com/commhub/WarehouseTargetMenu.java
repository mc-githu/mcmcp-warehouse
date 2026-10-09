package com.commhub;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class WarehouseTargetMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final int target;
    private final String name;

    // server constructor
    public WarehouseTargetMenu(int id, Inventory inv, BlockPos pos, int target, String name) {
        super(ModMenuTypes.WAREHOUSE_TARGET.get(), id);
        this.pos = pos;
        this.target = target;
        this.name = name == null ? "" : name;
    }

    // client constructor
    public WarehouseTargetMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos(), buf.readInt(), buf.readUtf());
    }

    public BlockPos getPos() {
        return pos;
    }

    public int getTarget() {
        return target;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
