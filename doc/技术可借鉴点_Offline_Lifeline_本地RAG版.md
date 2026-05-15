# 技术可借鉴点整理：Survive AI 项目 → 你的 Offline Lifeline Demo（本地内置 RAG 版）

> 本版已按你的新约束重写：
>
> - 不做远程查找、远程下载、远程更新数据库。
> - RAG 数据只使用软件内置的本地内容。
> - 不借鉴模型下载流程。
> - 不借鉴 Setup / Runtime State 引导流程。
> - 目标体验是：安装后打开就能用。

---

## 0. 新边界：你的项目应该走“内置知识库 + 本地检索”路线

你的 demo 现在已经有 Android 原生架构基础：

- Kotlin + Jetpack Compose
- Room 本地数据库
- DataStore 设置
- 本地 LLM 抽象层
- Mock / Fallback LLM
- Agent 分层：`RiskClassifier`、`IntentClassifier`、`QuestionPlanner`、`ActionPlanner`、`ToolRouter`、`SafetyKernel`
- CameraX 图片入口
- 工具箱能力：SOS、手电筒、电量建议、应急卡、Debug Log

所以不建议你借鉴对方项目里的远程同步、模型下载、启动设置页。你真正可以借鉴的是这几个纯技术能力：

1. 本地 RAG 知识库架构
2. 指南内容 chunk 化
3. SQLite / Room FTS 全文检索
4. 查询扩展 Query Expansion
5. 多路召回与融合排序
6. RiskDomain 到 topic 的检索路由
7. Agent 回答时注入本地指南片段
8. 本地内容更新的构建流程
9. 针对高风险场景的检索测试集

核心目标是：

> 把你的指南系统从“整篇 Guide + LIKE 搜索”升级成“内置 RAG 数据 + 本地全文检索 + Agent 引用本地片段”。

---

## 1. 最推荐的 RAG 数据格式：Markdown 源文件 + 构建期生成 SQLite / JSONL

你现在的问题很关键：

> 如果不远程下载数据库，RAG 数据到底以什么格式放进软件里？后续更新内容怎么办？

我建议采用“两层格式”：

```text
开发期源数据：Markdown 文件，方便人写和维护
运行期检索数据：预生成 SQLite 数据库，方便 App 打开即用
```

也就是：

```text
app/src/main/assets/guides_src/*.md
        ↓ 构建脚本生成
app/src/main/assets/databases/offline_guides.db
        ↓ App 安装后直接读取 / 复制
Room / SQLite / FTS 本地检索
```

### 为什么不建议继续把指南硬编码在 Kotlin 里

你现在的 `DefaultGuideData.kt` 是这样：

```kotlin
object DefaultGuideData {
    val guides: List<GuideEntity> = listOf(
        guide(
            id = "bleeding",
            title = "出血",
            summary = "直接压迫止血...",
            tags = "出血,伤口,压迫,急救",
            body = """
                先做这 3 步：
                ...
            """.trimIndent()
        )
    )
}
```

这个方式适合 demo 初期，但不适合后续维护 RAG 内容。原因是：

- 写长内容很难读。
- 非技术人员不方便改。
- 无法自然保留标题层级。
- 不方便做审校、版本、来源记录。
- 每次改内容都要改 Kotlin 文件。

建议把内容迁移成 Markdown 文件。

---

## 2. RAG 源内容格式建议：Markdown + YAML Front Matter

每篇指南一个 `.md` 文件，例如：

```text
app/src/main/assets/guides_src/medical/bleeding.md
app/src/main/assets/guides_src/medical/burn.md
app/src/main/assets/guides_src/navigation/lost.md
app/src/main/assets/guides_src/disaster/flood.md
```

单篇文件格式建议：

