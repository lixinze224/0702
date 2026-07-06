package com.example.jenkinsdemoformat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("jenkins_config")
public class JenkinsConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String jenkinsIp;
    private String username;
    private String password;
    private Integer deleted;
    private String token;
    private String ext1;
    private String ext2;
    private String ext3;
}