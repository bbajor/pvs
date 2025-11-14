package de.bbajor.pvs.function.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

/**
 * Database configuration optimized for serverless functions.
 * 
 * Serverless functions have short lifespans, so we need:
 * - Small connection pools (reduce cold start time)
 * - Fast connection timeouts
 * - No connection warming
 */
@Configuration
@Profile("serverless")
public class DatabaseConfig {
    
    /**
     * Serverless-optimized DataSource.
     * 
     * Configuration:
     * - Minimum pool size: 1 (reduce cold start)
     * - Maximum pool size: 5 (small pool for serverless)
     * - Connection timeout: 5 seconds (fast fail)
     * - Idle timeout: 30 seconds (quick cleanup)
     * - Max lifetime: 5 minutes (prevent stale connections)
     */
    @Bean
    @ConditionalOnProperty(name = "spring.datasource.type", havingValue = "com.zaxxer.hikari.HikariDataSource", matchIfMissing = true)
    public DataSource serverlessDataSource(org.springframework.boot.autoconfigure.jdbc.DataSourceProperties properties) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(properties.getUrl());
        config.setUsername(properties.getUsername());
        config.setPassword(properties.getPassword());
        config.setDriverClassName(properties.getDriverClassName());
        
        // Serverless-optimized settings
        config.setMinimumIdle(1);           // Minimal pool for cold starts
        config.setMaximumPoolSize(5);       // Small pool for serverless
        config.setConnectionTimeout(5000);   // 5 seconds - fast fail
        config.setIdleTimeout(30000);       // 30 seconds - quick cleanup
        config.setMaxLifetime(300000);       // 5 minutes - prevent stale connections
        config.setLeakDetectionThreshold(10000); // 10 seconds - detect leaks quickly
        
        // Disable connection warming (not needed for serverless)
        config.setInitializationFailTimeout(-1); // Don't fail on init
        
        return new HikariDataSource(config);
    }
}


