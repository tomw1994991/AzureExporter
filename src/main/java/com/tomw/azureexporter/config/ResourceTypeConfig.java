package com.tomw.azureexporter.config;

import java.util.List;

public record ResourceTypeConfig(String resourceType, List<String> metrics, int granularityInMins) {
}
