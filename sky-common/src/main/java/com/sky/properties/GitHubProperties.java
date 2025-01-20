package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sky.github")
@Data
public class GitHubProperties {
    private String owner;
    private String repo;
    private String branch;
    private String token;
}
