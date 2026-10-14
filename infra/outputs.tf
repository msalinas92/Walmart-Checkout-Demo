output "firestore_database" {
  description = "Base operacional que debe usar el backend."
  value       = google_firestore_database.default.name
}

output "pubsub_topic" {
  description = "Tópico de eventos confirmado por checkout."
  value       = google_pubsub_topic.checkout_transaction.name
}

output "pubsub_subscription" {
  description = "Suscripción pull usada por el consumidor analítico local."
  value       = google_pubsub_subscription.checkout_analytics_local.name
}

output "pubsub_bigquery_subscription" {
  description = "Suscripción administrada que proyecta eventos directamente en BigQuery."
  value       = google_pubsub_subscription.checkout_analytics_bigquery.name
}

output "bigquery_dataset" {
  description = "Dataset de la proyección analítica."
  value       = google_bigquery_dataset.retail_analytics.dataset_id
}

output "artifact_repository" {
  description = "Repositorio OCI usado por Cloud Build y Cloud Run."
  value       = google_artifact_registry_repository.checkout.repository_id
}

output "api_url" {
  description = "URL pública de la API Cloud Run cuando se despliega una imagen."
  value       = try(google_cloud_run_v2_service.checkout_api[0].uri, null)
}

output "api_image" {
  description = "Imagen actualmente desplegada en la API Cloud Run."
  value       = try(google_cloud_run_v2_service.checkout_api[0].template[0].containers[0].image, null)
}

output "web_url" {
  description = "URL pública del frontend Cloud Run cuando se despliega una imagen."
  value       = try(google_cloud_run_v2_service.checkout_web[0].uri, null)
}

output "web_image" {
  description = "Imagen actualmente desplegada en el frontend Cloud Run."
  value       = try(google_cloud_run_v2_service.checkout_web[0].template[0].containers[0].image, null)
}
