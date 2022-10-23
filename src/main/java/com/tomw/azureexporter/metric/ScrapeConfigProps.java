package com.tomw.azureexporter.metric;

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

    private int granularityInMins = 5;
    private int intervalInMillis = 300000;
    private List<ResourceTypeConfig> resourceTypeConfigs;
}
