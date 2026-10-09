package com.commhub;

import com.commhub.network.ModNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FriendListScreen extends Screen {
    public enum Mode { ADD, REMOVE, REQUESTS }

    private final Mode mode;
    private int page = 0;
    private int pageCount = 1;

    private void rebuild() {
        this.clearWidgets();
        this.init();
    }

    public FriendListScreen(Mode mode) {
        super(Component.literal(mode == Mode.ADD ? "添加好友" : mode == Mode.REMOVE ? "删除好友" : "好友请求"));
        this.mode = mode;
    }

    private List<UUID> entries() {
        UUID me = Minecraft.getInstance().player == null ? null : Minecraft.getInstance().player.getUUID();
        List<UUID> list = new ArrayList<>();
        if (mode == Mode.ADD) {
            for (ModNetworking.PlayerInfo p : ClientHubState.players) {
                if (me != null && p.uuid().equals(me)) continue;
                if (ClientHubState.friendIds.contains(p.uuid())) continue;
                list.add(p.uuid());
            }
        } else if (mode == Mode.REMOVE) {
            list.addAll(ClientHubState.friendIds);
        } else {
            list.addAll(ClientHubState.incomingIds);
        }
        return list;
    }

    private String nameOf(UUID id) {
        for (ModNetworking.PlayerInfo p : ClientHubState.players) {
            if (p.uuid().equals(id)) return p.name();
        }
        return id.toString().substring(0, 8);
    }

    @Override
    protected void init() {
        super.init();
        List<UUID> entries = entries();
        // 每屏最多放几个，保证不超出屏幕（底部还要留"关闭"按钮）
        int perScreen = Math.max(3, (this.height - 140) / 22);
        this.pageCount = Math.max(1, (entries.size() + perScreen - 1) / perScreen);
        if (this.page >= this.pageCount) this.page = this.pageCount - 1;
        int y = this.height / 2 - 60;
        int start = this.page * perScreen;
        int count = Math.min(perScreen, Math.max(0, entries.size() - start));
        for (int i = 0; i < count; i++) {
            UUID id = entries.get(start + i);
            String name = nameOf(id);
            final UUID fid = id;
            if (mode == Mode.ADD) {
                this.addRenderableWidget(Button.builder(Component.literal(name), btn -> addFriend(fid))
                        .bounds(this.width / 2 - 100, y + i * 22, 200, 20).build());
            } else if (mode == Mode.REMOVE) {
                this.addRenderableWidget(Button.builder(Component.literal(name), btn -> confirmRemove(fid, name))
                        .bounds(this.width / 2 - 100, y + i * 22, 200, 20).build());
            } else {
                this.addRenderableWidget(Button.builder(Component.literal(name), btn -> acceptRequest(fid))
                        .bounds(this.width / 2 - 100, y + i * 22, 150, 20).build());
                this.addRenderableWidget(Button.builder(Component.literal("✗"), btn -> rejectRequest(fid))
                        .bounds(this.width / 2 + 54, y + i * 22, 46, 20).build());
            }
        }
        // 翻页按钮（超出时才显示）
        if (this.pageCount > 1) {
            this.addRenderableWidget(Button.builder(Component.literal("◀"), btn -> {
                this.page = Math.floorMod(this.page - 1, this.pageCount);
                this.rebuild();
            }).bounds(this.width / 2 - 60, this.height / 2 + 60, 20, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal("▶"), btn -> {
                this.page = Math.floorMod(this.page + 1, this.pageCount);
                this.rebuild();
            }).bounds(this.width / 2 + 40, this.height / 2 + 60, 20, 20).build());
        }
        this.addRenderableWidget(Button.builder(Component.literal("关闭"), btn -> this.onClose())
                .bounds(this.width / 2 - 40, this.height / 2 + (this.pageCount > 1 ? 84 : 80), 80, 20).build());
    }

    private void addFriend(UUID id) {
        PacketDistributor.sendToServer(new ModNetworking.FriendAction(0, id));
        this.onClose();
    }

    private void acceptRequest(UUID id) {
        PacketDistributor.sendToServer(new ModNetworking.FriendAction(1, id));
        this.onClose();
    }

    private void rejectRequest(UUID id) {
        PacketDistributor.sendToServer(new ModNetworking.FriendAction(3, id));
        this.onClose();
    }

    private void confirmRemove(UUID id, String name) {
        this.minecraft.setScreen(new ConfirmScreen(
                result -> {
                    if (result) PacketDistributor.sendToServer(new ModNetworking.FriendAction(2, id));
                    this.minecraft.setScreen(this);
                },
                Component.literal("删除好友"),
                Component.literal("确定要删除好友 " + name + " 吗？"),
                Component.literal("删除"),
                Component.literal("取消")));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (entries().isEmpty()) {
            g.drawCenteredString(this.font, "（空）", this.width / 2, this.height / 2, 0xFFFFFF);
        } else if (this.pageCount > 1) {
            g.drawCenteredString(this.font, "第 " + (this.page + 1) + " / " + this.pageCount + " 页", this.width / 2, this.height / 2 + 50, 0xAAAAAA);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
