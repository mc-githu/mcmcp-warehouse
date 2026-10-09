package com.commhub.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * 补丁：修复 sable 2.0.3 + flywheel 的崩溃。
 * sable 的 BlockEntityStorageMixin 遍历子级别块实体时，posLookup.get() 对
 * 没有 flywheel visual 的块实体返回 null，随后 checkcast 直接 NPE。
 * 这里把 sable 注入的崩溃方法用 try-catch 包起来，跳过异常，防止整个渲染循环崩溃。
 * 只影响 sable 存在时的渲染路径，不改变任何功能。
 */
@Mixin(targets = "dev.engine_room.flywheel.impl.visualization.storage.BlockEntityStorage", remap = false)
public class SableFlywheelCrashFixMixin {

    /** 崩溃方法：遍历子级别块实体更新 embedding（sable 注入的私有方法） */
    @WrapMethod(method = "sable$updateSubLevelEmbeddingsFrame", remap = false)
    @Unique
    private void commhub$safeUpdate(Object visualizationContext, Operation<Void> original) {
        try {
            original.call(visualizationContext);
        } catch (Exception e) {
            // sable 对无 visual 的块实体不判空导致 NPE，跳过即可，不影响其它渲染
            org.slf4j.LoggerFactory.getLogger("commhub").debug(
                    "[commhub] sable 子级别块实体更新被跳过: {}", e.toString());
        }
    }

    /** 入口方法：每帧调用崩溃方法 */
    @WrapMethod(method = "sable$preFlywheelFrame", remap = false)
    @Unique
    private void commhub$safePre(Object originalVoid, Operation<Void> original) {
        try {
            original.call(originalVoid);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("commhub").debug(
                    "[commhub] sable flywheel 帧被跳过: {}", e.toString());
        }
    }
}
