package com.tomw.azureexporter;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusScrapeEndpoint;
import org.springframework.boot.actuate.metrics.export.prometheus.TextOutputFormat;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@EndpointWebExtension(endpoint = PrometheusScrapeEndpoint.class)
@Slf4j
@AllArgsConstructor
public class PrometheusScrapeEndpointWebExtension {

    private final PrometheusScrapeEndpoint delegate;

    @ReadOperation
    public WebEndpointResponse<String> scrape(TextOutputFormat format, @Nullable Set<String> includedNames) {
        log.info("Prometheus scrape endpoint hit.");
        return this.delegate.scrape(format, includedNames);
    }
}
