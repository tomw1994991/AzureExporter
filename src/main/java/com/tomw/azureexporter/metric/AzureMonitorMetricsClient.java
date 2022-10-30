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
import com.tomw.azureexporter.metric.config.ResourceTypeConfig;
import com.tomw.azureexporter.metric.config.ScrapeConfigProps;
import com.tomw.azureexporter.resource.AzureResource;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class AzureMonitorMetricsClient {

    private final ScrapeConfigProps scrapeConfig;
    private MetricsQueryClient queryClient;

    public AzureMonitorMetricsClient(ScrapeConfigProps scrapeConfig) {
        this.scrapeConfig = scrapeConfig;
        queryClient = new MetricsQueryClientBuilder()
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
    }

    public List<MetricResult> queryResourceMetrics(AzureResource resource, ResourceTypeConfig config) {
        MetricsQueryOptions queryOptions = getQueryOptions(config);
        Response<MetricsQueryResult> metricsResponse = queryClient
                .queryResourceWithResponse(resource.getId(), config.metrics(),
                        setMetricsQueryInterval(queryOptions, scrapeConfig.getIntervalInMillis()), Context.NONE);
        return getMetricResults(metricsResponse);
    }

    private MetricsQueryOptions getQueryOptions(ResourceTypeConfig config){
        return new MetricsQueryOptions().setGranularity(Duration.ofMinutes(config.granularityInMins()));
    }

    private List<MetricResult> getMetricResults(Response<MetricsQueryResult> metricsResponse) {
        MetricsQueryResult result = metricsResponse.getValue();
        return null != result.getMetrics() ? result.getMetrics() : new ArrayList<MetricResult>();
    }

    /* package */ MetricsQueryOptions setMetricsQueryInterval(MetricsQueryOptions options, int intervalInMillis) {
        options.setTimeInterval(QueryTimeInterval.parse(Duration.ofMillis(intervalInMillis).toString()));
        return options;
    }

}
