# Leaky 多模块整合计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 将 Forge 1.20.1 和 NeoForge 1.21.1 两个独立项目整合为一个多模块 Gradle 项目，共享核心逻辑，平台适配层分离，所有 bug 修复回移到 1.20.1。

**架构：** 采用 Gradle 多模块结构：`common` 模块存放共享代码（Mixin、工具类、接口、配置），`forge-1.20.1` 和 `neoforge-1.21.1` 模块仅包含平台入口和构建配置。common 模块编译为普通 Java 库，平台模块依赖 common 并通过 Mixin 配置引用 common 中的 Mixin 类。

**技术栈：** Forge 47.x + NeoForge 21.1.x + Mixin + CupboardConfig + Gradle 8.7 + Java 17/21

---

## 文件结构

```
Leaky-fix/
├── common/                                    ← 共享代码模块
│   ├── build.gradle
│   └── src/main/java/com/leaky/
│       ├── INearbyItemAwareEntity.java        ← 接口（无平台依赖）
│       ├── LeakMessageFormatter.java          ← 消息格式化工具类（从1.20.1移植增强）
│       ├── config/
│       │   └── CommonConfiguration.java       ← 配置类（含安全反序列化+边界验证）
│       └── mixin/
│           ├── ItemEntityMixin.java           ← Mixin（无平台API依赖）
│           ├── EntitySectionMixin.java        ← Mixin
│           └── ItemUpdateMixin.java           ← Mixin
├── forge-1.20.1/                              ← Forge 1.20.1 平台模块
│   ├── build.gradle
│   ├── gradle.properties
│   └── src/main/
│       ├── java/com/leaky/
│       │   └── Leaky.java                     ← Forge 入口（net.minecraftforge.*）
│       └── resources/
│           ├── META-INF/mods.toml
│           ├── META-INF/accesstransformer.cfg
│           ├── leaky.mixins.json
│           └── pack.mcmeta
├── neoforge-1.21.1/                           ← NeoForge 1.21.1 平台模块
│   ├── build.gradle
│   ├── gradle.properties
│   └── src/main/
│       ├── java/com/leaky/
│       │   └── Leaky.java                     ← NeoForge 入口（net.neoforged.*）
│       └── resources/
│           ├── META-INF/neoforge.mods.toml
│           ├── META-INF/accesstransformer.cfg
│           ├── leaky.mixins.json
│           └── pack.mcmeta
├── build.gradle                               ← 根构建脚本
├── gradle.properties                          ← 共享版本属性
├── settings.gradle                            ← 多模块声明
├── gradlew / gradlew.bat
└── gradle/wrapper/
```

---

## 任务分解

### 任务 1：创建项目根结构

**文件：**
- 创建：`settings.gradle`
- 创建：`gradle.properties`
- 创建：`build.gradle`

- [ ] **步骤 1：创建 settings.gradle**

```groovy
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url = 'https://maven.minecraftforge.net/' }
        maven { url = 'https://maven.neoforged.net/releases' }
    }
}

rootProject.name = 'Leaky-fix'
include 'common'
include 'forge-1.20.1'
include 'neoforge-1.21.1'
```

- [ ] **步骤 2：创建 gradle.properties**

```properties
org.gradle.jvmargs=-Xmx3G
mod_version=2.3
modid=leaky

# Forge 1.20.1
forge_mc_version=1.20.1
forge_version=47.0.3
forge_mappings_channel=official
forge_mappings_version=1.20.1

# NeoForge 1.21.1
neoforge_version=21.1.72
neoforge_mc_version=1.21.1
```

- [ ] **步骤 3：创建根 build.gradle**

```groovy
subprojects {
    tasks.withType(JavaCompile).configureEach {
        options.encoding = 'UTF-8'
    }
}
```

- [ ] **步骤 4：复制 gradle wrapper**

从现有项目复制 `gradlew`、`gradlew.bat`、`gradle/wrapper/` 到根目录。

- [ ] **步骤 5：Commit**

```bash
git add settings.gradle gradle.properties build.gradle gradlew gradlew.bat gradle/
git commit -m "chore: initialize multi-module project structure"
```

---

### 任务 2：创建 common 模块

