package com.tomw.azureexporter.metric;

import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpMethod;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.rest.Response;
import com.azure.core.http.rest.SimpleResponse;
import com.azure.core.util.Context;
import com.azure.monitor.query.MetricsQueryClient;
import com.azure.monitor.query.models.MetricResult;
import com.azure.monitor.query.models.MetricsQueryOptions;
import com.azure.monitor.query.models.MetricsQueryResult;
import com.azure.monitor.query.models.QueryTimeInterval;
import com.tomw.azureexporter.config.ResourceTypeConfig;
import com.tomw.azureexporter.config.ScrapeConfigProps;
import com.tomw.azureexporter.resource.AzureResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AzureMonitorMetricsClientTest {

    private AzureMonitorMetricsClient metricsClient;
    private final ScrapeConfigProps scrapeProps = new ScrapeConfigProps();
    private final MetricsQueryClient mockQueryClient = mock(MetricsQueryClient.class);
    ResourceTypeConfig config = new ResourceTypeConfig("type1", List.of("metric1"), 1);
    AzureResource resource = AzureResource.builder().id("id1").type(config.resourceType()).tags(new HashMap<>()).build();

    @BeforeEach
    public void setup() {
        metricsClient = new AzureMonitorMetricsClient(scrapeProps);
        when(mockQueryClient.queryResourceWithResponse(any(), any(), any(), any())).thenReturn(responseWithData());
        metricsClient.setQueryClient(mockQueryClient);
    }

    private Response<MetricsQueryResult> responseWithMetricsButNoData() {
        MetricsQueryResult value = createMetricsQueryResult(defaultMetricValues());
        return createResponse(value, 200);
    }

    private Response<MetricsQueryResult> responseWithData() {
        MetricsQueryResult value = createMetricsQueryResult(metricValuesWithData());
        return createResponse(value, 200);
    }

    private Response<MetricsQueryResult> emptyResponse() {
        MetricsQueryResult value = createMetricsQueryResult(new ArrayList<>());
        return createResponse(value, 200);
    }


    private List<MetricResult> defaultMetricValues() {
        return List.of(MetricResultGenerator.resultWithNoData());
    }

    private List<MetricResult> metricValuesWithData() {
        return List.of(MetricResultGenerator.resultWithSingleDataPointWithValue());
    }


    private MetricsQueryResult createMetricsQueryResult(List<MetricResult> metrics) {
        return new MetricsQueryResult(1, QueryTimeInterval.LAST_5_MINUTES, Duration.ofMinutes(5), "namespace1", "uksouth", metrics);
    }

    private Response<MetricsQueryResult> createResponse(MetricsQueryResult value, int statusCode) {
        return new SimpleResponse<>(new HttpRequest(HttpMethod.GET, "http://uri:1234"), statusCode, new HttpHeaders(), value);
    }

    @Test
    public void testQueryResourceMetrics_noResults_noError() {
        when(mockQueryClient.queryResourceWithResponse(any(), any(), any(), any())).thenReturn(emptyResponse());
        List<PrometheusMetric> metrics = metricsClient.retrieveResourceMetrics(resource, config);
        assertEquals(0, metrics.size());
    }

    @Test
    public void testQueryResourceMetrics_hasResultsButNoData_emptyDataPulled() {
        when(mockQueryClient.queryResourceWithResponse(any(), any(), any(), any())).thenReturn(responseWithMetricsButNoData());
        List<PrometheusMetric> metrics = metricsClient.retrieveResourceMetrics(resource, config);
        assertEquals(1, metrics.size());
    }

    @Test
    public void testQueryResourceMetrics_hasResultsWithData_prometheusMetricsReturned() {
        List<PrometheusMetric> metrics = metricsClient.retrieveResourceMetrics(resource, config);
        assertEquals(1, metrics.size());
        assertTrue(metrics.get(0).hasData());
        assertEquals(10d, metrics.get(0).getDataPoints().get(0).value);
    }

    @Test
    public void testQueryResourceMetrics_correctArgumentsPassedToClient() {
        metricsClient.retrieveResourceMetrics(resource, config);
        verify(mockQueryClient, times(1)).queryResourceWithResponse(eq(resource.getId()), eq(List.of("metric1")), any(), eq(Context.NONE));
    }

    @Test
    public void testQueryResourceMetrics_moreThan20Metrics_multipleRequestsSent() {
        ResourceTypeConfig configWithManyMetrics = new ResourceTypeConfig("type1", List.of("metric1", "metric2", "metric3", "metric4", "metric5", "metric6", "metric7", "metric8", "metric9", "metric10",
                "metric11", "metric12", "metric13", "metric14", "metric15", "metric16", "metric17", "metric18", "metric19", "metric20", "metric21", "metric22", "metric23", "metric24", "metric25"), 1);
        metricsClient.retrieveResourceMetrics(resource, configWithManyMetrics);
        verify(mockQueryClient, times(2)).queryResourceWithResponse(eq(resource.getId()), any(), any(), eq(Context.NONE));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3000, 100000000})
    public void testQueryWithTimeInterval_noErrors(int timeInterval) {
        MetricsQueryOptions queryWithTimeInterval = metricsClient.setMetricsQueryInterval(new MetricsQueryOptions(), timeInterval);
        assertEquals(Duration.ofMillis(timeInterval), queryWithTimeInterval.getTimeInterval().getDuration());
    }
}
