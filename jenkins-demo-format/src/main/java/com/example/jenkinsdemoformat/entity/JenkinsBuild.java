package com.example.jenkinsdemoformat.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.Date;

/**
 * Jenkins 构建记录实体类
 * 用于存储 Jenkins 流水线的构建历史信息
 */
@Data
public class JenkinsBuild {

    /**
     * 构建编号
     * Jenkins 分配的递增构建序号
     */
    private Integer number;

    /**
     * 构建详情URL
     * Jenkins 中该构建的访问链接
     */
    private String url;

    /**
     * 构建结果
     * 常见值：
     * - SUCCESS：构建成功
     * - FAILURE：构建失败
     * - ABORTED：构建被中止/取消
     * - UNSTABLE：构建不稳定（测试失败或有警告）
     * - NOT_BUILT：未构建（流水线被跳过）
     * - IN_PROGRESS：构建中
     * - QUEUED：排队中
     */
    private String result;

    /**
     * 构建触发时间
     * 格式：yyyy-MM-dd HH:mm:ss
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date timestamp;

    /**
     * 构建耗时
     * 人类可读格式，如：5m 30s
     */
    private String duration;

    /**
     * 流水线名称
     * 对应 Jenkins 中的 Job 名称
     */
    private String pipeline;

    /**
     * 构建人
     * 触发本次构建的用户ID
     */
    private String builder;

    /**
     * 阶段详情 URL
     * 用于查询该构建的流水线阶段和日志
     * 完整路径：/jenkins/stages?jobName={pipeline}&buildNumber={number}&fetchLog=false
     */
    private String stagesUrl;
}