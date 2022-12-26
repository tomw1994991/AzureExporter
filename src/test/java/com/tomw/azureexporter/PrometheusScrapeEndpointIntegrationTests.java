package com.tomw.azureexporter;


import io.prometheus.client.CollectorRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability
@DirtiesContext
public class PrometheusScrapeEndpointIntegrationTests {

    private static final String ACTUATOR_PROMETHEUS_ENDPOINT = "/actuator/prometheus";
    @Autowired
    private MockMvc mockMvc;

    @BeforeAll
    public static void setup() {
        CollectorRegistry.defaultRegistry.clear();
    }

    @Test
    public void testScrapeEndpoint_noParameters_ok() throws Exception {
        mockMvc.perform(get(ACTUATOR_PROMETHEUS_ENDPOINT)).andExpect(status().isOk());
    }

    @Test
    public void testScrapeEndpoint_noParameters_metricsReturned() throws Exception {
        MvcResult result = mockMvc.perform(get(ACTUATOR_PROMETHEUS_ENDPOINT)).andReturn();
        assertTrue(result.getResponse().getContentAsString().contains("TYPE"));
    }
}
