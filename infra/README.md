# Infraestructura Terraform

Este módulo aprovisiona la infraestructura mínima de la demo en
`walmart-demo-511500`:

- Bucket GCS privado y versionado para el estado Terraform (módulo bootstrap).
- APIs de Firestore, Pub/Sub y BigQuery.
- Base default de Firestore Native.
- Tópico `checkout.transaction.v1`, suscripción pull
  `checkout-analytics-local` y suscripción administrada
  `checkout-analytics-bigquery`.
- Dataset `retail_analytics`, tabla cruda particionada y vista deduplicada por
  `eventId`.
- Artifact Registry, una cuenta de servicio de mínimo privilegio y, al recibir
  imágenes, servicios Cloud Run públicos para API y frontend.

No crea VPC ni balanceador. La ruta analítica desplegada es Pub/Sub → BigQuery
administrada y no necesita un proceso consumidor. Cloud Build publica las
imágenes sólo al ejecutar los objetivos de despliegue; Cloud Run usa escala a
cero y no mantiene instancias mínimas.

## Requisitos

- Terraform 1.8+.
- Google Cloud CLI autenticado con permisos para habilitar APIs y administrar
  Firestore, Pub/Sub y BigQuery en `walmart-demo-511500`.

```bash
gcloud auth application-default login
gcloud config set project walmart-demo-511500
```

## Inicializar estado remoto

El backend GCS no puede crear su propio bucket. Por eso el primer `apply` del
submódulo `bootstrap/state-bucket` usa estado local; inmediatamente después se
migra también ese estado al bucket. El stack principal usa el prefijo
`retail-checkout/demo`.

```bash
cd infra/bootstrap/state-bucket
terraform init -backend=false
terraform apply
terraform init \
  -backend-config=backend.hcl.example \
  -migrate-state

cd ../..
terraform init \
  -backend-config=backend.hcl.example \
  -migrate-state
```

Si cambias el nombre del bucket, modifica o copia `backend.hcl.example` y usa
el mismo archivo en el comando `-backend-config`. El bucket usa acceso uniforme,
prevención de acceso público y versionado. Su contenido es el estado sensible
de Terraform: no lo borres ni lo hagas público.

## Ejecutar stack principal

En el proyecto actualmente desplegado, use los objetivos de la raíz. Éstos
recuperan las dos imágenes Cloud Run vigentes y evitan que variables vacías
retiren accidentalmente un servicio:

```bash
make infra-plan PROJECT_ID=walmart-demo-511500
make deploy-infra PROJECT_ID=walmart-demo-511500
```

Terraform guarda el estado del stack principal en el bucket GCS. Los estados
locales y archivos de plan están ignorados y no deben versionarse.

## Automatización con Make

Desde la raíz del repositorio puedes ejecutar `make infra-plan` o
`make deploy-infra`. Para publicar los contenedores y desplegar la aplicación,
ejecuta `make deploy-app IMAGE_TAG=demo-001` desde la raíz. Dentro de esta
carpeta, `make help` muestra los destinos de Terraform disponibles.
`bootstrap-create` está reservado al primer uso de un proyecto sin bucket de
estado; para el bucket existente usa `bootstrap-plan` o `bootstrap-apply`.

## Nota sobre Firestore

Una base default de Firestore sólo se crea una vez por proyecto y su región es
inmutable. Si ya existe una base, impórtala antes de aplicar:

```bash
terraform import google_firestore_database.default "projects/walmart-demo-511500/databases/(default)"
```

El módulo usa `DELETE_PROTECTION_ENABLED` y `deletion_policy = "ABANDON"` para
evitar que un `terraform destroy` elimine accidentalmente la base operacional.