```markdown
---
id: bleeding
title: 出血
topic: medical
risk_domains:
  - BLEEDING
tags:
  - 出血
  - 止血
  - 伤口
  - 压迫
  - 急救
priority: 5
version: 1
reviewed_at: 2026-05-15
source_type: internal
---

# 出血

## 先做这 3 步

1. 用干净布料直接压住出血点。
2. 能做到时让伤处高于心脏。
3. 大量出血、喷射样出血或止不住时，尽快联系救援或医疗人员。

## 接下来几分钟

- 持续压迫，不要频繁掀开查看。
- 保持身体温暖，避免休克。
- 记录受伤时间和出血变化。

## 不要做

- 不要用泥土、草药或不洁材料填塞伤口。
- 不要自行切开伤口或放血。
```

### 这个格式的好处

- Markdown 适合写指南。
- YAML Front Matter 适合存元数据。
- 可以按文件夹分 topic。
- 可以被脚本自动切 chunk。
- 后续更新内容只需要改 `.md` 文件。
- App 运行时不需要联网。
- 每次发新版 APK 时内置新版数据库。

---

## 3. 运行期数据格式建议：预生成 SQLite 数据库

为了“打开就能用”，最好的运行期格式不是远程 DB，也不是首次启动后慢慢下载，而是：

```text
预生成 SQLite 数据库，直接打包进 APK assets
```

推荐路径：

```text
app/src/main/assets/databases/offline_guides.db
```

App 启动时通过 Room 直接使用预打包数据库：

```kotlin
Room.databaseBuilder(
    context,
    AppDatabase::class.java,
    "offline_lifeline.db"
)
.createFromAsset("databases/offline_guides.db")
.build()
```

### 内容更新方式

不需要远程更新。更新流程变成：

```text
修改 Markdown 指南
  ↓
运行构建脚本重新生成 offline_guides.db
  ↓
打包新版 APK
  ↓
用户更新 App 后获得新版本地内容
```

这符合你的目标：

> 软件提供什么内容，用户就用什么内容；不需要运行时联网查找或下载数据库。

---



---

## 5. Chunk 数据结构建议

你现在的 `GuideEntity` 是整篇指南。建议保留它，用来做指南页浏览；同时新增 `GuideChunkEntity`，给 RAG 检索用。

```kotlin
@Entity(
    tableName = "guide_chunks",
    indices = [
        Index("guideId"),
        Index("topic"),
        Index("riskDomain")
    ]
)
data class GuideChunkEntity(
    @PrimaryKey val chunkId: String,
    val guideId: String,
    val topic: String,
    val riskDomain: String,
    val title: String,
    val headingPath: String,
    val body: String,
    val tags: String,
    val priority: Int,
    val chunkIndex: Int,
    val contentVersion: Int,
    val updatedAtMillis: Long
)
```

其中：

- `chunkId`：唯一片段 ID，例如 `bleeding_001`。
- `guideId`：对应整篇指南，例如 `bleeding`。
- `topic`：大类，例如 `medical`、`navigation`、`disaster`。
- `riskDomain`：对应你已有的 `RiskDomain`，例如 `BLEEDING`。
- `title`：指南标题。
- `headingPath`：片段所在标题路径，例如 `出血 > 先做这 3 步`。
- `body`：真正注入 LLM 的短文本。
- `tags`：逗号分隔或 JSON 字符串。
- `priority`：高风险内容可提高排序权重。
- `contentVersion`：内容版本，方便测试和迁移。

---

## 6. FTS 检索表建议

你的 `GuideDao.search()` 现在是 `LIKE`：

```sql
WHERE title LIKE '%' || :query || '%'
   OR summary LIKE '%' || :query || '%'
   OR body LIKE '%' || :query || '%'
   OR tags LIKE '%' || :query || '%'
```

这可以保留给用户在指南页搜索，但不适合作为 Agent RAG 检索。

建议新增 FTS 表。Android + Room 场景下，优先考虑 `@Fts4`，兼容性通常比依赖 FTS5 更稳。

```kotlin
@Fts4
@Entity(tableName = "guide_chunks_fts")
data class GuideChunkFtsEntity(
    val chunkId: String,
    val guideId: String,
    val topic: String,
    val riskDomain: String,
    val title: String,
    val headingPath: String,
    val body: String,
    val tags: String
)
```

