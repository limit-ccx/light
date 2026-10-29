package com.zhiguang.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 主从数据源配置（读写分离）
 * <p>
 * - 写操作走 master 数据源
 * - 读操作走 replica 数据源
 * - 通过 ThreadLocal 存储当前数据源类型
 */
@Slf4j
@Configuration
public class DataSourceConfig {

    /**
     * 主数据源（写）
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.master")
    public DataSourceProperties masterDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public HikariDataSource masterDataSource() {
        return masterDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    /**
     * 从数据源（读）
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.replica")
    public DataSourceProperties replicaDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public HikariDataSource replicaDataSource() {
        return replicaDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    /**
     * 动态路由数据源
     */
    @Bean
    @Primary
    public DataSource routingDataSource(DataSource masterDataSource, DataSource replicaDataSource) {
        AbstractRoutingDataSource routingDataSource = new AbstractRoutingDataSource() {
            @Override
            protected Object determineCurrentLookupKey() {
                return DataSourceContextHolder.getDataSourceType();
            }
        };

        Map<Object, Object> targetDataSources = new HashMap<>(2);
        targetDataSources.put(DataSourceType.MASTER, masterDataSource);
        targetDataSources.put(DataSourceType.REPLICA, replicaDataSource);

        routingDataSource.setDefaultTargetDataSource(masterDataSource);
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.afterPropertiesSet();

        log.info("主从数据源配置完成：master={}, replica={}", masterDataSource, replicaDataSource);
        return routingDataSource;
    }

    /**
     * 数据源类型枚举
     */
    public enum DataSourceType {
        MASTER,  // 主库（写）
        REPLICA  // 从库（读）
    }

    /**
     * 数据源上下文持有者（基于 ThreadLocal）
     */
    public static class DataSourceContextHolder {
        private static final ThreadLocal<DataSourceType> contextHolder = new ThreadLocal<>();

        /**
         * 设置数据源类型
         */
        public static void setDataSourceType(DataSourceType type) {
            contextHolder.set(type);
            log.debug("切换数据源: {}", type);
        }

        /**
         * 获取数据源类型
         */
        public static DataSourceType getDataSourceType() {
            DataSourceType type = contextHolder.get();
            return type != null ? type : DataSourceType.MASTER;
        }

        /**
         * 清除数据源类型
         */
        public static void clearDataSourceType() {
            contextHolder.remove();
            log.debug("清除数据源上下文");
        }

        /**
         * 强制使用主库
         */
        public static void useMaster() {
            setDataSourceType(DataSourceType.MASTER);
        }

        /**
         * 强制使用从库
         */
        public static void useReplica() {
            setDataSourceType(DataSourceType.REPLICA);
        }
    }
}