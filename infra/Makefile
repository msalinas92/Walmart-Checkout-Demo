SHELL := /bin/sh

TERRAFORM ?= terraform
PROJECT_ID ?= walmart-demo-511500
BACKEND_CONFIG ?= backend.hcl.example
BOOTSTRAP_DIR := bootstrap/state-bucket
API_IMAGE ?=
WEB_IMAGE ?=
export TF_VAR_project_id := $(PROJECT_ID)
export TF_VAR_api_image := $(API_IMAGE)
export TF_VAR_web_image := $(WEB_IMAGE)

.DEFAULT_GOAL := help

.PHONY: help fmt validate init plan apply deploy output api-url api-image web-url web-image deploy-api deploy-web bootstrap-create bootstrap-init bootstrap-plan bootstrap-apply

help: ## Muestro las tareas disponibles de infraestructura.
	@awk 'BEGIN {FS = ":.*##"}; /^[a-zA-Z0-9_-]+:.*##/ {printf "%-20s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

fmt: ## Formateo la configuración Terraform.
	$(TERRAFORM) fmt -recursive

validate: ## Valido la sintaxis y el grafo del stack principal.
	$(TERRAFORM) validate

init: ## Inicializo el backend GCS ya creado.
	$(TERRAFORM) init -input=false -backend-config=$(BACKEND_CONFIG)

plan: init validate ## Calculo cambios sin modificar recursos remotos.
	@test -n "$(API_IMAGE)" || (echo "Defina API_IMAGE para conservar la revisión Cloud Run"; exit 2)
	@test -n "$(WEB_IMAGE)" || (echo "Defina WEB_IMAGE para conservar la revisión Cloud Run"; exit 2)
	$(TERRAFORM) plan -input=false -var="api_image=$(API_IMAGE)" -var="web_image=$(WEB_IMAGE)"

apply: init validate ## Aplico el stack principal de Firestore, Pub/Sub y BigQuery.
	@test -n "$(API_IMAGE)" || (echo "Defina API_IMAGE para conservar la revisión Cloud Run"; exit 2)
	@test -n "$(WEB_IMAGE)" || (echo "Defina WEB_IMAGE para conservar la revisión Cloud Run"; exit 2)
	$(TERRAFORM) apply -input=false -auto-approve -var="api_image=$(API_IMAGE)" -var="web_image=$(WEB_IMAGE)"

deploy: apply ## Alias explícito para aplicar infraestructura en GCP.

deploy-api: init validate ## Despliego API conservando explícitamente la revisión web.
	@test -n "$(API_IMAGE)" || (echo "Defina API_IMAGE=<imagen OCI>"; exit 2)
	@test -n "$(WEB_IMAGE)" || (echo "Defina WEB_IMAGE=<imagen OCI actual>"; exit 2)
	$(TERRAFORM) apply -input=false -auto-approve -var="api_image=$(API_IMAGE)" -var="web_image=$(WEB_IMAGE)"

deploy-web: init validate ## Despliego la revisión web indicada por WEB_IMAGE y conserva la API actual.
	@test -n "$(API_IMAGE)" || (echo "Defina API_IMAGE=<imagen OCI actual>"; exit 2)
	@test -n "$(WEB_IMAGE)" || (echo "Defina WEB_IMAGE=<imagen OCI>"; exit 2)
	$(TERRAFORM) apply -input=false -auto-approve -var="api_image=$(API_IMAGE)" -var="web_image=$(WEB_IMAGE)"

output: init ## Muestro los identificadores de recursos creados.
	$(TERRAFORM) output

api-url: ## Imprimo sólo la URL de la API desplegada.
	@$(TERRAFORM) output -raw api_url

api-image: ## Imprimo sólo la imagen actualmente desplegada en la API.
	@$(TERRAFORM) output -raw api_image

web-url: ## Imprimo sólo la URL del frontend desplegado.
	@$(TERRAFORM) output -raw web_url

web-image: ## Imprimo sólo la imagen actualmente desplegada en el frontend.
	@$(TERRAFORM) output -raw web_image

bootstrap-create: ## Creo el bucket de estado y migro su propio estado (sólo primera ejecución).
	$(TERRAFORM) -chdir=$(BOOTSTRAP_DIR) init -input=false -backend=false
	$(TERRAFORM) -chdir=$(BOOTSTRAP_DIR) apply -input=false -auto-approve
	$(TERRAFORM) -chdir=$(BOOTSTRAP_DIR) init -input=false -migrate-state -force-copy -backend-config=backend.hcl.example

bootstrap-init: ## Inicializo el backend remoto del módulo de bootstrap ya existente.
	$(TERRAFORM) -chdir=$(BOOTSTRAP_DIR) init -input=false -backend-config=backend.hcl.example

bootstrap-plan: bootstrap-init ## Muestro cambios pendientes del bucket de estado.
	$(TERRAFORM) -chdir=$(BOOTSTRAP_DIR) plan -input=false

bootstrap-apply: bootstrap-init ## Aplico cambios al bucket de estado ya existente.
	$(TERRAFORM) -chdir=$(BOOTSTRAP_DIR) apply -input=false -auto-approve
