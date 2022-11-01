package com.tomw.azureexporter.metric;

import com.tomw.azureexporter.config.ResourceTypeConfig;
import com.tomw.azureexporter.config.ScrapeConfigProps;
import com.tomw.azureexporter.resource.AzureResource;
import com.tomw.azureexporter.resource.ResourceDiscoverer;
import io.prometheus.client.CollectorRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AzureMetricsScraperTest {

    ScrapeConfigProps config = setupScrapeConfigProps();

    ResourceDiscoverer resourceDiscoverer = mock(ResourceDiscoverer.class);

    AzureMonitorMetricsClient metricsClient = mock(AzureMonitorMetricsClient.class);

    AzureMetricsScraper metricsScraper = new AzureMetricsScraper(config, resourceDiscoverer, metricsClient);

    @BeforeEach
    public void setup() {
        AzureResource resource1 = new AzureResource("vm1", "virtualMachines", new HashMap<>());
        AzureResource resource2 = new AzureResource("vm2", "virtualMachines", new HashMap<>());
        when(metricsClient.queryResourceMetrics(resourceWithId(resource1.getId()), resourceTypeConfigWithType(resource1.getType()))).thenReturn(populatedMetrics(resource1));
        when(resourceDiscoverer.getResourcesForType(resource1.getType())).thenReturn(Set.of(resource1, resource2));
    }

    private List<PrometheusMetric> populatedMetrics(AzureResource resource) {
        return List.of(new PrometheusMetric(resource, MetricResultGenerator.resultWithSingleDataPointWithValue()));
    }

    private ScrapeConfigProps setupScrapeConfigProps() {
        ScrapeConfigProps props =  new ScrapeConfigProps();
        props.setResourceTypeConfigs(defaultResourceTypeConfigs());
        props.setInitialDelayMillis(999999999);
        return props;
    }

    private List<ResourceTypeConfig> defaultResourceTypeConfigs() {
        return List.of(new ResourceTypeConfig("virtualMachines", List.of("cpu", "memory"), 100));
    }

    @Test
    public void testScrapeResources_resourceNotFound_otherResourcesSuccessful() {
        when(metricsClient.queryResourceMetrics(resourceWithId("vm2"), resourceTypeConfigWithType("virtualMachines"))).thenThrow(new RuntimeException("Resource not found!"));
        metricsScraper.scrapeAllResources();
        CollectorRegistry.defaultRegistry.getSampleValue("a"); //TODO
    }

    private ResourceTypeConfig resourceTypeConfigWithType( String resourceType) {
        return argThat(conf -> conf != null && conf.resourceType() == resourceType);
    }

    private AzureResource resourceWithId(String id) {
        return argThat(resource -> resource != null && resource.getId().equals(id));
    }

    @Test
    public void testScrapeResource_resourceHasNoMetrics_success() {

    }

    @Test
    public void testScrapeResource_resourceHasMetrics_metricsReturned() {
    }

    @Test
    public void testScrapeResource_resourceHasMetricsWithMultipleDatapoints_metricsReturned() {
    }

    @Test
    public void testScrapeResourcesForType_happyPath() {
    }

    @Test
    public void testScrapeResourcesForType_noResources_success() {
    }
}
