package com.glowora.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.net.URI;

/**
 * Intelligent DataSource Configuration that seamlessly handles:
 * 1. Railway Internal DATABASE_URL (postgresql://user:pass@postgres.railway.internal:5432/railway)
 * 2. Railway Public DATABASE_PUBLIC_URL (postgresql://user:pass@proxy.rlwy.net:port/railway)
 * 3. Standard JDBC URL from application.properties / SPRING_DATASOURCE_URL
 */
@Configuration
@Slf4j
public class DatabaseConfig {

    @Value("${spring.datasource.url:#{null}}")
    private String configuredUrl;

    @Value("${spring.datasource.username:#{null}}")
    private String configuredUsername;

    @Value("${spring.datasource.password:#{null}}")
    private String configuredPassword;

    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;

    @Bean
    @Primary
    public DataSource dataSource() {
        // Priority 1: DATABASE_PUBLIC_URL (commonly used for connecting from external / local to Railway)
        // Priority 2: DATABASE_URL (Railway standard environment variable)
        // Priority 3: SPRING_DATASOURCE_URL
        // Priority 4: application.properties spring.datasource.url
        String dbUrl = System.getenv("DATABASE_PUBLIC_URL");
        if (!StringUtils.hasText(dbUrl)) {
            dbUrl = System.getenv("DATABASE_URL");
        }
        if (!StringUtils.hasText(dbUrl)) {
            dbUrl = System.getenv("SPRING_DATASOURCE_URL");
        }
        if (!StringUtils.hasText(dbUrl)) {
            dbUrl = configuredUrl;
        }

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName(driverClassName);
        hikariConfig.setConnectionTimeout(10000);
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(2);

        // Check if the URL is in postgresql:// or postgres:// URI format
        if (StringUtils.hasText(dbUrl) && (dbUrl.startsWith("postgres://") || dbUrl.startsWith("postgresql://"))) {
            try {
                URI uri = new URI(dbUrl);
                String host = uri.getHost();
                int port = uri.getPort() != -1 ? uri.getPort() : 5432;
                String path = uri.getPath();
                if (path != null && path.startsWith("/")) {
                    path = path.substring(1);
                }

                String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + (path != null ? path : "");
                hikariConfig.setJdbcUrl(jdbcUrl);

                if (uri.getUserInfo() != null) {
                    String[] userInfo = uri.getUserInfo().split(":", 2);
                    hikariConfig.setUsername(userInfo[0]);
                    if (userInfo.length > 1) {
                        hikariConfig.setPassword(userInfo[1]);
                    }
                } else {
                    hikariConfig.setUsername(configuredUsername);
                    hikariConfig.setPassword(configuredPassword);
                }

                log.info("Initialized PostgreSQL DataSource from Railway DATABASE_URL [host: {}, port: {}, database: {}]",
                        host, port, path);
                return new HikariDataSource(hikariConfig);
            } catch (Exception e) {
                log.warn("Could not parse DATABASE_URL as URI ('{}'), falling back to direct configuration: {}", dbUrl, e.getMessage());
            }
        }

        // Standard JDBC URL fallback
        hikariConfig.setJdbcUrl(dbUrl != null ? dbUrl : configuredUrl);
        hikariConfig.setUsername(configuredUsername);
        hikariConfig.setPassword(configuredPassword);
        return new HikariDataSource(hikariConfig);
    }
}
