package com.tomw.azureexporter.metric;

import com.tomw.azureexporter.resource.AzureResource;
import com.tomw.azureexporter.resource.ResourceDiscoverer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MetricsScraperTest {

    ScrapeConfigProps config = new ScrapeConfigProps();

    ResourceDiscoverer resourceDiscoverer = mock(ResourceDiscoverer.class);

    @Autowired
    MetricRegistry metricRegistry;

    AzureMonitorMetricsClient metricsClient = mock(AzureMonitorMetricsClient.class);

    MetricsScraper metricsScraper = new MetricsScraper(config, resourceDiscoverer, metricRegistry, metricsClient);

    @BeforeEach
    public void setup() {
        config.setResourceTypeConfigs(List.of(
                new ResourceTypeConfig("type1", List.of("metric1", "metric2"))
        ));
        when(metricsClient.retrieveResourceMetrics(any(), any())).thenReturn(new ArrayList<>());
    }

    @Test
    public void testScrapeResources_resourceNotFound_error() {
        AzureResource resource = new AzureResource("id", "type1", new HashMap<>());
        when(resourceDiscoverer.getResourcesForType("type1")).thenReturn(Set.of(resource));
        metricsScraper.scrapeAllResources();
        //TODO - what should happen when resource not found (e.g. it has been deleted)
    }

    @Test
    public void testScrapeResource_resourceHasNoMetrics_success() {

    }

    @Test
    public void testScrapeResource_resourceHasMetrics_metricsReturned() {
    }

    @Test
    public void testScrapeResourcesForType_happyPath() {
    }

    @Test
    public void testScrapeResourcesForType_noResources_success() {
    }
}
