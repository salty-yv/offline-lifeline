# 离线自救助手 App：Codex 执行计划

版本：v0.1  
面向对象：Codex / 自动化编程代理  
目标：按阶段实现一个 Android 离线自救助手 App，确保每一步都可编译、可运行、可测试、可回退。

---

## 0. 项目背景

本项目是一个 Android 本地对话式生存 Agent。

目标是在断网、低电量、户外受困、自然灾害、受伤、无法搜索信息等场景下，让用户通过手机本地模型获得保守、可执行的自救建议。

核心能力：

- 安装后离线可用；
- 本地 LiteRT-LM 推理；
- Gemma-4-E2B-it-litert-lm 模型；
- 多轮对话式 Agent；
- 主动追问；
- 风险识别；
- 阶段性行动建议；
- Safety Kernel 安全约束；
- 本地应急工具箱；
- 离线指南；
- 后续支持图片 + 文字输入。

---

## 1. Codex 总执行原则

请严格遵守以下原则：

1. 不要一次性实现完整 App。
2. 每个阶段结束后都必须保证项目可编译。
3. 先实现 MockLlmEngine，跑通聊天 UI、Agent Runtime、Safety Kernel，再接 LiteRT-LM。
4. UI 层不能直接调用 LiteRT-LM，必须通过 LocalLlmEngine 接口。
5. Agent 负责上下文、风险判断、追问、行动计划、工具推荐。
6. Safety Kernel 负责禁止危险建议、注入必要提醒、输出前校验。
7. 工具箱必须不依赖模型。
8. 模型不可用时，离线指南和工具箱仍可用。
9. 图片能力放在文字 Agent 稳定之后。
10. 所有 Debug Log 默认只保存在本地，不上传。
11. 不要把大模型文件提交进 Git 仓库。
12. 每次修改只处理当前阶段需要的最小文件集合。
13. 每个阶段都要提供手动验收方法。

---

## 2. 技术栈

- Android
- Kotlin
- Jetpack Compose
- Material 3
- Kotlin Coroutines
- StateFlow
- Room / SQLite
- DataStore
- CameraX
- LiteRT-LM
- Gemma-4-E2B-it-litert-lm
- SHA-256 文件校验
- CameraManager
- 本地 Debug Log

---

## 3. 推荐工程目录

请优先使用以下目录结构：

```text
app/src/main/java/com/example/offlinesurvival/
  MainActivity.kt
  SurvivalApp.kt

  di/
    AppContainer.kt

  core/
    common/
      ResultState.kt
      AppDispatchers.kt
      TimeProvider.kt

    logging/
      DebugLogger.kt
      DebugLogRepository.kt

    model/
      ChatMessage.kt
      ChatRole.kt
      Attachment.kt
      RiskDomain.kt
      ToolType.kt
      AgentContext.kt
      AgentResponse.kt
      ModelRuntimeState.kt

  inference/
    LocalLlmEngine.kt
    InferenceRequest.kt
    InferenceChunk.kt
    MockLlmEngine.kt
    LiteRtLmEngine.kt
    ModelAssetManager.kt
    ModelManifest.kt
    ModelIntegrityChecker.kt

  agent/
    SurvivalAgent.kt
    ContextManager.kt
    RiskClassifier.kt
    QuestionPlanner.kt
    ActionPlanner.kt
    ToolRouter.kt
    PromptBuilder.kt

  safety/
    SafetyKernel.kt
    SafetyRule.kt
    SafetyConstraintBuilder.kt
    OutputSafetyValidator.kt

  data/
    db/
      AppDatabase.kt
      ChatMessageEntity.kt
      ChatDao.kt
      EmergencyCardEntity.kt
      EmergencyCardDao.kt
      GuideEntity.kt
      GuideDao.kt

    datastore/
      SettingsStore.kt

    repository/
      ChatRepository.kt
      EmergencyCardRepository.kt
      GuideRepository.kt

  device/
    flashlight/
      FlashlightController.kt

    battery/
      BatteryStatusProvider.kt
      BatteryAdviceGenerator.kt

    image/
      ImagePreprocessor.kt
      ExifStripper.kt

  ui/
    navigation/
      AppNavHost.kt
      Routes.kt

    chat/
      ChatScreen.kt
      ChatViewModel.kt
      ChatUiState.kt
      ChatInputBar.kt
      ChatMessageBubble.kt
      RuntimeStatusBanner.kt

    toolbox/
      ToolboxScreen.kt
      SosFlashScreen.kt
      ScreenSosScreen.kt
      BatteryAdviceScreen.kt

    guide/
      GuideListScreen.kt
      GuideDetailScreen.kt

    emergencycard/
      EmergencyCardScreen.kt

    settings/
      SettingsScreen.kt
```

