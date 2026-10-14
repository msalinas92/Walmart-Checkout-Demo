terraform {
  required_version = ">= 1.8.0"

  # Tras el bootstrap inicial, este estado también vive en el bucket creado.
  backend "gcs" {}

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 6.20"
    }
  }
}
