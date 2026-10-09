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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;

/**
 * 机械手卡片 / 动力锯卡片的配置界面（带玩家背包，没装 JEI 也能从背包拿物品点进格子）。
 *
 * <ul>
 *   <li>机械手：手上物品 + 输入物品（<b>只做右键</b>：把手上物品按到输入物品上）</li>
 *   <li>动力锯：输入物品 + 选定产物（切割 / 去皮）</li>
 * </ul>
 */
public class SimpleCardScreen extends AbstractContainerScreen<SimpleCardMenu> {

    private static final int PANEL_W = 300;
    private static final int PANEL_H = 200;
    private static final int SLOT = 20;

    /** 两个幽灵格的位置（框 = 外扩 4px） */
    public static final int A_X = 96;
    public static final int A_Y = 58;
    public static final int B_X = 168;
    public static final int B_Y = 58;

    private static final int LABEL_Y = 42;   // 格子标题
    private static final int INFO_Y = 88;    // 说明第 1 行
    private static final int INFO2_Y = 98;   // 说明第 2 行
    private static final int INV_FRAME_X1 = 64;
    private static final int INV_FRAME_Y1 = 112;
    private static final int INV_FRAME_X2 = 236;
    private static final int INV_FRAME_Y2 = 196;

    private SimpleCard.Kind kind = SimpleCard.Kind.DEPLOYER;
    private ItemStack slotA = ItemStack.EMPTY;
    private ItemStack slotB = ItemStack.EMPTY;

    public SimpleCardScreen(SimpleCardMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
        this.inventoryLabelY = -9999;
        this.titleLabelY = -9999;
    }

