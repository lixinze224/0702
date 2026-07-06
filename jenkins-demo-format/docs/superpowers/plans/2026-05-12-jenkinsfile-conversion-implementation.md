# Jenkinsfile/YAML/JSON 转换服务实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 Jenkinsfile、YAML、JSON 三种格式之间的互相转换功能，支持语义校验。

**Architecture:** 使用 Jenkins Core 原生解析 Jenkinsfile，通过 PipelineModel 中间模型实现双向转换。Service 层负责转换和校验，Controller 层提供 REST API。

**Tech Stack:** Spring Boot, Jenkins Core, snakeyaml, gson

---

## 文件结构

```
src/main/java/com/example/jenkinsdemoformat/
├── model/
│   ├── PipelineModel.java          # Pipeline 中间模型
│   ├── StageModel.java             # Stage 模型
│   ├── StepModel.java              # Step 模型
│   └── WhenConditionModel.java      # when 条件模型
├── dto/
│   ├── ConvertRequest.java         # 转换请求
│   ├── ConvertResponse.java        # 转换响应
│   ├── ValidateRequest.java        # 校验请求
│   ├── ValidateResponse.java       # 校验响应
│   └── ErrorDetail.java            # 错误详情
├── service/
│   ├── ConversionService.java      # 转换服务调度
│   ├── JenkinsfileParser.java      # Jenkinsfile 解析
│   ├── JenkinsfileGenerator.java   # Jenkinsfile 生成
│   ├── YamlConverter.java          # YAML 转换
│   ├── JsonConverter.java          # JSON 转换
│   └── ValidationService.java      # 校验服务
├── controller/
│   └── ConversionController.java   # 转换和校验 API
└── JenkinsDemoFormatApplication.java
```

---

## Task 1: 添加 Maven 依赖

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: 添加 jenkins-core、snakeyaml、gson 依赖到 pom.xml**

在 `properties` 中添加 jenkins 版本：
```xml
<properties>
    <java.version>21</java.version>
    <springdoc.version>2.5.0</springdoc.version>
    <jenkins.version>1.65</jenkins.version>
</properties>
```

在 `dependencies` 中添加：
```xml
<dependency>
    <groupId>org.jenkins-ci.main</groupId>
    <artifactId>jenkins-core</artifactId>
    <version>${jenkins.version}</version>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>org.yaml</groupId>
    <artifactId>snakeyaml</artifactId>
</dependency>
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
</dependency>
```

- [ ] **Step 2: 验证依赖配置正确**

Run: `mvn dependency:tree -Dscope=compile | grep -E "(jenkins|snakeyaml|gson)"`
Expected: 显示 jenkins-core、snakeyaml、gson 依赖

- [ ] **Step 3: 提交**

```bash
git add pom.xml
git commit -m "deps: add jenkins-core, snakeyaml, gson dependencies"
```

---

## Task 2: 创建数据模型

**Files:**
- Create: `src/main/java/com/example/jenkinsdemoformat/model/PipelineModel.java`
- Create: `src/main/java/com/example/jenkinsdemoformat/model/StageModel.java`
- Create: `src/main/java/com/example/jenkinsdemoformat/model/StepModel.java`
- Create: `src/main/java/com/example/jenkinsdemoformat/model/WhenConditionModel.java`

- [ ] **Step 1: 创建 PipelineModel.java**

```java
package com.example.jenkinsdemoformat.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class PipelineModel {
    private String agent;
    private List<StageModel> stages;
    private Map<String, String> environment;
    private List<WhenConditionModel> whenConditions;
    private Map<String, Object> options;
    private List<String> tools;
    private List<Map<String, String>> parameters;
    private String description;
}
```

- [ ] **Step 2: 创建 StageModel.java**

```java
package com.example.jenkinsdemoformat.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class StageModel {
    private String name;
    private List<StepModel> steps;
    private String agent;
    private Map<String, Object> when;
    private Map<String, Object> options;
    private List<Map<String, String>> parameters;
    private String description;
}
```

- [ ] **Step 3: 创建 StepModel.java**

```java
package com.example.jenkinsdemoformat.model;

import lombok.Data;
import java.util.Map;

@Data
public class StepModel {
    private String type;  // "script", "sh", "bat", "echo", etc.
    private String name;  // step name like "sh", "echo", "input"
    private String block; // for script block
    private Map<String, Object> args;  // step arguments
    private String when; // for when conditions
}
```

- [ ] **Step 4: 创建 WhenConditionModel.java**

```java
package com.example.jenkinsdemoformat.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class WhenConditionModel {
    private String condition;  // "branch", "expression", "not", "allOf", "anyOf"
    private List<WhenConditionModel> children;  // for composite conditions
    private Map<String, Object> params;  // condition parameters
}
```

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/example/jenkinsdemoformat/model/
git commit -m "model: add PipelineModel, StageModel, StepModel, WhenConditionModel"
```

---

## Task 3: 创建 DTO 类

**Files:**
- Create: `src/main/java/com/example/jenkinsdemoformat/dto/ConvertRequest.java`
- Create: `src/main/java/com/example/jenkinsdemoformat/dto/ConvertResponse.java`
- Create: `src/main/java/com/example/jenkinsdemoformat/dto/ValidateRequest.java`
- Create: `src/main/java/com/example/jenkinsdemoformat/dto/ValidateResponse.java`
- Create: `src/main/java/com/example/jenkinsdemoformat/dto/ErrorDetail.java`

- [ ] **Step 1: 创建 ConvertRequest.java**

```java
package com.example.jenkinsdemoformat.dto;

import lombok.Data;

