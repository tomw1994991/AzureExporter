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
        return resultFromTimeSeriesList(new ArrayList<>());
    }

    public static MetricResult resultWithDataButNoValue(){
        MetricValue metricValue = new MetricValue(OffsetDateTime.now(), null, null, null, null, null);
        return resultFromSingleTimeSeries(timeSeriesWithMetricValue(metricValue));
    }

    public static MetricResult resultWithNullTimestamp(){
      MetricValue metricValue = new MetricValue(null, 10d, 10d, 10d, 10d, 10d);
      return resultFromSingleTimeSeries(timeSeriesWithMetricValue(metricValue));
    }

    public static MetricResult resultWithSingleDataPointWithValue() {
        MetricValue metricValue = new MetricValue(OffsetDateTime.now(), 10d, null, null, null, null);
        return resultFromSingleTimeSeries(timeSeriesWithMetricValue(metricValue));
    }

    public static MetricResult resultFromSingleMetricValue(MetricValue value){
        return resultFromSingleTimeSeries(timeSeriesWithMetricValue(value));
    }

    private static MetricResult resultFromSingleTimeSeries(TimeSeriesElement timeSeries){
        return resultFromTimeSeriesList(List.of(timeSeries));
    }

    private static TimeSeriesElement timeSeriesWithMetricValue(final MetricValue metricValue) {
        return new TimeSeriesElement(List.of(metricValue), null);
    }

    private static MetricResult resultFromTimeSeriesList(final List<TimeSeriesElement> timeSeries) {
        return new MetricResult(DEFAULT_RESOURCE.getId(), DEFAULT_RESOURCE.getType(), null, "metric1", timeSeries, "desc1", null);
    }
}
