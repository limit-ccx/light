package com.zhiguang.config;

import com.zhiguang.config.VectorProperties;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * PostgreSQL + pgvector 配置
 * <p>
 * 用于向量检索，与 MySQL 主数据库分离。
 * 使用 HikariCP 连接池：此前 DriverManagerDataSource 无连接池，
 * 每次向量操作都新建 TCP + 认证，并发下会造成 Postgres 连接风暴。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class VectorDatabaseConfig {

    private final VectorProperties vectorProperties;

    @Bean
    public DataSource vectorDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setJdbcUrl(String.format("jdbc:postgresql://%s:%d/%s",
                vectorProperties.getPostgres().getHost(),
                vectorProperties.getPostgres().getPort(),
                vectorProperties.getPostgres().getDatabase()));
        dataSource.setUsername(vectorProperties.getPostgres().getUsername());
        dataSource.setPassword(vectorProperties.getPostgres().getPassword());
        // 向量库只承载检索写入，小池即可
        dataSource.setMaximumPoolSize(8);
        dataSource.setMinimumIdle(2);
        dataSource.setConnectionTimeout(5_000);
        dataSource.setPoolName("HikariVector");

        log.info("PostgreSQL 向量数据库初始化完成: host={}, port={}, database={}",
                vectorProperties.getPostgres().getHost(),
                vectorProperties.getPostgres().getPort(),
                vectorProperties.getPostgres().getDatabase());

        return dataSource;
    }

    /**
     * 复用连接池的 JdbcTemplate 单例。
     * 必须显式 @Qualifier：容器中 routingDataSource 是 @Primary，
     * 仅靠参数名无法在存在 primary 时按名匹配——此前因此被注入 MySQL 数据源，
     * 全部 pgvector SQL 静默失败（向量功能实际不可用的根因）。
     */
    @Bean
    public JdbcTemplate vectorJdbcTemplate(@Qualifier("vectorDataSource") DataSource vectorDataSource) {
        return new JdbcTemplate(vectorDataSource);
    }
}
