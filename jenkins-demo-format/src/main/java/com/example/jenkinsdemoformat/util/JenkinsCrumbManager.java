package com.example.jenkinsdemoformat.util;

import com.example.jenkinsdemoformat.entity.JenkinsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Jenkins CSRF Crumb 管理器
 * <p>
 * 负责 Jenkins API 的认证管理，包括：
 * <ul>
 *   <li>获取 CSRF Crumb token</li>
 *   <li>crumb 缓存管理（默认 5 分钟有效）</li>
 *   <li>认证失败自动重试</li>
 * </ul>
 *
 * <p>使用方式：
 * <pre>
 * // 获取带 crumb 的认证头
 * HttpHeaders headers = crumbManager.getAuthHeaders();
 *
 * // 执行请求，自动处理认证失败
 * ResponseEntity<String> response = crumbManager.executeWithRetry(request, String.class);
 * </pre>
 *
 * @see <a href="https://www.jenkins.io/doc/book/security/csrf-protection/">Jenkins CSRF Protection</a>
 */
@Component
public class JenkinsCrumbManager {

    private static final Logger log = LoggerFactory.getLogger(JenkinsCrumbManager.class);

    @Autowired
    private RestTemplate restTemplate;

//    /**
//     * Jenkins 服务器地址
//     * 从配置文件读取 {@code jenkins.url}
//     */
//    @Value("${jenkins.url:}")
//    private String jenkinsUrl;

    /**
     * Jenkins 用户名
     * 从配置文件读取 {@code jenkins.username}
     */
//    @Value("${jenkins.username:}")
//    private String username;

    /**
     * Jenkins API Token
     * 从配置文件读取 {@code jenkins.token}
     */
//    @Value("${jenkins.token:}")
//    private String token;

    /** Crumb 缓存 */
    private String cachedCrumb = null;

    /** Crumb 缓存时间（毫秒） */
    private long crumbCacheTime = 0;

    /** Crumb 有效期：5 分钟 */
    private static final long CRUMB_TTL_MS = 5 * 60 * 1000;

