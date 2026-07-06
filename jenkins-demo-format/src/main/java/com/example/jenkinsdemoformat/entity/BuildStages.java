package com.example.jenkinsdemoformat.entity;

import java.util.List;

/**
 * 构建阶段信息
 * 包含指定流水线的指定构建的所有阶段信息
 */
public class BuildStages {

    /**
     * 构建编号
     * Jenkins 中的构建号，如 11, 23 等
     */
    private int buildNumber;

    /**
     * 流水线名称
     * 对应 Jenkins 中的 Job 名称，如 "zml0512pt-1"
     * 多分支流水线可能是 "parentJob/job/branchName"
     */
    private String jobName;

    /**
     * 阶段列表
     * 包含该构建的所有阶段，按执行顺序排列
     */
    private List<PipelineStage> stages;

    public BuildStages() {}

    public BuildStages(int buildNumber, String jobName, List<PipelineStage> stages) {
        this.buildNumber = buildNumber;
        this.jobName = jobName;
        this.stages = stages;
    }

    /**
     * 获取构建编号
     * @return 构建编号
     */
    public int getBuildNumber() { return buildNumber; }
    public void setBuildNumber(int buildNumber) { this.buildNumber = buildNumber; }

    /**
     * 获取流水线名称
     * @return 流水线名称
     */
    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }

    /**
     * 获取阶段列表
     * @return 阶段列表
     */
    public List<PipelineStage> getStages() { return stages; }
    public void setStages(List<PipelineStage> stages) { this.stages = stages; }
}