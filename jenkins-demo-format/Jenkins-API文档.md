# Jenkins Log Demo 接口文档

## 基础信息

- **Base URL**: `http://localhost:9999/jenkins`
- **认证方式**: Jenkins Basic Auth + CSRF Crumb（通过配置 `jenkins.username` 和 `jenkins.token`）
- **响应格式**: JSON

---

## 1. 获取构建历史列表

### 请求

```
GET /jenkinslog/builds
```

### 参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| jobName | String | 是 | 流水线名称 |
| depth | Integer | 否 | 返回记录数量，不传则查询所有记录 |

### 响应示例

```json
[
  {
    "number": 11,
    "result": "SUCCESS",
    "timestamp": "2026-05-19 13:55:42",
    "duration": "5m 30s",
    "pipeline": "zml0512pt-1",
    "builder": "admin",
    "url": "http://jenkins.example.com/job/zml0512pt-1/11/",
    "stagesUrl": "/jenkins/stages?jobName=zml0512pt-1&buildNumber=11&fetchLog=false"
  }
]
```

### 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| number | Integer | 构建编号 |
| result | String | 构建结果：SUCCESS / FAILURE / UNSTABLE / ABORTED / IN_PROGRESS / QUEUED |
| timestamp | Date | 构建触发时间，格式：yyyy-MM-dd HH:mm:ss |
| duration | String | 构建耗时，人类可读格式，如：5m 30s |
| pipeline | String | 流水线名称 |
| builder | String | 构建人用户ID |
| url | String | Jenkins 中该构建的访问链接 |
| stagesUrl | String | 查询该构建阶段的 URL |

---

## 2. 分页查询构建历史

### 请求

```
GET /jenkinslog/builds/page
```

### 参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| jobName | String | 是 | 流水线名称 |
| page | Integer | 是 | 页码，从1开始 |
| pageSize | Integer | 是 | 每页记录数 |

### 响应示例

```json
{
  "list": [
    {
      "number": 11,
      "result": "SUCCESS",
      "timestamp": "2026-05-19 13:55:42",
      "duration": "5m 30s",
      "pipeline": "zml0512pt-1",
      "builder": "admin"
    }
  ],
  "total": 28,
  "page": 1,
  "pageSize": 10
}
```

### 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| list | Array | 构建记录列表 |
| total | Long | 总记录数 |
| page | Integer | 当前页码 |
| pageSize | Integer | 每页大小 |

---

## 3. 获取最新构建记录

### 请求

```
GET /jenkinslog/latest
```

### 参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| jobName | String | 是 | 流水线名称 |

### 响应示例

```json
{
  "number": 11,
  "result": "SUCCESS",
  "timestamp": "2026-05-19 13:55:42",
  "duration": "5m 30s",
  "pipeline": "zml0512pt-1",
  "builder": "admin"
}
```

---

## 4. 获取构建阶段列表

### 请求

```
GET /jenkinslog/stages
```

### 参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| jobName | String | 是 | 流水线名称 |
| buildNumber | Integer | 是 | 构建编号 |
| fetchLog | Boolean | 否 | 是否获取每个阶段的日志，默认 false |

### 响应示例

```json
{
  "buildNumber": 11,
  "jobName": "zml0512pt-1",
  "stages": [
    {
      "id": "6",
      "name": "jd1",
      "status": "SUCCESS",
      "duration": 32,
      "log": null,
      "logUrl": "/jenkins/stage/log?jobName=zml0512pt-1&buildNumber=11&stageId=6",
      "steps": [
        {"id": "7"},
        {"id": "8"}
      ]
    }
  ]
}
```

### 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| buildNumber | Integer | 构建编号 |
| jobName | String | 流水线名称 |
| stages | Array | 阶段列表 |
| stages[].id | String | 阶段节点 ID |
| stages[].name | String | 阶段名称 |
| stages[].status | String | 阶段状态 |
| stages[].duration | Long | 阶段耗时（毫秒） |
| stages[].log | String | 阶段日志（仅当 fetchLog=true 时有值） |
| stages[].logUrl | String | 查询阶段日志的 URL |
| stages[].steps | Array | 阶段下的步骤列表 |
| stages[].steps[].id | String | 步骤节点 ID |

