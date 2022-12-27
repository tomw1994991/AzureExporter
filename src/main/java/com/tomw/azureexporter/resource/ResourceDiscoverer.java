package com.tomw.azureexporter.resource;

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
import java.util.stream.Collectors;

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

    @Scheduled(initialDelayString = "${resource-discovery.initial-delay-millis:1000}", fixedDelayString = "${resource-discovery.interval-in-millis:300000}")
    protected void discoverResources() {
        QueryResponse response = queryResourceGraphAPI();
        Map<String, Set<AzureResource>> foundResources = getResourcesFromResourceGraphResponse(response);
        this.resources = addStorageAccountSubResources(foundResources);
        log.info("Refreshed Azure resource information. Found resources for types: {}", getDiscoveredResourceTypes());
        log.debug("Full resource collection: {}", resources);
    }

    private QueryResponse queryResourceGraphAPI() {
        QueryRequest query = new QueryRequest().withQuery("Resources").withOptions(new QueryRequestOptions().withResultFormat(ResultFormat.OBJECT_ARRAY));
        QueryResponse response = resourceManager.resourceProviders().resources(query);
        return response;
    }

    private Map<String, Set<AzureResource>> getResourcesFromResourceGraphResponse(QueryResponse response) {
        Map<String, Set<AzureResource>> refreshedResources = new ConcurrentHashMap<>();
        ((List<Map<String, Object>>) response.data()).stream().map(this::convertMapToResource).forEach(resource -> saveResource(resource, refreshedResources));
        return refreshedResources;
    }

    /*package*/ AzureResource convertMapToResource(final Map<String, Object> responseMap) {
        Map<String, String> tags = (Map<String, String>) responseMap.getOrDefault("tags", new HashMap<>());
        return new AzureResource((String) responseMap.get("id"), (String) responseMap.get("type"), null != tags ? tags : Collections.emptyMap());
    }

    private Map<String, Set<AzureResource>> addStorageAccountSubResources(Map<String, Set<AzureResource>> foundResources) {
        Set<AzureResource> storageAccounts = foundResources.getOrDefault("microsoft.storage/storageaccounts", new HashSet<>());
        Map<String, Set<AzureResource>> updatedResources = new HashMap<>(foundResources);
        updatedResources.put("microsoft.storage/storageaccounts/blobservices", getStorageAccountServiceResources(storageAccounts, "blobservices"));
        updatedResources.put("microsoft.storage/storageaccounts/fileservices", getStorageAccountServiceResources(storageAccounts, "fileservices"));
        updatedResources.put("microsoft.storage/storageaccounts/queueservices", getStorageAccountServiceResources(storageAccounts, "queueservices"));
        return updatedResources;
    }

    private Set<AzureResource> getStorageAccountServiceResources(Set<AzureResource> storageAccounts, String service){
        return storageAccounts.stream().map(storageAccount -> new AzureResource(storageAccount.getId() + "/" + service + "/default", "microsoft.storage/storageaccounts/" + service, Collections.emptyMap())).collect(Collectors.toSet());
    }

    private void saveResource(AzureResource resource, Map<String, Set<AzureResource>> resourceMap) {
        String lowerCaseType = resource.getType().toLowerCase();
        resourceMap.putIfAbsent(lowerCaseType, new HashSet<>());
        resourceMap.get(lowerCaseType).add(resource);
    }

    public Set<AzureResource> getResourcesForType(final String type) {
        return null == type ? new HashSet<>() : resources.getOrDefault(type.toLowerCase(), new HashSet<>());
    }

    public Set<String> getDiscoveredResourceTypes() {
        return resources.keySet();
    }

}
