package com.tomw.azureexporter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusScrapeEndpoint;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureObservability
class AzureExporterApplicationTests {

	@Autowired
	private PrometheusScrapeEndpointWebExtension promScrapeExtension;

	@Autowired
	private PrometheusScrapeEndpoint promScrapeDelegate;

	@Test
	void contextLoads() {
		assertThat(promScrapeDelegate).isNotNull();
		assertThat(promScrapeExtension).isNotNull();
	}

}
