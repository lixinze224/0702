# Pipeline Editor - 流水线编排器

基于 Vue 3 + TypeScript + Element Plus 实现的流水线编排前端，参照 Blue Ocean 设计，模拟阿里云效流水线编排效果。

## 功能特性

- **可视化编排**：拖拽式 Stage 编排，支持串行和并行执行
- **步骤配置**：支持 11 种常用 Jenkins Pipeline 步骤类型
- **Jenkinsfile 转换**：自动将编排结果转换为 Jenkinsfile
- **运行模拟**：模拟流水线运行，可视化展示各 Stage 状态
- **日志查看**：实时查看各 Stage 运行日志，支持下载
- **运行历史**：记录每次构建的运行状态和日志

## 技术栈

- Vue 3 (Composition API)
- TypeScript
- Element Plus
- Pinia (状态管理)
- Vue Router

## 快速开始

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build
```

## 使用说明

1. **创建流水线**：点击"新建流水线"创建新的流水线
2. **添加 Stage**：在左侧 Stage 列表点击"添加 Stage"
3. **配置步骤**：在 Stage 编辑器中添加和配置步骤
4. **运行流水线**：点击"运行"按钮模拟执行
5. **查看日志**：在运行历史中点击构建记录查看日志
6. **导出 Jenkinsfile**：点击"Jenkinsfile"按钮预览生成的 Jenkinsfile

## 支持的步骤类型

- Shell 命令 (sh)
- 打印信息 (echo)
- Git 拉取 (git)
- 人工审批 (input)
- 等待 (sleep)
- 超时控制 (timeout)
- 重试 (retry)
- 归档制品 (archiveArtifacts)
- JUnit 测试 (junit)
- 清理工作区 (cleanWs)
- 自定义代码 (custom)

## 项目结构

```
src/
  components/
    PipelineCanvas.vue    # 流水线可视化画布
    StageEditor.vue      # Stage 编辑器
    LogViewer.vue        # 日志查看器
  views/
    PipelineEditor.vue   # 主页面
  stores/
    pipeline.ts          # Pinia Store
  types/
    pipeline.ts          # TypeScript 类型定义
  utils/
    jenkinsfile.ts       # Jenkinsfile 转换工具
    layout.ts            # 画布布局工具
```
