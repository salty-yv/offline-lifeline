# OffLifeline

<p align="center">
  <img src="docs/assets/offlifeline-icon.png" alt="OffLifeline logo" width="140">
</p>

<p align="center">
  <strong>简体中文</strong> | <a href="./README.md">English</a>
</p>

<p align="center">
  <img src="docs/assets/offlifeline-background-4k.png" alt="OffLifeline cover" width="900">
</p>

## 项目概览

OffLifeline 是一款面向 Android 的离线生存助手。它不是云端聊天机器人，也不是医疗诊断或救援调度系统；它的目标是在断网、低电量、户外迷路、受伤、自然灾害等场景下，尽量把安全、保守、可执行的下一步建议留在手机本地。

应用基于 Kotlin 与 Jetpack Compose 构建，核心体验是一个对话式生存 Agent。用户可以输入文字，也可以附加本地图片；系统会从内置离线指南中检索相关内容，结合本地 Gemma / LiteRT-LM 推理生成分步骤建议，并在展示前经过安全规则校验。

## 功能亮点

- 完全离线优先：指南、工具和本地推理链路围绕无网络场景设计。
- 对话式 Agent：根据用户描述持续追问关键信息，并调整行动建议。
- 本地 RAG 指南库：内置设备、灾害、医疗、导航等主题的 Markdown 指南，并通过 SQLite FTS 检索。
- 多模态输入：支持相机或相册图片输入，图片会先在本地压缩和预处理。
- 安全约束层：对误食、蛇咬、洪水、火灾、严重出血等高风险场景加入保守规则。
- 本地工具箱：SOS 手电、屏幕 SOS、省电建议、个人应急信息卡、离线指南、Debug Log 导出。
- 中英文界面：应用内支持中文和 English 切换。
- 双构建风味：`lite` 用于轻量开发和模型选择流程，`bundled` 用于本地打包模型的离线版本。

## 架构概览

系统分为六层：Compose UI、Agent Runtime、SQLite FTS 离线 RAG、LiteRT-LM 推理、安全约束、以及本地存储与设备工具层。这样的拆分让模型负责理解、推理和生成，让确定性代码负责检索、状态管理、设备能力和不可妥协的安全边界。

Agent 不会把用户原始输入直接丢给模型。它会先整理近期对话上下文，识别迷路、低电量、出血、洪水、火灾、雷暴等风险域，选择关键追问，推荐本地工具，检索最多 5 段离线指南片段，然后把角色、语言、图片规则、本地参考资料和安全约束一起注入 Prompt。

## 模型与离线知识库

`bundled` 构建风味以 `Gemma-4-E2B-it-litert-lm` 作为主要本地模型，模型文件放在 `app/src/bundled/assets/models/`。运行时 `ModelAssetManager` 会从应用模型目录、持久化外部引用和 bundled assets 中查找模型，并在加载前校验文件大小与 SHA-256。

离线指南源文件位于 `app/src/main/assets/guides_src/`，按 device、disaster、medical、navigation 等主题组织。生成后的 `offline_guides.db` 包含 `guides`、`guide_chunks` 和 `guide_chunks_fts` 三类数据表；当前内置知识库包含 26 篇指南和 78 个 FTS 片段。检索链路结合关键词提取、查询扩展、主题定向 FTS 和风险域兜底检索，最终由 `RankFusion` 合并去重。

## 设计取舍

OffLifeline 的核心约束是：没有网络、建议必须保守、手机电量有限、错误建议代价高。因此项目选择端侧 LiteRT-LM 推理、SQLite FTS 离线检索、简短上下文摘要、最多 3 张图片输入、可取消生成和确定性 Safety Kernel。

它不是为了取代专业救援，而是为了在无法联网求助时，帮助用户得到更安全的下一步。

## 技术栈

- Android / Kotlin
- Jetpack Compose / Material 3
- LiteRT-LM
- Gemma-4-E2B-it / Gemma-4-E4B-it model manifest
- Room / SQLite FTS
- DataStore
- CameraX
- WorkManager
- OkHttp

## 项目结构

```text
app/src/main/java/com/example/offlinelifeline/
  agent/          对话 Agent、风险判断、问题规划、工具推荐、Prompt 构建
  inference/      LiteRT-LM、本地模型清单、模型完整性校验、下载与加载状态
  safety/         高风险场景安全约束与输出校验
  data/           Room 数据库、Repository、DataStore 设置
  device/         电量、手电、图片预处理等设备能力
  ui/             Compose 页面：聊天、工具箱、指南、信息卡、设置

app/src/main/assets/
  guides_src/     离线指南 Markdown 源文件
  databases/      生成后的 offline_guides.db

app/src/bundled/assets/models/
  本地 bundled 构建使用的模型目录，*.litertlm 默认不会提交到 Git
```

## 构建与运行

克隆仓库后，可用 Android Studio 打开项目，或在命令行构建：

```powershell
.\gradlew.bat assembleLiteDebug
```

构建成功后，APK 通常位于：

```text
app/build/outputs/apk/lite/debug/app-lite-debug.apk
```

如果要构建内置模型版本，请先准备 LiteRT-LM 模型文件，并放到：

```text
app/src/bundled/assets/models/gemma-4-E2B-it.litertlm
```

然后执行：

```powershell
.\gradlew.bat assembleBundledDebug
```

说明：模型文件通常超过 2GB，仓库已通过 `.gitignore` 排除 `*.litertlm`、`*.task`、`*.apk` 等大文件。发布 GitHub 仓库时，建议通过 Release、外部下载地址或项目说明提供模型获取方式。

## 安全边界

OffLifeline 提供的是离线自救辅助建议，不能替代专业救援、医生诊断、急救培训或官方灾害指引。在有信号或可联系外界时，应优先联系当地紧急服务、救援人员或医疗机构。

## 文档

- [Kaggle / project write-up](doc/kaggle_writeup_offlifeline.md)
- [Technical design notes](doc/offline_survival_agent_technical_doc_revised.md)
- [RAG migration plan](doc/rag_migration_plan.md)
