package com.zhiguang.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 向量检索配置属性
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "vector")
public class VectorProperties {

    private String provider = "pgvector";
    private PostgresConfig postgres = new PostgresConfig();

    @Data
    public static class PostgresConfig {
        private String host = "localhost";
        private int port = 5432;
        private String database = "zhiguang_vectors";
        private String username = "postgres";
        private String password = "postgres123";
        private int dimension = 1024;  // Embedding 维度（须与 ai.embedding.dimension 一致，默认 bge-m3=1024）
    }
}