---

## 4. 总体执行顺序

按以下顺序执行，不要跳阶段：

```text
阶段 1：Android 空工程 + Compose 导航
阶段 2：核心数据模型 + UI 状态模型
阶段 3：本地日志、设置、Room 数据层
阶段 4：Mock LLM 引擎 + 文字聊天 UI 闭环
阶段 5：Agent Runtime：上下文、风险分类、追问、行动计划
阶段 6：Safety Kernel：约束注入 + 输出校验
阶段 7：模型资源管理：Manifest、SHA-256、状态机、降级模式
阶段 8：LiteRT-LM 真引擎接入：初始化、会话、流式输出、中断、释放
阶段 9：离线指南 + SQLite FTS 检索
阶段 10：本地工具箱：SOS、电量建议、应急信息卡、Debug Log 导出
阶段 11：Agent 工具推荐
阶段 12：真实设备稳定性测试
阶段 13：图片输入：CameraX、相册、压缩、去 EXIF
阶段 14：图片 + 文字 Agent
阶段 15：发布打包策略与验收测试
```

---

# 阶段 1：Android 基础工程

## 目标

创建一个能启动、能切换页面的 Android Compose App。

## 执行内容

1. 创建 Kotlin Android 项目。
2. 配置 Jetpack Compose。
3. 配置 Material 3 主题。
4. 创建 `MainActivity`。
5. 创建 `SurvivalApp`。
6. 创建 `AppNavHost`。
7. 创建底部导航。
8. 创建五个占位页面：

```text
对话
工具箱
指南
信息卡
设置
```

## 验收标准

- App 可以启动。
- 五个页面可以正常切换。
- 不引入模型。
- 不引入数据库。
- 不写复杂业务。

---

# 阶段 2：核心数据模型

## 目标

定义全项目统一的数据结构。

## 执行内容

创建以下模型。

```kotlin
enum class ChatRole {
    USER,
    ASSISTANT,
    SYSTEM
}

data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val text: String,
    val createdAtMillis: Long,
    val attachments: List<Attachment> = emptyList(),
    val isFinal: Boolean = true
)

sealed class Attachment {
    data class Image(
        val localPath: String,
        val width: Int? = null,
        val height: Int? = null
    ) : Attachment()
}

enum class RiskDomain {
    LOST,
    INJURY,
    BLEEDING,
    BURN,
    FRACTURE,
    SNAKE_BITE,
    POISONING,
    HYPOTHERMIA,
    HEATSTROKE,
    DEHYDRATION,
    FLOOD,
    FIRE,
    THUNDERSTORM,
    EARTHQUAKE,
    LOW_BATTERY,
    NIGHT,
    UNKNOWN
}

enum class ToolType {
    SOS_FLASHLIGHT,
    SCREEN_SOS,
    BATTERY_SAVER_ADVICE,
    EMERGENCY_CARD,
    OFFLINE_GUIDE,
    DEBUG_LOG_EXPORT
}

data class ToolRecommendation(
    val toolType: ToolType,
    val reason: String,
    val priority: Int
)

data class AgentContext(
    val riskDomains: Set<RiskDomain>,
    val knownFacts: Map<String, String>,
    val missingFacts: List<String>,
    val batteryPercent: Int?,
    val userCanMove: Boolean?,
    val hasWater: Boolean?,
    val hasWarmClothes: Boolean?
)

data class AgentResponse(
    val text: String,
    val riskDomains: Set<RiskDomain>,
    val followUpQuestions: List<String>,
    val toolRecommendations: List<ToolRecommendation>
)
```

## 验收标准

