# 父子流水线功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有流水线系统中新增父子流水线功能，支持创建流水线时关联已存在的子流水线

**Architecture:** 在 Pipeline 实体新增 parent_id 字段实现父子关联；修改 create 接口支持传入子流水线名称；新增查询子流水线列表和流水线详情接口

**Tech Stack:** Spring Boot + MyBatis-Plus + Jenkins API

---

## 涉及的文件

| 文件 | 改动 |
|------|------|
| `Pipeline.java` | 新增 `parentId` 字段 |
| `PipelineController.java` | 修改 create 接口 + 新增两个查询接口 |
| `PipelineService.java` | 接口方法新增参数 + 新增两个查询方法 |
| `PipelineServiceImpl.java` | 实现父子关联逻辑 + 实现新增的查询方法 |

---

## 任务清单

### Task 1: Pipeline 实体新增 parentId 字段

**Files:**
- Modify: `src/main/java/com/example/jenkinsdemoformat/entity/Pipeline.java`

- [ ] **Step 1: 添加 parentId 字段**

在 `Pipeline.java` 中 `stageNum` 字段后添加 `parentId` 字段：

```java
private Long parentId;
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/example/jenkinsdemoformat/entity/Pipeline.java
git commit -m "feat: Pipeline entity新增parentId字段用于父子流水线关联"
```

---

### Task 2: PipelineService 接口新增方法和参数

**Files:**
- Modify: `src/main/java/com/example/jenkinsdemoformat/service/PipelineService.java`

- [ ] **Step 1: 修改 create 方法签名**

将原有方法签名：
```java
Result<String> create(String jobName, String describe, String jenkinsfileContent);
```

修改为：
```java
Result<String> create(String jobName, String describe, String jenkinsfileContent, String childPipelineName);
```

- [ ] **Step 2: 新增 getChildren 方法**

在接口末尾添加：
```java
/**
 * 获取流水线下的所有子流水线
 *
 * @param parentId 父流水线ID
 * @return 子流水线列表
 */
List<Pipeline> getChildren(Long parentId);

/**
 * 获取流水线详情（含父/子流水线信息）
 *
 * @param pipelineId 流水线ID
 * @return 流水线详情，包含子流水线列表
 */
Result<Map<String, Object>> getPipelineDetail(Long pipelineId);
```

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/example/jenkinsdemoformat/service/PipelineService.java
git commit -m "feat: PipelineService接口新增父子流水线相关方法"
```

---

### Task 3: PipelineServiceImpl 实现父子关联逻辑

**Files:**
- Modify: `src/main/java/com/example/jenkinsdemoformat/service/impl/PipelineServiceImpl.java`

- [ ] **Step 1: 修改 create 方法实现**

将原方法签名和实现修改为：
```java
@Override
public Result<String> create(String jobName, String describe, String jenkinsfileContent, String childPipelineName) {
    try {
        Pipeline pipeline;
        //1.查询是否有可用的jenkins服务
        JenkinsConfig randomPipeLine = jenkinsConfigService.getRandomPipeLine();
        if (randomPipeLine == null) {
            return Result.error("没有可用的jenkins服务，请去新增jenkins服务");
        }

        pipeline = selectPipeLineIsExist(jobName);
        if (pipeline==null) {
            //开始新建流水线
            log.info("开始创建流水线");
            pipeline=new Pipeline();
            pipeline.setName(jobName);
            pipeline.setDescription(describe);
            pipeline.setDeleted(0L);
            pipeline.setJenkinsServerId(randomPipeLine.getId());
            pipeline.setStageNum((long) JenkinsfileStageCounter.countStages(jenkinsfileContent));
            jenkinsService.createPipeline(jobName, jenkinsfileContent, randomPipeLine);
            pipelineMapper.insert(pipeline);
            log.info("创建流水线完成");
        } else {
            log.info("开始更新流水线");
            Pipeline pipeline1 = pipelineMapper.selectOne(new LambdaQueryWrapper<Pipeline>().eq(Pipeline::getName, jobName));
            jenkinsService.updatePipelineJenkinsfile(jobName, jenkinsfileContent, jenkinsConfigService.getJenkinsServer(pipeline.getJenkinsServerId()));
            log.info("更新流水线");
            pipeline1.setStageNum((long) JenkinsfileStageCounter.countStages(jenkinsfileContent));
            pipelineMapper.updateById(pipeline1);
            pipeline = pipeline1;
        }

        // 2. 处理父子流水线关联
        if (childPipelineName != null && !childPipelineName.trim().isEmpty()) {
            log.info("开始处理父子流水线关联，子流水线名称: {}", childPipelineName);
            // 查询子流水线是否存在
            Pipeline childPipeline = selectPipeLineIsExist(childPipelineName);
            if (childPipeline == null) {
                log.error("子流水线不存在: {}", childPipelineName);
                return Result.error("新建流水线关联子流水线，子流水线必须存在");
            }
            // 将子流水线的 parent_id 设置为当前流水线ID
            childPipeline.setParentId(pipeline.getId());
            pipelineMapper.updateById(childPipeline);
            log.info("父子流水线关联成功，父流水线ID: {}, 子流水线ID: {}", pipeline.getId(), childPipeline.getId());
        }

        return Result.success("创建流水线成功");

    } catch (Exception e) {
        return Result.error(e.getMessage());
    }
}
```

- [ ] **Step 2: 新增 getChildren 方法实现**

在类末尾添加：
```java
@Override
public List<Pipeline> getChildren(Long parentId) {
    return pipelineMapper.selectList(new LambdaQueryWrapper<Pipeline>()
            .eq(Pipeline::getParentId, parentId)
            .eq(Pipeline::getDeleted, 0L));
}

