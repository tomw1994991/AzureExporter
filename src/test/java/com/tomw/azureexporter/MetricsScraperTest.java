package com.tomw.azureexporter;

import com.azure.monitor.query.models.MetricsQueryOptions;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MetricsScraperTest {

    MetricScrapeConfig config = new MetricScrapeConfig();

    ResourceDiscoverer resourceDiscoverer = mock(ResourceDiscoverer.class);

    @Autowired
    MeterRegistry meterRegistry;

    MetricsScraper metricsScraper = new MetricsScraper(config, resourceDiscoverer, meterRegistry);

    @Test
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
        assertTrue(queryWithTimeInterval.getTimeInterval().getStartTime().isBefore(OffsetDateTime.now()));
    }

    @ParameterizedTest
    @CsvSource({
            "Microsoft.Storage/storageAccounts, Availability, azure_storageaccounts_availability",
            "virtualMachines, CPU_USAGE, azure_virtualmachines_cpu_usage",
            "Microsoft.Compute/VirtualMachines, CPU, azure_virtualmachines_cpu"
    })
    public void testCreatePrometheusMetricName_validName( String azResourceType, String azMetricName, String expectedPrometheusName){
        assertEquals(expectedPrometheusName, MetricsScraper.createPrometheusMetricName(azMetricName, azResourceType));
    }
}