- 数据模型单独编译通过。
- 不依赖 UI。
- 不依赖 LiteRT-LM。
- 不依赖 Android Context。

---

# 阶段 3：本地日志、设置、Room 数据层

## 目标

先做好持久化和本地日志，方便后续调试模型生命周期。

## 执行内容

实现：

```text
DebugLogger
DebugLogRepository
SettingsStore
Room AppDatabase
ChatDao
EmergencyCardDao
GuideDao
ChatRepository
EmergencyCardRepository
GuideRepository
```

## 需要保存的数据

```text
对话历史
用户应急信息卡
设置项
模型状态
本地 Debug Log
离线指南条目
```

## 注意事项

- Debug Log 默认只保存在本地。
- 不自动上传任何日志。
- 用户后续可以手动导出日志。
- 对话历史应支持清空。
- 应急信息卡应支持本地保存和读取。

## 验收标准

- 能写入一条对话消息。
- 能读取历史消息。
- 能写入一条 Debug Log。
- 能清空对话历史。
- 能保存设置项。

---

# 阶段 4：Mock LLM 引擎 + 文字聊天 UI 闭环

## 目标

在没有真实模型的情况下，跑通完整聊天体验。

## 执行内容

先定义统一接口：

```kotlin
interface LocalLlmEngine {
    val runtimeState: StateFlow<ModelRuntimeState>

    suspend fun initialize(): Result<Unit>

    fun sendMessage(request: InferenceRequest): Flow<InferenceChunk>

    suspend fun stopGeneration()

    suspend fun resetConversation()

    suspend fun release()
}
```

定义请求和输出：

```kotlin
data class InferenceRequest(
    val text: String,
    val imagePaths: List<String> = emptyList(),
    val systemInstruction: String,
    val safetyInstruction: String
)

data class InferenceChunk(
    val text: String,
    val isFinal: Boolean = false
)
```

实现：

```kotlin
class MockLlmEngine : LocalLlmEngine
```

Mock 行为：

- `initialize()` 延迟约 500ms 后返回成功。
- `sendMessage()` 每 100ms 输出一小段文字。
- 支持 `stopGeneration()`。
- 支持 `resetConversation()`。
- 支持 `release()`。
- 能模拟模型加载失败。
- 能模拟生成中断。
- 能模拟生成失败。

## Chat UI 行为

Chat 页面需要支持：

1. 输入文字。
2. 点击发送。
3. 用户消息立即显示。
4. Assistant 消息流式出现。
5. 点击停止生成。
6. 生成失败时显示错误。
7. 生成结束后写入历史。
8. 未完成回答不写入最终历史。

## 验收标准

- 不接 LiteRT-LM，也能完整聊天。
- 流式输出正常。
- 停止生成后 UI 不再显示“生成中”。
- 生成失败有错误提示。
- 未完成回答不写入最终历史。

---

# 阶段 5：Agent Runtime

## 目标

让 App 从普通聊天壳变成离线自救 Agent。

Agent Runtime 包含：

```text
ContextManager
RiskClassifier
QuestionPlanner
ActionPlanner
ToolRouter
PromptBuilder
SurvivalAgent
```

---

## 5.1 ContextManager

实现：

```kotlin
class ContextManager {
    fun buildContext(messages: List<ChatMessage>): AgentContext
    fun summarizeIfNeeded(messages: List<ChatMessage>): List<ChatMessage>
}
```

职责：

- 提取已知事实。
- 控制上下文长度。
- 识别用户是否提到：
  - 电量；
  - 是否受伤；
  - 是否能移动；
  - 是否有水；
  - 是否天黑；
  - 是否低温；
  - 是否被困；
  - 是否有同伴。

第一版可以使用规则和关键词，不要做复杂 NLP。

---

## 5.2 RiskClassifier

实现：

```kotlin
class RiskClassifier {
    fun classify(input: String, context: AgentContext): Set<RiskDomain>
}
```

关键词示例：

