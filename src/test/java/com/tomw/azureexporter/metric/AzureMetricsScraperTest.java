package com.tomw.azureexporter.metric;

import com.tomw.azureexporter.config.ResourceTypeConfig;
import com.tomw.azureexporter.config.ScrapeConfigProps;
import com.tomw.azureexporter.resource.AzureResource;
import com.tomw.azureexporter.resource.ResourceDiscoverer;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

public class AzureMetricsScraperTest {

    ScrapeConfigProps config = setupScrapeConfigProps();

    ResourceDiscoverer resourceDiscoverer = mock(ResourceDiscoverer.class);

    AzureMonitorMetricsClient metricsClient = mock(AzureMonitorMetricsClient.class);

    AzureMetricsScraper metricsScraper = new AzureMetricsScraper(config, resourceDiscoverer, metricsClient);

    AzureResource resource1 = new AzureResource("vm1", "virtualMachines", new HashMap<>());
    AzureResource resource2 = new AzureResource("vm2", "virtualMachines", new HashMap<>());
    AzureResource resource3 = new AzureResource("database1", "database", new HashMap<>());


    @BeforeEach
    public void setup() {
        CollectorRegistry.defaultRegistry.clear();
        Mockito.reset(metricsClient);
        Mockito.reset(resourceDiscoverer);
        when(resourceDiscoverer.getResourcesForType(resource1.getType())).thenReturn(Set.of(resource1, resource2));
        when(resourceDiscoverer.getResourcesForType("database1")).thenReturn(Set.of(resource3));
    }

    private List<PrometheusMetric> metricWithSingleDatapoint(AzureResource resource, String metricName) {
        return List.of(new PrometheusMetric(resource, MetricResultGenerator.resultWithSingleDataPointWithValue(metricName)));
    }

    private List<PrometheusMetric> metricWithMultipleDatapoints(AzureResource resource, String metricName) {
        return List.of(new PrometheusMetric(resource, MetricResultGenerator.resultWithMultipleDataPointsWithValue(metricName)));
    }


    private List<PrometheusMetric> emptyMetrics() {
        return new ArrayList<>();
    }

    private ScrapeConfigProps setupScrapeConfigProps() {
        ScrapeConfigProps props = new ScrapeConfigProps();
        props.setResourceTypeConfigs(defaultResourceTypeConfigs());
        props.setInitialDelayMillis(999999999);
        return props;
    }

    private List<ResourceTypeConfig> defaultResourceTypeConfigs() {
        return List.of(new ResourceTypeConfig("virtualMachines", List.of("metric2", "metric3", "metric1"), 100));
    }

    @NotNull
    private static List<Collector.MetricFamilySamples.Sample> getMetricSamplesFromDefaultRegistry(Set<String> metricNames) {
        List<Collector.MetricFamilySamples.Sample> metricSamples = new ArrayList<>();
        Iterator<Collector.MetricFamilySamples> iterator = CollectorRegistry.defaultRegistry.filteredMetricFamilySamples(metricNames).asIterator();
        iterator.forEachRemaining(samples -> metricSamples.addAll(samples.samples));
        return metricSamples;
    }

    @Test
    public void testScrapeResources_resourceNotFound_otherResourcesSuccessful() {
        when(metricsClient.retrieveResourceMetrics(resourceWithId(resource2.getId()), resourceTypeConfigWithType(resource2.getType()))).thenThrow(new RuntimeException("Resource not found!"));
        when(metricsClient.retrieveResourceMetrics(resourceWithId(resource1.getId()), resourceTypeConfigWithType(resource1.getType()))).thenReturn(metricWithSingleDatapoint(resource1, "metric_3"));
        metricsScraper.scrapeAllResources();
        Double testVal = CollectorRegistry.defaultRegistry.getSampleValue("azure_virtualmachines_metric_3", new String[]{"id"}, new String[]{"vm1"});
        assertEquals(10.0, testVal);
    }

    private ResourceTypeConfig resourceTypeConfigWithMetrics(int numberOfMetrics) {
        return argThat(conf -> conf != null && conf.metrics().size() == numberOfMetrics);
    }

    private ResourceTypeConfig resourceTypeConfigWithType(String resourceType) {
        return argThat(conf -> conf != null && conf.resourceType() == resourceType);
    }

    private AzureResource resourceWithId(String id) {
        return argThat(resource -> resource != null && resource.getId().equals(id));
    }

    @Test
    public void testScrapeResource_resourceHasNoMetrics_noErrors() {
        when(metricsClient.retrieveResourceMetrics(any(), any())).thenReturn(emptyMetrics());
        metricsScraper.scrapeAllResources();
    }

    @Test
    public void testScrapeResource_resourceHasMetricsWithMultipleDatapoints_mostRecentRetrieved() {
        when(metricsClient.retrieveResourceMetrics(resourceWithId(resource2.getId()), resourceTypeConfigWithType(resource2.getType()))).thenReturn(metricWithMultipleDatapoints(resource2, "metric4"));
        metricsScraper.scrapeAllResources();
        List<Collector.MetricFamilySamples.Sample> metricSamples = getMetricSamplesFromDefaultRegistry(Set.of("azure_virtualmachines_metric4"));
        assertEquals(1, metricSamples.stream().filter(sample -> sample.labelValues.contains("vm2")).count());
    }

    @Test
    public void testScrapeResourcesForType_noResources_noError() {
        when(resourceDiscoverer.getResourcesForType(any())).thenReturn(new HashSet<>());
        metricsScraper.scrapeAllResources();
    }

    @Test
    public void testScrapeResources_multipleMetrics_allRetrieved() {
        List<PrometheusMetric> mixedMetrics = new ArrayList<>();
        mixedMetrics.addAll(metricWithMultipleDatapoints(resource2, "metric6"));
        mixedMetrics.addAll(metricWithMultipleDatapoints(resource2, "metric7"));
        when(metricsClient.retrieveResourceMetrics(resourceWithId(resource2.getId()), resourceTypeConfigWithType(resource2.getType()))).thenReturn(mixedMetrics);
        metricsScraper.scrapeAllResources();
        List<Collector.MetricFamilySamples.Sample> metricSamples = getMetricSamplesFromDefaultRegistry(Set.of("azure_virtualmachines_metric6", "azure_virtualmachines_metric7"));
        assertEquals(2, metricSamples.stream().filter(sample -> sample.labelValues.contains("vm2")).count());
    }

    @Test
    public void testScrapeResources_onlyConfiguredMetricsRequested() {
        metricsScraper.scrapeAllResources();
        verify(metricsClient, times(1)).retrieveResourceMetrics(resourceWithId("vm2"), resourceTypeConfigWithMetrics(3));
        verify(metricsClient, times(1)).retrieveResourceMetrics(resourceWithId("vm1"), resourceTypeConfigWithMetrics(3));
        verify(metricsClient, times(0)).retrieveResourceMetrics(resourceWithId("database1"), any());
    }
}
