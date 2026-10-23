# WATUT 26.1.2 / NeoForge 移植

本地移植发行：`1.2.7+26.1.2-viking602.1`。目标 Minecraft 26.1.2、NeoForge 26.1.2.114、Java 25。基于用户提供的 [Viking602/WATUT](https://github.com/Viking602/WATUT) 的 1.21.0 分支（原目标 1.21.1），保留 Corosus 的作者署名、模组命名空间和 All Rights Reserved 声明。

## 安装

客户端和服务器都安装这两个 JAR，然后重启：

- `watut-neoforge-1.2.7+26.1.2-viking602.1.jar`
- `coroutil-neoforge-1.3.8+26.1.2-viking602.1.jar`

新旧 Minecraft 的网络协议和模组接口不同，本版本不能与 1.21.1 的 WATUT 混用。这里只实现 NeoForge；保留的旧 Fabric/Forge 文件不参与构建。

默认保留打字、操作界面、挂机、TAB 状态和物品转移展示。首次启动生成 `config/watut-client.toml`、`watut-server.toml`、`watut-common.toml`；客户端 `dontSendDetailedGUIInfo` 和 `dontSendItemInfo` 仍可控制信息分享。默认动态投影仅截取界面中心，服务端控制更新率、背景、模糊和显示范围。实验性的整屏模式使用最大 1024×1024、等比例留边，仍有较大的 CPU 和网络开销。

## 实现

- Identifier、NBT Optional 读取、ItemStack 注册表编解码、新版 GUI/输入/玩家模型 API。
- 通过新版粒子组提取渲染状态；为状态卡注册独立的双面透明管线，动态纹理按玩家管理。
- 使用独立 GuiRenderState/GuiRenderer 提取界面，异步 GPU 读回及后台压缩；CPU 模糊替代旧 OpenGL 后处理。
- 独立渲染目标在 `finally` 中恢复投影矩阵，沿用新版画中画渲染器注册接口，兼容实例中的 GUI 扩展。
- 粒子图集显式包含上游 `textures/particles` 贴图。
- 修复首次打开界面时过早记录“已处理界面”的问题；GPU 忙碌时保留截图请求以便重试。
- 服务端只中继允许的客户端字段，并以真实发送者 UUID 标记；限制图像尺寸、分片、解压大小，拒绝损坏的数据，释放纹理及本机缓冲。

## 构建

将 WATUT 与 CoroUtil 放在同一个父目录，使用 Java 25：

```powershell
cd WATUT
.\gradlew.bat :build :coroutil:build
```

根目录 `build.gradle` 是本移植的正式入口，Gradle 9.2.1、ModDevGradle 2.0.148。`build_neoforge.gradle` 引用同一个入口。产物位于各项目的 `build/libs`。

## 验证范围

临时探针在 `src/probe`，不包含在正式 JAR；`:probeJar` 仅生成测试 JAR。隔离测试世界使用离线测试身份，不访问原实例存档。

- 专用服务器：启动、配置序列化、双向 NBT 消息往返、字段中继规则、无效状态值。
- 两个真实客户端的局域网：远程打字动画、界面图像接收与非空像素、粒子提取、挂机动画、TAB 标记、物品转移消息。
- 原实例全部模组的副本：启动、新建世界、界面投影及截图，包含 Sodium/Iris/LDLib2/AE2 等现有模组。
- 图像压缩往返和损坏输入拒绝；验证 Minecraft、本机库、公共资源及原有模组的 SHA-256 不变。

移植时的完整整合包双客户端测试曾被 Ender IO Evolution 配方同步错误阻断：`enderio_evolution:melodic_chemical_conduit` 无法在 `neoforge:recipe_content` 解码，当时只完成隔离联机和完整包单人渲染验证。随后在独立的 SISE Compat `0.2.3+26.1.2` 修复中排除了该错误，完整 221 模组双客户端联机、WATUT 打字、界面投影、挂机和 TAB 状态均已通过；WATUT 与 CoroUtil 正式 JAR 的哈希保持不变。详情见 [Ender IO 联机修复记录](../SISE-Compat/docs/enderio-conduit-lan-sync.md)；原移植输出中的历史验证记录保持原样。

验证明细、最终 JAR 哈希、截图和安装记录保存在父目录的 `watut-port-20261011` 及 `outputs/watut-port-20261011`。
