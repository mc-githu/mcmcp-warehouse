package com.commhub;

import com.commhub.network.ModNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 合成升级卡配置界面（带玩家背包的容器界面）。
 *
 * 左边是**可滚动**的目标列表（每张卡 5 个目标，鼠标滚轮翻），
 * 右边是选中目标的编辑区：3x3 配方幽灵格 + 产物幽灵格。
 * 幽灵格不占物品：可以从**下面背包**里拿物品点进去（左键点格子 = 放上手里拿的），
 * 支持 JEI 的也可以直接从 JEI 拖。
 */
public class CraftingCardScreen extends AbstractContainerScreen<CraftingCardMenu> {

    private static final int PANEL_W = 348;
    private static final int PANEL_H = 262;

    public static final int SLOT = 18;
    public static final int GRID_X = 128;
    public static final int GRID_Y = 74;
    public static final int RESULT_X = GRID_X + 3 * SLOT + 22;
    public static final int RESULT_Y = GRID_Y + SLOT;

    private static final int LIST_X = 8;
    private static final int LIST_Y = 56;
    private static final int LIST_W = 92;
    private static final int LIST_H = 112;
    private static final int ROW_H = 14;

    private final List<List<CraftingCard.Target>> allTargets = new ArrayList<>();
    private int selected = 0;
    private int scroll = 0;
    private final java.util.Set<Integer> dirty = new java.util.HashSet<>();

    public CraftingCardScreen(CraftingCardMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
        this.inventoryLabelY = -9999;   // 不显示自带的 "Inventory" 文字
        this.titleLabelY = -9999;
    }

    private int cardCount() {
        return Math.max(1, allTargets.size());
    }

    private int totalRows() {
        return cardCount() * CraftingCard.TARGETS;
    }

    private int selectedCard() {
        return selected / CraftingCard.TARGETS;
    }

    private int selectedIndexInCard() {
        return selected % CraftingCard.TARGETS;
    }

    private CraftingCard.Target current() {
        List<CraftingCard.Target> list = allTargets.get(Math.min(selectedCard(), allTargets.size() - 1));
        while (list.size() < CraftingCard.TARGETS) list.add(new CraftingCard.Target());
        return list.get(selectedIndexInCard());
    }

    @Override
    protected void init() {
        super.init();
        // 打开界面时读手里那张卡的配置
        allTargets.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null) {
            ItemStack card = mc.player.getItemInHand(
                    this.menu.getHand() == 0 ? net.minecraft.world.InteractionHand.MAIN_HAND
                                              : net.minecraft.world.InteractionHand.OFF_HAND);
            HolderLookup.Provider provider = mc.level.registryAccess();
            if (card.getItem() instanceof CraftingUpgradeCardItem) {
                allTargets.add(CraftingCard.read(card, provider));
            }
        }
        if (allTargets.isEmpty()) allTargets.add(CraftingCard.emptyTargets());

