package com.tomw.azureexporter.resource;

import com.azure.resourcemanager.resourcegraph.ResourceGraphManager;
import com.azure.resourcemanager.resourcegraph.models.QueryResponse;
import com.azure.resourcemanager.resourcegraph.models.ResourceProviders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ResourceDiscovererTest {

    ResourceDiscoverer resourceDiscoverer;
    ResourceGraphManager resourceGraphManager = mock(ResourceGraphManager.class);
    ResourceProviders resourceProviders = mock(ResourceProviders.class);
    QueryResponse queryResponse = mock(QueryResponse.class);

    @BeforeEach
    public void setup() {
        reset(queryResponse);
        when(resourceGraphManager.resourceProviders()).thenReturn(resourceProviders);
        when(resourceProviders.resources(any())).thenReturn(queryResponse);
        resourceDiscoverer = new ResourceDiscoverer();
        resourceDiscoverer.setResourceManager(resourceGraphManager);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"stor", "*", "///?"})
    public void testDiscoverResources_noMatchingResources_noError(String type) {
        defaultQueryResponse();
        resourceDiscoverer.discoverResources();
        assertEquals(0, resourceDiscoverer.getResourcesForType(type).size());
    }

    @Test
    public void testDiscoverResources_matchingResources_returned() {
        defaultQueryResponse();
        resourceDiscoverer.discoverResources();
        assertEquals(1, resourceDiscoverer.getResourcesForType("storage").size());
    }

    @Test
    public void testDiscoverResources_storageAccountSubResourcesAdded() {
        Map<String, Object> resource = setupResourceMap("id1", "Microsoft.Storage/storageAccounts", new HashMap<>());
        setupQueryResponseFromResourceMaps(List.of(resource));
        resourceDiscoverer.discoverResources();
        assertEquals(1, resourceDiscoverer.getResourcesForType("microsoft.storage/storageaccounts/blobservices").size());
        assertEquals(4, resourceDiscoverer.getDiscoveredResourceTypes().size());
    }

    @Test
    public void testGetResourcesForType_multipleResources_returned_caseIgnored() {

        Map<String, Object> resource = setupResourceMap("id1", "storage", new HashMap<>());
        Map<String, Object> resource2 = setupResourceMap("id2", "storage", new HashMap<>());
        Map<String, Object> resource3 = setupResourceMap("id3", "compute", new HashMap<>());

        setupQueryResponseFromResourceMaps(Arrays.asList(resource, resource2, resource3));
        resourceDiscoverer.discoverResources();
        assertEquals(2, resourceDiscoverer.getResourcesForType("STORage").size());
    }

    @Test
    public void testGetResourcesForType_deletedResource_deletedFromCollection() {

        Map<String, Object> resource = setupResourceMap("id1", "storage", new HashMap<>());
        setupQueryResponseFromResourceMaps(List.of(resource));
        resourceDiscoverer.discoverResources();
        assertEquals(1, resourceDiscoverer.getResourcesForType("storage").size());

        setupQueryResponseFromResourceMaps(new ArrayList<>());
        resourceDiscoverer.discoverResources();
        assertEquals(0, resourceDiscoverer.getResourcesForType("storage").size());
    }


    @Test
    public void testGetResourcesForType_resourcesNotYetDiscovered() {
        Set<AzureResource> resources = resourceDiscoverer.getResourcesForType("storage");
        assertEquals(resources.size(), 0);
    }

    @ParameterizedTest
    @CsvSource({
            "id1, storage",
            "web.address/abc1234/defgh56789/aoaisdjoij, Types.storage",
    })
    public void testConvertMapToResource_validWithNoTags(String id, String type) {
        Map<String, Object> resourceMap = setupResourceMap(id, type, new HashMap<>());
        AzureResource resource = resourceDiscoverer.convertMapToResource(resourceMap);
        assertEquals(resource.getId(), id);
        assertEquals(resource.getType(), type);
        assertEquals(0, resource.getTags().size());
    }

    @Test
    public void testConvertMapToResource_nullTags() {
        Map<String, Object> resourceMap = setupResourceMap("id1", "type1", null);
        AzureResource resource = resourceDiscoverer.convertMapToResource(resourceMap);
        assertEquals(0, resource.getTags().size());
    }

    @Test
    public void testConvertMapToResource_withTags() {
        Map<String, String> tags = new HashMap<>();
        tags.put("key1", "value1");
        tags.put("key2", "value2");
        Map<String, Object> resourceMap = setupResourceMap("exampleId", "exampleType", tags);
        AzureResource resource = resourceDiscoverer.convertMapToResource(resourceMap);
        assertTrue(resource.hasTag("key2"));
        assertEquals("value1", resource.getTagValue("key1"));
    }

    @Test
    public void testConvertMapToResource_invalidId_error() {
        Map<String, Object> resourceMap = setupResourceMap(null, "Types.storage", new HashMap<>());
        assertThrows(NullPointerException.class, () -> resourceDiscoverer.convertMapToResource(resourceMap));
    }

    @Test
    public void testConvertMapToResource_invalidType_error() {
        Map<String, Object> resourceMap = setupResourceMap("id123", null, new HashMap<>());
        assertThrows(NullPointerException.class, () -> resourceDiscoverer.convertMapToResource(resourceMap));
    }

    private void defaultQueryResponse() {
        Map<String, Object> resource = setupResourceMap("id1", "storage", new HashMap<>());
        setupQueryResponseFromResourceMaps(List.of(resource));
    }

    private Map<String, Object> setupResourceMap(String id, String type, Map<String, String> tags) {
        Map<String, Object> resource = new HashMap<>();
        resource.put("id", id);
        resource.put("type", type);
        resource.put("tags", tags);
        return resource;
    }

    private void setupQueryResponseFromResourceMaps(List<Map<String, Object>> responseData) {
        when(queryResponse.data()).thenReturn(responseData);
    }

}
