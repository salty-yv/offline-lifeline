# Offline Lifeline 本地 RAG 改造方案

> 基于 `doc/技术可借鉴点_Offline_Lifeline_本地RAG版.md` 整理，结合当前项目实际代码结构制定。

---

## 一、改造目标

把指南系统从 **"整篇 GuideEntity + LIKE 搜索"** 升级为 **"内置 RAG 数据 + FTS 全文检索 + Agent 引用本地 chunk"**。

核心路径：

```
Markdown 源文件（assets/guides_src/）
  ↓ 构建脚本生成（Python）
SQLite 预生成数据库（assets/databases/offline_guides.db）
  ↓ App 启动时 createFromAsset 加载
GuideChunkDao.searchFts()
  ↓ QueryExpander + RiskTopicRouter
GuideRetrievalService.retrieve()
  ↓ PromptBuilder 注入 [Local Guide Context]
SurvivalAgent → LLM → AgentResponse（含 citations）
```

---

## 二、不改动的部分

- `GuideEntity` + `GuideDao` + `GuideRepository`：**保留**，继续给"指南浏览页"的 LIKE 搜索用。
- `RiskClassifier`、`IntentClassifier`、`SafetyKernel`、`ToolRouter`：**不改动**。
- `DefaultGuideData.kt`：**暂时保留**，种子数据降级用，后续可逐步废弃。
- `AppDatabase` 版本号：**从 2 升到 3**（新增 chunk 表），用 Migration 平滑升级。

---

## 三、完整文件清单

### 3.1 新增内容源文件（Markdown）

```
app/src/main/assets/guides_src/
    medical/
        bleeding.md
        burn.md
        snake_bite.md
        hypothermia.md
        heatstroke.md
        poisoning.md
        fracture.md
        injury.md
        dehydration.md
    navigation/
        lost.md
    disaster/
        flood.md
        fire.md
        thunderstorm.md
        earthquake.md
    device/
        low_battery.md
        night.md
```

### 3.2 新增运行期数据库（构建期生成）

```
app/src/main/assets/databases/offline_guides.db
```

### 3.3 新增构建脚本

```
scripts/build_guides_db.py     ← 主脚本：读取 Markdown → 生成 SQLite
scripts/validate_guides.py     ← 校验脚本：检查 chunk 格式是否合规
```

### 3.4 新增数据层（Kotlin）

| 文件 | 位置 |
|------|------|
| `GuideChunkEntity.kt` | `data/db/` |
| `GuideChunkFtsEntity.kt` | `data/db/` |
| `GuideChunkDao.kt` | `data/db/` |

### 3.5 新增 RAG 服务层（Kotlin）

| 文件 | 位置 |
|------|------|
| `QueryExpander.kt` | `agent/rag/` |
| `KeywordExtractor.kt` | `agent/rag/` |
| `RiskTopicRouter.kt` | `agent/rag/` |
| `RankFusion.kt` | `agent/rag/` |
| `GuideRetrievalService.kt` | `agent/rag/` |

### 3.6 修改的已有文件

| 文件 | 改动摘要 |
|------|---------|
| `AppDatabase.kt` | 新增 `GuideChunkEntity`、`GuideChunkFtsEntity`，版本 2→3 |
| `AppContainer.kt` | 新增 `GuideRetrievalService` lazy 实例；`database` 改用 `createFromAsset` |
| `AgentResponse.kt` | 新增 `citations: List<GuideCitation>` 字段 |
| `SurvivalAgent.kt` | `prepareResponse` 中在调用 LLM 前先调 `GuideRetrievalService.retrieve()` |
| `PromptBuilder.kt` | `buildSystemInstruction` 中增加 `[Local Guide Context]` 段注入 |

### 3.7 新增测试

```
app/src/test/java/com/example/offlinelifeline/rag/
    GuideRetrievalServiceTest.kt
    QueryExpanderTest.kt
    RiskTopicRouterTest.kt
```

---

## 四、分阶段实施步骤

### 第一步：准备 Markdown 内容源文件

把 `DefaultGuideData.kt` 中的所有指南内容迁移成 Markdown 格式。

**格式规范（每个文件）：**

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

