package com.example.jenkinsdemoformat.service;

import com.example.jenkinsdemoformat.entity.BuildStages;
import com.example.jenkinsdemoformat.entity.JenkinsBuild;
import com.example.jenkinsdemoformat.entity.JenkinsConfig;
import com.example.jenkinsdemoformat.entity.PageResult;
import java.util.List;

/**
 * Jenkins 服务接口
 */
public interface JenkinsLogService {

    /**
     * 查询 Jenkins 流水线的构建历史记录
     *
     * @param jobName 流水线名称
     * @param depth  查询深度，返回最近N条记录
     * @param jenkinsConfig  Jenkins服务器
     * @return 构建历史列表，按构建编号倒序排列
     */
    List<JenkinsBuild> getBuildHistory(Long pipeLineId,String jobName, int depth, JenkinsConfig jenkinsConfig);

    /**
     * 分页查询 Jenkins 流水线的构建历史记录
     *
     * @param jobName   流水线名称
     * @param page      页码（从1开始）
     * @param pageSize  每页记录数
     * @param jenkinsConfig  Jenkins服务器
     * @return 分页结果
     */
    PageResult<JenkinsBuild> getBuildHistoryByPage(Long pipeLineId,String jobName, int page, int pageSize,JenkinsConfig jenkinsConfig);

    /**
     * 查询 Jenkins 流水线指定构建的阶段和日志
     *
     * @param jobName      流水线名称
     * @param buildNumber 构建编号
     * @param fetchLog     是否获取每个阶段的日志
     * @param jenkinsConfig  Jenkins服务器
     * @return 构建阶段信息，包含各阶段名称、状态、耗时和日志
     */
    BuildStages getBuildStages(String jobName, int buildNumber, boolean fetchLog,JenkinsConfig jenkinsConfig);

    /**
     * 查询 Jenkins 流水线指定阶段（单个）的日志
     * <p>
     * 性能优化：独立接口，支持按需获取单个阶段的日志
     *
     * @param jobName      流水线名称
     * @param buildNumber 构建编号
     * @param stageId     阶段节点ID
     * @param lines       返回多少行，null 表示返回全部
     * @param offset      从第几行开始（从0计），null 表示从最后往前取（兼容旧逻辑）
     * @param jenkinsConfig  Jenkins服务器
     * @return 阶段日志内容
     */
    String getStageLogById(String jobName, int buildNumber, String stageId, Integer lines, Integer offset,JenkinsConfig jenkinsConfig);

    /**
     * 查询 Jenkins 流水线指定步骤的日志
     *
     * @param jobName      流水线名称
     * @param buildNumber 构建编号
     * @param stepId      步骤节点ID
     * @param jenkinsConfig  Jenkins服务器
     * @return 步骤日志内容
     */
    String getStepLog(String jobName, int buildNumber, String stepId,JenkinsConfig jenkinsConfig);

    /**
     * 查询 Jenkins 流水线指定构建的全部日志
     *
     * @param jobName      流水线名称
     * @param buildNumber 构建编号
     * @param jenkinsConfig  Jenkins服务器
     * @return 构建的全部日志内容
     */
    String getBuildAllLog(String jobName, int buildNumber, JenkinsConfig jenkinsConfig);
}