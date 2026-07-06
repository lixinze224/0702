// 流水线编排核心类型定义

// 步骤类型 (参考 Jenkins Pipeline Steps)
export type StepType =
  // 构建相关
  | 'sh'                    // Shell 命令
  | 'bat'                   // Windows Batch
  | 'powershell'            // PowerShell
  | 'python'                // Python 脚本
  | 'dockerBuildAndPush'    // Docker 构建并推送
  | 'dockerPull'            // Docker 拉取镜像
  | 'dockerRun'             // Docker 运行容器
  | 'kubectl'               // Kubernetes 命令
  | 'maven'                 // Maven 构建
  | 'gradle'                // Gradle 构建
  | 'npm'                   // NPM 构建
  | 'ant'                   // Ant 构建
  // 版本控制
  | 'git'                   // Git 拉取
  | 'checkout'              // 通用 SCM 检出
  | 'svn'                   // SVN 拉取
  // 测试相关
  | 'junit'                 // JUnit 测试结果
  | 'coverage'              // 覆盖率报告
  | 'findbugs'              // FindBugs 静态分析
  | 'checkstyle'            // Checkstyle 代码检查
  | 'sonar'                 // SonarQube 扫描
  | 'pytest'                // PyTest 测试
  | 'nose'                  // Nose 测试
  // 部署相关
  | 'deploy'                // 通用部署
  | 'sshCommand'            // SSH 远程命令
  | 'sshScript'             // SSH 远程脚本
  | 'ansiblePlaybook'        // Ansible Playbook
  | 'ansibleAdHoc'           // Ansible 临时命令
  // 制品与文件
  | 'archiveArtifacts'      // 归档制品
  | 'archiveAWS S3'          // 归档到 S3
  | 'copyArtifacts'          // 复制制品
  | 'stash'                  // 暂存文件
  | 'unstash'                // 恢复暂存
  | 'cleanWs'                // 清理工作区
  | 'deleteDir'              // 删除目录
  | 'writeFile'              // 写入文件
  | 'readFile'               // 读取文件
  // 流程控制
  | 'input'                 // 人工审批
  | 'timeout'               // 超时控制
  | 'retry'                 // 重试
  | 'sleep'                 // 等待
  | 'waitUntil'             // 等待条件
  | 'catchError'            // 捕获错误
  | 'error'                 // 抛出错误
  | 'unstable'              // 标记不稳定
  | 'lock'                  // 锁定资源
  | 'milestone'             // 里程碑
  // 环境与凭据
  | 'withCredentials'       // 使用凭据
  | 'withEnv'               // 使用环境变量
  | 'withAWS'               // AWS 凭据
  | 'withDockerRegistry'    // Docker 注册表
  | 'dir'                   // 切换目录
  | 'chdir'                 // 切换目录 (Windows)
  | 'tool'                  // 调用工具
  // 通知
  | 'echo'                  // 打印消息
  | 'mail'                  // 发送邮件
  | 'slackSend'             // Slack 通知
  | 'dingtalk'              // 钉钉通知
  | 'wechat'                // 企业微信通知
  // 其他
  | 'build'                 // 触发构建
  | 'script'                // 脚本块
  | 'parallel'              // 并行块 (嵌套)
  | 'structuredClone'       // 结构化克隆
  | 'custom'               // 自定义代码
  // 编译相关
  | 'jar'                  // 构建jar
  | 'cpp'                  // C++多平台构建
  | 'go'                   // Go多平台构建
  | 'qt'                   // Qt多平台构建
  | 'javaImage'            // JAVA生成镜像
  | 'node'                 // Node编译
  | 'net'                  // Net编译
  | 'python'               // Python编译
  | 'php'                  // PHP编译
  // 推送相关
  | 'pushImageDeploy'     // 推送镜像到制品库（部署）
  | 'pushImageRelease'     // 推送镜像到制品库（发版）
  // 测试相关
  | 'apiTest'            // 接口测试
  // 部署相关
  | 'cloudDeploy'         // 云应用开发环境部署
  | 'scpDeploy';          // 远程传输文件部署

// 步骤定义
export interface PipelineStep {
  id: string;
  type: StepType;
  name: string;
  config: Record<string, any>;
}

// Stage 状态
export type StageStatus = 'pending' | 'running' | 'success' | 'failed' | 'aborted' | 'skipped' | 'unstable';

// 分支（用于并行）
export interface PipelineBranch {
  id: string;
  name: string;
  steps: PipelineStep[];
  status?: StageStatus;
  duration?: number;
  logs?: string[];
}

// Stage 定义
export interface PipelineStage {
  id: string;
  name: string;
  type: 'sequential' | 'parallel';
  branches: PipelineBranch[];
  // 当 type='sequential' 时，branches 只有一个元素
  // 当 type='parallel' 时，branches 有多个元素
  status?: StageStatus;
  duration?: number;
  when?: {
    branch?: string;
    expression?: string;
  };
}

// Agent 配置
export interface AgentConfig {
  type: 'any' | 'label' | 'docker' | 'kubernetes' | 'none';
  label?: string;
  dockerImage?: string;
  kubernetesYaml?: string;
}

// 环境变量
export interface EnvVar {
  key: string;
  value: string;
}

// 流水线定义
export interface Pipeline {
  id: string;
  name: string;
  description: string;
  agent: AgentConfig;
  environment: EnvVar[];
  stages: PipelineStage[];
  post: {
    always?: PipelineStep[];
    success?: PipelineStep[];
    failure?: PipelineStep[];
    unstable?: PipelineStep[];
    aborted?: PipelineStep[];
  };
  jenkinsfileContent?: string;
  createdAt: string;
  updatedAt: string;
  stageNum?: number;
}

// 流水线运行记录
export interface PipelineRun {
  id: string;
  pipelineId: string;
  buildNumber: number;
  status: StageStatus;
  startTime: string;
  endTime?: string;
  duration?: number;
  triggeredBy: string;
  commitId?: string;
  branch?: string;
  stages: {
    stageId: string;
    status: StageStatus;
    startTime?: string;
    endTime?: string;
    duration?: number;
    logs: string[];
  }[];
}

// 节点位置（用于可视化）
export interface NodePosition {
  x: number;
  y: number;
  width: number;
  height: number;
}

// 画布节点
export interface CanvasNode {
  id: string;
  type: 'stage' | 'start' | 'end' | 'parallel-group';
  data: PipelineStage | null;
  position: NodePosition;
  parentId?: string;
}

// 画布连线
export interface CanvasEdge {
  id: string;
  source: string;
  target: string;
  type: 'default' | 'parallel';
}
