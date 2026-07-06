package com.example.jenkinsdemoformat.service;

import com.example.jenkinsdemoformat.dto.JenkinsConfigRequest;
import com.example.jenkinsdemoformat.entity.JenkinsConfig;

import java.util.List;

public interface JenkinsConfigService {
    void create(JenkinsConfigRequest request);
    void update(JenkinsConfigRequest jenkinsConfigRequest);
    void delete(Long id);
    List<JenkinsConfig> list();
    JenkinsConfig getRandomPipeLine();
    JenkinsConfig getJenkinsServer(Long id);
}