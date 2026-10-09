package com.commhub;

import com.commhub.network.ModNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * 以物换物界面。
 *
 * <p>布局（300x200）：左侧「发布悬赏 / 换物」、右上「我给 / 我要」两块、中间一行操作按钮、
 * 下方背包。所有区域都有彩色框，且**不与背包格重叠**。</p>
 */
public class TradeScreen extends AbstractContainerScreen<TradeMenu> {

    // ---- 布局常量（格子与框用同一套，改这里就够）----
    private static final int PANEL_W = 300;
    private static final int PANEL_H = 200;
    private static final int OFFER_X = 110;
    private static final int OFFER_Y = 22;
    private static final int WANT_X = 110;
    private static final int WANT_Y = 58;
    private static final int SLOT_STEP = 18;
    private static final int BTN_Y = 84;

    private int selectedIndex = 0;
    private static final List<ItemStack> ghostOffer = new ArrayList<>();
    private static final List<ItemStack> ghostWant = new ArrayList<>();

    static {
        for (int i = 0; i < TradeMenu.TRADE_SLOTS; i++) {
            ghostOffer.add(ItemStack.EMPTY);
            ghostWant.add(ItemStack.EMPTY);
        }
    }

    public TradeScreen(TradeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
        this.inventoryLabelY = -9999;
        this.titleLabelY = -9999;
    }

    /** JEI 幽灵格：设置「我给」某一格的物品 */
    public void setOfferGhost(int index, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        if (index >= 0 && index < ghostOffer.size()) ghostOffer.set(index, stack.copyWithCount(1));
    }

    /** JEI 幽灵格：设置「我要」某一格的物品 */
    public void setWantGhost(int index, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        if (index >= 0 && index < ghostWant.size()) ghostWant.set(index, stack.copyWithCount(1));
    }

    private int mode() {
        return this.menu.getMode();
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(Button.builder(Component.literal("悬赏模式"), b -> switchMode(TradeMenu.MODE_BOUNTY))
                .bounds(this.leftPos + 8, this.topPos + 24, 90, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("换物模式"), b -> switchMode(TradeMenu.MODE_EXCHANGE))
                .bounds(this.leftPos + 8, this.topPos + 48, 90, 20).build());

        if (mode() == TradeMenu.MODE_BOUNTY) {
            this.addRenderableWidget(Button.builder(Component.literal("确认发布"), b -> publish())
                    .bounds(this.leftPos + OFFER_X, this.topPos + BTN_Y, 90, 20).build());
        } else {
            this.addRenderableWidget(Button.builder(Component.literal("◀"), b -> select(-1))
                    .bounds(this.leftPos + OFFER_X, this.topPos + BTN_Y, 20, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal("▶"), b -> select(1))
                    .bounds(this.leftPos + OFFER_X + 26, this.topPos + BTN_Y, 20, 20).build());
            boolean mine = false;
            try {
                var t = selectedTrade();
                mine = t != null && net.minecraft.client.Minecraft.getInstance().player != null
                        && t.owner().equals(net.minecraft.client.Minecraft.getInstance().player.getUUID());
            } catch (Throwable ignored) {
            }
            boolean selfOk = ClientHubState.selfTrade;
            if (mine) {
                // 自己的悬赏：永远给「取消」；调试允许自交易时再给一个「接受」
                this.addRenderableWidget(Button.builder(Component.literal("§c取消悬赏"), b -> cancel())
                        .bounds(this.leftPos + OFFER_X + 52, this.topPos + BTN_Y, 70, 20).build());
                if (selfOk) {
                    this.addRenderableWidget(Button.builder(Component.literal("§e接受(调试)"), b -> accept())
                            .bounds(this.leftPos + OFFER_X + 126, this.topPos + BTN_Y, 66, 20).build());
                }
            } else {
                this.addRenderableWidget(Button.builder(Component.literal("接受"), b -> accept())
                        .bounds(this.leftPos + OFFER_X + 52, this.topPos + BTN_Y, 50, 20).build());
            }
        }
    }

    private void switchMode(int mode) {
        PacketDistributor.sendToServer(new ModNetworking.OpenTrade(mode));
    }

    private void select(int delta) {
        List<ModNetworking.TradeInfo> trades = ClientHubState.trades;
        if (trades.isEmpty()) return;
        this.selectedIndex = Math.floorMod(this.selectedIndex + delta, trades.size());
    }

    /** 取消自己发布的悬赏 */
    private void cancel() {
        ModNetworking.TradeInfo t = selectedTrade();
        if (t != null) PacketDistributor.sendToServer(new ModNetworking.CancelTrade(t.id()));
    }

