package com.fitness.aiservice.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DataSourceDebugConfig {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @PostConstruct
    public void logDatasourceUrl() {
        System.out.println("===========================================");
        System.out.println("ACTUAL DATASOURCE URL: " + datasourceUrl);
        System.out.println("URL LENGTH: " + datasourceUrl.length());
        System.out.println("HAS PORT 5432: " + datasourceUrl.contains("5432"));
        System.out.println("STARTS WITH jdbc: " + datasourceUrl.startsWith("jdbc"));
        System.out.println("===========================================");
    }
}
