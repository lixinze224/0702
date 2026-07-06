package com.example.jenkinsdemoformat.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.example.jenkinsdemoformat.entity.JenkinsConfig;
import com.example.jenkinsdemoformat.service.JenkinsConfigService;
import com.example.jenkinsdemoformat.service.JenkinsService;
import com.example.jenkinsdemoformat.util.JenkinsCrumbManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.util.regex.Matcher;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;


@Slf4j
@Service
@RequiredArgsConstructor
public class JenkinsServiceImpl implements JenkinsService {

    private int timeout=10*1000;
    @Autowired
    private JenkinsConfigService jenkinsConfigService;
    private final JenkinsCrumbManager crumbManager;
    private final RestTemplate restTemplate;
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));

    @Override
    public String jenkinsfileToJson(MultipartFile jenkinsfile) {
        JenkinsConfig randomPipeLine = jenkinsConfigService.getRandomPipeLine();
        String endpoint = randomPipeLine.getJenkinsIp() + "/pipeline-model-converter/toJson";
        return doPost(endpoint, "jenkinsfile", jenkinsfile,randomPipeLine);
    }

    @Override
    public String jenkinsfileToYaml(MultipartFile jenkinsfile) {
        String jsonResponse = jenkinsfileToJson(jenkinsfile);
        String json = extractJsonFromResponse(jsonResponse);
        return jsonToYaml(json);
    }

    @Override
    public String jsonToJenkinsfile(MultipartFile jsonFile) {
        JenkinsConfig randomPipeLine = jenkinsConfigService.getRandomPipeLine();
        String endpoint = randomPipeLine.getJenkinsIp() + "/pipeline-model-converter/toJenkinsfile";
        return doPost(endpoint, "json", jsonFile,randomPipeLine);
    }

    @Override
    public String yamlToJenkinsfile(MultipartFile yamlFile) {
        JenkinsConfig randomPipeLine = jenkinsConfigService.getRandomPipeLine();
        String yamlContent = readFileContent(yamlFile);
        String json = yamlToJson(yamlContent);
        return jsonToJenkinsfileByContent(json,randomPipeLine);
    }

    @Override
    public String validateJenkinsfile(MultipartFile jenkinsfile) {
        JenkinsConfig randomPipeLine = jenkinsConfigService.getRandomPipeLine();
        String endpoint = randomPipeLine.getJenkinsIp() + "/pipeline-model-converter/validate";
        return doPost(endpoint, "jenkinsfile", jenkinsfile,randomPipeLine);
    }

    @Override
    public void deletePipeline(String jobName,JenkinsConfig jenkinsConfig) {
        String endpoint = jenkinsConfig.getJenkinsIp() + "/job/" + jobName + "/doDelete";
        try {
            String auth = jenkinsConfig.getUsername() + ":" + jenkinsConfig.getPassword();
            String crumb = getCrumb(jenkinsConfig);

            HttpRequest request = HttpRequest.post(endpoint)
                    .header("Authorization", "Basic " + cn.hutool.core.codec.Base64.encode(auth))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(timeout);

            if (crumb != null) {
                request.header("Jenkins-Crumb", crumb);
            }

            HttpResponse response = request.execute();

            int status = response.getStatus();
            if (status != 200 && status != 302 && status != 404) {
                log.error("Delete pipeline failed: {} -> {}", endpoint, status);
                throw new RuntimeException("Delete pipeline failed: " + status + " - " + response.body());
            }
        } catch (Exception e) {
            log.error("Failed to delete pipeline: {}", jobName, e);
            throw new RuntimeException("Failed to delete pipeline: " + e.getMessage(), e);
        }
    }

    @Override
    public void buildPipeline(String jobName,JenkinsConfig jenkinsConfig) {
        String endpoint = jenkinsConfig.getJenkinsIp() + "/job/" + jobName + "/build";
        try {
            String auth = jenkinsConfig.getUsername() + ":" + jenkinsConfig.getPassword();
            String crumb = getCrumb(jenkinsConfig);

            HttpRequest request = HttpRequest.post(endpoint)
                    .header("Authorization", "Basic " + cn.hutool.core.codec.Base64.encode(auth))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(timeout);

            if (crumb != null) {
                request.header("Jenkins-Crumb", crumb);
            }

            HttpResponse response = request.execute();

            int status = response.getStatus();
            if (status != 200 && status != 201 && status != 302) {
                log.error("Build pipeline failed: {} -> {}", endpoint, status);
                throw new RuntimeException("Build pipeline failed: " + status + " - " + response.body());
            }
        } catch (Exception e) {
            log.error("Failed to build pipeline: {}", jobName, e);
            throw new RuntimeException("Failed to build pipeline: " + e.getMessage(), e);
        }
    }

    @Override
    public void renamePipeline(String oldJobName, String newJobName,JenkinsConfig jenkinsConfig) {
        String endpoint = jenkinsConfig.getJenkinsIp() + "/job/" + oldJobName + "/confirmRename";
        try {
            String auth = jenkinsConfig.getUsername() + ":" + jenkinsConfig.getPassword();
            String crumb = getCrumb(jenkinsConfig);

            HttpRequest request = HttpRequest.post(endpoint)
                    .header("Authorization", "Basic " + cn.hutool.core.codec.Base64.encode(auth))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .form("newName", newJobName)
                    .timeout(timeout);

            if (crumb != null) {
                request.header("Jenkins-Crumb", crumb);
            }

            HttpResponse response = request.execute();

            int status = response.getStatus();
            if (status != 200 && status != 302) {
                log.error("Rename pipeline failed: {} -> {}", endpoint, status);
                throw new RuntimeException("Rename pipeline failed: " + status + " - " + response.body());
            }
        } catch (Exception e) {
            log.error("Failed to rename pipeline: {} to {}", oldJobName, newJobName, e);
            throw new RuntimeException("Failed to rename pipeline: " + e.getMessage(), e);
        }
    }

    @Override
    public String createPipeline(String jobName, String jenkinsfileContent,JenkinsConfig jenkinsConfig) {
        String endpoint = jenkinsConfig.getJenkinsIp() + "/createItem?name=" + jobName;
        try {
            String auth = jenkinsConfig.getUsername() + ":" + jenkinsConfig.getPassword();
            String crumb = getCrumb(jenkinsConfig);

            // Jenkins requires job configuration XML to create a WorkflowJob with inline Jenkinsfile
            String configXml = buildPipelineConfigXml(jenkinsfileContent);

            HttpRequest request = HttpRequest.post(endpoint)
                    .header("Authorization", "Basic " + cn.hutool.core.codec.Base64.encode(auth))
                    .header("Content-Type", "application/xml")
                    .body(configXml)
                    .timeout(timeout);

            if (crumb != null) {
                request.header("Jenkins-Crumb", crumb);
            }

            HttpResponse response = request.execute();

            if (!response.isOk() && response.getStatus() != 200 && response.getStatus() != 302) {
                log.error("Create pipeline failed: {} -> {}", endpoint, response.getStatus());
                throw new RuntimeException("Create pipeline failed: " + response.getStatus() + " - " + response.body());
            }
            return response.body();
        } catch (Exception e) {
            log.error("Failed to create pipeline: {}", jobName, e);
            throw new RuntimeException("Failed to create pipeline: " + e.getMessage(), e);
        }
    }

    @Override
    public String listPipelines(String name,JenkinsConfig jenkinsConfig) {
        String endpoint;
        boolean isSpecificJob = name != null && !name.isEmpty();
        if (isSpecificJob) {
            endpoint = jenkinsConfig.getJenkinsIp() + "/job/" + name + "/api/json?tree=name,url,description,lastBuild[number,result]";
        } else {
            endpoint = jenkinsConfig.getJenkinsIp() + "/api/json?tree=jobs[name,url,description,lastBuild[number,result]]";
        }
        try {
            String auth = jenkinsConfig.getUsername() + ":" + jenkinsConfig.getPassword();

            HttpResponse response = HttpRequest.get(endpoint)
                    .header("Authorization", "Basic " + cn.hutool.core.codec.Base64.encode(auth))
                    .timeout(timeout)
                    .execute();

            if (response.isOk()) {
                return response.body();
            } else if (response.getStatus() == 404 && isSpecificJob) {
                log.info("Pipeline not found: {}", name);
                return "{\"jobs\":[]}";
            } else {
                log.error("List pipelines failed: {} -> {}", endpoint, response.getStatus());
                throw new RuntimeException("List pipelines failed: " + response.getStatus() + " - " + response.body());
            }
        } catch (Exception e) {
            log.error("Failed to list pipelines", e);
            throw new RuntimeException("Failed to list pipelines: " + e.getMessage(), e);
        }
    }

    @Override
    public String getPipelineConfig(String jobName,JenkinsConfig jenkinsConfig) {
        String endpoint = jenkinsConfig.getJenkinsIp() + "/job/" + jobName + "/config.xml";
        try {
            String auth = jenkinsConfig.getUsername() + ":" + jenkinsConfig.getPassword();
            String crumb = getCrumb(jenkinsConfig);

            HttpRequest request = HttpRequest.get(endpoint)
                    .header("Authorization", "Basic " + cn.hutool.core.codec.Base64.encode(auth))
                    .timeout(timeout);

            if (crumb != null) {
                request.header("Jenkins-Crumb", crumb);
            }

            HttpResponse response = request.execute();

            if (response.isOk()) {
                return response.body();
            } else {
                log.error("Get pipeline config failed: {} -> {}", endpoint, response.getStatus());
                throw new RuntimeException("Get pipeline config failed: " + response.getStatus() + " - " + response.body());
            }
        } catch (Exception e) {
            log.error("Failed to get pipeline config: {}", jobName, e);
            throw new RuntimeException("Failed to get pipeline config: " + e.getMessage(), e);
        }
    }

    @Override
    public String updatePipelineJenkinsfile(String jobName, String jenkinsfileContent,JenkinsConfig jenkinsConfig) {
        String endpoint = jenkinsConfig.getJenkinsIp() + "/job/" + jobName + "/config.xml";
        log.info("Updating pipeline config: {}, endpoint: {}", jobName, endpoint);

        try {
            // 使用 username:token 进行认证
            String auth = jenkinsConfig.getUsername() + ":" + jenkinsConfig.getToken();
            String authHeader = "Basic " + cn.hutool.core.codec.Base64.encode(auth);
            log.info("Using token-based auth");

            // 1. 获取当前配置
            String crumb = getCrumbWithToken(jenkinsConfig, authHeader);
            log.info("Crumb from Jenkins: [{}]", crumb);

            HttpRequest getRequest = HttpRequest.get(endpoint)
                    .header("Authorization", authHeader)
                    .timeout(timeout);

            if (crumb != null) {
                getRequest.header("Jenkins-Crumb", crumb.trim());
            }

            HttpResponse getResponse = getRequest.execute();
            log.info("GET response status: {}", getResponse.getStatus());

            if (!getResponse.isOk()) {
                log.error("Get pipeline config failed: {} -> {}, body: {}", endpoint, getResponse.getStatus(), getResponse.body());
                throw new RuntimeException("Get pipeline config failed: " + getResponse.getStatus() + " - " + getResponse.body());
            }

            String currentConfig = getResponse.body();
            log.debug("Current config length: {}", currentConfig != null ? currentConfig.length() : 0);

            // 2. 更新配置中的 script 部分
            String updatedConfig = updateScriptInConfig(currentConfig, jenkinsfileContent);
            log.info("Updated config length: {}", updatedConfig.length());

            // 3. 重新获取 crumb 用于 POST
            crumb = getCrumbWithToken(jenkinsConfig, authHeader);
            log.info("Crumb for POST: [{}]", crumb);

            HttpRequest postRequest = HttpRequest.post(endpoint)
                    .header("Authorization", authHeader)
                    .header("Content-Type", "application/xml; charset=UTF-8")
                    .header("Jenkins-Crumb", crumb != null ? crumb.trim() : "")
                    .body(updatedConfig.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                    .timeout(timeout);

            log.info("Sending POST to {}, crumb header: [{}]", endpoint, crumb != null ? crumb.trim() : "null");
            HttpResponse postResponse = postRequest.execute();
            log.info("POST response status: {}", postResponse.getStatus());

            String responseBody = postResponse.body();
            log.info("POST response body length: {}", responseBody != null ? responseBody.length() : 0);
            if (responseBody != null && responseBody.length() > 500) {
                log.info("POST response body (first 500 chars): {}", responseBody.substring(0, 500));
            } else {
                log.info("POST response body: {}", responseBody);
            }

            if (postResponse.isOk() || postResponse.getStatus() == 200 || postResponse.getStatus() == 302) {
                log.info("Update pipeline jenkinsfile success: {}", jobName);
                return "Update success";
            } else {
                log.error("Update pipeline jenkinsfile failed: {} -> {}, body: {}", endpoint, postResponse.getStatus(), responseBody);
                throw new RuntimeException("Update pipeline jenkinsfile failed: " + postResponse.getStatus() + " - " + responseBody);
            }
        } catch (Exception e) {
            log.error("Failed to update pipeline jenkinsfile: {}", jobName, e);
            throw new RuntimeException("Failed to update pipeline jenkinsfile: " + e.getMessage(), e);
        }
    }

    /**
     * 更新 config.xml 中的 script 部分
     * 通过正则提取原配置中除了 definition 块之外的内容，然后用新的 definition 块替换
     */
    private String updateScriptInConfig(String currentConfig, String jenkinsfileContent) {
        // Escape special XML characters in Jenkinsfile
        String escapedJenkinsfile = jenkinsfileContent
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");

        // 构建新的 definition 块
        String newDefinition = "  <definition class=\"org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition\" plugin=\"workflow-cps\">\n" +
                "    <script>" + escapedJenkinsfile + "</script>\n" +
                "    <sandbox>true</sandbox>\n" +
                "  </definition>";

        // 匹配并替换整个 <definition>...</definition> 块
        String pattern = "<definition[^>]*>[\\s\\S]*?</definition>";
        String result = currentConfig.replaceFirst(pattern, Matcher.quoteReplacement(newDefinition));

        // 如果没有匹配到（说明原来的 config 没有 definition 块），直接在 </flow-definition> 前面插入
        if (result.equals(currentConfig)) {
            int lastCloseIndex = currentConfig.lastIndexOf("</flow-definition>");
            if (lastCloseIndex > 0) {
                result = currentConfig.substring(0, lastCloseIndex) + newDefinition + "\n" + currentConfig.substring(lastCloseIndex);
            }
        }

        return result;
    }

    private String buildPipelineConfigXml(String jenkinsfileContent) {
        // Escape special XML characters in Jenkinsfile
        String escapedJenkinsfile = jenkinsfileContent
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");

        return "<?xml version='1.1' encoding='UTF-8'?>\n" +
                "<flow-definition plugin=\"workflow-job\">\n" +
                "  <description></description>\n" +
                "  <keepDependencies>false</keepDependencies>\n" +
                "  <properties/>\n" +
                "  <definition class=\"org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition\" plugin=\"workflow-cps\">\n" +
                "    <script>" + escapedJenkinsfile + "</script>\n" +
                "    <sandbox>true</sandbox>\n" +
                "  </definition>\n" +
                "  <triggers/>\n" +
                "  <disabled>false</disabled>\n" +
                "</flow-definition>";
    }

    private String doPost(String endpoint, String paramName, MultipartFile file,JenkinsConfig randomPipeLine) {
        try {
            String auth = randomPipeLine.getUsername() + ":" + randomPipeLine.getPassword();
            String fileContent = readFileContent(file);
            String crumb = getCrumb(randomPipeLine);

            HttpRequest request = HttpRequest.post(endpoint)
                    .header("Authorization", "Basic " + cn.hutool.core.codec.Base64.encode(auth))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .form(paramName, fileContent)
                    .timeout(timeout);

            if (crumb != null) {
                request.header("Jenkins-Crumb", crumb);
            }

            HttpResponse response = request.execute();

            if (response.isOk()) {
                return response.body();
            } else {
                log.error("Jenkins API call failed: {} -> {}", endpoint, response.getStatus());
                throw new RuntimeException("Jenkins API call failed: " + response.getStatus() + " - " + response.body());
            }
        } catch (Exception e) {
            log.error("Failed to call Jenkins API: {}", endpoint, e);
            throw new RuntimeException("Failed to call Jenkins API: " + e.getMessage(), e);
        }
    }

    private String jsonToJenkinsfileByContent(String jsonContent,JenkinsConfig randomPipeLine) {

        String endpoint = randomPipeLine.getJenkinsIp() + "/pipeline-model-converter/toJenkinsfile";
        try {
            String auth = randomPipeLine.getUsername() + ":" + randomPipeLine.getPassword();
            String crumb = getCrumb(randomPipeLine);

            HttpRequest request = HttpRequest.post(endpoint)
                    .header("Authorization", "Basic " + cn.hutool.core.codec.Base64.encode(auth))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .form("json", jsonContent)
                    .timeout(timeout);

            if (crumb != null) {
                request.header("Jenkins-Crumb", crumb);
            }

            HttpResponse response = request.execute();

            if (response.isOk()) {
                return response.body();
            } else {
                log.error("Jenkins API call failed: {} -> {}", endpoint, response.getStatus());
                throw new RuntimeException("Jenkins API call failed: " + response.getStatus() + " - " + response.body());
            }
        } catch (Exception e) {
            log.error("Failed to call Jenkins API: {}", endpoint, e);
            throw new RuntimeException("Failed to call Jenkins API: " + e.getMessage(), e);
        }
    }

    private String extractJsonFromResponse(String response) {
        try {
            JsonNode root = jsonMapper.readTree(response);
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root.at("/data/json"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract JSON from response: " + e.getMessage(), e);
        }
    }

    private String jsonToYaml(String json) {
        try {
            JsonNode jsonNode = jsonMapper.readTree(json);
            return yamlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JSON to YAML: " + e.getMessage(), e);
        }
    }

    private String yamlToJson(String yaml) {
        try {
            JsonNode yamlNode = yamlMapper.readTree(yaml);
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(yamlNode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert YAML to JSON: " + e.getMessage(), e);
        }
    }

    private String readFileContent(MultipartFile file) {
        try {
            return new String(file.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file content: " + e.getMessage(), e);
        }
    }

    private String getCrumb(JenkinsConfig jenkinsConfig) {
        String crumbEndpoint = jenkinsConfig.getJenkinsIp() + "/crumbIssuer/api/xml";
        try {
            String auth = jenkinsConfig.getUsername() + ":" + jenkinsConfig.getPassword();

            HttpResponse response = HttpRequest.get(crumbEndpoint)
                    .header("Authorization", "Basic " + cn.hutool.core.codec.Base64.encode(auth))
                    .timeout(timeout)
                    .execute();

            if (response.isOk()) {
                String body = response.body();
                int start = body.indexOf("<crumb>") + 7;
                int end = body.indexOf("</crumb>");
                return body.substring(start, end);
            } else {
                log.warn("Failed to get crumb: {}", response.getStatus());
                return null;
            }
        } catch (Exception e) {
            log.warn("Exception getting crumb, continuing without it", e);
            return null;
        }
    }

    /**
     * 使用 username:token 获取 Jenkins Crumb
     */
    private String getCrumbWithToken(JenkinsConfig jenkinsConfig, String authHeader) {
        String crumbEndpoint = jenkinsConfig.getJenkinsIp() + "/crumbIssuer/api/xml";
        try {
            HttpResponse response = HttpRequest.get(crumbEndpoint)
                    .header("Authorization", authHeader)
                    .timeout(timeout)
                    .execute();

            if (response.isOk()) {
                String body = response.body();
                int start = body.indexOf("<crumb>") + 7;
                int end = body.indexOf("</crumb>");
                return body.substring(start, end);
            } else {
                log.warn("Failed to get crumb with token: {}", response.getStatus());
                return null;
            }
        } catch (Exception e) {
            log.warn("Exception getting crumb with token, continuing without it", e);
            return null;
        }
    }
}