package com.project.airbnb.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Sends email via Resend HTTP API.
 * Used when SMTP is blocked (e.g. Render free tier).
 */
@Component
public class ResendEmailSender {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailSender.class);
    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    private final HttpClient httpClient;

    @Value("${resend.api-key:}")
    private String apiKey;

    @Value("${resend.from-email:}")
    private String fromEmail;

    public ResendEmailSender() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && fromEmail != null && !fromEmail.isBlank();
    }

    /**
     * Send HTML email via Resend API.
     * @return true if sent successfully
     */
    public boolean send(String to, String subject, String htmlBody) {
        if (!isConfigured()) {
            log.warn("Resend not configured (missing RESEND_API_KEY or RESEND_FROM_EMAIL)");
            return false;
        }

        try {
            String json = """
                {
                  "from": "%s",
                  "to": ["%s"],
                  "subject": "%s",
                  "html": "%s"
                }
                """.formatted(
                    escapeJson(fromEmail),
                    escapeJson(to),
                    escapeJson(subject),
                    escapeJson(htmlBody)
                );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RESEND_API_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Email sent via Resend to {}: status={}", to, response.statusCode());
                return true;
            } else {
                log.error("Resend API error: status={}, body={}", response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to send email via Resend to {}: {}", to, e.getMessage());
            return false;
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
