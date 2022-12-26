package com.tomw.azureexporter.metric;

import com.tomw.azureexporter.config.ResourceTypeConfig;
import com.tomw.azureexporter.config.ScrapeConfigProps;
import com.tomw.azureexporter.resource.AzureResource;
import com.tomw.azureexporter.resource.ResourceDiscoverer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Thread.currentThread;

@Component
@RequiredArgsConstructor
@Slf4j
public class AzureMetricsScraper {


    private final ScrapeConfigProps scrapeConfig;
    private final ResourceDiscoverer resourceDiscoverer;
    private final AzureMonitorMetricsClient metricsClient;
    private ExecutorService executor;

    @Scheduled(initialDelayString = "${scrape.initial-delay-millis:5000}", fixedDelayString = "${scrape.interval-in-millis:300000}")
    public void scrapeAllResources() {
        executor = Objects.requireNonNullElse(executor, Executors.newFixedThreadPool(scrapeConfig.getThreads()));
        log.info("Scraping Azure resources for metrics.");
        List<CompletableFuture<Void>> futures = scrapeConfig.getResourceTypeConfigs().stream().map(resourceType -> CompletableFuture.runAsync(() -> scrapeResourceType(resourceType), executor)).toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();
    }

    private void scrapeResourceType(ResourceTypeConfig resourceType) {
        List<PrometheusMetric> metrics = queryResourceTypeMetrics(resourceType);
        saveMetrics(metrics);
        log.info("Saved {} metric result(s) for resource type {}", metrics.size(), resourceType);
        log.debug("Saved metric data: {}", metrics);
    }

    private List<PrometheusMetric> queryResourceTypeMetrics(ResourceTypeConfig resourceType) {
        Set<AzureResource> resources = resourceDiscoverer.getResourcesForType(resourceType.resourceType());
        return resources.stream().map(resource -> queryResourceMetrics(resource, resourceType)).flatMap(Collection::stream).toList();
    }

    private void saveMetrics(List<PrometheusMetric> metrics) {
        metrics.forEach(
            metric -> MetricExporter.forMetric(metric).saveMetric(metric)
        );
    }

    private List<PrometheusMetric> queryResourceMetrics(AzureResource resource, ResourceTypeConfig resourceType) {
        List<PrometheusMetric> metrics = new ArrayList<>();
        try {
            log.debug("[{}] Retrieving metrics for resource: {}", currentThread().getName(), resource.getId());
            metrics = metricsClient.retrieveResourceMetrics(resource, resourceType);
            log.debug("[{}] Retrieved {} metric results for resource: {}", currentThread().getName(), metrics.size(), resource.getId());
        } catch (RuntimeException ex) {
            log.error("Unexpected error retrieving metrics for resource {}", resource, ex);
        }
        return metrics;
    }
}
