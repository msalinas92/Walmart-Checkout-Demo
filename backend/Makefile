SHELL := /bin/sh

GRADLE ?= ./gradlew
PROJECT_ID ?= walmart-demo-511500
REGION ?= southamerica-west1
REPOSITORY ?= retail-checkout
IMAGE_TAG ?= $(shell date -u +%Y%m%d%H%M%S)
IMAGE := $(REGION)-docker.pkg.dev/$(PROJECT_ID)/$(REPOSITORY)/checkout-api:$(IMAGE_TAG)
export GOOGLE_CLOUD_PROJECT := $(PROJECT_ID)

.DEFAULT_GOAL := help

.PHONY: help build test run run-gcp run-analytics image deploy clean

help: ## Muestro las tareas disponibles del backend.
	@awk 'BEGIN {FS = ":.*##"}; /^[a-zA-Z0-9_-]+:.*##/ {printf "%-18s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

build: ## Compilo, pruebo y empaqueto el JAR ejecutable.
	$(GRADLE) build

test: ## Ejecuto pruebas unitarias y de integración sin depender de GCP.
	$(GRADLE) test

run: ## Inicio la API local sin credenciales ni servicios externos.
	$(GRADLE) bootRun

run-gcp: ## Inicio la API con Firestore y Pub/Sub usando ADC.
	SPRING_PROFILES_ACTIVE=gcp $(GRADLE) bootRun

run-analytics: ## Inicio la API GCP con el consumidor local Pub/Sub a BigQuery activo.
	SPRING_PROFILES_ACTIVE=gcp CHECKOUT_ANALYTICS_ENABLED=true $(GRADLE) bootRun

image: ## Construyo y publico la imagen OCI mediante Cloud Build.
	gcloud builds submit . --project=$(PROJECT_ID) --tag=$(IMAGE)

deploy: image ## Publico la imagen y despliego la API Cloud Run.
	$(MAKE) -C ../infra deploy-api PROJECT_ID=$(PROJECT_ID) API_IMAGE=$(IMAGE)

clean: ## Elimino sólo los artefactos generados por Gradle.
	$(GRADLE) clean
