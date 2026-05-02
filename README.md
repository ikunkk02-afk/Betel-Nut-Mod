# Betel Nut Mod

Betel Nut Mod 是一个面向 Fabric 的 Minecraft 生存扩展模组，围绕槟榔树、槟榔采集、营火烘烤、口味加工、村民交易、Farmer's Delight Refabricated 厨具联动、成瘾与戒断机制，以及槟榔空岛玩法展开。

本模组中的槟榔相关内容仅作为游戏机制与玩法创作使用，不代表现实健康建议，也不鼓励现实中食用槟榔。

## 支持版本

- Minecraft: `1.21.1`
- 当前模组版本: `4.0.0`
- Mod Loader: Fabric Loader `>=0.16.0`
- Java: `21` 或更高版本
- Mod ID: `betel-nut-mod`

## 必需前置

运行本模组需要安装以下前置：

- Fabric Loader `>=0.16.0`
- Fabric API `>=0.116.11+1.21.1`
- Cardinal Components API `6.x`
  - `cardinal-components-base`
  - `cardinal-components-entity`
  - `cardinal-components-world`
- MidnightLib `1.9.2+1.21.1-fabric`

Cardinal Components API 用于保存玩家的槟榔成瘾值、戒断值、上次食用时间，以及空岛生成状态和坐标等数据。

## 可选联动

- Farmer's Delight Refabricated

Farmer's Delight Refabricated 不是强制依赖。未安装时，本模组会保留原版工作台加工路线；安装后，相关口味槟榔的原版工作台配方会通过 Fabric Resource Conditions 隐藏，并改由 Farmer's Delight 的厨具配方提供。

## 主要玩法

- 在世界中寻找或种植槟榔树，获取生槟榔与槟榔叶。
- 使用营火将生槟榔烘烤为熟槟榔。
- 将熟槟榔与纸、不同材料组合，加工为多种口味槟榔。
- 食用不同槟榔获得短时增益，同时积累成瘾值。
- 食用槟榔后获得槟榔渣，并通过槟榔渣、槟榔渣块和堆肥建立资源循环。
- 长时间停止食用后，玩家会逐步进入戒断状态，并受到负面效果、生命上限惩罚和进食限制等影响。
- 通过成瘾 HUD 查看当前依赖阶段、成瘾进度和戒断倒计时。
- 创建普通槟榔空岛或槟榔一方块空岛世界，在虚空环境中围绕槟榔资源发展。
- 在空岛模式中通过水中召雷仪式或管理员指令补足进入末地的路线。
- 与农民村民交易，获取槟榔相关物品。
- 可通过配置文件调整世界生成、空岛、成瘾、戒断、交易、末影槟榔传送等行为。

## 槟榔渣与空岛玩法

从 `4.0.0` 开始，槟榔渣不只是食用后的副产物，也是空岛玩法的基础资源。玩家可以将槟榔渣放入堆肥桶获取骨粉，也可以用 9 个槟榔渣合成 1 个槟榔渣块；槟榔渣块可以拆回 9 个槟榔渣，并可作为扩建平台的基础方块。

模组提供两种空岛世界预设：

- 普通槟榔空岛：生成一座小型虚空岛屿，包含有限开局资源、功能方块、开局箱子和指南书，适合普通空岛生存流程。
- 槟榔一方块空岛：开局只生成 1 个槟榔渣块，并在玩家第一次进入时发放极少量初始物品，后续发展完全依赖玩家建立槟榔资源循环。

空岛只会在选择对应世界预设时启用。普通世界不会自动生成空岛、不会启用空岛虚空保护，也不会触发空岛专属的末地路线。

## 空岛指令与持久化

空岛生成状态通过 Cardinal Components API 的世界组件保存。普通空岛和一方块空岛分别记录生成状态与坐标，玩家对方块的破坏、放置和箱子物品变化由 Minecraft 存档正常保存，退出重进后不会重新覆盖为初始状态。

常用管理指令：

