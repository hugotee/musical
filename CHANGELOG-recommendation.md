# 推荐算法模块开发日志

## 概述

将 CookieMusic 首页推荐从人工精选模式升级为完整的混合推荐系统。实现了四种推荐算法的组合：基于内容的推荐、User-Based 协同过滤、Item-Based 协同过滤、热度排序，并通过加权融合得到最终的混合推荐结果。

## 算法总览

```
混合推荐 = Content × 0.25 + UserCF × 0.25 + ItemCF × 0.25 + (Popularity + Recency) × 0.25
冷启动 = Popularity × 0.7 + Recency × 0.3（点赞不足3首时回退）
```

## 变更清单

### 1. fix: 修复 good_count 字段更新 bug (d05156a)

**问题**：点赞/取消点赞时只操作 `music_info_action` 表，`music_info.good_count` 从未更新，一直是 0。

**修改文件**：
- `MusicInfoActionServiceImpl.java` - doGood() 方法中，点赞时调用 updateGoodCount(+1)，取消时 updateGoodCount(-1)
- `MusicInfoMapper.java` - 新增 updateGoodCount(delta) 方法
- `MusicInfoMapper.xml` - 新增 SQL：`UPDATE music_info SET good_count = good_count + #{delta}`

---

### 2. feat: 创建 RecommendationService 推荐服务 (acb22b3 → b301e58)

**新增文件**：
- `RecommendationService.java` - 推荐服务接口（支持 type 参数切换算法）
- `RecommendationServiceImpl.java` - 推荐服务实现（五种算法）

**文件结构**（~350行）：
```
RecommendationServiceImpl
├── 公开接口
│   ├── recommend(userId, limit)        → 默认混合推荐
│   └── recommend(userId, limit, type)  → 指定算法类型
├── 数据加载
│   ├── loadCandidates()                → 加载候选歌曲（已完成 + 排除自己的）
│   ├── loadCreationMap()               → 加载所有创作记录索引
│   └── loadActionMaps()                → 构建 user→likes 和 music→likers 双映射
├── 基于内容的推荐 (Content-Based)
│   ├── scoreByContent()                → 内容相似度打分
│   ├── buildContentProfile()           → 构建用户偏好画像
│   ├── computeContentSimilarity()      → 标签匹配相似度
│   └── parseSettings()                 → 解析 settings JSON 提取特征
├── User-Based 协同过滤
│   ├── scoreByUserCF()                 → 用户协同过滤打分
│   └── cosineSimilarity()              → 余弦相似度
├── Item-Based 协同过滤
│   └── scoreByItemCF()                 → 物品协同过滤打分
├── 热度排序 (Popularity)
│   └── scoreByPopularity()             → 播放量+点赞数归一化
├── 混合推荐 (Hybrid)
│   └── scoreHybrid()                   → 四种算法加权融合
└── 工具方法
    ├── popularityScore()               → 热度分计算
    ├── recencyScore()                  → 时间衰减 e^(-days/30)
    └── buildResult()                   → 排序+查询完整数据
```

---

### 3. feat: 新增 /music/loadRecommendMusic 接口 (9303c3c → b301e58)

**修改文件**：`MusicController.java`

- 支持可选 `type` 参数：`?type=content | userCf | itemCf | popularity | hybrid`
- 不传 type 默认使用 hybrid
- 未登录用户自动降级为热度排序

**API 示例**：
```
GET /api/music/loadRecommendMusic              → 混合推荐
GET /api/music/loadRecommendMusic?type=userCf  → 仅协同过滤
GET /api/music/loadRecommendMusic?type=content → 仅内容推荐
```

---

### 4. feat: 前端接入推荐接口 (c319ca3)

**修改文件**：
- `Api.js` - 新增 loadRecommendMusic 端点
- `CommendList.vue` - 改用新推荐接口

---

### 5. 数据迁移

**SQL**：
```sql
UPDATE music_info m SET good_count = (
  SELECT COUNT(*) FROM music_info_action a WHERE a.music_id = m.music_id
);
```
将 16 首歌的 good_count 从 0 更新为实际点赞数。

---

## 四种推荐算法详解

### 算法 A：基于内容的推荐 (Content-Based)

