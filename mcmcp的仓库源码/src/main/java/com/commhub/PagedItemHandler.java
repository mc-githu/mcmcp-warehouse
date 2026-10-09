package com.commhub;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 仓库的分页存储：内部是「每页 54 格（6 行 x 9 列）」的数组列表。
 *
 * - 溢出自建新页：所有页都塞满时，插入会自动新建一页继续装，容量真正无限
 * - 空页不留：末尾整页空的会被丢掉，空仓库只占 1 页（存档很小）
 * - 单格 99：MC 1.21 的 ItemStack 存档校验是 intRange(1, 99)，
 *   单格超过 99 存不进存档（会抛异常崩服），所以上限取 99
 */
public class PagedItemHandler implements IItemHandlerModifiable {

    /** 一页 = 6 行 x 9 列 */
    public static final int PAGE_SIZE = 54;
    /** 单格上限：内存里等同无限（真实数量用自定义字段存，绕过 MC 存档的 99 限制） */
    public static final int SLOT_LIMIT = 100_000_000;
    /**
     * 单次取出的上限。仓库内部可以堆上亿，但<b>离开仓库的每一个 ItemStack 必须 ≤99</b>，
     * 否则玩家背包/掉落物/其它容器在存档时会因为 count>99 直接崩服。
     */
    public static final int MAX_EXTRACT = 99;

    private final List<ItemStack[]> pages = new ArrayList<>();
    private Runnable changeListener = () -> {};

    public PagedItemHandler() {
        this(1);
    }

    public PagedItemHandler(int initialPages) {
        int n = Math.max(1, initialPages);
        for (int i = 0; i < n; i++) pages.add(blank());
    }

    private static ItemStack[] blank() {
        ItemStack[] page = new ItemStack[PAGE_SIZE];
        Arrays.fill(page, ItemStack.EMPTY);
        return page;
    }

    /** 内容变化时的回调（用来标脏存档） */
    public void setChangeListener(Runnable listener) {
        this.changeListener = (listener == null) ? () -> {} : listener;
    }

    private void changed() {
        changeListener.run();
    }

    // ---- 分页 ----

    /** 当前有多少页 */
    public int getPageCount() {
        return pages.size();
    }

    /** 新建一页（溢出了就调用） */
    public void addPage() {
        pages.add(blank());
        changed();
    }

    /** 保证至少有 count 页 */
    public void ensurePages(int count) {
        boolean grew = false;
        while (pages.size() < Math.max(1, count)) {
            pages.add(blank());
            grew = true;
        }
        if (grew) changed();
    }

    /** 丢掉末尾整页空的页（至少保留 1 页），返回丢弃的页数 */
    public int trimEmptyTail() {
        int removed = 0;
        while (pages.size() > 1 && isEmptyPage(pages.get(pages.size() - 1))) {
            pages.remove(pages.size() - 1);
            removed++;
        }
        if (removed > 0) changed();
        return removed;
    }

    private static boolean isEmptyPage(ItemStack[] page) {
        for (ItemStack s : page) {
            if (s != null && !s.isEmpty()) return false;
        }
        return true;
    }

    /** 仓库里非空格子数 */
    public int countUsed() {
        int n = 0;
        for (ItemStack[] page : pages) {
            for (ItemStack s : page) {
                if (s != null && !s.isEmpty()) n++;
            }
        }
        return n;
    }

    // ---- IItemHandler ----

    @Override
    public int getSlots() {
        return pages.size() * PAGE_SIZE;
    }

    private boolean valid(int slot) {
        return slot >= 0 && slot < getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (!valid(slot)) return ItemStack.EMPTY;
        ItemStack st = pages.get(slot / PAGE_SIZE)[slot % PAGE_SIZE];
        return st == null ? ItemStack.EMPTY : st;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (slot < 0) return;
        ensurePages(slot / PAGE_SIZE + 1);
        pages.get(slot / PAGE_SIZE)[slot % PAGE_SIZE] =
                (stack == null || stack.isEmpty()) ? ItemStack.EMPTY : stack;
        changed();
    }

