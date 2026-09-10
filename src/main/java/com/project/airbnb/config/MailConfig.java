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

    @Value("${spring.mail.port:587}")
    private int port;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    @PostConstruct
    void logMailConfig() {
        if (!mailEnabled) {
            log.warn("app.mail.enabled=false — OTP sẽ KHÔNG gửi qua email thật");
            return;
        }
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            log.error("Thiếu MAIL_USERNAME / MAIL_PASSWORD — không gửi được Gmail. "
                    + "Tạo App Password: https://myaccount.google.com/apppasswords");
        } else {
            log.info("Mail SMTP: {}:{} as {}", host, port, username);
        }
    }

    @Bean
    public JavaMailSender javaMailSender() {
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
            // Port 465 uses SSL from the start
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.starttls.enable", "false");
            props.put("mail.smtp.starttls.required", "false");
        } else {
            // Port 587 uses STARTTLS upgrade
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }

        // Generous timeouts — Render free tier cold-starts can be slow
        props.put("mail.smtp.connectiontimeout", "30000");
        props.put("mail.smtp.timeout", "30000");
        props.put("mail.smtp.writetimeout", "30000");

        return mailSender;
    }
}
