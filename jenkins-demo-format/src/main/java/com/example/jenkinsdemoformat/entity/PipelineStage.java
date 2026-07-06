package com.example.jenkinsdemoformat.entity;

import java.util.List;

/**
 * 流水线阶段实体
 * 对应 Jenkins wfapi 返回的单个阶段信息
 */
public class PipelineStage {

    /**
     * 阶段节点 ID
     * Jenkins 内部的唯一标识，用于查询阶段日志
     */
    private String id;

    /**
     * 阶段名称
     * 对应 Jenkins 中的 stage 名称，如 "Build", "Test", "Deploy"
     */
    private String name;

    /**
     * 阶段状态
     * <p>
     * 取值范围及含义：
     * <ul>
     *   <li><b>SUCCESS</b> - 阶段执行成功，所有操作都正常完成</li>
     *   <li><b>FAILURE</b> - 阶段执行失败，流水线在此阶段中断</li>
     *   <li><b>ABORTED</b> - 阶段被手动中止/取消</li>
     *   <li><b>UNSTABLE</b> - 阶段完成但有警告，如测试失败但不影响后续流程</li>
     *   <li><b>IN_PROGRESS</b> - 阶段正在执行中</li>
     *   <li><b>QUEUED</b> - 阶段在排队等待执行</li>
     *   <li><b>NOT_EXECUTED</b> - 阶段未执行（被跳过）</li>
     *   <li><b>PAUSED_PENDING_INPUT</b> - 阶段等待人工输入或审批</li>
     * </ul>
     *
     * <p>注意：build result 与 stage result 共享同一套状态体系
     */
    private String status;

    /**
     * 阶段耗时（毫秒）
     * 从阶段开始到结束的持续时间
     */
    private long duration;

    /**
     * 阶段执行日志
     * 当 fetchLog=true 时，该字段会填充阶段的具体日志内容
     * 否则为 null
     */
    private String log;

    /**
     * 阶段日志查询 URL
     * 用于按需获取指定阶段的日志内容
     * 完整路径：/jenkins/stage/log?jobName={jobName}&buildNumber={buildNumber}&stageId={id}
     */
    private String logUrl;

    /**
     * 阶段下的步骤列表
     * 每个步骤包含 id 和 name，对应 Jenkins 的 stageFlowNodes
     */
    private List<StepInfo> steps;

    /**
     * 步骤信息
     */
    public static class StepInfo {
        private String id;

        public StepInfo() {}
        public StepInfo(String id) {
            this.id = id;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
    }

    public PipelineStage() {}

    public PipelineStage(String name, String status, long duration) {
        this.name = name;
        this.status = status;
        this.duration = duration;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public String getLog() { return log; }
    public void setLog(String log) { this.log = log; }

    public String getLogUrl() { return logUrl; }
    public void setLogUrl(String logUrl) { this.logUrl = logUrl; }

    public List<StepInfo> getSteps() { return steps; }
    public void setSteps(List<StepInfo> steps) { this.steps = steps; }
}