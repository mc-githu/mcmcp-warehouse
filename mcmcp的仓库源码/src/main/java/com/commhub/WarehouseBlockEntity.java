package com.commhub;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.UUID;

public class WarehouseBlockEntity extends BlockEntity {
    private boolean output;
    private int target = 0; // 0 = shared, 1 = private
    private String warehouseName = "";
    private UUID owner;
    /** 谁放置的这个方块（防小偷：界面里显示出来） */
    private UUID placerUUID;
    private String placerName = "";

    public WarehouseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WAREHOUSE.get(), pos, state);
        this.output = state.getBlock() == ModBlocks.WAREHOUSE_OUTPUT.get();
    }

    public boolean isOutput() {
        return output;
    }

    public int getTarget() {
        return target;
    }

    public void setTarget(int target) {
        this.target = target;
        setChanged();
        if (level instanceof ServerLevel sl) {
            sl.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public String getWarehouseName() {
        return warehouseName == null ? "" : warehouseName;
    }

    public void setWarehouseName(String name) {
        this.warehouseName = name == null ? "" : name;
        setChanged();
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    private HubSavedData data() {
        if (level == null || level.isClientSide) return null;
        return HubSavedData.get(level.getServer());
    }

    private PagedItemHandler getTargetWarehouse() {
        if (level == null || level.isClientSide) return null;
        HubSavedData data = HubSavedData.get(level.getServer());
        if (target == 0) {
            // 共享大仓库被配置关掉时，共享目标直接失效
            return ModConfig.sharedWarehouseEnabled() ? data.getWarehouse() : null;
        }
        if (owner != null) {
            // 指定了名字就用指定的；否则退回第一个可访问的
            if (!getWarehouseName().isEmpty()) {
                HubSavedData.PrivateWarehouse w = data.getPrivateWarehouse(getWarehouseName());
                if (w != null && data.canAccessPrivateWarehouse(w, owner)) return w.storage;
            }
            java.util.List<String> list = data.accessiblePrivateWarehouses(owner);
            if (!list.isEmpty()) {
                HubSavedData.PrivateWarehouse w = data.getPrivateWarehouse(list.get(0));
                if (w != null) return w.storage;
            }
        }
        return null;
    }

    /** 记录放置者（放置方块时调用） */
    public void setPlacer(UUID uuid, String name) {
        this.placerUUID = uuid;
        this.placerName = (name == null) ? "" : name;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public UUID getPlacerUUID() {
        return placerUUID;
    }

    public String getPlacerName() {
        return placerName == null ? "" : placerName;
    }

    public IItemHandler getHandler() {
        return handler;
    }

    private final IItemHandler handler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            // 只有输出方块报告物品，输入方块不提供取出（避免漏斗被误导卡住）
            if (!output) return ItemStack.EMPTY;
            PagedItemHandler warehouse = getTargetWarehouse();
            if (warehouse == null) return ItemStack.EMPTY;
            for (int i = 0; i < warehouse.getSlots(); i++) {
                ItemStack s = warehouse.getStackInSlot(i);
                if (!s.isEmpty()) return s.copy();
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (output || stack.isEmpty()) return stack;
            PagedItemHandler warehouse = getTargetWarehouse();
            if (warehouse == null) return stack;
            // 分页仓库：满了自动加页（simulate 时不改结构）
            ItemStack result = simulate
                    ? ItemHandlerHelper.insertItemStacked(warehouse, stack.copy(), true)
                    : warehouse.insertStacked(stack.copy());
            if (!simulate) setChanged();
            return result;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!output) return ItemStack.EMPTY;
            PagedItemHandler warehouse = getTargetWarehouse();
            if (warehouse == null) return ItemStack.EMPTY;
            // find first non-empty slot and extract from it
            for (int i = 0; i < warehouse.getSlots(); i++) {
                ItemStack inSlot = warehouse.getStackInSlot(i);
                if (!inSlot.isEmpty()) {
                    ItemStack extracted = inSlot.copyWithCount(Math.min(amount, inSlot.getCount()));
                    if (!simulate) {
                        inSlot.shrink(extracted.getCount());
                        if (inSlot.isEmpty()) warehouse.setStackInSlot(i, ItemStack.EMPTY);
                        setChanged();
                    }
                    return extracted;
                }
            }
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }
    };

    /** 目标私人仓库（target=1 且指定名字）；共享时返回 null */
    private HubSavedData.PrivateWarehouse getTargetPrivateWarehouse() {
        if (target == 0) return null;
        HubSavedData d = data();
        if (d == null) return null;
        if (!getWarehouseName().isEmpty()) {
            HubSavedData.PrivateWarehouse w = d.getPrivateWarehouse(getWarehouseName());
            if (w != null && d.canAccessPrivateWarehouse(w, owner)) return w;
        }
        java.util.List<String> list = d.accessiblePrivateWarehouses(owner);
        return list.isEmpty() ? null : d.getPrivateWarehouse(list.get(0));
    }

    private int getTargetEnergy() {
        HubSavedData d = data();
        if (d == null) return 0;
        HubSavedData.PrivateWarehouse w = getTargetPrivateWarehouse();
        return w == null ? d.getSharedEnergy() : w.energy;
    }

    private void addTargetEnergy(int amount) {
        HubSavedData d = data();
        if (d == null) return;
        HubSavedData.PrivateWarehouse w = getTargetPrivateWarehouse();
        if (w == null) d.addSharedEnergy(amount);
        else {
            long v = (long) w.energy + amount;
            w.energy = (int) Math.max(0, Math.min(Integer.MAX_VALUE, v));
            d.setDirty();
        }
    }

    private void removeTargetEnergy(int amount) {
        HubSavedData d = data();
        if (d == null) return;
        HubSavedData.PrivateWarehouse w = getTargetPrivateWarehouse();
        if (w == null) d.removeSharedEnergy(amount);
        else {
            w.energy = Math.max(0, w.energy - amount);
            d.setDirty();
        }
    }

    private FluidStack getTargetFluid() {
        HubSavedData d = data();
        if (d == null) return FluidStack.EMPTY;
        HubSavedData.PrivateWarehouse w = getTargetPrivateWarehouse();
        return w == null ? d.getSharedFluid() : w.fluid;
    }

    private boolean addTargetFluid(FluidStack stack) {
        HubSavedData d = data();
        if (d == null) return false;
        HubSavedData.PrivateWarehouse w = getTargetPrivateWarehouse();
        if (w == null) return d.addSharedFluid(stack);
        if (w.fluid.isEmpty()) {
            w.fluid = stack.copy();
            d.setDirty();
            return true;
        }
        if (w.fluid.is(stack.getFluid())) {
            w.fluid.grow(stack.getAmount());
            d.setDirty();
            return true;
        }
        return false;
    }

    private void removeTargetFluid(int amount) {
        HubSavedData d = data();
        if (d == null) return;
        HubSavedData.PrivateWarehouse w = getTargetPrivateWarehouse();
        if (w == null) d.removeSharedFluid(amount);
        else {
            w.fluid.shrink(amount);
            if (w.fluid.getAmount() <= 0) w.fluid = FluidStack.EMPTY;
            d.setDirty();
        }
    }

    /** 液体接口：输入方块收（Create 管道推），输出方块放（管道抽） */
    public IFluidHandler getFluidHandler() {
        return fluidHandler;
    }

    /** 电能接口：输入方块收（电线灌），输出方块放（电缆抽） */
    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    private final IFluidHandler fluidHandler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return getTargetFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            FluidStack cur = getTargetFluid();
            return cur.isEmpty() || cur.is(stack.getFluid());
        }

        @Override
        public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
            if (output || resource == null || resource.isEmpty()) return 0;
            FluidStack cur = getTargetFluid();
            if (!cur.isEmpty() && !cur.is(resource.getFluid())) return 0;
            if (action.simulate()) return resource.getAmount();
            addTargetFluid(resource);
            setChanged();
            return resource.getAmount();
        }

        @Override
        public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
            if (!output || resource == null || resource.isEmpty()) return FluidStack.EMPTY;
            FluidStack cur = getTargetFluid();
            if (cur.isEmpty() || !cur.is(resource.getFluid())) return FluidStack.EMPTY;
            int amount = Math.min(resource.getAmount(), cur.getAmount());
            FluidStack taken = cur.copyWithAmount(amount);
            if (action.execute()) {
                removeTargetFluid(amount);
                setChanged();
            }
            return taken;
        }

        @Override
        public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
            if (!output) return FluidStack.EMPTY;
            FluidStack cur = getTargetFluid();
            if (cur.isEmpty()) return FluidStack.EMPTY;
            int amount = Math.min(maxDrain, cur.getAmount());
            FluidStack taken = cur.copyWithAmount(amount);
            if (action.execute()) {
                removeTargetFluid(amount);
                setChanged();
            }
            return taken;
        }
    };

    private final IEnergyStorage energyStorage = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (output || maxReceive <= 0) return 0;
            int accepted = Math.min(maxReceive, Integer.MAX_VALUE - getTargetEnergy());
            if (accepted <= 0) return 0;
            if (!simulate) {
                addTargetEnergy(accepted);
                setChanged();
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            if (!output || maxExtract <= 0) return 0;
            int extracted = Math.min(maxExtract, getTargetEnergy());
            if (extracted <= 0) return 0;
            if (!simulate) {
                removeTargetEnergy(extracted);
                setChanged();
            }
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return getTargetEnergy();
        }

        @Override
        public int getMaxEnergyStored() {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean canExtract() {
            return output;
        }

        @Override
        public boolean canReceive() {
            return !output;
        }
    };

    public void openTargetScreen(ServerPlayer player) {
        player.openMenu(new net.minecraft.world.MenuProvider() {
            @Override
            public net.minecraft.network.chat.Component getDisplayName() {
                return net.minecraft.network.chat.Component.literal("仓库装置");
            }

            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inv, net.minecraft.world.entity.player.Player p) {
                return new WarehouseTargetMenu(id, inv, getBlockPos(), getTarget(), getWarehouseName());
            }
        }, buf -> {
            buf.writeBlockPos(getBlockPos());
            buf.writeInt(getTarget());
            buf.writeUtf(getWarehouseName());
        });
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (placerUUID != null) tag.putUUID("placer", placerUUID);
        if (placerName != null && !placerName.isEmpty()) tag.putString("placerName", placerName);
        tag.putBoolean("output", output);
        tag.putInt("target", target);
        if (owner != null) tag.putUUID("owner", owner);
        tag.putString("wname", warehouseName);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        placerUUID = tag.hasUUID("placer") ? tag.getUUID("placer") : null;
        placerName = tag.getString("placerName");
        this.output = tag.getBoolean("output");
        this.target = tag.getInt("target");
        this.owner = tag.contains("owner") ? tag.getUUID("owner") : null;
        this.warehouseName = tag.getString("wname");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        tag.putInt("target", target);
        // 同步目标仓库名，保持客户端与服务端一致
        tag.putString("wname", warehouseName == null ? "" : warehouseName);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
