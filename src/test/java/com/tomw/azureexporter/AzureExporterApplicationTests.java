package com.tomw.azureexporter;

import io.prometheus.client.CollectorRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@AutoConfigureObservability
@DirtiesContext
class AzureExporterApplicationTests {

    @BeforeAll
    public static void setup() {
        CollectorRegistry.defaultRegistry.clear();
    }

    @Test
    void contextLoads() {
    }

}
