package com.commhub.network;

import com.commhub.ChatEntry;
import com.commhub.ClientHubState;
import com.commhub.HubMenu;
import com.commhub.HubSavedData;
import com.commhub.Trade;
import com.commhub.WarehouseBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ModNetworking {
    private static final StreamCodec<ByteBuf, UUID> UUID_CODEC =
            ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString);

    private static final StreamCodec<ByteBuf, UUID> NULLABLE_UUID_CODEC =
            ByteBufCodecs.STRING_UTF8.map(
                    s -> s.isEmpty() ? null : UUID.fromString(s),
                    u -> u == null ? "" : u.toString());

    private static final StreamCodec<ByteBuf, ChatEntry> CHAT_ENTRY_CODEC = StreamCodec.composite(
            UUID_CODEC, ChatEntry::sender,
            ByteBufCodecs.STRING_UTF8, ChatEntry::senderName,
            NULLABLE_UUID_CODEC, ChatEntry::recipient,
            ByteBufCodecs.STRING_UTF8, ChatEntry::message,
            ByteBufCodecs.VAR_LONG, ChatEntry::timestamp,
            ByteBufCodecs.BOOL, ChatEntry::isPublic,
            ChatEntry::new);

    private static final StreamCodec<ByteBuf, PlayerInfo> PLAYER_INFO_CODEC = StreamCodec.composite(
            UUID_CODEC, PlayerInfo::uuid,
            ByteBufCodecs.STRING_UTF8, PlayerInfo::name,
            PlayerInfo::new);

    private static final StreamCodec<ByteBuf, List<ChatEntry>> CHAT_LIST_CODEC =
            ByteBufCodecs.<ByteBuf, ChatEntry, List<ChatEntry>>collection(ArrayList::new, CHAT_ENTRY_CODEC);

    private static final StreamCodec<ByteBuf, List<PlayerInfo>> PLAYER_LIST_CODEC =
            ByteBufCodecs.<ByteBuf, PlayerInfo, List<PlayerInfo>>collection(ArrayList::new, PLAYER_INFO_CODEC);

    private static final StreamCodec<ByteBuf, TradeInfo> TRADE_INFO_CODEC = StreamCodec.composite(
            UUID_CODEC, TradeInfo::id,
            UUID_CODEC, TradeInfo::owner,
            ByteBufCodecs.STRING_UTF8, TradeInfo::ownerName,
            ByteBufCodecs.STRING_UTF8, TradeInfo::offer,
            ByteBufCodecs.STRING_UTF8, TradeInfo::want,
            TradeInfo::new);

    private static final StreamCodec<ByteBuf, List<UUID>> UUID_LIST_CODEC =
            ByteBufCodecs.<ByteBuf, UUID, List<UUID>>collection(ArrayList::new, UUID_CODEC);

    private static final StreamCodec<ByteBuf, List<String>> STRING_LIST_CODEC =
            ByteBufCodecs.<ByteBuf, String, List<String>>collection(ArrayList::new, ByteBufCodecs.STRING_UTF8);

    private static final StreamCodec<ByteBuf, List<TradeInfo>> TRADE_LIST_CODEC =
            ByteBufCodecs.<ByteBuf, TradeInfo, List<TradeInfo>>collection(ArrayList::new, TRADE_INFO_CODEC);

    public record PlayerInfo(UUID uuid, String name) {
    }

    public record TradeInfo(UUID id, UUID owner, String ownerName, String offer, String want) {
    }

    public record OpenHub(int mode, String name) implements CustomPacketPayload {
        public static final Type<OpenHub> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("commhub", "open_hub"));
        public static final StreamCodec<ByteBuf, OpenHub> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, OpenHub::mode,
                ByteBufCodecs.STRING_UTF8, OpenHub::name,
                OpenHub::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SendChat(boolean isPublic, UUID target, String message) implements CustomPacketPayload {
        public static final Type<SendChat> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("commhub", "send_chat"));
        public static final StreamCodec<ByteBuf, SendChat> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, SendChat::isPublic,
                NULLABLE_UUID_CODEC, SendChat::target,
                ByteBufCodecs.STRING_UTF8, SendChat::message,
                SendChat::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Transfer(UUID target) implements CustomPacketPayload {
        public static final Type<Transfer> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("commhub", "transfer"));
        public static final StreamCodec<ByteBuf, Transfer> STREAM_CODEC = StreamCodec.composite(
                UUID_CODEC, Transfer::target,
                Transfer::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ClaimMailbox() implements CustomPacketPayload {
        public static final Type<ClaimMailbox> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("commhub", "claim_mailbox"));
        public static final StreamCodec<ByteBuf, ClaimMailbox> STREAM_CODEC = StreamCodec.unit(new ClaimMailbox());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SetWarehouseTarget(BlockPos pos, int target, String name) implements CustomPacketPayload {
        public static final Type<SetWarehouseTarget> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("commhub", "set_warehouse_target"));
        public static final StreamCodec<ByteBuf, SetWarehouseTarget> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, SetWarehouseTarget::pos,
                ByteBufCodecs.VAR_INT, SetWarehouseTarget::target,
                ByteBufCodecs.STRING_UTF8, SetWarehouseTarget::name,
                SetWarehouseTarget::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record FriendAction(int action, UUID target) implements CustomPacketPayload {
        public static final Type<FriendAction> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("commhub", "friend_action"));
        public static final StreamCodec<ByteBuf, FriendAction> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, FriendAction::action,
                UUID_CODEC, FriendAction::target,
                FriendAction::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SyncTrades(List<TradeInfo> trades, boolean selfTrade) implements CustomPacketPayload {
        public static final Type<SyncTrades> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("commhub", "sync_trades"));
        public static final StreamCodec<ByteBuf, SyncTrades> STREAM_CODEC = StreamCodec.composite(
                TRADE_LIST_CODEC, SyncTrades::trades,
                ByteBufCodecs.BOOL, SyncTrades::selfTrade,
                SyncTrades::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record CreateTrade(List<String> offer, List<String> want) implements CustomPacketPayload {
        public static final Type<CreateTrade> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("commhub", "create_trade"));
        public static final StreamCodec<ByteBuf, CreateTrade> STREAM_CODEC = StreamCodec.composite(
                STRING_LIST_CODEC, CreateTrade::offer,
                STRING_LIST_CODEC, CreateTrade::want,
                CreateTrade::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record AcceptTrade(UUID id) implements CustomPacketPayload {
        public static final Type<AcceptTrade> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("commhub", "accept_trade"));
        public static final StreamCodec<ByteBuf, AcceptTrade> STREAM_CODEC = StreamCodec.composite(
                UUID_CODEC, AcceptTrade::id,
                AcceptTrade::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record CancelTrade(UUID id) implements CustomPacketPayload {
        public static final Type<CancelTrade> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("commhub", "cancel_trade"));
        public static final StreamCodec<ByteBuf, CancelTrade> STREAM_CODEC = StreamCodec.composite(
                UUID_CODEC, CancelTrade::id,
                CancelTrade::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record OpenTrade(int mode) implements CustomPacketPayload {
        public static final Type<OpenTrade> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("commhub", "open_trade"));
        public static final StreamCodec<ByteBuf, OpenTrade> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, OpenTrade::mode,
                OpenTrade::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ChangeView(int mode, int page, String name) implements CustomPacketPayload {
        public static final Type<ChangeView> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("commhub", "change_view"));
        public static final StreamCodec<ByteBuf, ChangeView> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ChangeView::mode,
                ByteBufCodecs.VAR_INT, ChangeView::page,
                ByteBufCodecs.STRING_UTF8, ChangeView::name,
                ChangeView::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record OpenCreateDialog() implements CustomPacketPayload {
        public static final Type<OpenCreateDialog> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("commhub", "open_create_dialog"));
        public static final StreamCodec<ByteBuf, OpenCreateDialog> STREAM_CODEC = StreamCodec.unit(new OpenCreateDialog());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record PublishResult(boolean ok) implements CustomPacketPayload {
        public static final Type<PublishResult> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("commhub", "publish_result"));
        public static final StreamCodec<ByteBuf, PublishResult> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, PublishResult::ok,
                PublishResult::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SyncPools(int energy, int fluidAmount, String fluidName) implements CustomPacketPayload {
        public static final Type<SyncPools> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("commhub", "sync_pools"));
        public static final StreamCodec<ByteBuf, SyncPools> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SyncPools::energy,
                ByteBufCodecs.VAR_INT, SyncPools::fluidAmount,
                ByteBufCodecs.STRING_UTF8, SyncPools::fluidName,
                SyncPools::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RequestPrivateWarehouses(BlockPos pos) implements CustomPacketPayload {
        public static final Type<RequestPrivateWarehouses> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("commhub", "request_private_warehouses"));
        public static final StreamCodec<ByteBuf, RequestPrivateWarehouses> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, RequestPrivateWarehouses::pos,
                RequestPrivateWarehouses::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record PrivateWarehouseList(BlockPos pos, List<String> names) implements CustomPacketPayload {
        public static final Type<PrivateWarehouseList> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("commhub", "private_warehouse_list"));
        public static final StreamCodec<ByteBuf, PrivateWarehouseList> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, PrivateWarehouseList::pos,
                STRING_LIST_CODEC, PrivateWarehouseList::names,
                PrivateWarehouseList::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ReopenWarehouseTarget(BlockPos pos) implements CustomPacketPayload {
        public static final Type<ReopenWarehouseTarget> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("commhub", "reopen_warehouse_target"));
        public static final StreamCodec<ByteBuf, ReopenWarehouseTarget> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, ReopenWarehouseTarget::pos,
                ReopenWarehouseTarget::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record BindTools(int mode, String name) implements CustomPacketPayload {
        public static final Type<BindTools> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("commhub", "bind_tools"));
        public static final StreamCodec<ByteBuf, BindTools> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, BindTools::mode,
                ByteBufCodecs.STRING_UTF8, BindTools::name,
                BindTools::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ResetTools(int mode, String name) implements CustomPacketPayload {
        public static final Type<ResetTools> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("commhub", "reset_tools"));
        public static final StreamCodec<ByteBuf, ResetTools> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ResetTools::mode,
                ByteBufCodecs.STRING_UTF8, ResetTools::name,
                ResetTools::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RenameWarehouse(String oldName, String newName) implements CustomPacketPayload {
        public static final Type<RenameWarehouse> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("commhub", "rename_warehouse"));
        public static final StreamCodec<ByteBuf, RenameWarehouse> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, RenameWarehouse::oldName,
                ByteBufCodecs.STRING_UTF8, RenameWarehouse::newName,
                RenameWarehouse::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 调试用：允许接受自己的悬赏（OP 指令 /commhub selftrade 控制，默认关闭） */
    public static boolean ALLOW_SELF_TRADE = false;

    /** 成交动画通知：outgoing=true 表示「从我这送出去」，false 表示「送到我这」 */
    public record TradeEffect(boolean outgoing, String partner, ItemStack item, int total) implements CustomPacketPayload {
        public static final Type<TradeEffect> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("commhub", "trade_effect"));
        public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, TradeEffect> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, TradeEffect::outgoing,
                ByteBufCodecs.STRING_UTF8, TradeEffect::partner,
                ItemStack.OPTIONAL_STREAM_CODEC, TradeEffect::item,
                ByteBufCodecs.VAR_INT, TradeEffect::total,
                TradeEffect::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 保存机械手 / 动力锯卡片的配置（就写手里那张卡的 NBT） */
    public record SaveSimpleCard(int hand, net.minecraft.nbt.CompoundTag cfg) implements CustomPacketPayload {
        public static final Type<SaveSimpleCard> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("commhub", "save_simple_card"));
        public static final StreamCodec<ByteBuf, SaveSimpleCard> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SaveSimpleCard::hand,
                ByteBufCodecs.COMPOUND_TAG, SaveSimpleCard::cfg,
                SaveSimpleCard::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 保存合成升级卡的配置（mode 0=手里的卡，1=仓库卡槽里的卡） */
    public record SaveCardConfig(int mode, String name, int hand, int slot, net.minecraft.nbt.CompoundTag tag) implements CustomPacketPayload {
        public static final Type<SaveCardConfig> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("commhub", "save_card_config"));
        public static final StreamCodec<ByteBuf, SaveCardConfig> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SaveCardConfig::mode,
                ByteBufCodecs.STRING_UTF8, SaveCardConfig::name,
                ByteBufCodecs.VAR_INT, SaveCardConfig::hand,
                ByteBufCodecs.VAR_INT, SaveCardConfig::slot,
                ByteBufCodecs.COMPOUND_TAG, SaveCardConfig::tag,
                SaveCardConfig::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record CreateWarehouse(String name) implements CustomPacketPayload {
        public static final Type<CreateWarehouse> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("commhub", "create_warehouse"));
        public static final StreamCodec<ByteBuf, CreateWarehouse> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, CreateWarehouse::name,
                CreateWarehouse::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ChatSync(List<ChatEntry> publicLog, List<ChatEntry> privateLog, List<PlayerInfo> players, List<String> warehouses, List<UUID> friends, List<UUID> incoming) implements CustomPacketPayload {
        public static final Type<ChatSync> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("commhub", "chat_sync"));
        public static final StreamCodec<ByteBuf, ChatSync> STREAM_CODEC = StreamCodec.composite(
                CHAT_LIST_CODEC, ChatSync::publicLog,
                CHAT_LIST_CODEC, ChatSync::privateLog,
                PLAYER_LIST_CODEC, ChatSync::players,
                STRING_LIST_CODEC, ChatSync::warehouses,
                UUID_LIST_CODEC, ChatSync::friends,
                UUID_LIST_CODEC, ChatSync::incoming,
                ChatSync::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ChatPush(boolean isPublic, UUID sender, String senderName, UUID target, String message, long timestamp) implements CustomPacketPayload {
        public static final Type<ChatPush> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("commhub", "chat_push"));
        public static final StreamCodec<ByteBuf, ChatPush> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, ChatPush::isPublic,
                UUID_CODEC, ChatPush::sender,
                ByteBufCodecs.STRING_UTF8, ChatPush::senderName,
                NULLABLE_UUID_CODEC, ChatPush::target,
                ByteBufCodecs.STRING_UTF8, ChatPush::message,
                ByteBufCodecs.VAR_LONG, ChatPush::timestamp,
                ChatPush::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModNetworking::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(OpenHub.TYPE, OpenHub.STREAM_CODEC, ModNetworking::handleOpenHub);
        registrar.playToServer(SendChat.TYPE, SendChat.STREAM_CODEC, ModNetworking::handleSendChat);
        registrar.playToServer(Transfer.TYPE, Transfer.STREAM_CODEC, ModNetworking::handleTransfer);
        registrar.playToServer(ClaimMailbox.TYPE, ClaimMailbox.STREAM_CODEC, ModNetworking::handleClaimMailbox);
        registrar.playToServer(SetWarehouseTarget.TYPE, SetWarehouseTarget.STREAM_CODEC, ModNetworking::handleSetWarehouseTarget);
        registrar.playToServer(RequestPrivateWarehouses.TYPE, RequestPrivateWarehouses.STREAM_CODEC, ModNetworking::handleRequestPrivateWarehouses);
        registrar.playToServer(ReopenWarehouseTarget.TYPE, ReopenWarehouseTarget.STREAM_CODEC, ModNetworking::handleReopenWarehouseTarget);
        registrar.playToServer(CreateTrade.TYPE, CreateTrade.STREAM_CODEC, ModNetworking::handleCreateTrade);
        registrar.playToServer(AcceptTrade.TYPE, AcceptTrade.STREAM_CODEC, ModNetworking::handleAcceptTrade);
        registrar.playToServer(CancelTrade.TYPE, CancelTrade.STREAM_CODEC, ModNetworking::handleCancelTrade);
        registrar.playToServer(OpenTrade.TYPE, OpenTrade.STREAM_CODEC, ModNetworking::handleOpenTrade);
        registrar.playToServer(FriendAction.TYPE, FriendAction.STREAM_CODEC, ModNetworking::handleFriendAction);
        registrar.playToServer(ChangeView.TYPE, ChangeView.STREAM_CODEC, ModNetworking::handleChangeView);
        registrar.playToServer(CreateWarehouse.TYPE, CreateWarehouse.STREAM_CODEC, ModNetworking::handleCreateWarehouse);
        registrar.playToServer(RenameWarehouse.TYPE, RenameWarehouse.STREAM_CODEC, ModNetworking::handleRenameWarehouse);
        registrar.playToServer(SaveCardConfig.TYPE, SaveCardConfig.STREAM_CODEC, ModNetworking::handleSaveCardConfig);
        registrar.playToServer(SaveSimpleCard.TYPE, SaveSimpleCard.STREAM_CODEC, ModNetworking::handleSaveSimpleCard);
        registrar.playToClient(TradeEffect.TYPE, TradeEffect.STREAM_CODEC, ModNetworking::handleTradeEffect);
        registrar.playToServer(ResetTools.TYPE, ResetTools.STREAM_CODEC, ModNetworking::handleResetTools);
        registrar.playToServer(BindTools.TYPE, BindTools.STREAM_CODEC, ModNetworking::handleBindTools);
        registrar.playToClient(ChatSync.TYPE, ChatSync.STREAM_CODEC, ModNetworking::handleChatSync);
        registrar.playToClient(ChatPush.TYPE, ChatPush.STREAM_CODEC, ModNetworking::handleChatPush);
        registrar.playToClient(OpenCreateDialog.TYPE, OpenCreateDialog.STREAM_CODEC, ModNetworking::handleOpenCreateDialog);
        registrar.playToClient(SyncTrades.TYPE, SyncTrades.STREAM_CODEC, ModNetworking::handleSyncTrades);
        registrar.playToClient(PublishResult.TYPE, PublishResult.STREAM_CODEC, ModNetworking::handlePublishResult);
        registrar.playToClient(SyncPools.TYPE, SyncPools.STREAM_CODEC, ModNetworking::handleSyncPools);
        registrar.playToClient(PrivateWarehouseList.TYPE, PrivateWarehouseList.STREAM_CODEC, ModNetworking::handlePrivateWarehouseList);
    }

    /**
     * 管理工具格（3 格）。
     * 关键：必须完整实现 IItemHandler 的读写/插入/抽取，且都作用在同一个 slots 数组上；
     * 否则 SlotItemHandler 默认的 extractItem 会去操作 ItemStackHandler 自己的内部数组，
     * 导致"工具拿不走"。
     */
    /** 合成升级卡卡槽（1 格） */
    private static IItemHandler cardHandlerFor(HubSavedData data, int mode, String name) {
        boolean shared = (mode != HubMenu.MODE_PRIVATE);
        ItemStack[] slots = data.getCardSlots(shared, shared ? "" : name);
        if (slots == null) return new net.neoforged.neoforge.items.ItemStackHandler(1);
        return new CardSlotHandler(slots, data);
    }

    /** 只放合成升级卡、每格 1 张 */
    private static final class CardSlotHandler implements net.neoforged.neoforge.items.IItemHandlerModifiable {
        private final ItemStack[] slots;
        private final HubSavedData data;

        CardSlotHandler(ItemStack[] slots, HubSavedData data) {
            this.slots = slots;
            this.data = data;
        }

        @Override public int getSlots() { return slots.length; }

        private boolean valid(int slot) { return slot >= 0 && slot < slots.length; }

        @Override public ItemStack getStackInSlot(int slot) {
            if (!valid(slot)) return ItemStack.EMPTY;
            ItemStack st = slots[slot];
            return st == null ? ItemStack.EMPTY : st;
        }

        @Override public void setStackInSlot(int slot, ItemStack stack) {
            if (!valid(slot)) return;
            slots[slot] = (stack == null || stack.isEmpty()) ? ItemStack.EMPTY : stack.copyWithCount(1);
            data.setDirty();
        }

        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!valid(slot)) return stack;
            if (!com.commhub.SimpleCard.isWarehouseCard(stack)) return stack;
            if (!getStackInSlot(slot).isEmpty()) return stack;
            if (simulate) return ItemStack.EMPTY;
            slots[slot] = stack.copyWithCount(1);
            data.setDirty();
            return stack.getCount() > 1 ? stack.copyWithCount(stack.getCount() - 1) : ItemStack.EMPTY;
        }

        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!valid(slot) || amount <= 0) return ItemStack.EMPTY;
            ItemStack cur = getStackInSlot(slot);
            if (cur.isEmpty()) return ItemStack.EMPTY;
            if (!simulate) {
                slots[slot] = ItemStack.EMPTY;
                data.setDirty();
            }
            return cur.copyWithCount(1);
        }

        @Override public int getSlotLimit(int slot) { return 1; }

        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return com.commhub.SimpleCard.isWarehouseCard(stack);
        }
    }

    private static net.neoforged.neoforge.items.IItemHandler toolHandlerFor(HubSavedData data, int mode, String name) {
        boolean shared = mode != HubMenu.MODE_PRIVATE;
        ItemStack[] slots = data.getToolSlots(shared, name);
        if (slots == null) {
            return new EmptyToolHandler();
        }
        return new ArrayToolHandler(slots, data);
    }

    /** 仅用于"仓库不存在"的兜底：永远为空、不可写入 */
    private static final class EmptyToolHandler implements net.neoforged.neoforge.items.IItemHandlerModifiable {
        @Override public int getSlots() { return 3; }

        /** 兜底 handler 也要实现，否则 SlotItemHandler.set() 的 checkcast 会抛异常 */
        @Override public void setStackInSlot(int slot, ItemStack stack) { }

        @Override public ItemStack getStackInSlot(int slot) { return ItemStack.EMPTY; }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return stack; }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return 0; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return false; }
    }

    /** 直接读写仓库工具栏数组（3 格，每格 1 把） */
    private static final class ArrayToolHandler implements net.neoforged.neoforge.items.IItemHandlerModifiable {
        private final ItemStack[] slots;
        private final HubSavedData data;

        ArrayToolHandler(ItemStack[] slots, HubSavedData data) {
            this.slots = slots;
            this.data = data;
        }

        @Override public int getSlots() { return 3; }

        /** SlotItemHandler.set() 需要这个（checkcast IItemHandlerModifiable），否则 ClassCastException 会把工具弄丢 */
        @Override public void setStackInSlot(int slot, ItemStack stack) {
            if (slot < 0 || slot >= 3) return;
            if (stack == null || stack.isEmpty()) {
                slots[slot] = ItemStack.EMPTY;
            } else {
                slots[slot] = stack.copyWithCount(1); // 工具每格 1 把
            }
            data.setDirty();
        }

        @Override public ItemStack getStackInSlot(int slot) {
            if (slot < 0 || slot >= 3) return ItemStack.EMPTY;
            ItemStack st = slots[slot];
            return st == null ? ItemStack.EMPTY : st;
        }

        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            // 只接受管理工具，且每格 1 把；放好后点「绑定」按钮绑定到本仓库
            if (!HubMenu.isManagementTool(stack)) return stack;
            if (slot < 0 || slot >= 3) return stack;
            if (!getStackInSlot(slot).isEmpty()) return stack;
            if (simulate) return ItemStack.EMPTY;
            slots[slot] = stack.copyWithCount(1);
            data.setDirty();
            return stack.getCount() > 1 ? stack.copyWithCount(stack.getCount() - 1) : ItemStack.EMPTY;
        }

        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < 0 || slot >= 3 || amount <= 0) return ItemStack.EMPTY;
            ItemStack cur = getStackInSlot(slot);
            if (cur.isEmpty()) return ItemStack.EMPTY;
            int take = Math.min(amount, cur.getCount());
            ItemStack out = cur.copyWithCount(take);
            if (!simulate) {
                ItemStack rest = cur.copyWithCount(cur.getCount() - take);
                slots[slot] = rest.isEmpty() ? ItemStack.EMPTY : rest;
                data.setDirty();
            }
            return out;
        }

        @Override public int getSlotLimit(int slot) { return 1; }

        @Override public boolean isItemValid(int slot, ItemStack stack) { return HubMenu.isManagementTool(stack); }
    }

    private static void handleOpenHub(OpenHub payload, IPayloadContext ctx) {
        ServerPlayer player = (ServerPlayer) ctx.player();
        HubSavedData data = HubSavedData.get(player.getServer());
        java.util.List<String> mine = data.accessiblePrivateWarehouses(player.getUUID());
        boolean sharedOn = com.commhub.ModConfig.sharedWarehouseEnabled();
        int want = payload.mode();
        String wantName = payload.name() == null ? "" : payload.name();

        // ① 能回到「上次看的标签页」就回去
        if (want == HubMenu.MODE_PRIVATE && !mine.isEmpty()) {
            String target = (!wantName.isEmpty() && mine.contains(wantName)) ? wantName : mine.get(0);
            handleChangeView(new ChangeView(HubMenu.MODE_PRIVATE, 0, target), ctx);
            return;
        }
        if (want == HubMenu.MODE_CHAT || want == HubMenu.MODE_TRANSFER) {
            handleChangeView(new ChangeView(want, 0, ""), ctx);
            return;
        }
        if (want == HubMenu.MODE_SHARED && sharedOn) {
            openSharedHub(player, data);
            return;
        }

        // ② 兜底：按「能用哪个用哪个」挑一个页面 ——
        //    重点：**没有私人仓库也要能开界面**（邮箱 / 以物换物 / 私聊都是免费的）
        if (!mine.isEmpty()) {
            handleChangeView(new ChangeView(HubMenu.MODE_PRIVATE, 0, mine.get(0)), ctx);
            return;
        }
        if (sharedOn) {
            openSharedHub(player, data);
            return;
        }
        // 共享关了、又没有私人仓库 → 开「传物品」页（邮箱是免费的），而不是把界面锁死
        handleChangeView(new ChangeView(HubMenu.MODE_TRANSFER, 0, ""), ctx);
    }

    /** 打开「共享大仓库」页 */
    private static void openSharedHub(ServerPlayer player, HubSavedData data) {
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("container.commhub.hub");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return new HubMenu(id, inv, data.getWarehouse(), data.getMailbox(p.getUUID()),
                        toolHandlerFor(data, HubMenu.MODE_SHARED, ""), cardHandlerFor(data, HubMenu.MODE_SHARED, ""),
                        data, HubMenu.MODE_SHARED, 0, "", null, null);
            }
        }, buf -> {
            buf.writeInt(HubMenu.MODE_SHARED);
            buf.writeInt(0);
            buf.writeUtf("");
            buf.writeBoolean(false);
            buf.writeBoolean(false);
            buf.writeInt(Math.max(1, data.getWarehouse().getPageCount()));
        });
        sendChatSync(player);
        sendPools(player, HubMenu.MODE_SHARED, "");
        broadcastTrades(player.getServer());
    }

    private static void sendChatSync(ServerPlayer player) {
        HubSavedData data = HubSavedData.get(player.getServer());
        List<PlayerInfo> players = new ArrayList<>();
        for (ServerPlayer p : player.getServer().getPlayerList().getPlayers()) {
            players.add(new PlayerInfo(p.getUUID(), p.getGameProfile().getName()));
        }
        PacketDistributor.sendToPlayer(player, new ChatSync(
                new ArrayList<>(data.getPublicLog()),
                new ArrayList<>(data.getPrivateLog()),
                players,
                data.accessiblePrivateWarehouses(player.getUUID()),
                data.getFriends(player.getUUID()),
                data.getIncomingRequests(player.getUUID())));
    }

    /** 按当前浏览的仓库发储量（共享 or 指定私人仓库） */
    private static void sendPools(ServerPlayer player, int mode, String name) {
        HubSavedData data = HubSavedData.get(player.getServer());
        boolean shared = mode != HubMenu.MODE_PRIVATE;
        int energy;
        net.neoforged.neoforge.fluids.FluidStack fluid;
        if (shared) {
            energy = data.getSharedEnergy();
            fluid = data.getSharedFluid();
        } else {
            HubSavedData.PrivateWarehouse w = data.getPrivateWarehouse(name);
            energy = w == null ? 0 : w.energy;
            fluid = w == null ? net.neoforged.neoforge.fluids.FluidStack.EMPTY : w.fluid;
        }
        PacketDistributor.sendToPlayer(player, new SyncPools(
                energy, fluid.getAmount(), fluid.isEmpty() ? "" : fluid.getDescriptionId()));
    }

    /** 私聊冷却（UUID -> 上次发送时间毫秒） */
    private static final java.util.Map<java.util.UUID, Long> CHAT_COOLDOWN = new java.util.HashMap<>();

    private static void handleSendChat(SendChat payload, IPayloadContext ctx) {
        ServerPlayer sender = (ServerPlayer) ctx.player();
        HubSavedData data = HubSavedData.get(sender.getServer());
        String name = sender.getGameProfile().getName();
        long ts = System.currentTimeMillis();

        // 长度限制：超过 256 字截断
        String msg = payload.message() == null ? "" : payload.message();
        if (msg.length() > 256) msg = msg.substring(0, 256);
        if (msg.isBlank()) return;
        // 频率限制：0.5 秒一条
        Long last = CHAT_COOLDOWN.get(sender.getUUID());
        if (last != null && ts - last < 500) {
            sender.sendSystemMessage(Component.literal("发太快了，稍等一下"));
            return;
        }
        CHAT_COOLDOWN.put(sender.getUUID(), ts);
        if (CHAT_COOLDOWN.size() > 256) CHAT_COOLDOWN.entrySet().removeIf(e -> ts - e.getValue() > 300_000L);

        if (payload.isPublic()) {
            ChatEntry entry = new ChatEntry(sender.getUUID(), name, null, msg, ts, true);
            data.addPublic(entry);
            PacketDistributor.sendToAllPlayers(new ChatPush(true, sender.getUUID(), name, null, msg, ts));
        } else {
            if (payload.target() == null) {
                sender.sendSystemMessage(Component.literal("私聊目标不能为空"));
                return;
            }
            boolean self = sender.getUUID().equals(payload.target());
            if (self && !com.commhub.ModConfig.allowSelfTest()) {
                sender.sendSystemMessage(Component.literal("不能给自己发私聊（测试可在配置里开 allow_self_test）"));
                return;
            }
            if (!self && !data.areFriends(sender.getUUID(), payload.target())) {
                sender.sendSystemMessage(Component.literal("你们还不是好友，先发好友请求并等对方同意"));
                return;
            }
            ChatEntry entry = new ChatEntry(sender.getUUID(), name, payload.target(), msg, ts, false);
            data.addPrivate(entry);
            ServerPlayer target = sender.getServer().getPlayerList().getPlayer(payload.target());
            ChatPush push = new ChatPush(false, sender.getUUID(), name, payload.target(), msg, ts);
            if (target != null) {
                PacketDistributor.sendToPlayer(target, push);
                target.sendSystemMessage(Component.literal("[私聊]" + name + " → 你: " + msg));
            }
            PacketDistributor.sendToPlayer(sender, push);
        }
    }

    private static void handleTransfer(Transfer payload, IPayloadContext ctx) {
        ServerPlayer sender = (ServerPlayer) ctx.player();
        if (sender.getUUID().equals(payload.target()) && !com.commhub.ModConfig.allowSelfTest()) return;
        if (!(sender.containerMenu instanceof HubMenu menu)) return;
        if (!HubSavedData.get(sender.getServer()).areFriends(sender.getUUID(), payload.target())) {
            sender.sendSystemMessage(Component.literal("你们还不是好友，先加好友才能传物品"));
            return;
        }

        HubSavedData data = HubSavedData.get(sender.getServer());
        com.commhub.InfiniteItemStackHandler targetMailbox = data.getMailbox(payload.target());
        IItemHandler send = menu.getSendSlots();
        boolean sent = false;

        for (int i = 0; i < send.getSlots(); i++) {
            ItemStack stack = send.extractItem(i, send.getStackInSlot(i).getCount(), false);
            if (!stack.isEmpty()) {
                ItemStack leftover = ItemHandlerHelper.insertItemStacked(targetMailbox, stack, false);
                if (!leftover.isEmpty()) {
                    // 邮箱满了：先放回发送格，还放不下就直接退回玩家背包（绝不吞物品）
                    ItemStack again = send.insertItem(i, leftover, false);
                    if (!again.isEmpty()) {
                        giveToPlayer(sender, again, data);
                    }
                }
                sent = true;
            }
        }

        if (sent) {
            data.setDirty();
            ServerPlayer target = sender.getServer().getPlayerList().getPlayer(payload.target());
            if (target != null) {
                target.sendSystemMessage(Component.literal("[" + sender.getGameProfile().getName() + "] 给你寄来了物品，按 K 打开界面 → 邮箱领取"));
            }
            sender.sendSystemMessage(Component.literal("物品已发送"));
        }
    }

    private static void handleSetWarehouseTarget(SetWarehouseTarget payload, IPayloadContext ctx) {
        ServerPlayer player = (ServerPlayer) ctx.player();
        // 距离校验：只能在方块旁边才能改目标
        double dx = payload.pos().getX() + 0.5 - player.getX();
        double dy = payload.pos().getY() + 0.5 - player.getY();
        double dz = payload.pos().getZ() + 0.5 - player.getZ();
        if (dx * dx + dy * dy + dz * dz > 64) {
            player.sendSystemMessage(Component.literal("离方块太远了，无法设置"));
            return;
        }
        if (player.level().getBlockEntity(payload.pos()) instanceof WarehouseBlockEntity be) {
            if (payload.target() == 0) {
                if (!com.commhub.ModConfig.sharedWarehouseEnabled()) {
                    player.sendSystemMessage(Component.literal("共享大仓库已在配置里关闭"));
                    return;
                }
                be.setTarget(0);
                be.setOwner(player.getUUID());
                be.setWarehouseName("");
            } else {
                HubSavedData data = HubSavedData.get(player.getServer());
                HubSavedData.PrivateWarehouse w = data.getPrivateWarehouse(payload.name());
                if (w == null || !data.canAccessPrivateWarehouse(w, player.getUUID())) {
                    player.sendSystemMessage(Component.literal("没有这个仓库，或你没有权限"));
                    return;
                }
                be.setTarget(1);
                be.setOwner(player.getUUID());
                be.setWarehouseName(payload.name());
            }
        }
    }

    private static void handleRequestPrivateWarehouses(RequestPrivateWarehouses payload, IPayloadContext ctx) {
        ServerPlayer player = (ServerPlayer) ctx.player();
        // 距离校验：只能在方块旁边才能设置
        double dx = payload.pos().getX() + 0.5 - player.getX();
        double dy = payload.pos().getY() + 0.5 - player.getY();
        double dz = payload.pos().getZ() + 0.5 - player.getZ();
        if (dx * dx + dy * dy + dz * dz > 64) {
            player.sendSystemMessage(Component.literal("离方块太远了，无法设置"));
            return;
        }
        HubSavedData data = HubSavedData.get(player.getServer());
        PacketDistributor.sendToPlayer(player, new PrivateWarehouseList(
                payload.pos(), data.accessiblePrivateWarehouses(player.getUUID())));
    }

    private static void handleReopenWarehouseTarget(ReopenWarehouseTarget payload, IPayloadContext ctx) {
        ServerPlayer player = (ServerPlayer) ctx.player();
        // 距离校验：防止远程打开任意方块界面
        double dx = payload.pos().getX() + 0.5 - player.getX();
        double dy = payload.pos().getY() + 0.5 - player.getY();
        double dz = payload.pos().getZ() + 0.5 - player.getZ();
        if (dx * dx + dy * dy + dz * dz > 64) {
            player.sendSystemMessage(Component.literal("离方块太远了"));
            return;
        }
        if (player.level().getBlockEntity(payload.pos()) instanceof WarehouseBlockEntity be) {
            be.openTargetScreen(player);
        }
    }

    private static void handlePrivateWarehouseList(PrivateWarehouseList payload, IPayloadContext ctx) {
        com.commhub.ClientScreens.openWarehouseList(payload.pos(), payload.names());
    }

    private static void handleClaimMailbox(ClaimMailbox payload, IPayloadContext ctx) {
        ServerPlayer sender = (ServerPlayer) ctx.player();
        HubSavedData data = HubSavedData.get(sender.getServer());
        com.commhub.InfiniteItemStackHandler mailbox = data.getMailbox(sender.getUUID());
        boolean claimed = false;
        boolean overflow = false;

        for (int i = 0; i < mailbox.getSlots(); i++) {
            ItemStack stack = mailbox.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            // 邮箱是无限堆叠的，必须按 64 拆开塞进背包，装不下的留回邮箱
            ItemStack remaining = stack.copy();
            while (!remaining.isEmpty()) {
                int take = Math.min(remaining.getCount(), 64);
                ItemStack one = remaining.copyWithCount(take);
                ItemStack left = insertToInventory(sender, one);
                if (!left.isEmpty()) {
                    // 背包满了，剩下的放回邮箱
                    mailbox.setStackInSlot(i, remaining);
                    overflow = true;
                    break;
                }
                remaining.shrink(take);
            }
            if (remaining.isEmpty()) mailbox.setStackInSlot(i, ItemStack.EMPTY);
            else mailbox.setStackInSlot(i, remaining);
            claimed = true;
        }

        if (claimed) {
            data.setDirty();
            sender.sendSystemMessage(Component.literal(overflow
                    ? "已领取邮箱物品（背包满了，部分还在邮箱里）"
                    : "邮箱物品已领取"));
        }
    }

    /** 把物品塞进玩家背包（返回装不下的剩余，空=全塞下） */
    private static ItemStack insertToInventory(ServerPlayer player, ItemStack stack) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (slot.isEmpty()) {
                player.getInventory().setItem(i, stack);
                return ItemStack.EMPTY;
            }
            if (ItemStack.isSameItemSameComponents(slot, stack) && slot.getCount() < slot.getMaxStackSize()) {
                int put = Math.min(slot.getMaxStackSize() - slot.getCount(), stack.getCount());
                slot.grow(put);
                stack.shrink(put);
                if (stack.isEmpty()) return ItemStack.EMPTY;
            }
        }
        return stack;
    }

    private static void handleChangeView(ChangeView payload, IPayloadContext ctx) {
        ServerPlayer player = (ServerPlayer) ctx.player();
        HubSavedData data = HubSavedData.get(player.getServer());
        int mode = payload.mode();
        if (mode == HubMenu.MODE_SHARED && !com.commhub.ModConfig.sharedWarehouseEnabled()) {
            player.sendSystemMessage(Component.literal("共享大仓库已在配置里关闭"));
            return;
        }
        final String name;
        final net.neoforged.neoforge.items.IItemHandler wh;
        if (mode == HubMenu.MODE_PRIVATE) {
            String req = payload.name();
            if (req == null || req.isEmpty()) {
                java.util.List<String> list = data.accessiblePrivateWarehouses(player.getUUID());
                if (list.isEmpty()) {
                    player.sendSystemMessage(Component.literal("你还没有私人仓库，先用「管理员凭证」右键创建一个"));
                    return;
                }
                name = list.get(0);
            } else {
                name = req;
            }
            HubSavedData.PrivateWarehouse w = data.getPrivateWarehouse(name);
            if (w == null || !data.canAccessPrivateWarehouse(w, player.getUUID())) {
                player.sendSystemMessage(Component.literal("没有这个仓库，或你没有权限"));
                return;
            }
            wh = w.storage;
        } else {
            name = "";
            wh = data.getWarehouse();
        }
        // 页数按分页仓库实际页数算；请求的页超出就自动扩页（分页存储，空页存档时丢弃）
        int realPages = Math.max(1, wh.getSlots() / HubMenu.WAREHOUSE_PER_PAGE);
        int want = Math.max(0, payload.page());
        if (want >= realPages && wh instanceof com.commhub.PagedItemHandler ph && want + 1 <= HubMenu.MAX_PAGES) {
            ph.ensurePages(want + 1);
            realPages = ph.getPageCount();
            data.setDirty();
        }
        int maxPages = Math.max(1, Math.min(realPages, HubMenu.MAX_PAGES));
        final int page = Math.floorMod(want, maxPages);
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("container.commhub.hub");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                HubSavedData.PrivateWarehouse pw = data.getPrivateWarehouse(name);
                return new HubMenu(id, inv, wh, data.getMailbox(p.getUUID()),
                        toolHandlerFor(data, mode, name), cardHandlerFor(data, mode, name), data, mode, page, name,
                        pw == null ? null : pw.owner, pw == null ? null : pw.admin);
            }
        }, buf -> {
            HubSavedData.PrivateWarehouse pw = data.getPrivateWarehouse(name);
            buf.writeInt(mode);
            buf.writeInt(page);
            buf.writeUtf(name);
            buf.writeBoolean(pw != null && pw.owner != null); if (pw != null && pw.owner != null) buf.writeUUID(pw.owner);
            buf.writeBoolean(pw != null && pw.admin != null); if (pw != null && pw.admin != null) buf.writeUUID(pw.admin);
            buf.writeInt(Math.max(1, wh.getSlots() / HubMenu.WAREHOUSE_PER_PAGE));
        });
        sendChatSync(player);
        sendPools(player, mode, name);
    }

    /** 好友操作冷却（防刷屏） */
    private static final java.util.Map<java.util.UUID, Long> FRIEND_COOLDOWN = new java.util.HashMap<>();

    private static void handleFriendAction(FriendAction payload, IPayloadContext ctx) {
        ServerPlayer self = (ServerPlayer) ctx.player();
        HubSavedData data = HubSavedData.get(self.getServer());
        ServerPlayer target = self.getServer().getPlayerList().getPlayer(payload.target());
        String selfName = self.getGameProfile().getName();
        // 冷却 1 秒，防止狂点刷屏（每次都会全量同步双方）
        long now = System.currentTimeMillis();
        Long last = FRIEND_COOLDOWN.get(self.getUUID());
        if (last != null && now - last < 1000) return;
        FRIEND_COOLDOWN.put(self.getUUID(), now);
        if (FRIEND_COOLDOWN.size() > 256) FRIEND_COOLDOWN.entrySet().removeIf(e -> now - e.getValue() > 300_000L);
        if (payload.target().equals(self.getUUID()) && !com.commhub.ModConfig.allowSelfTest()) {
            self.sendSystemMessage(Component.literal("不能对自己操作（测试可在配置里开 allow_self_test）"));
            return;
        }
        if (payload.action() == 0) {
            if (data.areFriends(self.getUUID(), payload.target())) {
                self.sendSystemMessage(Component.literal("你们已经是好友了"));
                return;
            }
            data.addFriendRequest(self.getUUID(), payload.target());
            self.sendSystemMessage(Component.literal("已发送好友请求"));
            if (target != null) {
                target.sendSystemMessage(Component.literal(selfName + " 想加你为好友（按 K → 私聊 → 同意）"));
            }
        } else if (payload.action() == 1) {
            data.acceptFriendRequest(payload.target(), self.getUUID());
            self.sendSystemMessage(Component.literal("已同意好友请求，现在可以私聊和传物品了"));
            if (target != null) {
                target.sendSystemMessage(Component.literal(selfName + " 已同意你的好友请求"));
            }
        } else if (payload.action() == 2) {
            data.removeFriend(self.getUUID(), payload.target());
            self.sendSystemMessage(Component.literal("已删除好友"));
        } else if (payload.action() == 3) {
            data.rejectFriendRequest(payload.target(), self.getUUID());
            self.sendSystemMessage(Component.literal("已拒绝好友请求"));
        }
        sendChatSync(self);
        if (target != null) sendChatSync(target);
    }

    private static void handleOpenTrade(OpenTrade payload, IPayloadContext ctx) {
        ServerPlayer player = (ServerPlayer) ctx.player();
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("以物换物");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return new com.commhub.TradeMenu(id, inv, payload.mode());
            }
        }, buf -> buf.writeInt(payload.mode()));
        broadcastTrades(player.getServer());
    }

    private static void handleCreateTrade(CreateTrade payload, IPayloadContext ctx) {
        ServerPlayer player = (ServerPlayer) ctx.player();
        List<ItemStack> offer = new ArrayList<>();
        List<ItemStack> want = new ArrayList<>();
        for (String st : payload.offer()) {
            ItemStack s = parseFilter(st);
            if (!s.isEmpty()) offer.add(s);
        }
        for (String st : payload.want()) {
            ItemStack s = parseFilter(st);
            if (!s.isEmpty()) want.add(s);
        }
        if (offer.isEmpty() || want.isEmpty()) {
            player.sendSystemMessage(Component.literal("「我给」和「我要」都要至少设置一样东西"));
            PacketDistributor.sendToPlayer(player, new PublishResult(false));
            return;
        }
        HubSavedData data = HubSavedData.get(player.getServer());
        // 限制：每人最多 5 条悬赏，防止刷屏
        int mine = 0;
        for (Trade t : data.getTrades()) {
            if (t.owner().equals(player.getUUID())) mine++;
        }
        if (mine >= 5) {
            player.sendSystemMessage(Component.literal("你已有 5 条悬赏，先取消一些再发布"));
            PacketDistributor.sendToPlayer(player, new PublishResult(false));
            return;
        }
        // 限制：内容完全相同的悬赏不重复发布
        for (Trade t : data.getTrades()) {
            if (t.owner().equals(player.getUUID()) && sameFilter(t.offer(), offer) && sameFilter(t.want(), want)) {
                player.sendSystemMessage(Component.literal("你已经发布过一模一样的悬赏了"));
                PacketDistributor.sendToPlayer(player, new PublishResult(false));
                return;
            }
        }
        // 发布悬赏不扣任何物品：「我给/我要」只是声明，成交时才从双方背包扣/给
        data.addTrade(new Trade(UUID.randomUUID(), player.getUUID(), player.getGameProfile().getName(), offer, want));
        broadcastTrades(player.getServer());
        player.sendSystemMessage(Component.literal("悬赏已发布"));
        PacketDistributor.sendToPlayer(player, new PublishResult(true));
    }

    private static void handleAcceptTrade(AcceptTrade payload, IPayloadContext ctx) {
        ServerPlayer player = (ServerPlayer) ctx.player();
        HubSavedData data = HubSavedData.get(player.getServer());
        Trade trade = null;
        for (Trade t : data.getTrades()) if (t.id().equals(payload.id())) { trade = t; break; }
        if (trade == null) return;
        if (trade.owner().equals(player.getUUID()) && !ALLOW_SELF_TRADE) {
            player.sendSystemMessage(Component.literal("不能接受自己的悬赏（调试可用 /commhub selftrade true 关掉这个限制）"));
            return;
        }
        // 发布者必须在线（他的「我给」在背包里）
        ServerPlayer owner = player.getServer().getPlayerList().getPlayer(trade.owner());
        if (owner == null) {
            player.sendSystemMessage(Component.literal("发布者不在线，无法兑换"));
            return;
        }
        // 检查双方都有货（背包+私人仓库）
        for (ItemStack o : trade.offer()) {
            if (!o.isEmpty() && countAvailable(owner, o.getItem(), data) < o.getCount()) {
                player.sendSystemMessage(Component.literal("发布者背包/仓库里没有足够的物品"));
                return;
            }
        }
        for (ItemStack w : trade.want()) {
            if (!w.isEmpty() && countAvailable(player, w.getItem(), data) < w.getCount()) {
                player.sendSystemMessage(Component.literal("你没有足够的物品来兑换"));
                return;
            }
        }
        // 先扣发布者的「我给」：扣不满就中止（此时还没扣接受者，可安全回滚）
        List<ItemStack> takenFromOwner = new ArrayList<>();
        for (ItemStack o : trade.offer()) {
            if (o.isEmpty()) continue;
            int got = removeAvailable(owner, o, data);
            takenFromOwner.add(o.copyWithCount(got));
            if (got < o.getCount()) {
                // 回滚已扣的部分
                for (ItemStack back : takenFromOwner) {
                    if (!back.isEmpty()) giveToPlayer(owner, back.copy(), data);
                }
                player.sendSystemMessage(Component.literal("§c发布者物品不足，兑换已取消（已把扣掉的物品还回去）"));
                return;
            }
        }
        // 再扣接受者的「我要」：扣不满就回滚发布者
        List<ItemStack> takenFromPlayer = new ArrayList<>();
        for (ItemStack w : trade.want()) {
            if (w.isEmpty()) continue;
            int got = removeAvailable(player, w, data);
            takenFromPlayer.add(w.copyWithCount(got));
            if (got < w.getCount()) {
                for (ItemStack back : takenFromOwner) {
                    if (!back.isEmpty()) giveToPlayer(owner, back.copy(), data);
                }
                for (ItemStack back : takenFromPlayer) {
                    if (!back.isEmpty()) giveToPlayer(player, back.copy(), data);
                }
                player.sendSystemMessage(Component.literal("§c你的物品不足，兑换已取消（双方扣掉的物品都已还回去）"));
                return;
            }
        }
        for (ItemStack o : trade.offer()) {
            if (!o.isEmpty()) giveToPlayer(player, o.copy(), data);
        }
        com.commhub.InfiniteItemStackHandler ownerMailbox = data.getMailbox(trade.owner());
        for (ItemStack w : trade.want()) {
            if (w.isEmpty()) continue;
            ItemStack leftover = ItemHandlerHelper.insertItemStacked(ownerMailbox, w.copy(), false);
            if (!leftover.isEmpty()) giveToPlayer(player, leftover, data);
        }
        // ---- 成交动画：双方各两段（先送出去，再收进来）----
        ItemStack fxOffer = ItemStack.EMPTY, fxWant = ItemStack.EMPTY;
        int fxOfferCount = 0, fxWantCount = 0;
        for (ItemStack st : trade.offer()) {
            if (st.isEmpty()) continue;
            if (fxOffer.isEmpty()) fxOffer = st;
            fxOfferCount += st.getCount();
        }
        for (ItemStack st : trade.want()) {
            if (st.isEmpty()) continue;
            if (fxWant.isEmpty()) fxWant = st;
            fxWantCount += st.getCount();
        }
        String accepterName = player.getGameProfile().getName();
        PacketDistributor.sendToPlayer(player, new TradeEffect(true, owner.getGameProfile().getName(), fxWant, fxWantCount));
        PacketDistributor.sendToPlayer(player, new TradeEffect(false, owner.getGameProfile().getName(), fxOffer, fxOfferCount));
        PacketDistributor.sendToPlayer(owner, new TradeEffect(true, accepterName, fxOffer, fxOfferCount));
        PacketDistributor.sendToPlayer(owner, new TradeEffect(false, accepterName, fxWant, fxWantCount));

        data.removeTrade(trade.id());
        broadcastTrades(player.getServer());
        if (owner != null) owner.sendSystemMessage(Component.literal(player.getGameProfile().getName() + " 接受了你的悬赏，物品已放入你的邮箱"));
        player.sendSystemMessage(Component.literal("兑换成功"));
    }

    /**
     * 真正执行一次交换（双方物品都已够）。抽出来让「玩家点接受」和「测试假人」走同一套逻辑。
     */
    public static boolean performTrade(ServerPlayer accepter, ServerPlayer owner, Trade trade, HubSavedData data) {
        List<ItemStack> takenFromOwner = new ArrayList<>();
        for (ItemStack o : trade.offer()) {
            if (o.isEmpty()) continue;
            int got = removeAvailable(owner, o, data);
            takenFromOwner.add(o.copyWithCount(got));
            if (got < o.getCount()) {
                for (ItemStack back : takenFromOwner) if (!back.isEmpty()) giveToPlayer(owner, back.copy(), data);
                accepter.sendSystemMessage(Component.literal("§c发布者物品不足，兑换取消（已归还）"));
                return false;
            }
        }
        List<ItemStack> takenFromAccepter = new ArrayList<>();
        for (ItemStack w : trade.want()) {
            if (w.isEmpty()) continue;
            int got = removeAvailable(accepter, w, data);
            takenFromAccepter.add(w.copyWithCount(got));
            if (got < w.getCount()) {
                for (ItemStack back : takenFromOwner) if (!back.isEmpty()) giveToPlayer(owner, back.copy(), data);
                for (ItemStack back : takenFromAccepter) if (!back.isEmpty()) giveToPlayer(accepter, back.copy(), data);
                accepter.sendSystemMessage(Component.literal("§c你的物品不足，兑换取消（双方都已归还）"));
                return false;
            }
        }
        for (ItemStack o : trade.offer()) if (!o.isEmpty()) giveToPlayer(accepter, o.copy(), data);
        var ownerBox = data.getMailbox(trade.owner());
        for (ItemStack w : trade.want()) {
            if (w.isEmpty()) continue;
            ItemStack leftover = ItemHandlerHelper.insertItemStacked(ownerBox, w.copy(), false);
            if (!leftover.isEmpty()) giveToPlayer(accepter, leftover, data);
        }
        ItemStack fxOffer = ItemStack.EMPTY, fxWant = ItemStack.EMPTY;
        int cOffer = 0, cWant = 0;
        for (ItemStack st : trade.offer()) { if (st.isEmpty()) continue; if (fxOffer.isEmpty()) fxOffer = st; cOffer += st.getCount(); }
        for (ItemStack st : trade.want()) { if (st.isEmpty()) continue; if (fxWant.isEmpty()) fxWant = st; cWant += st.getCount(); }
        String ownerName = owner.getGameProfile().getName();
        String accepterName = accepter.getGameProfile().getName();
        // **真正使用时也走 3D 实体动画**（和 /commhub rift 同一套）：
        // 每个人面前开一个裂缝，把自己收到的东西从裂缝里送出来
        spawnRiftFor(owner, fxWant, namesOf(trade.want()));
        spawnRiftFor(accepter, fxOffer, namesOf(trade.offer()));
        data.removeTrade(trade.id());
        broadcastTrades(accepter.getServer());
        owner.sendSystemMessage(Component.literal(accepterName + " 接受了你的悬赏"));
        accepter.sendSystemMessage(Component.literal("兑换成功"));
        return true;
    }

    /** 在玩家面前开一个裂缝，包裹里是 items（真正成交时用） */
    private static void spawnRiftFor(ServerPlayer p, ItemStack fx, String contents) {
        try {
            var look = p.getLookAngle();
            double x = p.getX() + look.x * 2.5;
            double y = p.getY();
            double z = p.getZ() + look.z * 2.5;
            com.commhub.RiftEntity e = com.commhub.RiftEntity.spawn(p.serverLevel(), x, y, z, false,
                    fx == null ? ItemStack.EMPTY : fx.copyWithCount(1),
                    fx == null ? 1 : Math.max(1, fx.getCount()), false, contents);
            e.setYRot(p.getYRot() + 180f);
            e.setYHeadRot(e.getYRot());
        } catch (Throwable ignored) {
        }
    }

    /** 物品名列表（给裂缝上方的提示用，多个用逗号隔开） */
    private static String namesOf(List<ItemStack> list) {
        StringBuilder sb = new StringBuilder();
        for (ItemStack st : list) {
            if (st == null || st.isEmpty()) continue;
            if (sb.length() > 0) sb.append(",");
            sb.append(st.getHoverName().getString());
        }
        return sb.toString();
    }

    /** 测试用：召唤一个假人来接受我最新发布的悬赏（OP 指令 /commhub faketrade） */
    public static boolean fakeAcceptsMyLatest(ServerPlayer me) {
        HubSavedData data = HubSavedData.get(me.getServer());
        Trade mine = null;
        for (Trade t : data.getTrades()) if (t.owner().equals(me.getUUID())) mine = t;
        if (mine == null) {
            me.sendSystemMessage(Component.literal("§c你还没有发布悬赏，先去「以物换物」发一条"));
            return false;
        }
        net.neoforged.neoforge.common.util.FakePlayer fake =
                net.neoforged.neoforge.common.util.FakePlayerFactory.get(
                        me.serverLevel(),
                        new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "测试假人"));
        // 假人「凭空」备齐它要付的东西（测试专用）
        for (ItemStack w : mine.want()) {
            if (w.isEmpty()) continue;
            fake.getInventory().add(w.copy());
        }
        boolean ok = performTrade(fake, me, mine, data);
        me.sendSystemMessage(Component.literal(ok
                ? "§a假人接受了你的悬赏，注意看空间裂缝动画"
                : "§c假人交换失败（可能你的物品不够）"));
        return ok;
    }

    private static void handleCancelTrade(CancelTrade payload, IPayloadContext ctx) {
        ServerPlayer player = (ServerPlayer) ctx.player();
        HubSavedData data = HubSavedData.get(player.getServer());
        Trade trade = null;
        for (Trade t : data.getTrades()) if (t.id().equals(payload.id())) { trade = t; break; }
        if (trade == null) return;
        if (!trade.owner().equals(player.getUUID()) && !data.isAdmin(player.getUUID())) return;
        data.removeTrade(trade.id());
        broadcastTrades(player.getServer());
        player.sendSystemMessage(Component.literal("已取消悬赏"));
    }

    private static void broadcastTrades(MinecraftServer server) {
        HubSavedData data = HubSavedData.get(server);
        List<TradeInfo> infos = new ArrayList<>();
        for (Trade t : data.getTrades()) {
            infos.add(new TradeInfo(t.id(), t.owner(), t.ownerName(),
                    stackListToString(t.offer()),
                    stackListToString(t.want())));
        }
        PacketDistributor.sendToAllPlayers(new SyncTrades(infos, ALLOW_SELF_TRADE));
    }

    private static void giveToPlayer(ServerPlayer player, ItemStack stack, HubSavedData data) {
        if (stack.isEmpty()) return;
        ItemStack leftover = stack.copy();
        player.getInventory().add(leftover);
        if (!leftover.isEmpty()) {
            ItemStack leftover2 = ItemHandlerHelper.insertItemStacked(data.getMailbox(player.getUUID()), leftover, false);
            if (!leftover2.isEmpty()) {
                // 背包和邮箱都满：丢到地上，绝不吞物品
                player.drop(leftover2, false, true);
            }
        }
    }

    /** 统计玩家在背包+自己私人仓库里有多少某物品 */
    private static int countAvailable(ServerPlayer p, net.minecraft.world.item.Item item, HubSavedData data) {
        int total = p.getInventory().countItem(item);
        for (String name : data.accessiblePrivateWarehouses(p.getUUID())) {
            HubSavedData.PrivateWarehouse w = data.getPrivateWarehouse(name);
            if (w != null) {
                for (int i = 0; i < w.storage.getSlots(); i++) {
                    ItemStack s = w.storage.getStackInSlot(i);
                    if (s.getItem() == item) total += s.getCount();
                }
            }
        }
        return total;
    }

    /** 从背包+私人仓库里扣除指定数量（背包优先） */
    /** 从背包+私人仓库扣物品，返回【实际扣除的数量】 */
    private static int removeAvailable(ServerPlayer p, ItemStack stack, HubSavedData data) {
        int removed = 0;
        ItemStack remaining = stack.copy();
        for (int i = 0; i < p.getInventory().getContainerSize() && !remaining.isEmpty(); i++) {
            ItemStack slot = p.getInventory().getItem(i);
            if (slot.getItem() == stack.getItem()) {
                int take = Math.min(slot.getCount(), remaining.getCount());
                slot.shrink(take);
                remaining.shrink(take);
                removed += take;          // ← 之前漏了这行：背包扣了却不算数，导致「说不够 + 回滚还 0 个」吞物品
            }
        }
        for (String name : data.accessiblePrivateWarehouses(p.getUUID())) {
            HubSavedData.PrivateWarehouse w = data.getPrivateWarehouse(name);
            if (w != null) {
                for (int i = 0; i < w.storage.getSlots() && !remaining.isEmpty(); i++) {
                    ItemStack slot = w.storage.getStackInSlot(i);
                    if (slot.getItem() == stack.getItem()) {
                        int take = Math.min(slot.getCount(), remaining.getCount());
                        slot.shrink(take);
                        if (slot.isEmpty()) w.storage.setStackInSlot(i, ItemStack.EMPTY);
                        remaining.shrink(take);
                        removed += take;
                    }
                }
            }
        }
        return removed;
    }

    private static void removeFromInventory(ServerPlayer p, ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < p.getInventory().getContainerSize() && !remaining.isEmpty(); i++) {
            ItemStack slot = p.getInventory().getItem(i);
            if (slot.getItem() == stack.getItem()) {
                int take = Math.min(slot.getCount(), remaining.getCount());
                slot.shrink(take);
                remaining.shrink(take);
            }
        }
    }

    /** 比较两组筛选是否完全相同（物品+数量） */
    private static boolean sameFilter(List<ItemStack> a, List<ItemStack> b) {
        if (a.size() != b.size()) return false;
        java.util.Map<String, Integer> ma = new java.util.HashMap<>();
        for (ItemStack s : a) ma.merge(s.getItem().toString() + "#" + s.getCount(), 1, Integer::sum);
        for (ItemStack s : b) {
            String k = s.getItem().toString() + "#" + s.getCount();
            Integer c = ma.get(k);
            if (c == null || c <= 0) return false;
            ma.put(k, c - 1);
        }
        return true;
    }

    private static ItemStack parseFilter(String s) {
        try {
            String[] parts = s.split("×", 2);
            if (parts.length != 2) return ItemStack.EMPTY;
            int count = Integer.parseInt(parts[0]);
            net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(parts[1]);
            if (id == null) return ItemStack.EMPTY;
            net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.get(id);
            if (item == null) return ItemStack.EMPTY;
            return new ItemStack(item, count);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    private static String stackListToString(List<ItemStack> list) {
        StringBuilder sb = new StringBuilder();
        for (ItemStack st : list) {
            if (st.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" + ");
            // 这个串只用于「显示」，所以直接用游戏内的名字（内部逻辑另外用 id）
            sb.append(st.getCount()).append("×").append(st.getHoverName().getString());
        }
        return sb.length() == 0 ? "无" : sb.toString();
    }

    private static void handleSyncTrades(SyncTrades payload, IPayloadContext ctx) {
        ClientHubState.trades = new ArrayList<>(payload.trades());
        ClientHubState.selfTrade = payload.selfTrade();   // 界面要知道「调试允许自交易」开着没有
    }

    private static void handleOpenCreateDialog(OpenCreateDialog payload, IPayloadContext ctx) {
        com.commhub.ClientScreens.openCreateDialog();
    }

    /** 客户端收到成交通知 → 播放空间裂缝动画 */
    private static void handleTradeEffect(TradeEffect payload, IPayloadContext ctx) {
        com.commhub.TradeEffectRenderer.trigger(payload.outgoing(), payload.partner(), payload.item(), payload.total());
    }


    /** 保存机械手 / 动力锯卡片配置 */
    private static void handleSaveSimpleCard(SaveSimpleCard payload, IPayloadContext ctx) {
        ServerPlayer player = (ServerPlayer) ctx.player();
        int h = Math.max(0, Math.min(1, payload.hand()));
        net.minecraft.world.InteractionHand hand = (h == 0)
                ? net.minecraft.world.InteractionHand.MAIN_HAND
                : net.minecraft.world.InteractionHand.OFF_HAND;
        ItemStack card = player.getItemInHand(hand);
        if (!(card.getItem() instanceof com.commhub.SimpleCardItem)) return;
        com.commhub.SimpleCard.writeCfg(card, payload.cfg());
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastChanges();
        player.sendSystemMessage(Component.literal("卡片配置已保存"));
    }

    /** 保存合成升级卡配置（写进对应卡片物品的 NBT） */
    private static void handleSaveCardConfig(SaveCardConfig payload, IPayloadContext ctx) {
        ServerPlayer player = (ServerPlayer) ctx.player();
        net.minecraft.core.HolderLookup.Provider provider = player.level().registryAccess();
        java.util.List<com.commhub.CraftingCard.Target> targets =
                com.commhub.CraftingCard.fromTag(payload.tag(), provider);
        if (payload.mode() == 0) {
            // 手里的卡
            int h = Math.max(0, Math.min(1, payload.hand()));
            net.minecraft.world.InteractionHand hand = (h == 0)
                    ? net.minecraft.world.InteractionHand.MAIN_HAND
                    : net.minecraft.world.InteractionHand.OFF_HAND;
            ItemStack card = player.getItemInHand(hand);
            if (!(card.getItem() instanceof com.commhub.CraftingUpgradeCardItem)) return;
            com.commhub.CraftingCard.write(card, targets, provider);
            player.inventoryMenu.broadcastChanges();
            if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastChanges();
            player.sendSystemMessage(Component.literal("合成升级卡已保存"));
            return;
        }
        // 仓库卡槽里的卡（最多 3 张）
        HubSavedData data = HubSavedData.get(player.getServer());
        boolean shared = payload.name() == null || payload.name().isEmpty();
        if (!shared) {
            HubSavedData.PrivateWarehouse w = data.getPrivateWarehouse(payload.name());
            if (w == null) return;
            if (!w.owner.equals(player.getUUID())
                    && !(w.admin != null && w.admin.equals(player.getUUID()))) {
                player.sendSystemMessage(Component.literal("只有仓库主人或管理员能配置合成升级卡"));
                return;
            }
        }
        ItemStack[] slots = data.getCardSlots(shared, payload.name());
        if (slots == null) return;
        // 一次只写一张卡（每张卡一个包，避免大配置超过数据包上限）
        int idx = Math.max(0, Math.min(slots.length - 1, payload.slot()));
        ItemStack card = slots[idx];
        if (card.isEmpty() || !(card.getItem() instanceof com.commhub.CraftingUpgradeCardItem)) return;
        com.commhub.CraftingCard.write(card, targets, provider);
        data.setDirty();
        int saved = 1;
        player.sendSystemMessage(Component.literal("合成升级卡已保存（"
                + (shared ? "共享大仓库" : payload.name()) + "，共 " + saved + " 张）"));
    }

    private static void handleRenameWarehouse(RenameWarehouse payload, IPayloadContext ctx) {
        ServerPlayer player = (ServerPlayer) ctx.player();
        HubSavedData data = HubSavedData.get(player.getServer());
        String newName = payload.newName() == null ? "" : payload.newName().trim();
        if (newName.isEmpty()) {
            player.sendSystemMessage(Component.literal("名字不能为空"));
            return;
        }
        if (newName.length() > 32) {
            player.sendSystemMessage(Component.literal("名字太长了（最多 32 个字符）"));
            return;
        }
        if (data.renamePrivateWarehouse(payload.oldName(), newName, player.getUUID())) {
            player.sendSystemMessage(Component.literal("已重命名为「" + newName + "」（若方块仍指向旧名，重新右键选一次目标即可）"));
            final String fixedName = newName;
            net.neoforged.neoforge.items.IItemHandler wh = data.getPrivateWarehouse(fixedName) == null
                    ? data.getWarehouse() : data.getPrivateWarehouse(fixedName).storage;
            player.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("container.commhub.hub");
                }

                @Override
                public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                    HubSavedData.PrivateWarehouse pw = data.getPrivateWarehouse(fixedName);
                    return new HubMenu(id, inv, wh, data.getMailbox(p.getUUID()),
                            toolHandlerFor(data, HubMenu.MODE_PRIVATE, fixedName),
                            cardHandlerFor(data, HubMenu.MODE_PRIVATE, fixedName), data,
                            HubMenu.MODE_PRIVATE, 0, fixedName,
                            pw == null ? null : pw.owner, pw == null ? null : pw.admin);
                }
            }, buf -> {
                HubSavedData.PrivateWarehouse pw = data.getPrivateWarehouse(fixedName);
                buf.writeInt(HubMenu.MODE_PRIVATE);
                buf.writeInt(0);
                buf.writeUtf(fixedName);
                buf.writeBoolean(pw != null && pw.owner != null); if (pw != null && pw.owner != null) buf.writeUUID(pw.owner);
                buf.writeBoolean(pw != null && pw.admin != null); if (pw != null && pw.admin != null) buf.writeUUID(pw.admin);
                buf.writeInt(pw == null ? 1 : Math.max(1, pw.storage.getPageCount()));
            });
            sendChatSync(player);
            sendPools(player, HubMenu.MODE_PRIVATE, fixedName);
        } else {
            player.sendSystemMessage(Component.literal("改名失败：重名、不存在，或你没有权限"));
        }
    }

    /**
     * 绑定管理工具到仓库。
     * 玩家把自己合成的三把工具放进界面右边的 3 个框，点「绑定」：
     * - 校验是不是本仓库的主人/管理员
     * - 校验这三把工具的类型齐全且都没绑定过
     * - 写入 NBT whname = 仓库名，并记录该仓库【已绑定】，之后不能再绑
     */
    /** 重开私人仓库菜单，刷新客户端（工具格内容变化后） */
    private static void reopenHubMenu(ServerPlayer player, HubSavedData data, String name) {
        HubSavedData.PrivateWarehouse pw = data.getPrivateWarehouse(name);
        if (pw == null) return;
        final String fName = name;
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("container.commhub.hub");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return new HubMenu(id, inv, pw.storage, data.getMailbox(p.getUUID()),
                        toolHandlerFor(data, HubMenu.MODE_PRIVATE, fName),
                        cardHandlerFor(data, HubMenu.MODE_PRIVATE, fName), data,
                        HubMenu.MODE_PRIVATE, 0, fName, pw.owner, pw.admin);
            }
        }, buf -> {
            buf.writeInt(HubMenu.MODE_PRIVATE);
            buf.writeInt(0);
            buf.writeUtf(fName);
            buf.writeBoolean(pw.owner != null); if (pw.owner != null) buf.writeUUID(pw.owner);
            buf.writeBoolean(pw.admin != null); if (pw.admin != null) buf.writeUUID(pw.admin);
            buf.writeInt(Math.max(1, pw.storage.getPageCount()));
        });
    }

    private static void handleBindTools(BindTools payload, IPayloadContext ctx) {
        ServerPlayer player = (ServerPlayer) ctx.player();
        HubSavedData data = HubSavedData.get(player.getServer());
        boolean shared = payload.mode() != HubMenu.MODE_PRIVATE;
        if (shared) {
            player.sendSystemMessage(Component.literal("共享大仓库没有管理工具（管理工具属于私人仓库）"));
            return;
        }
        String whName = payload.name();
        HubSavedData.PrivateWarehouse pw = data.getPrivateWarehouse(whName);
        if (pw == null) {
            player.sendSystemMessage(Component.literal("找不到仓库「" + whName + "」"));
            return;
        }
        // 只有主人或该仓库管理员能绑定
        if (!pw.owner.equals(player.getUUID())
                && !(pw.admin != null && pw.admin.equals(player.getUUID()))) {
            player.sendSystemMessage(Component.literal("只有仓库主人或管理员能绑定管理工具"));
            return;
        }
        // 一个仓库只能绑定一次
        if (data.isToolsBound(whName)) {
            player.sendSystemMessage(Component.literal("「" + whName + "」已经绑定过管理工具了，不能重复绑定"));
            return;
        }
        ItemStack[] slots = data.getToolSlots(false, whName);
        if (slots == null) {
            player.sendSystemMessage(Component.literal("找不到仓库「" + whName + "」"));
            return;
        }
        // 检查三格：必须是三把不同的管理工具，且都没绑定过
        int haveAdd = 0, haveRemove = 0, haveTransfer = 0;
        for (ItemStack st : slots) {
            if (st == null || st.isEmpty()) continue;
            if (isBound(st)) {
                player.sendSystemMessage(Component.literal("这把工具已经绑定过仓库了，换一把新的"));
                return;
            }
            if (st.getItem() == com.commhub.ModItems.ADD_MEMBER_TOOL.get()) haveAdd++;
            else if (st.getItem() == com.commhub.ModItems.REMOVE_MEMBER_TOOL.get()) haveRemove++;
            else if (st.getItem() == com.commhub.ModItems.TRANSFER_ADMIN_TOOL.get()) haveTransfer++;
        }
        if (haveAdd < 1 || haveRemove < 1 || haveTransfer < 1) {
            player.sendSystemMessage(Component.literal(
                    "需要把三把工具（添加成员 / 移除成员 / 转让管理员）都放进右边的框里才能绑定"));
            return;
        }
        // 写入归属 NBT
        for (ItemStack st : slots) {
            if (st == null || st.isEmpty()) continue;
            net.minecraft.nbt.CompoundTag tag = st.getOrDefault(
                    net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            tag.putString("whname", whName);
            st.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.of(tag));
        }
        data.markToolsBound(whName);
        data.setDirty();
        reopenHubMenu(player, data, whName);
        player.sendSystemMessage(Component.literal("已把三把管理工具绑定到「" + whName + "」（仅管理员可用）"));
    }

    /**
     * 重置管理工具：把散落在相关玩家背包里的工具收回，并补满 3 个框。
     * 工具丢了不怕，点一下就能找回来。
     */
    private static void handleResetTools(ResetTools payload, IPayloadContext ctx) {
        ServerPlayer player = (ServerPlayer) ctx.player();
        HubSavedData data = HubSavedData.get(player.getServer());
        boolean shared = payload.mode() != HubMenu.MODE_PRIVATE;
        if (shared) {
            player.sendSystemMessage(Component.literal("共享大仓库没有管理工具"));
            return;
        }
        String whName = payload.name();
        HubSavedData.PrivateWarehouse pw = data.getPrivateWarehouse(whName);
        if (pw == null) {
            player.sendSystemMessage(Component.literal("找不到仓库「" + whName + "」"));
            return;
        }
        if (!pw.owner.equals(player.getUUID())
                && !(pw.admin != null && pw.admin.equals(player.getUUID()))) {
            player.sendSystemMessage(Component.literal("只有仓库主人或管理员能重置管理工具"));
            return;
        }
        ItemStack[] slots = data.getToolSlots(false, whName);
        if (slots == null) return;
        // 1) 收回相关玩家背包/界面里的工具
        java.util.Set<java.util.UUID> targets = new java.util.HashSet<>();
        targets.add(pw.owner);
        targets.addAll(pw.members);
        targets.add(player.getUUID());
        int collected = 0;
        for (java.util.UUID uid : targets) {
            ServerPlayer tp = player.getServer().getPlayerList().getPlayer(uid);
            if (tp == null) continue;
            for (int i = 0; i < tp.getInventory().getContainerSize(); i++) {
                ItemStack st = tp.getInventory().getItem(i);
                if (!HubMenu.isManagementTool(st)) continue;
                if (!matchesWarehouse(st, whName)) continue;
                for (int j = 0; j < 3; j++) {
                    if (slots[j] == null || slots[j].isEmpty()) {
                        slots[j] = st.copyWithCount(1);
                        collected++;
                        st.shrink(1);
                        break;
                    }
                }
                if (st.isEmpty()) tp.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
        // 2) 补满缺的工具（绑定过的按仓库名生成，没绑定的给未绑定版）
        ItemStack[] fresh = new ItemStack[]{
                new ItemStack(com.commhub.ModItems.ADD_MEMBER_TOOL.get()),
                new ItemStack(com.commhub.ModItems.REMOVE_MEMBER_TOOL.get()),
                new ItemStack(com.commhub.ModItems.TRANSFER_ADMIN_TOOL.get())
        };
        boolean bound = data.isToolsBound(whName);
        for (int i = 0; i < 3; i++) {
            if (slots[i] == null || slots[i].isEmpty()) {
                ItemStack it = fresh[i];
                if (bound) {
                    net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
                    tag.putString("whname", whName);
                    it.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                            net.minecraft.world.item.component.CustomData.of(tag));
                }
                slots[i] = it;
            }
        }
        data.setDirty();
        reopenHubMenu(player, data, whName);
        player.sendSystemMessage(Component.literal("已重置管理工具：收回 " + collected + " 把，补满 3 个框"
                + (bound ? "（已绑定到「" + whName + "」）" : "（还没绑定，放好后点「绑定」）")));
    }

    /** 工具是否属于指定仓库 */
    private static boolean matchesWarehouse(ItemStack st, String whName) {
        net.minecraft.nbt.CompoundTag tag = st.getOrDefault(
                net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        String wh = tag.contains("whname") ? tag.getString("whname") : "";
        // 只认「已经绑定到这个仓库」的：没绑定的工具不属于任何仓库，系统不该去动它
        return !wh.isEmpty() && wh.equals(whName);
    }

    /** 工具是否已绑定过某个仓库 */
    private static boolean isBound(ItemStack st) {
        if (st == null || st.isEmpty()) return false;
        net.minecraft.nbt.CompoundTag tag = st.getOrDefault(
                net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        return tag.contains("whname") && !tag.getString("whname").isEmpty();
    }

    private static void handleCreateWarehouse(CreateWarehouse payload, IPayloadContext ctx) {
        ServerPlayer player = (ServerPlayer) ctx.player();
        HubSavedData data = HubSavedData.get(player.getServer());
        String name = payload.name() == null ? "" : payload.name().trim();
        if (name.isEmpty()) {
            player.sendSystemMessage(Component.literal("名字不能为空"));
            return;
        }
        if (name.length() > 32) {
            player.sendSystemMessage(Component.literal("名字太长了（最多 32 个字符）"));
            return;
        }
        if (data.hasPrivateWarehouse(name)) {
            // 重名：不创建、不消耗凭证
            player.sendSystemMessage(Component.literal("已经有同名仓库「" + name + "」了，换个名字（凭证未消耗）"));
            return;
        }
        // 必须背包里有管理员凭证才能创建；创建成功才消耗
        boolean hasToken = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).getItem() == com.commhub.ModItems.MEMBERSHIP_TOKEN.get()) {
                hasToken = true;
                break;
            }
        }
        if (!hasToken) {
            player.sendSystemMessage(Component.literal("需要背包里有一张「管理员凭证」才能创建仓库"));
            return;
        }
        data.createPrivateWarehouse(player.getUUID(), name);
        // 消耗一张凭证
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack st = player.getInventory().getItem(i);
            if (st.getItem() == com.commhub.ModItems.MEMBERSHIP_TOKEN.get()) {
                st.shrink(1);
                if (st.isEmpty()) player.getInventory().setItem(i, ItemStack.EMPTY);
                break;
            }
        }
        player.sendSystemMessage(Component.literal("私人仓库「" + name + "」创建成功（已消耗凭证）"));
        sendChatSync(player);
    }

    private static void handlePublishResult(PublishResult payload, IPayloadContext ctx) {
        com.commhub.ClientScreens.onPublishResult(payload.ok());
    }

    private static void handleSyncPools(SyncPools payload, IPayloadContext ctx) {
        ClientHubState.energy = payload.energy();
        ClientHubState.fluidAmount = payload.fluidAmount();
        ClientHubState.fluidName = payload.fluidName();
    }

    private static void handleChatSync(ChatSync payload, IPayloadContext ctx) {
        ClientHubState.applyChatSync(payload.publicLog(), payload.privateLog(), payload.players(), payload.warehouses(), payload.friends(), payload.incoming());
    }

    private static void handleChatPush(ChatPush payload, IPayloadContext ctx) {
        ClientHubState.addChatPush(new ChatEntry(
                payload.sender(), payload.senderName(), payload.target(),
                payload.message(), payload.timestamp(), payload.isPublic()));
    }
}
