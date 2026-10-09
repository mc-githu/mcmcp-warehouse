package com.commhub;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 装了合成升级卡的仓库会定时自动合成：每 10 tick（约 0.5 秒）每个仓库合成一次。
 * 完全跑在服务端，与区块是否加载无关。
 */
@EventBusSubscriber(modid = CommHub.MODID)
public class CraftingUpgradeTicker {

    private static final int INTERVAL = 10;
    /** 每 5 分钟清一次配方缓存（/reload 后配方可能变，避免一直用旧配方） */
    private static final int CACHE_CLEAR_TICKS = 20 * 60 * 5;
    private static int counter = 0;
    private static int cacheTicks = 0;

    /** /reload 或换存档后，Create 配方缓存要重读 */
    @SubscribeEvent
    public static void onDatapackSync(net.neoforged.neoforge.event.OnDatapackSyncEvent event) {
        CreateRecipes.invalidate();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (++cacheTicks >= CACHE_CLEAR_TICKS) {
            cacheTicks = 0;
            AutoCrafter.clearCache();
        }
        if (++counter < INTERVAL) return;
        counter = 0;
        MinecraftServer server = event.getServer();
        if (server == null) return;
        ServerLevel level = server.overworld();
        HubSavedData data = HubSavedData.get(server);
        CreateRecipes.ensureLoaded(server);   // 读一次 Create 的加工配方

        // 只有私人仓库有卡槽，所以只跑私人仓库
        for (HubSavedData.PrivateWarehouse w : data.allPrivateWarehouses()) {
            AutoCrafter.tick(level, w.storage, w.cardSlots);         // 合成升级卡
            AutoCrafter.tickSaw(level, w.storage, w.cardSlots);      // 动力锯卡片（切割 / 去皮）
            AutoCrafter.tickDeployer(level, w.storage, w.cardSlots); // 机械手卡片（物品应用）
        }
    }
}