```text
迷路、走丢、找不到路 -> LOST
脚扭了、摔伤、疼、伤口 -> INJURY
流血、止不住血 -> BLEEDING
烫伤、烧伤、起泡 -> BURN
骨折、变形、不能动 -> FRACTURE
蛇、咬、毒蛇 -> SNAKE_BITE
蘑菇、野果、吃了、误食 -> POISONING
冷、发抖、湿透 -> HYPOTHERMIA
热、头晕、暴晒、中暑 -> HEATSTROKE
渴、没水、脱水 -> DEHYDRATION
洪水、涨水、河水 -> FLOOD
雷、电闪、打雷 -> THUNDERSTORM
火、烟、烧 -> FIRE
地震、余震、楼塌 -> EARTHQUAKE
没电、低电量、12% -> LOW_BATTERY
天黑、夜里、晚上 -> NIGHT
```

---

## 5.3 QuestionPlanner

实现：

```kotlin
class QuestionPlanner {
    fun planQuestions(context: AgentContext): List<String>
}
```

原则：

- 一次最多问 3 个问题。
- 高风险先问生命安全。
- 信息不足时也必须先给立即行动。

示例：

用户说：

```text
我在山里迷路了，脚扭了，天快黑了
```

Agent 应追问：

```text
1. 手机还剩多少电？
2. 你现在还能缓慢移动吗，还是不能负重？
3. 你身边有没有水、保暖衣物或可以避风的地方？
```

---

## 5.4 ActionPlanner

实现：

```kotlin
class ActionPlanner {
    fun buildActionStructure(
        riskDomains: Set<RiskDomain>,
        context: AgentContext
    ): String
}
```

输出结构固定：

```text
先做这 3 步
当前风险
接下来几分钟
不要做
我还需要确认
什么时候必须求救
```

---

## 5.5 ToolRouter

实现：

```kotlin
class ToolRouter {
    fun recommendTools(
        riskDomains: Set<RiskDomain>,
        context: AgentContext
    ): List<ToolRecommendation>
}
```

规则：

```text
LOW_BATTERY -> BATTERY_SAVER_ADVICE
LOST + NIGHT -> SCREEN_SOS / SOS_FLASHLIGHT
INJURY -> EMERGENCY_CARD / OFFLINE_GUIDE
FLOOD / FIRE / EARTHQUAKE -> OFFLINE_GUIDE
DEBUG 模式 -> DEBUG_LOG_EXPORT
```

---

## 5.6 SurvivalAgent

实现：

```kotlin
class SurvivalAgent(
    private val contextManager: ContextManager,
    private val riskClassifier: RiskClassifier,
    private val questionPlanner: QuestionPlanner,
    private val actionPlanner: ActionPlanner,
    private val toolRouter: ToolRouter,
    private val promptBuilder: PromptBuilder,
    private val safetyKernel: SafetyKernel,
    private val llmEngine: LocalLlmEngine
)
```

执行流程：

```text
用户输入
↓
ContextManager 构建上下文
↓
RiskClassifier 识别风险
↓
QuestionPlanner 生成追问
↓
ActionPlanner 生成回答结构
↓
ToolRouter 推荐工具
↓
SafetyKernel 生成安全约束
↓
PromptBuilder 构建最终 Prompt
↓
LocalLlmEngine 生成回答
↓
SafetyKernel 输出校验
↓
返回 UI
```

## 阶段验收标准

输入：

```text
我迷路了，手机只有 12% 电，天快黑了
```

系统应识别：

```text
LOST
LOW_BATTERY
NIGHT
```

并推荐：

```text
BATTERY_SAVER_ADVICE
SCREEN_SOS
OFFLINE_GUIDE
```

---

# 阶段 6：Safety Kernel

## 目标

实现模型主导、安全兜底。

Safety Kernel 做三件事：

```text
1. 生成安全约束
2. 拦截危险建议
3. 必要时要求重写
```

## 执行内容

创建：

```kotlin
class SafetyKernel
class SafetyConstraintBuilder
class OutputSafetyValidator

data class SafetyRule(
    val riskDomain: RiskDomain,
    val bannedPatterns: List<String>,
    val requiredReminders: List<String>
)
```

## 禁止动作规则

第一版硬编码即可：