@Override
public Result<Map<String, Object>> getPipelineDetail(Long pipelineId) {
    Pipeline pipeline = pipelineMapper.selectById(pipelineId);
    if (pipeline == null) {
        return Result.error("流水线不存在!");
    }
    JenkinsConfig jenkinsServer = jenkinsConfigService.getJenkinsServer(pipeline.getJenkinsServerId());
    if (jenkinsServer == null) {
        return Result.error("Jenkins服务器不存在!");
    }

    String configXml = jenkinsService.getPipelineConfig(pipeline.getName(), jenkinsServer);
    String jenkinsfile = extractJenkinsfileFromXml(configXml);
    String json = jenkinsService.jenkinsfileToJson(convertToMultipart(jenkinsfile));
    String yaml = jenkinsService.jenkinsfileToYaml(convertToMultipart(jenkinsfile));

    // 获取子流水线列表
    List<Pipeline> children = getChildren(pipelineId);

    Map<String, Object> result = new HashMap<>();
    result.put("pipeline", pipeline);
    result.put("jenkinsfile", jenkinsfile);
    result.put("json", extractJsonFromResponse(json));
    result.put("yaml", yaml);
    result.put("children", children);

    return Result.success(result);
}
```

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/example/jenkinsdemoformat/service/impl/PipelineServiceImpl.java
git commit -m "feat: 实现父子流水线关联逻辑"
```

---

### Task 4: PipelineController 修改 create 接口和新增查询接口

**Files:**
- Modify: `src/main/java/com/example/jenkinsdemoformat/controller/PipelineController.java`

- [ ] **Step 1: 修改 create 方法**

将原方法：
```java
@PostMapping(value = "/create", consumes = MediaType.TEXT_PLAIN_VALUE)
@Operation(summary = "创建流水线", description = "创建流水线")
public Result<String> createPipeline(
        @Parameter(description = "Job name", required = true)
        @RequestParam("jobName") String jobName,
        @Parameter(description = "描述", required = true)
        @RequestParam("describe") String describe,
        @Parameter(description = "Jenkinsfile content", required = true)
        @RequestBody String jenkinsfile) {
    return pipelineService.create(jobName,describe, jenkinsfile);
}
```

修改为：
```java
@PostMapping(value = "/create", consumes = MediaType.TEXT_PLAIN_VALUE)
@Operation(summary = "创建流水线", description = "创建流水线")
public Result<String> createPipeline(
        @Parameter(description = "Job name", required = true)
        @RequestParam("jobName") String jobName,
        @Parameter(description = "描述", required = true)
        @RequestParam("describe") String describe,
        @Parameter(description = "子流水线名称（可选）", required = false)
        @RequestParam(value = "childPipelineName", required = false) String childPipelineName,
        @Parameter(description = "Jenkinsfile content", required = true)
        @RequestBody String jenkinsfile) {
    return pipelineService.create(jobName, describe, jenkinsfile, childPipelineName);
}
```

- [ ] **Step 2: 新增获取子流水线列表接口**

在类中添加：
```java
@GetMapping("/children/{parentId}")
@Operation(summary = "获取父流水线下的所有子流水线", description = "获取父流水线下的所有子流水线")
public Result<List<Pipeline>> getChildren(@PathVariable Long parentId) {
    return Result.success(pipelineService.getChildren(parentId));
}
```

- [ ] **Step 3: 新增获取流水线详情接口**

在类中添加：
```java
@GetMapping("/{pipelineId}")
@Operation(summary = "获取流水线详情", description = "获取流水线详情（含子流水线信息）")
public Result<Map<String, Object>> getPipelineDetail(@PathVariable Long pipelineId) {
    return pipelineService.getPipelineDetail(pipelineId);
}
```

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/example/jenkinsdemoformat/controller/PipelineController.java
git commit -m "feat: PipelineController新增父子流水线相关接口"
```

---

## 任务依赖关系

Task 1 → Task 2 → Task 3 → Task 4（顺序执行）

---

## 验证步骤

完成所有任务后，验证：

1. 启动应用，确保无编译错误
2. 创建一条普通流水线（不传 childPipelineName）→ 成功
3. 创建一条流水线作为子流水线
4. 创建另一条流水线并传入 childPipelineName → 成功关联
5. 调用 GET /api/pipeline/children/{parentId} → 返回子流水线列表
6. 调用 GET /api/pipeline/{pipelineId} → 返回流水线详情含子流水线信息