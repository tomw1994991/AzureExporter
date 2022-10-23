package com.tomw.azureexporter.metric;

import com.azure.core.http.rest.Response;
import com.azure.core.util.Context;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.monitor.query.MetricsQueryClient;
import com.azure.monitor.query.MetricsQueryClientBuilder;
import com.azure.monitor.query.models.MetricsQueryOptions;
import com.azure.monitor.query.models.MetricsQueryResult;
import com.azure.monitor.query.models.QueryTimeInterval;
import com.tomw.azureexporter.resource.AzureResource;
import com.tomw.azureexporter.resource.ResourceDiscoverer;
import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class MetricsScraper {

    @Setter(AccessLevel.PACKAGE)
    private MetricsQueryClient metricsQueryClient;
    private final MetricsQueryOptions defaultQueryOptions;

    private final ScrapeConfigProps scrapeConfig;
    private final ResourceDiscoverer resourceDiscoverer;

    private final MetricRegistry metricRegistry;


    public MetricsScraper(ScrapeConfigProps scrapeConfig, ResourceDiscoverer resourceDiscoverer, MetricRegistry metricRegistry) {
        metricsQueryClient = new MetricsQueryClientBuilder()
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
        defaultQueryOptions = new MetricsQueryOptions().setGranularity(Duration.ofMinutes(scrapeConfig.getGranularityInMins()));
        this.scrapeConfig = scrapeConfig;
        this.resourceDiscoverer = resourceDiscoverer;
        this.metricRegistry = metricRegistry;
    }

    @Scheduled(initialDelay = 4000L, fixedDelayString = "${scrape.interval-in-millis:300000}")
    public void scrapeAllResources(){
        //TODO - warnings about lombok & about unsafe method in discoverer
        //TODO - do resource types in parallel?
        //TODO - play around with interval, granularity and window via api - should null value be stored
        log.info("Beginning scrape of Azure resources for metrics.");
        scrapeConfig.getResourceTypeConfigs().forEach(resourceType -> {
            scrapeResourceType(resourceType);
        });
        log.info("Finished scrape of Azure resources for metrics.");
    }

    private void scrapeResourceType(ResourceTypeConfig resourceType) {
        log.info("Scraping metrics for resource type: {})", resourceType.getResourceType());
        Set<AzureResource> resources = resourceDiscoverer.getResourcesForType(resourceType.getResourceType());
        resources.forEach(resource -> {
            scrapeResource(resource, resourceType.getMetricNames());
        });
    }

    public void scrapeResource(AzureResource resource, List<String> metricsToQuery) {
        log.info("Retrieving metrics for resource: {}", resource);
        Response<MetricsQueryResult> metricsResponse = metricsQueryClient
                .queryResourceWithResponse(resource.getId(), metricsToQuery, setMetricsQueryInterval(defaultQueryOptions, scrapeConfig.getIntervalInMillis()),
                        Context.NONE);
        MetricsQueryResult result = metricsResponse.getValue();
        result.getMetrics().forEach(metric -> metricRegistry.registerMetric(metric, resource));
    }

    /* package */ MetricsQueryOptions setMetricsQueryInterval(MetricsQueryOptions options, int intervalInMillis){
        return options.setTimeInterval(QueryTimeInterval.parse(Duration.ofMillis(intervalInMillis).toString()));
    }
}