```text
未知蘑菇 / 野果 / 植物 -> 禁止说可以食用
蛇咬 -> 禁止吸毒、切开伤口、放血
严重出血 -> 禁止让用户继续冒险移动
失温 -> 禁止喝酒保暖
洪水 -> 禁止涉水强行穿越
雷暴 -> 禁止躲在孤立大树下
胸痛 / 呼吸困难 / 昏迷 -> 禁止弱化求救必要性
图片医疗场景 -> 禁止确定性诊断
```

## 必须提醒规则

```text
严重出血 -> 压迫止血，尽快求救
蛇咬 -> 减少活动，固定患肢，尽快求救
失温 -> 保持干燥、避风、保温
中暑 -> 降温、补水、避免继续暴晒
误食 -> 不要继续食用，保留样本或照片，尽快求助
低电量 -> 降亮度、关高耗电功能、减少连续对话
迷路 -> 停止盲目移动，保存体力，制造求救信号
```

## Prompt 安全约束注入

实现：

```kotlin
fun buildSafetyInstruction(riskDomains: Set<RiskDomain>): String
```

示例输出：

```text
你必须给出保守、可执行的建议。
如果涉及未知野外食物，不得判断其安全可食用。
如果涉及蛇咬，不得建议吸吮、切开、放血。
如果信息不足，先给立即行动，再问最多 3 个关键问题。
```

## 输出校验

实现：

```kotlin
fun validate(output: String, riskDomains: Set<RiskDomain>): SafetyValidationResult
```

结果：

```kotlin
sealed class SafetyValidationResult {
    data object Pass : SafetyValidationResult()
    data class Blocked(val reason: String) : SafetyValidationResult()
    data class NeedsRewrite(
        val reason: String,
        val rewriteInstruction: String
    ) : SafetyValidationResult()
}
```

## 验收标准

如果模型输出：

```text
你可以试着吸出蛇毒。
```

必须被拦截。

如果模型输出：

```text
这个蘑菇看起来可以吃。
```

必须被拦截。

---

# 阶段 7：模型资源管理

## 目标

在接 LiteRT-LM 前，先实现模型文件状态机和降级模式。

## 执行内容

创建：

```kotlin
data class ModelManifest(
    val modelName: String,
    val modelVersion: String,
    val fileName: String,
    val expectedSha256: String,
    val expectedSizeBytes: Long,
    val format: String = "litertlm"
)

class ModelAssetManager
class ModelIntegrityChecker
```

## 模型来源

第一版支持三种路径：

```text
1. 开发模式：从 externalFilesDir/models/ 读取
2. 内置模式：从 assets 或 app-specific storage 读取
3. 发布模式：从 Play Asset Delivery 或安装时资源目录读取
```

## 状态机

```kotlin
sealed class ModelRuntimeState {
    data object NotChecked : ModelRuntimeState()
    data object Checking : ModelRuntimeState()
    data object Missing : ModelRuntimeState()
    data object ChecksumFailed : ModelRuntimeState()
    data object ReadyToLoad : ModelRuntimeState()
    data object Loading : ModelRuntimeState()
    data object Ready : ModelRuntimeState()
    data class Failed(val message: String) : ModelRuntimeState()
    data object Releasing : ModelRuntimeState()
    data object Released : ModelRuntimeState()
}
```

## 验收标准

- 模型不存在时，Chat 页面显示“模型不可用，可使用离线指南和工具箱”。
- SHA-256 不匹配时，阻止进入模型对话。
- 模型状态写入 Debug Log。
- 没有真实模型文件时，App 仍可通过 Mock 模式运行。

---

# 阶段 8：LiteRT-LM 真引擎接入

## 目标

用 `LiteRtLmEngine` 替换 `MockLlmEngine`，但 UI 和 Agent 不需要改。

## 执行内容

创建：

```kotlin
class LiteRtLmEngine(
    private val modelAssetManager: ModelAssetManager,
    private val debugLogger: DebugLogger,
    private val dispatchers: AppDispatchers
) : LocalLlmEngine
```

## 初始化流程

```text
1. 检查模型文件是否存在
2. 校验 SHA-256
3. 获取模型绝对路径
4. 创建 EngineConfig
5. 初始化 Engine
6. 创建 Conversation
7. 更新状态为 Ready
8. 写入 Debug Log
```

