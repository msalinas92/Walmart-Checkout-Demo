provider "google" {
  project = var.project_id
}

data "google_project" "current" {
  project_id = var.project_id
}

locals {
  required_apis = toset([
    "firestore.googleapis.com",
    "pubsub.googleapis.com",
    "bigquery.googleapis.com",
    "run.googleapis.com",
    "artifactregistry.googleapis.com",
    "cloudbuild.googleapis.com",
    "iam.googleapis.com",
  ])

  api_deploy_enabled = try(trimspace(var.api_image) != "", false)
  web_deploy_enabled = try(trimspace(var.web_image) != "", false)

  labels = {
    application = "retail-checkout"
    environment = var.environment
    managed_by  = "terraform"
  }
}

# Aquí habilito sólo las APIs que requiere el flujo de checkout; no activo servicios de cómputo.
resource "google_project_service" "required" {
  for_each = local.required_apis

  project            = var.project_id
  service            = each.value
  disable_on_destroy = false
}

# Aquí preparo un único registro privado para las imágenes inmutables de la demo.
resource "google_artifact_registry_repository" "checkout" {
  project       = var.project_id
  location      = var.cloud_run_region
  repository_id = "retail-checkout"
  description   = "Imágenes OCI de la demo Retail Checkout."
  format        = "DOCKER"
  labels        = local.labels

  depends_on = [google_project_service.required["artifactregistry.googleapis.com"]]
}

# Aquí uso una identidad dedicada para que la API no ejecute con una cuenta por defecto.
resource "google_service_account" "checkout_api" {
  project      = var.project_id
  account_id   = "retail-checkout-api"
  display_name = "Retail Checkout API runtime"

  depends_on = [google_project_service.required["iam.googleapis.com"]]
}

# Aquí concedo sólo el acceso operacional que requiere la API en tiempo de ejecución.
resource "google_project_iam_member" "checkout_api_runtime" {
  for_each = toset([
    "roles/datastore.user",
    "roles/pubsub.publisher",
  ])

  project = var.project_id
  role    = each.value
  member  = "serviceAccount:${google_service_account.checkout_api.email}"
}

# Aquí despliego la API sólo después de que Make publique una imagen versionada.
resource "google_cloud_run_v2_service" "checkout_api" {
  count    = local.api_deploy_enabled ? 1 : 0
  name     = "retail-checkout-api"
  location = var.cloud_run_region
  ingress  = "INGRESS_TRAFFIC_ALL"

  template {
    service_account                  = google_service_account.checkout_api.email
    max_instance_request_concurrency = 80

    scaling {
      min_instance_count = 0
      max_instance_count = 1
    }

    containers {
      image = var.api_image

      ports {
        container_port = 8080
      }

      env {
        name  = "GOOGLE_CLOUD_PROJECT"
        value = var.project_id
      }

      # Aquí restrinjo CORS al frontend desplegado una vez que su URL esté disponible.
      env {
        name  = "CORS_ALLOWED_ORIGINS"
        value = local.web_deploy_enabled ? google_cloud_run_v2_service.checkout_web[0].uri : "*"
      }

      env {
        name  = "CHECKOUT_ANALYTICS_ENABLED"
        value = "false"
      }

      resources {
        cpu_idle = true
        limits = {
          cpu    = "1"
          memory = "512Mi"
        }
      }
    }
  }

  traffic {
    type    = "TRAFFIC_TARGET_ALLOCATION_TYPE_LATEST"
    percent = 100
  }

  depends_on = [
    google_project_service.required["run.googleapis.com"],
    google_project_iam_member.checkout_api_runtime,
  ]
}

# Aquí habilito invocación pública exclusivamente porque la demo no tiene autenticación.
resource "google_cloud_run_v2_service_iam_member" "checkout_api_public" {
  count    = local.api_deploy_enabled ? 1 : 0
  project  = var.project_id
  location = google_cloud_run_v2_service.checkout_api[0].location
  name     = google_cloud_run_v2_service.checkout_api[0].name
  role     = "roles/run.invoker"
  member   = "allUsers"
}

# Aquí despliego la interfaz Next.js cuando su imagen contiene la URL definitiva de la API.
resource "google_cloud_run_v2_service" "checkout_web" {
  count    = local.web_deploy_enabled ? 1 : 0
  name     = "retail-checkout-web"
  location = var.cloud_run_region
  ingress  = "INGRESS_TRAFFIC_ALL"

  template {
    max_instance_request_concurrency = 80

    scaling {
      min_instance_count = 0
      max_instance_count = 1
    }

    containers {
      image = var.web_image

      ports {
        container_port = 8080
      }

      resources {
        cpu_idle = true
        limits = {
          cpu    = "1"
          memory = "256Mi"
        }
      }
    }
  }

  traffic {
    type    = "TRAFFIC_TARGET_ALLOCATION_TYPE_LATEST"
    percent = 100
  }

  depends_on = [google_project_service.required["run.googleapis.com"]]
}

resource "google_cloud_run_v2_service_iam_member" "checkout_web_public" {
  count    = local.web_deploy_enabled ? 1 : 0
  project  = var.project_id
  location = google_cloud_run_v2_service.checkout_web[0].location
  name     = google_cloud_run_v2_service.checkout_web[0].name
  role     = "roles/run.invoker"
  member   = "allUsers"
}

