package com.tomw.azureexporter;

import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.resourcegraph.ResourceGraphManager;
import com.azure.resourcemanager.resourcegraph.models.QueryRequest;
import com.azure.resourcemanager.resourcegraph.models.QueryRequestOptions;
import com.azure.resourcemanager.resourcegraph.models.QueryResponse;
import com.azure.resourcemanager.resourcegraph.models.ResultFormat;
import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ResourceDiscoverer {
    @Setter(AccessLevel.PACKAGE)
    private ResourceGraphManager resourceManager;

    private Map<String, Set<AzureResource>> resources;

    public ResourceDiscoverer() {
        resources = new ConcurrentHashMap<>();
        AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);
        resourceManager = ResourceGraphManager.authenticate(new DefaultAzureCredentialBuilder().build(), profile);
    }

    @Scheduled(initialDelay = 1000L, fixedDelayString = "${resource-discovery.interval-in-millis:300000}")
    protected void discoverResources() {
        QueryRequest query = new QueryRequest().withQuery("Resources").withOptions(new QueryRequestOptions().withResultFormat(ResultFormat.OBJECT_ARRAY));
        QueryResponse response = resourceManager.resourceProviders().resources(query);
        saveResources(response);
        log.info("Refreshed Azure resource information.");
        log.debug("Full resource collection: {}", resources);
    }

    private void saveResources(QueryResponse response) {
        ((List<Map<String, Object>>) response.data()).stream().forEach(responseMap -> {
            AzureResource resource = convertMapToResource(responseMap);
            saveResource(resource);
        });
    }

    /*package*/ AzureResource convertMapToResource(final Map<String, Object> responseMap){
        return new AzureResource((String) responseMap.get("id"), (String) responseMap.get("type"), (Map<String, String>) responseMap.getOrDefault("tags", new HashMap<>()));
    }

    private void saveResource(AzureResource resource) {
        String lowerCaseType = resource.getType().toLowerCase();
        resources.putIfAbsent(lowerCaseType, new HashSet<>());
        resources.get(lowerCaseType).add(resource);
    }

    public Set<AzureResource> getResourcesForType(final String type) {
        return null == type ? new HashSet<>() : resources.getOrDefault(type.toLowerCase(), new HashSet<>());
    }

    public Set<String> getDiscoveredResourceTypes(){
        return resources.keySet();
    }

}
