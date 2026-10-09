package com.commhub;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * 邮箱专用处理器：单格可堆到「内存无限」。
 *
 * <p>关键点：MC 1.21 的 {@code ItemStack} 存档编码把 count 限制在 [1,99]
 * （{@code ExtraCodecs.intRange(1,99)}，已在字节码确认），超过 99 的堆叠
 * 用 {@code ItemStack.save()} 会直接抛异常崩服。所以这里<b>不用默认序列化</b>，
 * 而是自己存：基础堆叠裁到 99，真实数量写在单独字段里，读档时再还原。</p>
 *
 * <p>另外取出时会把数量裁到 99 —— 离开邮箱的堆叠会进玩家背包/掉落物/别的容器，
 * 那些地方还是会走标准存档，必须保证 ≤99。</p>
 */
public class InfiniteItemStackHandler extends ItemStackHandler {

    /** 单格上限：内存里等同无限 */
    public static final int INFINITE = 100_000_000;
    /** 单次取出上限（离开邮箱必须 ≤99 才能安全存档） */
    public static final int MAX_EXTRACT = 99;

    public InfiniteItemStackHandler(int size) {
        super(size);
    }

    @Override
    public int getSlotLimit(int slot) {
        return INFINITE;
    }

    @Override
    protected int getStackLimit(int slot, ItemStack stack) {
        return INFINITE;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;
        ItemStack cur = getStackInSlot(slot);
        if (cur.isEmpty()) return ItemStack.EMPTY;
        int take = Math.min(Math.min(amount, cur.getCount()), MAX_EXTRACT);
        ItemStack out = cur.copyWithCount(take);
        if (!simulate) {
            ItemStack rest = cur.copyWithCount(cur.getCount() - take);
            setStackInSlot(slot, rest.isEmpty() ? ItemStack.EMPTY : rest);
        }
        return out;
    }

    // ---- 自定义存档（支持 count > 99）----

    public CompoundTag serializeCustom(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Size", getSlots());
        ListTag list = new ListTag();
        for (int i = 0; i < getSlots(); i++) {
            ItemStack st = getStackInSlot(i);
            if (st.isEmpty()) continue;
            CompoundTag item = new CompoundTag();
            item.putInt("Slot", i);
            int real = st.getCount();
            item.put("Item", st.copyWithCount(Math.min(real, 99)).saveOptional(provider));
            if (real > 99) item.putInt("Count", real);
            list.add(item);
        }
        tag.put("Items", list);
        return tag;
    }

    public static InfiniteItemStackHandler deserializeCustom(HolderLookup.Provider provider, CompoundTag tag, int targetSize) {
        InfiniteItemStackHandler h = new InfiniteItemStackHandler(Math.max(1, targetSize));
        ListTag list = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag item = list.getCompound(i);
            int slot = item.getInt("Slot");
            if (slot < 0 || slot >= h.getSlots()) continue;
            ItemStack st = ItemStack.parseOptional(provider, item.getCompound("Item"));
            if (st == null || st.isEmpty()) continue;
            int real = item.contains("Count") ? item.getInt("Count") : st.getCount();
            st.setCount(Math.max(1, Math.min(real, INFINITE)));
            h.setStackInSlot(slot, st);
        }
        return h;
    }
}
