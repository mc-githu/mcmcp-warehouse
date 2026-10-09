# mcmcp 的仓库 · 模组介绍

> **本文件基于 v1.11.44 编写。**
> 如果模组版本更新了，请以**新版对应的介绍文档**为准，不要照旧版本文档操作。
> （每版 jar 都归档在 `模组归档/`，各版本行为可能有差异）

> Minecraft 1.21.1 · NeoForge · modId: `commhub` · 当前文档版本：**v1.11.44**

一个把「共享仓库 / 私人仓库 / 好友私聊 / 传物品 / 以物换物」整合进一个界面的仓库模组。

---

## 当前版本：v1.11.44 —— 更新 / 修复了什么

> 完整历史见同目录 `更新日报/` 文件夹（**每个版本一个独立文件**）

### v1.11.44（最新）
**修复（自查）**
- 锁链的**碰撞体积一直没生成**（生成代码写在客户端渲染里，永远不会执行）→ 改到服务端实体 tick，只生成一次

### v1.11.44
**调整（测试动画）**
- 常驻裂缝（`/commhub rift`）里包裹改成**乒乓循环**：门里出来 → 到我身上 → 从我身上出来 → 回门里（7 秒一轮）
- 门**保持全开**（上一版把门也做成循环开合了）
- 真实成交仍是单向（门 → 我 / 我 → 门）

### v1.11.41
**修复**
- 常驻裂缝（`/commhub rift`）下，包裹原来**定在身前不动**（进度恒为 1）→ 改成**循环播放**：反复从**门中间**出来 → 沿锁链走到你身前 → 回到门里

### v1.11.40
**美术调整**
- 门改回**单面平面**，并换成**绿色传送门**（瑞克和莫蒂那种，亮绿描边 + 深绿核心 + 星点）
- 包裹缩小到 **0.32**，且只走到全程 85%（停在身前约 1.2 格），不再糊住镜头

### v1.11.39
**修复（实际使用）**
- **正常成交也会播这套 3D 动画了**：成交时在双方各自面前开一个裂缝，包裹里是收到的物品，上方显示内容提示
- 之前只有 `/commhub rift [play]` 能看到 3D 版，真实交易看的还是旧的屏幕动画

### v1.11.38
**美术升级（用真模型）**
- 锁链：改用**原版铁链方块模型**（3D，按走向设置轴向）
- 裂缝：改成**十字交叉 4 层**（有体积，不再是单片贴图）
- 包裹：机械动力包裹模型 + 上方内容提示（前 3 样 + 「…」）

### v1.11.37
**修复**
- 裂缝改**双面可见**：原来单面剔除 → 一低头/转视角整张平面被剔掉，看着像消失
- 包裹上方显示**里面有什么**（最多 3 样，超过加「…」）

### v1.11.36
**修复 + 新功能**
- 锁链起点改到**裂缝正中间**（原来在裂缝最底下）
- 锁链有了**碰撞体积**：4 个链节实体，每个 0.45×0.45 碰撞箱，跟着锁链移动，能撞到也能绕过去

### v1.11.35
**修复（链节方向）**
- 链子贴图换成原版**方块**的链子贴图（**竖着的链环**，可无缝相接）—— 原来用的物品图标本身是横的
- 链节加大、间距加密 → **首尾相接成一条线**

### v1.11.34
**修复（锁链）**
- 锁链改成**每节用自己的世界坐标**绘制 → **一定连到玩家身上**（原来坐标系用错，链子落在地上）
- 链节改成**竖的**（细长竖条）
- 加了**流动动画**（链节沿锁链往裂缝方向移动）

### v1.11.33
**成交动画重做成真实体**
- 裂缝是**实体**：**碰撞箱 1.8×2.8**，能撞（像撞墙）、能推、能选中，能绕过去
- 裂缝**单面**：正面看到本体+星空，走到背面什么都看不到；锁链和包裹任何角度都看得见
- 包裹改用**机械动力的包裹模型**（`create:package`），里面的物品图标缩小不再遮住包裹
- 调试指令（OP）：`/commhub rift`（常驻，锁链一直伸着）、`/commhub rift stop`、`/commhub rift play [out]`（不用交易直接看动画）

### v1.11.32
**修复（成交动画）**
- 锁链横竖交替（横的那节用旋转画，不再全是竖的）
- 锁链近端**锁在玩家身上**（屏幕正下方）并跟随转身/移动摆动

### v1.11.31
**修复 + 测试工具**
- 修：开 `/commhub selftrade true` 后界面仍只有「取消悬赏」→ 开关**同步到客户端**，现在同时给「**接受(调试)**」
- 新增 `/commhub faketrade`（OP）：**召唤假人接受你最新的悬赏**，用来一键看成交动画

### v1.11.30
**移除 + 修复**
- **取用记录功能整个移除**（按钮/界面/记录逻辑都删了）
- 管理工具**只认已绑定的**（未绑定的一律不动）
- **悬赏可以取消**了：自己的悬赏标注「（我的，可取消）」，按钮变「取消悬赏」

### v1.11.29
- 修**吞物品**严重 bug（`removeAvailable` 背包扣物品时忘记计数）

### v1.11.28
- 成交动画：空间裂缝 + 原版铁链 + 机械动力纸箱 + 星辰 + 无尽锁链 + 跟随玩家
- 调试指令 `/commhub selftrade`（OP，关掉不能接受自己悬赏的限制）
**修复 + 文档**
- 修：悬赏里有 2 件以上物品时**仍然显示内部 id**（我上一版按逗号拆，服务端其实用 " + " 拼）
- 以物换物说明**重写**：手册两条目 + 本指南第七节，改成「步骤 + 表格 + 成交条件」

### v1.11.26
**以物换物一批修复**
- 左边栏按钮改名：**悬赏模式 / 换物模式**，执行按钮改叫 **确认发布**（原来两个同名）
- 悬赏里显示**游戏内的物品名字**（原来显示的是 create:cogwheel 这种内部 id）
- **我给 / 我要 各 10 格**（原来 5 格）
- 幽灵格：放物品**保留手持数量**、**滚轮调数量**（Shift 一次 64）、上限 **无限堆叠**
- 交易存档改成数量单独存 —— 支持无限堆叠（原来会被 99 上限截断）
- 取用记录界面结构改成和容器界面完全一致

### v1.11.25
**重写**
- 取用记录界面的背景**完全照抄仓库主界面**的写法：半透明底（`0xC0101010`）+ 同款 1px 细框
- **不再用全不透明去盖症状**（那是掩耳盗铃），也不做任何自己独创的渲染处理

### v1.11.23
**修复**
- **没有私人仓库也能打开界面了**：邮箱（传物品）、以物换物、私聊都是免费的，不该被私人仓库卡住
- 没私人仓库时点「私人仓库」标签只会提示"先创建一个"，**界面不会关掉**

### v1.11.22
**JEI 适配**
- 用整合包实际安装的 **JEI 19.27** 重新编译（原来用 19.57）→ **编译通过 = API 兼容** ✅
- 给 **实例 B** 补装了 JEI（原来没装，所以那边的 JEI 功能一直用不了）

### v1.11.21
**修复**
- **只有取用记录界面模糊**：对比发现它是唯一铺「全屏底」的界面（其它无容器界面都不铺）→ 去掉后与它们一致

### v1.11.20
**修复 + 文档**
- **以物换物界面**重排：加了彩色框（原来一个框都没有），背包整体下移，按钮/标签不再压背包格
- **邮箱说明重写**：手册「私聊与传物品」和本指南都改成「分块 + 3 步」讲清楚

### v1.11.19
**完全还原界面**
- 界面（框、背景、取用记录）**逐字回到 1.11.14** —— 那一版本来就没坏，是后面几版"修模糊"改坏的
- 与 1.11.14 的界面差异：**0**

### v1.11.18（已废弃）
**回退重做**
- 界面**严格还原到 1.11.14**：背景交回原版处理、不再动任何游戏设置
- **只加一处改动**：所有界面的框从 1px/80% 改成 **2px/94%**（原先被虚化糊掉，看起来"框没了"）

### v1.11.17（已废弃）
**修复**
- 彩色框"看不见"：其实是 1px 细线在发虚的画面上被糊掉了 → 所有界面框改成 **2px 粗、94% 不透明**

### v1.11.16
**修复（界面发虚，正确思路）**
- 根因：原版 `renderBackground()` 会先把世界渲到**另一张低分辨率缓冲**做模糊（另一个图层），
  Android GL 翻译层没还原好 → **连按钮都糊**
- 修法：我们的 6 个界面**背景与按钮在同一图层**绘制（用原版半透明渐变，不触发模糊后处理）
- 上一版（1.11.15）改你视频设置是错的 —— **已还原成 5**，不再动你的设置

### v1.11.15（已废弃）
**修复（界面发虚）**
- 定位到是原版「**菜单背景模糊**」后处理在 Android GL 翻译层上把整个界面弄糊（连原版按钮都虚）
- 已把两个整合包的 `menuBackgroundBlurriness` 改成 **0**
- 我们自己的 **6 个界面**都改成**纯色底**，不再走模糊后处理
- 如果以后还想用原版的模糊效果，把视频设置里「菜单背景模糊」拉回去即可（我们的界面不受影响）

### v1.11.14
**清理**
- 配置头部那段「佛祖保佑 / 佛曰」ASCII 与打油诗删掉，换成正经说明（已存在的配置文件也一起清了）
- JEI 里「自动化核心 · 获取方式」**保留**，并注明可用 `/commhub loottest` 自检

### v1.11.13
**文档**
- 手册新增「**取用记录（防小偷）**」条目；输出方块条目加「放置者」页；共享大仓库补「默认关闭」说明
- 使用指南补「**打开就回到上次那一页**」、配置默认值改 false、取用记录补 shift 快移说明

### v1.11.12
**修复 + 纠正**
- **纠正**：v1.11.11 里说的「颜色代码乱码」是误判 —— 反编译确认 `Font` 会解析 `§`，记录界面真正的问题是**背景模糊**（已修好）
- 修：配置默认值改了但老配置文件不生效（**已直接把两个整合包的 config 改成 `enable_shared_warehouse = false`**）
- 修：关掉共享仓库后标签页留空位（改成按顺序排）
- 卡片界面关闭时保存加保护（退出世界/断线不会出错）

