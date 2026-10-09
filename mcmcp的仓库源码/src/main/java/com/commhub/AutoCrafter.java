package com.commhub;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 自动合成：按合成升级卡上配置的配方，从仓库扣材料、把产物放回仓库。
 * 材料不够 / 配方无效 / 仓库放不下 → 直接跳过（自动停止），不会半途扣一半材料。
 */
public final class AutoCrafter {

    /** 配方查找缓存：大整合包里 CRAFTING 配方有几千条，每 0.5 秒重新匹配一遍会卡 */
    private static final java.util.Map<String, Optional<RecipeHolder<CraftingRecipe>>> RECIPE_CACHE = new java.util.HashMap<>();
    private static Object cachedServer = null;

    private AutoCrafter() {
    }

    /** 清掉配方缓存（数据包 /reload 后配方可能变了） */
    public static void clearCache() {
        RECIPE_CACHE.clear();
    }

    /** 网格签名（物品 + 数量） */
    private static String signature(List<ItemStack> items) {
        StringBuilder sb = new StringBuilder();
        for (ItemStack s : items) {
            if (s == null || s.isEmpty()) {
                sb.append('-');
            } else {
                sb.append(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()))
                  .append(':').append(s.getCount());
            }
            sb.append('|');
        }
        return sb.toString();
    }

    private static Optional<RecipeHolder<CraftingRecipe>> find(ServerLevel level, List<ItemStack> items) {
        Object server = level.getServer();
        if (server != cachedServer) {
            RECIPE_CACHE.clear();
            cachedServer = server;
        }
        String key = signature(items);
        Optional<RecipeHolder<CraftingRecipe>> hit = RECIPE_CACHE.get(key);
        if (hit != null) return hit;
        Optional<RecipeHolder<CraftingRecipe>> found =
                level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, items), level);
        if (RECIPE_CACHE.size() > 1024) RECIPE_CACHE.clear();
        RECIPE_CACHE.put(key, found);
        return found;
    }

    /** 同一个物品（含组件） */
    private static boolean sameAs(ItemStack a, ItemStack b) {
        return a != null && b != null && !a.isEmpty() && !b.isEmpty()
                && ItemStack.isSameItemSameComponents(a, b);
    }

    private static int countOf(PagedItemHandler wh, ItemStack sample) {
        int n = 0;
        for (int i = 0; i < wh.getSlots(); i++) {
            ItemStack s = wh.getStackInSlot(i);
            if (sameAs(s, sample)) n += s.getCount();
        }
        return n;
    }

    private static boolean consumeOne(PagedItemHandler wh, ItemStack sample) {
        for (int i = 0; i < wh.getSlots(); i++) {
            ItemStack s = wh.getStackInSlot(i);
            if (sameAs(s, sample)) {
                ItemStack got = wh.extractItem(i, 1, false);
                return !got.isEmpty();
            }
        }
        return false;
    }

    /** 动力锯去皮：原木/木头 → 去皮原木/去皮木头（按命名规律，不依赖模组配方） */
    private static ItemStack stripBySaw(ItemStack input) {
        if (input == null || input.isEmpty()) return ItemStack.EMPTY;
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(input.getItem());
        if (id == null || id.getPath().startsWith("stripped_")) return ItemStack.EMPTY;
        Item stripped = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "stripped_" + id.getPath()));
        if (stripped == null || stripped == net.minecraft.world.item.Items.AIR) return ItemStack.EMPTY;
        return new ItemStack(stripped);
    }

    /** 用斧头给原木去皮：oak_log → stripped_oak_log（原版/多数模组的命名规律） */
    private static ItemStack stripWithAxe(ItemStack held, ItemStack input) {
        if (held == null || input == null || held.isEmpty() || input.isEmpty()) return ItemStack.EMPTY;
        if (!(held.getItem() instanceof net.minecraft.world.item.AxeItem)) return ItemStack.EMPTY;
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(input.getItem());
        if (id == null) return ItemStack.EMPTY;
        if (id.getPath().startsWith("stripped_")) return ItemStack.EMPTY;
        Item stripped = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "stripped_" + id.getPath()));
        if (stripped == null || stripped == net.minecraft.world.item.Items.AIR) return ItemStack.EMPTY;
        return new ItemStack(stripped);
    }

    /** 按仓库物品找一个匹配的「物品应用」配方（例如 安山合金 + 去皮原木 → 安山机壳） */
    private static CreateRecipes.Application findApplication(ItemStack held, ItemStack input) {
        for (CreateRecipes.Application a : CreateRecipes.applications()) {
            if (a.applied().test(held) && a.target().test(input)) return a;
        }
        return null;
    }

    /**
     * 机械手卡片（右键模式）：
     * <ul>
     *   <li>斧头 + 原木 → 去皮原木</li>
     *   <li>安山合金 + 去皮原木 → 安山机壳（Create 的 item_application）</li>
     * </ul>
     * 只做「使用物品」，不做攻击。
     */
    public static boolean tickDeployer(ServerLevel level, PagedItemHandler wh, ItemStack[] cardSlots) {
        if (level == null || wh == null || cardSlots == null) return false;
        for (ItemStack card : cardSlots) {
            if (card == null || card.isEmpty()) continue;
            if (!(card.getItem() instanceof SimpleCardItem it) || it.kind() != SimpleCard.Kind.DEPLOYER) continue;
            ItemStack a = SimpleCard.get(card, 0, level.registryAccess());
            ItemStack b = SimpleCard.get(card, 1, level.registryAccess());
            if (a.isEmpty() || b.isEmpty()) {
                warnThrottled("机械手卡片：两个格子都要放东西（一般一格放斧头、一格放原木）");
                continue;
            }
            // 两个格子都试一遍，放哪格都行
            if (tryDeploy(wh, a, b) || tryDeploy(wh, b, a)) return true;
            warnThrottled("机械手卡片（只做右键/物品应用）：没匹配上。手上=" + a.getHoverName().getString()
                    + "，输入=" + b.getHoverName().getString()
                    + "；已读到物品应用配方 " + CreateRecipes.applications().size() + " 条"
                    + "；仓库里" + (countOf(wh, a) < 1 ? "缺[" + a.getHoverName().getString() + "] " : "")
                    + (countOf(wh, b) < 1 ? "缺[" + b.getHoverName().getString() + "]" : ""));
        }
        return false;
    }

    /**
     * 机械手只用「右键」：把 held 按到 input 上（Create 的 item_application）。
     * 例如 安山合金 + 去皮原木 → 安山机壳。不做攻击、不做破坏。
     */
    private static boolean tryDeploy(PagedItemHandler wh, ItemStack held, ItemStack input) {
        CreateRecipes.Application app = findApplication(held, input);
        if (app == null) return false;
        if (countOf(wh, input) < 1 || countOf(wh, held) < 1) return false;
        if (!ItemHandlerHelper.insertItemStacked(wh, app.out().copy(), true).isEmpty()) return false;
        consumeOne(wh, input);
        consumeOne(wh, held);
        wh.insertStacked(app.out().copy());
        return true;
    }

    private static long lastWarn = 0;

    /** 诊断日志：10 秒最多打一条，方便排查"为什么不动" */
    private static void warnThrottled(String msg) {
        long now = System.currentTimeMillis();
        if (now - lastWarn < 10_000L) return;
        lastWarn = now;
        CommHub.LOGGER.info("[commhub] {}", msg);
    }

    /**
     * 动力锯卡片：按**切割配方**加工（输入物品 → 卡上选定的产物）。
     * 一个输入对应多个产物时，用卡上选的产物来定位具体配方，不会选错。
     */
    public static boolean tickSaw(ServerLevel level, PagedItemHandler wh, ItemStack[] cardSlots) {
        if (level == null || wh == null || cardSlots == null) return false;
        // 标签：某张卡这轮做不了（没材料/放不下）只跳过它，不影响后面卡槽里的卡
        cards:
        for (ItemStack card : cardSlots) {
            if (card == null || card.isEmpty()) continue;
            if (!(card.getItem() instanceof SimpleCardItem it) || it.kind() != SimpleCard.Kind.SAW) continue;
            ItemStack in = SimpleCard.get(card, 0, level.registryAccess());
            ItemStack out = SimpleCard.get(card, 1, level.registryAccess());
            if (in.isEmpty() || out.isEmpty()) continue;

            // ---- 1) 原版切割配方 ----
            net.minecraft.world.item.crafting.SingleRecipeInput input =
                    new net.minecraft.world.item.crafting.SingleRecipeInput(in.copyWithCount(1));
            for (net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.StonecutterRecipe> holder
                    : level.getRecipeManager().getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.STONECUTTING)) {
                if (!holder.value().matches(input, level)) continue;
                ItemStack res = holder.value().getResultItem(level.registryAccess());
                if (res.isEmpty() || !ItemStack.isSameItemSameComponents(res, out)) continue;
                if (countOf(wh, in) < 1) continue cards;
                if (!ItemHandlerHelper.insertItemStacked(wh, res.copy(), true).isEmpty()) continue cards;
                consumeOne(wh, in);
                wh.insertStacked(res.copy());
                return true;
            }

            // ---- 2) Create 的切割配方（可能一次出多个产物 = 副产物）----
            for (CreateRecipes.Cutting c : CreateRecipes.cuttings()) {
                if (!c.in().test(in)) continue;
                boolean hasOut = false;
                for (ItemStack o : c.out()) {
                    if (ItemStack.isSameItemSameComponents(o, out)) { hasOut = true; break; }
                }
                if (!hasOut) continue;
                if (countOf(wh, in) < 1) continue cards;
                // 主产物 = 你选定的那个；其余是副产物（配置里可以关掉）
                List<ItemStack> give = new ArrayList<>();
                give.add(out.copy());
                if (ModConfig.byproductsEnabled()) {
                    for (ItemStack o : c.out()) {
                        if (ItemStack.isSameItemSameComponents(o, out)) continue;
                        give.add(o.copy());
                    }
                }
                boolean room = true;
                for (ItemStack o : give) {
                    if (!ItemHandlerHelper.insertItemStacked(wh, o.copy(), true).isEmpty()) { room = false; break; }
                }
                if (!room) continue cards;
                consumeOne(wh, in);
                for (ItemStack o : give) wh.insertStacked(o.copy());
                return true;
            }

            // ---- 3) 兜底：动力锯给原木去皮（模组没提供配方时也能用）----
            ItemStack stripped = stripBySaw(in);
            if (!stripped.isEmpty() && ItemStack.isSameItemSameComponents(stripped, out)) {
                if (countOf(wh, in) < 1) continue cards;
                if (!ItemHandlerHelper.insertItemStacked(wh, stripped.copy(), true).isEmpty()) continue cards;
                consumeOne(wh, in);
                wh.insertStacked(stripped.copy());
                return true;
            }

            // 诊断：配置了但没配方/没材料时说明原因
            if (countOf(wh, in) < 1) {
                warnThrottled("动力锯卡片：仓库里没有「" + in.getHoverName().getString() + "」");
            } else {
                warnThrottled("动力锯卡片：没找到匹配配方（" + in.getHoverName().getString()
                        + " → " + out.getHoverName().getString()
                        + "）；检查产物是否选对（去皮原木要选「去皮原木」）");
            }
        }
        return false;
    }

    /**
     * 尝试合成一次：先扫一遍仓库，看里面有没有「合成升级卡」（卡是直接放进仓库的，
     * 不占界面格子），有就按卡上的配方合成。
     */
    public static boolean tickStorage(ServerLevel level, PagedItemHandler wh) {
        if (level == null || wh == null) return false;
        java.util.List<ItemStack> cards = new java.util.ArrayList<>();
        for (int i = 0; i < wh.getSlots() && cards.size() < HubSavedData.CARD_SLOTS; i++) {
            ItemStack st = wh.getStackInSlot(i);
            if (!st.isEmpty() && st.getItem() instanceof CraftingUpgradeCardItem) cards.add(st);
        }
        if (cards.isEmpty()) return false;
        return tick(level, wh, cards.toArray(new ItemStack[0]));
    }

    /**
     * 尝试合成一次（每 10 tick 调一次 = 约 0.5 秒一个）。
     * 一组卡槽里只要合成成功一个就返回，避免一次 tick 里连做很多。
     */
    public static boolean tick(ServerLevel level, PagedItemHandler wh, ItemStack[] cardSlots) {
        if (level == null || wh == null || cardSlots == null) return false;
        for (ItemStack card : cardSlots) {
            if (card == null || card.isEmpty()) continue;
            if (!(card.getItem() instanceof CraftingUpgradeCardItem)) continue;
            List<CraftingCard.Target> targets;
            try {
                targets = CraftingCard.read(card, level.registryAccess());
            } catch (Exception e) {
                continue;
            }
            for (CraftingCard.Target t : targets) {
                if (t == null || !t.hasGrid()) continue;
                if (tryOne(level, wh, t)) return true;
            }
        }
        return false;
    }

    private static boolean tryOne(ServerLevel level, PagedItemHandler wh, CraftingCard.Target t) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack s = t.grid[i];
            items.add((s == null || s.isEmpty()) ? ItemStack.EMPTY : s.copyWithCount(1));
        }
        CraftingInput input = CraftingInput.of(3, 3, items);

        Optional<RecipeHolder<CraftingRecipe>> match = find(level, items);
        if (match.isEmpty()) return false;
        CraftingRecipe recipe = match.get().value();

        // 统计需要的材料（同一种物品出现在多个格子就累加）
        Map<Item, Integer> need = new LinkedHashMap<>();
        for (ItemStack s : t.grid) {
            if (s != null && !s.isEmpty()) need.merge(s.getItem(), 1, Integer::sum);
        }
        if (need.isEmpty()) return false;
        if (!hasAll(wh, need)) return false;   // 材料不够 → 跳过（自动停）

        ItemStack result = recipe.assemble(input, level.registryAccess());
        if (result.isEmpty()) return false;
        // 产物放不下（理论上分页仓库很少满）就不做，避免材料白白扣掉
        if (!ItemHandlerHelper.insertItemStacked(wh, result.copy(), true).isEmpty()) return false;

        consume(wh, need);
        ItemStack leftover = wh.insertStacked(result.copy());
        if (!leftover.isEmpty()) {
            // 极端情况：放不下就把材料还回去
            for (Map.Entry<Item, Integer> e : need.entrySet()) {
                ItemStack back = new ItemStack(e.getKey(), Math.min(e.getValue(), 99));
                wh.insertStacked(back);
            }
            return false;
        }
        // 合成剩余的容器（桶、瓶子之类）也放回仓库
        for (ItemStack rem : recipe.getRemainingItems(input)) {
            if (rem != null && !rem.isEmpty()) wh.insertStacked(rem.copy());
        }
        return true;
    }

    private static boolean hasAll(PagedItemHandler wh, Map<Item, Integer> need) {
        for (Map.Entry<Item, Integer> e : need.entrySet()) {
            int have = 0;
            for (int i = 0; i < wh.getSlots(); i++) {
                ItemStack s = wh.getStackInSlot(i);
                if (!s.isEmpty() && s.getItem() == e.getKey()) have += s.getCount();
                if (have >= e.getValue()) break;
            }
            if (have < e.getValue()) return false;
        }
        return true;
    }

    private static void consume(PagedItemHandler wh, Map<Item, Integer> need) {
        for (Map.Entry<Item, Integer> e : need.entrySet()) {
            int left = e.getValue();
            for (int i = 0; i < wh.getSlots() && left > 0; i++) {
                ItemStack s = wh.getStackInSlot(i);
                if (s.isEmpty() || s.getItem() != e.getKey()) continue;
                int take = Math.min(left, s.getCount());
                ItemStack got = wh.extractItem(i, take, false);
                left -= got.getCount();
            }
        }
    }
}