@Data
public class ConvertRequest {
    private String content;
}
```

- [ ] **Step 2: 创建 ErrorDetail.java**

```java
package com.example.jenkinsdemoformat.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorDetail {
    private Integer line;
    private Integer column;
    private String message;
}
```

- [ ] **Step 3: 创建 ConvertResponse.java**

```java
package com.example.jenkinsdemoformat.dto;

import lombok.Data;
import java.util.List;

@Data
public class ConvertResponse {
    private boolean success;
    private String data;
    private String error;
    private String message;
    private List<ErrorDetail> errors;

    public static ConvertResponse success(String data) {
        ConvertResponse response = new ConvertResponse();
        response.setSuccess(true);
        response.setData(data);
        return response;
    }

    public static ConvertResponse error(String error, String message, List<ErrorDetail> errors) {
        ConvertResponse response = new ConvertResponse();
        response.setSuccess(false);
        response.setError(error);
        response.setMessage(message);
        response.setErrors(errors);
        return response;
    }
}
```

- [ ] **Step 4: 创建 ValidateRequest.java**

```java
package com.example.jenkinsdemoformat.dto;

import lombok.Data;

@Data
public class ValidateRequest {
    private String content;
}
```

- [ ] **Step 5: 创建 ValidateResponse.java**

```java
package com.example.jenkinsdemoformat.dto;

import lombok.Data;
import java.util.List;

@Data
public class ValidateResponse {
    private boolean success;
    private List<ErrorDetail> errors;

    public static ValidateResponse success() {
        ValidateResponse response = new ValidateResponse();
        response.setSuccess(true);
        return response;
    }

    public static ValidateResponse error(List<ErrorDetail> errors) {
        ValidateResponse response = new ValidateResponse();
        response.setSuccess(false);
        response.setErrors(errors);
        return response;
    }
}
```

- [ ] **Step 6: 提交**

```bash
git add src/main/java/com/example/jenkinsdemoformat/dto/
git commit -m "dto: add request/response DTOs for conversion and validation APIs"
```

---

## Task 4: 创建 JenkinsfileParser

**Files:**
- Create: `src/main/java/com/example/jenkinsdemoformat/service/JenkinsfileParser.java`

- [ ] **Step 1: 创建 JenkinsfileParser.java**

```java
package com.example.jenkinsdemoformat.service;

