# Pipeline Format Converter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create `PipelineFormatConverter` utility class in `util` package supporting 6 conversion directions between Jenkinsfile, JSON, and YAML formats.

**Architecture:** All conversions flow through YAML as intermediate format using pipeline-as-yaml plugin for Jenkinsfile handling and jackson for JSON/YAML handling.

**Tech Stack:** pipeline-as-yaml, jackson-databind, jackson-dataformat-yaml

---

## File Structure

- Create: `src/main/java/com/example/jenkinsdemoformat/util/PipelineFormatConverter.java` — main converter class with all 6 conversion methods and main method for testing

---

## Implementation Steps

### Task 1: Create PipelineFormatConverter class

**Files:**
- Create: `src/main/java/com/example/jenkinsdemoformat/util/PipelineFormatConverter.java`

- [ ] **Step 1: Write PipelineFormatConverter class**

```java
package com.example.jenkinsdemoformat.util;

import io.jenkins.plugins.pipelineasyaml.PipelineAsYaml;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PipelineFormatConverter {

    private static final Logger logger = LoggerFactory.getLogger(PipelineFormatConverter.class);
    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    // Jenkinsfile -> YAML
    public static String jenkinsfileToYaml(String jenkinsfile) {
        try {
            Object yaml = PipelineAsYaml.getObjectFrom(jenkinsfile);
            return yamlMapper.writeValueAsString(yaml);
        } catch (Exception e) {
            logger.error("Failed to convert Jenkinsfile to YAML: {}", e.getMessage());
            return null;
        }
    }

    // YAML -> Jenkinsfile
    public static String yamlToJenkinsfile(String yaml) {
        try {
            return PipelineAsYaml.getJenkinsfileFrom(yaml);
        } catch (Exception e) {
            logger.error("Failed to convert YAML to Jenkinsfile: {}", e.getMessage());
            return null;
        }
    }

    // YAML -> JSON
    public static String yamlToJson(String yaml) {
        try {
            Object obj = yamlMapper.readValue(yaml, Object.class);
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            logger.error("Failed to convert YAML to JSON: {}", e.getMessage());
            return null;
        }
    }

    // JSON -> YAML
    public static String jsonToYaml(String json) {
        try {
            Object obj = jsonMapper.readValue(json, Object.class);
            return yamlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            logger.error("Failed to convert JSON to YAML: {}", e.getMessage());
            return null;
        }
    }

    // Jenkinsfile -> JSON (via YAML)
    public static String jenkinsfileToJson(String jenkinsfile) {
        String yaml = jenkinsfileToYaml(jenkinsfile);
        if (yaml == null) return null;
        return yamlToJson(yaml);
    }

    // JSON -> Jenkinsfile (via YAML)
    public static String jsonToJenkinsfile(String json) {
        String yaml = jsonToYaml(json);
        if (yaml == null) return null;
        return yamlToJenkinsfile(yaml);
    }

    public static void main(String[] args) {
        String sampleJenkinsfile = "pipeline {\n" +
            "    agent any\n" +
            "    stages {\n" +
            "        stage('Build') {\n" +
            "            steps {\n" +
            "                echo 'Building...'\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}";

        System.out.println("=== Jenkinsfile -> YAML ===");
        String yaml = jenkinsfileToYaml(sampleJenkinsfile);
        System.out.println(yaml);

        System.out.println("\n=== YAML -> Jenkinsfile ===");
        String roundTripJenkinsfile = yamlToJenkinsfile(yaml);
        System.out.println(roundTripJenkinsfile);

        System.out.println("\n=== YAML -> JSON ===");
        String json = yamlToJson(yaml);
        System.out.println(json);

        System.out.println("\n=== JSON -> YAML ===");
        String roundTripYaml = jsonToYaml(json);
        System.out.println(roundTripYaml);

        System.out.println("\n=== Jenkinsfile -> JSON ===");
        String json2 = jenkinsfileToJson(sampleJenkinsfile);
        System.out.println(json2);

        System.out.println("\n=== JSON -> Jenkinsfile ===");
        String roundTripJenkinsfile2 = jsonToJenkinsfile(json2);
        System.out.println(roundTripJenkinsfile2);
    }
}
```

- [ ] **Step 2: Compile to verify**

Run: `mvn compile -q`
Expected: SUCCESS

- [ ] **Step 3: Run main method to test**

Run: `mvn exec:java -Dexec.mainClass="com.example.jenkinsdemoformat.util.PipelineFormatConverter" -q`
Expected: Print all 6 conversion results

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/example/jenkinsdemoformat/util/PipelineFormatConverter.java
git commit -m "feat: add PipelineFormatConverter utility for Jenkinsfile/JSON/YAML conversion"
```

---

## Self-Review Checklist

- [ ] All 6 conversion methods implemented: jenkinsfileToYaml, yamlToJenkinsfile, yamlToJson, jsonToYaml, jenkinsfileToJson, jsonToJenkinsfile
- [ ] Main method with hardcoded test data
- [ ] Error handling returns null with logged error
- [ ] Uses pipeline-as-yaml for Jenkinsfile handling
- [ ] Uses jackson for JSON/YAML handling