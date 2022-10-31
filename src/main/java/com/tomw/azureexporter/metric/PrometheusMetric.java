package com.tomw.azureexporter.metric;

import com.azure.monitor.query.models.MetricResult;
import com.azure.monitor.query.models.MetricValue;
import io.prometheus.client.Collector;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Slf4j
@ToString
public class PrometheusMetric {

    private final String resourceType;
    private final String resourceId;
    @Getter
    private final String name;
    @Getter
    private final String description;

    @Getter
    private List<Collector.MetricFamilySamples.Sample> dataPoints = new ArrayList<>();


    public PrometheusMetric(String rawResourceType, MetricResult azureMetric, String resourceId, String metricDescription){
        this.resourceType = MetricNaming.substringAfterSlash(rawResourceType);
        this.resourceId = resourceId;
        this.description = metricDescription;
        this.name = MetricNaming.computePrometheusMetricName(resourceType, azureMetric.getMetricName());
        azureMetric.getTimeSeries().forEach(time -> {
            dataPoints.addAll(convertMetricValuesToPrometheus(time.getValues()));
        });
    }

    private List<Collector.MetricFamilySamples.Sample> convertMetricValuesToPrometheus(List<MetricValue> values) {
        List<Collector.MetricFamilySamples.Sample> mfs = new ArrayList<>();
        values.forEach(metricValue -> {
            try {
                mfs.add(convertMetricValueToPrometheus(metricValue));
            } catch (NoMetricValueException ex) {
                log.debug("No metric value scraped for metric {}", getName());
            }
        });
        return mfs;
    }

    private Collector.MetricFamilySamples.Sample convertMetricValueToPrometheus(MetricValue metricValue) {
        Double value = getValueFromAzMetricValues(metricValue);
        Collector.MetricFamilySamples.Sample sample = new Collector.MetricFamilySamples.Sample(getName(),
                List.of("id"), List.of(MetricNaming.substringAfterSlash(resourceId)), value, metricValue.getTimeStamp().toInstant().toEpochMilli());
        return sample;
    }

    private static Double getValueFromAzMetricValues(MetricValue azValues) {
        List<Double> values = Arrays.asList(azValues.getAverage(), azValues.getTotal(), azValues.getMaximum(), azValues.getMaximum());
        return values.stream().filter(value -> null != value).findFirst().orElseThrow(NoMetricValueException::new);
    }

    public boolean hasData() {
        return dataPoints.size() > 0;
    }

    private static class NoMetricValueException extends RuntimeException { }
}