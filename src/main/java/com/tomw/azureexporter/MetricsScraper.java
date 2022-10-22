package com.tomw.azureexporter;

import com.azure.core.http.rest.Response;
import com.azure.core.util.Context;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.monitor.query.MetricsQueryClient;
import com.azure.monitor.query.MetricsQueryClientBuilder;
import com.azure.monitor.query.models.MetricResult;
import com.azure.monitor.query.models.MetricsQueryOptions;
import com.azure.monitor.query.models.MetricsQueryResult;
import com.azure.monitor.query.models.QueryTimeInterval;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@Slf4j
public class MetricsScraper {

    @Setter(AccessLevel.PACKAGE)
    private MetricsQueryClient metricsQueryClient;
    private final MetricsQueryOptions defaultQueryOptions;

    private final MetricScrapeConfig scrapeConfig;
    private final ResourceDiscoverer resourceDiscoverer;

    private final MeterRegistry meterRegistry;

    private final static String OUTPUT_METRIC_PREFIX = "azure";


    public MetricsScraper(MetricScrapeConfig scrapeConfig, ResourceDiscoverer resourceDiscoverer, MeterRegistry meterRegistry) {
        metricsQueryClient = new MetricsQueryClientBuilder()
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
        defaultQueryOptions = new MetricsQueryOptions().setGranularity(Duration.ofMinutes(scrapeConfig.getGranularityInMins()));
        this.scrapeConfig = scrapeConfig;
        this.resourceDiscoverer = resourceDiscoverer;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(initialDelay = 4000L, fixedDelayString = "${scrape.interval-in-millis:300000}")
    public void scrapeAllResources(){
        //TODO - cache, register metrics
        //TODO - azure metric names need to be valid for prometheus
        //TODO - warnings about lombok & about unsafe method in discoverer
        //TODO - do resource types in parallel?
        //TODO - play around with interval, granularity and window via api
        log.info("Beginning scrape of Azure resources for metrics.");
        scrapeConfig.getMetrics().forEach(metricConfig -> {
            scrapeResourceType(metricConfig);
        });
        log.info("Finished scrape of Azure resources for metrics.");
    }

    private void scrapeResourceType(MetricConfig metric) {
        log.info("Scraping metrics for resource type: {})", metric.getResourceType());
        Set<AzureResource> resources = resourceDiscoverer.getResourcesForType(metric.getResourceType());
        resources.forEach(resource -> {
            scrapeResource(resource, metric.getMetricNames());
        });
    }

    public void scrapeResource(AzureResource resource, List<String> metricsToQuery) {
        log.info("Retrieving metrics for resource: {}", resource);
        Response<MetricsQueryResult> metricsResponse = metricsQueryClient
                .queryResourceWithResponse(resource.getId(), metricsToQuery, setMetricsQueryInterval(defaultQueryOptions, scrapeConfig.getIntervalInMillis()),
                        Context.NONE);
        MetricsQueryResult result = metricsResponse.getValue();
        result.getMetrics().forEach(metric -> registerMetric(metric, resource));
    }

    /* package */ MetricsQueryOptions setMetricsQueryInterval(MetricsQueryOptions options, int intervalInMillis){
        return options.setTimeInterval(QueryTimeInterval.parse(Duration.ofMillis(intervalInMillis).toString()));
    }

    private void registerMetric(MetricResult metric, AzureResource resource) {
        metric.getTimeSeries().forEach(dataPoint -> {
            Counter.builder(createPrometheusMetricName(metric.getMetricName(), resource.getType())).tag("id", resource.getId()).description(metric.getDescription()).register(meterRegistry);
        });
        log.debug("Registered metric: {}", metric);
    }


    private static String substringAfterSlash(String original){
        int lastSlash = original.lastIndexOf("/");
        String afterSlash = (lastSlash > 0 && lastSlash + 1 < original.length())? original.substring(lastSlash + 1) : original;
        return afterSlash;
    }

    private static String convertAzureMetricNameForPrometheus(final String metricName){
        return metricName.replaceAll("[^A-Za-z\\d]", "_").toLowerCase(Locale.ROOT);
    }

    public static String createPrometheusMetricName(String metricName, String resourceType) {
        return String.join("_", OUTPUT_METRIC_PREFIX, substringAfterSlash(resourceType).toLowerCase(Locale.ROOT), convertAzureMetricNameForPrometheus(metricName));
    }
}
