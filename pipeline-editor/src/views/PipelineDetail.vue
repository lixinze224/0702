<template>
  <div class="pipeline-editor-page">
    <!-- 顶部导航 -->
    <header class="header">
      <div class="header-left">
        <el-button text @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <div class="logo">
          <el-icon :size="20"><Connection /></el-icon>
          <span>Flow 流水线编排</span>
        </div>
        <div class="pipeline-selector">
          <el-select
            v-model="selectedPipelineId"
            placeholder="选择流水线"
            size="default"
            disabled
            style="width:200px;"
            @change="handlePipelineChange"
          >
            <el-option
              v-for="pipeline in store.pipelines"
              :key="pipeline.id"
              :label="pipeline.name"
              :value="String(pipeline.id)"
            />
          </el-select>
        </div>
        <!-- <span class="divider">|</span>
        <span class="save-status">未保存的更改</span> -->
      </div>
      <div class="header-right">
        <el-button @click="validatePipeline" :disabled="!hasSteps">
          <el-icon><Check /></el-icon>
          验证
        </el-button>
        <el-button @click="openJenkinsfileDialog" :disabled="!hasSteps">
          <el-icon><Document /></el-icon>
          查看YAML
        </el-button>
        <el-button @click="showRunsDrawer = true" :disabled="!hasSteps">
          <el-icon><Clock /></el-icon>
          执行记录
          <el-badge v-if="store.pipelineRuns.length > 0" :value="store.pipelineRuns.length" :max="99" />
        </el-button>
        <el-button type="primary" :loading="store.isRunning" :disabled="!hasSteps" @click="savePipeline">
          <el-icon><VideoPlay /></el-icon>
          保存
        </el-button>
        <el-button type="primary" :loading="store.isRunning" :disabled="!hasSteps" @click="runPipeline">
          <el-icon><VideoPlay /></el-icon>
          启动
        </el-button>
      </div>
    </header>

    <!-- 主体区域 -->
    <div class="main-container">
      <!-- 左侧组件库 -->
      <aside class="sidebar">
        <div class="sidebar-header">
          <el-icon color="#1677ff"><Box /></el-icon>
            组件库
            <el-button @click="addPipelineStageDialog">
              <el-icon><Plus /></el-icon>
              新建阶段
            </el-button>
        </div>
        <div class="sidebar-content">
          <div class="category">
            <div class="category-title">代码源</div>
            <div class="node-item" @click="addGitTask">
              <el-icon><Download /></el-icon>
              <div>
                <div>拉取代码</div>
                <div class="node-desc">从git拉代码</div>
              </div>
            </div>
          </div>

          <div class="category">
            <div class="category-title">编译</div>
            <div class="node-item" @click="addCompileTask('jar')">
              <el-icon><Box /></el-icon>
              <div>
                <div>构建jar</div>
                <div class="node-desc">将程序打成jar包</div>
              </div>
            </div>
            <!-- <div class="node-item" @click="addCompileTask('cpp')">
              <el-icon><Box /></el-icon>
              <div>
                <div>C++多平台构建</div>
                <div class="node-desc">使用多平台构建C++</div>
              </div>
            </div> -->
            <div class="node-item" @click="addCompileTask('go')">
              <el-icon><Box /></el-icon>
              <div>
                <div>Go多平台构建</div>
                <div class="node-desc">使用多平台构建Go</div>
              </div>
            </div>
            <div class="node-item" @click="addCompileTask('qt')">
              <el-icon><Box /></el-icon>
              <div>
                <div>Qt多平台构建</div>
                <div class="node-desc">使用多平台构建qt类型</div>
              </div>
            </div>
            <div class="node-item" @click="addCompileTask('javaImage')">
              <el-icon><Box /></el-icon>
              <div>
                <div>JAVA生成镜像(Arm)</div>
                <div class="node-desc">将jar包打成镜像(Arm版本)</div>
              </div>
            </div>
            <div class="node-item" @click="addCompileTask('node')">
              <el-icon><Cpu /></el-icon>
              <div>
                <div>Node编译</div>
                <div class="node-desc">Node编译</div>
              </div>
            </div>
            <div class="node-item" @click="addCompileTask('net')">
              <el-icon><Box /></el-icon>
              <div>
                <div>Net编译</div>
                <div class="node-desc">.NET编译</div>
              </div>
            </div>
            <!-- <div class="node-item" @click="addCompileTask('python')">
              <el-icon><Box /></el-icon>
              <div>
                <div>Python编译</div>
                <div class="node-desc">Python编译</div>
              </div>
            </div> -->
            <div class="node-item" @click="addCompileTask('php')">
              <el-icon><Box /></el-icon>
              <div>
                <div>PHP编译</div>
                <div class="node-desc">PHP编译</div>
              </div>
            </div>
          </div>

          <div class="category">
            <div class="category-title">扫描</div>
            <div class="node-item" @click="addScanTask('code')">
              <el-icon><Filter /></el-icon>
              <div>
                <div>代码扫描</div>
                <div class="node-desc">扫描当前代码</div>
              </div>
            </div>
          </div>

          <div class="category">
            <div class="category-title">推送</div>
            <div class="node-item" @click="addPushTask('deploy')">
              <el-icon><Upload /></el-icon>
              <div>
                <div>推送镜像到制品库（部署）</div>
                <div class="node-desc">推送镜像到制品库</div>
              </div>
            </div>
            <div class="node-item" @click="addPushTask('release')">
              <el-icon><Upload /></el-icon>
              <div>
                <div>推送镜像到制品库（发版）</div>
                <div class="node-desc">推送镜像到制品库</div>
              </div>
            </div>
          </div>

          <div class="category">
            <div class="category-title">测试</div>
            <div class="node-item" @click="addTestTask('api')">
              <el-icon><Clock /></el-icon>
              <div>
                <div>接口测试</div>
                <div class="node-desc">postman接口测试</div>
              </div>
            </div>
          </div>
          <div class="category">
            <div class="category-title">发送邮件</div>
            <div class="node-item" @click="addTestTask('email')">
              <el-icon><Message /></el-icon>
              <div>
                <div>发送邮件</div>
                <div class="node-desc">发送邮件</div>
              </div>
            </div>
          </div>
          <div class="category">
            <div class="category-title">审核</div>
            <div class="node-item" @click="addApproveTask('approve')">
              <el-icon><User /></el-icon>
              <div>
                <div>审批卡点</div>
                <div class="node-desc">进行人为卡点审批</div>
              </div>
            </div>
          </div>

          <div class="category">
            <div class="category-title">部署</div>
            <div class="node-item" @click="addDeployTask('cloud')">
              <el-icon><Monitor /></el-icon>
              <div>
                <div>云应用开发环境部署</div>
                <div class="node-desc">云应用开发环境部署</div>
              </div>
            </div>
            <div class="node-item" @click="addDeployTask('scp')">
              <el-icon><Upload /></el-icon>
              <div>
                <div>远程传输文件部署</div>
                <div class="node-desc">远程传输文件部署</div>
              </div>
            </div>
          </div>

          <!-- <div class="category">
            <div class="category-title">流程控制</div>
            <div class="node-item" @click="addEchoTask">
              <el-icon><ChatLineRound /></el-icon>
              <div>
                <div>打印日志</div>
                <div class="node-desc">输出文本到控制台</div>
              </div>
            </div>
          </div> -->

          <div class="category">
            <div class="category-title">子流水线</div>
            <div class="node-item" @click="addBuildTask">
              <el-icon><Connection /></el-icon>
              <div>
                <div>子流水线</div>
                <div class="node-desc">触发其他Jenkins任务</div>
              </div>
            </div>
          </div>
        </div>
      </aside>

      <!-- 中间画布区域 -->
      <main class="canvas-area">
        <PipelineCanvas2
          :run="selectedRun"
          @select-stage="selectStage"
          @select-step="selectStep"
          @view-logs="openLogDrawer"
          @edit-stage="editStage"
        />
      </main>

      <!-- 右侧配置面板 -->
      <aside class="config-panel" :class="{ collapsed: !showConfigPanel }">
        <div class="config-header">
          <div class="config-title">
            <el-icon color="#1677ff"><Setting /></el-icon>
            <span>{{ configPanelTitle }}</span>
          </div>
          <el-button text @click="showConfigPanel = false">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
        <div class="config-body">
          <!-- 空状态 -->
          <div v-if="!selectedStageId && !selectedStepId" class="empty-state">
            <el-icon :size="48" color="#d9d9d9"><Pointer /></el-icon>
            <p>点击画布中的节点进行配置</p>
          </div>

          <!-- 步骤配置 -->
          <template v-if="selectedStepId && editingStep">
            <div class="step-config-section">
              <div class="form-section-title">
                <el-icon><Edit /></el-icon>
                步骤配置
              </div>
              <el-form label-width="100px" size="small">
                <el-form-item label="步骤类型">
                  <span>{{ getStepTypeName(editingStep.type) }}</span>
                </el-form-item>
                <el-form-item label="步骤名称">
                  <el-input v-model="editingStep.name" size="small" @change="saveStepConfig" />
                </el-form-item>
                <el-form-item label="执行前睡眠">
                  <el-switch v-model="editingStep.config.enableSleep" @change="saveStepConfig" />
                </el-form-item>
                <el-form-item v-if="editingStep.config.enableSleep" label="睡眠时间(秒)">
                  <el-input-number v-model="editingStep.config.sleepTime" :min="1" :max="3600" @change="saveStepConfig" />
                </el-form-item>
                <template v-if="editingStep.type === 'echo'">
                  <el-form-item label="打印内容">
                    <el-input v-model="editingStep.config.message" type="textarea" :rows="4" placeholder="输入要打印的日志内容" @change="saveStepConfig" />
                  </el-form-item>
                </template>
                <template v-else-if="editingStep.type === 'sh'">
                  <el-form-item label="Shell 命令">
                    <el-input v-model="editingStep.config.command" type="textarea" :rows="4" placeholder="输入 Shell 命令" @change="saveStepConfig" />
                  </el-form-item>
                </template>
                <template v-else-if="editingStep.type === 'git'">
                  <el-form-item label="仓库地址">
                    <el-input v-model="editingStep.config.url" placeholder="例如：https://github.com/example/repo.git" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="分支">
                    <el-input v-model="editingStep.config.branch" placeholder="例如：main" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="凭据 ID">
                    <el-input v-model="editingStep.config.credentialsId" placeholder="Jenkins 凭据 ID" @change="saveStepConfig" />
                  </el-form-item>
                </template>
                <template v-else-if="editingStep.type === 'build'">
                  <!-- <el-form-item label="子流水线名称">
                    <el-input v-model="editingStep.name" placeholder="子流水线显示名称" @change="saveStepConfig" />
                  </el-form-item> -->
                  <el-form-item label="Job 名称">
                    <el-input v-model="editingStep.config.job" placeholder="要触发的 Jenkins 任务名称" @change="saveStepConfig" />
                  </el-form-item>
                </template>
                <template v-else-if="editingStep.type === 'sonar'">
                  <el-form-item label="项目名称">
                    <el-input v-model="editingStep.config.projectName" placeholder="例如：我的项目" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="项目 key">
                    <el-input v-model="editingStep.config.projectKey" placeholder="例如：my-project-key" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="代码扫描路径">
                    <el-input v-model="editingStep.config.sources" placeholder="例如：git" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="二进制文件路径">
                    <el-input v-model="editingStep.config.binaryPath" placeholder="例如：./target" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="扫描工具cli位置">
                    <el-input v-model="editingStep.config.cliPath" placeholder="例如：/usr/local/sonar-scanner/bin/sonar-scanner" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="sonar访问地址">
                    <el-input v-model="editingStep.config.serverUrl" placeholder="例如：http://sonar.example.com" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="sonar token">
                    <el-input v-model="editingStep.config.token" placeholder="例如：xxx" @change="saveStepConfig" />
                  </el-form-item>
                </template>
                <template v-else-if="editingStep.type === 'jar'">
                  <el-form-item label="Maven 命令">
                    <el-input v-model="editingStep.config.command" placeholder="例如：mvn clean package" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="超时(分钟)">
                    <el-input-number v-model="editingStep.config.timeout" :min="1" :max="120" @change="saveStepConfig" />
                  </el-form-item>
                </template>
                <template v-else-if="editingStep.type === 'cpp'">
                  <el-form-item label="编译命令">
                    <el-input v-model="editingStep.config.command" placeholder="例如：cmake .. && make" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="超时(分钟)">
                    <el-input-number v-model="editingStep.config.timeout" :min="1" :max="120" @change="saveStepConfig" />
                  </el-form-item>
                </template>
                <template v-else-if="editingStep.type === 'go'">
                  <el-form-item label="Go 命令">
                    <el-input v-model="editingStep.config.command" placeholder="例如：go build" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="超时(分钟)">
                    <el-input-number v-model="editingStep.config.timeout" :min="1" :max="120" @change="saveStepConfig" />
                  </el-form-item>
                </template>
                <template v-else-if="editingStep.type === 'qt'">
                  <el-form-item label="Qt 命令">
                    <el-input v-model="editingStep.config.command" placeholder="例如：qmake && make" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="超时(分钟)">
                    <el-input-number v-model="editingStep.config.timeout" :min="1" :max="120" @change="saveStepConfig" />
                  </el-form-item>
                </template>
                <template v-else-if="editingStep.type === 'javaImage'">
                  <el-form-item label="Dockerfile 内容">
                    <el-input v-model="editingStep.config.dockerfileContent" type="textarea" :rows="6" placeholder="输入 Dockerfile 内容" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="镜像名称">
                    <el-input v-model="editingStep.config.imageName" placeholder="例如：myapp:latest" @change="saveStepConfig" />
                  </el-form-item>
                </template>
                <template v-else-if="editingStep.type === 'node'">
                  <el-form-item label="NPM 命令">
                    <el-input v-model="editingStep.config.command" placeholder="例如：npm install && npm run build" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="超时(分钟)">
                    <el-input-number v-model="editingStep.config.timeout" :min="1" :max="120" @change="saveStepConfig" />
                  </el-form-item>
                </template>
                <template v-else-if="editingStep.type === 'net'">
                  <el-form-item label=".NET 命令">
                    <el-input v-model="editingStep.config.command" placeholder="例如：dotnet build" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="超时(分钟)">
                    <el-input-number v-model="editingStep.config.timeout" :min="1" :max="120" @change="saveStepConfig" />
                  </el-form-item>
                </template>
                <template v-else-if="editingStep.type === 'python'">
                  <el-form-item label="Python 命令">
                    <el-input v-model="editingStep.config.command" placeholder="例如：python setup.py build" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="超时(分钟)">
                    <el-input-number v-model="editingStep.config.timeout" :min="1" :max="120" @change="saveStepConfig" />
                  </el-form-item>
                </template>
                <template v-else-if="editingStep.type === 'php'">
                  <el-form-item label="Composer 命令">
                    <el-input v-model="editingStep.config.command" placeholder="例如：composer install" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="超时(分钟)">
                    <el-input-number v-model="editingStep.config.timeout" :min="1" :max="120" @change="saveStepConfig" />
                  </el-form-item>
                </template>
                <template v-else-if="editingStep.type === 'pushImageDeploy' || editingStep.type === 'pushImageRelease'">
                  <el-form-item label="制品库地址">
                    <el-input v-model="editingStep.config.productRepoUrl" placeholder="${PRODUCT_REPO_URL}" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="用户令牌">
                    <el-input v-model="editingStep.config.userToken" type="password" placeholder="${USER_TOKEN}" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="文件地址">
                    <el-input v-model="editingStep.config.filePath" placeholder="${IMG_WITH_ID}" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="制品标识">
                    <el-select v-model="editingStep.config.productStatus" placeholder="选择制品库" @change="saveStepConfig">
                      <el-option label="开发库 (product_status_dev)" value="product_status_dev" />
                      <el-option label="受控库 (product_status_test)" value="product_status_test" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="制品库名称">
                    <el-input v-model="editingStep.config.productRepoName" placeholder="${PRODUCT_REPO_NAME}" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="制品标识名称">
                    <el-input v-model="editingStep.config.productFlagName" placeholder="${PRODUCT_FLAG_NAME}" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="服务别名">
                    <el-input v-model="editingStep.config.serviceAlias" placeholder="${SERVICE_ALIAS}" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="Jenkins任务名">
                    <el-input v-model="editingStep.config.jenkinsName" placeholder="${JOB_NAME}" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="构建编号">
                    <el-input v-model="editingStep.config.jenkinsNumber" placeholder="${BUILD_NUMBER}" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="GitLab仓库地址">
                    <el-input v-model="editingStep.config.gitlabUri" placeholder="${CODE_REPOSITORY_ID2_DISPLAY_URL}" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="GitLab分支">
                    <el-input v-model="editingStep.config.gitlabBranch" placeholder="${CURBRANCH}" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="安全代码注入">
                    <el-select v-model="editingStep.config.safeCode" placeholder="是否注入安全代码" @change="saveStepConfig">
                      <el-option label="否" :value="0" />
                      <el-option label="是" :value="1" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="覆盖上传">
                    <el-switch v-model="editingStep.config.coverUpload" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="CPU核心数">
                    <el-input v-model="editingStep.config.cpu" placeholder="${CPU_NUM}" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="内存(M)">
                    <el-input v-model="editingStep.config.memory" placeholder="${MEMORY}" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="框架架构">
                    <el-select v-model="editingStep.config.framework" placeholder="选择架构" @change="saveStepConfig">
                      <el-option label="X86_64" value="X86_64" />
                      <el-option label="ARM64" value="ARM64" />
                      <el-option label="所有架构" value="all" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="暴露端口列表">
                    <el-input v-model="editingStep.config.exposePortList" placeholder="${EXPOSE_PORT}" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="应用ID">
                    <el-input v-model="editingStep.config.appId" placeholder="${APP_ID}" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="应用编码">
                    <el-input v-model="editingStep.config.appCode" placeholder="${APP_CODE}" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="应用名称">
                    <el-input v-model="editingStep.config.appName" placeholder="${APP_NAME}" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="应用版本ID">
                    <el-input v-model="editingStep.config.appVersionId" placeholder="${APP_VERSION_ID}" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="应用版本名称">
                    <el-input v-model="editingStep.config.appVersionName" placeholder="${VERSION_NAME}" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="第三方名称">
                    <el-input v-model="editingStep.config.tripartiteName" placeholder="${TRIPARTITE_NAME}" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="超时时间(秒)">
                    <el-input-number v-model="editingStep.config.timeout" :min="1" :max="3600" @change="saveStepConfig" />
                  </el-form-item>
                </template>
                <template v-else-if="editingStep.type === 'apiTest'">
                  <el-form-item label="测试命令">
                    <el-input v-model="editingStep.config.command" placeholder="例如：newman run" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="测试脚本">
                    <el-input v-model="editingStep.config.collectionPath" placeholder="例如：./postman/collection.json" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="环境变量">
                    <el-input v-model="editingStep.config.environment" placeholder="例如：./postman/env.json" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="产出报告地址">
                    <el-input v-model="editingStep.config.reportName" placeholder="例如：report.html" @change="saveStepConfig" />
                  </el-form-item>
                </template>
                <template v-else-if="editingStep.type === 'mail'">
                  <el-form-item label="项目名称">
                    <el-input v-model="editingStep.config.projectName" placeholder="例如：我的项目" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="收件人">
                    <el-input v-model="editingStep.config.to" placeholder="例如：example@example.com" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="主题">
                    <el-input v-model="editingStep.config.subject" placeholder="例如：构建完成通知" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="发件人地址">
                    <el-input v-model="editingStep.config.from" placeholder="例如：sender@example.com" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="概要">
                    <el-input v-model="editingStep.config.changes" type="textarea" :rows="4" placeholder="输入邮件概要" @change="saveStepConfig" />
                  </el-form-item>
                </template>
                <template v-else-if="editingStep.type === 'input'">
                  <el-form-item label="审批人">
                    <el-input v-model="editingStep.config.approver" placeholder="例如：admin" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="审批消息">
                    <el-input v-model="editingStep.config.message" placeholder="例如：请确认是否继续" @change="saveStepConfig" />
                  </el-form-item>
                </template>
                <template v-else-if="editingStep.type === 'cloudDeploy'">
                  <el-form-item label="应用管理地址">
                    <el-input v-model="editingStep.config.appManageUrl" placeholder="应用管理地址" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="用户名">
                    <el-input v-model="editingStep.config.username" placeholder="例如：admin" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="密码">
                    <el-input v-model="editingStep.config.password" type="password" placeholder="登录密码" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="部署应用名称">
                    <el-input v-model="editingStep.config.deployAppName" placeholder="${DEPLOY_APP_NAME}" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="Namespace">
                    <el-input v-model="editingStep.config.namespace" placeholder="例如：k8s-workbenches-dev" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="制品标识名称">
                    <el-input v-model="editingStep.config.productFlagName" placeholder="${PRODUCT_FLAG_NAME}" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="暴露端口列表">
                    <el-input v-model="editingStep.config.exposePortList" type="textarea" :rows="3" placeholder='例如：[{"port":8080,"protocol":"TCP"}]' @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="CPU核心数">
                    <el-input v-model="editingStep.config.cpuNum" placeholder="${CPU_NUM}" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="内存(M)">
                    <el-input v-model="editingStep.config.memory" placeholder="${MEMORY}" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="代码仓库展示名">
                    <el-input v-model="editingStep.config.codeRepositoryId2Display" placeholder="代码仓库展示名" @change="saveStepConfig" />
                  </el-form-item>
                </template>
                <template v-else-if="editingStep.type === 'scpDeploy'">
                  <el-form-item label="节点名称">
                    <el-input v-model="editingStep.config.nodeName" placeholder="例如：node-1" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="远端执行命令">
                    <el-input v-model="editingStep.config.remoteCommand" placeholder="例如：bash deploy.sh" @change="saveStepConfig" />
                  </el-form-item>
                  <el-form-item label="传输文件地址">
                    <el-input v-model="editingStep.config.transferPath" placeholder="例如：/tmp/files" @change="saveStepConfig" />
                  </el-form-item>
                </template>
              </el-form>
              <el-button size="small" @click="selectedStepId = ''">关闭步骤配置</el-button>
            </div>
          </template>

          <!-- 流水线配置（选中 stage 且没有选中 step 时显示） -->
          <template v-if="selectedStageId && !selectedStepId">
            <el-form :model="pipelineConfig" label-width="100px" size="small">
              <div class="form-section">
                <div class="form-section-title">
                  <el-icon><Warning /></el-icon>
                  基础配置
                </div>
                <el-form-item label="流水线名称">
                  <el-input v-model="pipelineConfig.name" @blur="savePipelineConfig" />
                </el-form-item>
                <el-form-item label="描述">
                  <el-input v-model="pipelineConfig.description" type="textarea" :rows="2" @blur="savePipelineConfig" />
                </el-form-item>
              </div>

              <div class="form-section">
                <div class="form-section-title">
                  <el-icon><Setting /></el-icon>
                  Agent 配置
                </div>
                <el-form-item label="Agent 类型">
                  <el-select v-model="pipelineConfig.agent.type" @change="savePipelineConfig">
                    <el-option label="Any" value="any" />
                    <el-option label="None" value="none" />
                    <el-option label="Label" value="label" />
                    <el-option label="Docker" value="docker" />
                  </el-select>
                </el-form-item>
                <el-form-item v-if="pipelineConfig.agent.type === 'label'" label="节点标签">
                  <el-input v-model="pipelineConfig.agent.label" placeholder="例如：linux, nodejs" @blur="savePipelineConfig" />
                </el-form-item>
                <el-form-item v-if="pipelineConfig.agent.type === 'docker'" label="Docker 镜像">
                  <el-input v-model="pipelineConfig.agent.dockerImage" placeholder="例如：node:18-alpine" @blur="savePipelineConfig" />
                </el-form-item>
              </div>

              <div class="form-section">
                <div class="form-section-title">
                  <el-icon><List /></el-icon>
                  环境变量
                </div>
                <div class="params-table">
                  <table>
                    <thead>
                      <tr>
                        <th>变量名</th>
                        <th>变量值</th>
                        <th></th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(env, index) in pipelineConfig.environment" :key="index">
                        <td><el-input v-model="env.key" size="small" @blur="savePipelineConfig" /></td>
                        <td><el-input v-model="env.value" size="small" @blur="savePipelineConfig" /></td>
                        <td><el-button type="danger" size="small" text @click="removeEnv(index)"><el-icon><Delete /></el-icon></el-button></td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                <div class="add-param-btn" @click="addEnv">
                  <el-icon><Plus /></el-icon> 添加变量
                </div>
              </div>

              <!-- 运行历史 -->
              <div class="run-history-section">
                <div class="form-section-title">
                  <el-icon><Clock /></el-icon>
                  运行历史
                </div>
                <div v-if="store.pipelineRuns.length === 0" class="empty-runs">
                  <p>暂无运行记录</p>
                </div>
                <div v-else class="run-list">
                  <div
                    v-for="run in store.pipelineRuns"
                    :key="run.id"
                    class="run-item"
                    :class="{ active: selectedRun?.id === run.id }"
                    @click="viewRun(run)"
                  >
                    <div class="run-header">
                      <span class="run-number">#{{ run.buildNumber }}</span>
                      <el-tag :type="getRunStatusType(run.status)" size="small">
                        {{ getRunStatusText(run.status) }}
                      </el-tag>
                    </div>
                    <div class="run-meta">
                      <span>{{ run.triggeredBy }}</span>
                      <span>{{ formatTime(run.startTime) }}</span>
                    </div>
                    <div v-if="run.duration" class="run-duration">
                      {{ formatDuration(run.duration) }}
                    </div>
                  </div>
                </div>
              </div>
            </el-form>
          </template>
        </div>
      </aside>
    </div>

    <!-- Stage 编辑对话框 -->
    <el-dialog
      v-model="showStageDialog"
      :title="editingStage ? '编辑 Stage' : '添加 Stage'"
      width="700px"
      destroy-on-close>
      <StageEditor2
        :stage="editingStage"
        @save="saveStage"
        @close="showStageDialog = false"
      />
    </el-dialog>

    <!-- Jenkinsfile 预览 -->
    <el-dialog v-model="showJenkinsfile" title="Pipeline 配置预览" width="800px">
      <el-tabs v-model="previewTab">
        <el-tab-pane label="Jenkinsfile" name="jenkinsfile">
          <div class="jenkinsfile-preview">
            <pre><code>{{ jenkinsfileContent }}</code></pre>
          </div>
        </el-tab-pane>
        <el-tab-pane label="JSON" name="json">
          <div class="json-preview">
            <pre><code>{{ jsonContent }}</code></pre>
          </div>
        </el-tab-pane>
        <el-tab-pane label="YAML" name="yaml">
          <div class="yaml-preview">
            <pre><code>{{ yamlContent }}</code></pre>
          </div>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="showJenkinsfile = false">关闭</el-button>
        <el-button type="primary" @click="copyContent">
          <el-icon><DocumentCopy /></el-icon> 复制
        </el-button>
      </template>
    </el-dialog>

    <!-- 日志查看器 -->
    <el-drawer
      v-model="showLogDrawer"
      title="构建日志"
      size="60%"
      destroy-on-close
    >
      <LogViewer2 :run="selectedRun" @close="showLogDrawer = false" />
    </el-drawer>

    <!-- 构建阶段详情弹窗 -->
    <RunStagesDialog
      v-model="showRunStagesDialog"
      :job-name="selectedRunForStages?.jobName || ''"
      :build-number="selectedRunForStages?.buildNumber || 0"
      @close="selectedRunForStages = null"
    />

    <!-- 执行记录抽屉 -->
    <el-drawer
      v-model="showRunsDrawer"
      title="执行记录"
      size="540px"
      destroy-on-close
    >
      <div class="runs-drawer-content">
        <div v-if="store.pipelineRuns.length === 0" class="empty-runs-state">
          <el-empty description="暂无执行记录">
            <template #image>
              <el-icon :size="64" color="#d9d9d9"><Clock /></el-icon>
            </template>
          </el-empty>
        </div>
        <div v-else class="runs-timeline">
          <div
            v-for="run in store.pipelineRuns"
            :key="run.id"
            class="run-record-card"
            :class="{ selected: selectedRun?.id === run.id }"
            @click="selectRunAndShowLogs(run)"
          >
            <div class="run-record-top">
              <div class="run-record-build">
                <span class="run-record-number">#{{ run.buildNumber }}</span>
                <el-tag :type="getRunStatusType(run.status)" size="small" effect="dark" round>
                  {{ getRunStatusText(run.status) }}
                </el-tag>
              </div>
              <div class="run-record-actions">
                <el-button size="small" text @click.stop="openStagesDialog(run)" title="查看阶段">
                  <el-icon><List /></el-icon>
                </el-button>
                <el-button size="small" text @click.stop="downloadRunLogs(run)" title="下载日志">
                  <el-icon><Download /></el-icon>
                </el-button>
              </div>
            </div>
            <div class="run-record-meta">
              <div class="run-record-item">
                <el-icon><User /></el-icon>
                <span>{{ run.triggeredBy }}</span>
              </div>
              <div class="run-record-item">
                <el-icon><Clock /></el-icon>
                <span>{{ formatTime(run.startTime) }}</span>
              </div>
              <div v-if="run.duration" class="run-record-item">
                <el-icon><Timer /></el-icon>
                <span>{{ formatDuration(run.duration) }}</span>
              </div>
            </div>
            <div class="run-record-stages">
              <span
                v-for="stage in run.stages"
                :key="stage.stageId"
                class="stage-status-dot"
                :class="stage.status"
                :title="getStageNameById(stage.stageId) + ': ' + getRunStatusText(stage.status)"
              ></span>
              <span v-if="run.stages.length === 0" class="no-stages">暂无阶段信息</span>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { v4 as uuidv4 } from 'uuid'