# Aquí creo la fuente operacional de catálogo, promociones, cotizaciones, órdenes e idempotencia.
resource "google_firestore_database" "default" {
  project                           = var.project_id
  name                              = "(default)"
  location_id                       = var.firestore_location
  type                              = "FIRESTORE_NATIVE"
  concurrency_mode                  = "OPTIMISTIC"
  point_in_time_recovery_enablement = "POINT_IN_TIME_RECOVERY_DISABLED"
  delete_protection_state           = "DELETE_PROTECTION_ENABLED"
  deletion_policy                   = "ABANDON"

  depends_on = [google_project_service.required["firestore.googleapis.com"]]
}

# Aquí publico un único evento de negocio versionado para desacoplar checkout y analítica.
resource "google_pubsub_topic" "checkout_transaction" {
  project = var.project_id
  name    = "checkout.transaction.v1"
  labels  = local.labels

  message_retention_duration = "86400s"

  depends_on = [google_project_service.required["pubsub.googleapis.com"]]
}

# Aquí dejo una suscripción pull para que el consumidor Java local procese eventos hacia BigQuery.
resource "google_pubsub_subscription" "checkout_analytics_local" {
  project = var.project_id
  name    = "checkout-analytics-local"
  topic   = google_pubsub_topic.checkout_transaction.id
  labels  = local.labels

  ack_deadline_seconds       = 30
  message_retention_duration = "86400s"
  retain_acked_messages      = false

  retry_policy {
    minimum_backoff = "10s"
    maximum_backoff = "60s"
  }
}

# Aquí materializo los eventos directamente en BigQuery para que la analítica no dependa de un proceso local.
resource "google_bigquery_dataset_iam_member" "pubsub_writer" {
  project    = var.project_id
  dataset_id = google_bigquery_dataset.retail_analytics.dataset_id
  role       = "roles/bigquery.dataEditor"
  member     = "serviceAccount:service-${data.google_project.current.number}@gcp-sa-pubsub.iam.gserviceaccount.com"
}

resource "google_project_iam_member" "pubsub_metadata_viewer" {
  project = var.project_id
  role    = "roles/bigquery.metadataViewer"
  member  = "serviceAccount:service-${data.google_project.current.number}@gcp-sa-pubsub.iam.gserviceaccount.com"
}

resource "google_pubsub_subscription" "checkout_analytics_bigquery" {
  project = var.project_id
  name    = "checkout-analytics-bigquery"
  topic   = google_pubsub_topic.checkout_transaction.id
  labels  = local.labels

  ack_deadline_seconds       = 60
  message_retention_duration = "86400s"

  bigquery_config {
    table               = "${var.project_id}.${google_bigquery_dataset.retail_analytics.dataset_id}.${google_bigquery_table.checkout_transactions_raw.table_id}"
    use_table_schema    = true
    drop_unknown_fields = true
  }

  depends_on = [google_bigquery_dataset_iam_member.pubsub_writer, google_project_iam_member.pubsub_metadata_viewer]
}

# Aquí defino un dataset de corta vida para que los datos ficticios no crezcan indefinidamente.
resource "google_bigquery_dataset" "retail_analytics" {
  project                     = var.project_id
  dataset_id                  = "retail_analytics"
  friendly_name               = "Retail Checkout Analytics"
  description                 = "Eventos ficticios del checkout para la demo."
  location                    = var.bigquery_location
  default_table_expiration_ms = 5184000000 # 60 días
  delete_contents_on_destroy  = false
  labels                      = local.labels

  depends_on = [google_project_service.required["bigquery.googleapis.com"]]
}

# Aquí mantengo eventos crudos, particionados y baratos de consultar por fecha y medio de pago.
resource "google_bigquery_table" "checkout_transactions_raw" {
  project             = var.project_id
  dataset_id          = google_bigquery_dataset.retail_analytics.dataset_id
  table_id            = "checkout_transactions_raw"
  deletion_protection = true
  labels              = local.labels

  time_partitioning {
    type  = "DAY"
    field = "occurredAt"
  }

  clustering = ["paymentMethod", "orderId"]

  schema = jsonencode([
    { name = "eventId", type = "STRING", mode = "REQUIRED" },
    { name = "eventType", type = "STRING", mode = "REQUIRED" },
    { name = "occurredAt", type = "TIMESTAMP", mode = "REQUIRED" },
    { name = "orderId", type = "STRING", mode = "REQUIRED" },
    { name = "subtotal", type = "NUMERIC", mode = "REQUIRED" },
    { name = "productDiscount", type = "NUMERIC", mode = "REQUIRED" },
    { name = "cartDiscount", type = "NUMERIC", mode = "REQUIRED" },
    { name = "paymentDiscount", type = "NUMERIC", mode = "REQUIRED" },
    { name = "finalTotal", type = "NUMERIC", mode = "REQUIRED" },
    { name = "paymentMethod", type = "STRING", mode = "REQUIRED" },
    { name = "promotionCodes", type = "STRING", mode = "REPEATED" },
    { name = "schemaVersion", type = "INTEGER", mode = "REQUIRED" },
  ])
}

# Aquí expongo una vista que conserva solamente la última entrega de cada evento.
resource "google_bigquery_table" "checkout_transactions" {
  project             = var.project_id
  dataset_id          = google_bigquery_dataset.retail_analytics.dataset_id
  table_id            = "checkout_transactions"
  deletion_protection = true
  labels              = local.labels

  view {
    use_legacy_sql = false
    query          = <<-SQL
      SELECT * EXCEPT(row_number)
      FROM (
        SELECT *, ROW_NUMBER() OVER (PARTITION BY eventId ORDER BY occurredAt DESC) AS row_number
        FROM `${var.project_id}.retail_analytics.checkout_transactions_raw`
      )
      WHERE row_number = 1
    SQL
  }

  depends_on = [google_bigquery_table.checkout_transactions_raw]
}
