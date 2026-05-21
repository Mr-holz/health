# 若水三千

若水三千是一款 Android 健康数据同步 APP，用 Health Connect 读取 Zepp / Amazfit 写入的健康数据，并在本地仪表盘展示。项目按“轻本地 + GitHub Actions 云端编译”设计，不要求本机安装 Android Studio 或 Gradle。

## 当前功能

- Health Connect 可用性检测
- 步数、心率、睡眠、血氧读取权限申请
- 今日健康摘要仪表盘
- WorkManager 每 60 分钟低功耗同步
- Retrofit 上传接口占位：`POST /health/summary`
- GitHub Actions 自动构建 Debug APK

## 云端构建

推送到 GitHub 后，Actions 会执行：

```bash
gradle assembleDebug
```

构建完成后，在 workflow artifact 中下载 `若水三千-debug-apk`。

## 本地开发

本机如果已安装 JDK、Android SDK 和 Gradle，可以运行：

```bash
gradle assembleDebug
```

当前环境没有这些工具时，直接推送到 GitHub 让 Actions 构建即可。

## 配置服务器地址

第一版上传地址在 [app/build.gradle.kts](app/build.gradle.kts) 中：

```kotlin
buildConfigField("String", "DEFAULT_API_BASE_URL", "\"https://example.com/\"")
```

上线前请替换为你的服务器地址，并确保以 `/` 结尾。
