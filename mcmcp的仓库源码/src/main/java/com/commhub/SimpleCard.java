package com.commhub;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * 机械手卡片 / 动力锯卡片的配置（存在卡片自己的 NBT 里，拿在手上也带着配置）。
 *
 * <ul>
 *   <li>机械手卡片：mode（0=右键 / 1=左键）+ 槽0 = 用哪个物品</li>
 *   <li>动力锯卡片：槽0 = 输入物品，槽1 = 选定的产物</li>
 * </ul>
 */
public final class SimpleCard {

    public enum Kind { DEPLOYER, SAW }

    private static final String KEY = "commhub_simple_card";
    private static final String[] SLOTS = {"a", "b"};

    private SimpleCard() {
    }

    /** 是不是「仓库用的卡片」（合成卡 / 机械手卡 / 动力锯卡） */
    public static boolean isWarehouseCard(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.getItem() instanceof CraftingUpgradeCardItem
                || stack.getItem() instanceof SimpleCardItem;
    }

    public static Kind kindOf(ItemStack card) {
        return (card != null && card.getItem() instanceof SimpleCardItem it) ? it.kind() : null;
    }

    private static CompoundTag root(ItemStack card) {
        return card.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static CompoundTag cfg(ItemStack card) {
        CompoundTag t = root(card);
        return t.contains(KEY) ? t.getCompound(KEY) : new CompoundTag();
    }

    private static void save(ItemStack card, CompoundTag cfg) {
        CompoundTag t = root(card);
        t.put(KEY, cfg);
        card.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    /** 机械手：0=右键，1=左键 */
    public static int getMode(ItemStack card) {
        return Math.max(0, Math.min(1, cfg(card).getInt("mode")));
    }

    public static void setMode(ItemStack card, int mode) {
        CompoundTag c = cfg(card);
        c.putInt("mode", Math.max(0, Math.min(1, mode)));
        save(card, c);
    }

    public static ItemStack get(ItemStack card, int idx, HolderLookup.Provider provider) {
        if (idx < 0 || idx >= SLOTS.length) return ItemStack.EMPTY;
        CompoundTag c = cfg(card);
        if (!c.contains(SLOTS[idx])) return ItemStack.EMPTY;
        ItemStack st = ItemStack.parseOptional(provider, c.getCompound(SLOTS[idx]));
        return (st == null) ? ItemStack.EMPTY : st;
    }

    public static void set(ItemStack card, int idx, ItemStack stack, HolderLookup.Provider provider) {
        if (idx < 0 || idx >= SLOTS.length) return;
        CompoundTag c = cfg(card);
        if (stack == null || stack.isEmpty()) {
            c.remove(SLOTS[idx]);
        } else {
            c.put(SLOTS[idx], stack.copyWithCount(1).saveOptional(provider));
        }
        save(card, c);
    }

    /** 直接把配置 NBT 写进卡片（界面保存用） */
    public static void writeCfg(ItemStack card, CompoundTag cfg) {
        if (card == null || card.isEmpty()) return;
        save(card, cfg == null ? new CompoundTag() : cfg);
    }

    public static CompoundTag readCfg(ItemStack card) {
        return cfg(card);
    }
}
