package com.krysta.emailreader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Main Spring Boot application class for Email Reader Agent.
 * This application provides a REST API to count emails from specific senders using Gmail API.
 */
@SpringBootApplication
@EnableCaching
public class EmailReaderApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmailReaderApplication.class, args);
    }
}
