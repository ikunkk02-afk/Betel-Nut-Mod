# NeoForge 5.8.1 本地验证

- 验证时间：2026-10-15 20:54–20:55（Asia/Shanghai）。
- 验证目录：`D:/Betel-Nut-Mod-main/betelnutmod-NeoForge-1.21.1`。
- Minecraft 1.21.1，NeoForge 21.1.259；GameTest 日志报告 Java 21.0.12.1。
- 执行命令：`.\gradlew.bat build runGameTestServer -PportTests --console=plain`。
- 结果：`BUILD SUCCESSFUL in 33s`；`All 137 required tests passed :)`。
- 运行环境：本地独立运行，未添加 Create 或 Farmer's Delight。本记录不代替 GitHub Actions 的运行结果。

新增测试覆盖全部 19 种可食用槟榔的主手／副手最后一颗替换、创造模式连续食用、生存／创造模式满背包掉落，以及不可食用原料、空栈和病变限制不产生槟榔渣。

安装包：`build/libs/betel-nut-mod-neoforge-5.8.1.jar`，1,125,878 字节。

SHA-256：`6fd9abd19263c93e742a1b40d065d4b99924abfe7b6a7c273bc6ab55efcb5037`。

原始本地日志保存在验证目录下的 `verification/residue-5.8.1.log`，仓库保留本摘要。

## 依赖解析复验

2026-10-15 21:11–21:12（Asia/Shanghai），改用 Modrinth 官方原始 CDN 的固定 MidnightLib 1.9.2 文件，绕过 Maven 重定向后的加号编码问题。Gradle 的依赖坐标仍为 `maven.modrinth:midnightlib:1.9.2+1.21.1-neoforge`。

再次执行 `.\gradlew.bat build runGameTestServer -PportTests --console=plain`：`BUILD SUCCESSFUL in 45s`，全部 **137 项 GameTest** 通过。本地原始日志：`verification/residue-5.8.1-cdn.log`。

直接从官方 CDN 获取的依赖和 JAR 内嵌依赖均为 60,347 字节，SHA-256 均为 `adb29c10b3bdc2fd1a1cb8f4cbeb0280f7d15af1a76ee3074d2db1eec7fe28e7`，与原缓存一致。`META-INF/jarjar/metadata.json` 的 artifactVersion 保持 `1.9.2+1.21.1-neoforge`，range 保持 `[1.9.2+1.21.1-neoforge,)`。生产 JAR 字节数和 SHA-256 与上方记录一致。
