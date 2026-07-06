# Pipeline Editor 流水线编排器
## 需求规格说明书

---

**文档版本**：1.0  
**编写日期**：2026-05-25  
**项目名称**：Pipeline Editor  
**项目描述**：基于 Vue 3 + TypeScript + Element Plus 实现的流水线编排前端，参照 Blue Ocean 设计，模拟阿里云效流水线编排效果。  
**用途**：汇报

---

## 一、项目概述

### 1.1 背景与目标

Pipeline Editor 是一款企业级流水线可视化编排工具，旨在帮助开发团队通过图形化界面快速构建、配置和管理 CI/CD 流水线。系统参照 Jenkins Blue Ocean 的设计风格，同时借鉴阿里云效流水线的交互体验，为用户提供简洁直观的操作界面。

### 1.2 核心价值

- **可视化编排**：通过拖拽式操作完成 Stage 和步骤的配置
- **多类型支持**：支持串行和并行两种执行模式
- **配置转换**：一键将可视化配置转换为 Jenkinsfile
- **执行模拟**：本地模拟流水线运行，实时展示各 Stage 状态
- **日志追踪**：完整记录每次构建的日志输出

---

## 二、技术架构

### 2.1 技术栈

| 类别 | 技术选型 |
|------|----------|
| 前端框架 | Vue 3 (Composition API) |
| 类型系统 | TypeScript |
| UI 组件库 | Element Plus |
| 状态管理 | Pinia |
| 路由管理 | Vue Router |
| 构建工具 | Vite |

### 2.2 项目结构

```
src/
├── components/           # 组件目录
│   ├── PipelineCanvas2.vue   # 流水线可视化画布
│   ├── StageEditor2.vue      # Stage 编辑器
│   ├── LogViewer2.vue        # 日志查看器
│   └── RunStagesDialog.vue   # 构建阶段详情弹窗
├── views/
│   ├── PipelineEditor2.vue   # 主页面
│   └── Login.vue             # 登录页
├── stores/
│   └── pipeline2.ts          # Pinia Store
├── types/
│   └── pipeline.ts           # TypeScript 类型定义
├── utils/
│   ├── jenkinsfile.ts        # Jenkinsfile 转换工具
│   ├── layout.ts             # 画布布局工具
│   └── axios.ts              # HTTP 请求工具
└── router/
    └── index.ts              # 路由配置
```

---

## 三、数据模型

### 3.1 核心类型定义

#### Pipeline（流水线）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | string | 唯一标识符 |
| name | string | 流水线名称 |
| description | string | 流水线描述 |
| agent | AgentConfig | Agent 配置 |
| environment | EnvVar[] | 环境变量列表 |
| stages | PipelineStage[] | 阶段列表 |
| post | PostConfig | 后置处理配置 |
| createdAt | string | 创建时间 |
| updatedAt | string | 更新时间 |

#### PipelineStage（阶段）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | string | 唯一标识符 |
| name | string | 阶段名称 |
| type | 'sequential' \| 'parallel' | 执行类型：串行/并行 |
| branches | PipelineBranch[] | 分支列表（并行模式下可有多个分支） |
| status | StageStatus | 当前状态 |
| when | WhenCondition | 执行条件 |

#### PipelineBranch（分支）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | string | 唯一标识符 |
| name | string | 分支名称 |
| steps | PipelineStep[] | 步骤列表 |
| status | StageStatus | 当前状态 |

#### PipelineStep（步骤）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | string | 唯一标识符 |
| type | StepType | 步骤类型 |
| name | string | 步骤名称 |
| config | Record<string, any> | 配置参数 |

#### AgentConfig（Agent 配置）

| 字段 | 类型 | 说明 |
|------|------|------|
| type | 'any' \| 'label' \| 'docker' \| 'kubernetes' \| 'none' | Agent 类型 |
| label | string | 节点标签（当 type 为 label 时） |
| dockerImage | string | Docker 镜像（当 type 为 docker 时） |
| kubernetesYaml | string | Kubernetes YAML（当 type 为 kubernetes 时） |

### 3.2 Stage 状态

| 状态值 | 说明 |
|--------|------|
| pending | 等待中 |
| running | 运行中 |
| success | 成功 |
| failed | 失败 |
| aborted | 已中止 |
| skipped | 已跳过 |
| unstable | 不稳定 |

---

## 四、功能需求

### 4.1 流水线管理

#### 4.1.1 创建流水线

- 用户输入流水线名称和描述
- 系统自动生成唯一 ID 和时间戳
- 支持多条流水线同时存在

