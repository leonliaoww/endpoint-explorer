# Spring Boot Endpoint Explorer — IntelliJ IDEA Plugin

## 简介

一款 IntelliJ IDEA 插件，自动扫描 Spring Boot 项目中的所有 REST API 端点，在工具窗口中按 Controller 分组展示。支持双击跳转到源码。

## 功能亮点

- 🔍 **自动扫描** — 识别 @RestController / @Controller 及所有请求映射注解（纯文本匹配，无需依赖解析）
- ⚙️ **自定义注解** — 支持配置公司内部的自定义注解（如 `@MyController`、`@MyGetMapping:GET`）
- 🎨 **彩色徽章** — GET(绿)、POST(蓝)、PUT(橙)、DELETE(红)、PATCH(紫)
- 📂 **按 Controller 分组** — 显示基路径和端点数量
- 🎯 **一键导航** — 双击端点跳转到对应的 Java 方法
- 🔎 **实时搜索** — 按路径、方法名、HTTP 方法过滤
- 🔄 **一键刷新** — 点击 Refresh 重新扫描
- 🚫 **自动排除** — 跳过 JDK 和依赖库文件，只扫项目源码

## 效果

在 IntelliJ IDEA 右侧 ToolWindow 面板找到 "Spring Endpoints"：

```
┌─ Spring Endpoints ─────────────────────┐
│ [🔄] [⤢] [⤣] | [⚙️] [🔍 Search...]  │
│ 📁 HomeController (3)                  │
│   🟢 GET    /                          │
│ 📁 QuestionController (3)              │
│   🟢 GET    /questions/{moduleKey}     │
│   🟢 GET    /question/{id}             │
│   🟢 GET    /buy                       │
│ 📁 UnlockController (2)                │
│   🟢 GET    /unlock                    │
│   🔵 POST   /unlock                    │
│────────────────────────────────────────│
│ Java files: 3 | 8 endpoints in 3       │
│ controllers                            │
└────────────────────────────────────────┘
```

## 前提

- IntelliJ IDEA **2021.1+**（Ultimate 或 Community Edition）
- JDK **11+**
- 项目使用 Spring Boot（含 @Controller / @RestController）

## 自定义注解配置

有些公司会自己封装注解，比如 `@MyController` 代替 `@Controller`，`@MyGetMapping` 代替 `@GetMapping`。

### 配置方法

点击工具栏的 **齿轮 ⚙️ 按钮**，弹出配置窗口：

```
► Custom Controller Annotations          ← 自定义 Controller 注解
  [MyController           ] [Add]        ← 输入 + 添加
  ┌────────────────────────┐
  │ MyController           │ ← 已添加的注解
  │ MyApiController        │
  └────────────────────────┘ [Remove]

► Custom Mapping Annotations             ← 自定义 Mapping 注解
  [MyGetMapping:GET       ] [Add]        ← 格式：注解名:HTTP方法
  ┌────────────────────────┐
  │ MyGetMapping:GET       │ ← 已添加的注解
  └────────────────────────┘ [Remove]
```

**格式说明：**

| 类型 | 输入示例 | 说明 |
|------|---------|------|
| Controller 注解 | `MyController` | 不加 `@` 符号，只输名字 |
| Controller 注解 | `MyApiController` | 同时支持多个 |
| Mapping 注解 | `MyGetMapping:GET` | 冒号分隔，后面是 HTTP 方法 |
| Mapping 注解 | `MyPostMapping:POST` | HTTP 方法必须大写 |

配置会自动保存，重启 IDEA 也不会丢。

### 存储位置

```
C:\Users\你的用户名\AppData\Roaming\JetBrains\IntelliJIdea2021.1\options\endpointExplorer.xml
```

可以手动编辑这个 XML 文件，效果一样。

## 构建

```bash
# 需要 JDK 11+ 和 IntelliJ IDEA

cd endpoint-explorer

# 构建插件
.\gradlew.bat buildPlugin

# 产出在 build/distributions/endpoint-explorer-1.0.0.zip
```

## 安装

1. 打开 IntelliJ IDEA → File → Settings → Plugins
2. 齿轮图标 ⚙️ → Install Plugin from Disk...
3. 选择 `build/distributions/endpoint-explorer-1.0.0.zip`
4. 重启 IDEA
5. 在右侧工具窗口找到 "Spring Endpoints" 面板
6. 打开一个 Spring Boot 项目，面板会自动扫描

## 在 IDEA 2021 下开发

构建插件时需要在 `build.gradle.kts` 中配置本地的 IDEA 安装路径：

```kotlin
intellij {
    localPath.set("D:\\Program Files\\JetBrains\\IntelliJ IDEA 2021.1.3")
    type.set("IC")
    ideaDependencyCachePath.set("D:/.idea-cache")  // 可选：缓存目录
}
```

## 在简历上的用法

```
个人项目: Spring Boot Endpoint Explorer (IntelliJ IDEA 插件)
- 基于 IntelliJ Plugin SDK + PSI API 实现 Spring Boot 项目 REST 端点自动扫描
- 纯文本注解匹配，无需依赖解析，兼容 IDEA 2021+
- 通过 ToolWindow + JTree 实现按 Controller 分组展示，带彩色 HTTP 方法徽章
- 支持自定义注解配置，适配企业内部封装的 @MyController 等场景
- 实现双击源码导航、实时搜索过滤、一键刷新
  ```

面试时直接打开 IDEA 展示插件，效果远好于口头描述。

## 技术栈

- Java 11
- IntelliJ Platform SDK (2021.1+)
- PSI (Program Structure Interface) + FileTypeIndex
- PersistentStateComponent（配置持久化）
- Gradle + org.jetbrains.intellij plugin