    /**
     * 获取 Jenkins CSRF Crumb（带缓存，5分钟有效）
     * <p>
     * 如果缓存有效，直接返回缓存的 crumb；
     * 否则调用 Jenkins /crumbIssuer/api/json 获取新的 crumb 并缓存。
     *
     * @return crumb 字符串，格式为 "Jenkins-Crumb:xxxxxx"，如果获取失败则返回 null
     */
    public String getCrumbHeader(JenkinsConfig jenkinsConfig) {
        // 检查缓存是否有效
        if (cachedCrumb != null && (System.currentTimeMillis() - crumbCacheTime) < CRUMB_TTL_MS) {
            return cachedCrumb;
        }

        try {
            String crumbUrl = jenkinsConfig.getJenkinsIp() + "/crumbIssuer/api/json";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", getAuthHeader(jenkinsConfig));

            RequestEntity<Void> request = new RequestEntity<>(headers, HttpMethod.GET, URI.create(crumbUrl));
            ResponseEntity<java.util.Map> response = restTemplate.exchange(request, java.util.Map.class);

            java.util.Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("crumbRequestField") && body.containsKey("crumb")) {
                String crumb = body.get("crumbRequestField") + ":" + body.get("crumb");
                // 更新缓存
                cachedCrumb = crumb;
                crumbCacheTime = System.currentTimeMillis();
                log.debug("获取新 Crumb 并缓存，当前时间: {}, 过期时间: {}", crumbCacheTime, crumbCacheTime + CRUMB_TTL_MS);
                return crumb;
            }
        } catch (Exception e) {
            log.warn("获取 Crumb 失败，使用无 crumb 模式: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 清除 Crumb 缓存
     * <p>
     * 当收到 401 认证失败响应时调用，清除缓存后下次请求会重新获取 crumb。
     */
    public void invalidateCache() {
        cachedCrumb = null;
        log.info("Crumb 缓存已清除");
    }

    /**
     * 构建 Basic 认证头信息
     * <p>
     * 格式：Authorization: Basic Base64(username:token)
     *
     * @return Basic 认证字符串
     */
    private String getAuthHeader(JenkinsConfig jenkinsConfig) {
        String auth = jenkinsConfig.getUsername() + ":" + jenkinsConfig.getToken();
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 判断响应是否为认证失败（401）
     *
     * @param response HTTP 响应实体
     * @return 如果状态码为 UNAUTHORIZED 返回 true，否则返回 false
     */
    public boolean isUnauthorized(ResponseEntity<?> response) {
        return response != null && response.getStatusCode() == HttpStatus.UNAUTHORIZED;
    }

    /**
     * 执行请求，如果认证失败则清除缓存并重试
     * <p>
     * 流程：
     * <ol>
     *   <li>使用 crumb 执行请求</li>
     *   <li>如果返回 401，说明 crumb 已失效</li>
     *   <li>清除 crumb 缓存</li>
     *   <li>使用纯 Basic 认证重试请求</li>
     * </ol>
     *
     * @param requestEntity 请求实体
     * @param <T>           响应类型
     * @param responseType  响应体类型
     * @return 响应实体（成功或重试后的响应）
     */
    public <T> ResponseEntity<T> executeWithRetry(RequestEntity<Void> requestEntity, Class<T> responseType,JenkinsConfig jenkinsConfig) {
        // 第一次请求
        ResponseEntity<T> response = restTemplate.exchange(requestEntity, responseType);

        // 检测认证失败，清除缓存后重试
        if (isUnauthorized(response)) {
            log.warn("Jenkins API 认证失败，清除 crumb 缓存后重试");
            invalidateCache();

            // 重新构建请求（不带 crumb，因为 crumb 已失效）
            HttpHeaders retryHeaders = new HttpHeaders();
            retryHeaders.set("Authorization", getAuthHeader(jenkinsConfig));

            // 从原始请求中获取 URI 和 Method
            URI uri = requestEntity.getUrl();
            HttpMethod method = requestEntity.getMethod();

            RequestEntity<Void> retryRequest = new RequestEntity<>(retryHeaders, method, uri);
            response = restTemplate.exchange(retryRequest, responseType);
        }

        return response;
    }

    /**
     * 获取认证头（包含 crumb）
     * <p>
     * 返回的 HttpHeaders 包含：
     * <ul>
     *   <li>Authorization: Basic Base64(username:token)</li>
     *   <li>Jenkins-Crumb: xxxxxx（如果 crumb 获取成功）</li>
     * </ul>
     *
     * @return 包含认证信息的 HttpHeaders
     */
    public HttpHeaders getAuthHeaders(JenkinsConfig jenkinsConfig) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", getAuthHeader(jenkinsConfig));

        String crumb = getCrumbHeader(jenkinsConfig);
        if (crumb != null && crumb.contains(":")) {
            String[] parts = crumb.split(":", 2);
            headers.set(parts[0], parts[1]);
        }

        return headers;
    }

    /**
     * 对 jobName 进行 URL 编码
     * 处理中文等特殊字符，避免 URI 构建失败
     *
     * @param jobName 流水线名称
     * @return 编码后的流水线名称
     */
    public String encodeJobName(String jobName) {
        if (jobName == null) {
            return null;
        }
        try {
            return java.net.URLEncoder.encode(jobName, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            log.warn("编码 jobName 失败: {}", jobName, e);
            return jobName;
        }
    }

    /**
     * 获取仅包含 Basic 认证的头（不带 crumb，用于重试）
     * <p>
     * 当 crumb 失效后重试请求时使用，此时不需要crumb
     *
     * @return 仅包含 Basic 认证的 HttpHeaders
     */
    public HttpHeaders getBasicAuthHeaders(JenkinsConfig jenkinsConfig) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", getAuthHeader(jenkinsConfig));
        return headers;
    }
}