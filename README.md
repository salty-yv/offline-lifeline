# OffLifeline

<p align="center">
  <img src="promo/offlifeline-icon.png" alt="OffLifeline logo" width="140">
</p>

<p align="center">
  <a href="#中文">简体中文</a> | <a href="#english">English</a>
</p>

<p align="center">
  <img src="promo/offlifeline-background-4k.png" alt="OffLifeline cover" width="900">
</p>

<a id="中文"></a>

## 中文

OffLifeline 是一款面向 Android 的离线生存助手。它不是云端聊天机器人，也不是医疗诊断或救援调度系统；它的目标是在断网、低电量、户外迷路、受伤、自然灾害等场景下，尽量把安全、保守、可执行的下一步建议留在手机本地。

应用基于 Kotlin 与 Jetpack Compose 构建，核心体验是一个对话式生存 Agent。用户可以输入文字，也可以附加本地图片；系统会从内置离线指南中检索相关内容，结合本地 Gemma / LiteRT-LM 推理生成分步骤建议，并在展示前经过安全规则校验。

### 功能亮点

- 完全离线优先：指南、工具和本地推理链路围绕无网络场景设计。
- 对话式 Agent：根据用户描述持续追问关键信息，并调整行动建议。
- 本地 RAG 指南库：内置设备、灾害、医疗、导航等主题的 Markdown 指南，并通过 SQLite FTS 检索。
- 多模态输入：支持相机或相册图片输入，图片会先在本地压缩和预处理。
- 安全约束层：对误食、蛇咬、洪水、火灾、严重出血等高风险场景加入保守规则。
- 本地工具箱：SOS 手电、屏幕 SOS、省电建议、个人应急信息卡、离线指南、Debug Log 导出。
- 中英文界面：应用内支持中文和 English 切换。
- 双构建风味：`lite` 用于轻量开发和模型选择流程，`bundled` 用于本地打包模型的离线版本。

### 架构概览

系统分为六层：Compose UI、Agent Runtime、SQLite FTS 离线 RAG、LiteRT-LM 推理、安全约束、以及本地存储与设备工具层。这样的拆分让模型负责理解、推理和生成，让确定性代码负责检索、状态管理、设备能力和不可妥协的安全边界。

Agent 不会把用户原始输入直接丢给模型。它会先整理近期对话上下文，识别迷路、低电量、出血、洪水、火灾、雷暴等风险域，选择关键追问，推荐本地工具，检索最多 5 段离线指南片段，然后把角色、语言、图片规则、本地参考资料和安全约束一起注入 Prompt。

### 模型与离线知识库

`bundled` 构建风味以 `Gemma-4-E2B-it-litert-lm` 作为主要本地模型，模型文件放在 `app/src/bundled/assets/models/`。运行时 `ModelAssetManager` 会从应用模型目录、持久化外部引用和 bundled assets 中查找模型，并在加载前校验文件大小与 SHA-256。

离线指南源文件位于 `app/src/main/assets/guides_src/`，按 device、disaster、medical、navigation 等主题组织。生成后的 `offline_guides.db` 包含 `guides`、`guide_chunks` 和 `guide_chunks_fts` 三类数据表；当前内置知识库包含 26 篇指南和 78 个 FTS 片段。检索链路结合关键词提取、查询扩展、主题定向 FTS 和风险域兜底检索，最终由 `RankFusion` 合并去重。

### 设计取舍

OffLifeline 的核心约束是：没有网络、建议必须保守、手机电量有限、错误建议代价高。因此项目选择端侧 LiteRT-LM 推理、SQLite FTS 离线检索、简短上下文摘要、最多 3 张图片输入、可取消生成和确定性 Safety Kernel。它不是为了取代专业救援，而是为了在无法联网求助时，帮助用户得到更安全的下一步。

### 技术栈

- Android / Kotlin
- Jetpack Compose / Material 3
- LiteRT-LM
- Gemma-4-E2B-it / Gemma-4-E4B-it model manifest
- Room / SQLite FTS
- DataStore
- CameraX
- WorkManager
- OkHttp

### 项目结构

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

### 构建与运行

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

### 安全边界

OffLifeline 提供的是离线自救辅助建议，不能替代专业救援、医生诊断、急救培训或官方灾害指引。在有信号或可联系外界时，应优先联系当地紧急服务、救援人员或医疗机构。

### 文档

- [Kaggle / project write-up](doc/kaggle_writeup_offlifeline.md)
- [Technical design notes](doc/offline_survival_agent_technical_doc_revised.md)
- [RAG migration plan](doc/rag_migration_plan.md)

<p align="right"><a href="#offlifeline">返回顶部</a></p>

---

<a id="english"></a>

## English

OffLifeline is an offline survival assistant for Android. It is not a cloud chatbot, a medical diagnosis product, or a rescue dispatch system. Its purpose is to keep conservative, actionable next-step guidance available on the phone when the user has no signal, low battery, an injury, bad weather, a disaster, or an outdoor emergency.