> [!NOTE]
> 一个 Markdown 文件对应一篇指南（一个 `guideId`）。  
> Chunk 切分规则：按二级标题（`##`）切分，每 chunk 150-500 字。

---

### 第二步：写 Python 构建脚本 `scripts/build_guides_db.py`

**职责：**

1. 遍历 `app/src/main/assets/guides_src/**/*.md`
2. 解析 YAML Front Matter（用 `python-frontmatter` 库）
3. 按 `##` 二级标题切 chunk
4. 写入三张表：`guides`、`guide_chunks`、`guide_chunks_fts`（FTS4）
5. 输出统计报告

**运行方式：**

```bash
cd scripts
pip install python-frontmatter
python build_guides_db.py
# 自动输出到 ../app/src/main/assets/databases/offline_guides.db
```

**输出报告示例：**

```
Guides: 16
Chunks: 72
Topics: medical, navigation, disaster, device
Warnings:
  - night.md 缺 reviewed_at
  - earthquake.md chunk 004 超过 900 字
```

---

### 第三步：新增数据层 Kotlin 文件

#### `GuideChunkEntity.kt`

```kotlin
@Entity(
    tableName = "guide_chunks",
    indices = [Index("guideId"), Index("topic"), Index("riskDomain")]
)
data class GuideChunkEntity(
    @PrimaryKey val chunkId: String,   // e.g. "bleeding_001"
    val guideId: String,               // e.g. "bleeding"
    val topic: String,                 // e.g. "medical"
    val riskDomain: String,            // e.g. "BLEEDING"
    val title: String,
    val headingPath: String,           // e.g. "出血 > 先做这 3 步"
    val body: String,
    val tags: String,
    val priority: Int,
    val chunkIndex: Int,
    val contentVersion: Int,
    val updatedAtMillis: Long
)
```

#### `GuideChunkFtsEntity.kt`

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

#### `GuideChunkDao.kt`

```kotlin
@Dao
interface GuideChunkDao {
    @Query("SELECT * FROM guide_chunks_fts WHERE guide_chunks_fts MATCH :query LIMIT :limit")
    suspend fun searchFts(query: String, limit: Int): List<GuideChunkFtsEntity>

    @Query("""
        SELECT * FROM guide_chunks_fts
        WHERE guide_chunks_fts MATCH :query AND topic = :topic
        LIMIT :limit
    """)
    suspend fun searchFtsByTopic(query: String, topic: String, limit: Int): List<GuideChunkFtsEntity>
}
```

---

### 第四步：新增 RAG 服务层

#### `agent/rag/RiskTopicRouter.kt`

```kotlin
object RiskTopicRouter {
    fun topicFor(domain: RiskDomain): String? = when (domain) {
        RiskDomain.BLEEDING, RiskDomain.BURN, RiskDomain.SNAKE_BITE,
        RiskDomain.HYPOTHERMIA, RiskDomain.HEATSTROKE, RiskDomain.INJURY,
        RiskDomain.FRACTURE, RiskDomain.POISONING, RiskDomain.DEHYDRATION -> "medical"
        RiskDomain.LOST -> "navigation"
        RiskDomain.LOW_BATTERY, RiskDomain.NIGHT -> "device"
        RiskDomain.FLOOD, RiskDomain.FIRE,
        RiskDomain.THUNDERSTORM, RiskDomain.EARTHQUAKE -> "disaster"
        else -> null
    }
}
```

#### `agent/rag/QueryExpander.kt`

```kotlin
object QueryExpander {
    private val expansions = mapOf(
        "出血" to listOf("流血", "止血", "压迫", "绷带", "伤口"),
        "bleeding" to listOf("blood", "wound", "pressure", "bandage"),
        "迷路" to listOf("方向", "求救", "信号", "保暖", "原地等待"),
        "lost" to listOf("navigation", "signal", "rescue", "shelter"),
        "蛇咬" to listOf("毒蛇", "咬伤", "固定", "肿胀"),
        "中暑" to listOf("高温", "暴晒", "降温", "补水"),
        "失温" to listOf("寒冷", "发抖", "湿透", "保暖"),
        "没电" to listOf("低电量", "节电", "省电"),
    )

    fun expandTerms(raw: String): List<String> {
        val normalized = raw.trim().lowercase()
        val terms = mutableSetOf<String>(normalized)
        expansions.forEach { (key, values) ->
            if (normalized.contains(key.lowercase())) {
                terms += key; terms += values
            }
        }
        return terms.toList()
    }

    fun toFtsOrQuery(terms: List<String>): String =
        terms.filter { it.isNotBlank() }
            .map { it.replace("\"", "") }
            .joinToString(" OR ") { "\"$it\"" }
}
```

