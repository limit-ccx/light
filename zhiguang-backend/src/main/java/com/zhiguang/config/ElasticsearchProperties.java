package com.zhiguang.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch 配置属性
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "elasticsearch")
public class ElasticsearchProperties {

    private String host = "localhost";
    private int port = 9200;
    private String username = "elastic";
    private String password = "elastic123";
    private IndexConfig index = new IndexConfig();

    @Data
    public static class IndexConfig {
        private String article = "zhiguang_articles";
    }
}