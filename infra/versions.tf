terraform {
  required_version = ">= 1.8.0"

  # El bucket se crea con el módulo bootstrap; este bloque mantiene el estado
  # del stack principal fuera del equipo local.
  backend "gcs" {}

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 6.20"
    }
  }
}
