package com.tomw.azureexporter.metric;

import com.azure.monitor.query.models.MetricsQueryOptions;
import com.tomw.azureexporter.metric.config.ResourceTypeConfig;
import com.tomw.azureexporter.resource.AzureResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AzureMonitorMetricsClientTest {

    @Autowired
    private AzureMonitorMetricsClient metricsClient;


    @Test
    public void test(){
        ResourceTypeConfig config = new ResourceTypeConfig("type1", List.of("metric1"), 1);
      AzureResource resource = AzureResource.builder().id("id1").type(config.resourceType()).tags(new HashMap<>()).build();
      metricsClient.queryResourceMetrics(resource,  config);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3000, 100000000})
    public void testQueryWithTimeInterval_noErrors(int timeInterval) {
        MetricsQueryOptions queryWithTimeInterval = metricsClient.setMetricsQueryInterval(new MetricsQueryOptions(), timeInterval);
        assertEquals(Duration.ofMillis(timeInterval), queryWithTimeInterval.getTimeInterval().getDuration());
    }
}