### v1.11.11
**修复 + 调整**
- **取用记录界面**修好两处：颜色代码被当普通文字画（乱码）、背景用了模糊效果导致发虚；改成**不透明底 + 结构化上色 + 隔行斑马纹**
- **共享大仓库默认关闭**（`enable_shared_warehouse = false`）
- 打开仓库界面（按 K）**回到上次关闭时的标签页**（记在 `config/commhub-client.toml`，重启也记得）

### v1.11.10
**验证**
- 新增自检指令 **`/commhub loottest [次数]`**：直接在游戏里验证「自动化核心能不能从遗迹宝箱开出来」
- 静态核对：22 个宝箱表 id **全部存在**、修改器格式与注入点都对照 NeoForge 源码/字节码确认过

### v1.11.9
**优化 + 修复**
- **JEI 里能看到自动化核心的「获取方式」**（宝箱清单 + 复刻方法）；宝箱来源普通 JEI 看不到，所以写进了 JEI 信息页
- Create 配方读取改「快路径 + 全量兜底」，启动不再卡
- 修：`/reload` 后 Create 配方缓存不刷新

### v1.11.8
**调整 + 修复**
- **卡槽 3 → 5 个**（私人仓库右侧金色框）
- 修：动力锯卡片里，**第一张卡没材料时后面的卡也不跑**（改成只跳过那一张）
- 清掉手册里过期的「黑曜石 + 钻石做核心」文案（现在是遗迹宝箱 + 复刻）

### v1.11.7
**修复**
- 战利品修改器条件写错（`minecraft:alternative` 在 1.21.1 里已改名成 `minecraft:any_of`）→ 不改的话**遗迹里永远开不出自动化核心**

### v1.11.6
**平衡性调整**
- **自动化核心**改成「下界合金升级模板」那种获取方式：**只能在遗迹宝箱里找到**，用「1 核心 + 1 下界合金锭 + 7 钻石」复刻出 1 个（产出 2 个）

### v1.11.5
**修复（界面）**
- 机械手 / 动力锯卡片的**配置界面重排**（色框、标签不再互相压），并补上 **JEI 拖拽**
- 仓库界面**文字重排**：标题不再压住 ◀▶/切换/重命名；仓库名移到左侧栏；「我的邮箱」不再压住发送格；「背包」不再压住聊天输入框

### v1.11.4
**修复**
- 机械手卡片**只做右键**（把手上物品按到输入物品上，去掉斧头去皮分支）
- Create 配方改成**全量扫描**（原来按路径过滤，可能一条都没读到 → 卡片不动）
- 诊断日志写明原因（配方读了几条 / 仓库缺什么）

### v1.11.3
**修复**
- **机械手卡片 / 动力锯卡片完全没被执行**：定时任务里的调用被覆盖丢失，已接回（合成卡一直正常，只有这两张不动就是这个原因）

### v1.11.2
**修复**
- 加诊断日志：卡片做不了事时日志里会说明原因（10 秒最多一条）

### v1.11.1
**大更新：自动化卡片 + 防小偷**
- 三张卡片：合成升级卡 / **机械手卡片** / **动力锯卡片**（都是右键空气配置 → 放进私人仓库卡槽）
  - 机械手：**斧头 + 原木 → 去皮原木**、**安山合金 + 去皮原木 → 安山机壳**（左右键自动判断，不攻击）
  - 动力锯：输入 + 选定产物，支持原版切割与**机械动力切割配方**
- **取用记录（防小偷）**：谁拿了什么、多少都能查（普通点击与 shift-click 都记）
- **输出方块显示放置者**
- 配置 **`enable_shared_warehouse`**：可以整体关掉共享大仓库
- 修了 4 个自查出来的 bug（卡槽不认新卡、配方缓存重复扫描、记录界面无限重开、shift-click 绕过记录）

**新功能：卡片接上真正的自动化**
- **机械手卡片**改成两个格子（手上物品 + 输入物品），左右键自动判断：
  - **斧头 + 原木 → 去皮原木**
  - **安山合金 + 去皮原木 → 安山机壳**（直接读 Create 的 item_application 配方，不依赖 Create 代码）
- **动力锯卡片**现在支持两类切割配方：原版切割 + **机械动力的 create:cutting**（一次多产物全放回仓库）

**新功能：防小偷**
- **取用记录**：左侧栏新增「取用记录」按钮，能看到**谁**、**拿了什么**、**拿了多少**、什么时间；共享仓库全记，私人仓库只记非主人的取用（不需要写理由）
- **输出方块显示放置者**：方块界面显示「放置者：xxx」
- **配置：`enable_shared_warehouse`** —— 可以整体关掉共享大仓库（界面不显示、方块不能指向、服务端拒绝）；配置文件里写了详细备注

### v1.10.13
**新功能：机械手卡片 / 动力锯卡片**
- 机械手卡片：右键空气配置「右键 / 左键 + 用哪个物品」，**不含攻击**
- 动力锯卡片：配置「输入物品 + 选定产物」，按**切割配方**自动加工，不会选错
- 两张卡都含**自动化核心**，配方分别需要**机械手 / 动力锯**

**改名**
- 中间物品「合成核心」→「**自动化核心**」（`commhub:automation_core`）—— 以后机械手/动力锯也用它当材料，名字要通用

### v1.10.11
**平衡性调整**
- 新增中间材料**自动化核心**：配方必须含 **黑曜石 ×4 + 钻石 ×4**（后期自动化的门槛）
- 合成升级卡配方改用该材料，**一次产出 10 张**

**修复（配方图）**
- 配方名「合成升级卡」缺字（字体表补了 合/升/级/卡；顺带修了「口」被填实的光栅化 bug）
- 「管理员工具」还画着早就删掉的合成配方 → 改成「**无配方**」卡片（只画物品本身，不画合成格）

**文档 / 配方图**
- 补上**合成升级卡**的配方图（配方总图更新为 8 个配方；单图放进模组资源与手册）
- 手册 `recipes_all` 新增「合成升级卡」图片页
- 本指南「配方一览」新增第 8 节（材料表 + 内嵌配方图），备用配方表补一行

**修复（界面）**
- 仓库界面各区域的框**互相重叠**（左侧栏↔仓库、仓库↔工具栏/卡槽、发送格↔邮箱）导致发糊 → 全部重排，相邻区域共用一条边线，不再重叠
- 框的描边细化：底色 25% → **13%**（格子里的物品更清楚），边线 80% 不透明、1px 细而清楚

**修复（断连崩溃）**
- **右键卡片打开配置界面会直接断连**：菜单没写数据包，客户端 `buf` 是 null 导致 NPE
- 打开菜单时补上数据写入，构造函数也加了空值保护

**修复**
- 配置界面加上**玩家背包**（底部显示），**没装 JEI 也能从背包拿物品点进配方格**
- 修「清空这个目标」按钮和配方框重叠导致发糊/图层不对（重新排版，各区域不再重叠）

**调整**
- 打开配置界面改成**拿着卡右键空气**（不再用仓库界面的「添加」按钮，不占界面空间）
- 私人仓库仍有 **5 个卡槽**（金色框）；共享大仓库没有卡槽

**修复**
- 「添加」按钮改为**动态**：卡放进卡槽立刻出现，拿走立刻消失，不用重开界面

### v1.10.3
**修复**
- 兼容 1.10.0/1.10.1 的单卡槽旧存档（卡片不会因升级消失）

### v1.10.2
**修复**
- 配方缓存每 5 分钟清理（/reload 换数据包后不会一直用旧配方）
- 文档同步：**只有私人仓库有卡槽**

### v1.10.1
**修复（卡槽 4 个 bug）**
- 客户端卡槽镜像格数不对（会索引越界崩溃）
- quickMoveStack 没把卡槽算进容器格数（shift-click 错位、可能丢物品）
- 保存配置改成一卡一包（原来 3 张打包可能超过数据包上限导致掉线）
- 卡槽限制为**只有私人仓库**有

### v1.10.0
**新功能：合成升级卡（仓库自动合成）**
- 新物品「合成升级卡」：放进仓库界面的卡槽（**最多 5 张**），仓库就会按卡上的配方自动合成
- 卡槽里放了卡后，界面出现 **「添加」按钮** → 打开配置界面
- 配置界面：左侧**可滚动**的目标列表（每张卡 5 个目标，滚轮翻找），右侧 3×3 配方幽灵格 + 产物幽灵格，**支持 JEI 拖拽**
- 自动合成：每约 0.5 秒一次，材料从仓库取、产物放回仓库，**材料不够自动停**，配方必须是合成表里真实存在的
- 仓库界面新增 5 个卡槽（金色框）和配置界面的分区框

### v1.9.14
**修复**
- 单格恢复**真·无限堆叠**：存档里基础堆叠裁到 99（MC 硬性要求），真实数量另存 `Count` 字段，读档还原
- 仓库/邮箱取出时一律裁到 99，保证大堆不会漏到会崩服的场合（玩家背包 / 掉落物）
- 参考了「超越维度」（数量单独存 long）的做法

### v1.9.13
**修复**
- 配置项 `shared_warehouse_size` / `private_warehouse_size` 在改成分页后失效 → 改为控制**初始页数**

### v1.9.12
**修复**
- 分页仓库：界面页数改按**真实页数**显示、页数上限 64 → 1024

### v1.9.11
**优化**
- 仓库存储改成**分页数组**：满了自动新建一页、空页丢弃 → 容量真正无限、空仓库不占存档

### v1.9.10
**修复**
- 堆叠上限统一为 99（MC 存档硬限制），叠好的堆不再被打散或丢弃

### v1.9.9
**清理**
- 删除未被引用的配方总览图和散图（图都在 md 里内嵌、jar 里内置）

### v1.9.8
**修复**
- 补齐 Patchouli 手册**英文版**缺失的「指令」条目（英文环境下少一页）

### v1.9.7
**修复**
- 兜底工具处理器（仓库不存在时）未实现 `IItemHandlerModifiable`，点工具格会抛异常
- 传物品页「背包」文字与第一行格子重叠（背包区下移）

### v1.9.6
**修复**
- **手册打不开**：Patchouli 1.20+ 要求 `book.json` 放 `data/`、书内容放 `assets/`，且必须声明 `"use_resource_pack": true`
- **管理员工具一拿就消失**：工具格子的处理器没实现 `IItemHandlerModifiable`，服务端 `set()` 时抛 `ClassCastException` 导致工具丢失

