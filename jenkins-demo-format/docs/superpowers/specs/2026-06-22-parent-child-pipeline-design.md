# 父子流水线功能设计方案

## 1. 需求概述

在现有流水线系统中新增父子流水线功能，支持：
- 创建流水线时可指定关联的子流水线
- 子流水线必须已存在才能建立关联
- 未指定子流水线则创建为普通流水线

## 2. 数据模型改动

### Pipeline 表新增字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `parent_id` | Long | 父流水线ID，NULL表示根流水线（普通流水线） |

## 3. 接口改动

### 3.1 修改 `POST /api/pipeline/create` 接口

**请求参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| jobName | String | 流水线名称 |
| describe | String | 描述 |
| jenkinsfile | String | Jenkinsfile内容 |
| childPipelineName | String | 子流水线名称（可选，为空则是普通流水线） |

**业务流程：**

```
1. 校验 Jenkinsfile 格式
2. 查询是否有可用的 Jenkins 服务
3. 检查 jobName 的流水线是否已存在
   - 存在 → 更新 Jenkinsfile
   - 不存在 → 新建流水线
4. 若传入了 childPipelineName：
   a. 查询子流水线是否已存在（按名称精确查找）
   b. 不存在 → 返回错误："新建流水线关联子流水线，子流水线必须存在"
   c. 存在 → 将子流水线的 parent_id 设置为当前流水线ID，建立关联
5. 返回结果
```

### 3.2 新增接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/pipeline/children/{parentId}` | GET | 获取某流水线下的所有子流水线 |
| `/api/pipeline/{pipelineId}` | GET | 获取流水线详情（含父/子流水线信息） |

## 4. 涉及的代码文件

| 文件 | 改动 |
|------|------|
| `Pipeline.java` | 新增 `parentId` 字段 |
| `PipelineController.java` | 修改 `create` 接口，新增 `childPipelineName` 参数 |
| `PipelineService.java` | 接口方法新增参数 |
| `PipelineServiceImpl.java` | 实现父子关联逻辑 |
| `PipelineMapper.java` | 无改动（继承 MyBatis-Plus） |

## 5. Jenkins 调用方式

父流水线 Jenkinsfile 中通过 `build` step 调用子流水线：

```groovy
pipeline {
    stages {
        stage('Build') {
            steps {
                echo 'Building...'
            }
        }
        stage('Child Pipeline') {
            steps {
                build job: 'child-pipeline-name', parameters: [
                    string(name: 'PARAM1', value: 'value1')
                ]
            }
        }
    }
}
```

子流水线作为独立 Job 在 Jenkins 中运行，父流水线通过 `build job:` 触发调用。

## 6. 错误处理

| 场景 | 错误信息 |
|------|----------|
| 子流水线不存在 | "新建流水线关联子流水线，子流水线必须存在" |
| 没有可用的 Jenkins 服务 | "没有可用的jenkins服务，请去新增jenkins服务" |
| 流水线不存在 | "流水线不存在!" |