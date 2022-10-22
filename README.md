# Azure-Exporter

- Simple implementation of a prometheus exporter for Azure Monitor metrics
- As of 2022/10/22, metrics can only be retrieved on a per-resource basis. The application will find all resources matching the configuration and return configured metrics.
- ResourceDiscoverer component finds resources. MetricsScraper component talks to Azure monitor.
- Actuator prometheus endpoint exposes metrics for prometheus.
- Docs for writing exporters: https://prometheus.io/docs/instrumenting/writing_exporters/ 

## Configuration

- For YML configuration, resource types are case insensitive. Metric names should be written exactly how Azure uses them.
- The exposed metrics for prometheus will be prefixed with 'azure_resourcetype_' and converted into an appropriate format: https://prometheus.io/docs/practices/naming/

### Example Metrics

Microsoft.Storage/storageAccounts  Availability -> azure_storageaccounts_availability