    private void accept() {
        ModNetworking.TradeInfo t = selectedTrade();
        if (t != null) PacketDistributor.sendToServer(new ModNetworking.AcceptTrade(t.id()));
    }

    private void publish() {
        List<String> offer = new ArrayList<>();
        List<String> want = new ArrayList<>();
        for (ItemStack s : ghostOffer) if (!s.isEmpty()) offer.add(s.getCount() + "×" + BuiltInRegistries.ITEM.getKey(s.getItem()));
        for (ItemStack s : ghostWant) if (!s.isEmpty()) want.add(s.getCount() + "×" + BuiltInRegistries.ITEM.getKey(s.getItem()));
        PacketDistributor.sendToServer(new ModNetworking.CreateTrade(offer, want));
    }

    /** 发布成功后被服务器调用：清空幽灵格 */
    public static void clearGhosts() {
        for (int i = 0; i < ghostOffer.size(); i++) {
            ghostOffer.set(i, ItemStack.EMPTY);
            ghostWant.set(i, ItemStack.EMPTY);
        }
    }

    private ModNetworking.TradeInfo selectedTrade() {
        List<ModNetworking.TradeInfo> trades = ClientHubState.trades;
        if (trades == null || trades.isEmpty()) return null;
        if (selectedIndex < 0 || selectedIndex >= trades.size()) selectedIndex = 0;
        return trades.get(selectedIndex);
    }

    // ---- 幽灵格命中判定（和绘制用同一套坐标）----

    private boolean isOfferSlot(int x, int y) {
        return x >= OFFER_X && x < OFFER_X + TradeMenu.TRADE_SLOTS * SLOT_STEP
                && y >= OFFER_Y && y < OFFER_Y + 18;
    }

    private boolean isWantSlot(int x, int y) {
        return x >= WANT_X && x < WANT_X + TradeMenu.TRADE_SLOTS * SLOT_STEP
                && y >= WANT_Y && y < WANT_Y + 18;
    }