import com.example.jenkinsdemoformat.dto.ConvertResponse;
import com.example.jenkinsdemoformat.dto.ErrorDetail;
import com.example.jenkinsdemoformat.model.PipelineModel;
import com.example.jenkinsdemoformat.model.StageModel;
import com.example.jenkinsdemoformat.model.StepModel;
import com.example.jenkinsdemoformat.model.WhenConditionModel;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.Run;
import jenkins.model.Jenkins;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class JenkinsfileParser {

    public ConvertResponse<PipelineModel> parse(String jenkinsfileContent) {
        List<ErrorDetail> errors = new ArrayList<>();

        try {
            // Use Jenkins Core CpsFlowDefinition to parse
            CpsFlowDefinition cpsFlow = new CpsFlowDefinition(jenkinsfileContent, true);
            PipelineModel model = extractPipelineModel(jenkinsfileContent, errors);
            
            if (!errors.isEmpty()) {
                return ConvertResponse.error("SYNTAX_ERROR", "Jenkinsfile has syntax errors", errors);
            }
            
            return ConvertResponse.success(model);
        } catch (Exception e) {
            errors.add(new ErrorDetail(null, null, e.getMessage()));
            return ConvertResponse.error("PARSE_ERROR", "Failed to parse Jenkinsfile: " + e.getMessage(), errors);
        }
    }

    private PipelineModel extractPipelineModel(String jenkinsfileContent, List<ErrorDetail> errors) {
        PipelineModel model = new PipelineModel();
        model.setStages(new ArrayList<>());
        model.setEnvironment(new java.util.HashMap<>());
        
        // Parse Jenkinsfile using Groovy AST or regex-based extraction
        // Since Jenkinsfile is Groovy DSL, we need to extract structure
        String trimmed = jenkinsfileContent.trim();
        
        // Check for pipeline block
        if (!trimmed.contains("pipeline") && !trimmed.contains("stage")) {
            errors.add(new ErrorDetail(1, 1, "Invalid Jenkinsfile: missing pipeline or stage block"));
            return model;
        }
        
        // Extract agent
        if (trimmed.contains("agent")) {
            String agentBlock = extractBlock(trimmed, "agent");
            model.setAgent(agentBlock);
        }
        
        // Extract stages
        List<StageModel> stages = extractStages(trimmed, errors);
        model.setStages(stages);
        
        // Extract environment
        if (trimmed.contains("environment")) {
            Map<String, String> env = extractEnvironment(trimmed);
            model.setEnvironment(env);
        }
        
        return model;
    }

    private List<StageModel> extractStages(String content, List<ErrorDetail> errors) {
        List<StageModel> stages = new ArrayList<>();
        List<String> stageBlocks = extractBlockList(content, "stage");
        
        for (String stageBlock : stageBlocks) {
            StageModel stage = new StageModel();
            stage.setSteps(new ArrayList<>());
            
            // Extract stage name
            String name = extractQuotedValue(stageBlock, "name");
            if (name == null) {
                name = extractBracesValue(stageBlock, "name");
            }
            stage.setName(name != null ? name : "unnamed");
            
            // Extract steps
            List<StepModel> steps = extractSteps(stageBlock, errors);
            stage.setSteps(steps);
            
            stages.add(stage);
        }
        
        return stages;
    }

    private List<StepModel> extractSteps(String stageContent, List<ErrorDetail> errors) {
        List<StepModel> steps = new ArrayList<>();
        String stepsBlock = extractBlock(stageContent, "steps");
        
        if (stepsBlock == null) {
            return steps;
        }
        
        // Extract individual steps: sh, bat, echo, script, input, etc.
        String[] lines = stepsBlock.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            
            StepModel step = new StepModel();
            
            if (trimmed.startsWith("sh ") || trimmed.startsWith("sh\t")) {
                step.setName("sh");
                step.setType("shell");
                step.setBlock(trimmed.substring(3).trim());
            } else if (trimmed.startsWith("bat ") || trimmed.startsWith("bat\t")) {
                step.setName("bat");
                step.setType("windows");
                step.setBlock(trimmed.substring(4).trim());
            } else if (trimmed.startsWith("echo ")) {
                step.setName("echo");
                step.setType("print");
                step.setBlock(extractQuotedValue(trimmed, null));
            } else if (trimmed.startsWith("script")) {
                step.setName("script");
                step.setType("script");
                step.setBlock(extractBlock(trimmed, "script"));
            } else if (trimmed.startsWith("input ")) {
                step.setName("input");
                step.setType("input");
                step.setBlock(trimmed.substring(6).trim());
            }
            
            if (step.getName() != null) {
                steps.add(step);
            }
        }
        
        return steps;
    }

    private String extractBlock(String content, String blockName) {
        String pattern = blockName + "\\s*\\{";
        int start = -1;
        int braceCount = 0;
        boolean inBlock = false;
        
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (content.substring(i).startsWith(pattern) && !inBlock) {
                start = i + blockName.length();
                inBlock = true;
                continue;
            }
            if (inBlock) {
                if (c == '{') braceCount++;
                else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        return content.substring(start + 1, i).trim();
                    }
                }
            }
        }
        return null;
    }

    private List<String> extractBlockList(String content, String blockName) {
        List<String> blocks = new ArrayList<>();
        String pattern = blockName + "\\s*\\{";
        int i = 0;
        
        while (i < content.length()) {
            if (content.substring(i).startsWith(pattern)) {
                int start = i + blockName.length();
                int braceCount = 0;
                boolean inBlock = false;
                int blockStart = -1;
                
                for (int j = start; j < content.length(); j++) {
                    char c = content.charAt(j);
                    if (!inBlock && c == '{') {
                        inBlock = true;
                        blockStart = j + 1;
                        braceCount = 1;
                    } else if (inBlock) {
                        if (c == '{') braceCount++;
                        else if (c == '}') {
                            braceCount--;
                            if (braceCount == 0) {
                                blocks.add(content.substring(blockStart, j).trim());
                                i = j + 1;
                                break;
                            }
                        }
                    }
                }
                if (braceCount != 0) break;
            } else {
                i++;
            }
        }
        
        return blocks;
    }

    private String extractQuotedValue(String content, String key) {
        if (key != null) {
            int keyIndex = content.indexOf(key);
            if (keyIndex == -1) return null;
            content = content.substring(keyIndex + key.length());
        }
        
        content = content.trim();
        if (content.startsWith("'")) {
            int end = content.indexOf("'", 1);
            return end > 1 ? content.substring(1, end) : null;
        }
        if (content.startsWith("\"")) {
            int end = content.indexOf("\"", 1);
            return end > 1 ? content.substring(1, end) : null;
        }
        return null;
    }

    private String extractBracesValue(String content, String key) {
        int keyIndex = content.indexOf(key);
        if (keyIndex == -1) return null;
        
        int start = content.indexOf("{", keyIndex);
        if (start == -1) return null;
        
        int braceCount = 0;
        for (int i = start; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') braceCount++;
            else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    return content.substring(start + 1, i).trim();
                }
            }
        }
        return null;
    }

    private Map<String, String> extractEnvironment(String content) {
        Map<String, String> env = new java.util.HashMap<>();
        String envBlock = extractBlock(content, "environment");
        
        if (envBlock == null) return env;
        
        String[] lines = envBlock.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.contains("=")) {
                int eqIndex = line.indexOf("=");
                String key = line.substring(0, eqIndex).trim();
                String value = line.substring(eqIndex + 1).trim();
                value = value.replaceAll("^[\"']|[\"']$", "");
                env.put(key, value);
            }
        }
        
        return env;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/example/jenkinsdemoformat/service/JenkinsfileParser.java
git commit -m "service: add JenkinsfileParser using Jenkins Core CpsFlowDefinition"
```

---

## Task 5: 创建 JenkinsfileGenerator

**Files:**
- Create: `src/main/java/com/example/jenkinsdemoformat/service/JenkinsfileGenerator.java`

- [ ] **Step 1: 创建 JenkinsfileGenerator.java**

```java
package com.example.jenkinsdemoformat.service;

