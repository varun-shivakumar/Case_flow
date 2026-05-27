package com.caseflow.iam.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
@Slf4j
public class MailConfig {

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String host;

    @Value("${spring.mail.port:587}")
    private int port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("ycnlmlyjrkhpknna")
    private String password;

    @Bean
    public JavaMailSender javaMailSender() {
        log.info("Initializing JavaMailSender with host: {}, port: {}", host, port);
        log.info("Email username loaded from config: {}", username != null ? username.replaceAll("(?<=^.{2}).*(?=.{2}$)", "****") : "NOT SET");

        if (username == null || username.isBlank()) {
            log.error("CRITICAL: spring.mail.username is not configured in config server!");
        }
        if (password == null || password.isBlank()) {
            log.error("CRITICAL: spring.mail.password is not configured in config server!");
        }

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);
        sender.setDefaultEncoding("UTF-8");

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        return sender;
    }
}
