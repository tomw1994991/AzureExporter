package com.tomw.azureexporter.metric;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MetricNamingTest {


    @ParameterizedTest
    @CsvSource({
            "storageAccounts, Availability, azure_storageaccounts_availability",
            "Microsoft.Storage/storageAccounts, AvailabilityPercentTotal, azure_storageaccounts_availabilitypercenttotal",
            "virtualMachines, CPU_USAGE, azure_virtualmachines_cpu_usage",
            "Microsoft.Compute/VirtualMachines, CPU, azure_virtualmachines_cpu",
            "unusual.characters, non-alphanumeric, azure_unusual_characters_non_alphanumeric"
    })
    public void testCreatePrometheusMetricName_validName(String resourceType, String metricName, String expectedPrometheusName) {
        assertEquals(expectedPrometheusName, MetricNaming.computePrometheusMetricName(resourceType, metricName));
    }

    @ParameterizedTest
    @CsvSource({
            "Microsoft.Storage/storageAccounts, storageAccounts",
            "a, a",
            "123_12345/123123, 123123",
            "a/b/c/d, d",
            "a/, a/",
            "virtualMachines, virtualMachines",
    })
    public void testSubstringAfterSlash_variousValues(String input, String expectedSubstring) {
        assertEquals(expectedSubstring, MetricNaming.substringAfterSlash(input));
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void testSubstringAfterSlash_nullAndEmptyReturned(String input) {
        assertEquals("", MetricNaming.substringAfterSlash(input));
    }
}
