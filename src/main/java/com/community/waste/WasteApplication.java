package com.community.waste;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WasteApplication {
    public static void main(String[] args) {
        SpringApplication.run(WasteApplication.class, args);
    }
}
