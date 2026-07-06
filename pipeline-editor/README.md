# Pipeline Editor - 流水线编排器

基于 Vue 3 + TypeScript + Element Plus 实现的流水线编排前端，参照 Blue Ocean 设计，模拟阿里云效流水线编排效果。

## 技术栈

- Vue 3 (Composition API)
- TypeScript
- Element Plus
- Pinia (状态管理)
- Vue Router

## 快速开始

```bash
npm install
npm run dev      # 启动开发服务器 (http://localhost:3000)
npm run build    # 构建生产版本
```

## 核心界面：流水线详情页

路由路径：`/pipelineDetail/:id`

该界面为核心编辑界面，分为三个区域：
- **左侧**：组件库（拖拽添加步骤）
- **中间**：画布（可视化编排 Stage / Step）
- **右侧**：配置面板（编辑选中节点）

### 文件依赖关系

```
PipelineDetail.vue  (主页面，三栏布局 + 所有事件处理)
  ├── PipelineCanvas2.vue   (中间画布，渲染 Stage/Branch/Step 层级结构)
  ├── StageEditor2.vue      (Stage/Step 配置弹窗)
  ├── LogViewer2.vue        (运行日志抽屉)
  ├── RunStagesDialog.vue   (运行阶段状态弹窗)
  ├── stores/pipeline2.ts   (Pinia Store，所有状态和业务逻辑)
  ├── types/pipeline.ts     (核心数据类型定义)
  └── utils/jenkinsfile.ts  (Pipeline ↔ Jenkinsfile 转换)
```

### 文件详解（按迁移优先级）

| 优先级 | 文件 | 行数 | 职责 |
|--------|------|------|------|
| ★★★★★ | `src/types/pipeline.ts` | 213 | 核心数据结构：`Pipeline`、`PipelineStage`、`PipelineBranch`、`PipelineStep`、`PipelineRun` 等，迁移前必须在目标系统中定义好这些类型 |
| ★★★★★ | `src/stores/pipeline2.ts` | 1135 | 所有业务逻辑入口：`fetchPipelines()`、`fetchPipelineContent()`、`addStage()`、`updateStage()`、`removeStage()`、`removeStep()`、`runPipeline()` |
| ★★★★★ | `src/views/PipelineDetail.vue` | 2059 | **主页面**，包含：三栏布局（61-249行左侧组件库、252-260行画布、261-315行右侧面板），所有事件处理函数（`selectStage`、`selectStep`、`addQuickTask`、`addStepToStage`、`runPipeline`、`savePipeline` 等） |
| ★★★★☆ | `src/components/PipelineCanvas2.vue` | 1349 | 画布组件：水平渲染 Stage 列表，每个 Stage 内渲染 Branch 和 Step，支持选中高亮、运行进度展示、添加/删除节点 |
| ★★★★☆ | `src/components/StageEditor2.vue` | 977 | Stage/Step 配置弹窗：编辑名称、执行类型（串行/并行）、执行条件、步骤参数配置等 |
| ★★★☆☆ | `src/utils/jenkinsfile.ts` | 813 | 双向转换：`pipelineToJenkinsfile()` 将 Pipeline 数据结构生成 Jenkinsfile Groovy 脚本；`jenkinsfileToPipeline()` 逆向解析 |
| ★★☆☆☆ | `src/components/LogViewer2.vue` | — | 运行日志展示面板 |
| ★★☆☆☆ | `src/components/RunStagesDialog.vue` | — | 运行阶段详情弹窗 |

### PipelineDetail.vue 内部关键函数

| 函数 | 行号 | 说明 |
|------|------|------|
| `selectStage(stageId)` | 1051 | 选中一个阶段，清空选中的步骤 |
| `selectStep(stepId, step)` | 1058 | 选中一个步骤，打开配置面板 |
| `addQuickTask(type, name, config)` | 1230 | 左侧组件库入口：无阶段时创建默认阶段；有选中阶段/步骤时添加到对应阶段，否则添加到最后一个阶段 |
| `addStepToStage(stageId, branchId, type, name, config)` | 1250 | 向指定阶段的指定分支添加步骤 |
| `addPipelineStageDialog()` | 1070 | 添加新阶段 |
| `savePipeline()` | — | 保存流水线到后端 |
| `runPipeline()` | — | 启动流水线执行 |

### 数据流

```
用户操作 (点击/拖拽)
  → PipelineDetail.vue 事件处理
    → store action (fetchPipelines / addStage / updateStage / runPipeline)
      → state 变更 (pipelines / currentPipeline / runs)
        → 响应式更新 UI (PipelineCanvas2 / StageEditor2 / LogViewer2)
```

### 路由定义

```ts
{ path: '/pipelines',       component: PipelineList }
{ path: '/pipelineDetail/:id?', component: PipelineDetail }
{ path: '/config',          component: Config }
{ path: '/login',           component: Login }
{ path: '/',                redirect: '/pipelines' }
```
