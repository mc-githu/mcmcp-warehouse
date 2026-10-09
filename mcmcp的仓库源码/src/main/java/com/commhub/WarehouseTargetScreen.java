package com.commhub;

import com.commhub.network.ModNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class WarehouseTargetScreen extends AbstractContainerScreen<WarehouseTargetMenu> {
    public WarehouseTargetScreen(WarehouseTargetMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 96;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.leftPos + 88;
        int sharedY = this.topPos + 24;
        int privateY = this.topPos + 48;

        // 配置里关掉共享大仓库时，这里就不给这个选项
        if (ModConfig.sharedWarehouseEnabled()) {
            this.addRenderableWidget(Button.builder(Component.literal("共享大仓库"), b -> choose(0, ""))
                    .bounds(cx - 60, sharedY, 120, 20).build());
        }
        this.addRenderableWidget(Button.builder(Component.literal("私人仓库"), b -> {
            // 先关闭当前菜单（把容器关包先发出去），再请求列表，避免时序交叉
            net.minecraft.core.BlockPos pos = this.menu.getPos();
            this.onClose();
            PacketDistributor.sendToServer(new ModNetworking.RequestPrivateWarehouses(pos));
        }).bounds(cx - 60, privateY, 120, 20).build());
    }

    private void choose(int target, String name) {
        PacketDistributor.sendToServer(new ModNetworking.SetWarehouseTarget(this.menu.getPos(), target, name));
        this.onClose();
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(this.leftPos, this.topPos, this.leftPos + 176, this.topPos + 96, 0xC0101010);
    }

    /** 从客户端方块实体读放置者 */
    private String placerText() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level == null) return "";
        if (mc.level.getBlockEntity(this.menu.getPos()) instanceof WarehouseBlockEntity be) {
            String n = be.getPlacerName();
            return (n == null || n.isEmpty()) ? "放置者：未知" : "放置者：" + n;
        }
        return "";
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        String cur = this.menu.getTarget() == 0 ? "当前: 共享大仓库" : "当前: 私人仓库: " + this.menu.getName();
        g.drawString(this.font, cur, 8, 8, 0xFFFFFF);
        // 显示是谁放的（防小偷）
        String placer = placerText();
        if (!placer.isEmpty()) g.drawString(this.font, placer, 8, 78, 0xA0FFA0);
    }
}
