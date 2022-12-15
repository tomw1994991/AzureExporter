package com.tomw.azureexporter.metric;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MetricExporter extends Collector {

    private final MetricFamilySamples metricData;
    private final static Map<String, MetricExporter> exporters = new ConcurrentHashMap<>();
    private long metricDataExpirationMillis = 600000;

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
        metricData = createMetricFamily(metric);
        this.register();
        log.info("Created new exporter for metric {}", metric.getName());
    }

    @Override
    public List<MetricFamilySamples> collect() {
        log.debug("Collected metric values for export: {}", metricData);
        return List.of(metricData);
    }

    public void saveMetric(PrometheusMetric metric) {
        removeExpiredData(metricData.samples);
        addNewData(metricData.samples, metric);
        log.debug("Saved metric data: {}", metricData.samples);
    }

    public MetricExporter withRetention(long metricDataExpirationMillis) {
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
        existingData.removeIf(this::isExpiredData);
    }

    private void addNewData(List<MetricFamilySamples.Sample> samples, PrometheusMetric metric) {
        samples.addAll(metric.getDataPoints());
    }

    private boolean isExpiredData(MetricFamilySamples.Sample data) {
        Instant expiryDate = Instant.now().minusMillis(metricDataExpirationMillis);
        return Instant.ofEpochMilli(data.timestampMs).isBefore(expiryDate);
    }
}
