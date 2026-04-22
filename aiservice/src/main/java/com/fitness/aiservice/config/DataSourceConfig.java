package com.fitness.aiservice.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.ai.url}")
    private String url;

    @Value("${spring.datasource.ai.username}")
    private String username;

    @Value("${spring.datasource.ai.password}")
    private String password;

    // ✅ Converts postgres:// to jdbc:postgresql:// automatically
    private String normalizeUrl(String rawUrl) {
        if (rawUrl == null) {
            throw new IllegalArgumentException("Database URL cannot be null");
        }

        rawUrl = rawUrl.trim();

        // Already correct
        if (rawUrl.startsWith("jdbc:postgresql://")) {
            return rawUrl;
        }

        // Render format: postgres://
        if (rawUrl.startsWith("postgres://")) {
            return rawUrl.replace("postgres://", "jdbc:postgresql://");
        }

        // Render format: postgresql://
        if (rawUrl.startsWith("postgresql://")) {
            return rawUrl.replace("postgresql://", "jdbc:postgresql://");
        }

        throw new IllegalArgumentException(
                "Cannot normalize DB URL: " + rawUrl.substring(0, Math.min(30, rawUrl.length()))
        );
    }

    @Bean
    public DataSource dataSource() {
        String jdbcUrl = normalizeUrl(url);

        // ✅ Debug logging
        System.out.println("=== DataSource Config ===");
        System.out.println("Original URL prefix: " + url.substring(0, Math.min(20, url.length())));
        System.out.println("Normalized URL prefix: " + jdbcUrl.substring(0, Math.min(50, jdbcUrl.length())));
        System.out.println("========================");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username.trim());
        config.setPassword(password.trim());
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(20000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(1200000);
        config.setConnectionTestQuery("SELECT 1");

        // ✅ SSL for Neon
        config.addDataSourceProperty("sslmode", "require");

        return new HikariDataSource(config);
    }
}