DAO 可以先做简单版本：

```kotlin
@Dao
interface GuideChunkDao {
    @Query(
        """
        SELECT * FROM guide_chunks_fts
        WHERE guide_chunks_fts MATCH :query
        LIMIT :limit
        """
    )
    suspend fun searchFts(query: String, limit: Int): List<GuideChunkFtsEntity>

    @Query(
        """
        SELECT * FROM guide_chunks_fts
        WHERE guide_chunks_fts MATCH :query
          AND topic = :topic
        LIMIT :limit
        """
    )
    suspend fun searchFtsByTopic(
        query: String,
        topic: String,
        limit: Int
    ): List<GuideChunkFtsEntity>
}
```

注意：

- FTS 查询不要直接使用用户原句。
- 需要先做 query normalize、停用词过滤、关键词提取。
- 中文分词能力有限时，可以在 `tags` 和 `riskDomain` 上做补偿。

---

## 7. Query Expansion 可以借鉴，但要改成本地固定词表

对方项目里 query expansion 的想法值得借鉴。你的项目不需要联网，不需要远程词库，直接内置一个本地词表即可。

```kotlin
object QueryExpander {
    private val expansions = mapOf(
        "出血" to listOf("流血", "止血", "压迫", "绷带", "休克", "伤口"),
        "bleeding" to listOf("blood", "wound", "pressure", "bandage", "tourniquet"),

        "迷路" to listOf("方向", "求救", "信号", "保暖", "电量", "原地等待"),
        "lost" to listOf("navigation", "signal", "rescue", "shelter", "battery"),

        "蛇咬" to listOf("毒蛇", "咬伤", "固定", "肿胀", "过敏", "呼吸困难"),
        "snake bite" to listOf("venom", "swelling", "immobilize", "allergy"),

        "中暑" to listOf("高温", "暴晒", "降温", "补水", "意识混乱"),
        "heatstroke" to listOf("heat", "cooling", "hydration", "confusion")
    )

    fun expandTerms(raw: String): List<String> {
        val normalized = raw.trim().lowercase()
        val terms = mutableSetOf<String>()
        terms += normalized
        expansions.forEach { (key, values) ->
            if (normalized.contains(key.lowercase())) {
                terms += key
                terms += values
            }
        }
        return terms.toList()
    }
}
```

不要把扩展词简单拼成一句很长的查询，因为 FTS 可能会要求所有词都匹配，导致召回变差。

推荐做法是构造 OR 查询：

```kotlin
fun toFtsOrQuery(terms: List<String>): String {
    return terms
        .map { it.replace("\"", "") }
        .filter { it.isNotBlank() }
        .joinToString(" OR ") { "\"$it\"" }
}
```

例如：

```text
用户输入：我一直流血止不住
关键词：流血, 出血, 止血, 压迫, 伤口
FTS 查询："流血" OR "出血" OR "止血" OR "压迫" OR "伤口"
```

---

## 8. 多路召回：比单次搜索更稳

对方项目里多路召回的思路值得借鉴，但你的版本可以简化。

建议做 3 路检索：

```text
1. 原始关键词检索
2. Query Expansion 扩展词检索
3. RiskDomain / topic 定向检索
```

然后把结果合并去重。

```kotlin
class GuideRetrievalService(
    private val dao: GuideChunkDao,
    private val queryExpander: QueryExpander,
    private val topicRouter: RiskTopicRouter
) {
    suspend fun retrieve(
        userInput: String,
        riskDomain: RiskDomain,
        limit: Int = 5
    ): List<GuideChunkFtsEntity> {
        val topic = topicRouter.topicFor(riskDomain)
        val originalTerms = KeywordExtractor.extract(userInput)
        val expandedTerms = queryExpander.expandTerms(userInput)

        val original = dao.searchFts(toFtsOrQuery(originalTerms), limit = 8)
        val expanded = dao.searchFts(toFtsOrQuery(expandedTerms), limit = 8)
        val topicHits = topic?.let {
            dao.searchFtsByTopic(toFtsOrQuery(expandedTerms), it, limit = 8)
        }.orEmpty()

        return RankFusion.merge(
            listOf(original, expanded, topicHits),
            limit = limit
        )
    }
}
```

