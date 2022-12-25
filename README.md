# Azure-Exporter

- Simple implementation of a prometheus exporter for Azure Monitor metrics
- As of 2022/10/22, metrics can only be retrieved on a per-resource basis. The application will find all resources
  matching the configuration and return configured metrics.
- ResourceDiscoverer component finds resources. MetricsScraper component talks to Azure monitor.
- Actuator prometheus endpoint exposes metrics for prometheus.
- Docs for writing exporters: https://prometheus.io/docs/instrumenting/writing_exporters/
- Metrics are at localhost:8090/actuator/prometheus by default

## Configuration

- For YML configuration, resource types are case insensitive. Metric names should be written exactly how Azure uses
  them.
- Can use the azure cli to list potential metric definitions https://learn.microsoft.com/en-us/cli/azure/monitor/metrics?view=azure-cli-latest#az-monitor-metrics-list-definitions
- The exposed metrics for prometheus will be prefixed with 'azure_resourcetype_' and converted into an appropriate
  format: https://prometheus.io/docs/practices/naming/
- supports SCCS 

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
- Docker image on docker hub available as tomw1994991/azure-exporter https://hub.docker.com/r/tomw1994991/azure-exporter
- If building behind a proxy, configure gradle proxy variables in ~/.gradle/gradle.properties https://stackoverflow.com/questions/5991194/gradle-proxy-configuration 