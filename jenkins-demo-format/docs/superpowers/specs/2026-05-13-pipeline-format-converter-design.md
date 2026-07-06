# Pipeline Format Converter Design

## Overview

Create a utility class `PipelineFormatConverter` in `util` package that enables bidirectional conversion between Jenkinsfile, JSON, and YAML formats using Jenkins official plugins and jackson.

## Architecture

```
Jenkinsfile ←→ YAML ←→ JSON
```

All conversions flow through YAML as the intermediate format:
- Jenkinsfile ↔ YAML: using `pipeline-as-yaml` plugin
- YAML ↔ JSON: using jackson

## Class Details

**Package:** `com.example.jenkinsdemoformat.util`

**Class Name:** `PipelineFormatConverter`

**Methods:**

| Method | Description |
|--------|-------------|
| `jenkinsfileToYaml(String)` | Convert Jenkinsfile string to YAML string |
| `yamlToJenkinsfile(String)` | Convert YAML string to Jenkinsfile string |
| `jenkinsfileToJson(String)` | Convert Jenkinsfile string to JSON string |
| `jsonToJenkinsfile(String)` | Convert JSON string to Jenkinsfile string |
| `yamlToJson(String)` | Convert YAML string to JSON string |
| `jsonToYaml(String)` | Convert JSON string to YAML string |

**Error Handling:** On conversion failure, log the error and return `null`.

## Dependencies

- `pipeline-as-yaml` — Jenkins official plugin for Jenkinsfile ↔ YAML conversion
- jackson (databind, core, annotations, dataformat-yaml) — JSON ↔ YAML conversion

## Main Method Test

Include a `main` method with hardcoded string constants demonstrating all 6 conversion directions with sample Jenkinsfile content.

## File Location

`src/main/java/com/example/jenkinsdemoformat/util/PipelineFormatConverter.java`