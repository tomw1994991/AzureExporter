package com.tomw.azureexporter.metric;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class MetricNaming {

    private final static String OUTPUT_METRIC_PREFIX = "azure";
    private final static String DEFAULT_SEPARATOR = "_";

    private final static Pattern UNDIFFERENTIATED_SUFFIX = Pattern.compile("^(.*)/[a-zA-Z]+/default$");


    public static String computePrometheusMetricName(String resourceType, String azureMetric){
        return String.join("_", OUTPUT_METRIC_PREFIX, replaceNonAlphanumeric(substringAfterSlash(resourceType)),
                replaceNonAlphanumeric(azureMetric)).toLowerCase(Locale.ROOT);
    }

    private static String replaceNonAlphanumeric(final String original) {
        return original.replaceAll("[^A-Za-z\\d]", MetricNaming.DEFAULT_SEPARATOR);
    }

    /* package*/ static String removeUndifferentiatedSuffix(String original){
        Matcher matchUndifferentiatedSuffix = UNDIFFERENTIATED_SUFFIX.matcher(original);
        return matchUndifferentiatedSuffix.matches() ? matchUndifferentiatedSuffix.group(1) : original;
    }

    /* package*/ static String substringAfterSlash(String original) {
        String sanitisedInput = Objects.requireNonNullElse(original, "");
        int lastSlash = sanitisedInput.lastIndexOf("/");
        return (lastSlash > 0 && lastSlash + 1 < sanitisedInput.length()) ? sanitisedInput.substring(lastSlash + 1) : sanitisedInput;
    }
}