### 状态值说明

| 状态 | 说明 |
|------|------|
| SUCCESS | 阶段执行成功 |
| FAILURE | 阶段执行失败 |
| UNSTABLE | 阶段完成但有警告 |
| ABORTED | 阶段被手动中止 |
| IN_PROGRESS | 阶段正在执行中 |
| NOT_EXECUTED | 阶段未执行（被跳过） |

---

## 5. 获取阶段日志

### 请求

```
GET /jenkinslog/stage/log
```

### 参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| jobName | String | 是 | 流水线名称 |
| buildNumber | Integer | 是 | 构建编号 |
| stageId | String | 是 | 阶段节点 ID，可通过 /stages 接口获取 |
| lines | Integer | 否 | 返回多少行，不传则返回全部 |
| offset | Integer | 否 | 从第几行开始（从0计），不传则从最后往前取 |

### 响应示例

```
hello

WARNING: Test failed: expected value mismatch
```

### 分页说明

| 参数组合 | 效果 |
|---------|------|
| 不传 lines/offset | 返回全部日志 |
| lines=100 | 返回最后 100 行 |
| lines=50&offset=0 | 获取第 1-50 行 |
| lines=50&offset=50 | 获取第 51-100 行 |

---

## 6. 获取步骤日志

### 请求

```
GET /jenkinslog/step/log
```

### 参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| jobName | String | 是 | 流水线名称 |
| buildNumber | Integer | 是 | 构建编号 |
| stepId | String | 是 | 步骤节点 ID，可通过 /stages 接口的 steps 获取 |

### 响应示例

```
hello
```

---

## 性能优化说明

### 缓存策略

| 缓存类型 | TTL | Key 格式 | 首次请求 | 缓存命中 |
|----------|-----|----------|----------|----------|
| Stage/Step 日志 | 5 分钟 | `jobName:buildNumber:stageId/stepId` | ~100-300ms | ~30ms |
| Crumb | 5 分钟 | - | - | - |

### 并行请求

- 获取构建列表时，使用 10 线程的线程池并行获取各构建详情

---

## 错误处理

| HTTP 状态码 | 说明 |
|-------------|------|
| 200 | 请求成功 |
| 400 | 参数缺失或格式错误 |
| 401 | 认证失败，检查 jenkins.username 和 jenkins.token 配置 |
| 500 | 服务器内部错误，可能是 Jenkins API 不可用 |

---

## 配置项

在 `application.yml` 或环境变量中配置：

```yaml
jenkins:
  url: http://jenkins.example.com
  username: your_username
  token: your_api_token
```

---

## 使用示例

### 1. 查询构建列表

```bash
curl "http://localhost:9999/jenkinslog/builds?jobName=zml0512pt-1&depth=5"
```

### 2. 分页查询

```bash
curl "http://localhost:9999/jenkinslog/builds/page?jobName=zml0512pt-1&page=1&pageSize=10"
```

### 3. 获取最新构建

```bash
curl "http://localhost:9999/jenkinslog/latest?jobName=zml0512pt-1"
```

### 4. 查询构建阶段

```bash
# 只获取阶段列表
curl "http://localhost:9999/jenkinslog/stages?jobName=zml0512pt-1&buildNumber=11"

# 同时获取阶段日志
curl "http://localhost:9999/jenkinslog/stages?jobName=zml0512pt-1&buildNumber=11&fetchLog=true"
```

### 5. 查询阶段日志

```bash
# 获取全部日志
curl "http://localhost:9999/jenkinslog/stage/log?jobName=zml0512pt-1&buildNumber=11&stageId=29"

# 获取最后 100 行
curl "http://localhost:9999/jenkinslog/stage/log?jobName=zml0512pt-1&buildNumber=11&stageId=29&lines=100"

# 分页获取
curl "http://localhost:9999/jenkinslog/stage/log?jobName=zml0512pt-1&buildNumber=11&stageId=29&lines=50&offset=0"
```

### 6. 查询步骤日志

```bash
curl "http://localhost:9999/jenkinslog/step/log?jobName=zml0512pt-1&buildNumber=11&stepId=29"
```