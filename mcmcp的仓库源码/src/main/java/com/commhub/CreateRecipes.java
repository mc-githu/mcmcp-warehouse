package com.commhub;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 直接读数据包里的 Create 加工配方（不需要在编译期依赖 Create）。
 *
 * <p>目前支持两类：</p>
 * <ul>
 *   <li>{@code create:cutting} —— 切割：原木 → 去皮原木 / 木板（动力锯卡片用）</li>
 *   <li>{@code create:item_application} —— 物品应用：把某个物品按到某个方块上
 *       （例如 <b>安山合金 + 去皮原木 → 安山机壳</b>，机械手卡片用）</li>
 * </ul>
 */
public final class CreateRecipes {

    /** 一个"材料要求"：物品 / 标签 / 任选其一 */
    public record Spec(String item, String tag, List<Spec> any) {
        public boolean test(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return false;
            if (item != null) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (id != null && id.toString().equals(item)) return true;
            }
            if (tag != null) {
                try {
                    TagKey<Item> key = TagKey.create(Registries.ITEM, ResourceLocation.parse(tag));
                    if (stack.is(key)) return true;
                } catch (Exception ignored) {
                }
            }
            if (any != null) {
                for (Spec s : any) if (s.test(stack)) return true;
            }
            return false;
        }
    }

    public record Cutting(Spec in, List<ItemStack> out) {
    }

    public record Application(Spec target, Spec applied, ItemStack out) {
    }

    private static List<Cutting> cuttings = new ArrayList<>();
    private static List<Application> applications = new ArrayList<>();
    private static Object loadedFor = null;
    private static boolean loadedOnce = false;

    private CreateRecipes() {
    }

    private static Spec spec(JsonElement e) {
        if (e == null || e.isJsonNull()) return null;
        if (e.isJsonArray()) {
            List<Spec> list = new ArrayList<>();
            for (JsonElement c : e.getAsJsonArray()) {
                Spec s = spec(c);
                if (s != null) list.add(s);
            }
            return list.isEmpty() ? null : new Spec(null, null, list);
        }
        if (!e.isJsonObject()) return null;
        JsonObject o = e.getAsJsonObject();
        if (o.has("item")) return new Spec(o.get("item").getAsString(), null, null);
        if (o.has("tag")) return new Spec(null, o.get("tag").getAsString(), null);
        // neoforge:compound 之类的嵌套写法
        if (o.has("ingredients")) {
            Spec inner = spec(o.get("ingredients"));
            if (inner != null) return inner;
        }
        if (o.has("id")) return new Spec(o.get("id").getAsString(), null, null);
        return null;
    }

    private static ItemStack result(JsonElement e) {
        if (e == null || !e.isJsonObject()) return ItemStack.EMPTY;
        JsonObject o = e.getAsJsonObject();
        String id = o.has("id") ? o.get("id").getAsString() : (o.has("item") ? o.get("item").getAsString() : null);
        if (id == null) return ItemStack.EMPTY;
        try {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
            if (item == null) return ItemStack.EMPTY;
            int count = o.has("count") ? Math.max(1, o.get("count").getAsInt()) : 1;
            return new ItemStack(item, Math.min(count, 99));
        } catch (Exception ex) {
            return ItemStack.EMPTY;
        }
    }

    /** 数据包重载（/reload、换存档等）后清掉缓存，重新读 */
    public static void invalidate() {
        loadedOnce = false;
        loadedFor = null;
    }

    /**
     * 读配方。先只看 cutting/ 和 item_application/ 两个目录（快）；
     * 一条都没读到再全量扫描（稳，兼容不同目录拼法）。
     */
    public static void ensureLoaded(MinecraftServer server) {
        if (server == null) return;
        if (loadedFor == server && loadedOnce) return;
        loadedFor = server;
        loadedOnce = true;
        List<Cutting> cut = new ArrayList<>();
        List<Application> app = new ArrayList<>();
        int scanned = 0;
        try {
            Map<ResourceLocation, net.minecraft.server.packs.resources.Resource> files =
                    server.getResourceManager().listResources("recipe", rl -> rl.getPath().endsWith(".json"));
            scanned = files.size();
            parse(files, cut, app, true);          // 快路径：只读两个目录
            if (cut.isEmpty() && app.isEmpty()) {  // 没读到 → 全量兜底
                parse(files, cut, app, false);
            }
        } catch (Exception ignored) {
        }
        cuttings = cut;
        applications = app;
        CommHub.LOGGER.info("[commhub] 读到 Create 加工配方：切割 {} 条，物品应用 {} 条（共扫了 {} 个配方文件）",
                cut.size(), app.size(), scanned);
    }

    private static void parse(Map<ResourceLocation, net.minecraft.server.packs.resources.Resource> files,
                              List<Cutting> cut, List<Application> app, boolean onlyTwoFolders) {
        cut.clear();
        app.clear();
        for (Map.Entry<ResourceLocation, net.minecraft.server.packs.resources.Resource> e : files.entrySet()) {
            String path = e.getKey().getPath();
            if (onlyTwoFolders && !(path.startsWith("cutting/") || path.startsWith("item_application/"))) continue;
            try (BufferedReader reader = e.getValue().openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                String type = json.has("type") ? json.get("type").getAsString() : "";
                JsonArray ingredients = json.has("ingredients") ? json.getAsJsonArray("ingredients") : new JsonArray();
                JsonArray results = json.has("results") ? json.getAsJsonArray("results") : new JsonArray();
                if ("create:cutting".equals(type) && !ingredients.isEmpty()) {
                    Spec in = spec(ingredients.get(0));
                    List<ItemStack> outs = new ArrayList<>();
                    for (JsonElement r : results) {
                        ItemStack st = result(r);
                        if (!st.isEmpty()) outs.add(st);
                    }
                    if (in != null && !outs.isEmpty()) cut.add(new Cutting(in, outs));
                } else if ("create:item_application".equals(type) && ingredients.size() >= 2) {
                    Spec target = spec(ingredients.get(0));
                    Spec applied = spec(ingredients.get(1));
                    ItemStack out = results.isEmpty() ? ItemStack.EMPTY : result(results.get(0));
                    if (target != null && applied != null && !out.isEmpty()) {
                        app.add(new Application(target, applied, out));
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    public static List<Cutting> cuttings() {
        return cuttings;
    }

    public static List<Application> applications() {
        return applications;
    }
}