#### 4.1.2 选择流水线

- 通过下拉框选择当前操作的流水线
- 切换流水线时自动加载对应配置

#### 4.1.3 删除流水线

- 删除前需二次确认
- 删除后自动切换到下一个流水线（如有）

#### 4.1.4 保存流水线

- 将 Jenkinsfile 内容保存到后端
- 验证流水线配置的完整性

### 4.2 Stage 编排

#### 4.2.1 添加 Stage

- 支持通过弹窗创建新 Stage
- Stage 名称必填
- 支持配置执行类型：串行/并行
- 支持配置执行条件（分支匹配、表达式）

#### 4.2.2 编辑 Stage

- 修改 Stage 名称
- 调整执行类型
- 配置/修改执行条件

#### 4.2.3 删除 Stage

- 直接删除，无需确认
- 删除后更新画布布局

#### 4.2.4 可视化展示

- Stage 以卡片形式展示在画布上
- 串行 Stage 按从左到右顺序排列
- 并行 Stage 内部以分支形式垂直排列
- Stage 状态通过颜色区分：
  - 绿色：成功
  - 红色：失败
  - 蓝色：运行中
  - 灰色：已跳过

### 4.3 步骤配置

#### 4.3.1 支持的步骤类型（共 56 种）

**构建类（11 种）**

| 类型 | 说明 |
|------|------|
| sh | Shell 命令 |
| bat | Windows Batch |
| powershell | PowerShell |
| python | Python 脚本 |
| dockerBuildAndPush | Docker 构建并推送 |
| dockerPull | Docker 拉取镜像 |
| dockerRun | Docker 运行容器 |
| kubectl | Kubernetes 命令 |
| maven | Maven 构建 |
| gradle | Gradle 构建 |
| npm | NPM 构建 |

**版本控制类（3 种）**

| 类型 | 说明 |
|------|------|
| git | Git 拉取 |
| checkout | 通用 SCM 检出 |
| svn | SVN 拉取 |

**测试类（7 种）**

| 类型 | 说明 |
|------|------|
| junit | JUnit 测试结果 |
| coverage | 覆盖率报告 |
| findbugs | FindBugs 静态分析 |
| checkstyle | Checkstyle 代码检查 |
| sonar | SonarQube 扫描 |
| pytest | PyTest 测试 |
| nose | Nose 测试 |

**部署类（5 种）**

| 类型 | 说明 |
|------|------|
| deploy | 通用部署 |
| sshCommand | SSH 远程命令 |
| sshScript | SSH 远程脚本 |
| ansiblePlaybook | Ansible Playbook |
| ansibleAdHoc | Ansible 临时命令 |

**制品与文件类（8 种）**

| 类型 | 说明 |
|------|------|
| archiveArtifacts | 归档制品 |
| archiveAWS S3 | 归档到 S3 |
| copyArtifacts | 复制制品 |
| stash | 暂存文件 |
| unstash | 恢复暂存 |
| cleanWs | 清理工作区 |
| deleteDir | 删除目录 |
| writeFile | 写入文件 |
| readFile | 读取文件 |

**流程控制类（9 种）**

| 类型 | 说明 |
|------|------|
| input | 人工审批 |
| timeout | 超时控制 |
| retry | 重试 |
| sleep | 等待 |
| waitUntil | 等待条件 |
| catchError | 捕获错误 |
| error | 抛出错误 |
| unstable | 标记不稳定 |
| lock | 锁定资源 |
| milestone | 里程碑 |

**环境与凭据类（6 种）**

| 类型 | 说明 |
|------|------|
| withCredentials | 使用凭据 |
| withEnv | 使用环境变量 |
| withAWS | AWS 凭据 |
| withDockerRegistry | Docker 注册表 |
| dir | 切换目录 |
| tool | 调用工具 |

**通知类（4 种）**

| 类型 | 说明 |
|------|------|
| echo | 打印消息 |
| mail | 发送邮件 |
| slackSend | Slack 通知 |
| dingtalk | 钉钉通知 |
| wechat | 企业微信通知 |

**其他类（3 种）**

| 类型 | 说明 |
|------|------|
| build | 触发构建 |
| script | 脚本块 |
| custom | 自定义代码 |

#### 4.3.2 添加步骤

- 从组件库或 Stage 内添加步骤
- 每个步骤包含类型、名称、配置参数
- 部分步骤需要特定配置（如 git 需要 url 和 branch）

#### 4.3.3 编辑步骤

- 修改步骤类型和名称
- 根据类型动态显示对应配置表单