import com.example.jenkinsdemoformat.model.PipelineModel;
import com.example.jenkinsdemoformat.model.StageModel;
import com.example.jenkinsdemoformat.model.StepModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class JenkinsfileGenerator {

    public String generate(PipelineModel model) {
        StringBuilder sb = new StringBuilder();
        sb.append("pipeline {\n");
        
        // Generate agent
        if (model.getAgent() != null && !model.getAgent().isEmpty()) {
            sb.append("    agent ").append(model.getAgent()).append("\n");
        } else {
            sb.append("    agent any\n");
        }
        
        // Generate environment
        if (model.getEnvironment() != null && !model.getEnvironment().isEmpty()) {
            sb.append("    environment {\n");
            for (Map.Entry<String, String> entry : model.getEnvironment().entrySet()) {
                sb.append("        ").append(entry.getKey()).append(" = '").append(entry.getValue()).append("'\n");
            }
            sb.append("    }\n");
        }
        
        // Generate options
        if (model.getOptions() != null && !model.getOptions().isEmpty()) {
            sb.append("    options {\n");
            for (Map.Entry<String, Object> entry : model.getOptions().entrySet()) {
                sb.append("        ").append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
            }
            sb.append("    }\n");
        }
        
        // Generate stages
        if (model.getStages() != null && !model.getStages().isEmpty()) {
            sb.append("    stages {\n");
            for (StageModel stage : model.getStages()) {
                sb.append(generateStage(stage));
            }
            sb.append("    }\n");
        }
        
        sb.append("}\n");
        return sb.toString();
    }

    private String generateStage(StageModel stage) {
        StringBuilder sb = new StringBuilder();
        sb.append("        stage('").append(stage.getName()).append("') {\n");
        
        if (stage.getAgent() != null) {
            sb.append("            agent ").append(stage.getAgent()).append("\n");
        }
        
        if (stage.getWhen() != null && !stage.getWhen().isEmpty()) {
            sb.append("            when {\n");
            for (Map.Entry<String, Object> entry : stage.getWhen().entrySet()) {
                sb.append("                ").append(entry.getKey()).append(" {\n");
                sb.append("                    ").append(entry.getValue()).append("\n");
                sb.append("                }\n");
            }
            sb.append("            }\n");
        }
        
        if (stage.getSteps() != null && !stage.getSteps().isEmpty()) {
            sb.append("            steps {\n");
            for (StepModel step : stage.getSteps()) {
                sb.append(generateStep(step));
            }
            sb.append("            }\n");
        }
        
        sb.append("        }\n");
        return sb.toString();
    }

    private String generateStep(StepModel step) {
        String indent = "                ";
        StringBuilder sb = new StringBuilder();
        
        switch (step.getName()) {
            case "sh":
                sb.append(indent).append("sh '''\n");
                sb.append(indent).append(step.getBlock()).append("\n");
                sb.append(indent).append("'''\n");
                break;
            case "bat":
                sb.append(indent).append("bat \"\n");
                sb.append(indent).append(step.getBlock()).append("\n");
                sb.append(indent).append("\"\n");
                break;
            case "echo":
                sb.append(indent).append("echo '").append(step.getBlock()).append("'\n");
                break;
            case "script":
                sb.append(indent).append("script {\n");
                sb.append(indent).append("    ").append(step.getBlock()).append("\n");
                sb.append(indent).append("}\n");
                break;
            case "input":
                sb.append(indent).append("input ").append(step.getBlock()).append("\n");
                break;
            default:
                if (step.getBlock() != null) {
                    sb.append(indent).append(step.getName()).append(" '").append(step.getBlock()).append("'\n");
                } else {
                    sb.append(indent).append(step.getName()).append("\n");
                }
        }
        
        return sb.toString();
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/example/jenkinsdemoformat/service/JenkinsfileGenerator.java
git commit -m "service: add JenkinsfileGenerator to generate Jenkinsfile from PipelineModel"
```

---

## Task 6: 创建 YamlConverter

**Files:**
- Create: `src/main/java/com/example/jenkinsdemoformat/service/YamlConverter.java`

- [ ] **Step 1: 创建 YamlConverter.java**

```java
package com.example.jenkinsdemoformat.service;

import com.example.jenkinsdemoformat.model.PipelineModel;
import com.example.jenkinsdemoformat.model.StageModel;
import com.example.jenkinsdemoformat.model.StepModel;
import com.example.jenkinsdemoformat.model.WhenConditionModel;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.ConstructorException;
import org.yaml.snakeyaml.parser.ParserException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class YamlConverter {

    private final Yaml yaml = new Yaml();

    public String toYaml(PipelineModel model) {
        Map<String, Object> yamlMap = new LinkedHashMap<>();
        
        // Agent
        if (model.getAgent() != null && !model.getAgent().isEmpty()) {
            yamlMap.put("agent", model.getAgent());
        } else {
            yamlMap.put("agent", "any");
        }
        
        // Environment
        if (model.getEnvironment() != null && !model.getEnvironment().isEmpty()) {
            yamlMap.put("environment", model.getEnvironment());
        }
        
        // Options
        if (model.getOptions() != null && !model.getOptions().isEmpty()) {
            yamlMap.put("options", model.getOptions());
        }
        
        // Stages
        if (model.getStages() != null && !model.getStages().isEmpty()) {
            List<Map<String, Object>> stagesList = new ArrayList<>();
            for (StageModel stage : model.getStages()) {
                Map<String, Object> stageMap = new LinkedHashMap<>();
                stageMap.put("name", stage.getName());
                
                if (stage.getAgent() != null) {
                    stageMap.put("agent", stage.getAgent());
                }
                
                if (stage.getWhen() != null && !stage.getWhen().isEmpty()) {
                    stageMap.put("when", stage.getWhen());
                }
                
                if (stage.getSteps() != null && !stage.getSteps().isEmpty()) {
                    List<Map<String, Object>> stepsList = new ArrayList<>();
                    for (StepModel step : stage.getSteps()) {
                        Map<String, Object> stepMap = new LinkedHashMap<>();
                        stepMap.put("_type", step.getName());
                        if (step.getBlock() != null) {
                            stepMap.put("_script", step.getBlock());
                        }
                        if (step.getArgs()() != null) {
                            stepMap.putAll(step.getArgs());
                        }
                        stepsList.add(stepMap);
                    }
                    stageMap.put("steps", stepsList);
                }
                
                stagesList.add(stageMap);
            }
            yamlMap.put("stages", stagesList);
        }
        
        return yaml.dump(yamlMap);
    }

    public PipelineModel fromYaml(String yamlContent) {
        PipelineModel model = new PipelineModel();
        model.setStages(new ArrayList<>());
        model.setEnvironment(new java.util.HashMap<>());
        
        try {
            Map<String, Object> yamlMap = yaml.load(yamlContent);
            if (yamlMap == null) {
                return model;
            }
            
            // Parse agent
            Object agent = yamlMap.get("agent");
            model.setAgent(agent != null ? agent.toString() : "any");
            
            // Parse environment
            Object envObj = yamlMap.get("environment");
            if (envObj instanceof Map) {
                Map<String, String> env = new java.util.HashMap<>();
                @SuppressWarnings("unchecked")
                Map<String, Object> envMap = (Map<String, Object>) envObj;
                for (Map.Entry<String, Object> entry : envMap.entrySet()) {
                    env.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : "");
                }
                model.setEnvironment(env);
            }
            
            // Parse stages
            Object stagesObj = yamlMap.get("stages");
            if (stagesObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> stagesList = (List<Map<String, Object>>) stagesObj;
                for (Map<String, Object> stageMap : stagesList) {
                    StageModel stage = parseStage(stageMap);
                    model.getStages().add(stage);
                }
            }
            
        } catch (ConstructorException | ParserException e) {
            throw new IllegalArgumentException("Invalid YAML format: " + e.getMessage());
        }
        
        return model;
    }

    private StageModel parseStage(Map<String, Object> stageMap) {
        StageModel stage = new StageModel();
        stage.setName(stageMap.get("name") != null ? stageMap.get("name").toString() : "unnamed");
        stage.setAgent(stageMap.get("agent") != null ? stageMap.get("agent").toString() : null);
        stage.setSteps(new ArrayList<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> whenMap = (Map<String, Object>) stageMap.get("when");
        if (whenMap != null) {
            stage.setWhen(whenMap);
        }
        
        Object stepsObj = stageMap.get("steps");
        if (stepsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> stepsList = (List<Map<String, Object>>) stepsObj;
            for (Map<String, Object> stepMap : stepsList) {
                StepModel step = parseStep(stepMap);
                stage.getSteps().add(step);
            }
        }
        
        return stage;
    }

    private StepModel parseStep(Map<String, Object> stepMap) {
        StepModel step = new StepModel();
        step.setName(stepMap.get("_type") != null ? stepMap.get("_type").toString() : "unknown");
        step.setBlock(stepMap.get("_script") != null ? stepMap.get("_script").toString() : null);
        
        // Copy other fields as args
        step.setArgs(new java.util.HashMap<>(stepMap));
        step.getArgs().remove("_type");
        step.getArgs().remove("_script");
        
        return step;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/example/jenkinsdemoformat/service/YamlConverter.java
git commit -m "service: add YamlConverter for YAML ↔ PipelineModel conversion"
```

---

## Task 7: 创建 JsonConverter

**Files:**
- Create: `src/main/java/com/example/jenkinsdemoformat/service/JsonConverter.java`

- [ ] **Step 1: 创建 JsonConverter.java**

```java
package com.example.jenkinsdemoformat.service;

import com.example.jenkinsdemoformat.model.PipelineModel;
import com.example.jenkinsdemoformat.model.StageModel;
import com.example.jenkinsdemoformat.model.StepModel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class JsonConverter {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final JsonParser jsonParser = new JsonParser();

    public String toJson(PipelineModel model) {
        JsonObject root = new JsonObject();
        
        // Agent
        if (model.getAgent() != null && !model.getAgent().isEmpty()) {
            root.addProperty("agent", model.getAgent());
        } else {
            root.addProperty("agent", "any");
        }
        
        // Environment
        if (model.getEnvironment() != null && !model.getEnvironment().isEmpty()) {
            JsonObject envObj = new JsonObject();
            for (Map.Entry<String, String> entry : model.getEnvironment().entrySet()) {
                envObj.addProperty(entry.getKey(), entry.getValue());
            }
            root.add("environment", envObj);
        }
        
        // Stages
        if (model.getStages() != null && !model.getStages().isEmpty()) {
            JsonArray stagesArray = new JsonArray();
            for (StageModel stage : model.getStages()) {
                stagesArray.add(stageToJson(stage));
            }
            root.add("stages", stagesArray);
        }
        
        return gson.toJson(root);
    }

    private JsonObject stageToJson(StageModel stage) {
        JsonObject stageObj = new JsonObject();
        stageObj.addProperty("name", stage.getName());
        
        if (stage.getAgent() != null) {
            stageObj.addProperty("agent", stage.getAgent());
        }
        
        if (stage.getWhen() != null && !stage.getWhen().isEmpty()) {
            JsonObject whenObj = new JsonObject();
            for (Map.Entry<String, Object> entry : stage.getWhen().entrySet()) {
                whenObj.addProperty(entry.getKey(), entry.getValue().toString());
            }
            stageObj.add("when", whenObj);
        }
        
        if (stage.getSteps() != null && !stage.getSteps().isEmpty()) {
            JsonArray stepsArray = new JsonArray();
            for (StepModel step : stage.getSteps()) {
                stepsArray.add(stepToJson(step));
            }
            stageObj.add("steps", stepsArray);
        }
        
        return stageObj;
    }

    private JsonObject stepToJson(StepModel step) {
        JsonObject stepObj = new JsonObject();
        stepObj.addProperty("_type", step.getName());
        
        if (step.getBlock() != null) {
            stepObj.addProperty("_script", step.getBlock());
        }
        
        if (step.getArgs() != null) {
            for (Map.Entry<String, Object> entry : step.getArgs().entrySet()) {
                stepObj.addProperty(entry.getKey(), entry.getValue().toString());
            }
        }
        
        return stepObj;
    }

    public PipelineModel fromJson(String jsonContent) {
        PipelineModel model = new PipelineModel();
        model.setStages(new ArrayList<>());
        model.setEnvironment(new HashMap<>());
        
        try {
            JsonObject root = jsonParser.parse(jsonContent).getAsJsonObject();
            
            // Parse agent
            if (root.has("agent")) {
                model.setAgent(root.get("agent").getAsString());
            } else {
                model.setAgent("any");
            }
            
            // Parse environment
            if (root.has("environment")) {
                JsonObject envObj = root.getAsJsonObject("environment");
                Map<String, String> env = new HashMap<>();
                for (Map.Entry<String, JsonElement> entry : envObj.entrySet()) {
                    env.put(entry.getKey(), entry.getValue().getAsString());
                }
                model.setEnvironment(env);
            }
            
            // Parse stages
            if (root.has("stages")) {
                JsonArray stagesArray = root.getAsJsonArray("stages");
                for (JsonElement stageElement : stagesArray) {
                    StageModel stage = stageFromJson(stageElement.getAsJsonObject());
                    model.getStages().add(stage);
                }
            }
            
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON format: " + e.getMessage());
        }
        
        return model;
    }

    private StageModel stageFromJson(JsonObject stageObj) {
        StageModel stage = new StageModel();
        stage.setName(stageObj.has("name") ? stageObj.get("name").getAsString() : "unnamed");
        stage.setAgent(stageObj.has("agent") ? stageObj.get("agent").getAsString() : null);
        stage.setSteps(new ArrayList<>());
        
        if (stageObj.has("when")) {
            JsonObject whenObj = stageObj.getAsJsonObject("when");
            Map<String, Object> whenMap = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : whenObj.entrySet()) {
                whenMap.put(entry.getKey(), entry.getValue().getAsString());
            }
            stage.setWhen(whenMap);
        }
        
        if (stageObj.has("steps")) {
            JsonArray stepsArray = stageObj.getAsJsonArray("steps");
            for (JsonElement stepElement : stepsArray) {
                StepModel step = stepFromJson(stepElement.getAsJsonObject());
                stage.getSteps().add(step);
            }
        }
        
        return stage;
    }

    private StepModel stepFromJson(JsonObject stepObj) {
        StepModel step = new StepModel();
        step.setName(stepObj.has("_type") ? stepObj.get("_type").getAsString() : "unknown");
        step.setBlock(stepObj.has("_script") ? stepObj.get("_script").getAsString() : null);
        
        Map<String, Object> args = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : stepObj.entrySet()) {
            String key = entry.getKey();
            if (!"_type".equals(key) && !"_script".equals(key)) {
                args.put(key, entry.getValue().getAsString());
            }
        }
        step.setArgs(args);
        
        return step;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/example/jenkinsdemoformat/service/JsonConverter.java
git commit -m "service: add JsonConverter for JSON ↔ PipelineModel conversion"
```

---

## Task 8: 创建 ConversionService

**Files:**
- Create: `src/main/java/com/example/jenkinsdemoformat/service/ConversionService.java`

- [ ] **Step 1: 创建 ConversionService.java**

```java
package com.example.jenkinsdemoformat.service;

import com.example.jenkinsdemoformat.dto.ConvertResponse;
import com.example.jenkinsdemoformat.model.PipelineModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConversionService {

    @Autowired
    private JenkinsfileParser jenkinsfileParser;

    @Autowired
    private JenkinsfileGenerator jenkinsfileGenerator;

    @Autowired
    private YamlConverter yamlConverter;

    @Autowired
    private JsonConverter jsonConverter;

    // Jenkinsfile → YAML
    public ConvertResponse<String> jenkinsfileToYaml(String jenkinsfileContent) {
        try {
            ConvertResponse<PipelineModel> parseResult = jenkinsfileParser.parse(jenkinsfileContent);
            if (!parseResult.isSuccess()) {
                return ConvertResponse.error(parseResult.getError(), parseResult.getMessage(), parseResult.getErrors());
            }
            String yamlContent = yamlConverter.toYaml(parseResult.getData());
            return ConvertResponse.success(yamlContent);
        } catch (Exception e) {
            return ConvertResponse.error("CONVERSION_ERROR", "Failed to convert Jenkinsfile to YAML: " + e.getMessage(), null);
        }
    }

    // Jenkinsfile → JSON
    public ConvertResponse<String> jenkinsfileToJson(String jenkinsfileContent) {
        try {
            ConvertResponse<PipelineModel> parseResult = jenkinsfileParser.parse(jenkinsfileContent);
            if (!parseResult.isSuccess()) {
                return ConvertResponse.error(parseResult.getError(), parseResult.getMessage(), parseResult.getErrors());
            }
            String jsonContent = jsonConverter.toJson(parseResult.getData());
            return ConvertResponse.success(jsonContent);
        } catch (Exception e) {
            return ConvertResponse.error("CONVERSION_ERROR", "Failed to convert Jenkinsfile to JSON: " + e.getMessage(), null);
        }
    }

    // YAML → Jenkinsfile
    public ConvertResponse<String> yamlToJenkinsfile(String yamlContent) {
        try {
            PipelineModel model = yamlConverter.fromYaml(yamlContent);
            String jenkinsfileContent = jenkinsfileGenerator.generate(model);
            return ConvertResponse.success(jenkinsfileContent);
        } catch (Exception e) {
            return ConvertResponse.error("CONVERSION_ERROR", "Failed to convert YAML to Jenkinsfile: " + e.getMessage(), null);
        }
    }

    // YAML → JSON
    public ConvertResponse<String> yamlToJson(String yamlContent) {
        try {
            PipelineModel model = yamlConverter.fromYaml(yamlContent);
            String jsonContent = jsonConverter.toJson(model);
            return ConvertResponse.success(jsonContent);
        } catch (Exception e) {
            return ConvertResponse.error("CONVERSION_ERROR", "Failed to convert YAML to JSON: " + e.getMessage(), null);
        }
    }

    // JSON → Jenkinsfile
    public ConvertResponse<String> jsonToJenkinsfile(String jsonContent) {
        try {
            PipelineModel model = jsonConverter.fromJson(jsonContent);
            String jenkinsfileContent = jenkinsfileGenerator.generate(model);
            return ConvertResponse.success(jenkinsfileContent);
        } catch (Exception e) {
            return ConvertResponse.error("CONVERSION_ERROR", "Failed to convert JSON to Jenkinsfile: " + e.getMessage(), null);
        }
    }

    // JSON → YAML
    public ConvertResponse<String> jsonToYaml(String jsonContent) {
        try {
            PipelineModel model = jsonConverter.fromJson(jsonContent);
            String yamlContent = yamlConverter.toYaml(model);
            return ConvertResponse.success(yamlContent);
        } catch (Exception e) {
            return ConvertResponse.error("CONVERSION_ERROR", "Failed to convert JSON to YAML: " + e.getMessage(), null);
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/example/jenkinsdemoformat/service/ConversionService.java
git commit -m "service: add ConversionService to orchestrate all conversion operations"
```

---

## Task 9: 创建 ValidationService

**Files:**
- Create: `src/main/java/com/example/jenkinsdemoformat/service/ValidationService.java`

- [ ] **Step 1: 创建 ValidationService.java**

```java
package com.example.jenkinsdemoformat.service;

import com.example.jenkinsdemoformat.dto.ConvertResponse;
import com.example.jenkinsdemoformat.dto.ErrorDetail;
import com.example.jenkinsdemoformat.dto.ValidateResponse;
import com.example.jenkinsdemoformat.model.PipelineModel;
import com.example.jenkinsdemoformat.model.StageModel;
import com.example.jenkinsdemoformat.model.StepModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ValidationService {

    @Autowired
    private JenkinsfileParser jenkinsfileParser;

    @Autowired
    private YamlConverter yamlConverter;

    @Autowired
    private JsonConverter jsonConverter;

    // Jenkinsfile 校验
    public ValidateResponse validateJenkinsfile(String content) {
        List<ErrorDetail> errors = new ArrayList<>();
        
        if (content == null || content.trim().isEmpty()) {
            errors.add(new ErrorDetail(1, 1, "Jenkinsfile content is empty"));
            return ValidateResponse.error(errors);
        }
        
        try {
            ConvertResponse<PipelineModel> result = jenkinsfileParser.parse(content);
            if (!result.isSuccess()) {
                return ValidateResponse.error(result.getErrors());
            }
            
            // 语义校验
            errors.addAll(validatePipelineModel语义(result.getData()));
            
            if (errors.isEmpty()) {
                return ValidateResponse.success();
            } else {
                return ValidateResponse.error(errors);
            }
        } catch (Exception e) {
            errors.add(new ErrorDetail(null, null, "Validation failed: " + e.getMessage()));
            return ValidateResponse.error(errors);
        }
    }

    // YAML 校验
    public ValidateResponse validateYaml(String content) {
        List<ErrorDetail> errors = new ArrayList<>();
        
        if (content == null || content.trim().isEmpty()) {
            errors.add(new ErrorDetail(1, 1, "YAML content is empty"));
            return ValidateResponse.error(errors);
        }
        
        try {
            PipelineModel model = yamlConverter.fromYaml(content);
            errors.addAll(validatePipelineModel语义(model));
            
            if (errors.isEmpty()) {
                return ValidateResponse.success();
            } else {
                return ValidateResponse.error(errors);
            }
        } catch (Exception e) {
            errors.add(new ErrorDetail(null, null, "YAML parse error: " + e.getMessage()));
            return ValidateResponse.error(errors);
        }
    }

    // JSON 校验
    public ValidateResponse validateJson(String content) {
        List<ErrorDetail> errors = new ArrayList<>();
        
        if (content == null || content.trim().isEmpty()) {
            errors.add(new ErrorDetail(1, 1, "JSON content is empty"));
            return ValidateResponse.error(errors);
        }
        
        try {
            PipelineModel model = jsonConverter.fromJson(content);
            errors.addAll(validatePipelineModel语义(model));
            
            if (errors.isEmpty()) {
                return ValidateResponse.success();
            } else {
                return ValidateResponse.error(errors);
            }
        } catch (Exception e) {
            errors.add(new ErrorDetail(null, null, "JSON parse error: " + e.getMessage()));
            return ValidateResponse.error(errors);
        }
    }

    // 语义校验
    private List<ErrorDetail> validatePipelineModel语义(PipelineModel model) {
        List<ErrorDetail> errors = new ArrayList<>();
        
        if (model.getStages() == null || model.getStages().isEmpty()) {
            errors.add(new ErrorDetail(null, null, "Pipeline must have at least one stage"));
        }
        
        int stageIndex = 1;
        for (StageModel stage : model.getStages()) {
            // 检查 stage 名称
            if (stage.getName() == null || stage.getName().trim().isEmpty()) {
                errors.add(new ErrorDetail(null, null, "Stage " + stageIndex + " is missing a name"));
            }
            
            // 检查 steps 是否在 stage 内
            if (stage.getSteps() == null || stage.getSteps().isEmpty()) {
                errors.add(new ErrorDetail(null, null, "Stage '" + stage.getName() + "' must have steps"));
            }
            
            // 校验每个 step
            if (stage.getSteps() != null) {
                for (StepModel step : stage.getSteps()) {
                    errors.addAll(validateStep(step, stage.getName()));
                }
            }
            
            stageIndex++;
        }
        
        return errors;
    }

    private List<ErrorDetail> validateStep(StepModel step, String stageName) {
        List<ErrorDetail> errors = new ArrayList<>();
        
        // 检查 step 名称
        if (step.getName() == null || step.getName().trim().isEmpty()) {
            errors.add(new ErrorDetail(null, null, "Step in stage '" + stageName + "' is missing a name"));
        }
        
        // 检查 agent 配置
        String agent = step.getName();
        if (agent != null && (agent.contains("kubernetes") || agent.contains("docker"))) {
            if (step.getArgs() == null || step.getArgs().isEmpty()) {
                errors.add(new ErrorDetail(null, null, "Agent '" + agent + "' in stage '" + stageName + "' is missing configuration"));
            }
        }
        
        return errors;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/example/jenkinsdemoformat/service/ValidationService.java
git commit -m "service: add ValidationService for syntax and semantic validation"
```

---

## Task 10: 创建 ConversionController

**Files:**
- Create: `src/main/java/com/example/jenkinsdemoformat/controller/ConversionController.java`

- [ ] **Step 1: 创建 ConversionController.java**

```java
package com.example.jenkinsdemoformat.controller;

import com.example.jenkinsdemoformat.dto.ConvertRequest;
import com.example.jenkinsdemoformat.dto.ConvertResponse;
import com.example.jenkinsdemoformat.dto.ValidateRequest;
import com.example.jenkinsdemoformat.dto.ValidateResponse;
import com.example.jenkinsdemoformat.service.ConversionService;
import com.example.jenkinsdemoformat.service.ValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/convert")
@Tag(name = "Conversion", description = "Jenkinsfile/YAML/JSON Conversion API")
public class ConversionController {

    @Autowired
    private ConversionService conversionService;

    @Autowired
    private ValidationService validationService;

    // Jenkinsfile → YAML
    @PostMapping("/jenkinsfile/to/yaml")
    @Operation(summary = "Convert Jenkinsfile to YAML")
    public ConvertResponse<String> jenkinsfileToYaml(@RequestBody ConvertRequest request) {
        return conversionService.jenkinsfileToYaml(request.getContent());
    }

    // Jenkinsfile → JSON
    @PostMapping("/jenkinsfile/to/json")
    @Operation(summary = "Convert Jenkinsfile to JSON")
    public ConvertResponse<String> jenkinsfileToJson(@RequestBody ConvertRequest request) {
        return conversionService.jenkinsfileToJson(request.getContent());
    }

    // YAML → Jenkinsfile
    @PostMapping("/yaml/to/jenkinsfile")
    @Operation(summary = "Convert YAML to Jenkinsfile")
    public ConvertResponse<String> yamlToJenkinsfile(@RequestBody ConvertRequest request) {
        return conversionService.yamlToJenkinsfile(request.getContent());
    }

    // YAML → JSON
    @PostMapping("/yaml/to/json")
    @Operation(summary = "Convert YAML to JSON")
    public ConvertResponse<String> yamlToJson(@RequestBody ConvertRequest request) {
        return conversionService.yamlToJson(request.getContent());
    }

    // JSON → Jenkinsfile
    @PostMapping("/json/to/jenkinsfile")
    @Operation(summary = "Convert JSON to Jenkinsfile")
    public ConvertResponse<String> jsonToJenkinsfile(@RequestBody ConvertRequest request) {
        return conversionService.jsonToJenkinsfile(request.getContent());
    }

    // JSON → YAML
    @PostMapping("/json/to/yaml")
    @Operation(summary = "Convert JSON to YAML")
    public ConvertResponse<String> jsonToYaml(@RequestBody ConvertRequest request) {
        return conversionService.jsonToYaml(request.getContent());
    }

    // Jenkinsfile 校验
    @PostMapping("/validate/jenkinsfile")
    @Operation(summary = "Validate Jenkinsfile")
    public ValidateResponse validateJenkinsfile(@RequestBody ValidateRequest request) {
        return validationService.validateJenkinsfile(request.getContent());
    }

    // YAML 校验
    @PostMapping("/validate/yaml")
    @Operation(summary = "Validate YAML")
    public ValidateResponse validateYaml(@RequestBody ValidateRequest request) {
        return validationService.validateYaml(request.getContent());
    }

    // JSON 校验
    @PostMapping("/validate/json")
    @Operation(summary = "Validate JSON")
    public ValidateResponse validateJson(@RequestBody ValidateRequest request) {
        return validationService.validateJson(request.getContent());
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/example/jenkinsdemoformat/controller/ConversionController.java
git commit -m "controller: add ConversionController with 6 conversion and 3 validation endpoints"
```

---

## Task 11: 编译验证

**Files:**
- Test: `pom.xml`, all created files

- [ ] **Step 1: 运行 Maven 编译**

Run: `mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 2: 如有编译错误，根据错误信息修复**

- [ ] **Step 3: 提交所有未提交更改**

```bash
git status
git add -A
git commit -m "chore: complete all implementation tasks"
```

---

## 实施检查清单

- [ ] Task 1: Maven 依赖
- [ ] Task 2: 数据模型
- [ ] Task 3: DTO 类
- [ ] Task 4: JenkinsfileParser
- [ ] Task 5: JenkinsfileGenerator
- [ ] Task 6: YamlConverter
- [ ] Task 7: JsonConverter
- [ ] Task 8: ConversionService
- [ ] Task 9: ValidationService
- [ ] Task 10: ConversionController
- [ ] Task 11: 编译验证
