package com.commhub;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * CommHub 配置文件。
 * 配置文件位置：.minecraft/config/commhub-server.toml
 */
public class ModConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<String> SHARED_WAREHOUSE_SIZE;
    public static final ModConfigSpec.ConfigValue<String> PRIVATE_WAREHOUSE_SIZE;
    public static final ModConfigSpec.BooleanValue ENABLE_SHARED;
    public static final ModConfigSpec.BooleanValue ENABLE_BYPRODUCTS;
    public static final ModConfigSpec.BooleanValue ALLOW_SELF_TEST;

    /** 客户端配置：记住「上次打开的标签页」，按 K 打开时直接回到那里 */
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec.IntValue LAST_MODE;
    public static final ModConfigSpec.ConfigValue<String> LAST_WAREHOUSE;

    /** 「无限」时使用的格数上限（约 990 万物品，足够视为无限） */
    public static final int INFINITE_SLOTS = 100000;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment(
                "============================ CommHub 共享通信与仓库 ============================",
                "  mcmcp的仓库 —— 共享/私人仓库、输入输出方块、好友私聊、以物换物、",
                "  合成升级卡与自动化卡片（机械手 / 动力锯）。",
                "",
                "  配置文件：config/commhub-server.toml（服务端）",
                "            config/commhub-client.toml（客户端，只记「上次看的标签页」）",
                "",                "",
                "CommHub 共享通信与仓库 配置",
                "",
                "【修改后怎么生效】",
                "· 这个文件是服务端配置（config/commhub-server.toml）",
                "· 改完保存后，单人游戏退出世界再进即可；服务器用 /reload 或重启",
                "",
                "【各个开关是干嘛的】",
                "· shared_warehouse_size / private_warehouse_size：仓库初始开几页（仓库是分页的，装满会自动加页，所以填 wuxian 就行）",
                "· enable_shared_warehouse：公共仓库（所有人共用的那个）要不要开。关掉后界面不显示共享大仓库，输入/输出方块也不能指向它",
                "· allow_self_test：测试用，允许对自己加好友/发私聊/给自己管理员权限",
                "",
                "【自动化核心怎么获得（写在配置里，JEI 里不显示）】",
                "· 只能在遗迹宝箱里找到：地牢、废弃矿井、沙漠神殿、丛林神庙、雪屋、林地府邸、",
                "  掠夺者前哨、沉船宝藏、埋藏的宝藏、下界要塞、要塞、远古城市、末地城、",
                "  堡垒遗迹（4 种）、试炼密室；每个宝箱约 20% 概率出 1 个",
                "· 也可以用复刻配方：正中放自动化核心、上方放下界合金锭、其余 7 格放钻石 → 出 2 个",
                "· 想改宝箱列表或概率：改模组数据包里的",
                "  data/commhub/loot_modifiers/automation_core_in_ruins.json（宝箱列表）",
                "  data/commhub/loot_table/inject/automation_core.json（20% 概率在这里，权重 1:空 4）",
                "· 游戏里可以用 /commhub loottest 自检这条注入到底生不生效")
                .push("warehouse");

        SHARED_WAREHOUSE_SIZE = builder
                .comment(
                        "共享大仓库的【初始页数】换算值（仓库现在是分页存储，容量无限、装满了自动加页）。",
                        "填 wuxian / 无限 / infinite / unlimited = 只开 1 页，按需自动扩页（推荐）；",
                        "填数字 = 按这个格数换算成初始页数（每页 54 格），例如 2700 会先开 50 页。",
                        "空页在存档时会自动丢弃，所以开多了不会占存档。")
                .define("shared_warehouse_size", "wuxian");

        PRIVATE_WAREHOUSE_SIZE = builder
                .comment(
                        "私人仓库的【初始页数】换算值（同样是分页存储，容量无限）。",
                        "填 wuxian = 只开 1 页按需扩页（推荐）；填数字 = 按格数换算初始页数。")
                .define("private_warehouse_size", "wuxian");

        ENABLE_SHARED = builder
                .comment(
                        "是否启用「共享大仓库」（所有人免费共用的那个仓库）。",
                        "true = 启用（默认）；",
                        "false = 完全关闭：界面不显示共享大仓库标签页、输入/输出方块也不能指向它。",
                        "有些整合包不需要这种公共仓库，可以关掉。",
                        "注意：关掉后已经存在共享仓库里的东西不会消失，只是界面和方块都用不了它。")
                .define("enable_shared_warehouse", false);

        ENABLE_BYPRODUCTS = builder
                .comment(
                        "加工时要不要产出「副产物」。",
                        "有些模组给配方加了副产物（例如锯原木去皮时多给一份木屑）。",
                        "true = 副产物也放进仓库（默认）；",
                        "false = 只产出你选定的主产物，副产物不产生。")
                .define("enable_byproducts", true);

        ALLOW_SELF_TEST = builder
                .comment(
                        "测试用：允许对自己加好友、给自己发私聊。",
                        "单人测试私聊/好友功能时打开；正常游玩请保持 false。")
                .define("allow_self_test", false);

        builder.pop();
        SPEC = builder.build();

        // ---- 客户端配置（config/commhub-client.toml）----
        ModConfigSpec.Builder cb = new ModConfigSpec.Builder();
        cb.comment("这里只是记住「你上次打开仓库界面看的是哪个标签页」，按 K 打开时直接回去，不用每次找。");
        LAST_MODE = cb.comment("上次的标签页：0=共享大仓库 1=私人仓库 2=私聊 3=传物品（-1=没记录）")
                .defineInRange("last_tab", -1, -1, 3);
        LAST_WAREHOUSE = cb.comment("上次看的私人仓库名字（空=没记录）")
                .define("last_warehouse", "");
        CLIENT_SPEC = cb.build();
    }

    /** 记住这次看的标签页（客户端调用） */
    public static void rememberTab(int mode, String name) {
        try {
            LAST_MODE.set(mode);
            LAST_WAREHOUSE.set(name == null ? "" : name);
            CLIENT_SPEC.save();
        } catch (Exception ignored) {
        }
    }

    /** 上次的标签页（客户端调用；出错就当没记录） */
    public static int lastTab() {
        try {
            return LAST_MODE.get();
        } catch (Exception e) {
            return -1;
        }
    }

    public static String lastWarehouse() {
        try {
            return LAST_WAREHOUSE.get();
        } catch (Exception e) {
            return "";
        }
    }

    /** 解析配置值：返回 INFINITE_SLOTS 表示无限，否则返回具体格数（最小 1，最大 INFINITE_SLOTS）。 */
    private static int parse(String value) {
        String s = value == null ? "" : value.trim().toLowerCase();
        if (s.equals("wuxian") || s.equals("无限") || s.equals("infinite") || s.equals("unlimited") || s.equals("-1")) {
            return INFINITE_SLOTS;
        }
        try {
            int n = Integer.parseInt(s);
            return Math.max(1, Math.min(n, INFINITE_SLOTS));
        } catch (NumberFormatException e) {
            return 2700; // 配置写错时回退到默认
        }
    }

    public static int getSharedSlots() {
        return parse(SHARED_WAREHOUSE_SIZE.get());
    }

    public static int getPrivateSlots() {
        return parse(PRIVATE_WAREHOUSE_SIZE.get());
    }

    /** 共享仓库的初始页数：wuxian 只开 1 页，按需自动扩页 */
    public static int getSharedInitialPages() {
        return initialPages(parse(SHARED_WAREHOUSE_SIZE.get()));
    }

    /** 私人仓库的初始页数 */
    public static int getPrivateInitialPages() {
        return initialPages(parse(PRIVATE_WAREHOUSE_SIZE.get()));
    }

    /** 无限 → 1 页（自动扩页）；具体格数 → 换算成页数（每页 54 格） */
    private static int initialPages(int slots) {
        if (slots >= INFINITE_SLOTS) return 1;
        return Math.max(1, (int) Math.ceil(slots / 54.0));
    }

    /** 加工是否产出副产物 */
    public static boolean byproductsEnabled() {
        try {
            return ENABLE_BYPRODUCTS.get();
        } catch (Exception e) {
            return true;
        }
    }

    /** 是否启用共享大仓库 */
    public static boolean sharedWarehouseEnabled() {
        try {
            return ENABLE_SHARED.get();
        } catch (Exception e) {
            return true;
        }
    }

    /** 是否允许自我操作（测试用） */
    public static boolean allowSelfTest() {
        try {
            return ALLOW_SELF_TEST.get();
        } catch (Exception e) {
            return false;
        }
    }
}
