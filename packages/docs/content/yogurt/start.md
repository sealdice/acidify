# 开始使用

Yogurt 是基于 Acidify 的 [Milky](https://milky.ntqqrev.org/) 实现，支持 Kotlin/JVM 和 Kotlin/Native 平台。

> **Acid**ify + **Milk**y = Yogurt!

## 启动

Yogurt 支持的平台有 Kotlin/JVM 和 Kotlin/Native。

### 通过可执行文件启动 (Kotlin/Native)

在 [Releases](https://github.com/LagrangeDev/acidify/releases) 中下载对应平台的可执行文件，解压到工作目录后运行：

```
./yogurt.kexe   (for Linux and macOS)
yogurt.exe      (for Windows)
```

支持的平台如下：

| OS      | Arch       |
|---------|------------|
| Windows | x64        |
| Linux   | x64, arm64 |
| macOS   | x64, arm64 |

### 通过 Java 运行时启动 (Kotlin/JVM)

配置 Java 21+ 运行时，然后在 [Releases](https://github.com/LagrangeDev/acidify/releases) 中下载 JAR 文件，运行：

```
java -jar yogurt-jvm-all.jar
```

注意：Yogurt 的 JVM 版本理论上可以在任何支持 Java 21+ 的平台上运行，但由于 Yogurt 依赖 [LagrangeCodec](https://github.com/LagrangeDev/LagrangeCodec) 的预编译构建，因此只支持在以下平台**发送语音和视频消息**：

| OS      | Arch       |
|---------|------------|
| Windows | x86, x64   |
| Linux   | x64, arm64 |
| macOS   | x64, arm64 |
