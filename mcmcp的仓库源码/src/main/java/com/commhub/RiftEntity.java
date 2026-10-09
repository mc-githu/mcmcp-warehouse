package com.commhub;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 空间裂缝实体：成交动画的**动态结构**。
 *
 * <ul>
 *   <li>有碰撞箱（1.8 × 2.8，见 {@link ModEntities#RIFT}），能被撞、能被推动</li>
 *   <li>普通模式：2.6 秒后自动消失</li>
 *   <li><b>常驻模式</b>（测试用，{@code /commhub rift}）：一直开着，直到 {@code /commhub riftstop}</li>
 * </ul>
 */
public class RiftEntity extends Entity {

    private static final EntityDataAccessor<Boolean> OUTGOING =
            SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<ItemStack> ITEM =
            SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> TOTAL =
            SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> PERSISTENT =
            SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.BOOLEAN);
    // ---- 链节模式：锁链上的小碰撞体积 ----
    private static final EntityDataAccessor<Boolean> CHAIN_NODE =
            SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ANCHOR_ID =
            SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> NODE_INDEX =
            SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.INT);
    /** 包裹里装了什么（显示用：物品名用逗号隔开） */
    private static final EntityDataAccessor<String> CONTENTS =
            SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.STRING);

    /** 每段锁链放几个碰撞节点 */
    public static final int NODES = 4;

    private static final int LIFE = 52;   // 2.6 秒（非常驻）

    private int age = 0;
    private boolean nodesSpawned = false;

    public RiftEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static RiftEntity spawn(Level level, double x, double y, double z,
                                   boolean outgoing, ItemStack item, int total, boolean persistent) {
        return spawn(level, x, y, z, outgoing, item, total, persistent, "");
    }

    public static RiftEntity spawn(Level level, double x, double y, double z,
                                   boolean outgoing, ItemStack item, int total, boolean persistent,
                                   String contents) {
        RiftEntity e = new RiftEntity(ModEntities.RIFT.get(), level);
        e.setPos(x, y, z);
        e.setOutgoing(outgoing);
        e.setItem(item == null ? ItemStack.EMPTY : item.copyWithCount(1));
        e.setTotal(Math.max(1, total));
        e.setPersistent(persistent);
        e.setContents(contents);
        level.addFreshEntity(e);
        return e;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OUTGOING, true);
        builder.define(ITEM, ItemStack.EMPTY);
        builder.define(TOTAL, 1);
        builder.define(PERSISTENT, false);
        builder.define(CHAIN_NODE, false);
        builder.define(ANCHOR_ID, -1);
        builder.define(NODE_INDEX, 0);
        builder.define(CONTENTS, "");
    }

    public String getContents() {
        return entityData.get(CONTENTS);
    }

    public void setContents(String s) {
        entityData.set(CONTENTS, s == null ? "" : s);
    }

    public boolean isChainNode() {
        return entityData.get(CHAIN_NODE);
    }

    public void setChainNode(boolean v) {
        entityData.set(CHAIN_NODE, v);
    }

    public int getAnchorId() {
        return entityData.get(ANCHOR_ID);
    }

    public void setAnchorId(int id) {
        entityData.set(ANCHOR_ID, id);
    }

    public int getNodeIndex() {
        return entityData.get(NODE_INDEX);
    }

    public void setNodeIndex(int i) {
        entityData.set(NODE_INDEX, i);
    }

    /** 链节用小碰撞箱；裂缝本体用实体类型里定义的 1.8×2.8 */
    @Override
    public net.minecraft.world.entity.EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose) {
        if (isChainNode()) {
            return net.minecraft.world.entity.EntityDimensions.scalable(0.45f, 0.45f);
        }
        return super.getDimensions(pose);
    }

    /** 在主裂缝生成时，顺便在锁链上摆好 NODES 个碰撞节点 */
    public void spawnChainNodes() {
        if (level().isClientSide() || isChainNode()) return;
        for (int i = 0; i < NODES; i++) {
            RiftEntity node = new RiftEntity(ModEntities.RIFT.get(), level());
            node.setPos(getX(), getY(), getZ());
            node.setChainNode(true);
            node.setAnchorId(getId());
            node.setNodeIndex(i + 1);
            node.setOutgoing(isOutgoing());
            node.setPersistent(isPersistentRift());
            node.setContents(getContents());
            level().addFreshEntity(node);
        }
    }

    public boolean isOutgoing() {
        return entityData.get(OUTGOING);
    }

    public void setOutgoing(boolean v) {
        entityData.set(OUTGOING, v);
    }

    public ItemStack getItem() {
        return entityData.get(ITEM);
    }

    public void setItem(ItemStack st) {
        entityData.set(ITEM, st);
    }

    public int getTotal() {
        return entityData.get(TOTAL);
    }

    public void setTotal(int n) {
        entityData.set(TOTAL, n);
    }

    public boolean isPersistentRift() {
        return entityData.get(PERSISTENT);
    }

    public void setPersistent(boolean v) {
        entityData.set(PERSISTENT, v);
    }

    /** 动画进度 0~1（常驻时门保持全开；包裹的来回由渲染器按 tickCount 做乒乓） */
    public float progress(float partialTick) {
        if (isPersistentRift()) return 1f;
        return Math.max(0f, Math.min(1f, (age + partialTick) / (float) LIFE));
    }

    @Override
    public void tick() {
        super.tick();
        age++;
        if (level().isClientSide) return;
        if (isChainNode()) {
            // 找主裂缝，然后把自己放到锁链的对应位置上
            var anchor = level().getEntity(getAnchorId());
            if (!(anchor instanceof RiftEntity rift) || rift.isRemoved() || rift.isChainNode()) {
                discard();
                return;
            }
            var player = level().getNearestPlayer(this, 24.0);
            if (player == null) {
                setPos(rift.getX(), rift.getY(), rift.getZ());
                return;
            }
            double f = getNodeIndex() / (double) (NODES + 1);
            double cx = rift.getX();
            double cy = rift.getY() + 1.6;               // 裂缝中心
            double cz = rift.getZ();
            double tx = cx + (player.getX() - cx) * f;
            double ty = cy + (player.getY() + player.getBbHeight() * 0.55 - cy) * f;
            double tz = cz + (player.getZ() - cz) * f;
            setPos(tx, ty, tz);
            return;
        }
        if (!isPersistentRift() && age >= LIFE) {
            // 连同链节一起收掉
            for (RiftEntity n : level().getEntitiesOfClass(RiftEntity.class, getBoundingBox().inflate(64))) {
                if (n.isChainNode() && n.getAnchorId() == getId()) n.discard();
            }
            discard();
        }
    }

    // ---- 碰撞箱：能撞到、能推动、能选中 ----
    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("outgoing", isOutgoing());
        tag.putInt("total", getTotal());
        tag.putInt("age", age);
        tag.putBoolean("persistent", isPersistentRift());
        tag.putString("contents", getContents());
        if (!getItem().isEmpty()) tag.put("item", getItem().save(registryAccess()));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setOutgoing(tag.getBoolean("outgoing"));
        setTotal(tag.getInt("total"));
        age = tag.getInt("age");
        setPersistent(tag.getBoolean("persistent"));
        setContents(tag.getString("contents"));
        if (tag.contains("item")) {
            ItemStack.parse(registryAccess(), tag.getCompound("item")).ifPresent(this::setItem);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        return dist < 128.0 * 128.0;
    }
}
