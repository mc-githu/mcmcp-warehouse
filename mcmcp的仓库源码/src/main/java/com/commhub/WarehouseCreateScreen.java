package com.commhub;

import com.commhub.network.ModNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class WarehouseCreateScreen extends Screen {
    public enum Mode { CREATE, RENAME }
    private EditBox nameInput;
    private final Mode mode;
    private final String oldName;

    public WarehouseCreateScreen(Mode mode, String oldName) {
        super(Component.literal(mode == Mode.RENAME ? "重命名仓库" : "创建私人仓库"));
        this.mode = mode;
        this.oldName = oldName;
    }

    public WarehouseCreateScreen() {
        this(Mode.CREATE, "");
    }

    @Override
    protected void init() {
        super.init();
        this.nameInput = new EditBox(this.font, this.width / 2 - 100, this.height / 2 - 20, 200, 20, Component.literal(""));
        this.nameInput.setMaxLength(32);
        this.addRenderableWidget(this.nameInput);
        this.addRenderableWidget(Button.builder(Component.literal("完成"), b -> create())
                .bounds(this.width / 2 - 40, this.height / 2 + 10, 80, 20).build());
        this.setInitialFocus(this.nameInput);
    }

    private void create() {
        String name = this.nameInput.getValue().trim();
        if (name.isEmpty()) return;
        if (mode == Mode.RENAME) {
            PacketDistributor.sendToServer(new ModNetworking.RenameWarehouse(this.oldName, name));
        } else {
            PacketDistributor.sendToServer(new ModNetworking.CreateWarehouse(name));
        }
        this.onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawString(this.font, mode == Mode.RENAME
                ? "给「" + this.oldName + "」起新名字（不能和别人的重名）"
                : "给私人仓库起个名字（不能和别人的重名）", this.width / 2 - 110, this.height / 2 - 44, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
