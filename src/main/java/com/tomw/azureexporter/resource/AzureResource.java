package com.tomw.azureexporter.resource;

import lombok.*;

import java.util.Map;

@Data
@Builder
public class AzureResource {

    @NonNull
    private final String id;

    @NonNull
    private final String type;

    @Getter(AccessLevel.PACKAGE)
    @NonNull
    private final Map<String, String> tags;

    public String getTagValue(final String key) {
        return tags.get(key);
    }

    public boolean hasTag(final String key){
        return tags.containsKey(key);
    }
}