**文件：**
- 创建：`common/build.gradle`
- 创建：`common/src/main/java/com/leaky/INearbyItemAwareEntity.java`
- 创建：`common/src/main/java/com/leaky/LeakMessageFormatter.java`
- 创建：`common/src/main/java/com/leaky/config/CommonConfiguration.java`
- 创建：`common/src/main/java/com/leaky/mixin/ItemEntityMixin.java`
- 创建：`common/src/main/java/com/leaky/mixin/EntitySectionMixin.java`
- 创建：`common/src/main/java/com/leaky/mixin/ItemUpdateMixin.java`
- 创建：`common/src/test/java/com/leaky/LeakMessageFormatterTest.java`

- [ ] **步骤 1：创建 common/build.gradle**

```groovy
plugins {
    id 'java-library'
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

repositories {
    mavenCentral()
    maven { url = "https://www.cursemaven.com" }
}

dependencies {
    implementation 'com.google.code.gson:gson:2.10.1'
    implementation 'it.unimi.dsi:fastutil:8.5.12'
    implementation "curse.maven:cupboard-326652:4669193"
    implementation 'org.spongepowered:mixin:0.8.5'

    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
}

test {
    useJUnitPlatform()
}
```

注意：common 模块需要 MC 的类路径来编译 Mixin，但不需要完整的 MC 运行环境。实际构建时，平台模块会提供 MC 依赖。common 模块的编译需要通过平台模块的 `compileOnly` 或 `api` 依赖来间接获取 MC 类。如果独立编译失败，可改为在平台模块中直接包含 common 源码（sourceSets 配置）。

- [ ] **步骤 2：创建 INearbyItemAwareEntity.java**

直接从 NeoForge 1.21.1 版本复制，无任何修改。

- [ ] **步骤 3：创建 LeakMessageFormatter.java**

从 Forge 1.20.1 版本移植，增加 Component 构建支持：

```java
package com.leaky;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class LeakMessageFormatter
{
    private LeakMessageFormatter()
    {
    }

    public static String formatBeforePosition(final int itemCount)
    {
        return "发现: " + itemCount + "的物品堆叠量 | 位于 ";
    }

    public static String formatAfterPosition(final String dimensionName, final String playerName, final boolean removedItems)
    {
        final StringBuilder builder = new StringBuilder()
            .append(" | 维度: ")
            .append(dimensionName)
            .append(" | 最近的玩家ID为 ")
            .append(playerName);

        if (removedItems)
        {
            builder.append(". 已自动清理物品以防止卡顿");
        }

        return builder.toString();
    }

    public static String formatFullMessage(final int itemCount, final String position, final String dimensionName, final String playerName,
        final boolean removedItems)
    {
        return formatBeforePosition(itemCount) + position + formatAfterPosition(dimensionName, playerName, removedItems);
    }

    public static MutableComponent buildComponent(final int itemCount, final String positionShort,
        final String dimensionLocation, final String dimensionTranslationKey,
        final String playerName, final String tpCommand, final boolean removedItems)
    {
        MutableComponent component = Component.literal(formatBeforePosition(itemCount))
            .withStyle(ChatFormatting.RED)
            .append(Component.literal("[" + positionShort + "]")
                .withStyle(ChatFormatting.YELLOW)
                .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand))))
            .append(Component.literal(formatAfterPosition(dimensionLocation, playerName, removedItems)
                .replaceFirst(" \\| 维度: ", " | 维度: ")));

        return component;
    }
}
```

- [ ] **步骤 4：创建 CommonConfiguration.java**

合并两个版本的优点：1.20.1 的 `getSafeXxx()` 方法 + 1.21.1 的边界验证 + 中文描述：

