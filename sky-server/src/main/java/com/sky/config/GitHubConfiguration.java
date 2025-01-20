package com.sky.config;

import com.sky.properties.GitHubProperties;
import com.sky.utils.GitHubUploadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class GitHubConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public GitHubUploadUtil gitHubUploadUtil(GitHubProperties gitHubProperties){
        log.info("开始创建Github传输工具：{}",gitHubProperties);
        return new GitHubUploadUtil(
                gitHubProperties.getOwner(),
                gitHubProperties.getRepo(),
                gitHubProperties.getBranch(),
                gitHubProperties.getToken());
    };
}