- `/betelskyblock generate`：生成普通槟榔空岛；如果已有生成记录，会提示先执行 reset。
- `/betelskyblock generate_one_block`：生成槟榔一方块空岛；如果已有生成记录，会提示先执行 reset_one_block。
- `/betelskyblock reset`、`/betelskyblock reset_one_block`：只重置生成状态，不会立即覆盖方块。
- `/betelskyblock force_generate`、`/betelskyblock force_generate_one_block`：强制覆盖生成，仅建议管理员在确认目标区域可覆盖时使用。
- `/betelskyblock tp`、`/betelskyblock tp_one_block`：传送到已记录的空岛出生点。
- `/betelskyblock generate_end_portal`：在管理员位置附近生成完整末地传送门结构，受配置项控制。

空岛模式还提供虚空保护。玩家在普通槟榔空岛或槟榔一方块空岛中掉入虚空时，会根据当前模式返回对应出生点；该逻辑不会影响普通世界。

## 配置与数据保存

MidnightLib 配置项用于控制普通空岛、一方块空岛、初始物品、虚空保护、槟榔渣堆肥概率和末地路线等行为。部分配置项会在世界创建或首次生成时生效，修改后可能需要重启游戏或重新创建世界才能完全体现。

Cardinal Components API 当前用于保存：

- 玩家成瘾值、戒断值、上次食用时间、死亡后保留状态和一方块初始物品领取状态。
- 世界空岛生成状态、空岛坐标、一方块坐标和出生点状态。

这些数据用于防止重复生成空岛、重复刷新开局箱子和重复发放一方块初始物品。

## 槟榔树系统

模组加入了槟榔树及对应木质方块，包括原木、木板、楼梯、台阶、栅栏、栅栏门、按钮、压力板、树叶和树苗。

槟榔树可以通过树苗种植，也可以在世界中自然生成。默认自然生成范围包括部分主世界生物群系，例如丛林、稀疏丛林、竹林和沙滩；红树林沼泽生成可通过配置开启。生成开关、生成概率、每区块树木数量以及最小/最大树高都可以在配置文件中调整。

## 营火烘烤

生槟榔可以通过营火烘烤为熟槟榔：

- `raw_betel_nut` -> `roasted_betel_nut`

熟槟榔是大多数后续加工配方的基础材料。

## 工作台加工

未安装 Farmer's Delight Refabricated 时，口味槟榔可以通过原版工作台制作。常见结构为：

- 熟槟榔
- 纸
- 对应口味材料

目前包含的口味与扩展槟榔包括：

- 辛辣槟榔
- 甜味槟榔
- 清凉槟榔
- 夜行槟榔
- 提神槟榔
- 蜂蜜槟榔
- 荧光槟榔
- 幻翼槟榔
- 末影槟榔
- 青金石槟榔
- 石英槟榔
- 岩浆槟榔
- 紫水晶槟榔
- 合成天下槟榔
- 琳琅天下槟榔
- 地下槟榔

安装 Farmer's Delight Refabricated 后，已有 Farmer's Delight 替代路线的口味槟榔工作台配方会被条件隐藏，避免同一种口味同时存在工作台与厨具两套路线。

## Farmer's Delight Refabricated 厨具联动

安装 Farmer's Delight Refabricated 后，模组会加载位于 `data/betel-nut-mod/recipe/compat/farmersdelight/` 的联动配方。

当前联动内容包括：

- Cooking Pot 制作多种口味槟榔。
- Cutting Board 处理槟榔树叶相关材料。
- 联动配方带有 `fabric:all_mods_loaded` 条件，只有检测到 `farmersdelight` 时才会加载。
- 对应的原版工作台口味配方带有反向条件，检测到 `farmersdelight` 时不会加载。

因此，在 REI/EMI 等配方查看器中，安装 Farmer's Delight Refabricated 时应只显示厨具路线；未安装时则显示原版工作台路线。

## 成瘾与戒断系统