The app is built with Kotlin and Jetpack Compose. Its main experience is a conversational survival agent: the user can send text and local images, the app retrieves relevant guidance from an embedded offline knowledge base, Gemma / LiteRT-LM generates a step-by-step response locally, and a safety layer validates the answer before it is shown.

### Highlights

- Offline-first design: guidance, tools, and local inference are designed around no-network situations.
- Conversational agent: asks key follow-up questions and adjusts advice across turns.
- Local RAG knowledge base: Markdown survival guides are packaged into a SQLite FTS database.
- Multimodal input: camera and gallery images are preprocessed locally before inference.
- Safety layer: conservative constraints for high-risk topics such as poisoning, snake bite, floodwater, fire, and severe bleeding.
- Local toolbox: SOS flashlight, screen SOS, battery-saving advice, emergency card, offline guides, and debug log export.
- Bilingual UI: in-app switching between Chinese and English.
- Build flavors: `lite` for lightweight development and model-selection workflows, `bundled` for an offline package with a local model asset.

### Architecture

The system is split into six layers: Compose UI, Agent Runtime, SQLite FTS offline RAG, LiteRT-LM inference, safety constraints, and local storage/device tooling. This keeps the model responsible for understanding, reasoning, and generation, while deterministic code handles retrieval, state, device tools, and hard safety boundaries.

The agent does not pass a raw user message directly to the model. It first summarizes recent conversation history, detects risk domains such as lost outdoors, low battery, bleeding, flood, fire, or thunderstorm, selects key follow-up questions, recommends local tools, retrieves up to 5 offline guide chunks, and injects role, language, image rules, local references, and safety constraints into the prompt.

### Model and Offline Knowledge Base

The `bundled` build flavor uses `Gemma-4-E2B-it-litert-lm` as the primary local model, stored under `app/src/bundled/assets/models/`. At runtime, `ModelAssetManager` searches app model storage, persisted external references, and bundled assets, then verifies file size and SHA-256 before loading.

Offline guide sources live under `app/src/main/assets/guides_src/`, grouped by device, disaster, medical, and navigation topics. The generated `offline_guides.db` contains `guides`, `guide_chunks`, and `guide_chunks_fts` tables; the current embedded knowledge base contains 26 guides and 78 FTS chunks. Retrieval combines keyword extraction, query expansion, topic-targeted FTS, and risk-domain fallback retrieval, then merges and deduplicates results with `RankFusion`.

### Design Tradeoffs

OffLifeline is built around a hard set of constraints: no network, conservative advice, limited battery, and high consequences for bad guidance. That is why it uses on-device LiteRT-LM inference, SQLite FTS retrieval, short context summaries, a maximum of 3 image attachments, cancellable generation, and a deterministic Safety Kernel. It is not meant to replace professional rescue; it is meant to help the user find a safer next step when online help is unavailable.

### Tech Stack

- Android / Kotlin
- Jetpack Compose / Material 3
- LiteRT-LM
- Gemma-4-E2B-it / Gemma-4-E4B-it model manifests
- Room / SQLite FTS
- DataStore
- CameraX
- WorkManager
- OkHttp

### Project Layout

```text
app/src/main/java/com/example/offlinelifeline/
  agent/          Agent runtime, risk detection, question planning, tool routing, prompts
  inference/      LiteRT-LM, model catalog, integrity checks, download and runtime state
  safety/         Risk-specific constraints and output validation
  data/           Room database, repositories, DataStore settings
  device/         Battery, flashlight, and local image preprocessing
  ui/             Compose screens: chat, toolbox, guides, emergency card, settings

app/src/main/assets/
  guides_src/     Offline guide Markdown sources
  databases/      Generated offline_guides.db

app/src/bundled/assets/models/
  Local model directory for bundled builds; *.litertlm is ignored by Git by default
```

### Build and Run

Open the project in Android Studio, or build from the command line:

```bash
./gradlew assembleLiteDebug
```

On Windows:

```powershell
.\gradlew.bat assembleLiteDebug
```

The debug APK is usually generated at:

```text
app/build/outputs/apk/lite/debug/app-lite-debug.apk
```

To build the model-bundled flavor, place a LiteRT-LM model file at:

```text
app/src/bundled/assets/models/gemma-4-E2B-it.litertlm
```

Then run:

```bash
./gradlew assembleBundledDebug
```

On Windows:

```powershell
.\gradlew.bat assembleBundledDebug
```

Note: model files are usually larger than 2 GB. This repository ignores `*.litertlm`, `*.task`, and `*.apk` files through `.gitignore`. For a public GitHub repository, provide the model through Releases, an external download link, or clear setup instructions instead of committing it directly.

### Safety Scope

OffLifeline provides offline self-help guidance only. It does not replace professional rescue, medical diagnosis, first-aid training, or official disaster instructions. When a network or other communication channel is available, contact local emergency services, rescue teams, or medical professionals first.

### Docs

- [Kaggle / project write-up](doc/kaggle_writeup_offlifeline.md)
- [Technical design notes](doc/offline_survival_agent_technical_doc_revised.md)
- [RAG migration plan](doc/rag_migration_plan.md)

<p align="right"><a href="#offlifeline">Back to top</a></p>
