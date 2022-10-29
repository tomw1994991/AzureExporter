package com.tomw.azureexporter.metric;

import com.azure.monitor.query.models.MetricResult;
import com.tomw.azureexporter.metric.config.ResourceTypeConfig;
import com.tomw.azureexporter.metric.config.ScrapeConfigProps;
import com.tomw.azureexporter.resource.AzureResource;
import com.tomw.azureexporter.resource.ResourceDiscoverer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Thread.currentThread;

@Component
@Slf4j
public class MetricsScraper {


    private final ScrapeConfigProps scrapeConfig;
    private final ResourceDiscoverer resourceDiscoverer;

    private final MetricRegistry metricRegistry;

    private final ExecutorService executor;

    private final AzureMonitorMetricsClient metricsClient;


    public MetricsScraper(ScrapeConfigProps scrapeConfig, ResourceDiscoverer resourceDiscoverer, MetricRegistry metricRegistry, AzureMonitorMetricsClient metricsClient) {
        this.metricsClient = metricsClient;
        this.scrapeConfig = scrapeConfig;
        this.resourceDiscoverer = resourceDiscoverer;
        this.metricRegistry = metricRegistry;
        this.executor = Executors.newFixedThreadPool(scrapeConfig.getThreads());
    }

    @Scheduled(initialDelayString = "${scrape.initial-delay-millis:5000}", fixedDelayString = "${scrape.interval-in-millis:300000}")
    public void scrapeAllResources() {
        scrapeConfig.getResourceTypeConfigs().forEach(resourceType -> {
            Runnable task = () -> scrapeResourceType(resourceType);
            executor.submit(task);
        });
        log.info("Finished scrape of Azure resources for metrics.");
    }


    private void scrapeResourceType(ResourceTypeConfig resourceType) {
        Set<AzureResource> resources = resourceDiscoverer.getResourcesForType(resourceType.resourceType());
        resources.forEach(resource -> {
            scrapeResource(resource, resourceType.metrics());
        });
    }

    public void scrapeResource(AzureResource resource, List<String> metricsToQuery) {
        try{
            log.debug("[{}] Retrieving metrics for resource: {}", currentThread().getName(), resource.getId());
            List<MetricResult> metrics = metricsClient.retrieveResourceMetrics(resource, metricsToQuery);
            metrics.forEach(metric -> metricRegistry.registerMetric(metric, resource));
            log.debug("[{}] Retrieved {} metric results for resource: {}", currentThread().getName(), metrics.size(), resource.getId());
        } catch (RuntimeException ex) {
            log.error("Unexpected error retrieving metrics for resource {}", resource, ex);
        }
    }
}
