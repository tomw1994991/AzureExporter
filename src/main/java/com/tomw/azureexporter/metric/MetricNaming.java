package com.tomw.azureexporter.metric;

import java.util.Locale;
import java.util.Objects;

public abstract class MetricNaming {

    private final static String OUTPUT_METRIC_PREFIX = "azure";
    private final static String DEFAULT_SEPARATOR = "_";


    public static String computePrometheusMetricName(String resourceType, String azureMetric){
        return String.join("_", OUTPUT_METRIC_PREFIX, resourceType,
                replaceNonAlphanumeric(azureMetric, DEFAULT_SEPARATOR)).toLowerCase(Locale.ROOT);
    }

    private static String replaceNonAlphanumeric(final String original, final String replacement) {
        return original.replaceAll("[^A-Za-z\\d]", replacement);
    }

    /* package*/ static final String substringAfterSlash(String original) {
        String sanitisedInput = Objects.requireNonNullElse(original, "");
        int lastSlash = sanitisedInput.lastIndexOf("/");
        String afterSlash = (lastSlash > 0 && lastSlash + 1 < sanitisedInput.length()) ? sanitisedInput.substring(lastSlash + 1) : sanitisedInput;
        return afterSlash;
    }
}
