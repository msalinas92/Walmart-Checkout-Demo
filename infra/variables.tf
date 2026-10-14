variable "project_id" {
  description = "ID del proyecto Google Cloud que alojará la demo."
  type        = string
  default     = "walmart-demo-511500"
}

variable "firestore_location" {
  description = "Región de la base Firestore Native. Esta ubicación no se puede cambiar después de crearla."
  type        = string
  default     = "southamerica-west1"
}

variable "bigquery_location" {
  description = "Ubicación del dataset analítico. US permite operar con BigQuery Sandbox."
  type        = string
  default     = "US"
}

variable "environment" {
  description = "Etiqueta de entorno para identificar los recursos de demo."
  type        = string
  default     = "demo"
}

variable "cloud_run_region" {
  description = "Región de Cloud Run y Artifact Registry para los componentes desplegados."
  type        = string
  default     = "southamerica-west1"
}

variable "api_image" {
  description = "Imagen OCI de la API. Vacía hasta ejecutar make deploy-api."
  type        = string
  default     = null
  nullable    = true
}

variable "web_image" {
  description = "Imagen OCI del frontend. Vacía hasta ejecutar make deploy-web."
  type        = string
  default     = null
  nullable    = true
}

