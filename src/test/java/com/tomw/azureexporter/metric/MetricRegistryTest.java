package com.tomw.azureexporter.metric;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MetricRegistryTest {


    @ParameterizedTest
    @CsvSource({
            "Microsoft.Storage/storageAccounts, Availability, azure_storageaccounts_availability",
            "Microsoft.Storage/storageAccounts, AvailabilityPercentTotal, azure_storageaccounts_availabilitypercenttotal",
            "virtualMachines, CPU_USAGE, azure_virtualmachines_cpu_usage",
            "Microsoft.Compute/VirtualMachines, CPU, azure_virtualmachines_cpu"
    })
    public void testCreatePrometheusMetricName_validName( String azResourceType, String azMetricName, String expectedPrometheusName){
        assertEquals(expectedPrometheusName, MetricRegistry.createPrometheusMetricName(azMetricName, azResourceType));
    }


}
