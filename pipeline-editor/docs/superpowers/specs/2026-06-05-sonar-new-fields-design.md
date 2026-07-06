# 代码扫描步骤新增字段设计

## 背景
为代码扫描（sonar）步骤增加扫描工具cli位置、sonar访问地址、sonar token三个配置字段。

## 设计

### 新增字段
| 字段名 | 说明 | 示例 |
|--------|------|------|
| `cliPath` | 扫描工具cli位置 | `/usr/local/sonar-scanner/bin/sonar-scanner` |
| `serverUrl` | SonarQube 访问地址 | `https://sonar.example.com` |
| `token` | Sonar Token | `abc123def456` |

### 展示方式
所有字段在同一区域展示（统一区域），顺序为：
1. cli位置 (cliPath)
2. 地址 (serverUrl)
3. token
4. 现有字段 (projectKey, projectName, sources, binaryPath, options)

### 涉及文件
1. `StageEditor2.vue` - 新增三个表单项
2. `jenkinsfile.ts` - 生成 Jenkinsfile 时使用新字段
3. `PipelineCanvas2.vue` - 更新默认配置模板