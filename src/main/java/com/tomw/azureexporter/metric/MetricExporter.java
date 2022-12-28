package com.tomw.azureexporter.metric;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.util.CollectionUtils;

@Slf4j
public class MetricExporter extends Collector {

    private final Map<String, MetricFamilySamples> metricData;
    private final static Map<String, MetricExporter> exporters = new ConcurrentHashMap<>();

    @Synchronized
    public static MetricExporter forMetric(PrometheusMetric metric) {
        MetricExporter exporter = exporters.get(metric.getName());
        if(null == exporter){
            exporter = new MetricExporter(metric);
        }
        exporters.putIfAbsent(metric.getName(), exporter);
        return exporter;
    }

    private MetricExporter(PrometheusMetric metric) {
        metricData = new ConcurrentHashMap<>();
        metricData.put("default", createMetricFamily(metric));
        this.register();
        log.info("Created new exporter for metric {}", metric.getName());
    }

    @Override
    public List<MetricFamilySamples> collect() {
        log.debug("Collected metric values for export: {}", metricData);
        return metricData.values().stream().toList();
    }
    
    public void saveMetric(PrometheusMetric metric) {
        
        MetricFamilySamples newSamples = createMetricFamily(metric);
        if (!CollectionUtils.isEmpty(metric.getDataPoints())) {
            newSamples.samples.add(metric.getDataPoints().get(metric.getDataPoints().size() - 1));
        }
        metricData.put(metric.getSanitisedResourceId(), newSamples);
        log.debug("Saved metric data: {} : {}", metric.getSanitisedResourceId(), metricData.get(metric.getSanitisedResourceId()));
    }

    private MetricFamilySamples createMetricFamily(PrometheusMetric metric) {
        return new GaugeMetricFamily(metric.getName(), metric.getDescription(), getMetricLabelKeys());
    }

    private List<String> getMetricLabelKeys() {
        return List.of("id");
    }
}
