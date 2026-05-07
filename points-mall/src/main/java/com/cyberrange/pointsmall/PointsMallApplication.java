package com.cyberrange.pointsmall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PointsMallApplication {

    public static void main(String[] args) {
        SpringApplication.run(PointsMallApplication.class, args);
    }
}