#### 4.3.4 删除步骤

- 从步骤列表中移除

### 4.4 Jenkinsfile 转换

#### 4.4.1 转换为 Jenkinsfile

- 将流水线配置转换为标准 Jenkinsfile 格式
- 支持 Agent、Environment、Stages、Post 各部分
- 生成语法正确的 Declarative Pipeline

#### 4.4.2 预览功能

- 支持 Jenkinsfile、JSON、YAML 三种格式预览
- 支持一键复制到剪贴板

### 4.5 执行与日志

#### 4.5.1 模拟运行

- 点击"保存并运行"触发模拟执行
- 运行时显示进度条和各 Stage 状态
- 随机生成执行结果（90% 成功率）
- 记录每个 Stage 的耗时

#### 4.5.2 执行记录

- 记录每次运行的元信息（构建号、触发者、时间、时长）
- 通过抽屉展示运行历史
- 每个运行记录可查看日志和阶段详情

#### 4.5.3 日志查看

- 支持查看运行日志
- 支持下载日志到本地

### 4.6 配置面板

#### 4.6.1 流水线配置

- 流水线名称和描述编辑
- Agent 类型选择（Any/Label/Docker/Kubernetes）
- 环境变量管理（添加、删除）

#### 4.6.2 步骤配置

- 选中步骤后显示配置面板
- 支持修改步骤名称和类型特定配置

---

## 五、界面设计

### 5.1 整体布局

系统采用三栏式布局：

```
+------------------+------------------------+------------------+
|    Header       |                        |                  |
+------------------+------------------------+------------------+
|                  |                        |                  |
|   左侧组件库     |     中间画布区域        |   右侧配置面板   |
|   (260px)       |     (自适应)            |   (380px)        |
|                  |                        |                  |
|                  |                        |                  |
+------------------+------------------------+------------------+
```

### 5.2 Header 区域

- 左侧：Logo + 流水线选择下拉框 + 保存状态
- 右侧：新建/删除流水线、验证、查看YAML、执行记录、保存并运行按钮

### 5.3 画布区域

- 顶部工具栏：添加阶段、自动布局、缩放控制
- 主体：Stage 卡片列表，支持水平滚动
- 每个 Stage 包含头部（名称、状态、操作按钮）和身体（步骤列表）

### 5.4 状态指示

| 状态 | 颜色 | 动画 |
|------|------|------|
| 成功 | 绿色 (#52c41a) | 无 |
| 失败 | 红色 (#ff4d4f) | 无 |
| 运行中 | 蓝色 (#1677ff) | 呼吸动画 |
| 已跳过 | 灰色 (#d9d9d9) | 无 |
| 等待中 | 橙色 (#faad14) | 无 |

---

## 六、接口设计

### 6.1 保存流水线

```
POST /api/pipeline/save
Request:
{
  "jobName": string,
  "jenkinsfile": string
}
Response:
{
  "success": boolean,
  "message": string
}
```

---

## 七、非功能需求

### 7.1 性能需求

- 页面初次加载时间 < 3 秒
- 流水线切换响应时间 < 500ms
- Jenkinsfile 生成时间 < 200ms

### 7.2 兼容性需求

- 支持 Chrome、Firefox、Safari、Edge 最新两个版本
- 1920 × 1080 及以上分辨率最佳
- 最小支持宽度 1280px

### 7.3 安全需求

- 前端不存储敏感凭据信息
- 所有凭据通过后端接口获取

---

## 八、版本信息

### 8.1 当前版本

| 字段 | 值 |
|------|-----|
| 版本号 | 1.0.0 |
| 构建命令 | npm run build |
| 开发命令 | npm run dev |
| 预览命令 | npm run preview |
| 开发端口 | 3000 |

### 8.2 已知限制

- 运行功能为模拟实现，非真实流水线执行
- Jenkinsfile 解析功能不完整
- 未实现用户登录认证

---

## 九、附录

### 9.1 术语表

| 术语 | 说明 |
|------|------|
| Stage | 流水线中的阶段，代表一个执行单元 |
| Branch | 分支，用于并行执行时的任务分组 |
| Step | 步骤，Stage 内具体的执行动作 |
| Agent | 执行器，指定流水线运行的节点 |
| Jenkinsfile | Jenkins 流水线的配置文件 |

### 9.2 参考资料

- [Jenkins Pipeline Syntax](https://www.jenkins.io/doc/book/pipeline/syntax/)
- [Vue 3 Documentation](https://vuejs.org/)
- [Element Plus](https://element-plus.org/)