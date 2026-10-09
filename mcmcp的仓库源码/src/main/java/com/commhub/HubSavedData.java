package com.commhub;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class HubSavedData extends SavedData {
    public static final String NAME = "commhub_hub";
    public static final SavedData.Factory<HubSavedData> FACTORY =
            new SavedData.Factory<>(HubSavedData::new, HubSavedData::load, null);

    /** 私人仓库（有名字） */
    public static class PrivateWarehouse {
        public String name;
        public UUID owner;
        /** 该仓库的管理员（每仓库最多一个，可为 null = 还没有管理员） */
        public UUID admin = null;
        public final Set<UUID> members = new HashSet<>();
        public PagedItemHandler storage;
        public int energy = 0;
        public FluidStack fluid = FluidStack.EMPTY;
        /** 是否已经把管理工具绑定到这个仓库（一个仓库只能绑一次） */
        public boolean toolsBound = false;
        /** 三个管理工具栏 */
        public final ItemStack[] toolSlots = emptyTools();
        /** 合成升级卡槽（最多 3 张）：装上后仓库会按卡上的配方自动合成 */
        public final ItemStack[] cardSlots = emptyCards();

        public PrivateWarehouse(String name, UUID owner) {
            this.name = name;
            this.owner = owner;
            this.storage = new PagedItemHandler(ModConfig.getPrivateInitialPages());
        }
    }

    /** 共享大仓库：分页存储，满了自动加页，空页丢弃 */
    private PagedItemHandler warehouse = new PagedItemHandler(ModConfig.getSharedInitialPages());

    /** 给分页仓库挂上「内容变化就标脏」的回调 */
    private PagedItemHandler wire(PagedItemHandler h) {
        h.setChangeListener(this::setDirty);
        return h;
    }
    private final Map<String, PrivateWarehouse> privateWarehouses = new LinkedHashMap<>();
    private final Map<UUID, InfiniteItemStackHandler> mailboxes = new HashMap<>();
    private final Set<UUID> admins = new HashSet<>();
    private final Map<UUID, Set<UUID>> friendRequests = new HashMap<>();
    private final Set<String> friends = new HashSet<>();
    private final List<Trade> trades = new ArrayList<>();
    private final List<ChatEntry> publicLog = new ArrayList<>();
    private final List<ChatEntry> privateLog = new ArrayList<>();
    /** 取用记录（防小偷）：谁、拿了什么、多少、理由、什么时候 */
    public record AuditEntry(UUID who, String whoName, String itemId, int count,
                             String reason, String where, long time) {
    }

    /** 最多保留多少条记录 */
    public static final int AUDIT_MAX = 300;
    private final List<AuditEntry> audit = new ArrayList<>();

    private int sharedEnergy = 0;
    private FluidStack sharedFluid = FluidStack.EMPTY;
    private final ItemStack[] sharedToolSlots = emptyTools();
    /** 共享大仓库的合成升级卡槽（最多 3 张） */
    private final ItemStack[] sharedCardSlots = emptyCards();

    public HubSavedData() {
        // 全新存档也要有共享仓库的管理工具（load() 不会跑）
        ensureSharedTools();
        // 共享仓库是分页存储：挂上「变化即标脏」
        wire(warehouse);
    }

    private static ItemStack[] emptyCards() {
        return new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    }

    private static void readCardSlots(ListTag list, ItemStack[] slots, HolderLookup.Provider provider) {
        for (int i = 0; i < slots.length; i++) {
            if (i >= list.size()) { slots[i] = ItemStack.EMPTY; continue; }
            CompoundTag t = list.getCompound(i);
            if (t.isEmpty()) { slots[i] = ItemStack.EMPTY; continue; }
            ItemStack st = ItemStack.parseOptional(provider, t);
            slots[i] = (st == null) ? ItemStack.EMPTY : st;
        }
    }

    private static ItemStack[] emptyTools() {
        return new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    }

    private static ItemStack makeTool(net.minecraft.world.item.Item item, String whName) {
        return makeTool(item, whName, false);
    }

    /** 生成工具；isGrant 只对管理员工具有意义（true=给予，false=转让） */
    private static ItemStack makeTool(net.minecraft.world.item.Item item, String whName, boolean isGrant) {
        if (item == null) return ItemStack.EMPTY;
        ItemStack st = new ItemStack(item);
        net.minecraft.nbt.CompoundTag tag = st.getOrDefault(
                net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if (whName != null && !whName.isEmpty()) {
            tag.putString("whname", whName);
        }
        // 管理员工具写入给予/转让标记
        if (item == ModItems.TRANSFER_ADMIN_TOOL.get()) {
            tag.putBoolean("isgrant", isGrant);
        }
        st.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        return st;
    }

    /** 生成三把工具；isGrant 表示管理员工具是"给予"（仓库无管理员）还是"转让"（有管理员） */
    private static ItemStack[] defaultTools(String whName, boolean isGrant) {
        return new ItemStack[]{
                makeTool(ModItems.ADD_MEMBER_TOOL.get(), whName),
                makeTool(ModItems.REMOVE_MEMBER_TOOL.get(), whName),
                makeTool(ModItems.TRANSFER_ADMIN_TOOL.get(), whName, isGrant)
        };
    }

    public static HubSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    public static HubSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        HubSavedData data = new HubSavedData();
        CompoundTag whTag = tag.getCompound("warehouse");
        data.warehouse = data.wire(whTag.contains("Pages")
                ? PagedItemHandler.deserialize(provider, whTag)
                : PagedItemHandler.deserializeLegacy(provider, whTag));

        ListTag mbList = tag.getList("mailboxes", Tag.TAG_COMPOUND);
        for (int i = 0; i < mbList.size(); i++) {
            CompoundTag mb = mbList.getCompound(i);
            data.mailboxes.put(mb.getUUID("uuid"), readMailbox(provider, mb.getCompound("items")));
        }

        ListTag pwList = tag.getList("privateWarehouses", Tag.TAG_COMPOUND);
        for (int i = 0; i < pwList.size(); i++) {
            CompoundTag pw = pwList.getCompound(i);
            PrivateWarehouse w = new PrivateWarehouse(pw.getString("name"), pw.getUUID("owner"));
            w.admin = pw.hasUUID("admin") ? pw.getUUID("admin") : null;
            CompoundTag stTag = pw.getCompound("items");
            w.storage = data.wire(stTag.contains("Pages")
                    ? PagedItemHandler.deserialize(provider, stTag)
                    : PagedItemHandler.deserializeLegacy(provider, stTag));
            w.energy = pw.getInt("energy");
            w.toolsBound = pw.getBoolean("toolsBound");
            w.fluid = FluidStack.parseOptional(provider, pw.getCompound("fluid"));
            readToolSlots(pw.getList("tools", Tag.TAG_COMPOUND), w.toolSlots, provider);
            readCardSlots(pw.getList("cards", Tag.TAG_COMPOUND), w.cardSlots, provider);
            // 兼容 1.10.0/1.10.1 的单卡槽存档（键名 "card"）
            if (!pw.contains("cards") && pw.contains("card")) {
                ItemStack legacy = ItemStack.parseOptional(provider, pw.getCompound("card"));
                w.cardSlots[0] = (legacy == null) ? ItemStack.EMPTY : legacy;
            }
            ListTag mem = pw.getList("members", Tag.TAG_COMPOUND);
            for (int j = 0; j < mem.size(); j++) w.members.add(mem.getCompound(j).getUUID("id"));
            data.privateWarehouses.put(w.name, w);
        }

        readUuidSet(tag, "admins", data.admins);

        data.friendRequests.clear();
        ListTag frList = tag.getList("friendRequests", Tag.TAG_COMPOUND);
        for (int i = 0; i < frList.size(); i++) {
            CompoundTag fr = frList.getCompound(i);
            UUID to = fr.getUUID("to");
            ListTag froms = fr.getList("from", Tag.TAG_COMPOUND);
            Set<UUID> set = new HashSet<>();
            for (int j = 0; j < froms.size(); j++) set.add(froms.getCompound(j).getUUID("id"));
            data.friendRequests.put(to, set);
        }
        data.friends.clear();
        ListTag fList = tag.getList("friends", Tag.TAG_COMPOUND);
        for (int i = 0; i < fList.size(); i++) data.friends.add(fList.getCompound(i).getString("pair"));

        data.trades.clear();
        ListTag trList = tag.getList("trades", Tag.TAG_COMPOUND);
        for (int i = 0; i < trList.size(); i++) {
            Trade tr = Trade.fromNbt(trList.getCompound(i), provider);
            if (tr != null) data.trades.add(tr);
        }

        data.publicLog.clear();
        data.privateLog.clear();
        ListTag pub = tag.getList("publicLog", Tag.TAG_COMPOUND);
        for (int i = 0; i < pub.size(); i++) {
            ChatEntry c = ChatEntry.fromNbt(pub.getCompound(i));
            if (c != null) data.publicLog.add(c);
        }
        ListTag priv = tag.getList("privateLog", Tag.TAG_COMPOUND);
        for (int i = 0; i < priv.size(); i++) {
            ChatEntry c = ChatEntry.fromNbt(priv.getCompound(i));
            if (c != null) data.privateLog.add(c);
        }
        data.sharedEnergy = tag.getInt("shared_energy");
        data.sharedFluid = FluidStack.parseOptional(provider, tag.getCompound("shared_fluid"));
        readToolSlots(tag.getList("shared_tools", Tag.TAG_COMPOUND), data.sharedToolSlots, provider);
        ListTag auditTag = tag.getList("audit", Tag.TAG_COMPOUND);
        for (int i = 0; i < auditTag.size() && data.audit.size() < AUDIT_MAX; i++) {
            CompoundTag t = auditTag.getCompound(i);
            data.audit.add(new AuditEntry(
                    t.hasUUID("who") ? t.getUUID("who") : null,
                    t.getString("name"), t.getString("item"), t.getInt("count"),
                    t.getString("reason"), t.getString("where"), t.getLong("time")));
        }
        readCardSlots(tag.getList("shared_cards", Tag.TAG_COMPOUND), data.sharedCardSlots, provider);
        if (!tag.contains("shared_cards") && tag.contains("shared_card")) {
            ItemStack legacyShared = ItemStack.parseOptional(provider, tag.getCompound("shared_card"));
            data.sharedCardSlots[0] = (legacyShared == null) ? ItemStack.EMPTY : legacyShared;
        }
        data.toolsInitialized = tag.getBoolean("tools_init");
        data.ensureSharedTools();
        return data;
    }

    /**
     * 按「当前配置」的格数重建仓库，同时把存档里的物品搬过来。
     * 注意：ItemStackHandler.deserializeNBT 会按存档里的 Size 覆盖格数，
     * 而 setSize 会清空物品，所以必须「先读进临时对象，再拷到正确大小的新对象」。
     */
    /** 邮箱读入：普通堆叠（每格 64） */
    private static InfiniteItemStackHandler readMailbox(HolderLookup.Provider provider, CompoundTag handlerTag) {
        // 自定义格式能同时读旧格式（没有 Count 字段就按 Item 里的 count）；
        // 大堆叠的真实数量存在 Count 字段里，读档时还原。
        return InfiniteItemStackHandler.deserializeCustom(provider, handlerTag, HubMenu.MAILBOX_SLOTS);
    }

    private static InfiniteItemStackHandler readHandlerSized(HolderLookup.Provider provider, CompoundTag handlerTag, int targetSize) {
        InfiniteItemStackHandler loaded = new InfiniteItemStackHandler(1);
        loaded.deserializeNBT(provider, handlerTag);
        InfiniteItemStackHandler result = new InfiniteItemStackHandler(Math.max(1, targetSize));
        int n = Math.min(result.getSlots(), loaded.getSlots());
        for (int i = 0; i < n; i++) result.setStackInSlot(i, loaded.getStackInSlot(i));
        return result;
    }

    private static void readToolSlots(ListTag list, ItemStack[] slots, HolderLookup.Provider provider) {
        for (int i = 0; i < 3; i++) {
            ItemStack parsed = (i < list.size()) ? ItemStack.parseOptional(provider, list.getCompound(i)) : ItemStack.EMPTY;
            slots[i] = (parsed == null) ? ItemStack.EMPTY : parsed;
        }
    }

    private static ListTag writeToolSlots(ItemStack[] slots, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (int i = 0; i < 3; i++) {
            list.add(slots[i].saveOptional(provider));
        }
        return list;
    }

    private static void readUuidSet(CompoundTag tag, String key, Set<UUID> set) {
        set.clear();
        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) set.add(list.getCompound(i).getUUID("id"));
    }

    /**
     * 保存前把"超过物品自身堆叠上限"的格子拆开，防止 ItemStack.save() 校验失败崩服。
     * 兼容老存档：以前无脑堆到 1 亿的物品（如 drivebywire:wire）会在这里自动拆分，
     * 拆不下的部分放到同仓库的空格里；实在放不下才丢弃并留日志。
     */

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        // 分页仓库单格上限就是 99（存档硬限制），只需丢掉末尾空页
        warehouse.trimEmptyTail();
        for (PrivateWarehouse w : privateWarehouses.values()) w.storage.trimEmptyTail();
        tag.put("warehouse", warehouse.serialize(provider));

        ListTag mbList = new ListTag();
        for (Map.Entry<UUID, InfiniteItemStackHandler> e : mailboxes.entrySet()) {
            CompoundTag mb = new CompoundTag();
            mb.putUUID("uuid", e.getKey());
            mb.put("items", e.getValue().serializeCustom(provider));
            mbList.add(mb);
        }
        tag.put("mailboxes", mbList);

        ListTag pwList = new ListTag();
        for (PrivateWarehouse w : privateWarehouses.values()) {
            CompoundTag pw = new CompoundTag();
            pw.putString("name", w.name);
            pw.putUUID("owner", w.owner);
            if (w.admin != null) pw.putUUID("admin", w.admin);
            pw.put("items", w.storage.serialize(provider));
            pw.putInt("energy", w.energy);
            pw.putBoolean("toolsBound", w.toolsBound);
            pw.put("fluid", w.fluid.saveOptional(provider));
            pw.put("tools", writeToolSlots(w.toolSlots, provider));
            ListTag cardsTag = new ListTag();
            for (ItemStack c : w.cardSlots) cardsTag.add(c.isEmpty() ? new CompoundTag() : c.saveOptional(provider));
            pw.put("cards", cardsTag);
            ListTag mem = new ListTag();
            for (UUID m : w.members) {
                CompoundTag t = new CompoundTag();
                t.putUUID("id", m);
                mem.add(t);
            }
            pw.put("members", mem);
            pwList.add(pw);
        }
        tag.put("privateWarehouses", pwList);

        writeUuidSet(tag, "admins", admins);

        ListTag frList = new ListTag();
        for (Map.Entry<UUID, Set<UUID>> e : friendRequests.entrySet()) {
            CompoundTag fr = new CompoundTag();
            fr.putUUID("to", e.getKey());
            ListTag froms = new ListTag();
            for (UUID from : e.getValue()) {
                CompoundTag t = new CompoundTag();
                t.putUUID("id", from);
                froms.add(t);
            }
            fr.put("from", froms);
            frList.add(fr);
        }
        tag.put("friendRequests", frList);

        ListTag fList = new ListTag();
        for (String pair : friends) {
            CompoundTag t = new CompoundTag();
            t.putString("pair", pair);
            fList.add(t);
        }
        tag.put("friends", fList);

        ListTag trList = new ListTag();
        for (Trade tr : trades) trList.add(tr.toNbt(provider));
        tag.put("trades", trList);

        ListTag pub = new ListTag();
        for (ChatEntry c : publicLog) pub.add(c.toNbt());
        tag.put("publicLog", pub);

        ListTag priv = new ListTag();
        for (ChatEntry c : privateLog) priv.add(c.toNbt());
        tag.put("privateLog", priv);

        tag.putInt("shared_energy", sharedEnergy);
        tag.put("shared_fluid", sharedFluid.saveOptional(provider));
        ListTag auditTag = new ListTag();
        for (AuditEntry e : audit) {
            CompoundTag t = new CompoundTag();
            if (e.who() != null) t.putUUID("who", e.who());
            t.putString("name", e.whoName() == null ? "" : e.whoName());
            t.putString("item", e.itemId() == null ? "" : e.itemId());
            t.putInt("count", e.count());
            t.putString("reason", e.reason() == null ? "" : e.reason());
            t.putString("where", e.where() == null ? "" : e.where());
            t.putLong("time", e.time());
            auditTag.add(t);
        }
        tag.put("audit", auditTag);
        tag.put("shared_tools", writeToolSlots(sharedToolSlots, provider));
        ListTag sharedCardsTag = new ListTag();
        for (ItemStack c : sharedCardSlots) sharedCardsTag.add(c.isEmpty() ? new CompoundTag() : c.saveOptional(provider));
        tag.put("shared_cards", sharedCardsTag);
        tag.putBoolean("tools_init", toolsInitialized);
        return tag;
    }

    private static void writeUuidSet(CompoundTag tag, String key, Set<UUID> set) {
        ListTag list = new ListTag();
        for (UUID id : set) {
            CompoundTag t = new CompoundTag();
            t.putUUID("id", id);
            list.add(t);
        }
        tag.put(key, list);
    }

    /** 该仓库是否已绑定管理工具 */
    public boolean isToolsBound(String whName) {
        PrivateWarehouse w = privateWarehouses.get(whName);
        return w != null && w.toolsBound;
    }

    /** 标记该仓库已绑定管理工具 */
    public void markToolsBound(String whName) {
        PrivateWarehouse w = privateWarehouses.get(whName);
        if (w != null) {
            w.toolsBound = true;
            setDirty();
        }
    }

    /** 取某仓库的管理工具栏（共享或私人） */
    public ItemStack[] getToolSlots(boolean shared, String name) {
        if (shared) return sharedToolSlots;
        PrivateWarehouse w = getPrivateWarehouse(name);
        return w == null ? null : w.toolSlots;
    }

    /** 重置管理工具：把三把工具放回对应仓库（共享或私人） */
    public void resetTools(boolean shared, String name) {
        ItemStack[] slots = shared ? sharedToolSlots : (getPrivateWarehouse(name) == null ? null : getPrivateWarehouse(name).toolSlots);
        if (slots == null) return;
        String whName = shared ? "共享大仓库" : name;
        boolean hasAdmin = !shared && getPrivateWarehouse(name) != null && getPrivateWarehouse(name).admin != null;
        for (int i = 0; i < 3; i++) {
            if (slots[i] == null || slots[i].isEmpty()) slots[i] = defaultTools(whName, !hasAdmin)[i];
        }
        if (shared) toolsInitialized = true;
        setDirty();
    }

    /** 是否已经初始化过工具栏（持久化；true 之后拿走工具就不会再自动补，只能点「重置工具」） */
    private boolean toolsInitialized = false;

    /**
     * 只在【第一次】初始化共享仓库工具栏。
     * 关键：初始化过之后就不再自动补，否则玩家把工具拿走会被立刻写回，永远拿不走。
     * 想恢复请用界面上的「重置工具」按钮。
     */
    public void ensureSharedTools() {
        if (toolsInitialized) return;
        boolean any = false;
        for (int i = 0; i < 3; i++) {
            if (sharedToolSlots[i] != null && !sharedToolSlots[i].isEmpty()) { any = true; break; }
        }
        if (!any) {
            ItemStack[] def = defaultTools("共享大仓库", false);
            for (int i = 0; i < 3; i++) sharedToolSlots[i] = def[i];
        }
        toolsInitialized = true;
        setDirty();
    }

    public int getSharedEnergy() {
        return sharedEnergy;
    }

    public void addSharedEnergy(int amount) {
        long v = (long) sharedEnergy + amount;
        sharedEnergy = (int) Math.max(0, Math.min(Integer.MAX_VALUE, v));
        setDirty();
    }

    public void removeSharedEnergy(int amount) {
        sharedEnergy = Math.max(0, sharedEnergy - amount);
        setDirty();
    }

    public FluidStack getSharedFluid() {
        return sharedFluid;
    }

    /** 存液体：空或同种液体可存；不同液体拒绝 */
    public boolean addSharedFluid(FluidStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (sharedFluid.isEmpty()) {
            sharedFluid = stack.copy();
            setDirty();
            return true;
        }
        if (sharedFluid.is(stack.getFluid())) {
            sharedFluid.grow(stack.getAmount());
            setDirty();
            return true;
        }
        return false;
    }

    public void removeSharedFluid(int amount) {
        sharedFluid.shrink(amount);
        if (sharedFluid.getAmount() <= 0) sharedFluid = FluidStack.EMPTY;
        setDirty();
    }


    /** 记一条取用记录（最新的在前面） */
    public void addAudit(UUID who, String whoName, String itemId, int count, String reason, String where) {
        audit.add(0, new AuditEntry(who, whoName, itemId, count,
                reason == null ? "" : reason, where == null ? "" : where, System.currentTimeMillis()));
        while (audit.size() > AUDIT_MAX) audit.remove(audit.size() - 1);
        setDirty();
    }

    public List<AuditEntry> getAudit() {
        return audit;
    }

    public PagedItemHandler getWarehouse() {
        return warehouse;
    }

    /** 合成升级卡槽数量上限 */
    public static final int CARD_SLOTS = 5;

    /** 取某个仓库的合成升级卡槽（最多 3 张）；仓库不存在返回 null */
    public ItemStack[] getCardSlots(boolean shared, String name) {
        if (shared) return sharedCardSlots;
        PrivateWarehouse w = privateWarehouses.get(name);
        return w == null ? null : w.cardSlots;
    }

    /** 所有私人仓库（自动合成要遍历） */
    public java.util.Collection<PrivateWarehouse> allPrivateWarehouses() {
        return privateWarehouses.values();
    }

    public InfiniteItemStackHandler getMailbox(UUID id) {
        // 邮箱同样是单格无限堆叠（自定义存档，见 InfiniteItemStackHandler）
        return mailboxes.computeIfAbsent(id, k -> new InfiniteItemStackHandler(HubMenu.MAILBOX_SLOTS));
    }

    // ---- 私人仓库（命名） ----

    public PrivateWarehouse createPrivateWarehouse(UUID owner, String name) {
        if (privateWarehouses.containsKey(name)) return null;
        PrivateWarehouse w = new PrivateWarehouse(name, owner);
        wire(w.storage);
        ItemStack[] def = defaultTools(name, true);
        for (int i = 0; i < 3; i++) w.toolSlots[i] = def[i];
        privateWarehouses.put(name, w);
        setDirty();
        return w;
    }

    /** 仅测试用：删除一个私人仓库（/commhub test 临时仓库清理） */
    public void removePrivateWarehouseForTest(String name) {
        if (privateWarehouses.remove(name) != null) setDirty();
    }

    public PrivateWarehouse getPrivateWarehouse(String name) {
        return privateWarehouses.get(name);
    }

    /** 重命名私人仓库（只能改自己的，或管理员改） */
    public boolean renamePrivateWarehouse(String oldName, String newName, UUID actor) {
        if (oldName == null || newName == null || oldName.equals(newName)) return false;
        if (privateWarehouses.containsKey(newName)) return false;
        PrivateWarehouse w = privateWarehouses.get(oldName);
        if (w == null) return false;
        if (!w.owner.equals(actor) && !(w.admin != null && w.admin.equals(actor))) return false;
        privateWarehouses.remove(oldName);
        w.name = newName;
        privateWarehouses.put(newName, w);
        setDirty();
        return true;
    }

    public boolean hasPrivateWarehouse(String name) {
        return privateWarehouses.containsKey(name);
    }

    /** 玩家能访问的私人仓库（自己创建的 + 是成员的） */
    public List<String> accessiblePrivateWarehouses(UUID player) {
        List<String> result = new ArrayList<>();
        if (player == null) return result;
        for (PrivateWarehouse w : privateWarehouses.values()) {
            if ((w.owner != null && w.owner.equals(player))
                    || (w.admin != null && w.admin.equals(player))
                    || w.members.contains(player)) {
                result.add(w.name);
            }
        }
        return result;
    }

    public boolean canAccessPrivateWarehouse(PrivateWarehouse w, UUID player) {
        if (w == null || player == null) return false;
        return (w.owner != null && w.owner.equals(player))
                || (w.admin != null && w.admin.equals(player))
                || w.members.contains(player);
    }

    /** 针对指定仓库加成员（工具归属仓库）；仓库名空或共享时退回"所有自己的仓库" */
    public int addMemberTo(String whName, UUID actor, UUID member) {
        if (whName == null || whName.isEmpty() || whName.equals("共享大仓库")) {
            return addMemberToOwned(actor, member);
        }
        PrivateWarehouse w = privateWarehouses.get(whName);
        if (w == null) return 0;
        if (!w.owner.equals(actor) && !(w.admin != null && w.admin.equals(actor))) return -1; // 无权限
        if (w.members.add(member)) {
            setDirty();
            return 1;
        }
        return 0;
    }

    /** 针对指定仓库移除成员 */
    public int removeMemberFrom(String whName, UUID actor, UUID member) {
        if (whName == null || whName.isEmpty() || whName.equals("共享大仓库")) {
            return removeMemberFromOwned(actor, member);
        }
        PrivateWarehouse w = privateWarehouses.get(whName);
        if (w == null) return 0;
        if (!w.owner.equals(actor) && !(w.admin != null && w.admin.equals(actor))) return -1;
        if (w.members.remove(member)) {
            setDirty();
            return 1;
        }
        return 0;
    }

    /** 转让指定仓库的所有权 */
    public int transferOwnershipOf(String whName, UUID from, UUID to) {
        if (whName == null || whName.isEmpty() || whName.equals("共享大仓库")) {
            return transferOwnership(from, to);
        }
        PrivateWarehouse w = privateWarehouses.get(whName);
        if (w == null) return 0;
        if (!w.owner.equals(from) && !admins.contains(from)) return -1;
        w.owner = to;
        setDirty();
        return 1;
    }

    /**
     * 给予/转让管理员（每仓库一个管理员）
     * 把「whName」的管理员身份给 target，自己（actor）失去管理员（主人身份保留）。
     * 权限：主人（owner）或当前管理员（admin）能操作。
     * 返回 1 成功，0 无仓库，-1 无权限。
     */
    public int grantOrTransferAdmin(String whName, UUID actor, UUID target) {
        if (whName == null || whName.isEmpty() || whName.equals("共享大仓库")) return 0;
        PrivateWarehouse w = privateWarehouses.get(whName);
        if (w == null) return 0;
        boolean allowed = (w.owner != null && w.owner.equals(actor))
                || (w.admin != null && w.admin.equals(actor));
        if (!allowed) return -1;
        // 自己不能给自己（测试模式 allow_self_test=true 时放行，供 /commhub test 自测）
        if (actor.equals(target) && !ModConfig.allowSelfTest()) return -1;
        w.admin = target;
        // 把工具栏里的「给予管理员」工具换成「转让管理员」（isgrant: true → false）
        for (ItemStack st : w.toolSlots) {
            if (st == null || st.isEmpty()) continue;
            if (st.getItem() != ModItems.TRANSFER_ADMIN_TOOL.get()) continue;
            net.minecraft.nbt.CompoundTag tag = st.getOrDefault(
                    net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            tag.putBoolean("isgrant", false);
            st.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.of(tag));
        }
        // 收回原操作者（actor）背包里该仓库的管理员工具，防止转让后越权使用
        // （由调用方在服务端执行，这里只标记 dirty；背包清理在 ModNetworking 里做）
        setDirty();
        return 1;
    }

    public int addMemberToOwned(UUID admin, UUID member) {
        int count = 0;
        for (PrivateWarehouse w : privateWarehouses.values()) {
            if (w.owner.equals(admin) && !w.members.contains(member)) {
                w.members.add(member);
                count++;
            }
        }
        if (count > 0) setDirty();
        return count;
    }

    public int removeMemberFromOwned(UUID admin, UUID member) {
        int count = 0;
        for (PrivateWarehouse w : privateWarehouses.values()) {
            if (w.owner.equals(admin) && w.members.remove(member)) count++;
        }
        if (count > 0) setDirty();
        return count;
    }

    public int transferOwnership(UUID from, UUID to) {
        int count = 0;
        for (PrivateWarehouse w : privateWarehouses.values()) {
            if (w.owner.equals(from)) {
                w.owner = to;
                count++;
            }
        }
        if (count > 0) setDirty();
        return count;
    }

    public boolean isAdmin(UUID id) {
        return admins.contains(id);
    }

    public void addAdmin(UUID id) {
        if (admins.add(id)) setDirty();
    }

    public void removeAdmin(UUID id) {
        if (admins.remove(id)) setDirty();
    }

    // ---- 好友 ----

    private static String pairKey(UUID a, UUID b) {
        String s1 = a.toString();
        String s2 = b.toString();
        return s1.compareTo(s2) <= 0 ? s1 + "|" + s2 : s2 + "|" + s1;
    }

    public boolean areFriends(UUID a, UUID b) {
        // 测试模式下允许"自己算自己的好友"，方便单人测私聊
        if (a.equals(b)) return ModConfig.allowSelfTest();
        return friends.contains(pairKey(a, b));
    }

    public void addFriendRequest(UUID from, UUID to) {
        friendRequests.computeIfAbsent(to, k -> new HashSet<>()).add(from);
        setDirty();
    }

    public void acceptFriendRequest(UUID from, UUID to) {
        Set<UUID> reqs = friendRequests.get(to);
        if (reqs != null && reqs.remove(from)) {
            friends.add(pairKey(from, to));
            setDirty();
        }
    }

    public List<UUID> getFriends(UUID player) {
        List<UUID> result = new ArrayList<>();
        for (String pair : friends) {
            String[] parts = pair.split("\\|");
            UUID a = UUID.fromString(parts[0]);
            UUID b = UUID.fromString(parts[1]);
            if (a.equals(player)) result.add(b);
            else if (b.equals(player)) result.add(a);
        }
        return result;
    }

    public List<UUID> getIncomingRequests(UUID player) {
        return new ArrayList<>(friendRequests.getOrDefault(player, java.util.Set.of()));
    }

    public void rejectFriendRequest(UUID from, UUID to) {
        java.util.Set<UUID> reqs = friendRequests.get(to);
        if (reqs != null && reqs.remove(from)) setDirty();
    }

    public void removeFriend(UUID a, UUID b) {
        if (friends.remove(pairKey(a, b))) setDirty();
    }

    public List<Trade> getTrades() {
        return trades;
    }

    public void addTrade(Trade trade) {
        trades.add(trade);
        setDirty();
    }

    public Trade removeTrade(UUID id) {
        for (int i = 0; i < trades.size(); i++) {
            if (trades.get(i).id().equals(id)) {
                Trade t = trades.remove(i);
                setDirty();
                return t;
            }
        }
        return null;
    }

    public List<ChatEntry> getPublicLog() {
        return publicLog;
    }

    public List<ChatEntry> getPrivateLog() {
        return privateLog;
    }

    public void addPublic(ChatEntry entry) {
        publicLog.add(entry);
        while (publicLog.size() > 100) publicLog.remove(0);
        setDirty();
    }

    public void addPrivate(ChatEntry entry) {
        privateLog.add(entry);
        while (privateLog.size() > 200) privateLog.remove(0);
        setDirty();
    }
}
