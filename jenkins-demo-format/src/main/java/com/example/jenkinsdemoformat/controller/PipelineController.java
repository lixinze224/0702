package com.example.jenkinsdemoformat.controller;

import com.example.jenkinsdemoformat.common.Result;
import com.example.jenkinsdemoformat.entity.BuildStages;
import com.example.jenkinsdemoformat.entity.JenkinsBuild;
import com.example.jenkinsdemoformat.entity.PageResult;
import com.example.jenkinsdemoformat.entity.Pipeline;
import com.example.jenkinsdemoformat.service.JenkinsService;
import com.example.jenkinsdemoformat.service.PipelineService;
import com.example.jenkinsdemoformat.util.StringToMultipartFileUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pipeline")
public class PipelineController {

    @Autowired
    private PipelineService pipelineService;
    @Autowired
    private JenkinsService jenkinsService;


    @PostMapping(value = "/validateJenkinsfile", consumes = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "校验JenkinsFile格式", description = "校验JenkinsFile格式")
    public Result<String> validateJenkinsfile(
            @Parameter(description = "Jenkinsfile content", required = true)
            @RequestBody String jenkinsfile) {
        try {
            String result = jenkinsService.validateJenkinsfile(
                    StringToMultipartFileUtil.toMultipartFile(jenkinsfile, "jenkinsfile"));
            if (result.contains("successfully validated")) {
                return Result.success("Jenkinsfile 校验成功", result);
            } else {
                return Result.error(400, result);
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }


    @PostMapping(value = "/getJsonAndYamlByJenkinsfile", consumes = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "根据JenkinsFile获取JSON文件和yaml文件", description = "根据JenkinsFile获取JSON文件和yaml文件")
    public Result<Map<String, String>> getJsonAndYamlByJenkinsfile(
            @Parameter(description = "Jenkinsfile content", required = true)
            @RequestBody String jenkinsfile) {
            return pipelineService.getJsonAndYamlByJenkinsfile(jenkinsfile);
    }

    @PostMapping(value = "/create", consumes = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "创建流水线",
            description = "创建流水线")
    public Result<String> createPipeline(
            @Parameter(description = "Job name", required = true)
            @RequestParam("jobName") String jobName,
            @Parameter(description = "描述", required = true)
            @RequestParam("describe") String describe,
            @Parameter(description = "Jenkinsfile content", required = true)
            @RequestBody String jenkinsfile) {
        return pipelineService.create(jobName,describe, jenkinsfile);
    }


    @PostMapping("/rename")
    @Operation(summary = "修改流水线名称", description = "修改流水线名称")
    public Result<String> renamePipeline(
            @RequestParam("pipeLineId") Long pipeLineId,
            @RequestParam("newName") String newName) {

        return pipelineService.update(pipeLineId, newName);
    }


    @PostMapping("/build")
    @Operation(summary = "启动流水线", description = "启动流水线")
    public Result<String> buildPipeline(@RequestParam("pipeLineId") Long pipeLineId) {
        return pipelineService.build(pipeLineId);
    }


    @DeleteMapping("/delete")
    @Operation(summary = "删除流水线名称", description = "删除流水线名称")
    public Result<String> deletePipeline(@RequestParam("pipeLineId") Long pipeLineId) {
        return pipelineService.delete(pipeLineId);
    }


    @GetMapping("/pipelines")
    @Operation(summary = "查询所有的流水线", description = "查询所有的流水线")
    public Result<List<Pipeline>> listPipelines(
            @Parameter(description = "Pipeline name (optional)")
            @RequestParam(value = "name", required = false) String name) {
        return Result.success(pipelineService.list(name));
    }


    @GetMapping("/pipeline/content")
    @Operation(summary = "根据流水线名称获取流水线的JenkinsFile内容", description = "根据流水线名称获取流水线的JenkinsFile内容")
    public Result<Map<String, String>> getPipelineContent(@RequestParam("pipeLineId") Long pipeLineId) {
        return pipelineService.getPipelineContent(pipeLineId);
    }

    //====================================================================================================================================================================

    /**
     * 查询 Jenkins 流水线的构建历史记录（多次运行记录）
     *
     * @param pipeLineId 流水线Id（必填）
     * @param depth  返回记录数量，不传则查询所有记录（可选）
     * @return 构建历史列表，按构建编号倒序排列（最新构建在前）
     */
    @GetMapping("/builds")
    public Result<List<JenkinsBuild>> getBuilds(@RequestParam Long pipeLineId,
                                                @RequestParam(required = false) Integer depth) {
        // 调用service层方法，获取指定流水线的构建历史
        // depth为空时传Integer.MAX_VALUE，表示查询所有记录
        return  pipelineService.getBuildHistory(pipeLineId, depth == null ? Integer.MAX_VALUE : depth);
    }

    /**
     * 分页查询 Jenkins 流水线的构建历史记录
     *
     * @param pipeLineId  流水线Id（必填）
     * @param page     页码，从1开始（必填）
     * @param pageSize 每页记录数（必填）
     * @return 分页结果，包含记录列表、总记录数、当前页、每页大小
     */
    @GetMapping("/builds/page")
    public Result<PageResult<JenkinsBuild>> getBuildsByPage(@RequestParam Long pipeLineId,
                                                            @RequestParam int page,
                                                            @RequestParam int pageSize) {
        return pipelineService.getBuildHistoryByPage(pipeLineId, page, pageSize);
    }


    /**
     * 查询 Jenkins 流水线的最新一次构建记录
     *
     * @param pipeLineId 流水线Id（必填）
     * @return 最新构建记录，若无记录则返回null
     */
    @GetMapping("/latest")
    public Result<JenkinsBuild> getLatestBuild(@RequestParam Long pipeLineId) {
        // 调用service层方法，传入depth=1只获取最新一条记录
        Result<List<JenkinsBuild>> buildHistory = pipelineService.getBuildHistory(pipeLineId, 1);
        // 判断是否有记录，有则返回第一条（最新），无则返回null
        return Result.success(buildHistory.getData().isEmpty() ? null : buildHistory.getData().get(0));
    }


    /**
     * 查询 Jenkins 流水线指定构建的阶段和日志
     *
     * @param pipeLineId      流水线Id（必填）
     * @param buildNumber 构建编号（必填）
     * @param fetchLog     是否获取每个阶段的日志，默认 false（可选）
     * @return 构建阶段信息，包含各阶段名称、状态、耗时和日志
     */
    @GetMapping("/stages")
    public Result<BuildStages> getBuildStages(@RequestParam Long pipeLineId,
                                              @RequestParam int buildNumber,
                                              @RequestParam(required = false, defaultValue = "false") Boolean fetchLog) {
        return pipelineService.getBuildStages(pipeLineId, buildNumber, fetchLog);
    }


    /**
     * 查询 Jenkins 流水线指定阶段（单个）的日志
     * <p>
     * 性能优化：将日志查询独立出来，用户可以按需获取
     *
     * @param pipeLineId  流水线Id（必填）
     * @param buildNumber 构建编号（必填）
     * @param stageId 阶段节点ID（必填），可通过 /stages 接口获取
     * @return 阶段日志内容
     */
    @GetMapping("/stage/log")
    public Result<String> getStageLog(@RequestParam Long pipeLineId,
                                      @RequestParam int buildNumber,
                                      @RequestParam String stageId,
                                      @RequestParam(required = false) Integer lines,
                                      @RequestParam(required = false) Integer offset) {
        return pipelineService.getStageLogById(pipeLineId, buildNumber, stageId, lines, offset);
    }


    /**
     * 查询 Jenkins 流水线指定步骤的日志
     *
     * @param pipeLineId      流水线Id（必填）
     * @param buildNumber 构建编号（必填）
     * @param stepId       步骤节点ID（必填），可通过 /stages 接口获取
     * @return 步骤日志内容
     */
    @GetMapping("/step/log")
    public Result<String> getStepLog(@RequestParam Long pipeLineId,
                                     @RequestParam int buildNumber,
                                     @RequestParam String stepId) {
        return pipelineService.getStepLog(pipeLineId, buildNumber, stepId);
    }

    /**
     * 查询 Jenkins 流水线指定构建的全部日志
     *
     * @param pipeLineId  流水线Id（必填）
     * @param buildNumber 构建编号（必填）
     * @return 构建的全部日志内容
     */
    @GetMapping("/build/log")
    public Result<String> getBuildAllLog(@RequestParam Long pipeLineId,
                                         @RequestParam int buildNumber) {
        return pipelineService.getBuildAllLog(pipeLineId, buildNumber);
    }

    /**
     * 下载 Jenkins 流水线指定构建的全部日志
     *
     * @param pipeLineId  流水线Id（必填）
     * @param buildNumber 构建编号（必填）
     * @return 日志文件，可直接下载
     */
    @GetMapping("/build/log/download")
    public ResponseEntity<byte[]> downloadBuildLog(@RequestParam Long pipeLineId,
                                                   @RequestParam int buildNumber) {
        Result<String> result = pipelineService.getBuildAllLog(pipeLineId, buildNumber);

        if (result.getData() == null) {
            return ResponseEntity.notFound().build();
        }

        String logContent = result.getData();
        String filename = "build_" + buildNumber + "_log.txt";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(logContent.getBytes(StandardCharsets.UTF_8).length);

        return new ResponseEntity<>(logContent.getBytes(StandardCharsets.UTF_8), headers, org.springframework.http.HttpStatus.OK);
    }

}