package com.tomw.azureexporter.metric;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class MetricExporter extends Collector {

    private final MetricFamilySamples metricData;
    private final static Map<String, MetricExporter> exporters = new HashMap<>();
    private long metricDataExpirationMillis = 600000;

    @Synchronized
    public static MetricExporter forMetric(PrometheusMetric metric){
        MetricExporter exporter = exporters.get(metric.getName());
        if(null == exporter){
            log.info("Creating new exporter for metric {}", metric.getName());
            exporter = new MetricExporter(metric);
        }
        exporters.putIfAbsent(metric.getName(), exporter);
        return exporter;
    }

    private MetricExporter(PrometheusMetric metric){
        metricData = createMetricFamily(metric);
        this.register();
    }

    @Override
    public List<MetricFamilySamples> collect() {
        if (log.isDebugEnabled()){
            log.debug("Collected metric values for export: {}", metricData);
        }
        return List.of(metricData);
    }

    public MetricExporter saveMetric(PrometheusMetric metric) {
        removeExpiredData(metricData.samples);
        addNewData(metricData.samples, metric);
        if (log.isDebugEnabled()){
            log.debug("Saved metric data: {}", metricData.samples);
        }
        return this;
    }

    public MetricExporter withRetention(long metricDataExpirationMillis){
        this.metricDataExpirationMillis = metricDataExpirationMillis;
        return this;
    }

    private MetricFamilySamples createMetricFamily(PrometheusMetric metric) {
        return new GaugeMetricFamily(metric.getName(), metric.getDescription(), getMetricLabelKeys());
    }

    private List<String> getMetricLabelKeys() {
        return List.of("id");
    }

    private void removeExpiredData(List<MetricFamilySamples.Sample> existingData) {
        existingData.removeIf(data -> isExpiredData(data));
    }

    private void addNewData(List<MetricFamilySamples.Sample> samples, PrometheusMetric metric) {
        samples.addAll(metric.getDataPoints());
    }

    private boolean isExpiredData(MetricFamilySamples.Sample data) {
        Instant expiryDate = Instant.now().minusMillis(metricDataExpirationMillis);
        return Instant.ofEpochMilli(data.timestampMs.longValue()).isBefore(expiryDate);
    }
}
