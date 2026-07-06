package com.example.jenkinsdemoformat.service;

import com.example.jenkinsdemoformat.entity.JenkinsConfig;
import org.springframework.web.multipart.MultipartFile;

public interface JenkinsService {

    String jenkinsfileToJson(MultipartFile jenkinsfile);


    String jenkinsfileToYaml(MultipartFile jenkinsfile);

    String jsonToJenkinsfile(MultipartFile jsonFile);

    String yamlToJenkinsfile(MultipartFile yamlFile);

    String validateJenkinsfile(MultipartFile jenkinsfile);

    void deletePipeline(String jobName, JenkinsConfig jenkinsConfig);

    void renamePipeline(String oldJobName, String newJobName,JenkinsConfig jenkinsConfig);

    void buildPipeline(String jobName, JenkinsConfig jenkinsConfig);

    String createPipeline(String jobName, String jenkinsfileContent,JenkinsConfig jenkinsConfig);

    String listPipelines(String name,JenkinsConfig jenkinsConfig);

    String getPipelineConfig(String jobName,JenkinsConfig jenkinsConfig);

    String updatePipelineJenkinsfile(String jobName, String jenkinsfileContent,JenkinsConfig jenkinsConfig);
}