# 若水三千 Health Connect 数据同步 APP 开发计划

## 1. 项目目标

开发一款 Android 健康数据同步 APP，名称为 **若水三千**。

核心链路：

```text
Zepp / Amazfit
  -> Health Connect
  -> 若水三千 Android APP
  -> 本地仪表盘展示
  -> 低功耗批量上传到自有服务器
```

本项目采用“轻本地 + 云端编译”模式：本地只维护代码，不强制安装 Android Studio、Android SDK、Gradle 或模拟器；APK 由 GitHub Actions 自动构建。

## 2. 技术选型

| 模块 | 技术 |
|---|---|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 健康数据 | Health Connect SDK |
| 后台任务 | WorkManager |
| 网络上传 | Retrofit + Kotlin Serialization |
| 架构 | MVVM / Repository 分层 |
| 构建 | GitHub Actions + Gradle |

Health Connect 当前采用官方最新稳定版 `androidx.health.connect:connect-client:1.1.0`，后续如需接入新能力，再评估升级到 `1.2.x alpha`。

## 3. 第一版 MVP 范围

已落地的第一版目标：

- APP 名称：若水三千
- 深色健康类仪表盘首页
- Health Connect 可用性检测
- Health Connect 权限申请入口
- 读取今日步数、平均心率、睡眠时长、最近血氧
- WorkManager 每 60 分钟调度一次后台同步
- Retrofit 上传接口占位：`POST /health/summary`
- GitHub Actions 云端构建 Debug APK 并上传 artifact

暂未在第一版加入 Room，原因是当前仓库从零开始，先保证 Health Connect 授权、读取、UI 和云端打包主链路闭环。下一阶段再加入本地缓存和上传队列，风险更低。

## 4. 后续阶段

### Phase 2：本地缓存与上传队列

- 引入 Room
- 保存每日健康摘要
- 保存 `lastSyncTime`
- 建立上传队列和失败重试记录
- UI 从本地缓存优先展示

### Phase 3：历史趋势

- 日、周、月趋势页
- 步数柱状图
- 心率曲线
- 睡眠统计
- 血氧趋势

### Phase 4：服务器端

推荐服务端：

| 模块 | 技术 |
|---|---|
| API | FastAPI |
| 数据库 | PostgreSQL |
| 鉴权 | JWT |
| 部署 | Docker |

建议接口：

```text
POST /health/summary
GET /health/summary/daily
GET /health/summary/range
```

### Phase 5：健康分析

- 睡眠评分
- 心率异常提示
- 周报和月报
- AI 健康摘要
- Web Dashboard

## 5. 低功耗原则

不采用：

- VPN 抓包
- Root / Hook
- Accessibility 常驻监听
- BLE 原始协议解析
- 常驻前台服务

采用：

- Health Connect 官方生态
- WorkManager 周期任务
- 本地缓存优先
- 批量上传
- 网络可用时再同步

## 6. 云端构建流程

```text
修改代码
  -> Git Push
  -> GitHub Actions
  -> 安装 JDK / Android SDK / Gradle
  -> gradle assembleDebug
  -> 下载 若水三千-debug-apk
```

工作流文件：

```text
.github/workflows/android.yml
```

## 7. 当前目录结构

```text
app/
  src/main/java/com/ruoshui/health/
    data/
    health/
    network/
    ui/
    worker/
.github/workflows/
gradle/libs.versions.toml
```

## 8. 真机测试建议

1. 将代码推送到 GitHub。
2. 在 Actions 页面下载 `若水三千-debug-apk`。
3. 安装到 Android 真机。
4. 确保 Zepp 已将数据写入 Health Connect。
5. 打开若水三千并授予步数、心率、睡眠、血氧读取权限。
6. 点击“同步”，查看仪表盘数据。

## 9. 发布前清单

- 替换 `BuildConfig.DEFAULT_API_BASE_URL`
- 增加隐私政策页面
- 检查 Health Connect 数据权限声明
- 配置正式签名
- 增加 Room 上传队列
- 增加错误态和空数据态
- 增加基础单元测试和 UI 截图检查
