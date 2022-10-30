package com.tomw.azureexporter.metric;

import com.azure.monitor.query.models.MetricResult;
import com.azure.monitor.query.models.MetricValue;
import com.tomw.azureexporter.resource.AzureResource;
import io.micrometer.common.util.StringUtils;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Slf4j
@Builder
@RequiredArgsConstructor
public class MetricExporter extends Collector {

    final List<MetricResult> metricData;
    final AzureResource resource;
    private final static String OUTPUT_METRIC_PREFIX = "azure";
    private final static String DEFAULT_SEPARATOR = "_";


    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> mfs = new ArrayList<>();
        metricData.forEach(metric -> {
            mfs.add(convertMetricResultToPrometheusMetrics(metric));
        });
        return mfs;
    }

    private MetricFamilySamples convertMetricResultToPrometheusMetrics(MetricResult metric) {
        String promMetricName = convertAzNameToPromName(metric.getMetricName(), resource.getType());
        GaugeMetricFamily gauges = new GaugeMetricFamily(promMetricName, metric.getDescription(), List.of("id"));
        metric.getTimeSeries().forEach(time -> {
            gauges.samples.addAll(convertMetricValuesAtTimeToPrometheus(time.getValues(), metric));
        });
        return gauges;
    }

    private List<MetricFamilySamples.Sample> convertMetricValuesAtTimeToPrometheus(List<MetricValue> values, MetricResult metric) {
        List<MetricFamilySamples.Sample> mfs = new ArrayList<>();
        values.forEach(metricValue -> {
            try {
                mfs.add(convertMetricValueToPrometheus(metricValue, metric));
            } catch (NoMetricValueException ex) {
                log.debug("Empty metric value for metric {}", metric.getMetricName());
            }
        });
        return mfs;
    }

    private MetricFamilySamples.Sample convertMetricValueToPrometheus(MetricValue metricValue, MetricResult metric) {
        String promMetricName = convertAzNameToPromName(metric.getMetricName(), resource.getType());
        Double value = getValueFromAzMetricValues(metricValue);
        MetricFamilySamples.Sample sample = new MetricFamilySamples.Sample(promMetricName, List.of("id"), List.of(substringAfterSlash(resource.getId())), value, metricValue.getTimeStamp().toInstant().toEpochMilli());
        return sample;
    }

    /*package*/
    static String convertAzNameToPromName(String metricName, String resourceType) {
        return String.join("_", OUTPUT_METRIC_PREFIX, substringAfterSlash(resourceType), replaceNonAlphanumeric(metricName, DEFAULT_SEPARATOR)).toLowerCase(Locale.ROOT);
    }

    private static String replaceNonAlphanumeric(final String original, final String replacement) {
        return original.replaceAll("[^A-Za-z\\d]", replacement);
    }

    /*package*/
    static final String substringAfterSlash(String original) {
        if (StringUtils.isEmpty(original)) {
            return original;
        } else {
            int lastSlash = original.lastIndexOf("/");
            String afterSlash = (lastSlash > 0 && lastSlash + 1 < original.length()) ? original.substring(lastSlash + 1) : original;
            return afterSlash;
        }
    }

    private static Double getValueFromAzMetricValues(MetricValue azValues) {
        List<Double> values = Arrays.asList(azValues.getAverage(), azValues.getTotal(), azValues.getMaximum(), azValues.getMaximum());
        return values.stream().filter(value -> null != value).findFirst().orElseThrow(NoMetricValueException::new);
    }

    private static class NoMetricValueException extends RuntimeException {
    }
}
