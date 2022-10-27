package com.tomw.azureexporter.metric;

import java.util.List;

public record ResourceTypeConfig(String resourceType, List<String> metrics) {
}
