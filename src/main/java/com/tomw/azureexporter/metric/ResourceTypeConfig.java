package com.tomw.azureexporter.metric;

import lombok.Data;

import java.util.List;

@Data
public class ResourceTypeConfig {

    private String resourceType;
    private List<String> metrics;
}
