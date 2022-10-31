package com.tomw.azureexporter.metric;

import com.azure.monitor.query.models.MetricResult;
import com.azure.monitor.query.models.MetricValue;
import com.azure.monitor.query.models.TimeSeriesElement;
import com.tomw.azureexporter.resource.AzureResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PrometheusMetricTest {

    MetricResult metricResult;
    AzureResource resource;
    PrometheusMetric promMetric;

    @BeforeEach
    public void setup() {
        resource = AzureResource.builder().id("resourceId").type("resourceType").tags(new HashMap<>()).build();
        metricResult = new MetricResult(resource.getId(), resource.getType(), null, "metric1",
                new ArrayList<>(), "desc1", null);
        promMetric = new PrometheusMetric(resource, metricResult);
    }

    @Test
    public void testHasData_emptyData_returnsFalse() {
        assertEquals(false, promMetric.hasData());
    }

    @Test
    public void testHasData_dataWithNoValue_returnsFalse() {
        MetricValue metricValue = new MetricValue(OffsetDateTime.now(), null, null, null, null, null);
        metricResult = metricResultWithTimeSeries(List.of(timeSeriesWithMetricValue(metricValue)));
        promMetric = new PrometheusMetric(resource, metricResult);
        assertEquals(false, promMetric.hasData());
    }

    @Test
    public void testHasData_dataWithValue_returnsTrue() {
        metricResult = metricResultWithTimeSeries(List.of(timeSeriesWithMetricValue(metricValueWithData())));
        promMetric = new PrometheusMetric(resource, metricResult);
        assertEquals(true, promMetric.hasData());
    }

    @Test
    public void testConstructor_NullTimestamp_throwsException() {
        MetricValue valueWithNullTimestamp = new MetricValue(null, 10d, 10d, 10d, 10d, 10d);
        metricResult = metricResultWithTimeSeries(List.of(timeSeriesWithMetricValue(valueWithNullTimestamp)));
        assertThrows(NullPointerException.class, () -> new PrometheusMetric(resource, metricResult));
    }

    @Test
    public void testConstructor_NullParameters_throwsException() {
        assertThrows(NullPointerException.class, () -> new PrometheusMetric(null, null));
    }


    @Test
    public void testGetName_happyPath() {
        assertEquals("azure_resourcetype_metric1", promMetric.getName());
    }

    @Test
    public void testGetDescription_happyPath() {
        assertEquals(metricResult.getDescription(), promMetric.getDescription());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "null,null,null,null,10d",
            "null,null,null,10d,null",
            "null,null,10d,null,null",
            "null, 10d,null,null,null",
            "10d,null,null,null,null",
            "10d,2d,3d,4d,5d",
             "null,11d,14d,15d,10d"}, nullValues = {"null"})
    public void testGetDataPoints_valuePrecedence(Double average, Double min, Double max, Double total, Double count) {
        MetricValue metricValue = new MetricValue(OffsetDateTime.now(), average, min, max, total, count);
        metricResult = metricResultWithTimeSeries(List.of(timeSeriesWithMetricValue(metricValue)));
        promMetric = new PrometheusMetric(resource, metricResult);
        assertEquals(1, promMetric.getDataPoints().size());
        assertEquals(10d, promMetric.getDataPoints().get(0).value);
    }

    private MetricValue metricValueWithData() {
        return new MetricValue(OffsetDateTime.now(), 1d, 2d, 3d, 4d, 5d);
    }

    private TimeSeriesElement timeSeriesWithMetricValue(final MetricValue metricValue) {
        return new TimeSeriesElement(List.of(metricValue), null);
    }

    private MetricResult metricResultWithTimeSeries(final List<TimeSeriesElement> timeSeries) {
        return new MetricResult(resource.getId(), resource.getType(), null, "metric1", timeSeries, "desc1", null);
    }

}