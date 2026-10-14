SHELL := /bin/sh

NPM ?= npm
PROJECT_ID ?= walmart-demo-511500
REGION ?= southamerica-west1
REPOSITORY ?= retail-checkout
IMAGE_TAG ?= $(shell date -u +%Y%m%d%H%M%S)
API_IMAGE ?=
API_BASE_URL ?=
IMAGE := $(REGION)-docker.pkg.dev/$(PROJECT_ID)/$(REPOSITORY)/checkout-web:$(IMAGE_TAG)

.DEFAULT_GOAL := help

.PHONY: help install build check dev start image deploy clean

help: ## Muestro las tareas disponibles del frontend.
	@awk 'BEGIN {FS = ":.*##"}; /^[a-zA-Z0-9_-]+:.*##/ {printf "%-18s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

install: ## Instalo exactamente las dependencias declaradas en package-lock.json.
	$(NPM) ci

build: install ## Genero la compilación de producción de Next.js.
	$(NPM) run build

check: install ## Verifico tipos TypeScript sin levantar el navegador.
	./node_modules/.bin/tsc --noEmit

dev: install ## Inicio Next.js en modo desarrollo.
	$(NPM) run dev

start: build ## Sirvo la compilación de producción en localhost.
	$(NPM) run start

image: ## Construyo y publico la imagen OCI con la URL pública de la API.
	@test -n "$(API_BASE_URL)" || (echo "Defina API_BASE_URL=<url de API>/api/v1"; exit 2)
	gcloud builds submit . --project=$(PROJECT_ID) --config=cloudbuild.yaml --substitutions=_IMAGE=$(IMAGE),_NEXT_PUBLIC_API_BASE_URL=$(API_BASE_URL)

deploy: image ## Publico la imagen y despliego el frontend Cloud Run.
	@test -n "$(API_IMAGE)" || (echo "Defina API_IMAGE=<imagen API actual>"; exit 2)
	$(MAKE) -C ../infra deploy-web PROJECT_ID=$(PROJECT_ID) API_IMAGE=$(API_IMAGE) WEB_IMAGE=$(IMAGE)

clean: ## Elimino sólo artefactos generados por Next.js.
	rm -rf .next
