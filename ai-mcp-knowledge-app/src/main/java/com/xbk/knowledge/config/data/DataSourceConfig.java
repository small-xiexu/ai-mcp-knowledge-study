package com.xbk.knowledge.config.data;

import com.alibaba.druid.pool.DruidDataSource;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

/**
 * 数据源配置
 * 拆分 MySQL（业务数据）与 PostgreSQL（向量存储）
 *
 * 职责：应用装配配置，用于集中接入基础设施能力
 * @author sxie
 */
@Configuration
public class DataSourceConfig {

    /**
     * MySQL 数据源配置属性
     *
     * @return 配置属性
     */
    @Bean
    @Qualifier("mysqlDataSourceProperties")
    @ConfigurationProperties("spring.datasource.mysql")
    public DataSourceProperties mysqlDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * MySQL 数据源
     *
     * @return MySQL 数据源
     */
    @Bean(name = "mysqlDataSource")
    @ConfigurationProperties("spring.datasource.mysql.druid")
    public DataSource mysqlDataSource(@Qualifier("mysqlDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(DruidDataSource.class)
                .build();
    }

    /**
     * PostgreSQL(pgvector) 数据源配置属性
     *
     * @return 配置属性
     */
    @Bean
    @Primary
    @Qualifier("pgvectorDataSourceProperties")
    @ConfigurationProperties("spring.datasource.pgvector")
    public DataSourceProperties pgvectorDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * PostgreSQL(pgvector) 数据源
     *
     * @return PostgreSQL 数据源
     */
    @Bean(name = "pgvectorDataSource")
    @Primary
    @ConfigurationProperties("spring.datasource.pgvector.hikari")
    public DataSource pgvectorDataSource(@Qualifier("pgvectorDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    /**
     * PostgreSQL(pgvector) JDBC 模板
     *
     * @param dataSource pgvector 数据源
     * @return JdbcTemplate
     */
    @Bean
    @Primary
    public JdbcTemplate pgvectorJdbcTemplate(@Qualifier("pgvectorDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * MySQL 事务管理器
     *
     * @param dataSource MySQL 数据源
     * @return 事务管理器
     */
    @Bean(name = "transactionManager")
    @Primary
    public DataSourceTransactionManager transactionManager(@Qualifier("mysqlDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
