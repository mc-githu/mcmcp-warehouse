package com.commhub;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class HubMenu extends AbstractContainerMenu {
    public static final int WAREHOUSE_PER_PAGE = 54;
    /** 页数上限（防极端值；客户端镜像按需建，不受这个数影响） */
    public static final int MAX_PAGES = 1024;
    public static final int MAILBOX_SLOTS = 36; // 9列 x 4行

    /** 把仓库容量向上取整到整页（54 的倍数），避免最后一页越界 */
    /** 是不是三把管理工具之一（未绑定/已绑定都算） */
    public static boolean isManagementTool(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        var it = stack.getItem();
        return it == ModItems.ADD_MEMBER_TOOL.get()
                || it == ModItems.REMOVE_MEMBER_TOOL.get()
                || it == ModItems.TRANSFER_ADMIN_TOOL.get();
    }

    public static int roundToPage(int slots) {
        int n = Math.max(1, slots);
        // 用 long 计算，防止 (n + 53) 在接近 Integer.MAX_VALUE 时溢出成负数
        long pages = ((long) n + WAREHOUSE_PER_PAGE - 1) / WAREHOUSE_PER_PAGE;
        long rounded = pages * WAREHOUSE_PER_PAGE;
        return (int) Math.min(rounded, Integer.MAX_VALUE / WAREHOUSE_PER_PAGE * (long) WAREHOUSE_PER_PAGE);
    }
    public static final int SEND_SLOTS = 9;

    public static final int MODE_SHARED = 0;
    public static final int MODE_PRIVATE = 1;
    public static final int MODE_CHAT = 2;
    public static final int MODE_TRANSFER = 3;

    private final int mode;
    private final int page;
    private final String name;
    private final IItemHandler warehouse;
    private final IItemHandler mailbox;
    private final IItemHandler sendSlots;
    private final IItemHandler tools;
    private final IItemHandler card;
    /** 服务端仓库数据（客户端为 null） */
    private final HubSavedData data;
    /** 服务端仓库真实页数（决定界面显示「第 x / y 页」和能翻到哪） */
    private int warehousePages = 1;
    private final java.util.UUID ownerUUID;
    private final java.util.UUID adminUUID;

    // client constructor (page/mode from server)
    public HubMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readInt(), buf.readInt(), buf.readUtf(),
                buf.readBoolean() ? buf.readUUID() : null,
                buf.readBoolean() ? buf.readUUID() : null,
                buf.readInt());
    }

    /** 客户端专用：按「当前页 + 服务端真实页数」建镜像，格号不会越界、页数显示也正确 */
    private HubMenu(int containerId, Inventory playerInventory, int mode, int page, String name,
                    java.util.UUID ownerUUID, java.util.UUID adminUUID, int pages) {
        this(containerId, playerInventory,
                new PagedItemHandler(Math.max(1, Math.min(MAX_PAGES, Math.max(page + 2, pages + 1)))),
                new ItemStackHandler(MAILBOX_SLOTS),
                new ItemStackHandler(SEND_SLOTS),
                new ItemStackHandler(3),
                new ItemStackHandler(HubSavedData.CARD_SLOTS),   // 卡片不再放仓库，这个 handler 只是为了保持构造签名
                null, mode, page, name, ownerUUID, adminUUID);
        this.warehousePages = Math.max(1, pages);
    }

    // server constructor
    public HubMenu(int containerId, Inventory playerInventory, IItemHandler warehouse, IItemHandler mailbox, IItemHandler tools, IItemHandler card, HubSavedData data, int mode, int page, String name, java.util.UUID ownerUUID, java.util.UUID adminUUID) {
        this(containerId, playerInventory, warehouse, mailbox, new ItemStackHandler(SEND_SLOTS), tools, card, data, mode, page, name, ownerUUID, adminUUID);
    }

    private HubMenu(int containerId, Inventory playerInventory, IItemHandler warehouse, IItemHandler mailbox, IItemHandler sendSlots, IItemHandler tools, IItemHandler card, HubSavedData data, int mode, int page, String name, java.util.UUID ownerUUID, java.util.UUID adminUUID) {
        super(ModMenuTypes.HUB_MENU.get(), containerId);
        this.warehouse = warehouse;
        this.mailbox = mailbox;
        this.sendSlots = sendSlots;
        this.tools = tools;
        this.card = card;
        this.data = data;
        this.mode = mode;
        this.page = page;
        this.name = name;
        this.ownerUUID = ownerUUID;
        this.adminUUID = adminUUID;
        this.warehousePages = Math.max(1, warehouse.getSlots() / (WAREHOUSE_PER_PAGE <= 0 ? 54 : WAREHOUSE_PER_PAGE));
        addSlots(playerInventory);
    }

    private void addSlots(Inventory inv) {
        if (mode == MODE_SHARED || mode == MODE_PRIVATE) {
            int base = page * WAREHOUSE_PER_PAGE;
            for (int row = 0; row < 6; row++) {
                for (int col = 0; col < 9; col++) {
                    this.addSlot(new SlotItemHandler(warehouse, base + col + row * 9, 100 + col * 18, 20 + row * 18) {
                        @Override
                        public int getMaxStackSize(ItemStack stack) {
                            // 跟随仓库的无限堆叠上限（否则鼠标点击放置会被卡在 64）
                            return this.getItemHandler().getSlotLimit(this.getSlotIndex());
                        }
                    });
                }
            }
            // 合成升级卡槽 ×3（只有私人仓库有）：卡放这里就会按卡上的配方自动合成
            // 配卡是右键空气打开的配置界面，所以这里不需要额外按钮
            for (int ci = 0; mode == MODE_PRIVATE && ci < HubSavedData.CARD_SLOTS; ci++) {
                this.addSlot(new SlotItemHandler(card, ci, 268, 76 + ci * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        // 合成升级卡 / 机械手卡片 / 动力锯卡片 都能放
                        return SimpleCard.isWarehouseCard(stack);
                    }

                    @Override
                    public boolean mayPickup(net.minecraft.world.entity.player.Player player) {
                        return player != null
                                && (player.getUUID().equals(ownerUUID)
                                    || (adminUUID != null && player.getUUID().equals(adminUUID)));
                    }
                });
            }
            // 管理工具只属于私人仓库：共享仓库没有主人，不需要管理工具
            if (mode == MODE_PRIVATE) {
                for (int i = 0; i < 3; i++) {
                    // 允许放入：只接受三把管理工具（未绑定的也行），放好后点「绑定」按钮绑定到本仓库
                    this.addSlot(new SlotItemHandler(tools, i, 268, 22 + i * 18) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return isManagementTool(stack);
                        }

                        @Override
                        public boolean mayPickup(Player player) {
                            // 只有主人或该仓库管理员能拿工具，成员/其他人拿不到
                            return player != null
                                    && (player.getUUID().equals(ownerUUID)
                                        || (adminUUID != null && player.getUUID().equals(adminUUID)));
                        }
                    });
                }
            }
            addInventory(inv);
        } else if (mode == MODE_TRANSFER) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    this.addSlot(new SlotItemHandler(sendSlots, col + row * 3, 100 + col * 18, 20 + row * 18));
                }
            }
            // 邮箱：9 列 x 4 行 = 36 格（容量>=旧5x7的35，不丢存档）
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 9; col++) {
                    this.addSlot(new SlotItemHandler(mailbox, col + row * 9, 100 + col * 18, 76 + row * 18));
                }
            }
            addInventory(inv);
        } else {
            addInventory(inv);
        }
    }

    private void addInventory(Inventory inv) {
        // 传物品页邮箱占位更大，背包整体下移
        int baseY = (mode == MODE_TRANSFER) ? 158 : 140;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 100 + col * 18, baseY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 100 + col * 18, baseY + 58));
        }
    }

    public int getMode() {
        return mode;
    }

    public String getName() {
        return name == null ? "" : name;
    }

    public int getPage() {
        return page;
    }

    /** 界面显示的页数：真实页数 + 1（多出来那页用来「往后翻」扩容；空页存档时会丢） */
    public int getMaxPages() {
        return Math.max(1, Math.min(MAX_PAGES, warehousePages + 1));
    }

    public IItemHandler getTools() {
        return tools;
    }

    /** 合成升级卡卡槽（1 格） */
    public IItemHandler getCard() {
        return card;
    }

    public IItemHandler getSendSlots() {
        return sendSlots;
    }

    public IItemHandler getMailbox() {
        return mailbox;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return result;
        ItemStack stack = slot.getItem();
        result = stack.copy();

        int warehouseSlots = (mode == MODE_SHARED || mode == MODE_PRIVATE) ? 54 : 0;

        int toolCount = (mode == MODE_PRIVATE) ? 3 : 0; // 只有私人仓库有管理工具栏
        int sendCount = (mode == MODE_TRANSFER) ? SEND_SLOTS : 0;
        int mailboxCount = (mode == MODE_TRANSFER) ? MAILBOX_SLOTS : 0;
        // 卡槽：只有私人仓库有 3 格（顺序在仓库格之后、管理工具之前）
        int cardCount = (mode == MODE_PRIVATE) ? HubSavedData.CARD_SLOTS : 0;
        // 注意：卡槽 3 格 + 工具栏 3 格都必须算进容器格，否则 shift-click 会越界/错位
        int containerSlots = warehouseSlots + cardCount + toolCount + sendCount + mailboxCount;
        int invStart = containerSlots;
        int invEnd = containerSlots + 36;

        if (mode == MODE_TRANSFER) {
            int sendEnd = SEND_SLOTS;                       // 发送格 0..8
            int mailStart = SEND_SLOTS;                     // 邮箱 9..43
            int mailEnd = SEND_SLOTS + MAILBOX_SLOTS;
            if (index < sendEnd) {
                // 发送格 -> 背包
                if (!this.moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
            } else if (index < mailEnd) {
                // 邮箱 -> 背包（领取）
                if (!this.moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
            } else {
                // 背包 -> 只放进【发送格】，不塞邮箱（邮箱是收件箱）
                if (!this.moveItemStackTo(stack, 0, sendEnd, false)) return ItemStack.EMPTY;
            }
        } else if (index < containerSlots) {
            if (!this.moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
        } else if (mode == MODE_SHARED || mode == MODE_PRIVATE) {
            // 背包 -> 仓库：分页仓库会自动扩页，塞不下的退回
            ItemStack leftover = (this.warehouse instanceof PagedItemHandler ph)
                    ? ph.insertStacked(stack)
                    : ItemHandlerHelper.insertItemStacked(this.warehouse, stack, false);
            slot.set(leftover);
            return result;
        } else if (containerSlots > 0) {
            // 兜底：其它模式的容器
            if (!this.moveItemStackTo(stack, 0, containerSlots, false)) return ItemStack.EMPTY;
        } else {
            // 聊天页：背包内主背包 <-> 快捷栏
            if (index < invStart + 27) {
                if (!this.moveItemStackTo(stack, invStart + 27, invEnd, false)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(stack, invStart, invStart + 27, false)) return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
