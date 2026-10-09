package com.commhub;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** 空间裂缝的 3D 渲染：裂缝本体 + 从玩家身上拉出来的锁链 + 箱子 */
public class RiftRenderer extends EntityRenderer<RiftEntity> {

    private static final ResourceLocation TEX_RIFT =
            ResourceLocation.fromNamespaceAndPath("commhub", "textures/gui/rift.png");
    // 用**方块**的链子贴图：它是竖着的链环，而且上下能无缝接起来（物品图标那个是横的）
    private static final ResourceLocation TEX_CHAIN =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/chain.png");
    private static final ResourceLocation TEX_BOX =
            ResourceLocation.fromNamespaceAndPath("commhub", "textures/gui/rift_package.png");

    public RiftRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(RiftEntity entity) {
        return TEX_RIFT;
    }

    @Override
    public void render(RiftEntity e, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight) {
        // 链节：只画一节朝向摄像机的链环（它有独立碰撞箱）
        if (e.isChainNode()) {
            pose.pushPose();
            pose.translate(-0.5, 0.5, -0.5);
            // 链节也横着：按「本链节 → 最近玩家」的水平方向选轴
            double lx2 = 1, lz2 = 0;
            var pl = e.level().getNearestPlayer(e, 32.0);
            if (pl != null) {
                lx2 = pl.getX() - e.getX();
                lz2 = pl.getZ() - e.getZ();
            }
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                    chainStateHorizontal(lx2, lz2),
                    pose, buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            pose.popPose();
            return;
        }
        float p = e.progress(partialTick);
        // 开合：先张开，常驻时保持全开
        float open = p < 0.5f ? p * 2f : 1f;
        if (open <= 0.02f) return;

        // ================= 裂缝：**固定朝向的单面平面** =================
        // 正面（朝向生成时玩家的方向）能看到星空；走到背面就什么都看不到（被剔除）
        float rw = 1.1f * open, rh = 1.6f * open;
        // 一扇**平面**的门（瑞克和莫蒂那种绿色传送门）
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-e.getYRot()));
        quadCulled(pose, buffer, TEX_RIFT, -rw, 0f, rw, rh * 2f, 0xFFFFFFFF);
        // 星空：裂缝内部的星点
        if (open > 0.5f) {
            for (int i = 0; i < 70; i++) {
                float fx = ((i * 7349 + 13) % 1000) / 1000f * 2f - 1f;
                float fy = ((i * 4177 + 71) % 1000) / 1000f * 2f - 1f;
                if (fx * fx + fy * fy > 0.85f) continue;
                float sz = (i % 7 == 0) ? 0.045f : 0.022f;
                int bright = 150 + (i % 6) * 17;
                int col = 0xFF000000 | (Math.min(255, bright) << 16) | (Math.min(235, bright + 25) << 8) | 255;
                quadCulled(pose, buffer, TEX_WHITE,
                        fx * rw - sz, rh + fy * rh * 0.96f - sz,
                        fx * rw + sz, rh + fy * rh * 0.96f + sz, col);
            }
        }
        pose.popPose();

        // ================= 锁链 + 包裹：每节都在**自己的世界坐标**上、朝向摄像机 =================
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            // 裂缝中心（裂缝本体是 0 ~ 3.2 高，中心在 1.6 处），锁链从中心出来
            Vec3 from = e.position().add(0.0, 1.6 * open, 0.0);
            Vec3 to = player.getPosition(partialTick).add(0.0, player.getBbHeight() * 0.55, 0.0);
            Vec3 dir = to.subtract(from);
            double len = dir.length();
            int n = Math.max(2, (int) (len / 0.22));

            // 动画：链节沿锁链"流动"，方向跟着包裹走
            //   收进来（!outgoing）：门 → 我；送出去（outgoing）：我 → 门
            double flow = (e.tickCount + partialTick) * 0.10 * (e.isOutgoing() ? -1.0 : 1.0);

            for (int i = 0; i <= n; i++) {
                double f = (((i + flow) % n) + n) % n / (double) n;
                f *= open;
                Vec3 wp = from.add(dir.scale(f));
                pose.pushPose();
                pose.translate(wp.x - e.getX(), wp.y - e.getY(), wp.z - e.getZ());
                // 先让平面朝向摄像机（广告牌），再在屏幕平面内旋转，
                // 让链节的长边对齐锁链的实际走向 —— 不转的话每一节都是竖直的
                org.joml.Quaternionf camRot = this.entityRenderDispatcher.cameraOrientation();
                pose.mulPose(camRot);
                org.joml.Vector3f rightV = camRot.transform(new org.joml.Vector3f(1f, 0f, 0f));
                org.joml.Vector3f upV = camRot.transform(new org.joml.Vector3f(0f, 1f, 0f));
                double projX = dir.x * rightV.x + dir.y * rightV.y + dir.z * rightV.z;
                double projY = dir.x * upV.x + dir.y * upV.y + dir.z * upV.z;
                float tilt = (float) Math.toDegrees(Math.atan2(-projX, projY));
                pose.mulPose(Axis.ZP.rotationDegrees(tilt));
                quad(pose, buffer, TEX_CHAIN, -0.075f, -0.185f, 0.075f, 0.185f, 0xFFFFFFFF);
                pose.popPose();
            }

            // 包裹：挂在锁链上（出去：我 → 裂缝；进来：裂缝 → 我）
            float move = Math.max(0f, Math.min(1f, (p - 0.30f) / 0.55f));
            // 常驻（测试）时做**乒乓**：门里出来 → 到我身上 → 从我身上出来 → 回门里
            if (e.isPersistentRift()) {
                float cyc = ((e.tickCount + partialTick) % 140f) / 140f;   // 7 秒一轮
                move = cyc < 0.5f ? (cyc / 0.5f) : (1f - (cyc - 0.5f) / 0.5f);
                move = move * move * (3f - 2f * move);                     // 平滑
            }
            // 进来：0（门中心）→ 0.85（身前 1.2 格）；出去：反过来
            double pk = (e.isOutgoing() ? (1.0 - move) : (double) move) * open * 0.85;
            Vec3 pp = from.add(dir.scale(pk));
            pose.pushPose();
            pose.translate(pp.x - e.getX(), pp.y - e.getY(), pp.z - e.getZ());
            pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
            net.minecraft.world.item.ItemStack pkg = packageStack();
            if (!pkg.isEmpty()) {
                pose.pushPose();
                pose.scale(0.32f, -0.32f, 0.32f);
                Minecraft.getInstance().getItemRenderer().renderStatic(pkg,
                        net.minecraft.world.item.ItemDisplayContext.FIXED,
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pose, buffer, player.level(), 0);
                pose.popPose();
            } else {
                quad(pose, buffer, TEX_BOX, -0.24f, -0.24f, 0.24f, 0.24f, 0xFFFFFFFF);
            }
            // 里面装了什么：小小的图标挂在包裹上方，不挡住包裹
            net.minecraft.world.item.ItemStack inside = e.getItem();
            if (inside == null || inside.isEmpty()) inside = player.getMainHandItem();
            if (!inside.isEmpty()) {
                pose.pushPose();
                pose.translate(0f, 0.42f, 0.05f);
                pose.scale(0.015f, -0.015f, 0.015f);
                Minecraft.getInstance().getItemRenderer().renderStatic(inside,
                        net.minecraft.world.item.ItemDisplayContext.GUI,
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pose, buffer, player.level(), 0);
                pose.popPose();
            }
            pose.popPose();
        }
    }

    /** 沿 (0, y0) → (tx, ty) 铺锁链 */
    private void drawChainTo(PoseStack pose, MultiBufferSource buffer, float tx, float ty, float y0, float open) {
        float len = (float) Math.sqrt(tx * tx + (ty - y0) * (ty - y0)) * open;
        int links = Math.max(1, (int) (len / 0.22f));
        for (int i = 0; i <= links; i++) {
            float f = (float) i / links * open;
            float x = tx * f;
            float y = y0 + (ty - y0) * f;
            boolean vertical = (i % 2) == 0;
            float hw = vertical ? 0.09f : 0.16f;
            float hh = vertical ? 0.16f : 0.09f;
            quad(pose, buffer, TEX_CHAIN, x - hw, y - hh, x + hw, y + hh, 0xFFFFFFFF);
        }
    }

    /** 统一取「横着的链环」方块状态：只用水平轴 X / Z，绝不用 Y */
    private static net.minecraft.world.level.block.state.BlockState chainStateHorizontal(double dx, double dz) {
        net.minecraft.core.Direction.Axis axis = (Math.abs(dx) >= Math.abs(dz))
                ? net.minecraft.core.Direction.Axis.X
                : net.minecraft.core.Direction.Axis.Z;
        return net.minecraft.world.level.block.Blocks.CHAIN.defaultBlockState()
                .setValue(net.minecraft.world.level.block.ChainBlock.AXIS, axis);
    }

    /** 机械动力的纸箱物品（没装 Create 就返回空 → 退回自己画的箱子贴图） */
    private static net.minecraft.world.item.ItemStack packageStack() {
        try {
            if (!net.neoforged.fml.ModList.get().isLoaded("create")) return net.minecraft.world.item.ItemStack.EMPTY;
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .get(ResourceLocation.fromNamespaceAndPath("create", "package"));
            if (item == null || item == net.minecraft.world.item.Items.AIR) return net.minecraft.world.item.ItemStack.EMPTY;
            return new net.minecraft.world.item.ItemStack(item);
        } catch (Throwable t) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
    }

    private static final ResourceLocation TEX_WHITE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");

    /** 裂缝用：**双面可见**（用 entityCutoutNoCull，转动视角不会再消失） */
    private void quadCulled(PoseStack pose, MultiBufferSource buffer, ResourceLocation tex,
                            float x1, float y1, float x2, float y2, int argb) {
        PoseStack.Pose last = pose.last();
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(tex));
        int a = (argb >>> 24) & 0xFF, r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        vc.addVertex(last, x1, y1, 0f).setColor(r, g, b, a).setUv(0f, 1f)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(last, 0f, 0f, 1f);
        vc.addVertex(last, x2, y1, 0f).setColor(r, g, b, a).setUv(1f, 1f)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(last, 0f, 0f, 1f);
        vc.addVertex(last, x2, y2, 0f).setColor(r, g, b, a).setUv(1f, 0f)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(last, 0f, 0f, 1f);
        vc.addVertex(last, x1, y2, 0f).setColor(r, g, b, a).setUv(0f, 0f)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(last, 0f, 0f, 1f);
    }

    /** 在广告牌平面上画一个矩形（x1,y1)-(x2,y2)，y 向上 */
    private void quad(PoseStack pose, MultiBufferSource buffer, ResourceLocation tex,
                      float x1, float y1, float x2, float y2, int argb) {
        PoseStack.Pose last = pose.last();
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(tex));
        int a = (argb >>> 24) & 0xFF, r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        vc.addVertex(last, x1, y1, 0f).setColor(r, g, b, a).setUv(0f, 1f)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(last, 0f, 0f, 1f);
        vc.addVertex(last, x2, y1, 0f).setColor(r, g, b, a).setUv(1f, 1f)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(last, 0f, 0f, 1f);
        vc.addVertex(last, x2, y2, 0f).setColor(r, g, b, a).setUv(1f, 0f)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(last, 0f, 0f, 1f);
        vc.addVertex(last, x1, y2, 0f).setColor(r, g, b, a).setUv(0f, 0f)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(last, 0f, 0f, 1f);
    }
}