    private ItemStack heldCard() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return ItemStack.EMPTY;
        return mc.player.getItemInHand(this.menu.getHand() == 0
                ? net.minecraft.world.InteractionHand.MAIN_HAND
                : net.minecraft.world.InteractionHand.OFF_HAND);
    }

    @Override
    protected void init() {
        super.init();
        ItemStack card = heldCard();
        SimpleCard.Kind k = SimpleCard.kindOf(card);
        if (k != null) this.kind = k;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            HolderLookup.Provider p = mc.level.registryAccess();
            this.slotA = SimpleCard.get(card, 0, p);
            this.slotB = SimpleCard.get(card, 1, p);
        }
        // 保存按钮（放在背包框右下角外侧，绝不压住任何框）
        addRenderableWidget(Button.builder(Component.literal("保存"), b -> {
            save();
            super.onClose();
        }).bounds(leftPos + PANEL_W - 62, topPos + PANEL_H - 18, 56, 16).build());
    }

    private void save() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.getConnection() == null) return;
        HolderLookup.Provider p = mc.level.registryAccess();
        net.minecraft.nbt.CompoundTag cfg = new net.minecraft.nbt.CompoundTag();
        if (!slotA.isEmpty()) cfg.put("a", slotA.copyWithCount(1).saveOptional(p));
        if (!slotB.isEmpty()) cfg.put("b", slotB.copyWithCount(1).saveOptional(p));
        PacketDistributor.sendToServer(new ModNetworking.SaveSimpleCard(this.menu.getHand(), cfg));
    }

    /** 关闭界面时自动保存（防止忘记点保存导致没生效） */
    @Override
    public void onClose() {
        save();
        super.onClose();
    }

    // ---- 幽灵格 ----

    public void setGhost(int idx, ItemStack st) {
        ItemStack one = (st == null) ? ItemStack.EMPTY : st.copyWithCount(1);
        if (idx == 0) slotA = one; else slotB = one;
    }

    public ItemStack getGhost(int idx) {
        return idx == 0 ? slotA : slotB;
    }

    private int ghostAt(double mx, double my) {
        double x = mx - leftPos;
        double y = my - topPos;
        if (x >= A_X && x < A_X + SLOT - 2 && y >= A_Y && y < A_Y + SLOT - 2) return 0;
        if (x >= B_X && x < B_X + SLOT - 2 && y >= B_Y && y < B_Y + SLOT - 2) return 1;
        return -1;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int i = ghostAt(mx, my);
        if (i >= 0) {
            ItemStack carried = this.menu.getCarried();
            if (button == 1 || carried.isEmpty()) setGhost(i, ItemStack.EMPTY);
            else setGhost(i, carried);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    // ---- 渲染 ----

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        g.fill(x, y, x + PANEL_W, y + PANEL_H, 0xF0101010);
        frame(g, x, y, x + PANEL_W, y + PANEL_H, 0x40808080);

        // 标题下面一条细分隔线（不占地方、不和别的框重叠）
        g.fill(x + 8, y + 34, x + PANEL_W - 8, y + 35, 0x60FFFFFF);

        // 两个格子
        frame(g, x + A_X - 4, y + A_Y - 4, x + A_X + SLOT - 2 + 4, y + A_Y + SLOT - 2 + 4, 0x4090E0FF);
        frame(g, x + B_X - 4, y + B_Y - 4, x + B_X + SLOT - 2 + 4, y + B_Y + SLOT - 2 + 4, 0x40FFC040);

        // 背包
        frame(g, x + INV_FRAME_X1, y + INV_FRAME_Y1, x + INV_FRAME_X2, y + INV_FRAME_Y2, 0x4080FF80);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int x = leftPos;
        int y = topPos;
        boolean deployer = (kind == SimpleCard.Kind.DEPLOYER);

        g.drawString(this.font, deployer ? "机械手卡片 · 配置" : "动力锯卡片 · 配置", x + 10, y + 9, 0xFFFFFF);
        g.drawString(this.font, "左键格子=放手持物品，右键格子=清空；下面就是背包",
                x + 10, y + 24, 0x909090);

        g.drawString(this.font, deployer ? "手上物品" : "输入物品", x + A_X - 2, y + LABEL_Y, 0xA0A0A0);
        g.drawString(this.font, deployer ? "输入物品" : "选定产物", x + B_X - 2, y + LABEL_Y, 0xA0A0A0);
        g.drawString(this.font, "→", x + 138, y + 62, 0xFFFFFF);

        drawGhost(g, A_X, A_Y, slotA, ghostAt(mouseX, mouseY) == 0);
        drawGhost(g, B_X, B_Y, slotB, ghostAt(mouseX, mouseY) == 1);

        if (deployer) {
            g.drawString(this.font, "只做右键：把「手上物品」按到「输入物品」上", x + 12, y + INFO_Y, 0x55FF55);
            g.drawString(this.font, "例：安山合金 + 去皮原木 → 安山机壳", x + 12, y + INFO2_Y, 0x909090);
        } else {
            g.drawString(this.font, "输入物品 + 选定产物（切割 / 去皮）", x + 12, y + INFO_Y, 0x55FF55);
            Optional<RecipeHolder<StonecutterRecipe>> r = lookupSaw();
            if (slotA.isEmpty() || slotB.isEmpty()) {
                g.drawString(this.font, "两个格子都要放：左边放要切的东西，右边放想要的结果", x + 12, y + INFO2_Y, 0x808080);
            } else if (r.isPresent()) {
                g.drawString(this.font, "原版切割配方有效 ✓", x + 12, y + INFO2_Y, 0x55FF55);
            } else {
                g.drawString(this.font, "原版切割里没这条；装了机械动力时按它的切割配方跑", x + 12, y + INFO2_Y, 0xFFAA55);
            }
        }
    }

    private void drawGhost(GuiGraphics g, int gx, int gy, ItemStack st, boolean hover) {
        int x = leftPos + gx;
        int y = topPos + gy;
        g.fill(x, y, x + SLOT - 2, y + SLOT - 2, hover ? 0x60FFFFFF : 0x40000000);
        if (st != null && !st.isEmpty()) g.renderItem(st, x + 1, y + 1);
    }

    /** 找「输入 → 产物」的原版切割配方（界面里给个提示用） */
    private Optional<RecipeHolder<StonecutterRecipe>> lookupSaw() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || slotA.isEmpty()) return Optional.empty();
        var input = new SingleRecipeInput(slotA.copyWithCount(1));
        for (RecipeHolder<StonecutterRecipe> holder : mc.level.getRecipeManager().getAllRecipesFor(RecipeType.STONECUTTING)) {
            if (!holder.value().matches(input, mc.level)) continue;
            ItemStack out = holder.value().getResultItem(mc.level.registryAccess());
            if (!out.isEmpty() && ItemStack.isSameItemSameComponents(out, slotB)) return Optional.of(holder);
        }
        return Optional.empty();
    }

    private void frame(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1, y1, x2, y2, color & 0x22FFFFFF);          // 底色只用 13% 不透明
        int border = (color & 0x00FFFFFF) | 0xD0000000;
        g.fill(x1, y1, x2, y1 + 1, border);
        g.fill(x1, y2 - 1, x2, y2, border);
        g.fill(x1, y1, x1 + 1, y2, border);
        g.fill(x2 - 1, y1, x2, y2, border);
    }
}
