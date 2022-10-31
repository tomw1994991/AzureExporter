package com.tomw.azureexporter.metric;

import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpMethod;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.rest.Response;
import com.azure.core.http.rest.SimpleResponse;
import com.azure.core.util.Context;
import com.azure.monitor.query.MetricsQueryClient;
import com.azure.monitor.query.models.*;
import com.tomw.azureexporter.config.ResourceTypeConfig;
import com.tomw.azureexporter.config.ScrapeConfigProps;
import com.tomw.azureexporter.resource.AzureResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

//TODO - generator / data provider for metric values
public class AzureMonitorMetricsClientTest {

    private AzureMonitorMetricsClient metricsClient;
    private final ScrapeConfigProps scrapeProps = new ScrapeConfigProps();
    private final MetricsQueryClient mockQueryClient = mock(MetricsQueryClient.class);
    ResourceTypeConfig config = new ResourceTypeConfig("type1", List.of("metric1"), 1);
    AzureResource resource = AzureResource.builder().id("id1").type(config.resourceType()).tags(new HashMap<>()).build();

    @BeforeEach
    public void setup(){
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
        return List.of(new MetricResult("id1", "resource1", MetricUnit.BYTES, "metric1", new ArrayList<TimeSeriesElement>(), "desc1", null));
    }

    private List<MetricResult> metricValuesWithData() {
        return List.of(new MetricResult("id1", "resource1", MetricUnit.BYTES, "metric1", getTimeSeriesListWithData(), "desc1", null));
    }

    private List<TimeSeriesElement> getTimeSeriesListWithData() {
        return List.of(new TimeSeriesElement(List.of(metricValueWithData()), null));
    }

    private MetricValue metricValueWithData() {
        return new MetricValue(OffsetDateTime.now(), 1d, null, null, null, null);
    }

    private MetricsQueryResult createMetricsQueryResult(List<MetricResult> metrics) {
        return new MetricsQueryResult(1, QueryTimeInterval.LAST_5_MINUTES, Duration.ofMinutes(5), "namespace1", "uksouth", metrics);
    }

    private Response<MetricsQueryResult> createResponse(MetricsQueryResult value, int statusCode) {
        return new SimpleResponse<>(new HttpRequest(HttpMethod.GET, "http://uri:1234"), statusCode, new HttpHeaders(), value);
    }

    @Test
    public void testQueryResourceMetrics_noResults_noError(){
      when(mockQueryClient.queryResourceWithResponse(any(), any(), any(), any())).thenReturn(emptyResponse());
      List<PrometheusMetric> metrics = metricsClient.queryResourceMetrics(resource,  config);
      assertEquals(0, metrics.size());
    }

    @Test
    public void testQueryResourceMetrics_hasResultsButNoData_noPrometheusMetrics(){
        when(mockQueryClient.queryResourceWithResponse(any(), any(), any(), any())).thenReturn(responseWithMetricsButNoData());
        List<PrometheusMetric> metrics = metricsClient.queryResourceMetrics(resource,  config);
        assertEquals(0, metrics.size());
    }

    @Test
    public void testQueryResourceMetrics_hasResultsWithData_prometheusMetricsReturned(){
        List<PrometheusMetric> metrics = metricsClient.queryResourceMetrics(resource,  config);
        assertEquals(1, metrics.size());
        assertTrue(metrics.get(0).hasData());
        assertEquals(1d, metrics.get(0).getDataPoints().get(0).value);
    }

    @Test
    public void testQueryResourceMetrics_correctArgumentsPassedToClient(){
        metricsClient.queryResourceMetrics(resource,  config);
        verify(mockQueryClient, times(1)).queryResourceWithResponse(eq(resource.getId()), eq(List.of("metric1")), any(), eq(Context.NONE));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3000, 100000000})
    public void testQueryWithTimeInterval_noErrors(int timeInterval) {
        MetricsQueryOptions queryWithTimeInterval = metricsClient.setMetricsQueryInterval(new MetricsQueryOptions(), timeInterval);
        assertEquals(Duration.ofMillis(timeInterval), queryWithTimeInterval.getTimeInterval().getDuration());
    }
}
