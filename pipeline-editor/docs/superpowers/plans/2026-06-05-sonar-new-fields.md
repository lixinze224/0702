# 代码扫描步骤新增字段实现计划

**Goal:** 为代码扫描（sonar）步骤增加扫描工具cli位置、sonar访问地址、sonar token三个配置字段

**Architecture:** 在现有sonar步骤配置基础上新增三个表单项，jenkinsfile生成时使用这些字段生成对应的sonar-scanner命令参数

**Tech Stack:** Vue + Element UI + TypeScript

---

### Task 1: StageEditor2.vue 新增表单项

**Files:**
- Modify: `src/components/StageEditor2.vue:272-276`

- [ ] **Step 1: 在现有 sonar 表单区域内新增三个表单项**

在 `stepForm.type === 'sonar'` 的 template 中，保留现有 options 字段，在其上方新增三个字段：

```vue
<template v-if="stepForm.type === 'sonar'">
  <el-form-item label="扫描工具cli位置">
    <el-input v-model="stepForm.config.cliPath" placeholder="例如：/usr/local/sonar-scanner/bin/sonar-scanner" />
  </el-form-item>
  <el-form-item label="Sonar 访问地址">
    <el-input v-model="stepForm.config.serverUrl" placeholder="例如：https://sonar.example.com" />
  </el-form-item>
  <el-form-item label="Sonar Token">
    <el-input v-model="stepForm.config.token" placeholder="SonarQube 认证令牌" />
  </el-form-item>
  <el-form-item label="Sonar 选项">
    <el-input v-model="stepForm.config.options" placeholder="额外的 sonar-scanner 选项" />
  </el-form-item>
</template>
```

- [ ] **Step 2: Commit**

```bash
git add src/components/StageEditor2.vue
git commit -m "feat: sonar步骤新增cli位置、地址、token字段"
```

---

### Task 2: jenkinsfile.ts 更新生成逻辑

**Files:**
- Modify: `src/utils/jenkinsfile.ts:191-199`

- [ ] **Step 1: 更新 sonar case 生成逻辑，使用新字段**

将原有的 sonar case 块替换为：

```typescript
case 'sonar':
  lines.push(`${prefix}script {`)
  lines.push(`${prefix} def SONAR_PROJECT_NAME = '${cfg.projectName || ''}'`)
  lines.push(`${prefix} def SONAR_PROJECT_KEY = '${cfg.projectKey || ''}'`)
  lines.push(`${prefix} def SONAR_SOURCES_PATH = '${cfg.sources || ''}'`)
  lines.push(`${prefix} def SONAR_JAVA_BINARIES_PATH = '${cfg.binaryPath || ''}'`)
  lines.push(`${prefix} def SONAR_CLI_PATH = '${cfg.cliPath || 'sonar-scanner'}'`)
  lines.push(`${prefix} def SONAR_SERVER_URL = '${cfg.serverUrl || ''}'`)
  lines.push(`${prefix} def SONAR_TOKEN = '${cfg.token || ''}'`)
  lines.push(`${prefix}}`)
  // 构建 sonar-scanner 命令
  let sonarCmd = cfg.cliPath ? cfg.cliPath : 'sonar-scanner'
  if (cfg.serverUrl) {
    sonarCmd += ` -Dsonar.server.url=${cfg.serverUrl}`
  }
  if (cfg.token) {
    sonarCmd += ` -Dsonar.token=${cfg.token}`
  }
  sonarCmd += ` -DprojectKey=${cfg.projectKey || ''} -DprojectName=${cfg.projectName || ''}`
  if (cfg.sources) {
    sonarCmd += ` -Dsources=${cfg.sources}`
  }
  if (cfg.binaryPath) {
    sonarCmd += ` -Dsonar.java.binaries=${cfg.binaryPath}`
  }
  if (cfg.options) {
    sonarCmd += ` ${cfg.options}`
  }
  lines.push(`${prefix}sh '${sonarCmd}'`)
  break
```

- [ ] **Step 2: Commit**

```bash
git add src/utils/jenkinsfile.ts
git commit -m "feat: sonar步骤支持cliPath、serverUrl、token参数"
```

---

### Task 3: PipelineCanvas2.vue 更新默认配置模板

**Files:**
- Modify: `src/components/PipelineCanvas2.vue:590`

- [ ] **Step 1: 更新 sonar 步骤的默认配置**

找到 `code: { command: 'sonar-scanner', projectKey: '', projectName: '', sources: '', binaryPath: '' }`，添加新字段：

```typescript
code: { command: 'sonar-scanner', projectKey: '', projectName: '', sources: '', binaryPath: '', cliPath: '', serverUrl: '', token: '' }
```

- [ ] **Step 2: Commit**

```bash
git add src/components/PipelineCanvas2.vue
git commit -m "feat: sonar默认配置添加cliPath、serverUrl、token"
```

---

### Task 4: 验证

- [ ] **Step 1: 启动开发服务器验证**

```bash
npm run dev
```

- [ ] **Step 2: 在流水线编辑器中添加一个代码扫描步骤，填写新增字段，保存后检查 Jenkinsfile 生成结果**

打开浏览器访问 http://localhost:3000，添加 sonar 步骤，填写 cliPath、serverUrl、token，检查生成的 Jenkinsfile 是否包含新参数

---

**执行方式选择：**

1. **Subagent-Driven (推荐)** - 每任务派遣独立子代理，任务间review
2. **Inline Execution** - 当前会话内顺序执行

选择哪种方式？