package com.tomw.azureexporter.metric;

import com.azure.monitor.query.models.MetricResult;
import com.azure.monitor.query.models.MetricValue;
import com.tomw.azureexporter.resource.AzureResource;
import io.prometheus.client.Collector;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


@Slf4j
@ToString
public class PrometheusMetric {

    private final String resourceType;
    private final String resourceId;
    @Getter
    private final String sanitisedResourceId;
    @Getter
    private final String name;
    @Getter
    private final String description;
    @Getter
    private final List<Collector.MetricFamilySamples.Sample> dataPoints = new ArrayList<>();


    public PrometheusMetric(@NotNull AzureResource resource, @NotNull MetricResult azureMetric){
        this.resourceType = MetricNaming.substringAfterSlash(resource.getType());
        this.resourceId = resource.getId();
        this.sanitisedResourceId = MetricNaming.substringAfterSlash(MetricNaming.removeUndifferentiatedSuffix(resourceId));
        this.description = azureMetric.getDescription();
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
        return new Collector.MetricFamilySamples.Sample(getName(),
                List.of("id"), List.of(sanitisedResourceId),
                value, metricValue.getTimeStamp().toInstant().toEpochMilli());
    }

    private static Double getValueFromAzMetricValues(MetricValue azValues) {
        List<Double> values = Arrays.asList(azValues.getAverage(), azValues.getCount(), azValues.getTotal(), azValues.getMaximum(), azValues.getMinimum());
        return values.stream().filter(Objects::nonNull).findFirst().orElseThrow(NoMetricValueException::new);
    }

    public boolean hasData() {
        return dataPoints.size() > 0;
    }

    private static class NoMetricValueException extends RuntimeException { }
}