| 步骤 | 说明 |
|------|------|
| 特征提取 | 从 `music_creation.settings` JSON 提取 musicGener(风格13种)、musicEmotion(情绪10种)、musicSex(声线2种) |
| 用户画像 | 统计用户历史点赞歌曲中每个标签的出现频次，归一化后作为偏好权重 |
| 相似度计算 | 候选歌曲标签与用户画像做加权余弦匹配，输出 0~1 之间的相似度 |

### 算法 B：User-Based 协同过滤

| 步骤 | 说明 |
|------|------|
| 构建交互矩阵 | 从 `music_info_action` 表构建 user_id → liked_music_ids 映射 |
| 用户相似度 | 计算目标用户与其他用户的余弦相似度：`cos(u,v) = |L_u ∩ L_v| / √(|L_u| × |L_v|)` |
| 邻居选择 | 取相似度最高的 Top-10 用户 |
| 评分预测 | 候选歌曲分数 = Σ 邻居用户的相似度（邻居点赞了该歌曲时累加） |

### 算法 C：Item-Based 协同过滤

| 步骤 | 说明 |
|------|------|
| 构建交互矩阵 | 从 `music_info_action` 表构建 music_id → liking_user_ids 映射 |
| 歌曲相似度 | 对每首候选歌曲，计算其与用户已点赞歌曲的余弦相似度（基于共同点赞用户数） |
| 评分预测 | 候选歌曲分数 = 与用户已点赞歌曲的最大相似度 |

### 算法 D：热度排序 (Popularity)

| 步骤 | 说明 |
|------|------|
| 归一化 | playCount / maxPlayCount × 0.5 + goodCount / maxGoodCount × 2.0 |
| 时间衰减 | e^(-创建天数 / 30)，30天半衰期 |

### 算法 E：混合推荐 (Hybrid)

四种算法的加权融合，权重分配：
- Content-Based：0.25 — 内容匹配
- User-Based CF：0.25 — 相似用户偏好
- Item-Based CF：0.25 — 相似歌曲关联
- 热度 + 时间：0.25 — 流行度与新鲜度

冷启动用户（点赞 < 3 首）自动切换为热度排序。

---

## 涉及的知识点

| 知识点 | 代码位置 |
|--------|---------|
| 基于内容的推荐 (Content-Based Filtering) | `scoreByContent()` `computeContentSimilarity()` |
| 协同过滤 (Collaborative Filtering) | `scoreByUserCF()` `scoreByItemCF()` |
| 余弦相似度 (Cosine Similarity) | `cosineSimilarity()` |
| 混合推荐 (Hybrid Recommendation) | `scoreHybrid()` |
| 特征工程 (Feature Engineering) | `parseSettings()` |
| 用户画像 (User Profiling) | `buildContentProfile()` |
| Min-Max 归一化 (Normalization) | `popularityScore()` |
| 时间衰减 (Time Decay) | `recencyScore()` - 指数衰减 e^(-days/30) |
| 冷启动问题 (Cold Start) | 点赞 < 3 回退热度排序 |
| 策略模式 (Strategy Pattern) | type 参数切换不同算法 |

---

## 论文实验建议

端点已支持 type 参数，可以直接用不同 type 请求来获取各算法的推荐结果进行对比：

```
# 四个算法分别请求
GET /api/music/loadRecommendMusic?type=popularity  → 基准对照组 (Baseline)
GET /api/music/loadRecommendMusic?type=content     → 基于内容
GET /api/music/loadRecommendMusic?type=userCf      → 用户协同过滤
GET /api/music/loadRecommendMusic?type=itemCf      → 物品协同过滤
GET /api/music/loadRecommendMusic?type=hybrid      → 混合推荐
```

评价指标建议：
- **准确率**：推荐列表中用户实际点赞的比例（离线：80%训练/20%验证）
- **覆盖率**：推荐列表覆盖的不同歌曲数 / 总歌曲数
- **多样性**：推荐列表中风格/情绪的种类数

## 已知限制

- Content-Based 依赖专家模式的 settings 字段，简单模式的歌曲无标签只能靠热度和 CF
- 当前数据量小（~30首歌，~16条点赞），协同过滤在数据稀疏时效果有限
- 未做在线学习 / 模型训练，属于规则式的启发算法
- 相似度计算在 Java 内存中完成，数据量大时需迁移到离线预计算 + Redis 缓存