**新增**
- 界面**分区框**：仓库（青）/ 邮箱（紫）/ 背包（绿）/ 管理工具栏（红）/ 左侧功能栏（黄）

### v1.9.5
**修复**
- 手册目录位置错误（放 `assets/` → 移到 `data/`）
- 配方图片移到 `assets/commhub/textures/`

### v1.9.4
**修复**
- **sable + flywheel 渲染崩溃**（`this.wrapped is null`）→ mod 内置 Mixin 补丁，异常时跳过该帧

### v1.9.3
**修复**
- 1.9.2 引入的「注册表已冻结」崩溃 → 回退为安全注册方式

### v1.9.1
**修复**
- 配方图滑槽 / 智能滑槽改用 mc百科官方渲染图（原来渲染成黑块）

### v1.9.0
**新增**
- 每仓库单管理员（主人 / 管理员 / 成员）+ 智能「给予/转让」管理员工具
- 管理员工具无配方、仓库自带；给予后自动变转让
- 邮箱 4×9=36 格 + 无限堆叠；领取按 64 拆分不丢物品
- 界面高度 248 不超屏；新增「绑定」按钮
- `/commhub test` 增加管理员给予/转让自测

**修复**
- 管理员字段未存档（重启丢管理员）
- 邮箱迁移静默丢物品
- 绑定/重置后客户端菜单不刷新
- 转让后原持有者工具未回收

---

## 文件说明

| 文件 | 说明 |
|---|---|
| `mcmcp的仓库-使用指南-v1.11.44.md` | 完整使用说明（可转发给他人） |
| `更新日报/` | 每个版本一个独立更新日报 + 版本号规则 + 版本索引 |
| `配方/` | 配方 JSON 文件（文字版） |

## 核心功能

- **共享大仓库** + **私人命名仓库**（可建多个、可重命名、可设成员）
- **输入/输出方块**：物品/液体/电能三合一，兼容所有标准能力模组
- **好友系统**：需双方同意，支持私聊、传物品
- **邮箱**：4×9 收件箱
- **以物换物**：发布悬赏、接受悬赏（有回滚保护）
- **管理工具**：绑定到仓库、可重置找回
- **合成升级卡**：装进仓库后按卡上的配方自动合成（最多 5 张卡，每张 5 个配方）
- **机械手 / 动力锯卡片**：机械手按配置使用物品（不攻击）；动力锯按切割配方加工
- **取用记录（防小偷）**：谁拿了什么、多少，都能查；输出方块显示是谁放的
- **共享大仓库开关**：配置里可以整体关掉公共仓库
- **无限堆叠**：仓库单格可堆极大量

## 依赖

- NeoForge 21.1.x（Minecraft 1.21.1）
- 可选：Patchouli（游戏内手册）、JEI（悬赏拖拽）、机械动力（主配方材料）
# mcmcp 的仓库 · 使用指南

> **Minecraft 1.21.1 · NeoForge**
> modId: `commhub` ｜ 中文名：mcmcp 的仓库

一个把「共享仓库 / 私人仓库 / 好友私聊 / 传物品 / 以物换物」全塞进一个界面的仓库模组。

---

## 目录

