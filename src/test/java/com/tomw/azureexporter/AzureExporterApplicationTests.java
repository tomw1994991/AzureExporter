package com.tomw.azureexporter;

import io.prometheus.client.CollectorRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@AutoConfigureObservability
class AzureExporterApplicationTests {

    @BeforeEach
    public void setup(){
        CollectorRegistry.defaultRegistry.clear();
    }

    @Test
    void contextLoads() {
    }

}
