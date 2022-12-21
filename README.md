# Azure-Exporter

- Simple implementation of a prometheus exporter for Azure Monitor metrics
- As of 2022/10/22, metrics can only be retrieved on a per-resource basis. The application will find all resources
  matching the configuration and return configured metrics.
- ResourceDiscoverer component finds resources. MetricsScraper component talks to Azure monitor.
- Actuator prometheus endpoint exposes metrics for prometheus.
- Docs for writing exporters: https://prometheus.io/docs/instrumenting/writing_exporters/

## Configuration

- For YML configuration, resource types are case insensitive. Metric names should be written exactly how Azure uses
  them.
- The exposed metrics for prometheus will be prefixed with 'azure_resourcetype_' and converted into an appropriate
  format: https://prometheus.io/docs/practices/naming/

### Example Metrics

| metricName in application.yml                      | exposed metric                            |
|----------------------------------------------------|-------------------------------------------|
| Microsoft.Storage/storageAccounts  Availability    | azure_storageaccounts_availability_bytes  |
| microsoft.category/resourceType    metricName      | azure_resourcetype_metricname_units       |
| microsoft.category/CASEINSENSITIVE   special-chars | azure_caseinsensitive_special_chars_units |

### Special Metrics

azure_monitor_metric_api_calls_total (counter) records the number of calls to the azure metrics api for cost tracking.

### Docker

- Using jib to produce docker image: https://github.com/GoogleContainerTools/jib/tree/master/jib-gradle-plugin
- For a local image: ./gradlew jibDockerBuild
- To build to ACR: ./gradlew jib -Djib.to.image='my_acr_name.azurecr.io/my-app'