import {
  Plus, VideoPlay, Document, Delete, Check, Close, ArrowLeft,
  Connection, Box, Cpu, Clock, Filter, Monitor, User,
  Setting, List, Warning, Pointer, Download, Timer, DocumentCopy, ChatLineRound, Upload, Message
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { Pipeline, PipelineStage, PipelineStep, PipelineRun, AgentConfig } from '@/types/pipeline'
import { usePipelineStore } from '@/stores/pipeline2'
import PipelineCanvas2 from '@/components/PipelineCanvas2.vue'
import StageEditor2 from '@/components/StageEditor2.vue'
import LogViewer2 from '@/components/LogViewer2.vue'
import RunStagesDialog from '@/components/RunStagesDialog.vue'
import axios from '@/utils/axios'
import { useRouter, useRoute } from 'vue-router'
import { pipelineToJenkinsfile } from '@/utils/jenkinsfile'

const store = usePipelineStore()
const router = useRouter()
const route = useRoute()

// 状态
const previewTab = ref('jenkinsfile')
const selectedPipelineId = ref('')
const selectedStageId = ref<string>('')
const isInitialLoad = ref(true)
const selectedStepId = ref<string>('')
const selectedRun = ref<PipelineRun | null>(null)
const selectedRunForStages = ref<{ jobName: string; buildNumber: number } | null>(null)
const showRunStagesDialog = ref(false)
const showStageDialog = ref(false)
const showJenkinsfile = ref(false)
const showLogDrawer = ref(false)
const showRunsDrawer = ref(false)
const showConfigPanel = ref(true)
const editingStage = ref<PipelineStage | null>(null)
const editingStep = ref<{ type: string; name: string; config: Record<string, any> } | null>(null)
const jenkinsfileContent = ref('')
const jsonContent = ref('')
const yamlContent = ref('')

const pipelineConfig = reactive({
  name: '',
  description: '',
  agent: { type: 'any' } as AgentConfig,
  environment: [] as { key: string; value: string }[]
})

const configPanelTitle = computed(() => selectedStageId.value ? '节点配置' : '流水线配置')

// 初始化流水线ID，等待store加载完成后再设置
onMounted(async () => {
  const routePipelineId = String(route.params.id) as string
  // 有效ID检查：不是undefined且不是空字符串
  const hasValidRouteId = routePipelineId && routePipelineId !== 'undefined' && routePipelineId.trim() !== ''

  // 从URL query参数获取流水线名称（新建流水线时传递）
  const pipelineName = route.query.name as string || '新流水线'

  // 保存本地创建的流水线（尚未持久化到后端），防止 fetchPipelines 覆盖后丢失
  const localPipelines = store.pipelines.filter(p =>
    typeof p.id === 'string' && (p.id.startsWith('new-') || /^[0-9a-f-]{36}$/i.test(p.id))
  )

  // 从后端加载流水线列表
  await store.fetchPipelines()

  // 恢复被后端数据覆盖的本地流水线
  for (const lp of localPipelines) {
    if (!store.pipelines.some(p => p.id === lp.id)) {
      store.pipelines.unshift(lp)
    }
  }

  // 设置当前流水线
  if (hasValidRouteId) {
    // 检查流水线是否存在
    const pipelineExists = store.pipelines.some(p => String(p.id) === routePipelineId)
    if (!pipelineExists) {
      // 流水线不存在于后端（新建未保存），创建一个空白流水线
      console.log('当前流水线不存在于后端数据中，创建空白流水线')
      const newPipeline: Pipeline = {
        id: routePipelineId,
        name: pipelineName,
        description: '',
        agent: { type: 'any' },
        environment: [],
        stages: [],
        post: {},
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      }
      store.pipelines.push(newPipeline)
      selectedPipelineId.value = routePipelineId
      store.currentPipelineId = routePipelineId
      return
    }
    selectedPipelineId.value = routePipelineId
    store.currentPipelineId = routePipelineId
    store.fetchPipelineContent(routePipelineId)
  } else {
    // 没有路由ID，先按名称查找已有流水线（可能来自 PipelineList 创建的本地流水线）
    const existingPipeline = store.pipelines.find(p => p.name === pipelineName)
    if (existingPipeline) {
      selectedPipelineId.value = existingPipeline.id
      store.currentPipelineId = existingPipeline.id
      return
    }
    // 未找到，创建一个新的空白流水线
    const newId = 'new-' + Date.now()
    const newPipeline: Pipeline = {
      id: newId,
      name: pipelineName,
      description: '',
      agent: { type: 'any' },
      environment: [],
      stages: [],
      post: {},
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    }
    store.pipelines.push(newPipeline)
    selectedPipelineId.value = newId
    store.currentPipelineId = newId
    // 更新路由（可选，让URL显示ID）
    // router.replace(`/pipelineDetail/${newId}`)
  }
})

// 监听路由参数变化，切换流水线
watch(() => route.params.id, (newId) => {
  if (newId) {
    const newIdStr = String(newId)
    // 有效ID检查：不是undefined且不是空字符串
    const hasValidId = newIdStr && newIdStr !== 'undefined' && newIdStr.trim() !== ''
    if (hasValidId) {
      const exists = store.pipelines.some(p => String(p.id) === newIdStr)
      if (exists) {
        // 设置selectedPipelineId和currentPipelineId并加载流水线内容
        isInitialLoad.value = false
        selectedPipelineId.value = newIdStr
        store.currentPipelineId = newIdStr
        store.fetchPipelineContent(newIdStr)
      }
    }
  }
})

// 处理流水线选择变化
function handlePipelineChange(id: string | number) {
  const idStr = String(id)
  isInitialLoad.value = false
  selectedPipelineId.value = idStr
  store.setCurrentPipeline(idStr)
  router.push(`/pipelineDetail/${idStr}`)
}

const hasSteps = computed(() => {
  if (!store.currentPipeline || store.currentPipeline.stages.length === 0) return false
  return store.currentPipeline.stages.some(stage =>
    stage.branches.some(branch => branch.steps.length > 0)
  )
})

const pipelineJson = computed(() => {
  if (!store.currentPipeline) return '{}'
  return JSON.stringify(store.currentPipeline, null, 2)
})

// 监听当前流水线变化
watch(() => store.currentPipeline, (pipeline) => {
  if (pipeline) {
    pipelineConfig.name = pipeline.name
    pipelineConfig.description = pipeline.description
    pipelineConfig.agent = { ...pipeline.agent }
    pipelineConfig.environment = [...pipeline.environment]
    // 只有从后端加载的流水线（有jenkinsfileContent）才获取执行记录
    // 新建的本地流水线没有jenkinsfileContent，不需要调用fetchRuns
    if (!isInitialLoad.value && pipeline.jenkinsfileContent) {
      store.fetchRuns(pipeline.id)
    }
    isInitialLoad.value = false
  }
}, { immediate: true })

function addPipelineStageDialog(){
  editingStage.value = null
  showStageDialog.value = true
}

function editStage(stage: PipelineStage) {
  editingStage.value = stage
  showStageDialog.value = true
}

async function fetchJsonAndYamlFromApi() {
  if (!store.currentPipeline) return
  console.log(store.currentPipeline)
  // const jenkinsfile = pipelineToJenkinsfile(store.currentPipeline as any)
  //const jenkinsfile = store.currentPipeline.jenkinsfileContent;
  const jenkinsfile = pipelineToJenkinsfile(store.currentPipeline)
  try {
    const res = await axios.post('/api/pipeline/getJsonAndYamlByJenkinsfile', jenkinsfile, {
      headers: { 'Content-Type': 'text/plain' }
    }) as any
    if (res.data.code === 200) {
      jenkinsfileContent.value = jenkinsfile
      jsonContent.value = res.data.data?.json || '{}'
      yamlContent.value = res.data.data?.yaml || ''
      console.log(jsonContent.value,5555555555555)
    }
  } catch (error) {
    console.error('获取 JSON/YAML 失败:', error)
  }
}

function openJenkinsfileDialog() {
  showJenkinsfile.value = true
  fetchJsonAndYamlFromApi()
}


function selectStage(stageId: string) {
  selectedStageId.value = stageId
  console.log(stageId)
  selectedStepId.value = ''
  showConfigPanel.value = true
}

function selectStep(stepId: string, step: { type: string; name: string; config: Record<string, any> }) {
  selectedStepId.value = stepId
  console.log(stepId)
  editingStep.value = { ...step, config: { ...step.config } }
  showConfigPanel.value = true
  console.log(editingStep.value)
}

function saveStepConfig() {
  if (!store.currentPipeline || !selectedStepId.value || !editingStep.value) return

  for (const stage of store.currentPipeline.stages) {
    for (const branch of stage.branches) {
      const stepIndex = branch.steps.findIndex(s => s.id === selectedStepId.value)
      if (stepIndex !== -1) {
        branch.steps[stepIndex] = {
          ...branch.steps[stepIndex],
          name: editingStep.value.name,
          config: { ...editingStep.value.config }
        }
        return
      }
    }
  }
}

function saveStage(stage: PipelineStage) {
  if (editingStage.value) {
    store.updateStage(editingStage.value.id, stage)
  } else {
    store.addStage(stage)
  }
  showStageDialog.value = false
  editingStage.value = null
}

function savePipelineConfig() {
  if (!store.currentPipeline) return
  store.updatePipeline(store.currentPipeline.id, {
    name: pipelineConfig.name,
    description: pipelineConfig.description,
    agent: { ...pipelineConfig.agent },
    environment: [...pipelineConfig.environment]
  })
}

function addEnv() {
  pipelineConfig.environment.push({ key: '', value: '' })
}

function removeEnv(index: number) {
  pipelineConfig.environment.splice(index, 1)
  savePipelineConfig()
}

function addDeployTask(type: string) {
  if (!store.currentPipeline) return
  const stepConfig: Record<string, { name: string; config: Record<string, any> }> = {
    cloud: { name: '云应用开发环境部署', config: { appManageUrl: '${APP_MANAGE_URL}', username: '', password: '', deployAppName: '', namespace: '', productFlagName: '', exposePortList: '', cpuNum: '', memory: '', codeRepositoryId2Display: '', exposePort: '' } },
    scp: { name: '远程传输文件部署', config: { nodeName: '', remoteCommand: '', transferPath: '' } }
  }
  const stepTypeMap: Record<string, PipelineStep['type']> = {
    cloud: 'cloudDeploy',
    scp: 'scpDeploy'
  }
  addQuickTask(stepTypeMap[type], stepConfig[type]?.name || '部署', stepConfig[type]?.config || {})
}

function addApproveTask(_type: string) {
  if (!store.currentPipeline) return
  addQuickTask('input', '人工审核', { approver: '', message: '' })
}

function addCompileTask(type: string) {
  if (!store.currentPipeline) return
  const stepConfig: Record<string, { name: string; config: Record<string, any> }> = {
    jar: { name: '构建jar', config: { command: 'mvn clean package', timeout: 30 } },
    cpp: { name: 'C++多平台构建', config: { command: 'cmake .. && make', timeout: 60 } },
    go: { name: 'Go多平台构建', config: { command: 'go build', timeout: 30 } },
    qt: { name: 'Qt多平台构建', config: { command: 'qmake && make', timeout: 60 } },
    javaImage: { name: 'JAVA生成镜像', config: { dockerfileContent: 'FROM openjdk:8\nCOPY target/app.jar /app/\nCMD java -jar /app/app.jar', imageName: 'myapp:latest', registryUrl: '' } },
    node: { name: 'Node编译', config: { command: 'npm install && npm run build', timeout: 30 } },
    net: { name: 'Net编译', config: { command: 'dotnet build', timeout: 30 } },
    python: { name: 'Python编译', config: { command: 'python setup.py build', timeout: 30 } },
    php: { name: 'PHP编译', config: { command: 'composer install', timeout: 30 } }
  }
  const stepTypeMap: Record<string, PipelineStep['type']> = {
    jar: 'jar',
    cpp: 'cpp',
    go: 'go',
    qt: 'qt',
    javaImage: 'javaImage',
    node: 'node',
    net: 'net',
    python: 'python',
    php: 'php'
  }
  addQuickTask(stepTypeMap[type], stepConfig[type]?.name || '编译', stepConfig[type]?.config || {})
}

function addPushTask(type: string) {
  if (!store.currentPipeline) return
  const stepConfig: Record<string, { name: string; config: Record<string, any> }> = {
    deploy: { name: '推送镜像到制品库（部署）', config: { productRepoUrl: '', userToken: '', filePath: '', productStatus: 'product_status_dev', productRepoName: '', productFlagName: '', serviceAlias: '', jenkinsName: '', jenkinsNumber: '', gitlabUri: '', gitlabBranch: '', safeCode: 0, coverUpload: true, cpu: '', memory: '', framework: 'X86_64', exposePortList: '', appId: '', appCode: '', appName: '', appVersionId: '', appVersionName: '', tripartiteName: '', timeout: 300 } },
    release: { name: '推送镜像到制品库（发版）', config: { productRepoUrl: '', userToken: '', filePath: '', productStatus: 'product_status_test', productRepoName: '', productFlagName: '', serviceAlias: '', jenkinsName: '', jenkinsNumber: '', gitlabUri: '', gitlabBranch: '', safeCode: 0, coverUpload: true, cpu: '', memory: '', framework: 'X86_64', exposePortList: '', appId: '', appCode: '', appName: '', appVersionId: '', appVersionName: '', tripartiteName: '', timeout: 300 } }
  }
  const stepTypeMap: Record<string, PipelineStep['type']> = {
    deploy: 'pushImageDeploy',
    release: 'pushImageRelease'
  }
  addQuickTask(stepTypeMap[type], stepConfig[type]?.name || '推送', stepConfig[type]?.config || {})
}

function addTestTask(type: string) {
  if (!store.currentPipeline) return
  const stepConfig: Record<string, { name: string; config: Record<string, any> }> = {
    api: { name: '接口测试', config: { command: 'newman run', collectionPath: '', environment: '', reportName: '' } },
    email: { name: '发送邮件', config: { projectName: '', from: '', to: '', subject: '', changes: '' } }
  }
  const stepTypeMap: Record<string, PipelineStep['type']> = {
    api: 'apiTest',
    email: 'mail'
  }
  addQuickTask(stepTypeMap[type], stepConfig[type]?.name || '测试', stepConfig[type]?.config || {})
}

function addScanTask(type: string) {
  if (!store.currentPipeline) return
  const stepConfig: Record<string, { name: string; config: Record<string, any> }> = {
    code: { name: '代码扫描', config: { projectKey: '', projectName: '', sources: '', binaryPath: '', cliPath: '', serverUrl: '', token: '' } }
  }
  const stepTypeMap: Record<string, PipelineStep['type']> = {
    code: 'sonar'
  }
  addQuickTask(stepTypeMap[type], stepConfig[type]?.name || '扫描', stepConfig[type]?.config || {})
}

function addEchoTask() {
  if (!store.currentPipeline) return
  addQuickTask('echo', '输出信息', { message: 'Hello World' })
}

function addBuildTask() {
  if (!store.currentPipeline) return
  addQuickTask('build', '子流水线', { job: '' })
}

function addGitTask() {
  if (!store.currentPipeline) return
  addQuickTask('git', 'Git 拉取', {
    url: 'https://github.com/example/repo.git',
    branch: 'main',
    credentialsId: ''
  })
}

// @ts-expect-error -- used in template
function addScriptTask() {
  if (!store.currentPipeline) return
  addQuickTask('sh', '执行脚本', { command: 'echo "Hello World"' })
}

function findStageIdByStepId(stepId: string): string | null {
  if (!stepId || !store.currentPipeline) return null
  for (const stage of store.currentPipeline.stages) {
    for (const branch of stage.branches) {
      if (branch.steps.some(s => s.id === stepId)) return stage.id
    }
  }
  return null
}

function addQuickTask(stepType: PipelineStep['type'], name: string, config: Record<string, any>) {
  if (!store.currentPipeline) return
  const pipeline = store.currentPipeline
  if (pipeline.stages.length === 0) {
    // Create a default stage
    const stageId = uuidv4()
    const branchId = uuidv4()
    store.addStage({
      id: stageId,
      name: '阶段 1',
      type: 'sequential',
      branches: [{
        id: branchId,
        name: '默认分支',
        steps: []
      }]
    })
    addStepToStage(stageId, branchId, stepType, name, config)
  } else {
    // 优先：选中组件 → 其所属阶段；其次：选中阶段；最后：最后一个阶段
    const stageId = selectedStageId.value || findStageIdByStepId(selectedStepId.value)
    const targetStage = stageId ? pipeline.stages.find(s => s.id === stageId) : null
    const stage = targetStage || pipeline.stages[pipeline.stages.length - 1]
    const branchId = stage.branches[0]?.id || uuidv4()
    addStepToStage(stage.id, branchId, stepType, name, config)
  }
  ElMessage.success(`已添加 ${name}`)
}

function addStepToStage(stageId: string, branchId: string, stepType: PipelineStep['type'], name: string, config: Record<string, any>) {
  const pipeline = store.currentPipeline
  if (!pipeline) return
  const stage = pipeline.stages.find(s => s.id === stageId)
  if (!stage) return

  const newStep: PipelineStep = {
    id: uuidv4(),
    type: stepType,
    name,
    config
  }

  const branch = stage.branches.find(b => b.id === branchId)
  if (branch) {
    branch.steps.push(newStep)
    // 触发更新 - 使用索引直接更新
    const stageIndex = pipeline.stages.findIndex(s => s.id === stageId)
    if (stageIndex !== -1) {
      pipeline.stages[stageIndex] = { ...pipeline.stages[stageIndex] }
    }
  }
}

async function validatePipeline() {
  if (!store.currentPipeline) return
  const stages = store.currentPipeline.stages
  let errors: string[] = []

  stages.forEach(stage => {
    const totalSteps = stage.branches.reduce((sum, b) => sum + b.steps.length, 0)
    if (totalSteps === 0) {
      errors.push(`阶段 "${stage.name}" 没有任务`)
    }
  })
  console.log(store.currentPipeline,6666)
  console.log(stages,11111111)
  console.log(store.pipelines,2222222)
  console.log(store.currentPipeline,5555)
  console.log(pipelineToJenkinsfile(store.currentPipeline),44444)
  const res = await axios.post(`/api/pipeline/validateJenkinsfile`, pipelineToJenkinsfile(store.currentPipeline),
    { headers: { 'Content-Type': 'text/plain' } }
  ) as any
  if (errors.length > 0) {
    ElMessage.error('验证失败: ' + errors[0])
  } else {
    if(res.data.code == 200){
      ElMessage.success('流水线配置验证通过')
    }
  }
}

//保存流水线
async function savePipeline() {
  if (!store.currentPipeline) return

  const jenkinsfile = (store.currentPipeline as any)?.jenkinsfileContent || store.jenkinsfile
  if (!jenkinsfile) {
    ElMessage.warning('请先生成 Jenkinsfile 内容')
    return
  }
  console.log('========== 发送给后端的数据 ==========')
  console.log('Job Name:', store.currentPipeline.name)
  console.log('Jenkinsfile:', jenkinsfile)
  console.log('=====================================')
  try {
    const res = await axios.post(`/api/pipeline/create?jobName=${store.currentPipeline.name}&describe=${store.currentPipeline.description}`,
    pipelineToJenkinsfile(store.currentPipeline),
    { headers: { 'Content-Type': 'text/plain' } }
  ) as any
    console.log('保存流水线返回:', res.data)
    if (res.data?.code === 200 || res.data?.code === 0) {
      ElMessage.success('保存成功')
      router.push('/pipelines')
    } else {
      ElMessage.error(res.data?.message || '保存失败')
    }
  } catch (error) {
    console.error('保存流水线失败:', error)
    ElMessage.error('保存失败')
  }
}

//启动流水线
async function runPipeline() {
  if (!store.currentPipeline) return

  const jenkinsfile = (store.currentPipeline as any)?.jenkinsfileContent || store.jenkinsfile
  if (!jenkinsfile) {
    ElMessage.warning('请先生成 Jenkinsfile 内容')
    return
  }

  console.log('========== 发送给后端的数据 ==========')
  console.log('Job Name:', store.currentPipeline.name)
  console.log('Jenkinsfile:', jenkinsfile)
  console.log('=====================================')

  try {
    // await store.savePipeline(store.currentPipeline.name, jenkinsfile)
    // ElMessage.success('流水线保存成功')
    // 运行功能待开发
    // ElMessage.info('运行功能开发中...')
    const res = await axios.post(`/api/pipeline/build?pipeLineId=${store.currentPipeline.id}`,{ headers: { 'Content-Type': 'text/plain' } }) as any
    console.log(res.data)
    if(res.data.code == 200){
      await store.fetchRuns(store.currentPipeline.id)
      store.startAutoRefresh(store.currentPipeline.id)
      ElMessage.success(res.data.message)
    }else{
      ElMessage.success(res.data.message)
    }
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

function copyContent() {
  const content = previewTab.value === 'jenkinsfile' ? store.jenkinsfile : pipelineJson.value
  navigator.clipboard.writeText(content).then(() => {
    ElMessage.success('已复制到剪贴板')
  })
}

function viewRun(run: PipelineRun) {
  selectedRun.value = run
  showLogDrawer.value = true
}

function getRunStatusType(status: string): string {
  const map: Record<string, string> = {
    success: 'success',
    failed: 'danger',
    running: 'primary',
    aborted: 'warning',
    unstable: 'warning',
    skipped: 'info',
    pending: 'info'
  }
  return map[status] || 'info'
}

function getRunStatusText(status: string): string {
  const map: Record<string, string> = {
    success: '成功',
    failed: '失败',
    running: '运行中',
    aborted: '已中止',
    unstable: '不稳定',
    skipped: '已跳过',
    pending: '构建中'
  }
  return map[status] || status
}

function formatTime(timeStr: string): string {
  return new Date(timeStr).toLocaleString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

function formatDuration(ms?: number): string {
  if (!ms) return '-'
  const seconds = Math.floor(ms / 1000)
  if (seconds < 60) return `${seconds}s`
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60
  return `${minutes}m ${remainingSeconds}s`
}

const stepTypeNames: Record<string, string> = {
  echo: '打印日志',
  sh: '执行脚本',
  git: 'Git 拉取',
  sonar: '代码扫描',
  jar: '构建jar',
  cpp: 'C++多平台构建',
  go: 'Go多平台构建',
  qt: 'Qt多平台构建',
  javaImage: 'JAVA生成镜像',
  node: 'Node编译',
  net: 'Net编译',
  python: 'Python编译',
  php: 'PHP编译',
  pushImageDeploy: '推送镜像到制品库',
  pushImageRelease: '推送镜像到制品库',
  apiTest: '接口测试',
  mail: '发送邮件',
  input: '审批卡点',
  cloudDeploy: '云应用开发环境部署',
  scpDeploy: '远程传输文件部署',
  custom: '自定义'
}

function getStepTypeName(type: string): string {
  return stepTypeNames[type] || type
}

function selectRunAndShowLogs(run: PipelineRun) {
  selectedRun.value = run
  showLogDrawer.value = true
}

function openStagesDialog(run: PipelineRun) {
  selectedRunForStages.value = {
    jobName: run.pipelineId,
    buildNumber: run.buildNumber
  }
  showRunStagesDialog.value = true
}

function openLogDrawer() {
  // 找到当前正在运行的 run 或最新的 run
  if (!selectedRun.value) {
    const runningRun = store.pipelineRuns.find(r => r.status === 'running')
    if (runningRun) {
      selectedRun.value = runningRun
    } else if (store.pipelineRuns.length > 0) {
      selectedRun.value = store.pipelineRuns[0]
    }
  }
  showLogDrawer.value = true
}

function getStageNameById(stageId: string): string {
  const stage = store.currentPipeline?.stages.find(s => s.id === stageId)
  return stage?.name || stageId
}

function goBack() {
  router.push('/pipelines')
}

function downloadRunLogs(run: PipelineRun) {
  const logs: string[] = []
  run.stages.forEach(stageRun => {
    const stageName = getStageNameById(stageRun.stageId)
    logs.push(`=== ${stageName} (${getRunStatusText(stageRun.status)}) ===`)
    if (stageRun.logs && stageRun.logs.length > 0) {
      logs.push(...stageRun.logs)
    } else {
      logs.push('(无日志)')
    }
    logs.push('')
  })

  const content = logs.join('\n')
  const blob = new Blob([content], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `build-${run.buildNumber}-logs.txt`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('日志已下载')
}
</script>

<style scoped>
.pipeline-editor-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f0f2f5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
}

/* Header */
.header {
  height: 64px;
  background: white;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  z-index: 100;
  position: relative;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-left .el-button {
  color: #8c8c8c;
  transition: color 0.2s;
}

.header-left .el-button:hover {
  color: #1677ff;
}

.logo {
  font-size: 20px;
  font-weight: 700;
  color: #1677ff;
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo .el-icon {
  background: linear-gradient(135deg, #1677ff 0%, #4096ff 100%);
  color: white;
  border-radius: 8px;
  padding: 6px;
}

.pipeline-selector :deep(.el-select) {
  --el-select-input-focus-border-color: #1677ff;
}

.pipeline-selector :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #d9d9d9;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-right .el-button {
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.2s;
}

.header-right .el-button:not(.is-text):hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(22, 119, 255, 0.2);
}

/* Main Container */
.main-container {
  display: flex;
  flex: 1;
  overflow: hidden;
  padding: 20px;
  gap: 20px;
  background: transparent;
}

/* Sidebar */
.sidebar {
  width: 280px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-header {
  padding: 18px 20px;
  border-bottom: 1px solid #f0f0f0;
  font-weight: 600;
  font-size: 15px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #fafafa 0%, #ffffff 100%);
}

.sidebar-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.category {
  margin-bottom: 20px;
}

.category-title {
  font-size: 11px;
  color: #8c8c8c;
  margin-bottom: 10px;
  padding-left: 8px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.8px;
}

.node-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  margin-bottom: 8px;
  background: linear-gradient(135deg, #fafafa 0%, #f5f5f5 100%);
  border: 1px solid transparent;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  font-size: 14px;
  color: #595959;
}

.node-item:hover {
  background: linear-gradient(135deg, #e6f4ff 0%, #bae0ff 100%);
  border-color: #1677ff;
  color: #1677ff;
  transform: translateX(4px);
  box-shadow: 0 4px 12px rgba(22, 119, 255, 0.15);
}

.node-item .el-icon {
  width: 22px;
  font-size: 18px;
  color: #1677ff;
  transition: color 0.2s;
}

.node-item:hover .el-icon {
  color: #1677ff;
}

.node-item .node-desc {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 2px;
  transition: color 0.2s;
}

.node-item:hover .node-desc {
  color: #4096ff;
}

/* Canvas Area */
.canvas-area {
  flex: 1;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  position: relative;
  overflow: hidden;
}

/* Config Panel */
.config-panel {
  width: 360px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: width 0.3s;
}

.config-panel.collapsed {
  width: 0;
}

.config-header {
  padding: 18px 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #fafafa 0%, #ffffff 100%);
}

.config-title {
  font-weight: 600;
  font-size: 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #1a1a1a;
}

.config-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #8c8c8c;
  background: linear-gradient(135deg, #fafafa 0%, #ffffff 100%);
  border-radius: 12px;
  margin: 20px;
}

.empty-state p {
  margin-top: 16px;
  color: #8c8c8c;
}

/* Form Sections */
.form-section {
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f5f5f5;
}

.form-section-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 16px;
  color: #1a1a1a;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* Params Table */
.params-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.params-table th, .params-table td {
  border: 1px solid #d9d9d9;
  padding: 8px;
  text-align: left;
}

.params-table th {
  background: #f0f2f5;
  font-weight: 500;
  color: #595959;
}

.add-param-btn {
  margin-top: 8px;
  color: #1677ff;
  font-size: 13px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.add-param-btn:hover {
  opacity: 0.8;
}

/* Run History Section */
.run-history-section {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

/* Step Config Section */
.step-config-section {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.step-config-section .el-form-item {
  margin-bottom: 12px;
}

.step-config-section .el-textarea {
  font-family: 'Consolas', 'Monaco', monospace;
}

.empty-runs {
  text-align: center;
  color: #8c8c8c;
  padding: 20px 0;
  font-size: 13px;
}

.run-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.run-item {
  padding: 12px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  background: white;
}

.run-item:hover {
  border-color: #1677ff;
  background: #e6f4ff;
}

.run-item.active {
  border-color: #1677ff;
  background: #e6f4ff;
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.1);
}

.run-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.run-number {
  font-weight: 600;
  font-size: 14px;
  color: #262626;
}

.run-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #8c8c8c;
}

.run-duration {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 4px;
}

/* Execution Records Drawer */
.runs-drawer-content {
  padding: 0 20px 24px;
}

.runs-timeline {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.run-record-card {
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.25s;
  background: linear-gradient(135deg, #fafafa 0%, #ffffff 100%);
}

.run-record-card:hover {
  border-color: #1677ff;
  box-shadow: 0 4px 16px rgba(22, 119, 255, 0.12);
  transform: translateY(-2px);
}

.run-record-card.selected {
  border-color: #1677ff;
  background: linear-gradient(135deg, #e6f4ff 0%, #bae0ff 100%);
  box-shadow: 0 4px 16px rgba(22, 119, 255, 0.2);
}

.run-record-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.run-record-build {
  display: flex;
  align-items: center;
  gap: 10px;
}

.run-record-number {
  font-weight: 700;
  font-size: 18px;
  color: #1a1a1a;
}

.run-record-actions {
  display: flex;
  gap: 6px;
  opacity: 0.5;
  transition: opacity 0.2s;
}

.run-record-card:hover .run-record-actions {
  opacity: 1;
}

.run-record-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.run-record-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #6b7280;
}

.run-record-item .el-icon {
  color: #9ca3af;
}

.no-stages {
  font-size: 12px;
  color: #9ca3af;
}

.run-record-stages {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.stage-status-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  display: inline-block;
  cursor: pointer;
  transition: transform 0.2s;
}

.stage-status-dot:hover {
  transform: scale(1.4);
}

.stage-status-dot.success { background: linear-gradient(135deg, #52c41a, #73d13d); }
.stage-status-dot.failed { background: linear-gradient(135deg, #ff4d4f, #ff7875); }
.stage-status-dot.running { background: linear-gradient(135deg, #1677ff, #4096ff); animation: pulse 1s infinite; }
.stage-status-dot.skipped { background: linear-gradient(135deg, #d9d9d9, #bfbfbf); }
.stage-status-dot.pending { background: linear-gradient(135deg, #faad14, #ffc53d); }

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(0.85); }
}

.empty-runs-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
}

.empty-runs-state .el-empty {
  padding: 40px;
  background: linear-gradient(135deg, #fafafa 0%, #f0f0f0 100%);
  border-radius: 16px;
}

/* Jenkinsfile Preview */
.jenkinsfile-preview,
.json-preview,
.yaml-preview {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 16px;
  border-radius: 6px;
  overflow-x: auto;
  max-height: 60vh;
}

.jenkinsfile-preview pre,
.json-preview pre,
.yaml-preview pre {
  margin: 0;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
}

/* Scrollbar */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>