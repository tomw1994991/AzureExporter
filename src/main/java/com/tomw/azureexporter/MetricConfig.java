package com.tomw.azureexporter;

import lombok.Data;

import java.util.List;

@Data
public class MetricConfig {

    private String resourceType;
    private List<String> metricNames;
}
