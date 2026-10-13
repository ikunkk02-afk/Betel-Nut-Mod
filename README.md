# Betel Nut Mod — NeoForge 1.21.1

这是 Fabric 5.7.0 的独立 NeoForge 移植工程，版本为 5.7.1，模组 ID 为 `betel_nut_mod`，Java 包名仍为 `betel.nut`。

此工程位于原仓库的 [`codex/neoforge-1.21.1` 分支](https://github.com/ikunkk02-afk/Betel-Nut-Mod/tree/codex/neoforge-1.21.1)。`main` 分支继续维护 Fabric 版。克隆此版本：

```sh
git clone --branch codex/neoforge-1.21.1 https://github.com/ikunkk02-afk/Betel-Nut-Mod.git betel-nut-neoforge
```

## 安装

- Minecraft **1.21.1**、Java **21**、NeoForge **21.1.259 或更新的 21.1.x**。
- 安装 `betel-nut-mod-neoforge-5.7.1.jar`。MidnightLib 1.9.2 已内嵌，无需额外安装。
- 可选：Farmer's Delight 1.21.1-1.3.4、Create 1.21.1-6.0.10。未安装时也可独立运行。
- 使用 NeoForge 原生注册、事件、网络与数据存储；无需 Fabric API、Cardinal Components API 或兼容加载器。

## 继承的玩法

- 四种槟榔树冠、折叠树叶网格、绿色冠鞘、青／黄果串；剪刀右键完整采收、自然重新结果、育苗、树叶衰败和自然树群。
- 生果营火烘烤、裹叶生槟榔、17 种口味、回响感知、蜂蜜恢复及特殊口味的增益与后效。
- 槟榔木板、楼梯、台阶、栅栏、栅栏门、按钮、压力板、门和活板门；保留燃料、可燃性与原版交互。
- 贝壳、贝壳粉、远古贝壳及钓鱼／古城战利品；农民、渔夫、牧师交易。
- 三款混合装、品牌封签和服务器决定结果的奖券。
- 按世界日期累计的依赖、戒断、口腔暴露、进食限制、恢复用品、紧凑 HUD 和 H 键布局编辑。
- 经典槟榔空岛、一方块空岛、开局物资、九页中英指南、手动堆肥、虚空救援、水中召雷仪式和末地路线。
- 两个进度页、28 个可见节点，以及原有隐藏任务触发器。
- 原有 `/betel` 与 `/betelskyblock` 指令。

## 可选联动

Farmer's Delight 安装后，原有口味工作台路线按资源条件切换为烹饪锅路线；砧板处理青果串得 3 个生果、黄果串得 5 个，完整树叶得 2 片槟榔叶。瓶装恢复品仍需玻璃瓶。

Create 提供 **33 条**可选生产配方：果串／木材／树叶切割、贝壳研磨、烟熏、机械手分步包叶、分层调味与加热浸提、注液器裹蜜、特殊槟榔与品牌包装的机械合成、瓶装恢复用品及槟榔渣压块。原有手工配方保留，在 Create 中则使用对应的显式自动化路线。

完整材料、用量、机器及工序见 [5.7.1 机械动力生产线](docs/create-production-5.7.1.md)。

所有联动均为可选，不把 Create 或 Farmer's Delight 打进发布包。

## 配置与存档

- `config/betel_nut.json`：原有玩法、自然生成、交易和依赖配置字段。
- `config/betel_nut_mod.json`：MidnightLib 空岛与特殊效果配置，可从模组配置入口编辑。
- 客户端 HUD 布局保留原配置文件名；H 键打开编辑界面。
- 玩家依赖与开局物资标记由 NeoForge 附件保存；死亡复制遵循原有规则。
- 空岛状态由主世界 `data/betel_nut_mod_skyblock.dat` 保存；重进不会重建岛屿或重复发物资。

Fabric 的命名空间是 `betel-nut-mod`，NeoForge 的命名空间是 `betel_nut_mod`。此工程没有提供旧 Fabric 世界的跨加载器转换器；请为 NeoForge 创建独立存档。NeoForge 版存档按本工程的 ID 和存储格式持续保存。

## 开发与验证

Windows 使用 `.\gradlew.bat`；Linux／macOS 使用 `./gradlew`。

```powershell
.\gradlew.bat build
.\gradlew.bat -PportTests runGameTestServer
.\gradlew.bat -PportTests -PcreateOnly runGameTestServer
.\gradlew.bat -PportTests -PcompatMods runGameTestServer
.\gradlew.bat -PportTests -PcompatMods runPortSmoke
python -X utf8 scripts/verify-port.py
```

`-PcompatMods` 仅为开发运行添加固定版本的 Farmer's Delight 和 Create；发布包不包含这两个模组。测试源码独立在 `src/gametest`，发布 JAR 与源码 JAR 均不包含测试或预览入口。客户端检查重复运行时用 `-PportSmokeDir=新的目录名` 指定独立的运行目录。

实际验证结果、截图和产物哈希见 [NeoForge 移植说明](docs/neoforge-port-5.7.0.md) 与 `verification/report.json`。其余版本文档继承自 Fabric 工程，历史验证记录不代表 NeoForge 验证；安装与平台信息以本文件为准。

MIT 许可。[原工程](https://github.com/ikunkk02-afk/Betel-Nut-Mod)。