这个比单次 `LIKE` 更适合救急场景，因为用户可能说得很口语化：

```text
我腿上一直冒血
我朋友不清醒了
我在山里找不到路
手机快没电了怎么办
```

---

## 9. RiskDomain → topic 路由可以直接结合你的 Agent 架构

你的项目已经有 `RiskClassifier` 和 `RiskDomain`，这是很大的优势。可以把它接进 RAG 检索。

示例：

```kotlin
object RiskTopicRouter {
    fun topicFor(domain: RiskDomain): String? = when (domain) {
        RiskDomain.BLEEDING -> "medical"
        RiskDomain.BURN -> "medical"
        RiskDomain.HYPOTHERMIA -> "medical"
        RiskDomain.HEATSTROKE -> "medical"
        RiskDomain.LOST -> "navigation"
        RiskDomain.LOW_BATTERY -> "device"
        RiskDomain.FLOOD -> "disaster"
        RiskDomain.THUNDERSTORM -> "disaster"
        RiskDomain.SNAKE_BITE -> "medical"
        else -> null
    }
}
```

Agent 流程可以改成：

```text
用户输入
  ↓
RiskClassifier
  ↓
RiskTopicRouter
  ↓
GuideRetrievalService
  ↓
PromptBuilder 注入本地指南 chunk
  ↓
LocalLlmEngine / FallbackLlmEngine
  ↓
SafetyKernel
  ↓
输出答案 + 本地依据
```

---

## 10. PromptBuilder 应该注入“本地指南依据”

你现在可以让 Agent 回答更可信：不是只让模型自由发挥，而是把本地 chunk 明确放进 prompt。

建议 Prompt 中加入类似结构：

```text
[Local Guide Context]
来源 1：出血 > 先做这 3 步
内容：用干净布料直接压住出血点。能做到时让伤处高于心脏。大量出血、喷射样出血或止不住时，尽快联系救援或医疗人员。

来源 2：出血 > 不要做
内容：不要用泥土、草药或不洁材料填塞伤口。不要自行切开伤口或放血。

[Instruction]
请优先依据 Local Guide Context 回答。
如果本地指南没有覆盖，不要编造具体医疗操作。
回答要短、分步骤、适合离线紧急场景。
```

Kotlin 结构可以是：

```kotlin
data class RetrievedGuideChunk(
    val chunkId: String,
    val guideId: String,
    val title: String,
    val headingPath: String,
    val body: String
)

fun buildLocalGuideContext(chunks: List<RetrievedGuideChunk>): String {
    return chunks.joinToString("\n\n") { chunk ->
        "来源：${chunk.title} > ${chunk.headingPath}\n内容：${chunk.body}"
    }
}
```

---

## 11. UI 上建议显示“依据本地指南”

这个也属于技术实现层面，不是 UI 美化。它能让用户知道答案来自软件内置内容，而不是模型瞎编。

`AgentResponse` 可以加：

```kotlin
data class AgentResponse(
    val text: String,
    val riskDomain: RiskDomain,
    val suggestedTools: List<ToolType>,
    val citations: List<GuideCitation> = emptyList()
)

data class GuideCitation(
    val guideId: String,
    val chunkId: String,
    val title: String,
    val headingPath: String
)
```

回答下面可以显示：

```text
依据本地指南：
- 出血 > 先做这 3 步
- 出血 > 不要做
```

这样做有三个好处：

- 用户更信任。
- 你自己更容易调试 RAG 命中是否正确。
- 后续内容审校可以直接追踪到具体 chunk。

---

## 12. 本地内容更新流程：通过 App 发版更新，不通过远程数据库

