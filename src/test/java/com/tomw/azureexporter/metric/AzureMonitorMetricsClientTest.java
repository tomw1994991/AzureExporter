package com.tomw.azureexporter.metric;

import com.azure.monitor.query.models.MetricsQueryOptions;
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
      AzureResource resource = AzureResource.builder().id("id1").type("type1").tags(new HashMap<>()).build();
      metricsClient.retrieveResourceMetrics(resource,  List.of("metric1"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3000, 100000000})
    public void testQueryWithTimeInterval_noErrors(int timeInterval) {
        MetricsQueryOptions queryWithTimeInterval = metricsClient.setMetricsQueryInterval(new MetricsQueryOptions(), timeInterval);
        assertEquals(Duration.ofMillis(timeInterval), queryWithTimeInterval.getTimeInterval().getDuration());
    }
}
