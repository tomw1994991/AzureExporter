package com.tomw.azureexporter;

import io.prometheus.client.CollectorRegistry;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureObservability
@DirtiesContext
@Slf4j
public class IntegrationWithPrometheusContainerTests {

    private static final int PROMETHEUS_PORT = 9090;

    @BeforeAll
    public static void beforeAll() {
        CollectorRegistry.defaultRegistry.clear();
        Testcontainers.exposeHostPorts(8090);
    }

    @Container
    public static GenericContainer prometheus = new GenericContainer(DockerImageName.parse("prom/prometheus")).withExposedPorts(PROMETHEUS_PORT)
            .withCopyFileToContainer(MountableFile.forClasspathResource("prometheus.yml"), "/etc/prometheus/prometheus.yml")
            .withAccessToHost(true)
            .withLogConsumer(new Slf4jLogConsumer(log));

    @Test
    @Disabled("Requires local azure login and docker daemon")
    public void testPrometheusIntegration_canScrapeAzureMetrics() {
        HttpWaitStrategy waitStrategy = new HttpWaitStrategy().forPort(PROMETHEUS_PORT).forStatusCode(200).forPath("/api/v1/label/__name__/values")
                .forResponsePredicate(response -> prometheusResponseHasMetric(response, "azure_"));
        prometheus.waitingFor(waitStrategy).start();
        //while(true){} //Allows inspection of local prometheus alongside the exporter e.g. docker ps | grep prom   and localhost:8090/actuator/prometheus
    }

    private boolean prometheusResponseHasMetric(String response, String metric) {
        log.info("Checking response {} for metric {}", response, metric);
        return response.contains(metric);
    }
}
