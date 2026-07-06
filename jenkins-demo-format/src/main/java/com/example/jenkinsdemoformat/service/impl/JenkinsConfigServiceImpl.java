package com.example.jenkinsdemoformat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.jenkinsdemoformat.dto.JenkinsConfigRequest;
import com.example.jenkinsdemoformat.entity.JenkinsConfig;
import com.example.jenkinsdemoformat.mapper.JenkinsConfigMapper;
import com.example.jenkinsdemoformat.service.JenkinsConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JenkinsConfigServiceImpl implements JenkinsConfigService {

    private final JenkinsConfigMapper mapper;

    @Override
    public void create(JenkinsConfigRequest request) {
        JenkinsConfig config = new JenkinsConfig();
        config.setJenkinsIp(request.getJenkinsIp());
        config.setUsername(request.getUsername());
        config.setPassword(request.getPassword());
        config.setToken(request.getToken());
        config.setDeleted(0);
        mapper.insert(config);
    }

    @Override
    public void update(JenkinsConfigRequest request) {
        JenkinsConfig config = mapper.selectById(request.getId());
        if (config != null) {
            config.setJenkinsIp(request.getJenkinsIp());
            config.setUsername(request.getUsername());
            config.setPassword(request.getPassword());
            config.setToken(request.getToken());
            mapper.updateById(config);
        }
    }

    @Override
    public void delete(Long id) {
        JenkinsConfig config = mapper.selectById(id);
        if (config != null) {
            config.setDeleted(1);
            mapper.updateById(config);
        }
    }

    @Override
    public List<JenkinsConfig> list() {
        return mapper.selectList(new LambdaQueryWrapper<JenkinsConfig>().eq(JenkinsConfig::getDeleted,0L));
    }

    @Override
    public JenkinsConfig getJenkinsServer(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public JenkinsConfig getRandomPipeLine() {
        return mapper.selectList(null).get(0);
    }
}