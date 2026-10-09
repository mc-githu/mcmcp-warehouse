package com.commhub;

import com.commhub.network.ModNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** 选择私人仓库列表：点一个选中，点退出回上一级（共享/私人选择） */
public class WarehouseListScreen extends Screen {
    private final BlockPos pos;
    private final List<String> names;
    private int page = 0;
    private int pageCount = 1;

    public WarehouseListScreen(BlockPos pos, List<String> names) {
        super(Component.literal("选择私人仓库"));
        this.pos = pos;
        // 空值保护：包解析失败时给空列表，避免 NPE
        this.names = (names == null) ? new java.util.ArrayList<>() : names;
    }

    private void rebuild() {
        this.clearWidgets();
        this.init();
    }

    @Override
    protected void init() {
        super.init();
        int y = this.height / 2 - 60;
        // 每屏数量按屏幕高度自适应
        int perScreen = Math.max(3, (this.height - 150) / 22);
        this.pageCount = Math.max(1, (names.size() + perScreen - 1) / perScreen);
        if (this.page >= this.pageCount) this.page = this.pageCount - 1;
        if (names.isEmpty()) {
            this.addRenderableWidget(Button.builder(Component.literal("还没有私人仓库（先用管理员凭证建一个）"), b -> back())
                    .bounds(this.width / 2 - 120, y, 240, 20).build());
        } else {
            int start = this.page * perScreen;
            int count = Math.min(perScreen, names.size() - start);
            for (int i = 0; i < count; i++) {
                String name = names.get(start + i);
                this.addRenderableWidget(Button.builder(Component.literal(name), b -> choose(name))
                        .bounds(this.width / 2 - 90, y + i * 22, 180, 20).build());
            }
        }
        if (this.pageCount > 1) {
            this.addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
                this.page = Math.floorMod(this.page - 1, this.pageCount);
                this.rebuild();
            }).bounds(this.width / 2 - 60, this.height / 2 + 60, 20, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
                this.page = Math.floorMod(this.page + 1, this.pageCount);
                this.rebuild();
            }).bounds(this.width / 2 + 40, this.height / 2 + 60, 20, 20).build());
        }
        this.addRenderableWidget(Button.builder(Component.literal("退出"), b -> back())
                .bounds(this.width / 2 - 40, this.height / 2 + (this.pageCount > 1 ? 86 : 64), 80, 20).build());
    }

    private void choose(String name) {
        PacketDistributor.sendToServer(new ModNetworking.SetWarehouseTarget(this.pos, 1, name));
        this.onClose();
    }

    private void back() {
        // 回上一级（共享大仓库/私人仓库 选择界面）
        PacketDistributor.sendToServer(new ModNetworking.ReopenWarehouseTarget(this.pos));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, "选择要连接的私人仓库", this.width / 2, this.height / 2 - 84, 0xFFFFFF);
        if (this.pageCount > 1) {
            g.drawCenteredString(this.font, "第 " + (this.page + 1) + " / " + this.pageCount + " 页", this.width / 2, this.height / 2 + 50, 0xAAAAAA);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
