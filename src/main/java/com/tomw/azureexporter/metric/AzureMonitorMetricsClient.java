package com.tomw.azureexporter.metric;

import com.azure.core.http.rest.Response;
import com.azure.core.util.Context;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.monitor.query.MetricsQueryClient;
import com.azure.monitor.query.MetricsQueryClientBuilder;
import com.azure.monitor.query.models.MetricResult;
import com.azure.monitor.query.models.MetricsQueryOptions;
import com.azure.monitor.query.models.MetricsQueryResult;
import com.azure.monitor.query.models.QueryTimeInterval;
import com.tomw.azureexporter.config.ResourceTypeConfig;
import com.tomw.azureexporter.config.ScrapeConfigProps;
import com.tomw.azureexporter.resource.AzureResource;
import io.prometheus.client.Counter;
import lombok.AccessLevel;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
public class AzureMonitorMetricsClient {

    private final ScrapeConfigProps scrapeConfig;
    @Setter(AccessLevel.PACKAGE)
    private MetricsQueryClient queryClient;
    private static final Counter azureMetricApiCalls = Counter.build().name("azure_monitor_metric_api_calls").help("Number of calls to the azure monitor api for metrics.").labelNames("metric").register();

    public AzureMonitorMetricsClient(@NotNull ScrapeConfigProps scrapeConfig) {
        this.scrapeConfig = scrapeConfig;
        queryClient = new MetricsQueryClientBuilder()
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
    }
    
    public List<PrometheusMetric> retrieveResourceMetrics(@NotNull AzureResource resource, @NotNull ResourceTypeConfig config) {
        MetricsQueryOptions queryOptions = getQueryOptions(config);
        return splitToMaxSizeChunks(config.metrics(), 20).stream().
                map( chunk -> queryAzureMonitorForMetrics(resource, config, queryOptions)).flatMap(Collection::stream).toList();
    }

    private List<PrometheusMetric> queryAzureMonitorForMetrics(@NotNull AzureResource resource, @NotNull ResourceTypeConfig config, MetricsQueryOptions queryOptions) {
        Response<MetricsQueryResult> metricsResponse = queryClient
                .queryResourceWithResponse(resource.getId(), config.metrics(),
                        setMetricsQueryInterval(queryOptions, scrapeConfig.getQueryWindowInMillis()), Context.NONE);
        incrementApiCallMetric(resource.getType(), config.metrics());
        return getPrometheusMetrics(resource, metricsResponse);
    }

    private void incrementApiCallMetric(String resourceType, List<String> metrics) {
        metrics.forEach(metric -> azureMetricApiCalls.labels(resourceType + "_" + metric).inc());
    }

    private MetricsQueryOptions getQueryOptions(ResourceTypeConfig config){
        return new MetricsQueryOptions().setGranularity(Duration.ofMinutes(config.granularityInMins()));
    }

    private List<PrometheusMetric> getPrometheusMetrics(AzureResource resource, Response<MetricsQueryResult> metricsResponse) {
      List<MetricResult> metricResults = getMetricResponseResults(metricsResponse);
      return metricResults.stream()
              .map(metricResult -> new PrometheusMetric(resource, metricResult))
              .filter(PrometheusMetric::hasData).toList();
    }


    private List<MetricResult> getMetricResponseResults(Response<MetricsQueryResult> metricsResponse) {
        MetricsQueryResult result = metricsResponse.getValue();
        return null != result.getMetrics() ? result.getMetrics() : new ArrayList<>();
    }

    /* package */ MetricsQueryOptions setMetricsQueryInterval(MetricsQueryOptions options, int intervalInMillis) {
        options.setTimeInterval(QueryTimeInterval.parse(Duration.ofMillis(intervalInMillis).toString()));
        return options;
    }

    public static <T> Collection<List<T>> splitToMaxSizeChunks(List<T> inputList, int chunkSize) {
        AtomicInteger counter = new AtomicInteger();
        return inputList.stream().collect(Collectors.groupingBy(it -> counter.getAndIncrement() / chunkSize)).values();
    }
}
