---
name: pipeline-auto-refresh-design
description: 流水线启动后自动刷新执行记录
type: spec
---

# 流水线启动后自动刷新执行记录

## 问题

当前点击启动按钮后，执行记录只刷新一次，无法实时显示流水线运行状态。

## 解决方案

### 方案 C：立即刷新 + 持续轮询直到完成

1. **立即刷新**：点击启动后立即调用 `fetchRuns`
2. **持续轮询**：每 3 秒轮询一次执行记录，直到所有 run 完成或超时

## 实现

### Store 修改 (`pipeline2.ts`)

**新增 State：**
- `autoRefreshRunId: string | null` - 当前自动刷新的 run ID
- `refreshIntervalId: number | null` - 定时器 ID

**新增 Action：**
- `startAutoRefresh(pipelineId: string)` - 启动自动刷新
  - 立即调用 `fetchRuns(pipelineId)`
  - 每 3 秒调用 `fetchRuns(pipelineId)`
  - 当所有 runs 不再是 `running` 状态，停止轮询
  - 超时时间：60 次（3 分钟）

- `stopAutoRefresh()` - 停止自动刷新
  - 清除定时器
  - 重置 `autoRefreshRunId`

### 组件修改 (`PipelineEditor2.vue`)

在 `runPipeline()` 函数中：
- 启动成功后调用 `store.startAutoRefresh(store.currentPipeline.id)`

## 轮询策略

| 参数 | 值 |
|------|-----|
| 轮询间隔 | 3 秒 |
| 最大次数 | 60 次 |
| 最大时长 | 3 分钟 |
| 停止条件 | 所有 runs 状态不是 `running` |

## 数据流

```
用户点击启动 → API 调用成功 → startAutoRefresh()
                                        ↓
                              fetchRuns() 立即执行
                                        ↓
                              设置定时器，每 3 秒执行
                                        ↓
                              检查 runs 状态
                                        ↓
                              全部完成？ → 停止轮询
                              继续运行？ → 继续轮询（最多 60 次）
```