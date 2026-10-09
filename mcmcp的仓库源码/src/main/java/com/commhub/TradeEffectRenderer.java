package com.commhub;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 以物换物成交动画：**空间裂缝打开 → 锁链伸出 → 包着物品的箱子被拉进裂缝 → 锁链收回 → 裂缝关闭**。
 *
 * <p>以物换物是双向的，所以两边都会依次看到两段动画（传出去 / 收进来）。</p>
 *
 * <p>素材：铁链用**原版** {@code minecraft:textures/item/chain.png}；
 * 箱子优先用**机械动力**的纸箱贴图（装了才用），没装就用模组自带的；空间裂缝是本模组自己画的。</p>
 */
public final class TradeEffectRenderer {

    private static final long T_OPEN = 380;
    private static final long T_CHAIN = 780;
    private static final long T_MOVE = 1560;
    private static final long T_RETRACT = 1860;
    private static final long T_CLOSE = 2260;

    private static final ResourceLocation TEX_RIFT =
            ResourceLocation.fromNamespaceAndPath("commhub", "textures/gui/rift.png");
    private static final ResourceLocation TEX_CHAIN =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/chain.png");
    private static final ResourceLocation TEX_BOX_SELF =
            ResourceLocation.fromNamespaceAndPath("commhub", "textures/gui/rift_package.png");
    private static final ResourceLocation TEX_BOX_CREATE =
            ResourceLocation.fromNamespaceAndPath("create", "textures/block/cardboard_block_side.png");

    private record Fx(boolean outgoing, String partner, ItemStack item, int total, long start) {
    }

    private static final Deque<Fx> QUEUE = new ArrayDeque<>();
    private static Fx current;

    private TradeEffectRenderer() {
    }

    /** 服务端通知：来一段动画 */
    public static void trigger(boolean outgoing, String partner, ItemStack item, int total) {
        ItemStack icon = (item == null || item.isEmpty()) ? ItemStack.EMPTY : item.copyWithCount(1);
        QUEUE.addLast(new Fx(outgoing, partner == null ? "" : partner, icon, Math.max(1, total), now()));
    }

    private static long now() {
        return System.nanoTime() / 1_000_000L;
    }

    private static ResourceLocation boxTexture() {
        try {
            if (ModList.get().isLoaded("create")) return TEX_BOX_CREATE;
        } catch (Throwable ignored) {
        }
        return TEX_BOX_SELF;
    }

