package com.tomw.azureexporter.metric;

import com.tomw.azureexporter.config.ScrapeConfigProps;
import com.tomw.azureexporter.resource.AzureResource;
import com.tomw.azureexporter.resource.ResourceDiscoverer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MetricsScraperTest {

    ScrapeConfigProps config = new ScrapeConfigProps();

    ResourceDiscoverer resourceDiscoverer = mock(ResourceDiscoverer.class);

    AzureMonitorMetricsClient metricsClient = mock(AzureMonitorMetricsClient.class);

    AzureMetricsScraper metricsScraper = new AzureMetricsScraper(config, resourceDiscoverer, metricsClient);

    @BeforeEach
    public void setup() {
        when(metricsClient.queryResourceMetrics(any(), any())).thenReturn(new ArrayList<>());
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
