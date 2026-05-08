# 推荐算法模块开发日志

## 概述

将 EasyMusic 首页推荐从人工精选模式升级为混合推荐算法。实现基于内容+热度+时间衰减的个性化推荐。

## 变更清单

### 1. fix: 修复 good_count 字段更新 bug (d05156a)

**问题**：点赞/取消点赞时只操作 `music_info_action` 表，`music_info.good_count` 从未更新，一直是 0。

**修改文件**：
- `MusicInfoActionServiceImpl.java` - doGood() 方法中，点赞时调用 updateGoodCount(+1)，取消时 updateGoodCount(-1)
- `MusicInfoMapper.java` - 新增 updateGoodCount 方法
- `MusicInfoMapper.xml` - 新增 SQL：`UPDATE music_info SET good_count = good_count + #{delta}`

### 2. feat: 创建 RecommendationService 推荐服务 (acb22b3)

**新增文件**：
- `RecommendationService.java` - 推荐服务接口
- `RecommendationServiceImpl.java` - 推荐服务实现

**算法设计**：

```
推荐分数 = 内容相似度 × 0.6 + 热度分 × 0.3 + 时间衰减 × 0.1
冷启动 = 热度 × 0.7 + 时间衰减 × 0.3（点赞不足3首时）
```

**内容相似度**：
- 从 music_creation.settings JSON 中提取三个维度：musicGener(风格)、musicEmotion(情绪)、musicSex(声线)
- 统计用户历史点赞的标签频率构建偏好画像
- 候选歌曲与用户画像做归一化余弦相似度匹配

**热度分**：
- Min-Max 归一化：playCount/maxPlayCount × 0.5 + goodCount/maxGoodCount × 2.0
- 点赞权重 = 播放权重的 4 倍

**时间衰减**：
- e^(-days/30)，30 天半衰期

### 3. feat: 新增 /music/loadRecommendMusic 接口 (9303c3c)

**修改文件**：
- `MusicController.java` - 新增 loadRecommendMusic() 方法
  - 未登录 → 纯热度排序
  - 已登录 → 混合推荐算法

### 4. feat: 前端接入推荐接口 (c319ca3)

**修改文件**：
- `Api.js` - 新增 loadRecommendMusic 端点
- `CommendList.vue` - 改用新推荐接口

### 5. 数据迁移

**SQL**：
```sql
UPDATE music_info m SET good_count = (
  SELECT COUNT(*) FROM music_info_action a WHERE a.music_id = m.music_id
);
```
将 16 首歌的 good_count 从 0 更新为实际点赞数。

## 算法涉及的知识点

| 知识点 | 实现 |
|--------|------|
| 混合推荐 (Hybrid Recommendation) | 内容+热度+时间三因子加权 |
| 基于内容的推荐 (Content-Based) | 风格/情绪/声线标签匹配 |
| 特征工程 | settings JSON 特征提取 |
| 用户画像构建 | 基于显式反馈的统计式画像 |
| 归一化 (Normalization) | Min-Max 归一化 |
| 时间衰减 (Time Decay) | 指数衰减，30天半衰期 |
| 冷启动 (Cold Start) | 点赞不足3首回退热度排序 |

## 已知限制

- content-based 依赖专家模式的 settings 字段，简单模式的歌曲无标签，只能靠热度
- 当前无用户行为追踪（收听时长、跳过），推荐信号主要是点赞
- 未做 item-based 协同过滤，无法捕捉"喜欢这首歌的人也喜欢"模式
- 相似度计算在 Java 内存中完成，数据量大时建议迁移到离线计算+缓存
