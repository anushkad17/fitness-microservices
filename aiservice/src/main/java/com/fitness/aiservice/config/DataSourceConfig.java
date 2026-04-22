package com.fitness.aiservice.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.ai.url}")
    private String url;

    @Value("${spring.datasource.ai.username}")
    private String username;

    @Value("${spring.datasource.ai.password}")
    private String password;

    private String normalizeJdbcUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalStateException("SPRING_DATASOURCE_URL is not set!");
        }
        rawUrl = rawUrl.trim();

        if (rawUrl.startsWith("jdbc:postgresql://")) {
            return rawUrl; // already correct
        }
        if (rawUrl.startsWith("postgres://")) {
            return "jdbc:postgresql://" + rawUrl.substring("postgres://".length());
        }
        if (rawUrl.startsWith("postgresql://")) {
            return "jdbc:postgresql://" + rawUrl.substring("postgresql://".length());
        }

        throw new IllegalStateException("Unknown DB URL format: " + rawUrl.substring(0, 20));
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        String jdbcUrl = normalizeJdbcUrl(url);

        System.out.println("=== DB CONNECTION ===");
        System.out.println("JDBC URL: " + jdbcUrl.replaceAll(":[^:@]+@", ":****@")); // hide password
        System.out.println("====================");

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
        config.addDataSourceProperty("sslmode", "require");

        return new HikariDataSource(config);
    }
}