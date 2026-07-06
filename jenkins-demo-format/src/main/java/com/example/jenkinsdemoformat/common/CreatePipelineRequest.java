package com.example.jenkinsdemoformat.common;

import lombok.Data;

@Data
public class CreatePipelineRequest {

    private String jobName;

    private String description;

    private String jenkinsfileContent;
}