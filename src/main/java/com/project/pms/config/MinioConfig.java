package com.project.pms.config;


import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @className: MinioConfig
 * @description: Minio配置类
 * @author: loser
 * @createTime: 2026/2/3 21:30
 */

@Data
@Configuration
@ConfigurationProperties(prefix = "minio") // 读取application.yml中的minio配置
public class MinioConfig {

    /**
     * MinIO服务器地址
     */
    private String endpoint;

    /**
     * 访问密钥
     */
    private String accessKey;

    /**
     * 秘密密钥
     */
    private String secretKey;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 连接超时时间（毫秒）
     */
    private Long connectTimeout = 60000L;

    /**
     * 写入超时时间（毫秒）
     */
    private Long writeTimeout = 60000L;

    /**
     * 读取超时时间（毫秒）
     */
    private Long readTimeout = 60000L;

    /**
     * 创建MinioClient bean
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}

