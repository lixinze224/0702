package com.example.jenkinsdemoformat.controller;

import com.example.jenkinsdemoformat.common.Result;
import com.example.jenkinsdemoformat.dto.JenkinsConfigRequest;
import com.example.jenkinsdemoformat.entity.JenkinsConfig;
import com.example.jenkinsdemoformat.service.JenkinsConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jenkins-config")
@RequiredArgsConstructor
public class JenkinsConfigController {

    private final JenkinsConfigService service;

    @PostMapping
    public Result<String> create(@RequestBody JenkinsConfigRequest request) {
        service.create(request);
        return Result.success("操作成功");
    }

    @PutMapping("/{id}")
    public Result<String> update(@RequestBody JenkinsConfigRequest request) {
        service.update(request);
        return Result.success("操作成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.success("操作成功");
    }

    @GetMapping
    public Result<List<JenkinsConfig>> list() {
        return Result.success(service.list());
    }
}