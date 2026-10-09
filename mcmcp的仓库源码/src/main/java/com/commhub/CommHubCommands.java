package com.commhub;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.fluids.FluidStack;

@EventBusSubscriber(modid = CommHub.MODID, bus = EventBusSubscriber.Bus.GAME)
public class CommHubCommands {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("commhub")
            // /commhub —— 总览
            .executes(ctx -> overview(ctx.getSource()))
            // /commhub energy —— 查电能
            .then(Commands.literal("energy")
                .executes(ctx -> showEnergy(ctx.getSource()))
                // /commhub energy set <n>
                .then(Commands.literal("set")
                    .requires(src -> src.hasPermission(2))
                    .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                        .executes(ctx -> setEnergy(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "amount")))))
                // /commhub energy add <n>
                .then(Commands.literal("add")
                    .requires(src -> src.hasPermission(2))
                    .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                        .executes(ctx -> addEnergy(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "amount"))))))
            // /commhub fluid —— 查液体
            .then(Commands.literal("fluid")
                .executes(ctx -> showFluid(ctx.getSource())))
            // /commhub test —— 一键自测全部功能
            .then(Commands.literal("test")
                .executes(ctx -> runTest(ctx.getSource())))

            // ---- 空间裂缝（动态结构，有碰撞箱）：一直开着，直到停止 ----
            .then(Commands.literal("rift")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> rift(ctx.getSource(), true))
                .then(Commands.literal("play")
                    .executes(ctx -> riftPlay(ctx.getSource(), false))
                    .then(Commands.literal("out")
                        .executes(ctx -> riftPlay(ctx.getSource(), true)))))

            .then(Commands.literal("riftstop")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> rift(ctx.getSource(), false)))

            .then(Commands.literal("faketrade")
                .requires(src -> src.hasPermission(2))   // OP 专用
                .executes(ctx -> {
                    net.minecraft.server.level.ServerPlayer p = ctx.getSource().getPlayer();
                    if (p == null) { ctx.getSource().sendFailure(Component.literal("必须由玩家执行")); return 0; }
                    boolean ok = com.commhub.network.ModNetworking.fakeAcceptsMyLatest(p);
                    return ok ? 1 : 0;
                }))

            .then(Commands.literal("selftrade")
                .requires(src -> src.hasPermission(2))   // 只有 OP 能用
                .executes(ctx -> selfTrade(ctx.getSource(), !com.commhub.network.ModNetworking.ALLOW_SELF_TRADE))
                .then(Commands.argument("value", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                    .executes(ctx -> selfTrade(ctx.getSource(),
                            com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "value")))))

            .then(Commands.literal("loottest")
                .executes(ctx -> lootTest(ctx.getSource(), 100))
                .then(Commands.argument("times", IntegerArgumentType.integer(1, 2000))
                    .executes(ctx -> lootTest(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "times")))))
            // /commhub look <仓库名> —— 查看指定仓库（含共享）
            .then(Commands.literal("look")
                .then(Commands.argument("name", com.mojang.brigadier.arguments.StringArgumentType.word())
                    .executes(ctx -> lookWarehouse(ctx.getSource(),
                            com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "name")))))
        );
    }

    private static HubSavedData data(CommandSourceStack src) {
        return HubSavedData.get(src.getServer());
    }

    private static int overview(CommandSourceStack src) {
        HubSavedData d = data(src);
        FluidStack fluid = d.getSharedFluid();
        String fluidText = fluid.isEmpty() ? "无" : fluid.getDescriptionId() + " " + fluid.getAmount() + " mB";
        src.sendSuccess(() -> Component.literal("【仓库】电能: " + d.getSharedEnergy() + " FE | 液体: " + fluidText
                + " | 共享仓库格数: " + d.getWarehouse().getSlots()), false);
        return 1;
    }

    private static int showEnergy(CommandSourceStack src) {
        src.sendSuccess(() -> Component.literal("仓库电能: " + data(src).getSharedEnergy() + " FE"), false);
        return 1;
    }

    private static int showFluid(CommandSourceStack src) {
        FluidStack fluid = data(src).getSharedFluid();
        src.sendSuccess(() -> Component.literal(fluid.isEmpty()
                ? "仓库液体: 无"
                : "仓库液体: " + fluid.getDescriptionId() + " " + fluid.getAmount() + " mB"), false);
        return 1;
    }

    private static int setEnergy(CommandSourceStack src, int amount) {
        HubSavedData d = data(src);
        // 直接设置：减掉当前再添到目标
        int current = d.getSharedEnergy();
        if (current > amount) d.removeSharedEnergy(current - amount);
        else if (current < amount) d.addSharedEnergy(amount - current);
        src.sendSuccess(() -> Component.literal("电能已设为 " + amount + " FE"), false);
        return 1;
    }

    private static int addEnergy(CommandSourceStack src, int amount) {
        data(src).addSharedEnergy(amount);
        src.sendSuccess(() -> Component.literal("已添加 " + amount + " FE，当前 " + data(src).getSharedEnergy() + " FE"), false);
        return 1;
    }

    private static int lookWarehouse(CommandSourceStack src, String name) {
        HubSavedData d = data(src);
        // 共享仓库：名字写 共享 / shared / 共享大仓库
        if (name.equals("共享") || name.equalsIgnoreCase("shared") || name.equals("共享大仓库")) {
            int used = 0;
            for (int i = 0; i < d.getWarehouse().getSlots(); i++) {
                if (!d.getWarehouse().getStackInSlot(i).isEmpty()) used++;
            }
            int finalUsed = used;
            FluidStack sf = d.getSharedFluid();
            src.sendSuccess(() -> Component.literal("【共享大仓库】物品 " + finalUsed + "/" + d.getWarehouse().getSlots()
                    + " 格 | 电能 " + d.getSharedEnergy() + " FE | 液体 "
                    + (sf.isEmpty() ? "无" : sf.getDescriptionId() + " " + sf.getAmount() + " mB")), false);
            return 1;
        }
        HubSavedData.PrivateWarehouse w = d.getPrivateWarehouse(name);
        if (w == null) {
            src.sendSuccess(() -> Component.literal("没有仓库「" + name + "」（共享仓库请写 look 共享）"), false);
            return 0;
        }
        // 权限：只能查自己的仓库（主人/成员/管理员）；共享仓库谁都能查
        net.minecraft.server.level.ServerPlayer sp = src.getPlayer();
        if (sp != null && !d.canAccessPrivateWarehouse(w, sp.getUUID())) {
            src.sendSuccess(() -> Component.literal("你没有「" + name + "」的权限（只能查自己的仓库）"), false);
            return 0;
        }
        int used = 0;
        for (int i = 0; i < w.storage.getSlots(); i++) if (!w.storage.getStackInSlot(i).isEmpty()) used++;
        int finalUsed = used;
        src.sendSuccess(() -> Component.literal("【" + name + "】物品 " + finalUsed + "/" + w.storage.getSlots()
                + " 格 | 电能 " + w.energy + " FE | 液体 "
                + (w.fluid.isEmpty() ? "无" : w.fluid.getDescriptionId() + " " + w.fluid.getAmount() + " mB")
                + " | 成员 " + w.members.size()), false);
        return 1;
    }

    /** /commhub test —— 一键自测：物品/电能/液体/好友/私聊 全跑一遍 */
    /**
     * 战利品自检：直接掷「自动化核心」的注入表和地牢宝箱表，
     * 看核心到底出不出得来（判断战利品修改器有没有真的生效）。
     */
    /**
     * 空间裂缝测试：/commhub rift 开一个**常驻**的（锁链一直伸着），
     * /commhub riftstop 把它们全收掉。（OP 专用）
     */
    private static int rift(CommandSourceStack src, boolean spawn) {
        net.minecraft.server.level.ServerPlayer p = src.getPlayer();
        if (p == null) {
            src.sendFailure(Component.literal("必须由玩家执行"));
            return 0;
        }
        net.minecraft.server.level.ServerLevel level = p.serverLevel();
        if (!spawn) {
            var list = level.getEntitiesOfClass(RiftEntity.class, p.getBoundingBox().inflate(256));
            int n = list.size();
            for (RiftEntity e : list) e.discard();
            src.sendSuccess(() -> Component.literal("§a空间裂缝已关闭（收掉 " + n + " 个）"), true);
            return n;
        }
        // 放在玩家正前方 2.5 格
        var look = p.getLookAngle();
        double x = p.getX() + look.x * 2.5;
        double y = p.getY();
        double z = p.getZ() + look.z * 2.5;
        RiftEntity e = RiftEntity.spawn(level, x, y, z, false, net.minecraft.world.item.ItemStack.EMPTY, 1, true);
        e.setYRot(p.getYRot() + 180f);      // 正面朝向玩家（走到背后就看不到裂缝本体了）
        e.setYHeadRot(e.getYRot());
        src.sendSuccess(() -> Component.literal("§a空间裂缝已打开（常驻，锁链一直伸着）—— 用 /commhub riftstop 关闭"), true);
        src.sendSuccess(() -> Component.literal("§7碰撞箱: 1.8 × 2.8，能被撞/被推；实体 id=" + e.getId()), false);
        return 1;
    }

    /**
     * 直接放一段裂缝动画（**不需要以物换物**，纯调试）：
     * /commhub rift play      收进来的动画
     * /commhub rift play out  送出去的动画
     */
    private static int riftPlay(CommandSourceStack src, boolean outgoing) {
        net.minecraft.server.level.ServerPlayer p = src.getPlayer();
        if (p == null) {
            src.sendFailure(Component.literal("必须由玩家执行"));
            return 0;
        }
        net.minecraft.server.level.ServerLevel level = p.serverLevel();
        var look = p.getLookAngle();
        double x = p.getX() + look.x * 2.5;
        double y = p.getY();
        double z = p.getZ() + look.z * 2.5;
        var held = p.getMainHandItem();
        RiftEntity e = RiftEntity.spawn(level, x, y, z, outgoing,
                held.copy(), Math.max(1, held.getCount()), false,
                held.isEmpty() ? "" : held.getHoverName().getString());
        e.setYRot(p.getYRot() + 180f);
        e.setYHeadRot(e.getYRot());
        src.sendSuccess(() -> Component.literal("§a放了一段裂缝动画（" + (outgoing ? "送出去" : "收进来")
                + "，2.6 秒后自动关闭）"), true);
        return 1;
    }

    /** 调试用：开关「能不能接受自己的悬赏」（OP 专用） */
    private static int selfTrade(CommandSourceStack src, boolean value) {
        com.commhub.network.ModNetworking.ALLOW_SELF_TRADE = value;
        CommHub.LOGGER.info("[commhub] 自交易限制 = {}", value ? "关闭（可自己接受自己的悬赏）" : "开启");
        src.sendSuccess(() -> Component.literal("自交易限制：" + (value
                ? "§a已关闭§r —— 现在可以接受自己的悬赏（调试用）"
                : "§c已开启§r —— 不能接受自己的悬赏")), true);
        return 1;
    }

    private static int lootTest(CommandSourceStack src, int times) {
        try {
            net.minecraft.server.MinecraftServer server = src.getServer();
            net.minecraft.server.level.ServerLevel level = server.overworld();
            net.minecraft.resources.ResourceLocation subId =
                    net.minecraft.resources.ResourceLocation.parse("commhub:inject/automation_core");
            net.minecraft.resources.ResourceLocation chestId =
                    net.minecraft.resources.ResourceLocation.parse("minecraft:chests/simple_dungeon");
            var rl = server.reloadableRegistries();
            net.minecraft.world.level.storage.loot.LootTable sub = rl.getLootTable(
                    net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.LOOT_TABLE, subId));
            net.minecraft.world.level.storage.loot.LootTable chest = rl.getLootTable(
                    net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.LOOT_TABLE, chestId));
            if (sub == null || chest == null) {
                src.sendFailure(Component.literal("【战利品自检】找不到战利品表："
                        + (sub == null ? "注入表缺失 " : "") + (chest == null ? "地牢表缺失" : "")));
                return 0;
            }
            net.minecraft.world.level.storage.loot.LootParams params =
                    new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,
                                    net.minecraft.world.phys.Vec3.atCenterOf(
                                            net.minecraft.core.BlockPos.containing(src.getPosition())))
                            .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
            net.minecraft.world.item.Item core = ModItems.AUTOMATION_CORE.get();
            int subHits = 0, chestHits = 0, chestItems = 0;
            for (int i = 0; i < times; i++) {
                for (net.minecraft.world.item.ItemStack st : sub.getRandomItems(params)) {
                    if (st.is(core)) subHits++;
                }
                for (net.minecraft.world.item.ItemStack st : chest.getRandomItems(params)) {
                    chestItems++;
                    if (st.is(core)) chestHits++;
                }
            }
            int expect = Math.max(1, times / 5);
            String verdict;
            if (subHits == 0) {
                verdict = "§c注入表本身没产出核心 → 子表/条目有问题";
            } else if (chestHits == 0) {
                verdict = "§c宝箱里一次都没出 → 战利品修改器没生效（检查 data/neoforge/loot_modifiers 清单）";
            } else {
                verdict = "§a注入生效 ✓";
            }
            final int f1 = subHits, f2 = chestHits, f3 = chestItems;
            src.sendSuccess(() -> Component.literal("§6【战利品自检】各掷 " + times + " 次"), false);
            src.sendSuccess(() -> Component.literal("§7· 注入表 commhub:inject/automation_core：核心 §f"
                    + f1 + "§7 次（期望约 " + expect + "）"), false);
            src.sendSuccess(() -> Component.literal("§7· 地牢宝箱 minecraft:chests/simple_dungeon：共 §f"
                    + f3 + "§7 件，其中核心 §f" + f2 + "§7 次（期望约 " + expect + "）"), false);
            src.sendSuccess(() -> Component.literal("§7→ " + verdict), false);
            CommHub.LOGGER.info("[commhub] 战利品自检：注入表核心 {} 次 / 宝箱核心 {} 次（各掷 {} 次）", f1, f2, times);
            return f2 > 0 ? 1 : 0;
        } catch (Exception e) {
            src.sendFailure(Component.literal("【战利品自检】出错: " + e));
            return 0;
        }
    }

    private static int runTest(CommandSourceStack src) {
        HubSavedData d = data(src);
        StringBuilder sb = new StringBuilder();
        sb.append("===== mcmcp的仓库 一键自测 =====\n");
        int pass = 0, fail = 0;

        // 共享大仓库被关掉时，物品/电能/液体这三项没法测（它们都基于共享仓库）
        if (!com.commhub.ModConfig.sharedWarehouseEnabled()) {
            sb.append("1~3) 物品/电能/液体: ⏭ 跳过（配置里关闭了共享大仓库）\n");
        }
        // 用共享仓库自测（人人可读写，不需要先建私人仓库）
        var wh = d.getWarehouse();

        // ---- 1. 物品 存/取 ----
        net.minecraft.world.item.ItemStack testItem = new net.minecraft.world.item.ItemStack(
                net.minecraft.world.item.Items.DIAMOND, 64);
        int usedBefore = countUsed(wh);
        net.minecraft.world.item.ItemStack left = (wh instanceof com.commhub.PagedItemHandler ph)
                ? ph.insertStacked(testItem.copy())
                : net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(wh, testItem.copy(), false);
        boolean itemIn = left.isEmpty() && countUsed(wh) >= usedBefore;
        // 取出
        net.minecraft.world.item.ItemStack out = net.minecraft.world.item.ItemStack.EMPTY;
        for (int i = 0; i < wh.getSlots(); i++) {
            if (wh.getStackInSlot(i).getItem() == net.minecraft.world.item.Items.DIAMOND) {
                out = wh.extractItem(i, 64, false);
                break;
            }
        }
        boolean itemOut = out.getCount() == 64;
        sb.append("1) 物品 存入/取出: ").append(itemIn && itemOut ? "✅" : "❌")
          .append(" (存入").append(itemIn ? "OK" : "失败").append(", 取出").append(out.getCount()).append("个)\n");
        if (itemIn && itemOut) pass++; else fail++;

        // ---- 2. 电能 存/取 ----
        int e0 = d.getSharedEnergy();
        d.addSharedEnergy(12345);
        int e1 = d.getSharedEnergy();
        d.removeSharedEnergy(12345);
        int e2 = d.getSharedEnergy();
        boolean energyOk = (e1 - e0 == 12345) && (e1 - e2 == 12345);
        sb.append("2) 电能 充入/放出: ").append(energyOk ? "✅" : "❌")
          .append(" (").append(e0).append(" → ").append(e1).append(" → ").append(e2).append(" FE)\n");
        if (energyOk) pass++; else fail++;

        // ---- 3. 液体 存/取 ----
        net.neoforged.neoforge.fluids.FluidStack f0 = d.getSharedFluid().copy();
        net.neoforged.neoforge.fluids.FluidStack water =
                new net.neoforged.neoforge.fluids.FluidStack(
                        net.minecraft.world.level.material.Fluids.WATER, 1000);
        boolean fluidOk;
        if (f0.isEmpty() || f0.is(water.getFluid())) {
            int before = d.getSharedFluid().getAmount();
            boolean added = d.addSharedFluid(water);
            int after = d.getSharedFluid().getAmount();
            d.removeSharedFluid(1000);
            int back = d.getSharedFluid().getAmount();
            fluidOk = added && (after - before == 1000) && (back == before);
            sb.append("3) 液体 灌入/抽出: ").append(fluidOk ? "✅" : "❌")
              .append(" (").append(before).append(" → ").append(after).append(" → ").append(back).append(" mB)\n");
        } else {
            fluidOk = true;
            sb.append("3) 液体: ⏭ 跳过（共享仓库已存了其他液体: ").append(f0.getDescriptionId()).append("）\n");
            pass++;
        }
        if (fluidOk && !f0.isEmpty() && f0.is(water.getFluid())) pass++;
        else if (!fluidOk) fail++;

        // ---- 4. 好友 加/删（测试模式） ----
        java.util.UUID me = src.getPlayer() == null ? null : src.getPlayer().getUUID();
        if (me != null) {
            boolean selfTest = ModConfig.allowSelfTest();
            if (!selfTest) {
                sb.append("4) 好友 加/删: ⏭ 跳过（配置里 allow_self_test=false）\n");
            } else {
                d.addFriendRequest(me, me);
                d.acceptFriendRequest(me, me);
                boolean friended = d.areFriends(me, me);
                d.removeFriend(me, me);
                boolean unfriended = !d.areFriends(me, me);
                boolean friendOk = friended && unfriended;
                sb.append("4) 好友 加/删: ").append(friendOk ? "✅" : "❌")
                  .append(" (加=").append(friended ? "OK" : "失败").append(", 删=").append(unfriended ? "OK" : "失败").append(")\n");
                if (friendOk) pass++; else fail++;
            }

            // ---- 5. 私聊 发/收（测试模式） ----
            if (selfTest) {
                long ts = System.currentTimeMillis();
                ChatEntry entry = new ChatEntry(me, src.getTextName(), me, "test", ts, false);
                d.addPrivate(entry);
                boolean got = false;
                for (ChatEntry c : d.getPrivateLog()) {
                    if ("test".equals(c.message()) && me.equals(c.sender()) && me.equals(c.recipient())) { got = true; break; }
                }
                sb.append("5) 私聊 发送\"test\"/收到: ").append(got ? "✅" : "❌")
                  .append(" (记录数 ").append(d.getPrivateLog().size()).append(")\n");
                if (got) pass++; else fail++;
            } else {
                sb.append("5) 私聊: ⏭ 跳过（需 allow_self_test=true）\n");
            }
            // ---- 6. 管理员 给予/转让（测试模式）----
            if (selfTest) {
                String testName = "__test__";
                // 先清理可能残留的旧测试仓库
                d.removePrivateWarehouseForTest(testName);
                HubSavedData.PrivateWarehouse tw = d.createPrivateWarehouse(me, testName);
                if (tw == null) {
                    sb.append("6) 管理员 给予/转让: ❌（临时仓库创建失败）\n");
                    fail++;
                } else {
                    boolean allOk = true;
                    // a) 新建仓库无管理员，第三格应是「给予」（isgrant=true）
                    net.minecraft.world.item.ItemStack adminTool = tw.toolSlots[2];
                    net.minecraft.nbt.CompoundTag tg = adminTool.getOrDefault(
                            net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                            net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
                    boolean grant0 = tg.contains("isgrant") && tg.getBoolean("isgrant");
                    // b) 给予自己管理员
                    int r1 = d.grantOrTransferAdmin(testName, me, me);
                    boolean granted = r1 == 1 && me.equals(tw.admin);
                    // c) 给予后第三格自动变「转让」（isgrant=false）
                    tg = tw.toolSlots[2].getOrDefault(
                            net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                            net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
                    boolean nowTransfer = tg.contains("isgrant") && !tg.getBoolean("isgrant");
                    // d) 转让给自己（再来一次，测试模式放行）
                    int r2 = d.grantOrTransferAdmin(testName, me, me);
                    boolean transferred = r2 == 1 && me.equals(tw.admin);
                    allOk = grant0 && granted && nowTransfer && transferred;
                    sb.append("6) 管理员 给予/转让: ").append(allOk ? "✅" : "❌")
                      .append(" (初始给予标记=").append(grant0 ? "OK" : "失败")
                      .append(", 给予=").append(granted ? "OK" : "失败")
                      .append(", 自动变转让=").append(nowTransfer ? "OK" : "失败")
                      .append(", 转让=").append(transferred ? "OK" : "失败").append(")\n");
                    if (allOk) pass++; else fail++;
                    // e) 清理：置空 admin 并删除临时仓库
                    tw.admin = null;
                    d.removePrivateWarehouseForTest(testName);
                }
            } else {
                sb.append("6) 管理员 给予/转让: ⏭ 跳过（需 allow_self_test=true）\n");
            }
        } else {
            sb.append("4) 好友/私聊/管理员: ⏭ 跳过（请玩家执行，控制台无身份）\n");
        }

        sb.append("--------------------------------\n");
        sb.append("结果: 通过 ").append(pass).append(" 项, 失败 ").append(fail).append(" 项");
        if (fail == 0) sb.append("  →  🎉 全部正常");
        sb.append("\n================================");
        src.sendSuccess(() -> Component.literal(sb.toString()), false);
        return fail == 0 ? 1 : 0;
    }

    private static int countUsed(net.neoforged.neoforge.items.IItemHandler wh) {
        int n = 0;
        for (int i = 0; i < wh.getSlots(); i++) {
            if (!wh.getStackInSlot(i).isEmpty()) n++;
        }
        return n;
    }
}
