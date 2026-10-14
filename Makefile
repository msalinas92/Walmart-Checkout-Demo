SHELL := /bin/sh

PROJECT_ID ?= walmart-demo-511500
REGION ?= southamerica-west1
REPOSITORY ?= retail-checkout
export GOOGLE_CLOUD_PROJECT := $(PROJECT_ID)

.DEFAULT_GOAL := help


.PHONY: help build test verify deploy deploy-infra deploy-api deploy-web deploy-app infra-bootstrap infra-plan \
	backend-build backend-test backend-run backend-run-gcp backend-run-analytics \
	frontend-install frontend-build frontend-check frontend-run frontend-start

help: ## Muestro los comandos disponibles del monorepo.
	@awk 'BEGIN {FS = ":.*##"}; /^[a-zA-Z0-9_-]+:.*##/ {printf "%-24s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

build: backend-build frontend-build ## Construyo los artefactos de backend y frontend.

test: backend-test frontend-check ## Ejecuto pruebas backend y chequeo estático del frontend.

verify: build ## Ejecuto la verificación completa de compilación.

deploy: deploy-infra build ## Aprovisiono GCP y genero los artefactos locales de la demo.
	@echo "Infraestructura aplicada. La aplicación permanece local: use make backend-run y make frontend-run."

deploy-infra: ## Aplico la infraestructura Terraform en GCP.
	@api_image="$$($(MAKE) -s -C infra api-image PROJECT_ID=$(PROJECT_ID))"; \
	 web_image="$$($(MAKE) -s -C infra web-image PROJECT_ID=$(PROJECT_ID))"; \
	 test -n "$$api_image" && test -n "$$web_image" || (echo "No hay revisiones actuales para conservar"; exit 2); \
	 $(MAKE) -C infra deploy PROJECT_ID=$(PROJECT_ID) API_IMAGE="$$api_image" WEB_IMAGE="$$web_image"

deploy-api: ## Publico y despliego el backend Java en Cloud Run.
	@tag="$$(date -u +%Y%m%d%H%M%S)"; \
	 api_image="$(REGION)-docker.pkg.dev/$(PROJECT_ID)/$(REPOSITORY)/checkout-api:$$tag"; \
	 web_image="$$($(MAKE) -s -C infra web-image PROJECT_ID=$(PROJECT_ID))"; \
	 test -n "$$web_image" || (echo "No existe una imagen web que conservar"; exit 2); \
	 $(MAKE) -C backend image PROJECT_ID=$(PROJECT_ID) IMAGE_TAG="$$tag"; \
	 $(MAKE) -C infra deploy-api PROJECT_ID=$(PROJECT_ID) API_IMAGE="$$api_image" WEB_IMAGE="$$web_image"

deploy-web: ## Publico y despliego el frontend Next.js usando la API ya desplegada.
	@api_image="$$($(MAKE) -s -C infra api-image PROJECT_ID=$(PROJECT_ID))"; \
	 api_url="$$($(MAKE) -s -C infra api-url PROJECT_ID=$(PROJECT_ID))"; \
	 test -n "$$api_image" && test -n "$$api_url" || (echo "Despliegue primero la API: make deploy-api"; exit 2); \
	 $(MAKE) -C frontend deploy PROJECT_ID=$(PROJECT_ID) API_IMAGE="$$api_image" API_BASE_URL="$$api_url/api/v1"

deploy-app: ## Despliego API y frontend Cloud Run de forma ordenada.
	@tag="$$(date -u +%Y%m%d%H%M%S)"; \
	 api_image="$(REGION)-docker.pkg.dev/$(PROJECT_ID)/$(REPOSITORY)/checkout-api:$$tag"; \
	 web_image="$(REGION)-docker.pkg.dev/$(PROJECT_ID)/$(REPOSITORY)/checkout-web:$$tag"; \
	 api_url="$$($(MAKE) -s -C infra api-url PROJECT_ID=$(PROJECT_ID))"; \
	 test -n "$$api_url" || (echo "La API debe existir antes del despliegue conjunto"; exit 2); \
	 $(MAKE) -C backend image PROJECT_ID=$(PROJECT_ID) IMAGE_TAG="$$tag"; \
	 $(MAKE) -C frontend image PROJECT_ID=$(PROJECT_ID) IMAGE_TAG="$$tag" API_BASE_URL="$$api_url/api/v1"; \
	 $(MAKE) -C infra deploy-web PROJECT_ID=$(PROJECT_ID) API_IMAGE="$$api_image" WEB_IMAGE="$$web_image"

infra-bootstrap: ## Creo y migro el backend remoto GCS (sólo la primera vez).
	$(MAKE) -C infra bootstrap-create PROJECT_ID=$(PROJECT_ID)

infra-plan: ## Muestro los cambios Terraform pendientes sin aplicarlos.
	@api_image="$$($(MAKE) -s -C infra api-image PROJECT_ID=$(PROJECT_ID))"; \
	 web_image="$$($(MAKE) -s -C infra web-image PROJECT_ID=$(PROJECT_ID))"; \
	 $(MAKE) -C infra plan PROJECT_ID=$(PROJECT_ID) API_IMAGE="$$api_image" WEB_IMAGE="$$web_image"

backend-build: ## Compilo el backend Java con Gradle.
	$(MAKE) -C backend build

backend-test: ## Ejecuto pruebas unitarias e integración del backend.
	$(MAKE) -C backend test

backend-run: ## Inicio Spring Boot local, sin credenciales ni servicios GCP.
	$(MAKE) -C backend run

backend-run-gcp: ## Inicio Spring Boot contra los servicios GCP configurados.
	$(MAKE) -C backend run-gcp PROJECT_ID=$(PROJECT_ID)

backend-run-analytics: ## Inicio el perfil GCP y el consumidor local Pub/Sub a BigQuery.
	$(MAKE) -C backend run-analytics PROJECT_ID=$(PROJECT_ID)

frontend-install: ## Instalo dependencias reproducibles del frontend.
	$(MAKE) -C frontend install

frontend-build: ## Compilo el frontend Next.js.
	$(MAKE) -C frontend build

frontend-check: ## Valido los tipos TypeScript del frontend.
	$(MAKE) -C frontend check

frontend-run: ## Inicio Next.js en desarrollo.
	$(MAKE) -C frontend dev

frontend-start: ## Sirvo el frontend compilado.
	$(MAKE) -C frontend start