## 后端选择策略

```text
默认 CPU：稳定优先
设置中允许切换 GPU
NPU 暂时作为实验选项
```

## 生命周期原则

```text
Engine 生命周期 > Conversation 生命周期 > 单次生成任务生命周期
```

不要每次发送消息都重新初始化 Engine。

## 流式输出

优先使用 Kotlin Flow 风格的流式输出：

```kotlin
conversation.sendMessageAsync(prompt)
    .catch { throwable ->
        // log and update state
    }
    .collect { message ->
        // emit InferenceChunk
    }
```

## 推理中断

实现：

```kotlin
private var generationJob: Job? = null

override suspend fun stopGeneration() {
    generationJob?.cancel()
    generationJob = null
}
```

要求：

```text
取消当前生成
UI 状态恢复 Idle
不写入未完成回答
不销毁 Engine
允许重新发送
```

## 资源释放

必须释放：

```text
Conversation
Engine
图片临时文件
生成 Job
回调引用
```

## 验收标准

- 真模型能初始化。
- 能完成一轮文字对话。
- 能流式输出。
- 点击停止生成有效。
- 清空会话后重建 Conversation。
- 退出 Agent 页面后释放资源。
- 模型异常时进入降级模式。

---

# 阶段 9：离线指南与 SQLite FTS

## 目标

即使模型不可用，App 仍有价值。

## 执行内容

创建离线指南数据：

```text
迷路
出血
扭伤与骨折
烧伤
失温
中暑
脱水
蛇虫咬伤
误食
雷电
洪水
火灾
地震
求救信号
电量保护
```

实现：

```kotlin
class GuideRepository
class GuideSearchRepository
```

第一版可以先不用复杂 FTS，先做标题和标签匹配。第二版再接 SQLite FTS。

## Guide 页面

```text
指南列表
搜索框
指南详情
风险提醒
相关工具入口
```

## 验收标准

- 飞行模式下可浏览指南。
- 模型缺失时仍可打开指南。
- Agent 可以推荐某个指南入口。

---

# 阶段 10：本地工具箱

## 目标

实现不依赖模型的离线工具。

---

## 10.1 SOS 闪光灯

使用 `CameraManager` 控制闪光灯。

功能：

```text
打开 / 关闭
SOS 闪烁模式
低电量提醒
退出页面自动关闭
```

---

## 10.2 屏幕高亮 SOS

功能：

```text
全屏显示 SOS
高对比文字
可切换闪烁
保持屏幕常亮
退出恢复亮度设置
```

---

## 10.3 电量保护建议

读取电量状态，生成本地建议：

```text
降低屏幕亮度
关闭蓝牙 / Wi-Fi / 定位
开启省电模式
减少连续模型对话
准备一条固定求救信息
优先使用离线指南
```

---

## 10.4 本地个人应急信息卡

字段：

```text
姓名
血型
过敏史
慢性病
常用药
紧急联系人
备注
```

注意：

```text
默认本地保存
支持隐藏敏感字段
支持一键展示给救援人员
```

---

## 10.5 Debug Log 导出

功能：

```text
导出为 .txt 或 .json
不自动上传
用户手动分享
```

## 阶段验收标准

- 工具箱不依赖模型。
- 模型初始化失败时工具箱仍可用。
- 低电量场景下工具可直接打开。

---

# 阶段 11：Agent 工具推荐

## 目标

让 Agent 不只是回答，还能引导用户使用工具。

## 执行流程

```text
用户输入
↓
RiskClassifier
↓
ContextManager
↓
ToolRouter
↓
PromptBuilder 把工具推荐写入模型上下文
↓
模型生成回答
↓
UI 显示工具卡片
```

## UI 表现

Assistant 消息下方显示：

```text
推荐工具：
[电量保护建议]
[屏幕 SOS]
[离线指南：迷路]
```

点击工具卡片跳转对应页面。

## 验收标准

输入：

```text
我迷路了，手机只有 12% 电。
```

Agent 应回答：

```text
先停止不必要操作，降低屏幕亮度，减少连续对话。
```

并显示工具卡片：

```text
电量保护建议
屏幕 SOS
离线指南
```

---