#### `agent/rag/GuideRetrievalService.kt`

```kotlin
class GuideRetrievalService(private val dao: GuideChunkDao) {
    suspend fun retrieve(
        userInput: String,
        riskDomains: Set<RiskDomain>,
        limit: Int = 5
    ): List<GuideChunkFtsEntity> {
        val expandedTerms = QueryExpander.expandTerms(userInput)
        val ftsQuery = QueryExpander.toFtsOrQuery(expandedTerms)
        val topic = riskDomains.firstOrNull()?.let { RiskTopicRouter.topicFor(it) }

        val base = dao.searchFts(ftsQuery, limit = 8)
        val topicHits = topic?.let { dao.searchFtsByTopic(ftsQuery, it, limit = 8) }.orEmpty()

        return (base + topicHits)
            .distinctBy { it.chunkId }
            .sortedByDescending { if (it.topic == topic) 1 else 0 }
            .take(limit)
    }
}
```

---

### 第五步：修改现有文件

#### 修改 `AppDatabase.kt`

- 新增 `GuideChunkEntity::class`、`GuideChunkFtsEntity::class`
- version 改为 `3`
- 新增 `abstract fun guideChunkDao(): GuideChunkDao`

#### 修改 `AppContainer.kt`

```kotlin
// 1. database 改用 createFromAsset（首次安装用内置 DB）
val database: AppDatabase by lazy {
    Room.databaseBuilder(appContext, AppDatabase::class.java, DATABASE_NAME)
        .createFromAsset("databases/offline_guides.db")
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .build()
}

// 2. 新增 RAG 服务
val guideRetrievalService: GuideRetrievalService by lazy {
    GuideRetrievalService(database.guideChunkDao())
}

// 3. survivalAgent 注入 guideRetrievalService
val survivalAgent: SurvivalAgent by lazy {
    SurvivalAgent(
        ...,
        guideRetrievalService = guideRetrievalService
    )
}
```

> [!WARNING]
> `createFromAsset` 与 `addMigrations` 可以共存：首次安装从 asset 加载；已有旧版数据库则走 Migration。  
> MIGRATION_2_3 只需 `CREATE TABLE guide_chunks ...` 和 `CREATE VIRTUAL TABLE guide_chunks_fts ...`。

#### 修改 `AgentResponse.kt`

```kotlin
data class AgentResponse(
    val text: String,
    val riskDomains: Set<RiskDomain>,
    val followUpQuestions: List<String>,
    val toolRecommendations: List<ToolRecommendation>,
    val citations: List<GuideCitation> = emptyList()   // ← 新增
)

data class GuideCitation(
    val guideId: String,
    val chunkId: String,
    val title: String,
    val headingPath: String
)
```

#### 修改 `SurvivalAgent.kt`

在 `prepareResponse()` 中，`actionPlanner.buildActionStructure(...)` 之后加入 RAG 检索：

```kotlin
// RAG 检索：在调用 LLM 前先拿本地 chunk
val ragChunks = runBlocking {
    guideRetrievalService.retrieve(userInput, risks, limit = 5)
}
val citations = ragChunks.map {
    GuideCitation(it.guideId, it.chunkId, it.title, it.headingPath)
}
```

然后把 `ragChunks` 传给 `promptBuilder.buildSystemInstruction()`，`citations` 放进 `AgentResponse`。

#### 修改 `PromptBuilder.kt`

`buildSystemInstruction` 新增参数 `ragContext: String`，在 `[Known Context]` 段之后注入：