```java
package com.leaky.config;

import com.cupboard.config.ICommonConfig;
import com.google.gson.JsonObject;

public class CommonConfiguration implements ICommonConfig
{
    public int     reportInterval      = 60 * 3;
    public String  chatnotification    = "PLAYER";
    public boolean highlightitems      = true;
    public int     reportThreshold     = 200;
    public int     autoremovethreshold = 400;
    public boolean improveItemPerformance = true;

    public CommonConfiguration() {}

    public JsonObject serialize() { /* 中文描述版本，从1.20.1移植 */ }

    public void deserialize(JsonObject data)
    {
        reportInterval = getSafeInt(data, "reportInterval", reportInterval);
        chatnotification = getSafeString(data, "chatnotification", chatnotification);
        highlightitems = getSafeBoolean(data, "highlightitems", highlightitems);
        improveItemPerformance = getSafeBoolean(data, "improveItemPerformance", improveItemPerformance);
        reportThreshold = getSafeInt(data, "reportThreshold", reportThreshold);
        autoremovethreshold = getSafeInt(data, "autoremovethreshold", autoremovethreshold);

        validate();
    }

    private void validate()
    {
        if (reportInterval < 1) { reportInterval = 60 * 3; }
        if (reportThreshold < 1) { reportThreshold = 200; }
        if (autoremovethreshold < reportThreshold) { autoremovethreshold = 400; }
        if (!chatnotification.equalsIgnoreCase("PLAYER") && !chatnotification.equalsIgnoreCase("EVERYONE")
            && !chatnotification.equalsIgnoreCase("OP") && !chatnotification.equalsIgnoreCase("NONE"))
        { chatnotification = "PLAYER"; }
    }

    // getSafeInt / getSafeString / getSafeBoolean 从1.20.1移植
}
```

- [ ] **步骤 5：创建 Mixin 类**

从 NeoForge 1.21.1 版本复制（已包含所有修复），但需注意：
- `ItemUpdateMixin.java` 中 `tickCount / 200`（整数除法）— 已修复
- `ItemUpdateMixin.java` 中 `closePlayer.isRemoved()` 检查 — 已修复
- `ItemEntityMixin.java` 中 `reported = false` 重置 — 已修复
- `EntitySectionMixin.java` 中 `leaky$lastReportTime` 冷却 — 已修复
- `nearbyItems = items` 直接赋值 — 已修复

- [ ] **步骤 6：创建 LeakMessageFormatterTest.java**

从 Forge 1.20.1 版本复制，无需修改。

- [ ] **步骤 7：Commit**

```bash
git add common/
git commit -m "feat: add common module with shared code"
```

---

### 任务 3：创建 Forge 1.20.1 平台模块

**文件：**
- 创建：`forge-1.20.1/build.gradle`
- 创建：`forge-1.20.1/gradle.properties`
- 创建：`forge-1.20.1/src/main/java/com/leaky/Leaky.java`
- 创建：`forge-1.20.1/src/main/resources/META-INF/mods.toml`
- 创建：`forge-1.20.1/src/main/resources/META-INF/accesstransformer.cfg`
- 创建：`forge-1.20.1/src/main/resources/leaky.mixins.json`
- 创建：`forge-1.20.1/src/main/resources/pack.mcmeta`

- [ ] **步骤 1：创建 forge-1.20.1/build.gradle**

基于现有 1.20.1 的 build.gradle，添加对 common 模块的依赖：

```groovy
dependencies {
    implementation project(':common')
    minecraft "net.minecraftforge:forge:${forge_mc_version}-${forge_version}"
    // ...
}

sourceSets.main.java.srcDirs += '../common/src/main/java'
```

注意：由于 Mixin 需要在平台模块的类路径中，最简单的方式是将 common 的源码目录加入平台模块的 sourceSets，而非通过 jar 依赖。这样 Mixin 类会被直接编译到平台模块的 jar 中。

- [ ] **步骤 2：创建 Leaky.java（Forge 入口）**

从 1.20.1 版本移植，应用所有 1.21.1 的修复：
- 维度感知的 reportedLocations（String 键）
- effectiveSize 替代 size/=2
- 冷却逻辑不跳过清理
- 消息发送改为单次（移除 for 循环5次）
- 消息颜色改为 RED
- /tp 改为 /execute in
- getServer() 空检查
- 使用 LeakMessageFormatter 构建消息
- cfg 局部变量统一引用

- [ ] **步骤 3：创建资源文件**

从现有 1.20.1 版本复制 mods.toml、accesstransformer.cfg、pack.mcmeta，更新版本号。

- [ ] **步骤 4：创建 leaky.mixins.json**

```json
{
  "required": false,
  "package": "com.leaky.mixin",
  "compatibilityLevel": "JAVA_17",
  "refmap": "leaky.refmap.json",
  "mixins": ["ItemEntityMixin", "EntitySectionMixin", "ItemUpdateMixin"],
  "client": [],
  "injectors": { "defaultRequire": 1 },
  "minVersion": "0.8"
}
```

