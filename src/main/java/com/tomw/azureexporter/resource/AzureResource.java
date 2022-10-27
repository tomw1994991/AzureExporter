package com.tomw.azureexporter.resource;

import lombok.*;

import java.util.Map;

import static lombok.AccessLevel.PACKAGE;
import static lombok.AccessLevel.PUBLIC;

@Data
@Builder
@RequiredArgsConstructor(access = PUBLIC)
public class AzureResource {

    @NonNull
    private final String id;

    @NonNull
    private final String type;

    @Getter(PACKAGE)
    @NonNull
    private final Map<String, String> tags;

    public String getTagValue(final String key) {
        return tags.get(key);
    }

    public boolean hasTag(final String key) {
        return tags.containsKey(key);
    }
}
