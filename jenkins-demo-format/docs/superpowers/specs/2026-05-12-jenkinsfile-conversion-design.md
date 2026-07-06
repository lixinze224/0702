# Jenkinsfile/YAML/JSON 转换服务设计

## 概述

实现 Jenkinsfile、YAML、JSON 三种格式之间的互相转换功能，支持全面的语义校验。

## 技术选型

- **Jenkinsfile 解析**：使用 Jenkins Core 原生解析 (`jenkins-core`)
- **YAML 处理**：snakeyaml
- **JSON 处理**：gson
- **转换架构**：直接双向转换（Jenkinsfile ↔ PipelineModel 中间模型 ↔ YAML/JSON）

## 支持的转换方向

| 源格式 | 目标格式 |
|--------|----------|
| jenkinsfile | yaml |
| jenkinsfile | json |
| yaml | jenkinsfile |
| yaml | json |
| json | jenkinsfile |
| json | yaml |

## 语义校验

支持完整的 Jenkins Pipeline 语义校验：
- 步骤（steps）必须在 stage 内
- agent 配置有效性
- when 条件语法
- 必要参数存在性

## API 设计

### 转换接口

#### POST /convert/jenkinsfile/to/yaml
- Body: `{ "content": "pipeline { ... }" }`
- Response: `{ "success": true, "data": "yaml content" }`

#### POST /convert/jenkinsfile/to/json
- Body: `{ "content": "pipeline { ... }" }`
- Response: `{ "success": true, "data": "{ ... }" }`

#### POST /convert/yaml/to/jenkinsfile
- Body: `{ "content": "stages:\n  - name: Build..." }`
- Response: `{ "success": true, "data": "pipeline { ... }" }`

#### POST /convert/yaml/to/json
- Body: `{ "content": "stages:\n  - name: Build..." }`
- Response: `{ "success": true, "data": "{ ... }" }`

#### POST /convert/json/to/jenkinsfile
- Body: `{ "content": "{ \"stages\": [...] }" }`
- Response: `{ "success": true, "data": "pipeline { ... }" }`

#### POST /convert/json/to/yaml
- Body: `{ "content": "{ \"stages\": [...] }" }`
- Response: `{ "success": true, "data": "yaml content" }`

### 校验接口

#### POST /validate/jenkinsfile
- Body: `{ "content": "pipeline { ... }" }`
- Response: `{ "success": true, "errors": [] }`

#### POST /validate/yaml
- Body: `{ "content": "stages:\n  - name: Build..." }`
- Response: `{ "success": true, "errors": [] }`

#### POST /validate/json
- Body: `{ "content": "{ \"stages\": [...] }" }`
- Response: `{ "success": true, "errors": [] }`

## 错误响应

```json
{
  "success": false,
  "error": "SYNTAX_ERROR",
  "message": "Missing closing brace at line 3",
  "errors": [
    { "line": 3, "column": 10, "message": "Unclosed block" }
  ]
}
```

## Service 层设计

### ConversionService
- 调度各转换器

### JenkinsfileParser
- 使用 Jenkins Core 原生解析 Jenkinsfile DSL
- 输出 PipelineModel 中间模型

### JenkinsfileGenerator
- 从 PipelineModel 生成 Jenkinsfile 字符串

### YamlConverter
- YAML ↔ PipelineModel

### JsonConverter
- JSON ↔ PipelineModel

### ValidationService
- 语法校验
- 语义校验（步骤、参数、agent、when 条件等）

## 中间模型 PipelineModel

```java
public class PipelineModel {
    String agent;
    List<StageModel> stages;
    Map<String, Object> environment;
    List<WhenCondition> whenConditions;
    // ...
}
```

## 依赖

- spring-boot-starter-web
- jenkins-core (provided)
- snakeyaml
- gson

## 测试策略

使用 Swagger UI (`http://localhost:9999/swagger-ui.html`) 手动测试 Controller 层。