- [一、快速开始](#一快速开始)
- [二、仓库系统](#二仓库系统)
- [三、输入 / 输出方块](#三输入--输出方块)
- [四、私人仓库与权限](#四私人仓库与权限)
- [五、好友与私聊](#五好友与私聊)
- [六、传物品与邮箱](#六传物品与邮箱)
- [七、以物换物](#七以物换物)
- [八、指令](#八指令)
- [九、配方一览](#九配方一览)
- [十、技术说明](#十技术说明)

---

## 一、快速开始

1. **按 `K` 打开主界面**
2. 左侧四个标签：`共享大仓库` / `私人仓库` / `私聊` / `传物品`，上方还有 `以物换物`
3. 底部实时显示当前仓库的**电能**和**液体**储量
4. 想拥有自己的仓库？合成一个 **管理员凭证**，右键它

---

## 二、仓库系统

### 共享大仓库

- 所有玩家共用的一个大仓库
- 人人可存可取，**没有主人、没有管理工具**

### 私人仓库

- 由**管理员凭证**创建并命名，创建者就是**主人**
- 每个私人仓库的角色：**主人**（创建者，永不变）+ **管理员**（每仓库最多 1 个，可空）+ **成员**
- 可以建多个，用「切换」按钮在仓库之间轮换
- 支持**重命名**（主人或管理员可改）

### 自动合成（合成升级卡）

1. 合成一张 **合成升级卡**（纸 ×4 + 铁板 ×4 + 工作台；没装机械动力则用铁锭）
2. **拿着卡右键空气** → 打开配置界面
3. 在配置界面里设置配方（每张卡最多 5 个目标）：
   - 左边是**目标列表**；**目标多了用鼠标滚轮上下翻**
   - 右边是 **3×3 配方幽灵格 + 产物幽灵格**
   - **界面下方就是你的背包**：从背包左键拿起物品，再点配方格即可（没装 JEI 也行）
   - 装了 JEI 也可以直接把物品拖进格子；右键格子清空
4. 点「保存」写回卡片
5. 把卡放进**私人仓库界面右侧的卡槽**（金色框，**最多 5 张**；共享大仓库没有卡槽）

之后仓库会**自动合成**：

| 规则 | 说明 |
|---|---|
| 速度 | 每约 **0.5 秒**合成一次 |
| 材料 | **从仓库里取**（共享仓库 / 对应私人仓库） |
| 产物 | **放回仓库** |
| 材料不够 | **自动停下**，不会扣一半材料 |
| 配方校验 | 必须是合成表里真实存在的配方，防止凭空造物 |
| 容器返还 | 桶、瓶子之类的返还物也放回仓库 |

> 只有**私人仓库**有卡槽；且只有**主人或管理员**能放卡、能配置。共享大仓库没有卡槽。

### 机械手 / 动力锯卡片

和合成升级卡一样：**拿着右键空气**配置 → 放进**私人仓库的卡槽**。仓库每约 **0.5 秒**自动干一次活。

#### 动力锯卡片（两个格子）

| 格子 | 放什么 |
|---|---|
| 输入物品 | 要切/要去皮的东西（原木、石头…） |
| 选定产物 | 想要产出什么（比如**去皮原木**、木板） |

能干两类活：

| 活 | 例子 |
|---|---|
| **切割** | 原木 → 木板、石头 → 台阶（原版切割 + 机械动力切割配方） |
| **去皮** | **原木 → 去皮原木**（锯子给原木去皮，不依赖模组配方） |

一个输入有多个产物时，用你选的产物定位配方，**不会选错**。

#### 副产物开关

有些模组给配方加了**副产物**（例如锯原木去皮时多给一份**木屑**）。配置里：

```toml
# 加工时要不要产出「副产物」
# true = 副产物也放进仓库（默认）；false = 只产出你选定的主产物
enable_byproducts = true
```

#### 机械手卡片（两个格子）

| 格子 | 放什么 |
|---|---|
| 手上物品 | 材料（比如**安山合金**） |
| 输入物品 | 被加工的东西（比如**去皮原木**） |

做的是「**把物品按到东西上**」这类加工（机械动力的 item_application）：

| 配置 | 结果 |
|---|---|
| **安山合金 + 去皮原木** | → **安山机壳** |

左右键 **自动判断**，**不会攻击生物**。

> 物品应用是直接读数据包里的 Create `item_application` 配方，**任何模组**的这类配方都能跑。
### 传物品页怎么用（别把两块弄混）

| 区域 | 是什么 |
|---|---|
| **上面 = 发送格** | 你要寄给别人的东西放这里（9 × 3） |
| **下面 = 我的邮箱** | 别人寄给你的东西都会进这里（4 × 9 = 36 格） |

**寄东西（3 步）**

1. 把要寄的物品放进**发送格**（拖动或 shift 点击）
2. 点「**目标：xxx**」按钮，切到要寄给的好友
3. 点「**发送**」

东西会直接进对方的**邮箱**（不需要对方在线）。

**收东西**：点「**领取全部**」→ 邮箱里的东西一次性拿回背包。

- 背包装不下时，装不下的部分**留在邮箱里**，不会丢
- 邮箱和仓库一样支持**无限堆叠**
- 右侧「◀ ▶」翻邮箱的页
### 打开就回到上次那一页

按 **K** 打开仓库界面时，会**直接回到你上次关闭时看的那个标签页**（连私人仓库的名字一起记住），不用每次翻半天。

- 记在**客户端配置** `config/commhub-client.toml`（`last_tab` / `last_warehouse`）
- **重启游戏也还在**
- 如果上次看的是私人仓库、但现在没权限了，会自动退回你能用的仓库
- 共享大仓库关掉时，标签页会按顺序重排，不留空位

### 共享大仓库可以关掉

配置文件 `config/commhub-server.toml` 里：

```toml
# 是否启用共享大仓库（所有人免费共用的那个）
# false = 界面不显示共享大仓库标签页、输入/输出方块也不能指向它
enable_shared_warehouse = false
```

**默认就是关闭的**（`false`）—— 有些整合包不需要这种公共仓库，觉得太轮椅。
想开就在配置里改成 `true`。

> ⚠️ 注意：如果你**之前**跑过旧版本，配置文件里已经写死了 `true`，
> NeoForge **不会**用新默认值覆盖它 —— 需要手动改，或删掉那一行让它重新生成。

另外还有一个副产物开关（见上面「副产物开关」）：

```toml
# 加工时要不要产出「副产物」（例如木屑）
enable_byproducts = true
```

### 无限堆叠（单格无限 + 分页无限）

仓库是**两层无限**：

| 层面 | 说明 |
|---|---|
| **堆叠无限** | 一格能堆到**上亿**，鼠标把两格合起来可以一直往上叠 |
| **页数无限** | 一格（每页 54 格）装满了**自动新建一页**，总容量无上限；空页存档时丢掉，所以空仓库不占地方 |

**技术上怎么做到的**：MC 自己的存档编码把单格数量限制在 `[1, 99]`
（`ExtraCodecs.intRange(1,99)`，超过 99 直接崩服），网络同步却没有这个限制。
所以本模组**自己存数量**：存档里把基础堆叠裁到 99（合法），真实数量写在单独的 `Count` 字段里，读档时还原
（和「超越维度」把数量单独存成 `long` 是同一个思路）。

**安全兜底**：大堆**不会离开仓库** —— 从界面拿取、漏斗抽取、输入输出方块运输，
单次最多给出 **99**，剩下的留在格子里。这样玩家背包、掉落物、其它容器都不会出现「存不下的堆叠」而崩服。
### 独立储量

**物品、液体、电能都跟仓库走**：

| | 共享大仓库 | 私人仓库 A | 私人仓库 B |
|---|---|---|---|
| 物品 | 独立 | 独立 | 独立 |
| 液体 | 独立 | 独立 | 独立 |
| 电能 | 独立 | 独立 | 独立 |

互相不会串。

---

## 三、输入 / 输出方块

### 仓库输入方块

外观是**四箭头指向中心**（青绿色）。

- 把物品 / 液体 / 电能**存进**仓库
- 右键选择目标仓库：
  - 选 `共享大仓库` → 直接生效
  - 选 `私人仓库` → 弹出你的仓库列表，选一个（列表下方有「退出」返回上一级）
- 物品：漏斗 / 管道投入
- 液体：机械动力或任意 mod 的管道灌入
- 电能：电线 / 电缆灌入

### 仓库输出方块

外观是**四箭头从中心向外**（橙色）。

- 把仓库的物品 / 液体 / 电能**放出去**
- 右键同样选择目标仓库
- 物品：管道 / 漏斗抽出
- 液体：管道抽出
- 电能：机器 / 电线抽取

### 兼容所有模组

三个接口都用**标准能力**实现：

| 类型 | 接口 | 兼容 |
|---|---|---|
| 物品 | `IItemHandler` | 所有管道 / 漏斗类模组 |
| 液体 | `IFluidHandler` | 机械动力、AE2 等各种管道 |
| 电能 | `IEnergyStorage` (FE) | 机械动力附属、交叉电网、电气时代等所有 FE 模组 |

---

## 四、私人仓库与权限

### 三个管理工具

私人仓库界面右侧有 **3 个固定位置**，放三把工具：

1. **添加成员工具** — 对玩家右键，把对方加为成员
2. **移除成员工具** — 对玩家右键，移除成员
3. **管理员工具**（智能切换）— 对玩家右键，把管理员身份交给对方：
   - 仓库**还没有管理员**时 = **给予管理员**
   - 仓库**已有管理员**时 = **转让管理员**
   - 给予/转让后，自己不再是管理员（主人身份不变，仍可管理）

### 绑定工具（重要）

工具需要**绑定到某个仓库**才能用：

管理员工具由**仓库自带**（创建仓库时自动出现在右边 3 个框里）：
- 添加/移除成员工具需要自己合成后放进去绑定
- 管理员工具（给予/转让）仓库自动送，**没有配方**

绑定后：
- 工具悬浮显示 **「归于 xxx 仓库」**，管理员工具还会显示当前是「给予」还是「转让」
- **一个仓库只能绑定一次**（不能重复绑来刷）
- 只有**主人和管理员**能从格子里拿走/使用工具，**成员和其他人拿不到也用不了**

### 重置工具

工具弄丢了怎么办？点 **重置工具** 按钮：

- 自动收回散落在成员背包里的工具
- 补满 3 个框
- **工具再也不怕丢**

### 重命名

私人仓库标签页点 **重命名** → 输入新名字 → 完成。

- 只有主人或管理员能改名
- 新名字不能和别人的重名

### 管理员凭证

- 右键使用 → 获得管理员权限，并弹出创建仓库的起名框
- **创建成功才消耗**凭证
- 如果仓库**重名了，凭证不会消耗**
- 配方较贵（见下方配方表）

---

## 五、好友与私聊

### 加好友

私聊页三个按钮：

- **加好友** — 弹在线玩家列表，点谁给谁发请求
- **删好友** — 从好友列表选，二次确认
- **好友请求** — 别人加你时出现，✓ 同意 / ✗ 拒绝

> 好友需要**双方同意**才生效。

### 私聊

- 选择好友作为目标，输入消息发送
- 限制：**0.5 秒一条**、最多 **256 字**

---

## 六、传物品与邮箱

1. 打开 **传物品** 标签页
2. 把要寄的东西放进 **3×3 发送格**
3. 选好友作为目标
4. 点 **发送** → 东西进对方邮箱
5. 对方点 **领取全部** 领走

> **邮箱是 4×9（36 格）** 的收件箱，每格 64 个。

---

## 七、以物换物

打开：按 **K** → 左侧「**以物换物**」。

### 发布悬赏（换东西出去）

1. 左上角选「**悬赏模式**」
2. 填两块（每块 **10 格**）：

| 格子 | 意思 |
|---|---|
| **我给** | 你愿意拿出来的东西 |
| **我要** | 你希望换到的东西 |

3. 每格操作：**左键**=放上手光标拿着的物品 · **滚轮**=调数量（**Shift** 一次 ±64）· **右键**=清空
4. 点「**确认发布**」

- **发布不扣物品** —— 有人接受时才真正交换 ✓
- 数量支持**无限堆叠**（和仓库一样），显示会压缩成「1.2万」「1亿」
- 装了 **JEI** 可以直接拖物品到格子里
- 每人最多 **5 条**悬赏，内容相同的不能重复发布

### 接受悬赏（把东西换进来）

1. 左上角切到「**换物模式**」
2. 用「**◀ ▶**」翻上下一个悬赏；显示格式：`谁 给: xxx / 要: yyy`（物品显示的是**游戏内的名字** ✓）
3. 点「**接受**」→ 自动交换：
   - 你收到对方的「我给」
   - 对方收到你的「我要」（进他的**邮箱**）

- 自己发的悬赏：标注「（我的，可取消）」，按钮变成「**取消悬赏**」

**成交条件**：

- 双方物品都要够（**背包 + 私人仓库**一起算）
- **发布者必须在线**
- 不能接受自己的悬赏
- 任何一方不足 → **自动取消并回滚**，不会吞东西 ✓

---
## 八、指令

| 指令 | 说明 | 需要 OP |
|---|---|---|
| `/commhub` | 总览：所有仓库储量 | 否 |
| `/commhub look <仓库名>` | 查看某仓库储量 | 否 |
| `/commhub look 共享` | 查看共享大仓库 | 否 |
| `/commhub energy` | 查共享仓库电能 | 否 |
| `/commhub fluid` | 查共享仓库液体 | 否 |
| `/commhub energy set <数值>` | 设置共享仓库电能 | **是** |
| `/commhub energy add <数值>` | 增加共享仓库电能 | **是** |

> `look` 只能查**自己的**仓库；共享仓库谁都能查。

---

## 九、配方一览

> 以下每条：文字说明 + 图片对照。

### 1. 仓库输入方块

把物品/液体/电能存进仓库的方块（青绿色四箭头指向中心）。

| 位置 | 材料 |
|---|---|
| 四角+上下中 | 安山合金 ×4 |
| 左右中 | 安山机壳 ×2 |
| 正中 | 滑槽 |
| 底部中 | 铁板 |

![仓库输入方块](data:image/png;base64,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)

### 2. 仓库输出方块

把仓库的物品/液体/电能放出去的方块（橙色四箭头向外）。

| 位置 | 材料 |
|---|---|
| 四角+上下中 | 安山合金 ×4 |
| 左右中 | 安山机壳 ×2 |
| 正中 | 智能滑槽 |
| 底部中 | 黄铜板 |

![仓库输出方块](data:image/png;base64,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)

### 3. 管理员凭证（很贵）

右键获得管理员权限并创建私人仓库；创建成功才消耗。

| 位置 | 材料 |
|---|---|
| 四角 | 下界合金块 ×4 |
| 上下中 | 精密构件 ×2 |
| 左右中 | 黄铜锭 ×2 |
| 正中 | 绿宝石 |

![管理员凭证](data:image/png;base64,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)

### 4. 添加成员工具（无序）

对玩家右键，把对方加为仓库成员。

黄铜板 + 黄铜手 + 电子管

![添加成员工具](data:image/png;base64,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)

### 5. 移除成员工具（无序）

对玩家右键，把对方移出仓库成员。

铁板 + 黄铜手 + 电子管

![移除成员工具](data:image/png;base64,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KB+RQbaKDFhgBTCjfEQG2SoyYIEVwIzyERlkq8iABVYAM8pHZJCtIgMWWAHMKB+RQbaKDFhgBTCjfEQG2SoyYIEVwIzyERlkq8iABVYAM8pHZJCtIgMWWAHMKB+RQbaKDFhgBTCjfEQG2SoyYIEVwIzyERlkq8iABVYAM8pHZJCtIgMWWAHMKB+RQbaKDFhgBTCjfEQG2SoyYIEVwIzyERlkq8iABVYAM8pHZJCtIgMWWAHMKB+RQbaKDFhgBTCjfEQG2SoyYIEVwIzyERlkq8iABVYAM8pHZJCtIgMWWAHMKB+RQbaKDFhgBTCjfEQG2SoyYIEVwIzyERlkq8iABVYAM8pHZJCtIgMWWAHMKB+RQbaKDFhgBTCjfEQG2SoyYIEVwIzyERlkq8iABVYAM8pHZJCtIgMWWAHMKB+RQbaKDFhgBTCjfEQG2SoyYIEVwIzyERlkq8iABVYAM8pHZJCtIgMWWAHMKB+RQbaKDFhgBTCjfEQG2SoyYIEVwIzyERlkq8iABVYAM8pHZJCtIgMWWAHMKB+RQbaKDFhgBTCjfEQG2SoyYIEVwIzyERlkq8iABVYAM8pHZJCtIgMWWAHMKB+RQbaKDFhgBTCjfEQG2SoyYIEVwIzyERlkq8iABVYAM8pHZJCtIgMWWAHMKB+RQbaKDFhgBTCjfEQG2SoyYIEVwIzyERlkq8iABVYAM8pHZJCtIgMWWAHMKB+RQbaKDFhgBTCjfEQG2SoyYIEVwIzyERlkq8iABVYAM8pHZJCtIgMWWAHMKB+RQbaKDFhgBTCjfEQG2SoyYIEVwIzyERlkq8iABVYAM8pHZJCtIgMWWAHMKB+RQbaKDFhgBTCjfEQG2SoyYIEVwIzyERlkq8iABVYAM8pHZJCtIgMWWAHMKB+RQbaKDFhgBTCjfEQG2SoyYIEVwIzyERlkq8iABVYAM8pHZJCtIgMWWAHMKB+RQbaKDFhgBTCjfEQG2SoyYIEVwIzyERlkq8iABVYAM8pHZJCtIgMWWAHMKB+RQbaKDFhgBTCjfEQG2SoyYIEVwIzyERlkq8iABVYAM8pHZJCtIgMWWAHMKB+RQbaKDFhgBTCjfEQG2SoyYIEVwIzyERlkq8iABVYAM8pHZJCtIgMWWAHMKB+RQbaKDFhgBTCjfEQG2SoyYIEVwIzyERlkq8iABVYAM8pHZJCtIgMWWAHM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)

### 6. 管理员工具（无配方，仓库自带）

**没有合成配方**，由仓库自动生成：

- 创建私人仓库时，右侧 3 个格子自动出现（添加成员 / 移除成员 / 给予管理员）
- 给出管理员后，第三格自动变成**转让管理员**
- 丢了点界面上的**重置工具**按钮找回
- 只有**主人/管理员**能从格子拿走使用，成员和其他人拿不到

![管理员工具](data:image/png;base64,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)

### 7. 仓库指南（无序，需 Patchouli）

游戏内手册，右键打开。

书 + 合成蓝图

![仓库指南](data:image/png;base64,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yIAD1gAb5SMyyKrIgAPWABvlIzLIqsiAA9YAG+UjMsiqyIAD1gAb5SMyyKrIgAPWABvlIzLIqsiAA9YAG+UjMsiqyIAD1gAb5SMyyKrIgAPWABvlIzLIqsiAA9YAG+UjMsiqyIAD1gAb5SMyyKrIgAPWABvlIzLIqsiAA9YAG+UjMsiqyIAD1gAb5SMyyKrIgAPWABvlIzLIqsiAA9YAG+UjMsiqyIAD1gAb5SMyyKrIgAPWABvlIzLIqsiAA9YAG+UjMsiqyIAD1gAb5SMyyKrIgAPWABvlIzLIqsiAA9YAG+UjMsiqyIAD1gAb5SMyyKrIgAPWABvlIzLIqsiAA9YAG+UjMsiqyIAD1gAb5SMyyKrIgAPWABvlIzLIqsiAA9YAG+UjMsiqyIAD1gAb5SMyyKrIgAPWABvlIzLIqsiAA9YAG+UjMsiqyIAD1gAb5SMyyKrIgAPWABvlIzLIqsiAA9YAG+UjMsiqyIAD1gAb5SMyyKrIgAPWABvlIzLIqsiAA9YAG+UjMsiqyIAD1gAb5SMyyKrIgAPWABvlIzLIqsiAA9YAG+UjMsiqyIAD1gAb5SMyyKrIgAPWABvlIzLIqsiAA9YAG+UjMsiqyIAD1gAb5SMyyKrIgAPWABvlIzLIqsiAA9YAG+UjMsiqyIAD1gAb5SMyyKrIgAPWABvlIzLIqsiAA9YAG+UjMsiqyIAD1gAb5SMyyKrIgAPWABvlIzLIqsiAA9YAG+UjMsiqyIAD1gAb5SMyyKrIgAPWABvlIzLIqsiAA9YAG+UjMsiqyIAD1gAb5SMyyKrIgAPWABvlIzLIqsiAA9YAG+UjMsiqyIAD1gAb5SMyyKrIgAPWABvlIzLIqsiAA9YAG+UjMsiqyIAD1gAb5SMyyKrIgAPWABvlIzLIqsiAA9YAG+UjMsiqyIAD1gAb5SMyyKrIgAPWABvlIzLIqsiAA9YAG+UjMsiqyIAD1gAb5SMyyKrIgAPWABvlIzLIqsiAA9YAG+UjMsiqyIAD1gAb5SMyyKrIgAPWABvlIzLIqsiAA9YAG+UjMsiqyIAD1gAb5SMyyKrIgAPWABvlIzLIqsiAA9YAG+UjMsiqyIAD1gAb5SMyyKrIgAPWABvlIzLIqsiAA9YAG+UjMsiqyIAD1gAb5SMyyKrIgAPWABvlIzLIqsiAA9YAG+UjMsiqyIAD1gAb5SMy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)

### 8. 合成升级卡（有序，一次出 10 张）

仓库自动合成的核心道具。需要先做出**自动化核心**。

| 位置 | 材料 |
|---|---|
| 四角 | 纸 ×4 |
| 上下左右中 | 铁板 ×4 |
| 正中 | **自动化核心** ×1 |

**一次合成产出 10 张卡片。**

![合成升级卡](data:image/png;base64,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)
### 9. 自动化核心（不能直接合成）

**通用自动化元件**：合成升级卡要用它，机械手 / 动力锯卡片也要用它。

#### 怎么获得

1. **只能在遗迹宝箱里找到**（地牢、废弃矿井、沙漠神殿、丛林神庙、雪屋、林地府邸、掠夺者前哨、沉船宝藏、埋藏的宝藏、下界要塞、要塞、远古城市、末地城、堡垒遗迹、试炼密室…）
2. **复刻**：中间放 1 个自动化核心，**核心上方放 1 个下界合金锭**，其余 7 格全部用**钻石**包起来 → 得到 **2 个**（等于复刻出 1 个）

| 位置 | 材料 |
|---|---|
| 正中 | **自动化核心** ×1 |
| 正上方 | **下界合金锭** ×1 |
| 其余 7 格 | **钻石** ×7 |
| 产出 | **自动化核心 ×2** |

![自动化核心](data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAswAAAJACAIAAAAWwafuAAAnDUlEQVR4nO3dP8tt23mecdchRWRUHWSjyiFCjV3FdprEGBwRg7EKg4tDylSByJ2+giq16lQJBCr1EdS5VZA6lSoikcKoUbFdvAsz2eOde4/55xnjecb83VyVz3vWu9a817jHZex99h/8/d99+RH/659/DAAAcIiv//GffMQfkAwAAHAdkgEAAEIgGQAAIASSAQAAQiAZAAAgBJIBAABCIBkAACCELsn4i//83wAAAA5BMgAAQAgkAwAAhEAyAABACCQDAACEQDIAAEAIJAMAAIRAMgAAQAgkAwAAhEAyAABACCQDAACEQDIAAEAIJAMAAIRAMgAAQAgkAwAAhEAyAABACCQDAACEQDIAAEAIJAMAAIRAMgAAQAgkAwAAhEAyAABACCQDAACEQDIAAEAIJAMAAIRAMgAAQAgkAwAAhEAyAABACCQDAACEQDIAAEAIJAMAAIRAMgAAQAgkAwAAhEAyAABACCQDAACEQDIAAEAISSXjJzIwXzbRUbboKH/OdQSsDckQF1iB6Ch/SAbQQjLEBVYgOsofkgG0kAxxgRWIjvKHZAAtJENcYAWio/whGUALyRAXWIHoKH9IBtBSRjK+J2G56wKb/TlWjo7yh2QALSRDXGAFoqP8IRlAC8kQF1iB6Ch/SAbQQjLEBVYgOsofkgG0kAxxgRWIjvKHZAAtJENcYAWio/whGUALyRAXWIHoKH9IBtBCMsQFViA6yh+SAbSQDHGBFYiO8odkAC0kQ1xgBaKj/CEZQAvJEBdYgegof0gG0EIyxAVWIDrKH5IBtJAMcYEViI7yh2QALSRDXGAFoqP8IRlAC8kQF1iB6Ch/SAbQQjLEBVYgOsofkgG0kAxxgRWIjvKHZAAtJENcYAWio/whGUALyTicr3znuzcy+9N8zwX22fzHr/1RJ3HvYb2OnCPgCZCMwzGO+Tu6NyQjIs4R8ARIxuEYx/wd3RuSERHnCHgCJONwjGP+ju4NyYiIcwQ8AZJxOMYxf0f3hmRExDkCngDJOBzjmL+je0MyIuIcAU+AZByOcczf0b0hGRFxjoAnQDIOxzjm7+jekIyIOEfAEyAZu9mbsw8X8vMPv/uIDKO53gXWpl8UWv7Hf/2rTv70P33jI+56/3U7co6AJ0MydmMc83fUH5LhHEWHZAAtJGM3xjF/R/0hGc5RdEgG0EIydmMc83fUH5LhHEWHZAAtJGM3xjF/R/0hGc5RdEgG0EIydmMc83fUH5LhHEWHZAAtJOOV/hH81m9++Vl++Ptfv0s7jl//2U/f5atffvsj4j57lQvsiij8zV//TSd/+9//tpNz2nFOPqp05ByRDGALyXjFOOa/wEhG/o6cI5IBbCEZrxjH/BcYycjfkXNEMoAtJOMV45j/AiMZ+TtyjkgGsIVkvGIc819gJCN/R84RyQC2kIxXjGP+C4xk5O/IOSIZwJaHSkbPFB4dvk+P4BvtT+79lpFzOfICmyUK//gP/3g7PSKy9w6PakdOyXCOtiEZQAvJMI4kg2ScjHO0DckAWkiGcSQZJONknKNtSAbQQjKMI8kgGSfjHG1DMoAWkmEcSQbJOBnnaBuSAbSQDONIMkjGyThH25AMoIVkGEeSQTJOxjnahmQALSTDOJIMknEyztE2JANoIRmHx7H9yXYE25/Z+8m939KSbRw/3dGeKLRXdf9fpN5e4f0/OUY7/un//FMnR7VjPclwjoAnQDKMI8kgGSfjHG1DMoAWkmEcSQbJOBnnaBuSAbSQDONIMkjGyThH25AMoIVkGEeSQTJOxjnahmQALSTDOJIMknEyztE2JANoWVwy2hHsn8K9gesZsp4/nvfGFz/+0bu0f/Su/Zl2Ls+N5kjJaIVg7xruudR7ZGKPPUHpF4Vz6vDGX/75X37EV5pEd+QcZThHwNqQDONIMkiGc0QygBBIhnEkGSTDOSIZQAgkwziSDJLhHJEMIASSYRxJBslwjkgGEALJMI4kg2Q4RyQDCIFk7E7e3l/OdGUKW772/R+8S/5xPCcZ/+XP/qyTc9px9I+S9ojC3jvcE4gte//ucyTDOQKeDMkwjiSDZDhHJAMIgWQYR5JBMpwjkgGEQDKMI8kgGc4RyQBCIBnGkWSQDOeIZAAhkAzjSDJIhnNEMoAQSIZxJBkkwzkiGUAIJMM4kgyS4RyRDCAEkrGbdp7euDKF7bDujePef1wozziek4z2cu3nXlHoUYSjXPl0a0iGc0QygC0kwziSDJLhHJEMIASSYRxJBslwjkgGEALJMI4kg2Q4RyQDCIFkGEeSQTKcI5IBhEAyjCPJIBnOEckAQnioZPT8gbo9emarn70JbmkHtKJk9Pxhzj1addj7yXvVof+PobY/06rDXsZ31B/nqCckA2ghGcaRZJCMz8Q56gnJAFpIhnEkGSTjM3GOekIygBaSYRxJBsn4TJyjnpAMoIVkGEeSQTI+E+eoJyQDaCEZxpFkkIzPxDnqCckAWkiGcSQZJOMzcY56QjKAFpJhHEkGyfhMnKOekAygZXHJ2Es7KP0j2D9nPfT/lusjuJeRF9iVK3zMX8J+RRSOqkPOjvrjHG1DMoAWkmEcSQbJOBnnaBuSAbSQDONIMkjGyThH25AMoIVkGEeSQTJOxjnahmQALSTDOJIMknEyztE2JANoIRnGkWSQjJNxjrYhGUALyXjxzV/94iPmjmP711tnG8e7OuqXgJ4/MronGRlE4UqqSIZzRDKALSTDOJIMknEyztE2JANoIRnGkWSQjJNxjrYhGUALyTCOJINknIxztA3JAFpIhnEkGSTjZJyjbUgG0EIyjCPJIBkn4xxtQzKAlodKRpt2etq/SOmNnr/8qZ22PfZGM24K2+S8wHr+GGotUbiSnB21cY5IBrCFZLxiHLNdYCRjm5wdtXGOSAawhWS8YhyzXWAkY5ucHbVxjkgGsIVkvGIcs11gJGObnB21cY5IBrCFZLxiHLNdYCRjm5wdtXGOSAawhWS8YhyzXWAkY5ucHbVxjkgGsIVkvGIcs11gJGObnB21cY5IBrCFZLxiHLNdYCRjm5wdtXGOSAawhWTspp2nkYz8pFUusCenbkfOEfBkSMZujGP+jp6Tuh05R8CTIRm7MY75O3pO6nbkHAFPhmTsxjjm7+g5qduRcwQ8GZKxG+OYv6PnpG5HzhHwZEjGboxj/o6ek7odOUfAkyEZUvgCe050lD8kA2ghGeICKxAd5Q/JAFpIhrjACkRH+UMygBaSIS6wAtFR/pAMoIVkiAusQHSUPyQDaCEZ4gIrEB3lD8kAWkiGuMAKREf5QzKAFpIhLrAC0VH+kAyghWSIC6xAdJQ/JANoIRniAisQHeUPyQBakkpGe1xlZHSUPzrKn+n7DkyHZMg70VH+6Ch/pu87MB2SIe9ER/mjo/yZvu/AdEiGvBMd5Y+O8mf6vgPTIRnyTnSUPzrKn+n7PowPslzu+m6QDHknOsofHeXP9Lt/GLMvRLk/d303kkoGAKAKb9fS7D9ELPeEZAAAEkEyVgrJAAAkgmSsFJIBAEgEyVgpJAMAkAiSsVJIBgAgESRjpZAMAEAiSMZKIRkAgESQjJVCMgAAiSAZK4VkAAASQTJWCskAACSCZKwUkgEASATJWCmPkIyfyMCc+4udZr/rZ0VH+eMvSOu5wGa39Ky030mS8WJ2Nc+KCyx/dJQ/JINkZAvJ2GV2Nc+KCyx/dJQ/JINkZAvJ2GV2Nc+KCyx/dJQ/JINkZAvJ2GV2Nc+KCyx/dJQ/JINkZAvJ2GV2Nc+KCyx/dJQ/JINkZAvJ2KV9WD2PRs7lrgts9udYOTrKH5LR85R8J0eGZOziizgyLrD80VH+kIyep+Q7OTIkYxdfxJFxgeWPjvKHZPQ8Jd/JkSEZu/gijowLLH90lD8ko+cp+U6ODMnYxRdxZCIusH//7/4QF4nuSO4Nyeh5SnZj7m70dEQy5OaQjJxEdyT3hmT0PCW7MXc3ejoiGXJzSEZOojuSe0Myep6S3Zi7Gz0dkQy5OSQjJ9Edyb0hGT1PyW7M3Y2ejkiG3BySkZPojuTekIyep2Q35u5GT0ckQ24OychJdEdyb0hGz1OyG3N3o6cjkiE3h2TkJLojuTcko+cp2Y25u9HTEcmQm0MychLdkdwbktHzlOzG3N3o6YhkyM0ZKRnf+OIv8C4ko3pIRs9Tshtzd6OnI5IhN4dkZIBkVA/J6HlKdmPubvR0RDLk5pCMDJCM6iEZPU/JbszdjZ6OSIbcHJKRAZJRPSSj5ynZjbm70dMRyZCbQzIyQDKqh2T0PCW7MXc3ejoiGXJzSEYGSEb1kIyep2Q35u5GT0ckY3K+8p3v3sjsT/O96ZLxxX/4kwdCMpyjlRgvGdOPcNHd6OmIZEyOcezpyFhEj4VztMY5WgOSUWU3ejoiGZNjHHs6MhbRY+EcrXGO1oBkVNmNno5IxuQYx56OjEX0WDhHa5yjNSAZVXajpyOSMTnGsacjYxE9Fs7RGudoDUhGld3o6YhkTI5x7OnIWESPhXO0xjlaA5JRZTd6OiIZk2McezoyFtFj4RytcY7WgGRU2Y2ejkjG5BjHno6MRfRYOEdrnKM1IBlVdqOnI5IxKHtz9uFCfv7hdx+RYTRzSsYff/UbC0AynKMnkEcyph/55LvR0xHJGBTjeKUjYxE9Fs7RGudoDUhGld3o6YhkDIpxvNKRsYgeC+dojXO0BiSjym70dEQyBsU4XunIWESPhXO0xjlaA5JRZTd6OiIZg2Icr3RkLKLHwjla4xytAcmoshs9HZGMQTGOVzoyFtFj4RytcY7WgGRU2Y2ejkhGSPpH8Fu/+eVn+eHvf/0u7Th+/Wc/fZevfvntj4j77CSj4lg4R2ucozUgGVV2o6cjkhES40gyao2Fc7TGOVoDklFlN3o6IhkhMY4ko9ZYOEdrnKMq/Pb//e4jtv+UZFTZjZ6OSEZIjCPJqDUWztEa56gKJGON3ejpiGSExDiSjFpj4RytcY7y0+oFyai7Gz0dkYyQGEeSUWssnKM1zlF+SMZKu9HTEcm4IT1TeHT4Pj2Cb7Q/ufdbRs7lEyTjf/7/X3dSZSycozXOUWb29OI5krHebvR0RDJuiHHchmRUHAvnaI1zlBmSsd5u9HREMm6IcdyGZFQcC+dojXOUk369eINkVNmNno5Ixg0xjtuQjIpj4RytcY5yQjJW3Y2ejkjGDTGO25CMimPhHK1xjrJxVC/eIBlVdqOnI5JxQ4zjNiSj4lg4R2uco2yQjLV3o6cjknFDjOM2JKPiWDhHa5yjPJzTizdIRpXd6OmIZNwQ47gNyag4Fs7RGucoDyTjCbvR0xHJuCFXxrH9yXYE25/Z+8m939KSbRwzj0X/NETPB8lwjvJzRS/eWEMynrAbPR2RjBtiHLchGRXHwjla4xxlgGQ8Zzd6OiIZN8Q4bkMyKo6Fc7TGOZrLdb14g2RU2Y2ejkjGDTGO25CMimPhHK1xjuZCMp62Gz0dkYwbYhy3IRkVx8I5WuMczeIuvXiDZFTZjZ6OSMYNMY7bkIyKY+EcrXGOZkEynrkbPR2RjANpR7B/CvcGrmfIev543htf/PhH79L+0bv2Z9q5PDeadSXj3lGIGJE1JMM5ijtH47lXL96oJRlP3o2ejkjGgRjHnpCMimPhHK1xjsZDMp68Gz0dkYwDMY49IRkVx8I5WuMc9V//0VxZbJJRZTd6OiIZB2Ice0IyKo6Fc7TGOSIZ239qN0gGyTCOJCPFWDhHa5yjDJJxfbFJRpXd6OmIZByIcewJyag4Fs7RGueIZGz/qd0gGQ+SjL2/nOnKFLZ87fs/eJf842gsSIZzdL0jkrH9p3aDZJAM40gyUoyFc7TGOSIZ239qN0gGyTCOJCPFWDhHa5wjkrH9p3aDZJAM40gyUoyFc7TGOSIZ239qN0gGyTCOJCPFWDhHa5wjkrH9p3aDZJAM40gyUoyFc7TGOSIZ239qN0gGyTCOJCPFWDhHa5yjNSAZVXajpyOScSDGsScko+JYOEdrnKM1IBlVdqOnI5JxIP3j2KadpzeuTGE7rHvjuPcfF8ozjhnGYsyIXHknT5MM54hkfDp2g2SQDONIMgqMhXO0xjlaA5JRZTd6OiIZB2Ice0IyKo6Fc7TGOVoDklFlN3o6IhkHYhx7QjIqjoVztMY5WgOSUWU3ejoiGQdiHHtCMiqOhXO0xjlaA5JRZTd6OiIZB2Ice0IyKo6Fc7TGOVoDklFlN3o6IhkHsjeOPX+gbo+e2epnb4Jb2gElGRHzce/vXVsynKPrHa3BGpLxhN3o6YhkHIhx7AnJqDgWztEa52gNSEaV3ejpiGQciHHsCcmoOBbO0RrnaA1IRpXd6OmIZByIcewJyag4Fs7RGudoDUhGld3o6YhkHIhx7AnJqDgWztEa52gNSEaV3ejpiGQciHHsCcmoOBbO0RrnaA1IRpXd6OmIZByIcewJyag4Fs7RGudoDUhGld3o6YhkHIhx7AnJqDgWztEa52gNSEaV3ejpiGTckHZQ+kewf8566P8t10dwL0+QjFmsIRl7cY62IRk9T8luzN2Nno5Ixg0xjtuQjIpj4RytcY7WgGRU2Y2ejkjGDTGO25CMimPhHK1xjtaAZFTZjZ6OSMYNMY7bkIyKY+EcrXGO1oBkVNmNno5Ixg0xjtuQjIpj4RytcY7WgGRU2Y2ejkjGDTGO25CMimPhHK1xjtaAZFTZjZ6OSMYNaYfmm7/6xUfMHcf2r7fONo7GYu5YOEdrnKM1IBlVdqOnI5JxQ4zjNiSj4lg4R2ucozUgGVV2o6cjknFDjOM2JKPiWDhHa5yjNSAZVXajpyOScUOM4zYko+JYOEdrnKM1IBlVdqOnI5JxQ4zjNiSj4lg4R2ucozUgGVV2o6cjknFDjOM2JKPiWDhHa5yjNSAZVXajpyOSEZJ2etq/SOmNnr/8qZ22PfZGM24K2+SUjLVZQzLaOEck49OxG3N3o6cjkhES40gyao2Fc7TGOVoDklFlN3o6IhkhMY4ko9ZYOEdrnKM1IBlVdqOnI5IREuNIMmqNhXO0xjlaA5JRZTd6OiIZITGOJKPWWDhHa5yjNSAZVXajpyOSERLjSDJqjYVztMY5WgOSUWU3ejoiGSExjiSj1lg4R2ucozUgGVV2o6cjkhES40gyao2Fc7TGOVoDklFlN3o6IhmD0s7TSEZ+0rmSgZUko41z9ATGSwbO7UZPRyRjUIzjlY6MRfRYOEdrnKM1IBlVdqOnI5IxKMbxSkfGInosnKM1ztEakIwqu9HTEckYFON4pSNjET0WztEa52gNSEaV3ejpiGQMinG80pGxiB4L52iNc7QGJKPKbvR0RDIGxThe6chYRI+Fc7TGOVoDklFlN3o6Ihlyc0ZKBvqJ7kjuDcnoeUp2Y+5u9HREMuTmkIycRHck94Zk9DwluzF3N3o6Ihlyc0hGTqI7kntDMnqekt2Yuxs9HZEMuTkkIyfRHcm9IRk9T8luzN2Nno5IhtwckpGT6I7k3pCMnqdkN+buRk9HJENuDsnISXRHcm9IRs9Tshtzd6OnI5IhN8cFlj86yh+S0fOUfCdHhmTs4os4Mi6w/NFR/pCMnqfkOzkyJGMXX8SRcYHlj47yh2T0PCXfyZEhGbv4Io6MCyx/dJQ/JKPnKflOjgzJ2KV9NDIyOsofHeXP9Lt/GP2SMbuTp4dkvJhdxNOjo/zRUf5Mv/uHQTKqhGS8mF3E06Oj/NFR/ky/+4dBMqqEZLyYXcTTo6P80VH+TL/7h0EyqoRkvJhdxNOjo/zRUf5Mv/uHQTKqhGS8mF3E06Oj/NFR/ky/+4dBMqqEZAAAitEvGZI/JAMAkAiSsVJIBgAgESRjpZAMAEAiSMZKIRkAgESQjJVCMgAAiSAZK4VkAAASQTJWCskAACSCZKwUkgEASATJWCkkAwCQCJKxUkgGACARJGOlkAwAQCJIxkp5hGT8RAam/Ut0dJQtOsqfcx2twQdZLnd9N0iGuMAKREf5QzJkpdz13SAZ4gIrEB3lz5MlA9iDZIgLrEB0lD8kA2ghGeICKxAd5Q/JAFpIhrjACkRH+UMygJYykjH7D/WsnLsusNmfY+XoKH9IBtBCMsQFViA6yh+SAbSQDHGBFYiO8odkZOPD//3f/8aYf3f7b+0x/bEMhmSIC6xAdJQ/JCMbJCMDJENcYAWio/whGdkgGRkgGeICKxAd5Q/JyAbJyADJEBdYgegof0hGNsZIxtHf8jThIBniAisQHeUPycgGycgAyRAXWIHoKH9IRjZIRgZIhrjACkRH+UMyskEyMkAyxAVWIDrKH5KRmZ6r/fr/s2fEz1eHZIgLrEB0lD8kIzMkYxYkQ1xgBaKj/CEZmYmTjIh3shIkQ1xgBaKj/CEZmSEZsyAZ4gIrEB3lD8mowsj/TJb/JBfJEBdYgegof0hGFUjGSEiGuMAKREf5QzKqMFcypn/8wZAMcYEViI7yh2RUgWSMhGQczle+890bmf1pvrfkBaYjHY0PyahCtGQ8WSlaSMbhGEcdjY+OVu0I4yEZIyEZh2McdTQ+Olq1I4wnTjL8H0daSMbhGEcdjY+OVu0I4yEZIyEZh2McdTQ+Olq1I4xhzH+Mi160kIzDMY46Gh8drdoRxkAyZkEyDsc46mh8dLRqRxhDnGQQi09DMg7HOOpofHS0akcYA8mYBcnYzd6cfbiQn3/43UdkGM26F5iOdFS9I8Rx5frv/3dJxqchGbsxjjrS0fXoCLMgGRkgGbsxjjrS0fXoCLMgGRkgGbsxjjrS0fXoCLMgGRkgGbsxjjrS0fXoCLMYLxlHmf6IBkAydmMcdaSj69ERZkEyMkAyXukfwW/95pef5Ye///W7tOP49Z/99F2++uW3PyLus1e5wHSko/U6QhwkIwMk4xXj6ALT0fXoiGTkgWRkgGS8YhxdYDq6Hh2RDGALyXjFOLrAdHQ9OiIZwBaS8YpxdIHp6Hp0RDKALSTjFePoAtPR9eiIZABbHioZPVN4dPg+PYJvtD+591tGzmXOC0xH2+ho1Y6AtSEZxtEFpqOT0dE2JANoIRnG0QWmo5PR0TYkA2ghGcbRBaajk9HRNiQDaCEZxtEFpqOT0dE2JANoIRnG0QWmo5PR0TYkA2ghGcbRBaajk9HRNiQDaCEZxtEFpqOT0dE2JANoIRmHx7H9yXYE25/Z+8m939KSbRx1pCMdbUMygBaSYRxdYDo6GR1tQzKAFpJhHF1gOjoZHW1DMoAWkmEcXWA6OhkdbUMygBaSYRxdYDo6GR1tQzKAFpJhHF1gOjoZHW1DMoCWxSWjHcH+KdwbuJ4h6/njeW988eMfvUv7R+/an2nn8txozr3AdKSjJ3cErA3JMI4uMB3piGQAIZAM4+gC05GOSAYQAskwji4wHemIZAAhkAzj6ALTkY5IBhACyTCOLjAd6YhkACGQjN3Ja/+Y3N4fluufwpavff8H75J/HHWkIx1d7whYG5JhHF1gOtIRyQBCIBnG0QWmIx2RDCAEkmEcXWA60hHJAEIgGcbRBaYjHZEMIASSYRxdYDrSEckAQiAZxtEFpiMdkQwgBJJhHF1gOtIRyQBCIBm7aefpjStT2A7r3ji2U5htHHWkIx1d7whYG5JhHF1gOtIRyQBCIBnG0QWmIx2RDCAEkmEcXWA60hHJAEIgGcbRBaYjHZEMIASSYRxdYDrSEckAQnioZPT8gbo9emarn70JbmkHdO0LTEc6ekJHwNqQDOPoAtORjkgGEALJMI4uMB3piGQAIZAM4+gC05GOSAYQAskwji4wHemIZAAhkAzj6ALTkY5IBhACyTCOLjAd6YhkACGQDOPoAtORjkgGEMLikrGXdlD6R7B/znro/y3XR3Avcy+wvehoGx2t2hGwNiTDOLrAdHQyOtqGZAAtJMM4usB0dDI62oZkAC0kwzi6wHR0MjrahmQALSTDOLrAdHQyOtqGZAAtJMM4usB0dDI62oZkAC0k48U3f/WLj5g7ju1fb51tHHWkIx1tQzKAFpJhHF1gOjoZHW1DMoAWkmEcXWA6OhkdbUMygBaSYRxdYDo6GR1tQzKAFpJhHF1gOjoZHW1DMoAWkmEcXWA6OhkdbUMygJaHSkabdnrav0jpjZ6//Kmdtj32RjNuCtvkvMDa6EhH63UErA3JeMU4usB0dD06IhnAFpLxinF0genoenREMoAtJOMV4+gC09H16IhkAFtIxivG0QWmo+vREckAtpCMV4yjC0xH16MjkgFsIRmvGEcXmI6uR0ckA9hCMl4xji4wHV2PjkgGsIVk7Kadp5GM/KRVLrA2OtJR9Y6AtSEZuzGOOtLR9egIeDIkYzfGUUc6uh4dAU+GZOzGOOpIR9ejI+DJkIzdGEcd6eh6dAQ8GZKxG+OoIx1dj46AJ0MypPAF9pzoKH9IBtBCMsQFViA6yh+SAbSQDHGBFYiO8odkAC0kQ1xgBaKj/CEZQAvJEBdYgegof0gG0EIyxAVWIDrKH5IBtJAMcYEViI7yh2QALSRDXGAFoqP8IRlAC8kQF1iB6Ch/SAbQQjLEBVYgOsofkgG0JJWM9rjKyOgof3SUP9P3HZgOyZB3oqP80VH+TN93YDokQ96JjvJHR/kzfd+B6ZAMeSc6yh8d5c/0fQemQzLknegof3SUP9P3HZgOyZB3oqP80VH+TN93YDpJJQMAAFSHZAAAgBBIBgAACIFkAACAEEgGAAAIgWQAAIAQSAYAAAiBZAAAgBBIBgAACIFkAACAEEgGAAAIgWQAAIAQSAYAAAiBZAAAgBBIBgAACIFkAACAEEgGAAAIgWQAAIAQSAYAAAiBZAAAgBBIBgAACIFkAACAEEgGAAAIgWQAAIAQSAYAAAiBZAAAgBBIBgAACIFkAACAEEgGAAAIgWQAAIAQSAYAAAiBZAAAgBBIBgAACIFkAACAEEgGAAAIgWQAAIAQSAYAAAiBZAAAgBBIBgAACIFkAACAEEgGAAAIgWQAAIAQSAYAAAiBZAAAgBBIBgAACIFkAACAEEgGAAAIgWQAAIAQSAYAAAiBZAAAgBBIBgAACIFkAACAEEgGpvHb3/7LvzH9zQDAJ7BX5yAZmIZDC6AK9uocJAPTcGgBVMFenYNkYBoOLYAq2KtzkAxMY9ah3f7envdw9OcrPrcMnwtj0LXnNhKSgWmQjDzPLcPnwhh07bmNhGRgGiQjz3PL8LkwBl17biMhGZgGycjz3DJ8LoxB157bSEhGIrJ9ifcu1x4yf16SkaeL6u+tIlfOdfQmZGalzzISkpGIbF9iknHu50e+/8yv+YT3VhGScf25TX8zhSAZicj2JSYZ535+5PvP/JpPeG8VIRnXn9v0N1MIkpGIbF9iknHu50e+/8yv+YT3VhGScf25TX8zhSAZE3CY2+eQ+fdme85XxKjidyzDe8AYZknP0TM1/UEVgmRMYKUL4K7nkPn3ZnvOJKPW9xz9kIz1IBkTWOkCuOs5ZP692Z4zyaj1PUc/JGM9SMYEVroA7noOmX9vtudMMmp9z9EPyVgPkjGBiC9r5gOQ7ZKLkIyI9xb9fKpc3lXeZxVGXt4jP0v06/junYNkTIBkkIzoZ3XX58pAlfdZBZJx7nV8985BMiZAMkhG9LO663NloMr7rALJOPc6vnvnIBkTIBkkI/pZ3fW5MlDlfVaBZJx7Hd+9c5CMCTxNMu56z1c+Y8RlPGtkZz2H6M+S4f08gaftz10CkfkzZoZkTOBph/yu90wy5j6H6M+S4f08gaftD8mYC8mYwNMO+V3vmWTMfQ7RnyXD+3kCT9sfkjEXkjGBpx3yu94zyZj7HKI/S4b38wSetj8kYy4kYwKZD/nRC/XKpbuSZIz8ztz1/qPfc/X3tioRZzy6u7vOfvTPoIVkTIBknHvPJINkIO6ZkwySEQHJmADJOPeeSQbJQNwzJxkkIwKSMQGSce49kwySgbhnTjJIRgQkYwJPO+R3veZd76HiJX209wzfpaPDXfE7XJEMZ/noa0Z/h49+V6eXWAiSMYEIsag40BFDEP17Zw3N0d4zfJeODnfF73BFMpzlo68Z/R0++l2dXmIhSMYEIsSi4kBHDEH07501NEd7z/BdOjrcFb/DFclwlo++ZvR3+Oh3dXqJhSAZE4gQi4oDHTEE0b931tAc7T3Dd+nocFf8Dlckw1k++prR3+Gj39XpJRaCZEygyiHP9p4zXH4jh+aKQFTpl2SM567Le9a5iOiXZMRBMiaQ4cLO8Pok49z7JBlx7+cJkIxz/26Gz1gRkjGBDBd2htcnGefeJ8mIez9PgGSc+3czfMaKkIwJZLiwM7w+yTj3PklG3Pt5AiTj3L+b4TNWhGRMIMOFPev1M1yEVy7vlSQj+nNlk4yI71J1solChudw5WfQQjImQDJIRvT7PPr6JOOZkIz2OVz5GbSQjAmQDJIR/T6Pvj7JeCYko30OV34GLSRjAiSDZES/z6OvTzKeCclon8OVn0ELyZjA0cts1uWX7TUjJKPnGWYb4rtkIkO/JGM8VfZn1jO58jNoIRkTqHLIs70myTj3HDJ/Z0jGeKrsz6xncuVn0EIyJlDlkGd7TZJx7jlk/s6QjPFU2Z9Zz+TKz6CFZEygyiHP9pok49xzyPydIRnjqbI/s57JlZ9BC8mYQIZxj3jN6IMaIRlHf77i+x9Jhmfrwhj/zKd/qAHbku3zVoFkTCCDEES8JsnI+f5HkuHZujDGP/PpH4pkpIVkTCCDEES8JsnI+f5HkuHZujDGP/PpH4pkpIVkTCCDEES8JsnI+f5HkuHZujDGP/PpH4pkpIVkTCCDEPS8zhWi39vI18k2yiuN3SzJeAIR4hX9bK+855HvbXq5hSAZEyAZeT7jlWdS5f1nhmTkebYko/+9TS+3ECRjAiQjz2e88kyqvP/MkIw8z5Zk9L+36eUWgmRMgGTk+YxXnkmV958ZkpHn2ZKM/vc2vdxCkIwJZJaMK6+5qmRk+Heju54FycjDlf9F4gk8+bNfgWRMIIMQRLwmycj//rNBMvJAMvqfz/Q3UwiSMYEMQhDxmiQj//vPBsnIA8nofz7T30whSMYEMghBxGuSjPzvPxskIw8ko//5TH8zhSAZEzh6mB3+uOc/q8eR77/K98RZmPucZ71OFZ7wGSMgGRMgGXme/6weR77/Kt8TZ2Huc571OlV4wmeMgGRMgGTkef6zehz5/qt8T5yFuc951utU4QmfMQKSMQGSkef5z+px5Puv8j1xFuY+51mvU4UnfMYISAYALEIG8bryHp4mLk+AZADAIpAMZINkAMAikAxkg2QAwCKQDGSDZAAAgBBIBgAACOEdyWj/RwAAANchGQAAIASSAQAAQiAZAAAgBJIBAABCIBkAACCEfwW1ZslvpQjBIAAAAABJRU5ErkJggg==)
### 10. 机械手卡片（有序）

让仓库把「手上物品」按到「输入物品」上（**只做右键**，不攻击）。

| 位置 | 材料 |
|---|---|
| 四角 | 纸 ×4 |
| 左右中 + 上中 | 铁板 ×3 |
| 正中 | 自动化核心 ×1 |
| 下中 | **机械手**（没装机械动力则用发射器） |

![机械手卡片](data:image/png;base64,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)

### 11. 动力锯卡片（有序）

让仓库按**切割配方**自动加工（输入物品 → 选定产物），也能给**原木去皮**。

| 位置 | 材料 |
|---|---|
| 四角 | 纸 ×4 |
| 左右中 + 上中 | 铁板 ×3 |
| 正中 | 自动化核心 ×1 |
| 下中 | **动力锯**（没装机械动力则用切石机） |

![动力锯卡片](data:image/png;base64,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)
### 没装机械动力时的备用配方（原版材料）

| 物品 | 备用配方 |
|---|---|
| 输入方块 | 黑曜石 ×8 + 漏斗 |
| 输出方块 | 黑曜石 ×8 + 投掷器 |
| 管理员凭证 | 下界合金块 + 钻石块 + 绿宝石块 |
| 添加成员工具 | 钻石 + 金锭 + 木棍 |
| 移除成员工具 | 钻石 + 铁锭 + 木棍 |
| 仓库指南 | 书 + 纸 |
| 合成升级卡 | 纸 ×4 + 铁锭 ×4 + 自动化核心 ×1（一次出 10 张） |

---

## 十、技术说明

- **modId**：`commhub`
- **依赖**：NeoForge 21.1.x（Minecraft 1.21.1）
- **可选依赖**：Patchouli（游戏内手册）、JEI（悬赏幽灵格拖拽）、机械动力（主要配方材料）
- **兼容性**：物品 / 液体 / 电能全部走标准能力接口
- **存档**：基于 SavedData，自动保存

---

## 游戏内手册

模组内置 **Patchouli 手册《仓库指南》**（配方：书 + 蓝图）。
安装 Patchouli 后右键即可打开，内容与本指南一致。

---

*本指南由模组作者整理，可自由转发给他人。*

---

## 更新记录

### 1.9.8（最新）
- 补齐 Patchouli 手册英文版缺失的「指令」条目

### 1.9.7
- 修复兜底工具 handler 未实现 `IItemHandlerModifiable`（仓库被删时点工具格会报错）
- 修复传物品页「背包」文字和格子重叠（背包下移到 158）

### 1.9.6
- **修复手册打不开**：Patchouli 1.20+ 要求 book.json 放 `data/`、书内容放 `assets/`，并开 `use_resource_pack`
- **修复管理员工具一拿就消失**：工具格子 handler 没实现 `IItemHandlerModifiable`，服务端 set 时抛异常导致物品丢失
- **界面加框框**：仓库（青）、邮箱（紫）、背包（绿）、管理工具栏（红）、左侧功能栏（黄）一眼区分

### 1.9.4
- **修复崩溃**：sable 2.0.3 + flywheel 兼容性崩溃（子级别块实体渲染 NPE）——mod 内置补丁，遇到异常跳过该渲染帧，不再崩溃
- **指南书**：没装 Patchouli 时物品不显示（创造栏/配方都没有）；装了才能获得

### 1.9.3
- 修复 1.9.2 的「注册表冻结」崩溃（指南书条件注册回退为安全方案）

### 1.9.0~1.9.2
- 权限系统：每仓库单管理员（主人不变 + 管理员可给予/转让）+ 智能管理员工具
- 管理员工具无配方，仓库自带；给予后自动变转让
- 邮箱 4×9=36 格 + 无限堆叠；领取按 64 拆分不丢
- 界面高度 248 不超屏；「绑定」+「重置工具」按钮
- 配方图滑槽改用 mcmod 官方渲染图
- 每仓库管理员存档修复；邮箱迁移不静默丢物品
- 绑定/重置后菜单自动刷新
