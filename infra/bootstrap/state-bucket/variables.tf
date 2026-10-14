variable "project_id" {
  description = "Proyecto que contiene el bucket de estado."
  type        = string
  default     = "walmart-demo-511500"
}

variable "state_bucket_name" {
  description = "Nombre global y único del bucket de estado remoto."
  type        = string
  default     = "walmart-demo-511500-retail-checkout-tfstate"
}

variable "state_bucket_location" {
  description = "Ubicación multirregional del estado; se mantiene en US junto a BigQuery."
  type        = string
  default     = "US"
}
