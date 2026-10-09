package com.commhub;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, CommHub.MODID);

    /** 空间裂缝：成交动画用的动态结构，**有碰撞箱**（1.8 × 2.8） */
    public static final DeferredHolder<EntityType<?>, EntityType<RiftEntity>> RIFT =
            ENTITY_TYPES.register("rift", () -> EntityType.Builder.<RiftEntity>of(RiftEntity::new, MobCategory.MISC)
                    .sized(1.8f, 2.8f)              // ← 碰撞箱尺寸
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .noSummon()
                    .fireImmune()
                    .build("commhub:rift"));

    private ModEntities() {
    }
}
