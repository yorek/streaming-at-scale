output "subscription_id" {
  description = "The Azure Subscription identifier."
  value       = data.azurerm_client_config.current.subscription_id
}

output "iothub_resource_id" {
  value       = module.iothub.resource_id
  description = "The ARM Resource ID of the IoT Hub."
}

output "eventhub_namespace_names" {
  description = "The list of Event Hub namespaces."
  value       = "${module.eventhubs_adt.eventhub_namespace_name} ${module.eventhubs_tsi.eventhub_namespace_name}"
}

output "iothub_registry_write_primary_connection_string" {
  value       = module.iothub.registry_write_primary_connection_string
  description = "The primary connection string to manage IoT Hub device registry."
  sensitive   = true
}

output "digital_twins_explorer_url" {
  value       = ""
  description = "The URL of the Azure Digital Twins Explorer webapp."
}

output "digital_twins_service_url" {
  value       = module.digital_twins.service_url
  description = "The URL of the Azure Digital Twins instance service."
}

output "time_series_insights_data_access_fqdn" {
  value       = module.time_series_insights.data_access_fqdn
  description = "The Time Series Insights data access host name."
}