# 阶段 12：PromptBuilder

## 目标

把 Agent、Safety、上下文、工具推荐稳定组合成模型输入。

## Prompt 结构

```text
[System Instruction]
你是一个完全离线运行的自救助手。
你不是医生、不是救援调度系统、不能替代专业救援。
你的回答必须保守、短句、步骤化、可执行。
信息不足时，先给立即行动，再问最多 3 个关键问题。

[Safety Constraints]
根据风险域注入禁止动作和必要提醒。

[Known Context]
用户已知情况：
- 场景：
- 电量：
- 身体状态：
- 装备：
- 天色：
- 已经建议过：

[Available Local Tools]
可推荐但不能自动执行：
- SOS 闪光灯
- 屏幕高亮 SOS
- 电量保护建议
- 本地个人应急信息卡
- 离线指南

[User Message]
用户最新输入。

[Required Output Format]
请按以下结构回答：
1. 先做这 3 步
2. 当前风险
3. 接下来几分钟
4. 不要做
5. 我还需要确认
6. 可使用的本地工具
```

## 验收标准

- 每次生成前都能构造完整 Prompt。
- 高风险场景自动插入安全约束。
- 低电量场景自动插入“减少连续对话”的建议。
- 工具只推荐，不自动执行。

---

# 阶段 13：真实设备稳定性测试

## 目标

验证 LiteRT-LM 生命周期是否可靠。

## 测试清单

```text
首次启动模型检查
模型 SHA-256 校验
Runtime 初始化耗时
首 token 延迟
连续 10 轮对话
点击停止生成
生成中切后台
生成中旋转屏幕
清空会话
退出 Agent 页面
重新进入 Agent 页面
低电量状态
飞行模式首次打开
模型缺失降级
模型损坏降级
```

## 必须记录 Debug Log

每次记录：

```text
设备型号
Android 版本
可用内存
电量
模型路径
模型校验结果
初始化耗时
首 token 延迟
生成总耗时
是否中断
错误堆栈
释放结果
```

## 验收标准

- App 不崩溃。
- 资源释放后再次进入可重新初始化。
- 中断生成后 UI 状态一致。
- 模型不可用时工具箱和指南仍可用。

---

# 阶段 14：图片输入基础能力

## 目标

先完成图片采集和预处理，不急着让模型理解图片。

## 执行内容

实现：

```text
CameraX 拍照
相册选择
图片压缩
尺寸限制
去除 EXIF
临时文件管理
图片预览
删除图片
```

## 图片处理要求

```text
最大边长限制，例如 1024 或 1280
压缩为 JPEG / WebP
去除 EXIF 位置信息
推理结束后释放临时文件
中断生成后释放临时文件
```

## 验收标准

- 用户能添加图片。
- 图片能预览。
- 图片能删除。
- EXIF 被移除。
- 不接模型也能跑通 UI。

---

# 阶段 15：图片 + 文字 Agent

## 目标

接入多模态输入。

注意：图片能力必须以实际模型包能力为准。不要假设所有 LiteRT-LM 模型都支持图片。如果当前模型不支持图片，应优雅降级为文字描述模式。

## 执行内容

扩展 `InferenceRequest`：

```kotlin
data class InferenceRequest(
    val text: String,
    val imagePaths: List<String> = emptyList(),
    val systemInstruction: String,
    val safetyInstruction: String
)
```

扩展 `LiteRtLmEngine.sendMessage()`：

```text
无图片：走纯文本。
有图片：构建包含图片和文字的消息。
如果当前模型不支持图片：降级提示“当前模型不支持图片理解，可改用文字描述”。
```

扩展 Safety Kernel：

```text
伤口图片：不做确定性诊断。
蘑菇 / 植物图片：不判断可食用。
河流 / 洪水图片：提醒涉水风险。
天气 / 雷暴图片：提醒避险。
```

## 图片回答格式

```text
我能看到的内容
可能的风险
先做这 3 步
不要做
我还需要确认
```

## 验收标准

- 伤口图片：输出保守急救建议，追问风险信息。
- 蘑菇图片：不判断可食用。
- 河流图片：提醒水流、低温、污染和涉水风险。
- 图片处理失败时，降级为文字对话。