食用槟榔会增加玩家的成瘾值，并重置当前戒断进度。不同口味槟榔的成瘾增量和效果不同，部分口味提供火焰抗性、速度、水下呼吸、夜视、急迫、生命恢复、缓降、幸运等效果。

当玩家达到配置中的最低成瘾值，并在一段时间内没有继续食用槟榔时，戒断值会随服务器刻逐步增长。戒断阶段会带来多种惩罚，包括：

- 移动速度降低
- 挖掘速度降低
- 饥饿
- 虚弱
- 反胃或黑暗等高阶段效果
- 生命上限惩罚
- 高阶段进食限制

牛奶、金苹果和附魔金苹果可以缓解或清除部分成瘾与戒断状态。成瘾与戒断数据通过 Cardinal Components API 持久化，并可按配置决定死亡后是否保留。

## 成瘾 HUD 系统

Betel Nut Mod 现在加入了成瘾 HUD 可视化系统。玩家在食用槟榔并产生依赖后，可以在屏幕上看到当前依赖阶段、成瘾进度和戒断倒计时。

HUD 会根据依赖程度显示不同的阶段状态，例如轻度依赖、中度依赖、重度依赖、严重依赖和极度依赖。戒断反应即将触发时，HUD 会提供更明显的提示，帮助玩家判断风险。

玩家也可以根据自己的习惯调整 HUD 位置，或者进入 HUD 编辑模式直接拖动 HUD。HUD 位置设置会保存在客户端本地，不会影响服务器成瘾、戒断和惩罚逻辑。

## HUD 位置调整

玩家可以通过客户端配置或 HUD 编辑模式调整槟榔成瘾 HUD 的位置。

如果已设置按键绑定，可以在游戏控制设置中找到：

- 调整槟榔 HUD 位置

进入编辑模式后，按住 HUD 并拖动即可调整位置。松开鼠标或按 ESC 后，位置会自动保存。

## 村民交易

农民村民会根据职业等级提供槟榔相关交易，包括：

- 生槟榔买卖
- 槟榔叶
- 熟槟榔
- 部分口味槟榔
- 合成天下槟榔

交易是否启用、绿宝石价格和物品数量可以通过配置文件调整。

## 安装方法

1. 安装 Minecraft `1.21.1` 的 Fabric Loader。
2. 安装 Fabric API。
3. 安装 Cardinal Components API。
4. 将本模组 jar 文件放入 `.minecraft/mods` 目录。
5. 如需 Farmer's Delight Refabricated 厨具联动，将 Farmer's Delight Refabricated 一并放入 `mods` 目录。

首次启动后，模组会在配置目录生成 `betel_nut.json`，可按需要调整玩法参数。客户端 HUD 设置会保存在 `betel_nut_mod_client.json` 中，用于记录每位玩家自己的 HUD 显示偏好。

## 构建方法

本项目使用 Gradle 与 Fabric Loom 构建。

常用开发命令：

```bash
./gradlew build
./gradlew checkSkyblockPersistence
./gradlew runClient
```

在 Windows PowerShell 中也可以使用：

```powershell
.\gradlew.bat build
.\gradlew.bat checkSkyblockPersistence
.\gradlew.bat runClient
```

`checkSkyblockPersistence` 是脚本化资源检查任务，用于确认空岛 CCA 保存字段、reset/generate 行为和开局箱子不重复刷新的关键约束仍然存在。

## 许可证

本项目使用 MIT License。详情见 [LICENSE](LICENSE)。
除特别说明外，项目内贴图、模型与数据文件均为作者原创。
Minecraft、Fabric、Farmer's Delight Refabricated 等名称与资源归其各自权利方所有，本项目仅进行兼容联动，不包含其原始代码或资源。
## 作者信息

- 作者：寿云
- GitHub: <https://github.com/ikunkk02-afk/Betel-Nut-Mod>
- Bilibili: <https://space.bilibili.com/1832031043?spm_id_from=333.1007.0.0>
