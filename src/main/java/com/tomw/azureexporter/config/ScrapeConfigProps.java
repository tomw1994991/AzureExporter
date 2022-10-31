package com.tomw.azureexporter.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@ConfigurationProperties(prefix = "scrape")
@ConfigurationPropertiesScan
@Getter
@Setter
@Configuration
public class ScrapeConfigProps {

    private int intervalInMillis = 300000;
    private int queryWindowInMillis = 300000;
    private int initialDelayMillis = 5000;
    private int threads = 8;
    private List<ResourceTypeConfig> resourceTypeConfigs;
}
