package com.project.airbnb.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.util.StringUtils;

import java.util.Properties;

@Configuration
@EnableAsync
public class MailConfig {

    private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String host;

    @Value("${spring.mail.port:465}")
    private int port;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    @Value("${resend.api-key:}")
    private String resendApiKey;

    @PostConstruct
    void logMailConfig() {
        if (!mailEnabled) {
            log.warn("app.mail.enabled=false — email sẽ KHÔNG được gửi");
            return;
        }
        if (resendApiKey != null && !resendApiKey.isBlank()) {
            log.info("Email transport: Resend API (HTTP)");
        } else if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
            log.info("Email transport: SMTP {}:{} as {}", host, port, username);
        } else {
            log.warn("No email transport configured (no RESEND_API_KEY and no MAIL_USERNAME/MAIL_PASSWORD). "
                    + "Emails will not be sent.");
        }
    }

    /**
     * Only create JavaMailSender when SMTP credentials are provided.
     * On Render, use Resend API instead (SMTP ports are blocked).
     */
    @Bean
    public JavaMailSender javaMailSender() {
        if (!StringUtils.hasText(password)) {
            log.warn("MAIL_PASSWORD is empty — skipping SMTP bean creation");
            return null;
        }

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.trust", host);

        if (port == 465) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.starttls.enable", "false");
            props.put("mail.smtp.starttls.required", "false");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }

        props.put("mail.smtp.connectiontimeout", "30000");
        props.put("mail.smtp.timeout", "30000");
        props.put("mail.smtp.writetimeout", "30000");

        log.info("JavaMailSender created: {}:{} as {}", host, port, username);
        return mailSender;
    }
}
