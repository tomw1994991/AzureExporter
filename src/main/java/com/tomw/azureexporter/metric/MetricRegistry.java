package com.tomw.azureexporter.metric;

import com.azure.monitor.query.models.MetricResult;
import com.tomw.azureexporter.resource.AzureResource;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class MetricRegistry {

    private final MeterRegistry prometheusRegistry;
    private final static String OUTPUT_METRIC_PREFIX = "azure";


    public void registerMetric(MetricResult metric, AzureResource resource) {
        metric.getTimeSeries().forEach(dataPoint -> {
            Counter.builder(createPrometheusMetricName(metric.getMetricName(), resource.getType())).tag("id", substringAfterSlash(resource.getId())).description(metric.getDescription()).baseUnit(metric.getUnit().toString().toLowerCase(Locale.ROOT)).register(prometheusRegistry);
        });
        log.debug("Registered metric: {}", metric);
    }


    public static final String substringAfterSlash(String original){
        int lastSlash = original.lastIndexOf("/");
        String afterSlash = (lastSlash > 0 && lastSlash + 1 < original.length())? original.substring(lastSlash + 1) : original;
        return afterSlash;
    }

    private static String convertAzureMetricNameForPrometheus(final String metricName){
        return metricName.replaceAll("[^A-Za-z\\d]", "_").toLowerCase(Locale.ROOT);
    }

    public static String createPrometheusMetricName(String metricName, String resourceType) {
        return String.join("_", OUTPUT_METRIC_PREFIX, substringAfterSlash(resourceType).toLowerCase(Locale.ROOT), convertAzureMetricNameForPrometheus(metricName));
    }
}
