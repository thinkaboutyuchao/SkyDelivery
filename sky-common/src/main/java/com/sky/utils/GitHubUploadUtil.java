package com.sky.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * GitHub 文件上传工具类
 */
@Data
@AllArgsConstructor
@Slf4j
public class GitHubUploadUtil {

    private String owner;        // GitHub 用户名
    private String repo;         // GitHub 仓库名
    private String branch;       // GitHub 分支名
    private String token;        // GitHub Token

    /**
     * 文件上传到 GitHub 仓库
     *
     * @param bytes      文件内容（字节数组）
     * @param objectName 上传到 GitHub 中的文件路径 (例如：images/example.png)
     * @return 文件的原始访问地址
     */
    public String upload(byte[] bytes, String objectName) {

        // 构建上传 API 地址
        String apiUrl = String.format("https://api.github.com/repos/%s/%s/contents/%s", owner, repo, objectName);
        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        // 构建请求体
        Map<String, String> body = new HashMap<>();
        body.put("message", "Upload file: " + objectName);
        body.put("content", Base64.getEncoder().encodeToString(bytes));
        body.put("branch", branch);

        RestTemplate restTemplate = new RestTemplate();

        try {
            // 发送 PUT 请求到 GitHub API
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.PUT, request, String.class);
            // 检查响应状态
            if (response.getStatusCode() == HttpStatus.CREATED) {
                // 文件上传成功，返回文件的原始访问地址
                String fileUrl = String.format("https://github.com/%s/%s/raw/%s/%s", owner, repo, branch, objectName);

                log.info("文件成功上传到 GitHub: {}", fileUrl);
                return fileUrl;
            } else {
                log.error("文件上传失败，响应状态: {}", response.getStatusCode());
                throw new RuntimeException("GitHub 文件上传失败: " + response.getBody());
            }
        } catch (Exception e) {
            log.error("GitHub 文件上传过程中发生异常: {}", e.getMessage(), e);
            throw new RuntimeException("GitHub 文件上传失败", e);
        }
    }
}
