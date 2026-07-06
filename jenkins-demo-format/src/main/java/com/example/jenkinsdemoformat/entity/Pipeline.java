package com.example.jenkinsdemoformat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("pipeline")
public class Pipeline {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private Long jenkinsServerId;
    private Long stageNum;
    private Long deleted;
    private String ext1;
    private String ext2;
    private String ext3;
}