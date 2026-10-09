package com.commhub;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 合成升级卡的配置数据：5 个合成目标，每个目标 = 3x3 配方幽灵格 + 1 个产物。
 * 直接存在卡片物品的 CUSTOM_DATA 里，所以卡片拿在手上/放进仓库都带着配置。
 */
public final class CraftingCard {

    /** 一张卡最多几个合成目标 */
    public static final int TARGETS = 5;
    /** 3x3 配方格 */
    public static final int GRID = 9;
    /** 存档用的 NBT 键 */
    private static final String KEY = "commhub_card_targets";

    private CraftingCard() {
    }

    /** 一个合成目标 */
    public static final class Target {
        public final ItemStack[] grid = new ItemStack[GRID];
        public ItemStack result = ItemStack.EMPTY;

        public Target() {
            Arrays.fill(grid, ItemStack.EMPTY);
        }

        public boolean isEmpty() {
            if (!result.isEmpty()) return false;
            for (ItemStack s : grid) {
                if (!s.isEmpty()) return false;
            }
            return true;
        }

        /** 配方格里有东西吗（产物不算） */
        public boolean hasGrid() {
            for (ItemStack s : grid) {
                if (!s.isEmpty()) return true;
            }
            return false;
        }
    }

    /** 空的 5 个目标 */
    public static List<Target> emptyTargets() {
        List<Target> list = new ArrayList<>();
        for (int i = 0; i < TARGETS; i++) list.add(new Target());
        return list;
    }

    /** 读卡片配置；没有就返回 5 个空目标 */
    public static List<Target> read(ItemStack card, HolderLookup.Provider provider) {
        if (card == null || card.isEmpty()) return emptyTargets();
        CompoundTag tag = card.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return fromTag(tag, provider);
    }

    /** 从配置 NBT 读出 5 个目标（界面/数据包都用这个） */
    public static List<Target> fromTag(CompoundTag tag, HolderLookup.Provider provider) {
        List<Target> list = emptyTargets();
        if (tag == null || !tag.contains(KEY)) return list;
        ListTag targets = tag.getList(KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < TARGETS && i < targets.size(); i++) {
            CompoundTag t = targets.getCompound(i);
            Target target = list.get(i);
            ListTag gridTag = t.getList("Grid", Tag.TAG_COMPOUND);
            for (int g = 0; g < gridTag.size(); g++) {
                CompoundTag e = gridTag.getCompound(g);
                int slot = e.getInt("Slot");
                if (slot < 0 || slot >= GRID) continue;
                ItemStack st = ItemStack.parseOptional(provider, e.getCompound("Item"));
                if (st != null && !st.isEmpty()) target.grid[slot] = st.copyWithCount(1);
            }
            ItemStack res = ItemStack.parseOptional(provider, t.getCompound("Result"));
            if (res != null && !res.isEmpty()) target.result = res;
        }
        return list;
    }

    /** 写卡片配置 */
    public static void write(ItemStack card, List<Target> targets, HolderLookup.Provider provider) {
        if (card == null || card.isEmpty()) return;
        CompoundTag tag = card.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.put(KEY, targetsToList(targets, provider));
        card.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** 把 5 个目标转成 NBT（界面保存时用） */
    public static CompoundTag toTag(List<Target> targets, HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.put(KEY, targetsToList(targets, provider));
        return tag;
    }

    private static ListTag targetsToList(List<Target> targets, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (int i = 0; i < TARGETS; i++) {
            Target target = (i < targets.size()) ? targets.get(i) : new Target();
            CompoundTag t = new CompoundTag();
            ListTag gridTag = new ListTag();
            for (int g = 0; g < GRID; g++) {
                ItemStack st = target.grid[g];
                if (st == null || st.isEmpty()) continue;
                CompoundTag e = new CompoundTag();
                e.putInt("Slot", g);
                e.put("Item", st.copyWithCount(1).saveOptional(provider));
                gridTag.add(e);
            }
            t.put("Grid", gridTag);
            if (target.result != null && !target.result.isEmpty()) {
                t.put("Result", target.result.copy().saveOptional(provider));
            }
            list.add(t);
        }
        return list;
    }

    /** 多张卡的配置打包成一个 NBT：{Cards:[卡1配置, 卡2配置, ...]} */
    public static CompoundTag toCardsTag(List<List<Target>> cards, HolderLookup.Provider provider) {
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();
        for (List<Target> targets : cards) list.add(toTag(targets, provider));
        root.put("Cards", list);
        return root;
    }

    /** 从 {Cards:[...]} 读出每张卡的配置（固定返回 count 组） */
    public static List<List<Target>> fromCardsTag(CompoundTag root, HolderLookup.Provider provider, int count) {
        List<List<Target>> out = new ArrayList<>();
        ListTag list = (root == null) ? new ListTag() : root.getList("Cards", Tag.TAG_COMPOUND);
        for (int i = 0; i < count; i++) {
            if (i < list.size()) out.add(fromTag(list.getCompound(i), provider));
            else out.add(emptyTargets());
        }
        // 兼容老格式（整包只有一个 KEY）
        if (list.isEmpty() && root != null && root.contains(KEY)) out.set(0, fromTag(root, provider));
        return out;
    }

    /** 已配置了几个目标 */
    public static int configuredCount(ItemStack card, HolderLookup.Provider provider) {
        int n = 0;
        for (Target t : read(card, provider)) {
            if (t.hasGrid()) n++;
        }
        return n;
    }
}