    /** 注册到 GUI 图层（最后画） */
    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) return;

        if (current == null) {
            current = QUEUE.pollFirst();
            if (current == null) return;
        }
        long t = now() - current.start();
        if (t >= T_CLOSE) {
            current = null;
            return;
        }

        int w = g.guiWidth();
        int h = g.guiHeight();

        // ---- 跟随玩家：转身 / 移动都会带着裂缝和锁链动 ----
        float swayX = 0f, swayY = 0f;
        try {
            float yaw = mc.player.getYRot();
            // 水平朝向 → 左右摆动（转身能明显看到）
            swayX = (float) (Math.sin(Math.toRadians(yaw)) * 46.0);
            var mv = mc.player.getDeltaMovement();
            swayX += (float) Math.max(-40, Math.min(40, mv.x * 260));
            swayY += (float) Math.max(-26, Math.min(26, mv.y * 200));
            swayX += (float) Math.max(-20, Math.min(20, -mv.z * 120 * Math.cos(Math.toRadians(yaw))));
        } catch (Throwable ignored) {
        }
        int cx = w / 2 + (int) swayX;
        int cy = h / 2 - 10 + (int) swayY;

        // ---- 裂缝开合 ----
        float open;
        if (t < T_OPEN) open = t / (float) T_OPEN;
        else if (t < T_RETRACT) open = 1f;
        else open = Math.max(0f, 1f - (t - T_RETRACT) / (float) (T_CLOSE - T_RETRACT));
        open = ease(open);
        int rw = (int) (64 * open);
        int rh = (int) (128 * open);
        if (rw > 2 && rh > 2) {
            int rx = cx - rw / 2, ry = cy - rh / 2;
            g.blit(TEX_RIFT, rx, ry, rw, rh, 0f, 0f, 64, 128, 64, 128);
            if (open > 0.45f) {
                drawRiftInner(g, cx, cy, rw / 2, rh / 2, t);
            }
        }

        // ---- 锁链伸出/收回 ----
        // 近端锁在「玩家身上」：屏幕正下方（跟着玩家的摆动一起动）
        int fromX = w / 2 + (int) (swayX * 1.6f);
        int fromY = h - 40 + (int) swayY;
        float chainT;
        if (t < T_OPEN) chainT = 0f;
        else if (t < T_CHAIN) chainT = (t - T_OPEN) / (float) (T_CHAIN - T_OPEN);
        else if (t < T_RETRACT) chainT = 1f;
        else chainT = Math.max(0f, 1f - (t - T_RETRACT) / (float) (T_CLOSE - T_RETRACT));
        chainT = ease(chainT);
        if (chainT > 0.02f) {
            int ex = (int) (cx + (fromX - cx) * chainT);
            int ey = (int) (cy + (fromY - cy) * chainT);
            drawChain(g, cx, cy, ex, ey);
        }

        // ---- 箱子移动 ----
        float p;
        if (t < T_CHAIN) p = current.outgoing() ? 0f : 1f;
        else if (t < T_MOVE) {
            p = ease((t - T_CHAIN) / (float) (T_MOVE - T_CHAIN));
            if (!current.outgoing()) p = 1f - p;
        } else p = current.outgoing() ? 1f : 0f;
        int px = (int) (fromX + (cx - fromX) * p);
        int py = (int) (fromY + (cy - fromY) * p);
        if (chainT > 0.05f) drawBox(g, px, py, current.item());

        // ---- 说明文字 ----
        String caption = current.outgoing()
                ? "空间裂缝：把箱子送给 " + current.partner()
                : "空间裂缝：" + current.partner() + " 的箱子送达";
        g.drawString(mc.font, caption, cx - mc.font.width(caption) / 2, cy + 74, 0xFFD0FF, true);
    }

    /**
     * 裂缝内部：**星辰** + 一条**看不到头的锁链**（越往里越小、越暗，像通向虚空）。
     */
    private static void drawRiftInner(GuiGraphics g, int cx, int cy, int halfW, int halfH, long t) {
        // ---- 星辰 ----
        for (int i = 0; i < 90; i++) {
            // 用 i 做伪随机，位置稳定；再随时间缓慢漂移
            int hx = (i * 7349 + 13) % 1000;
            int hy = (i * 4177 + 71) % 1000;
            float drift = ((t / 90f) + i * 3f) % 100f;
            float fx = (hx / 1000f) * 2f - 1f;
            float fy = ((hy / 1000f) * 2f - 1f) * 0.98f + (drift / 100f) * 0.02f;
            if (fx * fx + fy * fy > 0.92f) continue;
            int sx = cx + (int) (fx * halfW * 0.92f);
            int sy = cy + (int) (fy * halfH * 0.92f);
            int bright = 120 + (i % 5) * 26;
            int col = 0xFF000000 | (Math.min(255, bright) << 16) | (Math.min(230, bright + 20) << 8) | 255;
            int sz = (i % 7 == 0) ? 2 : 1;
            g.fill(sx, sy, sx + sz, sy + sz, col);
        }
        // ---- 看不到头的锁链：向裂缝深处收敛 ----
        for (int j = 0; j < 14; j++) {
            float k = j / 14f;
            int ly = cy - (int) (halfH * 0.72f) + (int) (k * halfH * 1.44f);
            int halfChain = Math.max(1, (int) (halfW * 0.42f * (1f - k) ));
            int alpha = (int) (200 * (1f - k * 0.85f));
            if (alpha <= 6) continue;
            int col = (alpha << 24) | 0x9AA0B4;
            boolean vertical = (j % 2) == 0;
            if (vertical) {
                g.fill(cx - halfChain, ly - 2, cx + halfChain, ly + 2, col);
            } else {
                g.fill(cx - Math.max(1, halfChain / 2), ly - 4, cx + Math.max(1, halfChain / 2), ly + 4, col);
            }
        }
    }

    /** 沿直线铺原版铁链贴图 */
    private static void drawChain(GuiGraphics g, int x1, int y1, int x2, int y2) {
        double dx = x2 - x1, dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 6) return;
        int links = (int) (len / 8);
        for (int i = 0; i <= links; i++) {
            double f = (double) i / Math.max(1, links);
            int lx = (int) (x1 + dx * f);
            int ly = (int) (y1 + dy * f);
            boolean vertical = (i % 2) == 0;
            if (vertical) {
                g.blit(TEX_CHAIN, lx - 5, ly - 7, 10, 14, 0f, 0f, 16, 16, 16, 16);
            } else {
                // 横过来：用旋转，不然拉宽了看还是竖的
                var pose = g.pose();
                pose.pushPose();
                pose.translate(lx, ly, 0);
                pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90f));
                g.blit(TEX_CHAIN, -7, -5, 14, 10, 0f, 0f, 16, 16, 16, 16);
                pose.popPose();
            }
        }
    }

    /** 箱子：纸箱贴图 + 里面物品的图标 */
    private static void drawBox(GuiGraphics g, int cx, int cy, ItemStack item) {
        int s = 30;
        int x = cx - s / 2, y = cy - s / 2;
        g.blit(boxTexture(), x, y, s, s, 0f, 0f, 16, 16, 16, 16);
        if (item != null && !item.isEmpty()) {
            var pose = g.pose();
            pose.pushPose();
            pose.translate(x + s / 2 - 12, y + s / 2 - 12, 250);
            pose.scale(1.5f, 1.5f, 1f);
            g.renderItem(item, 0, 0);
            pose.popPose();
        }
        // 描边，保证在任何背景上都看得清
        int b = 0xC0000000;
        g.fill(x, y, x + s, y + 1, b);
        g.fill(x, y + s - 1, x + s, y + s, b);
        g.fill(x, y, x + 1, y + s, b);
        g.fill(x + s - 1, y, x + s, y + s, b);
    }

    private static float ease(float f) {
        f = Math.max(0f, Math.min(1f, f));
        return f * f * (3 - 2 * f);
    }
}
