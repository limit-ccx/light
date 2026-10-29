package com.zhiguang.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch 配置
 * <p>
 * 使用 Elasticsearch Java API Client 8.x
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ElasticsearchConfig {

    private final ElasticsearchProperties elasticsearchProperties;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        // 创建 RestClient
        RestClientBuilder builder = RestClient.builder(
                new HttpHost(elasticsearchProperties.getHost(), elasticsearchProperties.getPort())
        );

        // 配置认证
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(
                        elasticsearchProperties.getUsername(),
                        elasticsearchProperties.getPassword()
                )
        );

        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            return httpClientBuilder;
        });

        RestClient restClient = builder.build();

        // 创建 ElasticsearchTransport
        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper()
        );

        // 创建 ElasticsearchClient
        ElasticsearchClient client = new ElasticsearchClient(transport);

        log.info("Elasticsearch 客户端初始化完成: host={}, port={}", 
                elasticsearchProperties.getHost(), elasticsearchProperties.getPort());

        return client;
    }
}