package com.commhub;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, CommHub.MODID);

    public static final Supplier<Item> MEMBERSHIP_TOKEN =
            ITEMS.register("membership_token", () -> new MembershipTokenItem(new Item.Properties().stacksTo(1)));

    public static final Supplier<Item> ADD_MEMBER_TOOL =
            ITEMS.register("add_member_tool",
                    () -> new PermissionToolItem(PermissionToolItem.Kind.ADD, new Item.Properties().stacksTo(1)));

    public static final Supplier<Item> REMOVE_MEMBER_TOOL =
            ITEMS.register("remove_member_tool",
                    () -> new PermissionToolItem(PermissionToolItem.Kind.REMOVE, new Item.Properties().stacksTo(1)));

    public static final Supplier<Item> TRANSFER_ADMIN_TOOL =
            ITEMS.register("transfer_admin_tool",
                    () -> new PermissionToolItem(PermissionToolItem.Kind.ADMIN, new Item.Properties().stacksTo(1)));

    /** 自动化核心：通用自动化元件（黑曜石 + 钻石），自动化设备与升级卡都用它当材料 */
    public static final Supplier<Item> AUTOMATION_CORE =
            ITEMS.register("automation_core", () -> new Item(new Item.Properties()));

    /** 合成升级卡：装进仓库卡槽后，仓库会按卡上的配方自动合成 */
    public static final Supplier<Item> CRAFTING_UPGRADE_CARD =
            ITEMS.register("crafting_upgrade_card",
                    () -> new CraftingUpgradeCardItem(new Item.Properties().stacksTo(16)));

    /** 机械手卡片：配置「左键/右键 + 用哪个物品」 */
    public static final Supplier<Item> DEPLOYER_CARD =
            ITEMS.register("deployer_card",
                    () -> new SimpleCardItem(SimpleCard.Kind.DEPLOYER, new Item.Properties().stacksTo(16)));

    /** 动力锯卡片：配置「输入物品 + 选定产物」，按切割配方加工 */
    public static final Supplier<Item> SAW_CARD =
            ITEMS.register("saw_card",
                    () -> new SimpleCardItem(SimpleCard.Kind.SAW, new Item.Properties().stacksTo(16)));

    public static final Supplier<BlockItem> WAREHOUSE_INPUT =
            ITEMS.register("warehouse_input", () -> new BlockItem(ModBlocks.WAREHOUSE_INPUT.get(), new Item.Properties()));

    public static final Supplier<BlockItem> WAREHOUSE_OUTPUT =
            ITEMS.register("warehouse_output", () -> new BlockItem(ModBlocks.WAREHOUSE_OUTPUT.get(), new Item.Properties()));

    /** 教程手册：始终注册（DeferredRegister 表操作安全）；没装 Patchouli 时退化成普通 Item，创造栏/配方条件不出现 */
    public static final Supplier<Item> GUIDE_BOOK =
            ITEMS.register("guide_book", () -> new GuideBookItem(new Item.Properties().stacksTo(1)));

    /** 是否应该把教程书放进创造栏 / 让配方可用 */
    public static boolean guideBookAvailable() {
        return GuideBookItem.hasPatchouli();
    }
}
