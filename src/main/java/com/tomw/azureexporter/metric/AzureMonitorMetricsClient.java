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
import com.tomw.azureexporter.metric.config.ScrapeConfigProps;
import com.tomw.azureexporter.resource.AzureResource;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class AzureMonitorMetricsClient {

    private final ScrapeConfigProps scrapeConfig;
    private MetricsQueryClient metricsQueryClient;
    private final MetricsQueryOptions defaultQueryOptions;


    public AzureMonitorMetricsClient(ScrapeConfigProps scrapeConfig) {
        this.scrapeConfig = scrapeConfig;
        metricsQueryClient = new MetricsQueryClientBuilder()
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
        defaultQueryOptions = new MetricsQueryOptions().setGranularity(Duration.ofMinutes(scrapeConfig.getGranularityInMins()));
    }

    public List<MetricResult> retrieveResourceMetrics(AzureResource resource, List<String> metricsToQuery) {
        Response<MetricsQueryResult> metricsResponse = metricsQueryClient
                .queryResourceWithResponse(resource.getId(), metricsToQuery,
                        setMetricsQueryInterval(defaultQueryOptions, scrapeConfig.getIntervalInMillis()), Context.NONE);
        MetricsQueryResult result = metricsResponse.getValue();
        return null != result.getMetrics() ? result.getMetrics() : new ArrayList<MetricResult>();
    }

    /* package */ MetricsQueryOptions setMetricsQueryInterval(MetricsQueryOptions options, int intervalInMillis) {
        options.setTimeInterval(QueryTimeInterval.parse(Duration.ofMillis(intervalInMillis).toString()));
        return options;
    }

}
