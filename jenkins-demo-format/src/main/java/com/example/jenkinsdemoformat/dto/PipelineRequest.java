package com.example.jenkinsdemoformat.dto;

import lombok.Data;

@Data
public class PipelineRequest {
    private String name;
    private String description;
    private Long jenkinsServerId;
    private String ext1;
    private String ext2;
    private String ext3;
}