---

# 阶段 16：发布打包策略

## 目标

解决“安装后即可离线使用”与“大模型体积”的冲突。

## 开发阶段

```text
不要把 2GB+ 模型放进 Git 仓库。
使用 externalFilesDir/models/ 或 adb push 模型文件。
ModelAssetManager 支持本地路径覆盖。
保留 Mock 模式开关。
```

## 内部测试阶段

```text
可以使用旁加载 APK / AAB 测试完整模型包。
保留 Mock 模式开关。
记录安装包体积、安装耗时、首次启动耗时。
```

## Google Play 阶段

```text
优先研究 Play Asset Delivery 的 install-time asset pack。
如果单个模型文件超过单个资源包限制，需要拆分资源包并在首次启动时合并到 app-specific storage。
如果无法满足安装即离线，则提供非 Play 完整包或轻量商店版。
```

---

# 17. MVP 最小可交付顺序

真正开发时，优先追这个 MVP 顺序：

```text
MVP 1：
Compose App + Chat UI + MockLlmEngine

MVP 2：
Agent Runtime + Safety Kernel

MVP 3：
模型文件检查 + LiteRT-LM 初始化 + 文字生成

MVP 4：
流式输出 + 停止生成 + 资源释放

MVP 5：
离线指南 + 工具箱

MVP 6：
Agent 推荐工具

MVP 7：
图片输入 + 图片 Agent
```

---

# 18. 第一个端到端验收用例

## 测试环境

```text
飞行模式
无网络
首次打开 App
模型已准备或使用 Mock 模式
```

## 用户输入

```text
我在山里迷路了，脚扭伤，天快黑了。
```

## 期望结果

Agent 必须：

1. 先给立即行动。
2. 主动追问电量、能否移动、水和保暖物。
3. 不建议继续盲目移动。
4. 建议保存电量。
5. 推荐离线指南或 SOS 工具。
6. 回答短句化、步骤化、保守。
7. 不输出危险建议。
8. 在模型不可用时，仍能打开离线指南和工具箱。

---

# 19. Codex 单次任务模板

每次让 Codex 执行一个阶段时，使用这个模板：

```text
请只实现“阶段 X：<阶段名称>”。

要求：
1. 只修改本阶段必要文件。
2. 不要提前实现后续阶段。
3. 保证项目可编译。
4. 给出新增 / 修改文件清单。
5. 给出关键实现说明。
6. 给出手动验收步骤。
7. 如果需要新增依赖，请说明原因。
8. 不要提交大模型文件。
```

---

# 20. 当前建议从这里开始

建议第一条 Codex 指令：

```text
请实现阶段 1：Android 空工程 + Compose 导航。

创建一个 Kotlin + Jetpack Compose Android App，包含 MainActivity、SurvivalApp、AppNavHost 和底部导航。底部导航包含五个页面：对话、工具箱、指南、信息卡、设置。所有页面先用占位内容。不要引入模型、数据库或 Agent 逻辑。完成后给出修改文件清单和手动验收步骤。
```

---

# 21. 重要禁止事项

Codex 不应做以下事情：

```text
不要一次性写完所有模块。
不要让 UI 直接调用 LiteRT-LM。
不要把模型文件放进 Git。
不要默认联网下载模型。
不要让工具箱依赖模型才能打开。
不要在 Safety Kernel 未完成前接入高风险自由回答。
不要在文字 Agent 稳定前做图片 Agent。
不要自动上传 Debug Log。
不要让模型判断未知蘑菇、野果、植物可食用。
不要建议吸蛇毒、切开伤口、放血、喝酒保暖、洪水涉水、雷暴躲孤立树下。
不要把未完成的流式回答写入最终历史。
```

---

# 22. 参考资料

- Google AI Edge LiteRT-LM Android 文档  
  https://ai.google.dev/edge/litert-lm/android

- Google AI Edge LiteRT-LM 概览  
  https://ai.google.dev/edge/litert-lm/overview

- Google Play 应用大小和资源包限制  
  https://support.google.com/googleplay/android-developer/answer/9859372

- Android Play Asset Delivery  
  https://developer.android.com/guide/playcore/asset-delivery