    private int slotIndex(int x, int base) {
        return Math.min(TradeMenu.TRADE_SLOTS - 1, Math.max(0, (x - base) / SLOT_STEP));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = (int) (mx - this.leftPos);
        int y = (int) (my - this.topPos);
        if (mode() == TradeMenu.MODE_BOUNTY) {
            if (isOfferSlot(x, y)) {
                setGhost(ghostOffer, slotIndex(x, OFFER_X));
                return true;
            }
            if (isWantSlot(x, y)) {
                setGhost(ghostWant, slotIndex(x, WANT_X));
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private void setGhost(List<ItemStack> list, int index) {
        ItemStack carried = this.menu.getCarried();
        // 保留手持数量（不再写死 1）；上限取「物品最大堆叠」和 64 的较小值
        if (carried.isEmpty()) {
            list.set(index, ItemStack.EMPTY);
        } else {
            // 和仓库一样支持无限堆叠
            int max = com.commhub.InfiniteItemStackHandler.INFINITE;
            list.set(index, carried.copyWithCount(Math.max(1, Math.min(max, carried.getCount()))));
        }
    }

    /** 滚轮调幽灵格数量（1 ~ 物品最大堆叠） */
    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (mode() == TradeMenu.MODE_BOUNTY) {
            int x = (int) (mx - this.leftPos);
            int y = (int) (my - this.topPos);
            List<ItemStack> list = null;
            int base = 0;
            if (isOfferSlot(x, y)) {
                list = ghostOffer;
                base = OFFER_X;
            } else if (isWantSlot(x, y)) {
                list = ghostWant;
                base = WANT_X;
            }
            if (list != null) {
                int i = slotIndex(x, base);
                ItemStack cur = list.get(i);
                if (!cur.isEmpty()) {
                    int max = com.commhub.InfiniteItemStackHandler.INFINITE;
                    int step = net.minecraft.client.gui.screens.Screen.hasShiftDown() ? 64 : 1;
                    int n = (int) Math.max(1, Math.min(max, cur.getCount() + sy * step));
                    list.set(i, cur.copyWithCount(n));
                }
                return true;
            }
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    // ---- 绘制 ----

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        g.fill(x, y, x + PANEL_W, y + PANEL_H, 0xC0101010);

        frame(g, x + 6, y + 20, x + 102, y + 74, 0x40FFD080);       // 左侧：模式切换（黄）
        frame(g, x + 106, y + 6, x + 294, y + 42, 0x4000FF88);      // 我给（绿）
        frame(g, x + 106, y + 42, x + 294, y + 78, 0x40FF8888);     // 我要（红）
        frame(g, x + 60, y + 108, x + 240, y + 198, 0x4080FF80);    // 背包（绿）
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        if (mode() == TradeMenu.MODE_BOUNTY) {
            g.drawString(this.font, "我给（左键放物品 / 滚轮调数量 / Shift×64 / 右键清空）",
                    this.leftPos + OFFER_X, this.topPos + 10, 0x00FF88);
            g.drawString(this.font, "我要", this.leftPos + WANT_X, this.topPos + 46, 0xFF8888);
            for (int i = 0; i < TradeMenu.TRADE_SLOTS; i++) {
                int ox = this.leftPos + OFFER_X + i * SLOT_STEP;
                int oy = this.topPos + OFFER_Y;
                int wx = this.leftPos + WANT_X + i * SLOT_STEP;
                int wy = this.topPos + WANT_Y;
                g.renderOutline(ox, oy, 16, 16, 0x60FFFFFF);
                g.renderOutline(wx, wy, 16, 16, 0x60FFFFFF);
                ItemStack o = ghostOffer.get(i);
                if (!o.isEmpty()) {
                    g.renderItem(o, ox, oy);
                    g.renderItemDecorations(this.font, o, ox, oy, compact(String.valueOf(o.getCount())));
                }
                ItemStack w = ghostWant.get(i);
                if (!w.isEmpty()) {
                    g.renderItem(w, wx, wy);
                    g.renderItemDecorations(this.font, w, wx, wy, compact(String.valueOf(w.getCount())));
                }
            }
        } else {
            List<ModNetworking.TradeInfo> trades = ClientHubState.trades;
            if (trades.isEmpty()) {
                g.drawString(this.font, "当前没有悬赏", this.leftPos + OFFER_X, this.topPos + 14, 0xAAAAAA);
            } else {
                ModNetworking.TradeInfo t = trades.get(Math.floorMod(selectedIndex, trades.size()));
                var me = net.minecraft.client.Minecraft.getInstance().player;
                boolean mine = me != null && t.owner().equals(me.getUUID());
                g.drawString(this.font, "第 " + (selectedIndex + 1) + " / " + trades.size() + " 个悬赏"
                                + (mine ? (ClientHubState.selfTrade ? "（我的 · 调试可自收）" : "（我的，可取消）") : ""),
                        this.leftPos + OFFER_X, this.topPos + 12, 0xFFFFFF);
                g.drawString(this.font, truncate(t.ownerName() + " 给: " + prettyItems(t.offer()), 30),
                        this.leftPos + OFFER_X, this.topPos + 28, 0x00FF88);
                g.drawString(this.font, truncate("要: " + prettyItems(t.want()), 30),
                        this.leftPos + OFFER_X, this.topPos + 46, 0xFF8888);
            }
        }
        // 背包标题在框上方，绝不压格子
        g.drawString(this.font, "背包", this.leftPos + 62, this.topPos + 96, 0xFFFFFF);
    }

    private void frame(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1, y1, x2, y2, color & 0x22FFFFFF);
        int border = (color & 0x00FFFFFF) | 0xD0000000;
        g.fill(x1, y1, x2, y1 + 1, border);
        g.fill(x1, y2 - 1, x2, y2, border);
        g.fill(x1, y1, x1 + 1, y2, border);
        g.fill(x2 - 1, y1, x2, y2, border);
    }

    /**
     * 把服务端存的「3×create:cogwheel」这种字符串转成玩家看得懂的「3×齿轮」。
     * 注意：内部逻辑仍然用 id（服务端要靠它认物品），这里只是**显示**时翻译一下。
     */
    public static String prettyItems(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String part : raw.split("\\s*\\+|,")) {
            String t = part.trim();
            if (t.isEmpty()) continue;
            if (sb.length() > 0) sb.append("、");
            int x = t.indexOf('×');
            if (x <= 0) { sb.append(t); continue; }
            String count = t.substring(0, x);
            String id = t.substring(x + 1);
            String name = id;
            try {
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .get(net.minecraft.resources.ResourceLocation.parse(id));
                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                    name = new net.minecraft.world.item.ItemStack(item).getHoverName().getString();
                }
            } catch (Exception ignored) {
            }
            sb.append(compact(count)).append("×").append(name);
        }
        return sb.toString();
    }

    /** 大数字压缩显示：12000 → 1.2万；100000000 → 1亿 */
    private static String compact(String num) {
        try {
            long n = Long.parseLong(num.trim());
            if (n >= 100_000_000L) return (n / 100_000_000L) + "亿";
            if (n >= 10_000L) return (n / 10_000L) + "万";
            return String.valueOf(n);
        } catch (Exception e) {
            return num;
        }
    }

    private static String truncate(String s, int max) {
        if (s.length() <= max) return s;
        return s.substring(0, max - 1) + "…";
    }
}
