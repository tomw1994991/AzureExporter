package com.tomw.azureexporter.metric;

import com.azure.monitor.query.MetricsQueryClient;
import com.azure.monitor.query.models.MetricsQueryOptions;
import com.tomw.azureexporter.resource.AzureResource;
import com.tomw.azureexporter.resource.ResourceDiscoverer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MetricsScraperTest {

    ScrapeConfigProps config = new ScrapeConfigProps();

    ResourceDiscoverer resourceDiscoverer = mock(ResourceDiscoverer.class);

    @Autowired
    MetricRegistry metricRegistry;

    MetricsScraper metricsScraper = new MetricsScraper(config, resourceDiscoverer, metricRegistry);

    MetricsQueryClient queryClient = mock(MetricsQueryClient.class);

    @BeforeEach
    public void setup(){
      metricsScraper.setMetricsQueryClient(queryClient);
      when(queryClient.queryResource(any(), any())).thenReturn(null); //TODO - what does azure do if resource not found
    }

    //@Test
    public void testScrapeResource_resourceNotFound_error(){
        when(resourceDiscoverer.getResourcesForType("type")).thenReturn(new HashSet<>());
        AzureResource resource = new AzureResource("id", "type", new HashMap<>());
        metricsScraper.scrapeResource(resource, Arrays.asList("Metric1"));
        //TODO
    }

    @Test
    public void testScrapeResource_resourceHasNoMetrics_success(){

    }

    @Test
    public void testScrapeResource_resourceHasMetrics_metricsReturned(){}

    @Test
    public void testScrapeResourcesForType_happyPath(){}

    @Test
    public void testScrapeResourcesForType_noResources_success(){}

    @ParameterizedTest
    @ValueSource( ints = {0,1,3000,100000000})
    public void testQueryWithTimeInterval_noErrors(int timeInterval){
        MetricsQueryOptions queryWithTimeInterval = metricsScraper.setMetricsQueryInterval(new MetricsQueryOptions(), timeInterval);
        assertEquals( Duration.ofMillis(timeInterval), queryWithTimeInterval.getTimeInterval().getDuration());
    }
}
