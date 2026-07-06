package com.example.jenkinsdemoformat.service.impl;

import com.example.jenkinsdemoformat.entity.*;
import com.example.jenkinsdemoformat.service.JenkinsLogService;
import com.example.jenkinsdemoformat.util.JenkinsCrumbManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

@Service
public class JenkinsLogServiceImpl implements JenkinsLogService {

    private static final Logger log = LoggerFactory.getLogger(JenkinsServiceImpl.class);

    @Autowired
    private JenkinsCrumbManager crumbManager;

    private final ObjectMapper objectMapper = new ObjectMapper();
    // 线程池，用于并行请求
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    // 日志缓存：key -> (log content, expiration time)
    private static final long STAGE_LOG_CACHE_TTL_MS = 5 * 60 * 1000; // 5分钟缓存
    private final ConcurrentHashMap<String, CacheEntry<String>> stageLogCache = new ConcurrentHashMap<>();

    /**
     * 对 jobName 进行 URL 编码
     * 处理中文等特殊字符，避免 URI 构建失败
     */
    private String encodeJobName(String jobName) {
        if (jobName == null) {
            return null;
        }
        try {
            return java.net.URLEncoder.encode(jobName, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            log.warn("编码 jobName 失败: {}", jobName, e);
            return jobName;
        }
    }

    /**
     * 日志缓存条目
     */
    private static class CacheEntry<T> {
        final T value;
        final long expireTime;

        CacheEntry(T value, long ttlMs) {
            this.value = value;
            this.expireTime = System.currentTimeMillis() + ttlMs;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }

    /**
     * 构建 Basic 认证头信息
     * 格式：Authorization: Basic Base64(username:token)
     */
    private String getAuthHeader(JenkinsConfig jenkinsConfig) {
        String auth = jenkinsConfig.getUsername() + ":" + jenkinsConfig.getToken();
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 查询 Jenkins 流水线的构建历史记录
     * 使用并行请求优化，每个构建详情异步获取
     *
     * @param jobName 流水线名称
     * @param depth  查询深度，返回最近N条记录
     * @return 构建历史列表，按构建编号倒序排列（最新构建在前）
     */
    @Override
    public List<JenkinsBuild> getBuildHistory(Long pipeLineId,String jobName, int depth,JenkinsConfig jenkinsConfig) {
        // 先获取job下的构建编号列表
        // 注意：MultiBranch项目（如cf58）没有直接的builds，需要获取其branch下的job
        String encodedJobName = crumbManager.encodeJobName(jobName);
        String apiUrl = jenkinsConfig.getJenkinsIp() + "/job/" + encodedJobName + "/api/json?tree=builds[number],jobs[name]";

        long startTime = System.currentTimeMillis();
        long jobApiTime = 0;

        HttpHeaders headers = crumbManager.getAuthHeaders(jenkinsConfig);

        RequestEntity<Void> request = new RequestEntity<>(headers, HttpMethod.GET, URI.create(apiUrl));
        long jobStart = System.currentTimeMillis();
        ResponseEntity<String> response = crumbManager.executeWithRetry(request, String.class,jenkinsConfig);

        jobApiTime = System.currentTimeMillis() - jobStart;
        log.info("[Job API] 获取构建列表耗时: {}ms", jobApiTime);

        List<JenkinsBuild> builds = new ArrayList<>();

        try {
            Map<String, Object> body = objectMapper.readValue(response.getBody(), Map.class);
            log.debug("[DEBUG] body keys: {}", body != null ? body.keySet() : "null");
            log.debug("[DEBUG] body has builds: {}", body != null && body.containsKey("builds"));
            log.debug("[DEBUG] body has jobs: {}", body != null && body.containsKey("jobs"));

            // 先尝试从根级别获取builds（普通Job）
            if (body != null && body.containsKey("builds")) {
                List<Map<String, Object>> buildsList = (List<Map<String, Object>>) body.get("builds");
                int count = 0;
                List<CompletableFuture<JenkinsBuild>> futures = new ArrayList<>();
                List<Integer> buildNumbers = new ArrayList<>();

                for (Map<String, Object> build : buildsList) {
                    if (count >= depth) break;
                    Integer buildNumber = (Integer) build.get("number");
                    if (buildNumber != null) {
                        CompletableFuture<JenkinsBuild> future = CompletableFuture.supplyAsync(
                            () -> getBuildDetail(pipeLineId,jobName, buildNumber,jenkinsConfig), executor
                        );
                        futures.add(future);
                        buildNumbers.add(buildNumber);
                    }
                    count++;
                }

                long buildStartTime = System.currentTimeMillis();
                for (int i = 0; i < futures.size(); i++) {
                    try {
                        JenkinsBuild result = futures.get(i).get(10, TimeUnit.SECONDS);
                        if (result != null) {
                            builds.add(result);
                        }
                    } catch (Exception e) {
                        log.error("获取构建详情失败: jobName={}, buildNumber={}, error: {}", jobName, buildNumbers.get(i), e.getMessage(), e);
                    }
                }
                long buildTotalTime = System.currentTimeMillis() - buildStartTime;
                long totalTime = System.currentTimeMillis() - startTime;
                log.info("[Build API] 并行获取{}个构建详情耗时: {}ms", builds.size(), buildTotalTime);
                log.info("[TOTAL] 总耗时: {}ms", totalTime);
            }
            // 如果没有builds，尝试从jobs获取（MultiBranch项目）
            else if (body != null && body.containsKey("jobs")) {
                log.debug("[DEBUG] 检测到jobs字段，准备处理MultiBranch");
                List<Map<String, Object>> jobsList = (List<Map<String, Object>>) body.get("jobs");
                log.debug("[DEBUG] jobsList: {}", jobsList);
                if (jobsList != null && !jobsList.isEmpty()) {
                    // 使用第一个branch（如master）
                    Map<String, Object> firstJob = jobsList.get(0);
                    String branchName = (String) firstJob.get("name");
                    log.info("[MultiBranch] 检测到多分支项目，使用分支: {}", branchName);

                    // 递归调用获取该分支的构建
                    String newJobName = jobName + "/job/" + branchName;
                    builds = getBuildHistory(pipeLineId,newJobName, depth,jenkinsConfig);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return builds;
    }

    /**
     * 获取单个构建的详细信息
     */
    private JenkinsBuild getBuildDetail(Long pipeLineId,String jobName, int buildNumber,JenkinsConfig jenkinsConfig) {
        String encodedJobName = encodeJobName(jobName);
        String detailUrl = jenkinsConfig.getJenkinsIp() + "/job/" + encodedJobName + "/" + buildNumber + "/api/json";

        try {
            RequestEntity<Void> request = new RequestEntity<>(crumbManager.getAuthHeaders(jenkinsConfig), HttpMethod.GET, URI.create(detailUrl));
            ResponseEntity<String> response = crumbManager.executeWithRetry(request, String.class,jenkinsConfig);

            Map<String, Object> build = objectMapper.readValue(response.getBody(), Map.class);

            JenkinsBuild jenkinsBuild = new JenkinsBuild();
            jenkinsBuild.setNumber((Integer) build.get("number"));
            jenkinsBuild.setUrl((String) build.get("url"));
            // 设置构建结果：区分排队中和构建中
            Object result = build.get("result");
            Object building = build.get("building");
            if (result == null && Boolean.TRUE.equals(building)) {
                jenkinsBuild.setResult("IN_PROGRESS");  // 构建中
            } else if (result == null && Boolean.FALSE.equals(building)) {
                jenkinsBuild.setResult("QUEUED");  // 排队中
            } else {
                jenkinsBuild.setResult((String) result);
            }
            jenkinsBuild.setPipeline(jobName);
            jenkinsBuild.setStagesUrl("/jenkins/stages?pipeLineId=" + pipeLineId + "&buildNumber=" + build.get("number") + "&fetchLog=false");

            // 处理时间戳
            Object timestamp = build.get("timestamp");
            if (timestamp != null) {
                long ts = ((Number) timestamp).longValue();
                jenkinsBuild.setTimestamp(new Date(ts));
            }

            // 处理构建耗时
            Object duration = build.get("duration");
            if (duration != null) {
                long dur = ((Number) duration).longValue();
                jenkinsBuild.setDuration(formatDuration(dur));
            }

            // 处理构建人信息
            Object actions = build.get("actions");
            if (actions != null && actions instanceof List) {
                List<Map<String, Object>> actionsList = (List<Map<String, Object>>) actions;
                for (Map<String, Object> action : actionsList) {
                    if (action.containsKey("causes")) {
                        List<Map<String, Object>> causes = (List<Map<String, Object>>) action.get("causes");
                        if (causes != null && !causes.isEmpty()) {
                            Map<String, Object> cause = causes.get(0);
                            Object userId = cause.get("userId");
                            if (userId != null) {
                                jenkinsBuild.setBuilder(userId.toString());
                            }
                        }
                        break;
                    }
                }
            }

            return jenkinsBuild;
        } catch (Exception e) {
            log.error("获取构建详情失败: jobName={}, buildNumber={}, error: {}", jobName, buildNumber, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 分页查询 Jenkins 流水线的构建历史记录
     * 优化：复用 getBuildHistory 的高效模式，但限制获取数量
     *
     * @param jobName   流水线名称
     * @param page      页码（从1开始）
     * @param pageSize  每页记录数
     * @return 分页结果
     */
    @Override
    public PageResult<JenkinsBuild> getBuildHistoryByPage(Long pipeLineId,String jobName, int page, int pageSize,JenkinsConfig jenkinsConfig) {
        long startTime = System.currentTimeMillis();

        // 1. 获取所有构建编号列表（轻量级调用，只获取number）
        List<Integer> allBuildNumbers = getAllBuildNumbers(jobName,jenkinsConfig);
        long total = allBuildNumbers.size();

        // 2. 计算分页索引
        int fromIndex = (page - 1) * pageSize;
        if (fromIndex >= allBuildNumbers.size()) {
            return new PageResult<>(Collections.emptyList(), total, page, pageSize);
        }

        // 3. 计算当前页需要获取详情的构建编号
        int toIndex = Math.min(fromIndex + pageSize, allBuildNumbers.size());
        List<Integer> pageBuildNumbers = allBuildNumbers.subList(fromIndex, toIndex);

        // 4. 直接复用高效并行获取模式（不额外调用方法，减少开销）
        List<JenkinsBuild> builds = new ArrayList<>();
        List<CompletableFuture<JenkinsBuild>> futures = new ArrayList<>();

        for (Integer buildNumber : pageBuildNumbers) {
            CompletableFuture<JenkinsBuild> future = CompletableFuture.supplyAsync(
                () -> getBuildDetail(pipeLineId,jobName, buildNumber,jenkinsConfig), executor
            );
            futures.add(future);
        }

        for (int i = 0; i < futures.size(); i++) {
            try {
                JenkinsBuild result = futures.get(i).get(10, TimeUnit.SECONDS);
                if (result != null) {
                    builds.add(result);
                }
            } catch (Exception e) {
                log.error("获取构建详情失败: jobName={}, buildNumber={}, error: {}", jobName, pageBuildNumbers.get(i), e.getMessage(), e);
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("[PageQuery] 分页查询耗时: {}ms, 总记录数: {}, 当前页: {}, 每页: {}", totalTime, total, page, pageSize);

        return new PageResult<>(builds, total, page, pageSize);
    }

    /**
     * 获取所有构建编号（轻量级，仅获取number字段）
     */
    private List<Integer> getAllBuildNumbers(String jobName,JenkinsConfig jenkinsConfig) {
        String encodedJobName = encodeJobName(jobName);
        String apiUrl = jenkinsConfig.getJenkinsIp() + "/job/" + encodedJobName + "/api/json?tree=builds[number],jobs[name]";

        RequestEntity<Void> request = new RequestEntity<>(crumbManager.getAuthHeaders(jenkinsConfig), HttpMethod.GET, URI.create(apiUrl));
        ResponseEntity<String> response = crumbManager.executeWithRetry(request, String.class,jenkinsConfig);

        List<Integer> buildNumbers = new ArrayList<>();

        try {
            Map<String, Object> body = objectMapper.readValue(response.getBody(), Map.class);

            // 普通 Job：直接从 builds 获取编号列表
            if (body != null && body.containsKey("builds")) {
                List<Map<String, Object>> buildsList = (List<Map<String, Object>>) body.get("builds");
                for (Map<String, Object> build : buildsList) {
                    Integer buildNumber = (Integer) build.get("number");
                    if (buildNumber != null) {
                        buildNumbers.add(buildNumber);
                    }
                }
            }
            // MultiBranch 项目：递归获取第一个分支的构建编号
            else if (body != null && body.containsKey("jobs")) {
                List<Map<String, Object>> jobsList = (List<Map<String, Object>>) body.get("jobs");
                if (jobsList != null && !jobsList.isEmpty()) {
                    String branchName = (String) jobsList.get(0).get("name");
                    log.info("[MultiBranch] 检测到多分支项目，使用分支: {}", branchName);
                    buildNumbers = getAllBuildNumbers(jobName + "/job/" + branchName,jenkinsConfig);
                }
            }
        } catch (Exception e) {
            log.error("获取构建编号列表失败: jobName={}, error: {}", jobName, e.getMessage(), e);
        }

        return buildNumbers;
    }

    /**
     * 并行获取指定构建编号的详情
     */
    private List<JenkinsBuild> getBuildDetails(Long pipeLineId,String jobName, List<Integer> buildNumbers,JenkinsConfig jenkinsConfig) {
        List<CompletableFuture<JenkinsBuild>> futures = new ArrayList<>();

        for (Integer buildNumber : buildNumbers) {
            CompletableFuture<JenkinsBuild> future = CompletableFuture.supplyAsync(
                () -> getBuildDetail(pipeLineId,jobName, buildNumber,jenkinsConfig), executor
            );
            futures.add(future);
        }

        List<JenkinsBuild> builds = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            try {
                JenkinsBuild result = futures.get(i).get(10, TimeUnit.SECONDS);
                if (result != null) {
                    builds.add(result);
                }
            } catch (Exception e) {
                log.error("获取构建详情失败: jobName={}, buildNumber={}, error: {}", jobName, buildNumbers.get(i), e.getMessage(), e);
            }
        }

        return builds;
    }

    /**
     * 格式化构建耗时
     */
    private String formatDuration(long durationMs) {
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }

    /**
     * 查询 Jenkins 流水线指定构建的阶段和日志
     *
     * @param jobName      流水线名称
     * @param buildNumber 构建编号
     * @param fetchLog     是否获取每个阶段的日志
     * @return 构建阶段信息
     */
    @Override
    public BuildStages getBuildStages(String jobName, int buildNumber, boolean fetchLog,JenkinsConfig jenkinsConfig) {
        String encodedJobName = encodeJobName(jobName);
        String apiUrl = jenkinsConfig.getJenkinsIp() + "/job/" + encodedJobName + "/" + buildNumber + "/wfapi/describe";

        RequestEntity<Void> request = new RequestEntity<>(crumbManager.getAuthHeaders(jenkinsConfig), HttpMethod.GET, URI.create(apiUrl));
        ResponseEntity<String> response = crumbManager.executeWithRetry(request, String.class,jenkinsConfig);

        try {
            Map<String, Object> body = objectMapper.readValue(response.getBody(), Map.class);

            List<PipelineStage> stages = new ArrayList<>();

            // 获取阶段列表
            List<Map<String, Object>> stageList = (List<Map<String, Object>>) body.get("stages");
            if (stageList != null) {
                for (Map<String, Object> stage : stageList) {
                    PipelineStage pipelineStage = new PipelineStage();
                    pipelineStage.setId((String) stage.get("id"));
                    pipelineStage.setName((String) stage.get("name"));
                    pipelineStage.setStatus((String) stage.get("status"));

                    Object duration = stage.get("durationMillis");
                    if (duration != null) {
                        pipelineStage.setDuration(((Number) duration).longValue());
                    }

                    // 设置阶段日志查询URL
                    pipelineStage.setLogUrl("/jenkins/stage/log?jobName=" + jobName + "&buildNumber=" + buildNumber + "&stageId=" + stage.get("id"));

                    // 获取阶段下的步骤（stageFlowNodes）
                    List<PipelineStage.StepInfo> steps = getStageFlowNodes(jobName, buildNumber, (String) stage.get("id"),jenkinsConfig);
                    pipelineStage.setSteps(steps);

                    // 获取阶段日志
                    if (fetchLog) {
                        String stageLog = getStageLog(jobName, buildNumber, (String) stage.get("id"),jenkinsConfig);
                        pipelineStage.setLog(stageLog);
                    }

                    stages.add(pipelineStage);
                }
            }

            return new BuildStages(buildNumber, jobName, stages);
        } catch (Exception e) {
            log.error("获取构建阶段失败: jobName={}, buildNumber={}, error: {}", jobName, buildNumber, e.getMessage(), e);
            return new BuildStages(buildNumber, jobName, Collections.emptyList());
        }
    }

    /**
     * 查询 Jenkins 流水线指定阶段（单个）的日志
     * <p>
     * 性能优化：独立接口，支持按需获取单个阶段的日志
     *
     * @param lines  返回多少行，null 表示返回全部
     * @param offset 从第几行开始（从0计），null 表示从最后往前取
     */
    @Override
    public String getStageLogById(String jobName, int buildNumber, String stageId, Integer lines, Integer offset,JenkinsConfig jenkinsConfig) {
        String log = getStageLog(jobName, buildNumber, stageId,jenkinsConfig);
        if (log == null) {
            return null;
        }

        String[] allLines = log.split("\n");
        int totalLines = allLines.length;

        // 如果只指定了 lines，没指定 offset：取最后 lines 行（兼容旧逻辑）
        if (lines != null && lines > 0 && offset == null) {
            offset = Math.max(0, totalLines - lines);
        }

        // 如果指定了 offset
        if (offset != null && offset >= 0) {
            if (offset >= totalLines) {
                return null; // 超出范围
            }

            int end = (lines != null && lines > 0) ? Math.min(offset + lines, totalLines) : totalLines;
            StringBuilder sb = new StringBuilder();

            if (offset > 0) {
                sb.append("... ").append(offset).append(" previous lines ...\n");
            }
            for (int i = offset; i < end; i++) {
                sb.append(allLines[i]).append("\n");
            }
            if (end < totalLines) {
                sb.append("... ").append(totalLines - end).append(" more lines ...");
            }
            return sb.toString();
        }

        // 什么都没指定，返回全部
        return log;
    }

    /**
     * 获取单个阶段的日志
     * <p>
     * Jenkins Pipeline 的阶段日志通过 /wfapi/log 接口获取
     * 如果阶段本身没有日志，会尝试从子节点（stageFlowNodes）获取
     * <p>
     * 性能优化：5分钟缓存，相同 stage 的日志不重复请求 Jenkins
     */
    private String getStageLog(String jobName, int buildNumber, String stageId,JenkinsConfig jenkinsConfig) {
        if (stageId == null) {
            return null;
        }

        // 尝试从缓存获取
        String cacheKey = jobName + ":" + buildNumber + ":" + stageId;
        CacheEntry<String> cached = stageLogCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("[Cache HIT] stage log: {}", cacheKey);
            return cached.value;
        }
        log.debug("[Cache MISS] stage log: {}", cacheKey);

        String encodedJobName = encodeJobName(jobName);
        String logUrl = jenkinsConfig.getJenkinsIp() + "/job/" + encodedJobName + "/" + buildNumber + "/execution/node/" + stageId + "/wfapi/log";

        try {
            RequestEntity<Void> request = new RequestEntity<>(crumbManager.getAuthHeaders(jenkinsConfig), HttpMethod.GET, URI.create(logUrl));
            ResponseEntity<String> response = crumbManager.executeWithRetry(request, String.class,jenkinsConfig);

            Map<String, Object> body = objectMapper.readValue(response.getBody(), Map.class);
            Object logText = body.get("text");

            // 如果阶段本身没有日志，尝试从 stageFlowNodes 获取
            if (logText == null || ((String) logText).isEmpty()) {
                // 获取阶段描述，包含 stageFlowNodes
                String encodedJobNameForDesc = encodeJobName(jobName);
                String descUrl = jenkinsConfig.getJenkinsIp() + "/job/" + encodedJobNameForDesc + "/" + buildNumber + "/execution/node/" + stageId + "/wfapi/describe";
                RequestEntity<Void> descRequest = new RequestEntity<>(crumbManager.getAuthHeaders(jenkinsConfig), HttpMethod.GET, URI.create(descUrl));
                ResponseEntity<String> descResponse = crumbManager.executeWithRetry(descRequest, String.class,jenkinsConfig);

                Map<String, Object> descBody = objectMapper.readValue(descResponse.getBody(), Map.class);

                // 从 stageFlowNodes 获取子节点的日志
                List<Map<String, Object>> stageFlowNodes = (List<Map<String, Object>>) descBody.get("stageFlowNodes");
                if (stageFlowNodes != null) {
                    StringBuilder combinedLog = new StringBuilder();
                    for (Map<String, Object> node : stageFlowNodes) {
                        String nodeId = (String) node.get("id");
                        String nodeLog = getNodeLog(jobName, buildNumber, nodeId,jenkinsConfig);
                        if (nodeLog != null && !nodeLog.isEmpty()) {
                            if (combinedLog.length() > 0) {
                                combinedLog.append("\n");
                            }
                            combinedLog.append(nodeLog);
                        }
                    }
                    String result = combinedLog.length() > 0 ? combinedLog.toString() : null;
                    // 缓存结果
                    if (result != null) {
                        stageLogCache.put(cacheKey, new CacheEntry<>(result, STAGE_LOG_CACHE_TTL_MS));
                    }
                    return result;
                }
            }

            String result = (String) logText;
            // 缓存结果
            if (result != null) {
                stageLogCache.put(cacheKey, new CacheEntry<>(result, STAGE_LOG_CACHE_TTL_MS));
            }
            return result;
        } catch (Exception e) {
            log.warn("获取阶段日志失败: jobName={}, buildNumber={}, stageId={}, error: {}", jobName, buildNumber, stageId, e.getMessage());
            return null;
        }
    }

    @Override
    public String getStepLog(String jobName, int buildNumber, String stepId,JenkinsConfig jenkinsConfig) {
        return getNodeLog(jobName, buildNumber, stepId,jenkinsConfig);
    }

    @Override
    public String getBuildAllLog(String jobName, int buildNumber, JenkinsConfig jenkinsConfig) {
        String encodedJobName = encodeJobName(jobName);
        String apiUrl = jenkinsConfig.getJenkinsIp() + "/job/" + encodedJobName + "/" + buildNumber + "/consoleText";

        try {
            RequestEntity<Void> request = new RequestEntity<>(crumbManager.getAuthHeaders(jenkinsConfig), HttpMethod.GET, URI.create(apiUrl));
            ResponseEntity<String> response = crumbManager.executeWithRetry(request, String.class, jenkinsConfig);
            return response.getBody();
        } catch (Exception e) {
            log.error("获取构建全部日志失败: jobName={}, buildNumber={}, error: {}", jobName, buildNumber, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取阶段下的步骤列表（stageFlowNodes）
     * 每个步骤包含 id 和 name
     */
    private List<PipelineStage.StepInfo> getStageFlowNodes(String jobName, int buildNumber, String stageId,JenkinsConfig jenkinsConfig) {
        List<PipelineStage.StepInfo> steps = new ArrayList<>();

        try {
            String encodedJobName = encodeJobName(jobName);
            String descUrl = jenkinsConfig.getJenkinsIp() + "/job/" + encodedJobName + "/" + buildNumber + "/execution/node/" + stageId + "/wfapi/describe";
            RequestEntity<Void> descRequest = new RequestEntity<>(crumbManager.getAuthHeaders(jenkinsConfig), HttpMethod.GET, URI.create(descUrl));
            ResponseEntity<String> descResponse = crumbManager.executeWithRetry(descRequest, String.class,jenkinsConfig);

            Map<String, Object> descBody = objectMapper.readValue(descResponse.getBody(), Map.class);
            List<Map<String, Object>> stageFlowNodes = (List<Map<String, Object>>) descBody.get("stageFlowNodes");

            if (stageFlowNodes != null) {
                for (Map<String, Object> node : stageFlowNodes) {
                    String nodeId = (String) node.get("id");
                    if (nodeId != null) {
                        steps.add(new PipelineStage.StepInfo(nodeId));
                    }
                }
            }
        } catch (Exception e) {
            log.debug("获取阶段步骤失败: jobName={}, buildNumber={}, stageId={}, error: {}", jobName, buildNumber, stageId, e.getMessage());
        }

        return steps;
    }

    /**
     * 获取单个节点的日志
     */
    private String getNodeLog(String jobName, int buildNumber, String nodeId,JenkinsConfig jenkinsConfig) {
        if (nodeId == null) {
            return null;
        }

        String encodedJobName = encodeJobName(jobName);
        String apiUrl = jenkinsConfig.getJenkinsIp() + "/job/" + encodedJobName + "/" + buildNumber + "/execution/node/" + nodeId + "/wfapi/log";

        try {
            RequestEntity<Void> request = new RequestEntity<>(crumbManager.getAuthHeaders(jenkinsConfig), HttpMethod.GET, URI.create(apiUrl));
            ResponseEntity<String> response = crumbManager.executeWithRetry(request, String.class,jenkinsConfig);

            Map<String, Object> body = objectMapper.readValue(response.getBody(), Map.class);
            return (String) body.get("text");
        } catch (Exception e) {
            log.debug("获取节点日志失败: jobName={}, buildNumber={}, nodeId={}, error: {}", jobName, buildNumber, nodeId, e.getMessage());
            return null;
        }
    }
}