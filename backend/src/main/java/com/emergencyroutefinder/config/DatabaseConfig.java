package com.emergencyroutefinder.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * Database Configuration — uses HikariCP connection pool with plain JDBC.
 * All DAO classes use JdbcTemplate + PreparedStatement (not JPA/ORM).
 *
 * Demonstrates: JDBC, Connection Pooling, Spring Configuration
 */
@Configuration
@EnableTransactionManagement
public class DatabaseConfig {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.hikari.pool-name:ERF-Pool}")
    private String poolName;

    @Value("${spring.datasource.hikari.maximum-pool-size:15}")
    private int maxPoolSize;

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        String effectiveUrl = jdbcUrl;
        if (effectiveUrl != null && effectiveUrl.startsWith("mysql://")) {
            effectiveUrl = "jdbc:" + effectiveUrl;
        }
        config.setJdbcUrl(effectiveUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setPoolName(poolName);
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(3);
        config.setIdleTimeout(300_000);
        config.setConnectionTimeout(20_000);
        config.setAutoCommit(true);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        return new HikariDataSource(config);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
