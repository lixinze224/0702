package com.example.jenkinsdemoformat.dto;

import lombok.Data;

@Data
public class JenkinsConfigRequest {
    private Long id;
    private String jenkinsIp;
    private String username;
    private String password;
    private String token;
}