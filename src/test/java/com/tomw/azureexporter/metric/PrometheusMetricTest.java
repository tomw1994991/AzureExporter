package com.tomw.azureexporter.metric;

import com.azure.monitor.query.models.MetricResult;
import com.azure.monitor.query.models.MetricValue;
import com.tomw.azureexporter.resource.AzureResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PrometheusMetricTest {

    MetricResult metricResult;
    AzureResource resource;
    PrometheusMetric promMetric;

    @BeforeEach
    public void setup() {
        resource = MetricResultGenerator.DEFAULT_RESOURCE;
        metricResult = MetricResultGenerator.resultWithNoData();
        promMetric = new PrometheusMetric(resource, metricResult);
    }

    @Test
    public void testHasData_emptyData_returnsFalse() {
        assertFalse(promMetric.hasData());
    }

    @Test
    public void testHasData_dataWithNoValue_returnsFalse() {
        metricResult = MetricResultGenerator.resultWithDataButNoValue();
        promMetric = new PrometheusMetric(resource, metricResult);
        assertFalse(promMetric.hasData());
    }

    @Test
    public void testHasData_dataWithValue_returnsTrue() {
        metricResult = MetricResultGenerator.resultWithSingleDataPointWithValue();
        promMetric = new PrometheusMetric(resource, metricResult);
        assertTrue(promMetric.hasData());
    }

    @Test
    public void testConstructor_NullTimestamp_throwsException() {
        metricResult = MetricResultGenerator.resultWithNullTimestamp();
        assertThrows(NullPointerException.class, () -> new PrometheusMetric(resource, metricResult));
    }

    @Test
    public void testConstructor_NullParameters_throwsException() {
        assertThrows(NullPointerException.class, () -> new PrometheusMetric(null, null));
    }


    @Test
    public void testGetName_happyPath() {
        assertEquals("azure_virtualmachine_metric1", promMetric.getName());
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
        metricResult = MetricResultGenerator.resultFromSingleMetricValue(metricValue);
        promMetric = new PrometheusMetric(resource, metricResult);
        assertEquals(1, promMetric.getDataPoints().size());
        assertEquals(10d, promMetric.getDataPoints().get(0).value);
    }


}