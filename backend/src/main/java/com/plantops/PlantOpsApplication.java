package com.plantops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PlantOpsApplication {
    public static void main(String[] args) { SpringApplication.run(PlantOpsApplication.class, args); }
}
