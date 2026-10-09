package com.commhub;

import com.commhub.network.ModNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HubScreen extends AbstractContainerScreen<HubMenu> {
    private int selectedPlayerIndex = 0;
    private EditBox chatInput;
    private Button targetButton;

    public HubScreen(HubMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 360;
        this.imageHeight = 248;
    }

    private int mode() {
        return this.menu.getMode();
    }

    @Override
    protected void init() {
        super.init();

        // 侧边标签页：按顺序往下排，共享大仓库被配置关掉时不留空位
        int tabY = 20;
        if (ModConfig.sharedWarehouseEnabled()) {
            addTab("共享大仓库", HubMenu.MODE_SHARED, tabY);
            tabY += 24;
        }
        addTab("私人仓库", HubMenu.MODE_PRIVATE, tabY);
        tabY += 24;
        addTab("私聊", HubMenu.MODE_CHAT, tabY);
        tabY += 24;
        addTab("传物品", HubMenu.MODE_TRANSFER, tabY);
        tabY += 24;

        final int tradeY = tabY + 4;
        this.addRenderableWidget(Button.builder(Component.literal("以物换物"), b -> openTrade())
                .bounds(this.leftPos + 8, this.topPos + tradeY, 84, 20).build());

        if (mode() == HubMenu.MODE_SHARED || mode() == HubMenu.MODE_PRIVATE) {
            // 页标题占 100..206，按钮全部排在右边，互不重叠
            this.addRenderableWidget(Button.builder(Component.literal("◀"), b -> changePage(-1))
                    .bounds(this.leftPos + 210, this.topPos + 3, 16, 16).build());
            this.addRenderableWidget(Button.builder(Component.literal("▶"), b -> changePage(1))
                    .bounds(this.leftPos + 228, this.topPos + 3, 16, 16).build());
            if (mode() == HubMenu.MODE_PRIVATE) {
                this.addRenderableWidget(Button.builder(Component.literal("切换"), b -> cycleWarehouse())
                        .bounds(this.leftPos + 250, this.topPos + 3, 40, 16).build());
                this.addRenderableWidget(Button.builder(Component.literal("重命名"), b -> renameWarehouse())
                        .bounds(this.leftPos + 294, this.topPos + 3, 52, 16).build());
            }
            // 卡槽里有卡 → 显示「添加」按钮，点开配置界面
            // 管理工具区只对私人仓库（共享仓库没有管理工具）
            if (mode() == HubMenu.MODE_PRIVATE) {
                this.addRenderableWidget(Button.builder(Component.literal("绑定"), b -> bindTools())
                        .bounds(this.leftPos + 196, this.topPos + 132, 60, 16).build());
                this.addRenderableWidget(Button.builder(Component.literal("重置工具"), b -> resetTools())
                        .bounds(this.leftPos + 260, this.topPos + 132, 84, 16).build());
            }
        } else if (mode() == HubMenu.MODE_CHAT) {
            this.chatInput = new EditBox(this.font, this.leftPos + 100, this.topPos + 124, 250, 14, Component.literal(""));
            this.addRenderableWidget(this.chatInput);
            this.targetButton = Button.builder(Component.literal("目标: 无"), b -> cycleTarget())
                    .bounds(this.leftPos + 100, this.topPos + 20, 100, 18).build();
            this.addRenderableWidget(this.targetButton);
            this.addRenderableWidget(Button.builder(Component.literal("加好友"), b -> openSubScreen(new FriendListScreen(FriendListScreen.Mode.ADD)))
                    .bounds(this.leftPos + 100, this.topPos + 42, 60, 18).build());
            this.addRenderableWidget(Button.builder(Component.literal("删好友"), b -> openSubScreen(new FriendListScreen(FriendListScreen.Mode.REMOVE)))
                    .bounds(this.leftPos + 164, this.topPos + 42, 60, 18).build());
            this.addRenderableWidget(Button.builder(Component.literal("好友请求"), b -> openSubScreen(new FriendListScreen(FriendListScreen.Mode.REQUESTS)))
                    .bounds(this.leftPos + 228, this.topPos + 42, 70, 18).build());
        } else if (mode() == HubMenu.MODE_TRANSFER) {
            this.targetButton = Button.builder(Component.literal("目标: 无"), b -> cycleTarget())
                    .bounds(this.leftPos + 160, this.topPos + 20, 100, 18).build();
            this.addRenderableWidget(this.targetButton);
            this.addRenderableWidget(Button.builder(Component.literal("发送"), b -> sendItems())
                    .bounds(this.leftPos + 264, this.topPos + 20, 60, 18).build());
            this.addRenderableWidget(Button.builder(Component.literal("领取全部"), b -> claimMailbox())
                    .bounds(this.leftPos + 270, this.topPos + 80, 80, 18).build());
        }
    }

    private void addTab(String label, int mode, int y) {
        boolean active = mode() == mode;
        this.addRenderableWidget(Button.builder(Component.literal(label), b -> switchMode(mode))
                .bounds(this.leftPos + 8, this.topPos + y, 84, 20).build());
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        int m = mode();
        g.fill(x, y, x + 360, y + 250, 0xC0101010);

        // 各区域用「细边框 + 极淡底色」描出来，框与框共用边线、绝不重叠
        // 左侧功能栏（黄）
        frame(g, x + 6, y + 2, x + 95, y + 246, 0x40FFD080);

        int baseY = (m == HubMenu.MODE_TRANSFER) ? 158 : 140;

        if (m == HubMenu.MODE_TRANSFER) {
            // 发送格（青）—— 格子 100..154 / 20..74
            frame(g, x + 97, y + 19, x + 157, y + 75, 0x4090E0FF);
            // 邮箱（紫）—— 格子 100..262 / 76..148，和发送格共用边线
            frame(g, x + 97, y + 75, x + 263, y + 149, 0x40C080FF);
        } else if (m == HubMenu.MODE_SHARED || m == HubMenu.MODE_PRIVATE) {
            // 仓库（青）—— 格子 100..262 / 20..128
            frame(g, x + 97, y + 19, x + 263, y + 129, 0x4090E0FF);
        }

        if (m == HubMenu.MODE_PRIVATE) {
            // 管理工具栏（红）—— 格子 268..286 / 22..76
            frame(g, x + 265, y + 19, x + 289, y + 76, 0x40FF8080);
            // 合成升级卡槽 ×3（金）—— 格子 268..286 / 76..130，和工具栏共用边线
            for (int i = 0; i < HubSavedData.CARD_SLOTS; i++) {
                int cy = y + 76 + i * 18;
                frame(g, x + 265, cy, x + 289, cy + 18, 0x40FFC040);
            }
        }

        // 背包 + 快捷栏（绿）—— 格子 100..262 / baseY..baseY+76
        frame(g, x + 97, y + baseY - 1, x + 263, y + baseY + 77, 0x4080FF80);
    }

    /** 画一个细描边的区域框：1px 边框 + 很淡的底色（底色淡，格子里的物品才清楚） */
    private void frame(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1, y1, x2, y2, color & 0x22FFFFFF);          // 底色只用 13% 不透明
        int border = (color & 0x00FFFFFF) | 0xD0000000;       // 边线 80% 不透明，细而清楚
        g.fill(x1, y1, x2, y1 + 1, border);
        g.fill(x1, y2 - 1, x2, y2, border);
        g.fill(x1, y1, x1 + 1, y2, border);
        g.fill(x2 - 1, y1, x2, y2, border);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        int m = mode();

        if (m == HubMenu.MODE_SHARED) {
            // 标题短一点，绝不压到右边的按钮
            g.drawString(this.font, "共享大仓库 · 第 " + (this.menu.getPage() + 1) + "/" + this.menu.getMaxPages() + " 页",
                    this.leftPos + 100, this.topPos + 7, 0xFFFFFF);
        } else if (m == HubMenu.MODE_PRIVATE) {
            g.drawString(this.font, "第 " + (this.menu.getPage() + 1) + "/" + this.menu.getMaxPages() + " 页",
                    this.leftPos + 100, this.topPos + 7, 0xFFFFFF);
            // 仓库名放到左侧栏（那里是空的），避免和按钮打架
            String name = this.menu.getName();
            String label = name.isEmpty() ? "尚未创建仓库" : "仓库：" + name;
            g.drawString(this.font, truncate(label, 10), this.leftPos + 8, this.topPos + 178, 0xFFFF55);
        } else if (m == HubMenu.MODE_CHAT) {
            g.drawString(this.font, "私聊（需先加好友）", this.leftPos + 100, this.topPos + 6, 0xFFFFFF);
            renderChat(g);
        } else if (m == HubMenu.MODE_TRANSFER) {
            g.drawString(this.font, "发送物品", this.leftPos + 100, this.topPos + 8, 0xFFFFFF);
            // 放到右边空白处（原来画在 y=64，正好压在发送格第三行上）
            g.drawString(this.font, "我的邮箱", this.leftPos + 266, this.topPos + 44, 0xFFFFFF);
        }
        // 「背包」标签放到左侧栏（那里一直空着）—— 原来画在 y≈130，会压住聊天输入框和邮箱框
        g.drawString(this.font, "背包 ↓", this.leftPos + 8, this.topPos + 200, 0x80FF80);
        String fluidText = ClientHubState.fluidAmount > 0
                ? "💧 " + Component.translatable(ClientHubState.fluidName).getString() + ": " + ClientHubState.fluidAmount + " mB"
                : "💧 无液体";
        g.drawString(this.font, "⚡ 电能: " + ClientHubState.energy + " FE    " + fluidText, this.leftPos + 100, this.topPos + 236, 0xFFFF55);
    }

    private void renderChat(GuiGraphics g) {
        UUID me = this.minecraft.player == null ? null : this.minecraft.player.getUUID();
        if (this.targetButton != null) {
            ModNetworking.PlayerInfo t = selectedPlayer();
            this.targetButton.setMessage(Component.literal("目标: " + (t == null ? "无" : t.name())));
        }
        List<ChatEntry> log = currentLog(me);
        int start = Math.max(0, log.size() - 5);
        for (int i = start; i < log.size(); i++) {
            ChatEntry e = log.get(i);
            String line;
            if (e.isPublic()) {
                line = e.senderName() + ": " + e.message();
            } else {
                line = (me != null && e.sender().equals(me) ? "我: " : e.senderName() + ": ") + e.message();
            }
            g.drawString(this.font, truncate(line, 40), this.leftPos + 100, this.topPos + 66 + (i - start) * 10, 0xE0E0E0);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.chatInput != null && this.chatInput.isFocused()
                && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            sendChatMessage();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** 关界面时记住现在看的是哪个标签页 */
    @Override
    public void onClose() {
        ModConfig.rememberTab(mode(), this.menu.getName());
        super.onClose();
    }

    private void switchMode(int mode) {
        // 切标签页也顺手记一下（万一不是正常关闭）
        ModConfig.rememberTab(mode, this.menu.getName());
        PacketDistributor.sendToServer(new ModNetworking.ChangeView(mode, 0, this.menu.getName()));
    }

    /** 打开子界面前先关闭容器，避免「容器开着但没界面」的脏状态 */
    private void openSubScreen(net.minecraft.client.gui.screens.Screen screen) {
        this.onClose();
        this.minecraft.setScreen(screen);
    }

    private void newWarehouse() {
        openSubScreen(new WarehouseCreateScreen());
    }

    private void renameWarehouse() {
        openSubScreen(new WarehouseCreateScreen(WarehouseCreateScreen.Mode.RENAME, this.menu.getName()));
    }


    private void bindTools() {
        PacketDistributor.sendToServer(new ModNetworking.BindTools(mode(), this.menu.getName()));
    }

    private void resetTools() {
        PacketDistributor.sendToServer(new ModNetworking.ResetTools(mode(), this.menu.getName()));
    }

    private void cycleWarehouse() {
        List<String> list = ClientHubState.warehouses;
        if (list.isEmpty()) return;
        int idx = list.indexOf(this.menu.getName());
        String next = list.get(Math.floorMod(idx + 1, list.size()));
        PacketDistributor.sendToServer(new ModNetworking.ChangeView(HubMenu.MODE_PRIVATE, 0, next));
    }

    private void changePage(int delta) {
        int newPage = Math.floorMod(this.menu.getPage() + delta, this.menu.getMaxPages());
        PacketDistributor.sendToServer(new ModNetworking.ChangeView(mode(), newPage, this.menu.getName()));
    }

    private List<ModNetworking.PlayerInfo> getOtherPlayers() {
        List<ModNetworking.PlayerInfo> list = new ArrayList<>();
        UUID me = this.minecraft.player == null ? null : this.minecraft.player.getUUID();
        for (ModNetworking.PlayerInfo p : ClientHubState.players) {
            if (me != null && p.uuid().equals(me)) {
                // 测试模式：把自己也列进目标，方便单人测私聊
                if (ModConfig.allowSelfTest()) list.add(p);
                continue;
            }
            // 私聊/传物品只能对好友，目标只列好友
            if (ClientHubState.friendIds.contains(p.uuid())) list.add(p);
        }
        return list;
    }

    private ModNetworking.PlayerInfo selectedPlayer() {
        List<ModNetworking.PlayerInfo> list = getOtherPlayers();
        if (list == null || list.isEmpty()) return null;
        if (this.selectedPlayerIndex < 0 || this.selectedPlayerIndex >= list.size()) this.selectedPlayerIndex = 0;
        return list.get(this.selectedPlayerIndex);
    }

    private void cycleTarget() {
        List<ModNetworking.PlayerInfo> list = getOtherPlayers();
        if (list.isEmpty()) return;
        this.selectedPlayerIndex = (this.selectedPlayerIndex + 1) % list.size();
    }

    private List<ChatEntry> currentLog(UUID me) {
        ModNetworking.PlayerInfo target = selectedPlayer();
        if (target == null || me == null) return List.of();
        List<ChatEntry> result = new ArrayList<>();
        for (ChatEntry e : ClientHubState.privateLog) {
            boolean fromMe = e.sender().equals(me) && target.uuid().equals(e.recipient());
            boolean toMe = target.uuid().equals(e.sender()) && me.equals(e.recipient());
            if (fromMe || toMe) result.add(e);
        }
        return result;
    }

    private void sendChatMessage() {
        String text = this.chatInput.getValue().trim();
        if (text.isEmpty()) return;
        ModNetworking.PlayerInfo target = selectedPlayer();
        if (target == null) return;
        PacketDistributor.sendToServer(new ModNetworking.SendChat(false, target.uuid(), text));
        this.chatInput.setValue("");
    }

    private void sendItems() {
        ModNetworking.PlayerInfo target = selectedPlayer();
        if (target == null) return;
        PacketDistributor.sendToServer(new ModNetworking.Transfer(target.uuid()));
    }

    private void openTrade() {
        PacketDistributor.sendToServer(new ModNetworking.OpenTrade(TradeMenu.MODE_BOUNTY));
    }

    private void friendAction(int action) {
        ModNetworking.PlayerInfo t = selectedPlayer();
        if (t == null) return;
        PacketDistributor.sendToServer(new ModNetworking.FriendAction(action, t.uuid()));
    }

    private void claimMailbox() {
        PacketDistributor.sendToServer(new ModNetworking.ClaimMailbox());
    }




    private static String truncate(String s, int max) {
        if (s.length() <= max) return s;
        return s.substring(0, max - 1) + "…";
    }
}
