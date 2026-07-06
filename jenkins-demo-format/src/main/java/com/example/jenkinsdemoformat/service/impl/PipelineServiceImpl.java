package com.example.jenkinsdemoformat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.jenkinsdemoformat.common.PipelineInfo;
import com.example.jenkinsdemoformat.common.Result;
import com.example.jenkinsdemoformat.entity.*;
import com.example.jenkinsdemoformat.mapper.JenkinsConfigMapper;
import com.example.jenkinsdemoformat.mapper.PipelineMapper;
import com.example.jenkinsdemoformat.service.JenkinsConfigService;
import com.example.jenkinsdemoformat.service.JenkinsLogService;
import com.example.jenkinsdemoformat.service.JenkinsService;
import com.example.jenkinsdemoformat.service.PipelineService;
import com.example.jenkinsdemoformat.util.JenkinsfileStageCounter;
import com.example.jenkinsdemoformat.util.SnowflakeIdGenerator;
import com.example.jenkinsdemoformat.util.StringToMultipartFileUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PipelineServiceImpl implements PipelineService {

    @Autowired
    private PipelineMapper pipelineMapper;
    @Autowired
    private JenkinsConfigMapper jenkinsConfigMapper;
    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;


    @Autowired
    private JenkinsService jenkinsService;
    @Autowired
    private JenkinsLogService jenkinsLogService;
    @Autowired
    private JenkinsConfigService jenkinsConfigService;

    private final ObjectMapper objectMapper = new ObjectMapper();


    public PipelineServiceImpl(PipelineMapper pipelineMapper) {
        this.pipelineMapper = pipelineMapper;
    }

    @Override
    public Result<String> create(String jobName, String describe, String jenkinsfileContent) {
        try {
            Pipeline pipeline ;
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
                jenkinsService.createPipeline(jobName, jenkinsfileContent,randomPipeLine);
                pipelineMapper.insert(pipeline);
                log.info("创建流水线完成");
            } else {
                log.info("开始更新流水线");
                Pipeline pipeline1 = pipelineMapper.selectOne(new LambdaQueryWrapper<Pipeline>().eq(Pipeline::getName, jobName));
                jenkinsService.updatePipelineJenkinsfile(jobName, jenkinsfileContent,jenkinsConfigService.getJenkinsServer(pipeline.getJenkinsServerId()));
                log.info("更新流水线");
                pipeline1.setStageNum((long) JenkinsfileStageCounter.countStages(jenkinsfileContent));
                pipelineMapper.updateById(pipeline1);
            }
//            log.info("开始启动流水线");
//            jenkinsService.buildPipeline(jobName,jenkinsConfigService.getJenkinsServer(pipeline.getJenkinsServerId()));
//            log.info("启动流水线完毕");
            return Result.success("创建流水线成功");

        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Override
    public Result<String> update(Long pipeLineId, String newName) {
        Pipeline pipeline = pipelineMapper.selectById(pipeLineId);
        if (pipeline == null) {
            return Result.error("流水线不存在!");
        }
        JenkinsConfig jenkinsServer = jenkinsConfigService.getJenkinsServer(pipeline.getJenkinsServerId());
        if (jenkinsServer == null) {
            return Result.error("Jenkins服务器不存在!");
        }
        //更新流水线
        jenkinsService.renamePipeline(pipeline.getName(), newName,jenkinsServer);
        pipeline.setName(newName);
        pipeline.setDeleted(0L);
        pipelineMapper.updateById(pipeline);
        return Result.success("流水线名称更新完毕");
    }

    @Override
    public Result<String> build(Long pipeLineId) {
        Pipeline pipeline = pipelineMapper.selectById(pipeLineId);
        if (pipeline == null) {
            return Result.error("流水线不存在!");
        }
        JenkinsConfig jenkinsServer = jenkinsConfigService.getJenkinsServer(pipeline.getJenkinsServerId());
        if (jenkinsServer == null) {
            return Result.error("Jenkins服务器不存在!");
        }
        jenkinsService.buildPipeline(pipeline.getName(),jenkinsServer);
        return Result.success("流水线启动成功", null);
    }

    @Override
    public Result<String> delete(Long pipeLineId) {
        Pipeline pipeline = pipelineMapper.selectById(pipeLineId);
        if (pipeline == null) {
            return Result.error("流水线不存在!");
        }
        JenkinsConfig jenkinsServer = jenkinsConfigService.getJenkinsServer(pipeline.getJenkinsServerId());
        if (jenkinsServer == null) {
            return Result.error("Jenkins服务器不存在!");
        }

        pipeline.setDeleted(1L);
        pipelineMapper.updateById(pipeline);
        jenkinsService.deletePipeline(pipeline.getName(),jenkinsServer);
        return Result.error("删除流水线成功");
    }


    @Override
    public List<Pipeline> list(String name) {
        return pipelineMapper.selectList(new LambdaQueryWrapper<Pipeline>().like(name != null, Pipeline::getName, name).eq(Pipeline::getDeleted,0));
    }

    @Override
    public Result<Map<String, String>> getPipelineContent(Long pipeLineId) {

        Pipeline pipeline = pipelineMapper.selectById(pipeLineId);
        if (pipeline == null) {
            return Result.error("流水线不存在!");
        }
        JenkinsConfig jenkinsServer = jenkinsConfigService.getJenkinsServer(pipeline.getJenkinsServerId());
        if (jenkinsServer == null) {
            return Result.error("Jenkins服务器不存在!");
        }

        String configXml = jenkinsService.getPipelineConfig(pipeline.getName(),jenkinsServer);
        String jenkinsfile = extractJenkinsfileFromXml(configXml);
        String json = jenkinsService.jenkinsfileToJson(convertToMultipart(jenkinsfile));
        String yaml = jenkinsService.jenkinsfileToYaml(convertToMultipart(jenkinsfile));

        Map<String, String> result = new HashMap<>();
        result.put("jenkinsfile", jenkinsfile);
        result.put("json", extractJsonFromResponse(json));
        result.put("yaml", yaml);

        return Result.success(result);
    }

    @Override
    public Result<Map<String, String>> getJsonAndYamlByJenkinsfile(String jenkinsfileContent) {
        String result = jenkinsService.validateJenkinsfile(StringToMultipartFileUtil.toMultipartFile(jenkinsfileContent, "jenkinsfile"));
        if (!result.contains("successfully validated")) {
            return Result.error(400, "JenkinsFile校验失败!");
        }
        String json = jenkinsService.jenkinsfileToJson(convertToMultipart(jenkinsfileContent));
        String yaml = jenkinsService.jenkinsfileToYaml(convertToMultipart(jenkinsfileContent));

        Map<String, String> resultMap = new HashMap<>();
        resultMap.put("jenkinsfile", jenkinsfileContent);
        resultMap.put("json", extractJsonFromResponse(json));
        resultMap.put("yaml", yaml);
        return Result.success(resultMap);
    }




    private Pipeline selectPipeLineIsExist(String jobName) {
        return pipelineMapper.selectOne(new LambdaQueryWrapper<Pipeline>().eq(Pipeline::getDeleted, 0L).eq(Pipeline::getName,jobName));
    }


    private MultipartFile convertToMultipart(String content) {
        return new MultipartFile() {
            @Override
            public String getName() {
                return "jenkinsfile";
            }

            @Override
            public String getOriginalFilename() {
                return "jenkinsfile";
            }

            @Override
            public String getContentType() {
                return "text/plain";
            }

            @Override
            public boolean isEmpty() {
                return content == null || content.isEmpty();
            }

            @Override
            public long getSize() {
                return content.length();
            }

            @Override
            public byte[] getBytes() {
                return content.getBytes();
            }

            @Override
            public java.io.InputStream getInputStream() {
                return new java.io.ByteArrayInputStream(content.getBytes());
            }

            @Override
            public void transferTo(java.io.File dest) {
                try {
                    java.nio.file.Files.write(dest.toPath(), content.getBytes());
                } catch (java.io.IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private String extractJenkinsfileFromXml(String configXml) {
        try {
            // Handle CDATA section
            if (configXml.contains("<![CDATA[")) {
                int start = configXml.indexOf("<![CDATA[") + 9;
                int end = configXml.indexOf("]]>");
                return configXml.substring(start, end);
            }
            // Handle escaped XML
            int start = configXml.indexOf("<script>");
            int end = configXml.indexOf("</script>");
            if (start < 0 || end < 0) {
                throw new RuntimeException("Jenkinsfile script not found in config");
            }
            String script = configXml.substring(start + 8, end);
            // Unescape XML entities
            return script
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&amp;", "&")
                    .replace("&quot;", "\"")
                    .replace("&apos;", "'");
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract Jenkinsfile from XML: " + e.getMessage(), e);
        }
    }

    private List<PipelineInfo> parsePipelines(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            List<PipelineInfo> pipelines = new ArrayList<>();
            JsonNode jobs = root.get("jobs");
            if (jobs != null && jobs.isArray()) {
                for (JsonNode job : jobs) {
                    pipelines.add(buildPipelineInfo(job));
                }
            } else if (root.has("name")) {
                pipelines.add(buildPipelineInfo(root));
            }
            return pipelines;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse pipelines: " + e.getMessage(), e);
        }
    }

    private PipelineInfo buildPipelineInfo(JsonNode job) {
        PipelineInfo info = new PipelineInfo();
        info.setName(job.has("name") ? job.get("name").asText() : null);
        info.setDescription(job.has("description") ? job.get("description").asText() : null);
        return info;
    }

    private String readFileContent(MultipartFile file) {
        try {
            return new String(file.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file content: " + e.getMessage(), e);
        }
    }

    private String extractJsonFromResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode jsonNode = root.at("/data/json");
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract JSON from response: " + e.getMessage(), e);
        }
    }

    private String extractJenkinsfileFromResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            String jenkinsfile = root.at("/data/jenkinsfile").asText();
            return jenkinsfile.replace("\\n", "\n");
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract Jenkinsfile from response: " + e.getMessage(), e);
        }
    }




























    //=======================================================================================================================================


    @Override
    public Result<List<JenkinsBuild>> getBuildHistory(Long pipeLineId, int depth) {
        Pipeline pipeline = pipelineMapper.selectById(pipeLineId);
        if (pipeline==null){
            return Result.error("获取流水线失败");
        }
        JenkinsConfig jenkinsConfig = jenkinsConfigMapper.selectById(pipeline.getJenkinsServerId());
        if (jenkinsConfig==null){
            return Result.error("根据流水线获取jenkins服务失败");
        }
        String jobName=pipeline.getName();
        List<JenkinsBuild> buildHistory = jenkinsLogService.getBuildHistory(pipeLineId,jobName, depth, jenkinsConfig);
        return Result.success(buildHistory);
    }

    @Override
    public Result<PageResult<JenkinsBuild>> getBuildHistoryByPage(Long pipeLineId, int page, int pageSize) {
        Pipeline pipeline = pipelineMapper.selectById(pipeLineId);
        if (pipeline==null){
            return Result.error("获取流水线失败");
        }
        JenkinsConfig jenkinsConfig = jenkinsConfigMapper.selectById(pipeline.getJenkinsServerId());
        if (jenkinsConfig==null){
            return Result.error("根据流水线获取jenkins服务失败");
        }
        String jobName=pipeline.getName();
        PageResult<JenkinsBuild> buildHistoryByPage = jenkinsLogService.getBuildHistoryByPage(pipeLineId,jobName, page, pageSize, jenkinsConfig);
        return Result.success(buildHistoryByPage);
    }

    @Override
    public Result<BuildStages> getBuildStages(Long pipeLineId, int buildNumber, boolean fetchLog) {
        Pipeline pipeline = pipelineMapper.selectById(pipeLineId);
        if (pipeline==null){
            return Result.error("获取流水线失败");
        }
        JenkinsConfig jenkinsConfig = jenkinsConfigMapper.selectById(pipeline.getJenkinsServerId());
        if (jenkinsConfig==null){
            return Result.error("根据流水线获取jenkins服务失败");
        }
        String jobName=pipeline.getName();
        BuildStages buildStages = jenkinsLogService.getBuildStages(jobName, buildNumber,fetchLog,jenkinsConfig);
        return Result.success(buildStages);
    }

    @Override
    public Result<String> getStageLogById(Long pipeLineId, int buildNumber, String stageId, Integer lines, Integer offset) {
        Pipeline pipeline = pipelineMapper.selectById(pipeLineId);
        if (pipeline==null){
            return Result.error("获取流水线失败");
        }
        JenkinsConfig jenkinsConfig = jenkinsConfigMapper.selectById(pipeline.getJenkinsServerId());
        if (jenkinsConfig==null){
            return Result.error("根据流水线获取jenkins服务失败");
        }
        String jobName=pipeline.getName();
        String stageLogById = jenkinsLogService.getStageLogById(jobName, buildNumber, stageId, lines, offset, jenkinsConfig);
        return Result.success(stageLogById);
    }

    @Override
    public Result<String> getStepLog(Long pipeLineId, int buildNumber, String stepId) {
        Pipeline pipeline = pipelineMapper.selectById(pipeLineId);
        if (pipeline==null){
            return Result.error("获取流水线失败");
        }
        JenkinsConfig jenkinsConfig = jenkinsConfigMapper.selectById(pipeline.getJenkinsServerId());
        if (jenkinsConfig==null){
            return Result.error("根据流水线获取jenkins服务失败");
        }
        String jobName=pipeline.getName();
        String stepLog = jenkinsLogService.getStepLog(jobName, buildNumber, stepId, jenkinsConfig);
        return Result.success(stepLog);
    }

    @Override
    public Result<String> getBuildAllLog(Long pipeLineId, int buildNumber) {
        Pipeline pipeline = pipelineMapper.selectById(pipeLineId);
        if (pipeline==null){
            return Result.error("获取流水线失败");
        }
        JenkinsConfig jenkinsConfig = jenkinsConfigMapper.selectById(pipeline.getJenkinsServerId());
        if (jenkinsConfig==null){
            return Result.error("根据流水线获取jenkins服务失败");
        }
        String jobName=pipeline.getName();
        String buildAllLog = jenkinsLogService.getBuildAllLog(jobName, buildNumber, jenkinsConfig);
        return Result.success(buildAllLog);
    }
}