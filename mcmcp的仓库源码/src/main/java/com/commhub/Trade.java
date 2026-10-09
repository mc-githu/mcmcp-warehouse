package com.commhub;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 一条悬赏（以物换物）。
 *
 * <p>物品数量支持<b>无限堆叠</b>（和仓库/邮箱一样，可达 1 亿）。</p>
 * <p>注意：{@code ItemStack} 的存档 codec 上限是 <b>99</b>，所以数量单独存一个 {@code Count} 字段，
 * 读档时再用 {@code setCount} 还原 —— 否则大数量会在存档时被截断。</p>
 */
public record Trade(UUID id, UUID owner, String ownerName, List<ItemStack> offer, List<ItemStack> want) {

    public CompoundTag toNbt(HolderLookup.Provider provider) {
        CompoundTag t = new CompoundTag();
        t.putUUID("id", id);
        t.putUUID("owner", owner);
        t.putString("ownerName", ownerName);
        t.put("offer", writeStacks(offer, provider));
        t.put("want", writeStacks(want, provider));
        return t;
    }

    /** 每个物品存两段：item（数量压到 99 以内，因为 codec 上限 99）+ Count（真实数量） */
    private static ListTag writeStacks(List<ItemStack> list, HolderLookup.Provider provider) {
        ListTag tag = new ListTag();
        for (ItemStack s : list) {
            if (s == null || s.isEmpty()) continue;
            CompoundTag c = new CompoundTag();
            c.put("item", s.copyWithCount(Math.max(1, Math.min(99, s.getCount()))).save(provider));
            c.putInt("Count", s.getCount());
            tag.add(c);
        }
        return tag;
    }

    private static List<ItemStack> readStacks(ListTag tag, HolderLookup.Provider provider) {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < tag.size(); i++) {
            CompoundTag c = tag.getCompound(i);
            // 兼容旧存档：以前直接就是一个 ItemStack 的 NBT
            CompoundTag itemTag = c.contains("item") ? c.getCompound("item") : c;
            ItemStack st = ItemStack.parse(provider, itemTag).orElse(ItemStack.EMPTY);
            if (st.isEmpty()) continue;
            int n = c.contains("Count") ? c.getInt("Count") : st.getCount();
            if (n > 0) st.setCount(n);
            list.add(st);
        }
        return list;
    }

    /** 从 NBT 还原；数据损坏/缺字段时返回 null（调用方跳过，避免整个存档加载失败） */
    public static Trade fromNbt(CompoundTag t, HolderLookup.Provider provider) {
        try {
            if (!t.hasUUID("id") || !t.hasUUID("owner")) return null;
            return new Trade(
                    t.getUUID("id"),
                    t.getUUID("owner"),
                    t.getString("ownerName"),
                    readStacks(t.getList("offer", Tag.TAG_COMPOUND), provider),
                    readStacks(t.getList("want", Tag.TAG_COMPOUND), provider));
        } catch (Exception e) {
            return null;
        }
    }
}
