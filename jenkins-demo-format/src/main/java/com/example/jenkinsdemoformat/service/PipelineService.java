package com.example.jenkinsdemoformat.service;

import com.example.jenkinsdemoformat.common.Result;
import com.example.jenkinsdemoformat.entity.*;

import java.util.List;
import java.util.Map;

public interface PipelineService {
    Result<String> create(String jobName, String describe,String jenkinsfileContent);
    Result<String> update(Long pipeLineId, String newName);
    Result<String> build(Long pipeLineId);
    Result<String> delete(Long id);
    List<Pipeline> list(String name);
    Result<Map<String, String>> getPipelineContent(Long pipeLineId);
    Result<Map<String, String>> getJsonAndYamlByJenkinsfile(String jenkinsfileContent);




    //====================================================================================================================
    /**
     * 查询 Jenkins 流水线的构建历史记录
     *
     * @param pipeLineId 流水线Id
     * @param depth  查询深度，返回最近N条记录
     * @return 构建历史列表，按构建编号倒序排列
     */
    Result<List<JenkinsBuild>> getBuildHistory(Long pipeLineId, int depth);

    /**
     * 分页查询 Jenkins 流水线的构建历史记录
     *
     * @param pipeLineId 流水线Id
     * @param page      页码（从1开始）
     * @param pageSize  每页记录数
     * @return 分页结果
     */
    Result<PageResult<JenkinsBuild>> getBuildHistoryByPage(Long pipeLineId, int page, int pageSize);


    /**
     * 查询 Jenkins 流水线指定构建的阶段和日志
     *
     * @param pipeLineId 流水线Id
     * @param buildNumber 构建编号
     * @param fetchLog     是否获取每个阶段的日志
     * @return 构建阶段信息，包含各阶段名称、状态、耗时和日志
     */
    Result<BuildStages> getBuildStages(Long pipeLineId, int buildNumber, boolean fetchLog);


    /**
     * 查询 Jenkins 流水线指定阶段（单个）的日志
     * <p>
     * 性能优化：独立接口，支持按需获取单个阶段的日志
     *
     * @param pipeLineId 流水线Id
     * @param buildNumber 构建编号
     * @param stageId     阶段节点ID
     * @param lines       返回多少行，null 表示返回全部
     * @param offset      从第几行开始（从0计），null 表示从最后往前取（兼容旧逻辑）
     * @return 阶段日志内容
     */
    Result<String> getStageLogById(Long pipeLineId, int buildNumber, String stageId, Integer lines, Integer offset);

    /**
     * 查询 Jenkins 流水线指定步骤的日志
     *
     * @param pipeLineId 流水线Id
     * @param buildNumber 构建编号
     * @param stepId      步骤节点ID
     * @return 步骤日志内容
     */
    Result<String> getStepLog(Long pipeLineId, int buildNumber, String stepId);

    /**
     * 查询 Jenkins 流水线指定构建的全部日志
     *
     * @param pipeLineId 流水线Id
     * @param buildNumber 构建编号
     * @return 构建的全部日志内容
     */
    Result<String> getBuildAllLog(Long pipeLineId, int buildNumber);

}