你不想要远程查找下载数据库，那更新流程应该是开发者侧完成：

```text
1. 编辑 app/src/main/assets/guides_src/*.md
2. 运行 scripts/build_guides_db.py
3. 生成 app/src/main/assets/databases/offline_guides.db
4. 运行单元测试，确认关键 query 能命中正确 chunk
5. 打包新版 APK
6. 用户更新 App 后获得新内容
```

可以在项目里放一个脚本：

```text
scripts/build_guides_db.py
```

脚本职责：

```text
读取 Markdown
解析 YAML Front Matter
按标题和段落切 chunk
生成 guide 表
生成 guide_chunks 表
生成 guide_chunks_fts 表
写入 offline_guides.db
输出内容统计报告
```

输出报告建议包含：

```text
Guides: 12
Chunks: 58
Topics: medical, navigation, disaster, device
Warnings:
- burn.md 缺 reviewed_at
- flood.md chunk 003 超过 900 字
```

这样你后续更新数据内容时，不需要碰 App 运行逻辑。

---

## 13. Chunk 切分规则建议

RAG 效果很大程度取决于 chunk 切得好不好。

建议规则：

```text
按 Markdown 二级标题 / 三级标题切分
每个 chunk 控制在 150-500 中文字左右
超长段落继续按空行拆
每个 chunk 保留 title、headingPath、tags、riskDomain
不要把“不应该做”和“应该做”混在太长的 chunk 里
```

示例：

```text
出血.md
  chunk 1: 出血 > 先做这 3 步
  chunk 2: 出血 > 接下来几分钟
  chunk 3: 出血 > 不要做
```

这样当用户问：

```text
我流血止不住怎么办
```

Agent 可以只拿到最相关的 `先做这 3 步` 和 `不要做`，而不是把整篇指南塞进去。

---

## 14. 本地 RAG 检索测试集必须做

对方项目值得借鉴的一点是重视 RAG 链路。但你的项目更应该加“检索质量测试”。

建议新增：

```text
app/src/test/java/com/example/offlinelifeline/rag/GuideRetrievalServiceTest.kt
```

测试目标不是生成答案，而是确认 query 能命中正确指南。

示例测试用例：

```kotlin
@Test
fun bleedingQuery_hitsBleedingGuide() = runTest {
    val result = retrieval.retrieve(
        userInput = "我腿上一直流血止不住",
        riskDomain = RiskDomain.BLEEDING
    )

    assertThat(result.map { it.guideId }).contains("bleeding")
}

@Test
fun lostQuery_hitsLostGuide() = runTest {
    val result = retrieval.retrieve(
        userInput = "我在山里找不到路 手机快没电了",
        riskDomain = RiskDomain.LOST
    )

    assertThat(result.map { it.guideId }).contains("lost")
}
```

建议覆盖这些场景：

```text
出血：我一直流血 / bleeding badly / 止不住血
迷路：找不到路 / 山里迷路 / lost in forest
低电量：手机快没电 / battery almost dead
中暑：头晕暴晒 / heatstroke / high temperature
失温：很冷发抖 / wet and freezing
蛇咬：被蛇咬 / snake bite
洪水：水涨起来 / flood / trapped by water
雷电：打雷在山上 / thunderstorm
烧伤：烫伤起泡 / burn blister
误食：吃了野果 / mushroom poisoning
```

只要这些检索稳定，你的 Agent 才有可靠基础。

---



## 16. “打开就能用”的启动策略

为了满足打开就能用，建议启动策略是：

```text
MainActivity
  ↓
AppContainer 初始化
  ↓
Room 使用 createFromAsset 加载内置 DB
  ↓
直接进入 Chat / Guide / Toolbox 主界面
```

不要做强制 setup 页。

如果本地模型初始化需要时间，也不要阻塞指南和工具箱：

```text
指南：立即可用
工具箱：立即可用
RAG 检索：立即可用
LLM：可用则增强回答，不可用则用模板 / Fallback 回答
```