        addRenderableWidget(Button.builder(Component.literal("清空这个目标"), b -> {
            dirty.add(selectedCard());
            CraftingCard.Target t = current();
            for (int i = 0; i < CraftingCard.GRID; i++) t.grid[i] = ItemStack.EMPTY;
            t.result = ItemStack.EMPTY;
        }).bounds(leftPos + GRID_X - 6, topPos + 26, 96, 18).build());
        addRenderableWidget(Button.builder(Component.literal("保存"), b -> saveAndClose())
                .bounds(leftPos + PANEL_W - 70, topPos + 26, 62, 18).build());
    }

    private void save() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            HolderLookup.Provider provider = mc.level.registryAccess();
            java.util.Set<Integer> send = dirty.isEmpty() ? java.util.Set.of(selectedCard())
                                                          : new java.util.HashSet<>(dirty);
            for (int ci : send) {
                if (ci < 0 || ci >= allTargets.size()) continue;
                net.minecraft.nbt.CompoundTag tag = CraftingCard.toTag(allTargets.get(ci), provider);
                PacketDistributor.sendToServer(new ModNetworking.SaveCardConfig(
                        0, "", this.menu.getHand(), ci, tag));
            }
        }
    }

    // ---- 幽灵格 ----

    public void setGhost(int index, ItemStack stack) {
        dirty.add(selectedCard());
        CraftingCard.Target t = current();
        ItemStack one = (stack == null) ? ItemStack.EMPTY : stack.copyWithCount(1);
        if (index >= 0 && index < 9) t.grid[index] = one;
        else if (index == 9) t.result = one;
    }

    public ItemStack getGhost(int index) {
        CraftingCard.Target t = current();
        if (index >= 0 && index < 9) return t.grid[index];
        if (index == 9) return t.result;
        return ItemStack.EMPTY;
    }

    private int ghostAt(double mx, double my) {
        double x = mx - leftPos;
        double y = my - topPos;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                double gx = GRID_X + col * SLOT;
                double gy = GRID_Y + row * SLOT;
                if (x >= gx && x < gx + 16 && y >= gy && y < gy + 16) return col + row * 3;
            }
        }
        if (x >= RESULT_X && x < RESULT_X + 16 && y >= RESULT_Y && y < RESULT_Y + 16) return 9;
        return -1;
    }

    private int rowAt(double mx, double my) {
        double x = mx - leftPos;
        double y = my - topPos;
        if (x < LIST_X || x >= LIST_X + LIST_W || y < LIST_Y || y >= LIST_Y + LIST_H) return -1;
        int row = (int) ((y - LIST_Y + scroll) / ROW_H);
        return (row >= 0 && row < totalRows()) ? row : -1;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int idx = ghostAt(mx, my);
        if (idx >= 0) {
            ItemStack carried = this.menu.getCarried();
            if (button == 1 || carried.isEmpty()) setGhost(idx, ItemStack.EMPTY);
            else setGhost(idx, carried);
            return true;
        }
        int row = rowAt(mx, my);
        if (row >= 0) {
            if (button == 1) {
                dirty.add(row / CraftingCard.TARGETS);
                CraftingCard.Target t = allTargets.get(row / CraftingCard.TARGETS).get(row % CraftingCard.TARGETS);
                for (int i = 0; i < CraftingCard.GRID; i++) t.grid[i] = ItemStack.EMPTY;
                t.result = ItemStack.EMPTY;
            } else {
                selected = row;
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        int content = totalRows() * ROW_H;
        int max = Math.max(0, content - LIST_H);
        scroll = (int) Math.max(0, Math.min(max, scroll - scrollY * ROW_H));
        return true;
    }

    // ---- 渲染 ----

    private List<ItemStack> gridList() {
        CraftingCard.Target t = current();
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack s = t.grid[i];
            items.add((s == null || s.isEmpty()) ? ItemStack.EMPTY : s.copyWithCount(1));
        }
        return items;
    }

    private Optional<RecipeHolder<CraftingRecipe>> lookupRecipe() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return Optional.empty();
        if (!current().hasGrid()) return Optional.empty();
        return mc.level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, gridList()), mc.level);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        // 整块面板
        g.fill(x, y, x + PANEL_W, y + PANEL_H, 0xF0101010);
        frame(g, x, y, x + PANEL_W, y + PANEL_H, 0x40808080);
        // 顶部按钮行（黄）
        frame(g, x + 6, y + 22, x + PANEL_W - 6, y + 50, 0x40FFD080);
        // 左侧目标列表（黄）
        frame(g, x + LIST_X, y + LIST_Y, x + LIST_X + LIST_W, y + LIST_Y + LIST_H, 0x40FFD080);
        // 配方区（青）
        frame(g, x + GRID_X - 6, y + GRID_Y - 16, x + GRID_X + 3 * SLOT + 6, y + GRID_Y + 3 * SLOT + 6, 0x4090E0FF);
        // 产物区（金）
        frame(g, x + RESULT_X - 6, y + GRID_Y - 16, x + RESULT_X + 44, y + RESULT_Y + 26, 0x40FFC040);
        // 背包（绿）
        frame(g, x + 88, y + 172, x + 262, y + PANEL_H - 4, 0x4080FF80);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int x = leftPos;
        int y = topPos;

        g.drawString(this.font, "合成升级卡 · 配置", x + 10, y + 8, 0xFFFFFF);
        g.drawString(this.font, "目标列表（滚轮上下翻）", x + LIST_X + 2, y + LIST_Y - 11, 0xA0A0A0);
        g.drawString(this.font, "配方（3×3）", x + GRID_X, y + GRID_Y - 12, 0xA0A0A0);
        g.drawString(this.font, "产物", x + RESULT_X, y + GRID_Y - 12, 0xA0A0A0);

        renderList(g, mouseX, mouseY);

        // 幽灵格
        for (int i = 0; i < 10; i++) {
            int sx;
            int sy;
            if (i < 9) {
                sx = x + GRID_X + (i % 3) * SLOT;
                sy = y + GRID_Y + (i / 3) * SLOT;
            } else {
                sx = x + RESULT_X;
                sy = y + RESULT_Y;
            }
            boolean hover = ghostAt(mouseX, mouseY) == i;
            g.fill(sx, sy, sx + 16, sy + 16, hover ? 0x60FFFFFF : 0x40000000);
            frame(g, sx, sy, sx + 16, sy + 16, 0x809A9A9A);
            ItemStack st = getGhost(i);
            if (st != null && !st.isEmpty()) g.renderItem(st, sx, sy);
        }

        // 配方状态
        int statusY = y + GRID_Y + 3 * SLOT + 16;
        Optional<RecipeHolder<CraftingRecipe>> recipe = lookupRecipe();
        if (!current().hasGrid()) {
            g.drawString(this.font, "还没设置配方：从下面背包拿物品点进格子（或 JEI 拖）", x + GRID_X - 6, statusY, 0x808080);
        } else if (recipe.isPresent()) {
            Minecraft mc = Minecraft.getInstance();
            ItemStack real = recipe.get().value().assemble(CraftingInput.of(3, 3, gridList()), mc.level.registryAccess());
            g.drawString(this.font, "配方有效 →", x + GRID_X - 6, statusY, 0x55FF55);
            if (!real.isEmpty()) {
                g.renderItem(real, x + GRID_X + 54, statusY - 4);
                g.drawString(this.font, "×" + real.getCount(), x + GRID_X + 74, statusY, 0xFFFFFF);
            }
        } else {
            g.drawString(this.font, "配方无效：合成表里没这个配方，不会自动合成", x + GRID_X - 6, statusY, 0xFF5555);
        }

        g.drawString(this.font, "左键格子=放手里拿的物品　右键=清空　滚轮=翻列表　JEI 可拖",
                x + 10, y + 158, 0x909090);
        g.drawString(this.font, "背包（从这里拿物品放进配方格）", x + 90, y + 176, 0xA0A0A0);
    }

    private void renderList(GuiGraphics g, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        for (int row = 0; row < totalRows(); row++) {
            int ry = y + LIST_Y + 1 + row * ROW_H - scroll;
            if (ry + ROW_H < y + LIST_Y || ry > y + LIST_Y + LIST_H) continue;
            boolean isSel = (row == selected);
            boolean hover = rowAt(mouseX, mouseY) == row;
            if (isSel) g.fill(x + LIST_X + 1, ry, x + LIST_X + LIST_W - 1, ry + ROW_H, 0x60FFD080);
            else if (hover) g.fill(x + LIST_X + 1, ry, x + LIST_X + LIST_W - 1, ry + ROW_H, 0x30FFFFFF);

            int ci = row / CraftingCard.TARGETS;
            int ti = row % CraftingCard.TARGETS;
            CraftingCard.Target t = allTargets.get(ci).get(ti);
            g.drawString(this.font, "卡" + (ci + 1) + "·目标" + (ti + 1), x + LIST_X + 4, ry + 3, isSel ? 0xFFFFFF : 0xC0C0C0);
            ItemStack icon = t.result;
            if (icon == null || icon.isEmpty()) {
                for (ItemStack s : t.grid) {
                    if (s != null && !s.isEmpty()) { icon = s; break; }
                }
            }
            if (icon != null && !icon.isEmpty()) g.renderItem(icon, x + LIST_X + LIST_W - 16, ry - 1);
        }
        int content = totalRows() * ROW_H;
        if (content > LIST_H) {
            int barH = Math.max(12, LIST_H * LIST_H / content);
            int barY = y + LIST_Y + (LIST_H - barH) * scroll / Math.max(1, content - LIST_H);
            g.fill(x + LIST_X + LIST_W - 3, barY, x + LIST_X + LIST_W - 1, barY + barH, 0xC0FFD080);
        }
    }

    /** 关闭界面时自动保存（防止忘记点保存） */
    @Override
    public void onClose() {
        save();
    }

    private void saveAndClose() {
        save();
        super.onClose();
    }

    private void frame(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1, y1, x2, y2, color & 0x40FFFFFF);
        int border = (color & 0x00FFFFFF) | 0xC0000000;
        g.fill(x1, y1, x2, y1 + 1, border);
        g.fill(x1, y2 - 1, x2, y2, border);
        g.fill(x1, y1, x1 + 1, y2, border);
        g.fill(x2 - 1, y1, x2, y2, border);
    }
}
