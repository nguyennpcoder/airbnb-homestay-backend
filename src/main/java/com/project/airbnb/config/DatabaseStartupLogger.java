package com.project.airbnb.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
public class DatabaseStartupLogger {

    private static final Logger log = LoggerFactory.getLogger(DatabaseStartupLogger.class);

    private final DataSource dataSource;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${server.port}")
    private String serverPort;

    public DatabaseStartupLogger(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logDatabaseInfo() {
        String catalog = "?";
        try (Connection conn = dataSource.getConnection()) {
            catalog = conn.getCatalog();
        } catch (Exception e) {
            log.warn("Could not read DB catalog: {}", e.getMessage());
        }

        log.info("=================================================");
        log.info(" Airbnb HOMESTAY Auth Backend READY");
        log.info(" Port     : {}", serverPort);
        log.info(" JDBC URL : {}", datasourceUrl);
        log.info(" Database : {}", catalog);
        log.info("=================================================");

        if (datasourceUrl != null && datasourceUrl.contains("/airbnb?")) {
            log.error(" WRONG DATABASE: URL points to `airbnb` — must be `airbnb_homestay`!");
        }
        if ("airbnb".equalsIgnoreCase(catalog)) {
            log.error(" WRONG DATABASE: connected catalog is `airbnb` — stop old backend and use this project.");
        }
    }
}
