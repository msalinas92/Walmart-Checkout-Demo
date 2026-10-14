output "state_bucket_name" {
  description = "Bucket que debe configurarse como backend GCS del stack principal."
  value       = google_storage_bucket.terraform_state.name
}