- [ ] **步骤 5：构建验证**

运行：`./gradlew :forge-1.20.1:build`
预期：BUILD SUCCESSFUL

- [ ] **步骤 6：Commit**

```bash
git add forge-1.20.1/
git commit -m "feat: add Forge 1.20.1 platform module with all fixes backported"
```

---

### 任务 4：创建 NeoForge 1.21.1 平台模块

**文件：**
- 创建：`neoforge-1.21.1/build.gradle`
- 创建：`neoforge-1.21.1/gradle.properties`
- 创建：`neoforge-1.21.1/src/main/java/com/leaky/Leaky.java`
- 创建：`neoforge-1.21.1/src/main/resources/META-INF/neoforge.mods.toml`
- 创建：`neoforge-1.21.1/src/main/resources/META-INF/accesstransformer.cfg`
- 创建：`neoforge-1.21.1/src/main/resources/leaky.mixins.json`
- 创建：`neoforge-1.21.1/src/main/resources/pack.mcmeta`

- [ ] **步骤 1：创建 neoforge-1.21.1/build.gradle**

基于现有 1.21.1 的 build.gradle，添加对 common 模块的源码引用：

```groovy
sourceSets.main.java.srcDirs += '../common/src/main/java'

dependencies {
    implementation "net.neoforged:neoforge:${neoforge_version}"
    // cupboard 依赖
}
```

- [ ] **步骤 2：创建 Leaky.java（NeoForge 入口）**

从当前 1.21.1 版本移植，改用 LeakMessageFormatter 构建消息，其余逻辑保持不变。

- [ ] **步骤 3：创建资源文件**

从现有 1.21.1 版本复制 neoforge.mods.toml、accesstransformer.cfg、pack.mcmeta。

- [ ] **步骤 4：创建 leaky.mixins.json**

```json
{
  "required": false,
  "package": "com.leaky.mixin",
  "compatibilityLevel": "JAVA_21",
  "mixins": ["ItemEntityMixin", "EntitySectionMixin", "ItemUpdateMixin"],
  "client": [],
  "injectors": { "defaultRequire": 1 },
  "minVersion": "0.8"
}
```

- [ ] **步骤 5：构建验证**

运行：`./gradlew :neoforge-1.21.1:build`
预期：BUILD SUCCESSFUL

- [ ] **步骤 6：Commit**

```bash
git add neoforge-1.21.1/
git commit -m "feat: add NeoForge 1.21.1 platform module using common code"
```

---

### 任务 5：清理和最终验证

- [ ] **步骤 1：双平台构建验证**

运行：`./gradlew build`
预期：两个平台模块均 BUILD SUCCESSFUL

- [ ] **步骤 2：运行 common 模块单元测试**

运行：`./gradlew :common:test`
预期：LeakMessageFormatterTest 全部通过

- [ ] **步骤 3：验证 jar 输出**

检查 `forge-1.20.1/build/libs/` 和 `neoforge-1.21.1/build/libs/` 中生成的 jar 文件，确认包含所有 Mixin 类和资源文件。

- [ ] **步骤 4：最终 Commit**

```bash
git add -A
git commit -m "chore: complete multi-module project integration"
```

---

## 关键技术决策

1. **源码引用 vs jar 依赖**：common 模块通过 `sourceSets.main.java.srcDirs` 将源码直接加入平台模块，而非编译为 jar 依赖。原因：Mixin 类必须在目标平台模块的类路径中，且不同平台的 Mixin 配置（compatibilityLevel、refmap）不同。

2. **LeakMessageFormatter 双模式**：同时提供纯文本格式化（用于日志和测试）和 Component 构建（用于游戏内消息），兼顾可测试性和样式支持。

3. **CommonConfiguration 合并策略**：采用 1.20.1 的 `getSafeXxx()` 方法（更优雅）+ 1.21.1 的边界验证逻辑（更健壮），中文描述。

4. **Cupboard 依赖**：两个平台使用不同版本的 Cupboard（Forge 1.20.1 和 NeoForge 1.21.1 的 Cupboard 版本不同），在各自的 build.gradle 中声明。