```kotlin
if (ragContext.isNotBlank()) {
    appendLine("[Local Guide Context]")
    appendLine(ragContext)
    appendLine("请优先依据 Local Guide Context 回答。如本地指南未覆盖，不要编造具体操作。")
    appendLine()
}
```

`ragContext` 由 `buildLocalGuideContext(chunks)` 生成：

```kotlin
fun buildLocalGuideContext(chunks: List<GuideChunkFtsEntity>): String =
    chunks.joinToString("\n\n") { "来源：${it.title} > ${it.headingPath}\n内容：${it.body}" }
```

---

### 第六步：UI 展示引用来源

在对话 UI 的 `AgentResponse` 渲染处，若 `citations` 不为空，在回答下方展示：

```
依据本地指南：
- 出血 > 先做这 3 步
- 出血 > 不要做
```

---

### 第七步：加单元测试

```kotlin
// GuideRetrievalServiceTest.kt
@Test
fun bleedingQuery_hitsBleedingGuide() = runTest {
    val result = retrieval.retrieve("我腿上一直流血止不住", setOf(RiskDomain.BLEEDING))
    assertThat(result.map { it.guideId }).contains("bleeding")
}

@Test
fun lostQuery_hitsLostGuide() = runTest {
    val result = retrieval.retrieve("我在山里找不到路 手机快没电了", setOf(RiskDomain.LOST))
    assertThat(result.map { it.guideId }).contains("lost")
}
```

---

## 五、RAG 数据格式示例（出血.md）

> 这是一个完整示例，展示 Front Matter + 内容格式 + Chunk 切分结果。

**源文件** `guides_src/medical/bleeding.md`：

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
  - 流血
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

**构建脚本生成的 chunk 结果（插入 `guide_chunks` 表）：**

| chunkId | guideId | headingPath | body |
|---------|---------|-------------|------|
| bleeding_001 | bleeding | 出血 > 先做这 3 步 | 1. 用干净布料直接压住... |
| bleeding_002 | bleeding | 出血 > 接下来几分钟 | 持续压迫，不要频繁... |
| bleeding_003 | bleeding | 出血 > 不要做 | 不要用泥土、草药... |

**FTS 查询示例：**

```sql
-- 用户输入"我一直流血止不住"，扩展后查询
SELECT * FROM guide_chunks_fts
WHERE guide_chunks_fts MATCH '"流血" OR "出血" OR "止血" OR "压迫" OR "伤口"'
LIMIT 5;
-- 命中 bleeding_001、bleeding_003
```

---

## 六、新增 Migration MIGRATION_2_3 示例

```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS guide_chunks (
                chunkId TEXT NOT NULL PRIMARY KEY,
                guideId TEXT NOT NULL,
                topic TEXT NOT NULL,
                riskDomain TEXT NOT NULL,
                title TEXT NOT NULL,
                headingPath TEXT NOT NULL,
                body TEXT NOT NULL,
                tags TEXT NOT NULL,
                priority INTEGER NOT NULL,
                chunkIndex INTEGER NOT NULL,
                contentVersion INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_chunk_guide ON guide_chunks(guideId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_chunk_topic ON guide_chunks(topic)")
        db.execSQL("""
            CREATE VIRTUAL TABLE IF NOT EXISTS guide_chunks_fts
            USING fts4(chunkId, guideId, topic, riskDomain, title, headingPath, body, tags)
        """.trimIndent())
    }
}
```

---

## 七、实施优先级

| 优先级 | 步骤 | 耗时估算 |
|--------|------|---------|
| ⭐⭐⭐ | 第一步：迁移 Markdown 内容源文件 | 2-3 小时 |
| ⭐⭐⭐ | 第二步：写 Python 构建脚本 | 1-2 小时 |
| ⭐⭐⭐ | 第三步：新增 Kotlin 数据层 | 1 小时 |
| ⭐⭐⭐ | 第四步：新增 RAG 服务层 | 1-2 小时 |
| ⭐⭐⭐ | 第五步：修改 AppDatabase / AppContainer / Agent | 1-2 小时 |
| ⭐⭐ | 第六步：UI 展示引用来源 | 30 分钟 |
| ⭐⭐ | 第七步：单元测试 | 1 小时 |
