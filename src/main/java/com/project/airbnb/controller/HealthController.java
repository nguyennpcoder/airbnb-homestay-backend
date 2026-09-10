package com.project.airbnb.controller;

import com.project.airbnb.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final DataSource dataSource;
    private final EmailService emailService;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${server.port}")
    private String serverPort;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    public HealthController(DataSource dataSource, EmailService emailService) {
        this.dataSource = dataSource;
        this.emailService = emailService;
    }

    @GetMapping("/db")
    public ResponseEntity<Map<String, Object>> databaseHealth() {
        Map<String, Object> body = new HashMap<>();
        body.put("project", "airbnb_homestay");
        body.put("port", serverPort);
        body.put("jdbcUrl", datasourceUrl);

        try (Connection conn = dataSource.getConnection()) {
            String catalog = conn.getCatalog();
            body.put("catalog", catalog);
            body.put("ok", "airbnb_homestay".equalsIgnoreCase(catalog));
            if (!"airbnb_homestay".equalsIgnoreCase(catalog)) {
                body.put("warning", "Connected to wrong database. Expected airbnb_homestay.");
            }
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            body.put("ok", false);
            body.put("error", e.getMessage());
            return ResponseEntity.status(500).body(body);
        }
    }

    @GetMapping("/mail")
    public ResponseEntity<Map<String, Object>> mailHealth(
            @RequestParam(required = false) String to) {
        Map<String, Object> body = new HashMap<>();
        body.put("mailEnabled", mailEnabled);
        body.put("usernameConfigured", StringUtils.hasText(mailUsername));
        body.put("username", mailUsername != null && mailUsername.contains("@")
                ? mailUsername.replaceAll("(^.).*(@.*$)", "$1***$2") : "(empty)");

        if (!mailEnabled) {
            body.put("ok", false);
            body.put("hint", "Set MAIL_ENABLED=true");
            return ResponseEntity.ok(body);
        }
        if (!StringUtils.hasText(mailUsername)) {
            body.put("ok", false);
            body.put("hint", "Tạo file backend/airbnb/.env với MAIL_USERNAME và MAIL_PASSWORD (Gmail App Password)");
            return ResponseEntity.ok(body);
        }
        if (StringUtils.hasText(to)) {
            boolean sent = emailService.sendOtpEmail(to, "000000");
            body.put("testSend", sent);
            body.put("ok", sent);
            body.put("hint", sent ? "Kiểm tra hộp thư (cả Spam)" : "SMTP lỗi — tạo App Password mới tại myaccount.google.com/apppasswords");
            return ResponseEntity.ok(body);
        }
        body.put("ok", true);
        body.put("hint", "Thêm ?to=your@email.com để gửi mail test");
        return ResponseEntity.ok(body);
    }
}
