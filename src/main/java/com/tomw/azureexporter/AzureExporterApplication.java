package com.tomw.azureexporter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AzureExporterApplication {

    public static void main(String[] args) {
        SpringApplication.run(AzureExporterApplication.class, args);
    }

}