也就是说，最核心的安全能力不应该依赖模型是否加载成功。

更稳的降级策略：

```text
有 LLM：RAG chunk + LLM 生成自然语言回答
无 LLM：RAG chunk + 模板生成步骤化回答
```

模板回答示例：

```text
我找到了本地指南「出血」中的相关内容：

1. 用干净布料直接压住出血点。
2. 能做到时让伤处高于心脏。
3. 大量出血、喷射样出血或止不住时，尽快联系救援或医疗人员。

不要做：
- 不要用泥土、草药或不洁材料填塞伤口。
- 不要自行切开伤口或放血。
```

这样即使模型没有准备好，App 也不是废的。

---

## 17. 你项目中建议新增 / 修改的文件

### 新增内容源文件

```text
app/src/main/assets/guides_src/medical/bleeding.md
app/src/main/assets/guides_src/medical/burn.md
app/src/main/assets/guides_src/navigation/lost.md
app/src/main/assets/guides_src/disaster/flood.md
```

### 新增运行期数据库

```text
app/src/main/assets/databases/offline_guides.db
```

### 新增脚本

```text
scripts/build_guides_db.py
scripts/validate_guides.py
```

### 新增数据层

```text
GuideChunkEntity.kt
GuideChunkFtsEntity.kt
GuideChunkDao.kt
```

### 新增 RAG 服务

```text
GuideRetrievalService.kt
QueryExpander.kt
KeywordExtractor.kt
RiskTopicRouter.kt
RankFusion.kt
```

### 修改 Agent 层

```text
SurvivalAgent.kt
PromptBuilder.kt
AgentResponse.kt
```

### 新增测试

```text
GuideRetrievalServiceTest.kt
QueryExpanderTest.kt
RiskTopicRouterTest.kt
```

---

## 18. 推荐实施顺序

### 第一步：先迁移内容格式

把 `DefaultGuideData.kt` 中的内容迁移到 Markdown：

```text
DefaultGuideData.kt → app/src/main/assets/guides_src/*.md
```

先不用做复杂 RAG，只要保证内容能从 Markdown 或生成后的 DB 进入 App。

### 第二步：做 chunk 表

新增 `GuideChunkEntity`，把每篇指南拆成：

```text
先做这 3 步
接下来几分钟
不要做
```

### 第三步：做 FTS 检索

保留原来的 `GuideDao.search()` 给指南页用；新增 `GuideChunkDao.searchFts()` 给 Agent 用。

### 第四步：做 Query Expansion

先内置 10-20 个高频风险词表，不用追求完整。

### 第五步：接入 Agent

让 `SurvivalAgent` 在调用 LLM 前先检索本地 chunk，并让 `PromptBuilder` 注入本地指南上下文。

### 第六步：加引用

让 `AgentResponse` 返回 `citations`，在回答下方显示来源。

### 第七步：加测试集

至少保证核心风险场景能命中正确指南。

---

## 19. 最终你应该借鉴到什么程度

不需要照搬对方项目。你只需要借鉴它的这条技术链路：

```text
本地指南内容
  ↓
chunk 化
  ↓
FTS 检索
  ↓
Query Expansion
  ↓
RiskDomain / topic 路由
  ↓
RAG 上下文注入
  ↓
Agent 输出引用来源
```

不要借鉴：

```text
远程下载模型
远程同步数据库
启动 setup 引导页
Wi-Fi 下载检查
运行时拉取 manifest
```

你的最佳路线是：

> 内容随 APK 内置，打开即用；更新内容靠发新版 App；Agent 回答基于本地 RAG chunk，并明确展示本地指南来源。

---

## 20. 一句话总结

技术上最值得你借鉴的是：**把本地指南做成可检索、可引用、可测试的 RAG 数据层**。

你的 RAG 数据格式建议是：

```text
Markdown 作为源内容，方便维护；构建期生成 SQLite / FTS 数据库，随 APK 内置；运行时不联网、不下载、不 setup，打开即可检索和回答。
```
