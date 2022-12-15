package com.tomw.azureexporter.metric;

import com.azure.monitor.query.models.MetricResult;
import com.azure.monitor.query.models.MetricValue;
import com.azure.monitor.query.models.TimeSeriesElement;
import com.tomw.azureexporter.resource.AzureResource;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class MetricResultGenerator {

    public static AzureResource DEFAULT_RESOURCE = AzureResource.builder().id("resource1").type("virtualMachine").tags(new HashMap<>()).build();

    public static MetricResult resultWithNoData(){
        return resultFromTimeSeriesList(new ArrayList<>(), "metric1");
    }

    public static MetricResult resultWithDataButNoValue(){
        MetricValue metricValue = new MetricValue(OffsetDateTime.now(), null, null, null, null, null);
        return resultFromSingleTimeSeries(timeSeriesWithMetricValue(metricValue), "metric1");
    }

    public static MetricResult resultWithNullTimestamp(){
      MetricValue metricValue = new MetricValue(null, 10d, 10d, 10d, 10d, 10d);
      return resultFromSingleTimeSeries(timeSeriesWithMetricValue(metricValue), "metric1");
    }

    public static MetricResult resultWithSingleDataPointWithValue(String metricName) {
        MetricValue metricValue = new MetricValue(OffsetDateTime.now(), 10d, null, null, null, null);
        return resultFromSingleTimeSeries(timeSeriesWithMetricValue(metricValue), metricName);
    }

    public static MetricResult resultWithSingleDataPointWithValue() {
        return resultWithSingleDataPointWithValue("metric1");
    }

    public static MetricResult resultWithMultipleDataPointsWithValue(String metricName) {
        MetricValue metricValue1 = new MetricValue(OffsetDateTime.now(), 10d, null, null, null, null);
        MetricValue metricValue2 = new MetricValue(OffsetDateTime.now(), 15d, null, null, null, null);
        MetricValue metricValue3 = new MetricValue(OffsetDateTime.now(), 20d, null, null, null, null);
        return resultFromTimeSeriesList(List.of(timeSeriesWithMetricValue(metricValue1), timeSeriesWithMetricValue(metricValue2), timeSeriesWithMetricValue(metricValue3)), metricName);
    }

    public static MetricResult resultFromSingleMetricValue(MetricValue value){
        return resultFromSingleTimeSeries(timeSeriesWithMetricValue(value), "metric1");
    }

    private static MetricResult resultFromSingleTimeSeries(TimeSeriesElement timeSeries, String metricName){
        return resultFromTimeSeriesList(List.of(timeSeries), metricName);
    }


    private static TimeSeriesElement timeSeriesWithMetricValue(final MetricValue metricValue) {
        return new TimeSeriesElement(List.of(metricValue), null);
    }

    private static MetricResult resultFromTimeSeriesList(final List<TimeSeriesElement> timeSeries, String metricName) {
        return new MetricResult(DEFAULT_RESOURCE.getId(), DEFAULT_RESOURCE.getType(), null, metricName, timeSeries, "desc1", null);
    }
}