    @Override
    public int getSlotLimit(int slot) {
        return SLOT_LIMIT;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack == null || stack.isEmpty() || !valid(slot)) return stack;
        ItemStack cur = getStackInSlot(slot);
        if (!cur.isEmpty() && !ItemStack.isSameItemSameComponents(cur, stack)) return stack;
        int room = SLOT_LIMIT - cur.getCount();
        if (room <= 0) return stack;
        int put = Math.min(room, stack.getCount());
        if (!simulate) {
            if (cur.isEmpty()) {
                pages.get(slot / PAGE_SIZE)[slot % PAGE_SIZE] = stack.copyWithCount(put);
            } else {
                cur.grow(put);
            }
            changed();
        }
        return (put >= stack.getCount()) ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - put);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0 || !valid(slot)) return ItemStack.EMPTY;
        ItemStack cur = getStackInSlot(slot);
        if (cur.isEmpty()) return ItemStack.EMPTY;
        // 离开仓库的堆叠一律裁到 99（保证任何目的地都能正常存档）
        int take = Math.min(Math.min(amount, cur.getCount()), MAX_EXTRACT);
        ItemStack out = cur.copyWithCount(take);
        if (!simulate) {
            ItemStack rest = cur.copyWithCount(cur.getCount() - take);
            pages.get(slot / PAGE_SIZE)[slot % PAGE_SIZE] = rest.isEmpty() ? ItemStack.EMPTY : rest;
            changed();
        }
        return out;
    }

    /**
     * 堆叠式插入，装满所有页就自动新建一页继续塞。
     * 返回装不下的剩余（正常情况下是空的）。
     */
    public ItemStack insertStacked(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack rest = ItemHandlerHelper.insertItemStacked(this, stack.copy(), false);
        int guard = 0;
        while (!rest.isEmpty() && guard++ < 4096) {
            addPage();
            int before = rest.getCount();
            rest = ItemHandlerHelper.insertItemStacked(this, rest, false);
            if (rest.getCount() >= before) break; // 没有进展，防止死循环
        }
        return rest;
    }

    // ---- 存档 ----

    /** 新格式：{Pages: [[{Slot,Item}...], ...]}，空的页不写进去 */
    public CompoundTag serialize(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("PageSize", PAGE_SIZE);
        ListTag pagesTag = new ListTag();
        for (ItemStack[] page : pages) {
            ListTag pageTag = new ListTag();
            for (int i = 0; i < PAGE_SIZE; i++) {
                ItemStack st = page[i];
                if (st == null || st.isEmpty()) continue;
                CompoundTag item = new CompoundTag();
                item.putInt("Slot", i);
                int real = st.getCount();
                // 基础堆叠裁到 99（MC 存档校验上限），真实数量另存一个字段
                item.put("Item", st.copyWithCount(Math.min(real, 99)).saveOptional(provider));
                if (real > 99) item.putInt("Count", real);
                pageTag.add(item);
            }
            pagesTag.add(pageTag);
        }
        tag.put("Pages", pagesTag);
        return tag;
    }

    public static PagedItemHandler deserialize(HolderLookup.Provider provider, CompoundTag tag) {
        PagedItemHandler h = new PagedItemHandler(1);
        ListTag pagesTag = tag.getList("Pages", Tag.TAG_LIST);
        if (!pagesTag.isEmpty()) {
            h.pages.clear();
            for (int p = 0; p < pagesTag.size(); p++) {
                ItemStack[] page = blank();
                ListTag pageTag = pagesTag.getList(p);
                for (int i = 0; i < pageTag.size(); i++) {
                    CompoundTag item = pageTag.getCompound(i);
                    int slot = item.getInt("Slot");
                    if (slot < 0 || slot >= PAGE_SIZE) continue;
                    ItemStack st = ItemStack.parseOptional(provider, item.getCompound("Item"));
                    if (st != null && !st.isEmpty()) {
                        // 真实数量优先取自 Count 字段（没有就是老存档，按 99 以内处理）
                        int real = item.contains("Count") ? item.getInt("Count") : st.getCount();
                        st.setCount(Math.max(1, Math.min(real, SLOT_LIMIT)));
                        page[slot] = st;
                    }
                }
                h.pages.add(page);
            }
            if (h.pages.isEmpty()) h.pages.add(blank());
        }
        h.trimEmptyTail();
        return h;
    }

    /** 兼容旧存档：ItemStackHandler 的扁平格式 {Size:int, Items:[{Slot,...}]} */
    public static PagedItemHandler deserializeLegacy(HolderLookup.Provider provider, CompoundTag tag) {
        PagedItemHandler h = new PagedItemHandler(1);
        ListTag items = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag item = items.getCompound(i);
            int slot = item.getInt("Slot");
            if (slot < 0) continue;
            h.ensurePages(slot / PAGE_SIZE + 1);
            ItemStack st = ItemStack.parseOptional(provider, item);
            if (st == null || st.isEmpty()) continue;
            if (slot >= h.getSlots()) continue;
            h.setStackInSlot(slot, st);
        }
        h.trimEmptyTail();
        return h;
    }
}
