provider "google" {
  project = var.project_id
}

# Aquí habilito únicamente Cloud Storage para poder crear el backend remoto.
resource "google_project_service" "storage" {
  project            = var.project_id
  service            = "storage.googleapis.com"
  disable_on_destroy = false
}

# Aquí creo un bucket privado, versionado y no destruible: almacena el historial
# de estado para que el equipo y CI trabajen sobre la misma fuente de verdad.
resource "google_storage_bucket" "terraform_state" {
  project                     = var.project_id
  name                        = var.state_bucket_name
  location                    = var.state_bucket_location
  storage_class               = "STANDARD"
  force_destroy               = false
  uniform_bucket_level_access = true
  public_access_prevention    = "enforced"

  versioning {
    enabled = true
  }

  labels = {
    application = "retail-checkout"
    environment = "demo"
    managed_by  = "terraform"
    purpose     = "terraform-state"
  }

  depends_on = [google_project_service.storage]
}
