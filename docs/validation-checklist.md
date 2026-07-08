# Validation checklist

Esta guía deja los comandos que conviene correr antes de entregar o antes de pedir revisión. La idea es poder demostrar rápido que cada pieza funciona por separado y que el flujo completo también queda cubierto.

## Validación local rápida

Desde la raíz del repositorio:

```powershell
.\scripts\verify-local.ps1
```

Ese comando ejecuta:

- pruebas de `products-api`
- pruebas, build y e2e de `clients-api`
- `verify` del `order-worker`, incluyendo JaCoCo

Si se quiere incluir el flujo con Kafka, MongoDB y Redis reales por Testcontainers:

```powershell
.\scripts\verify-local.ps1 -RunWorkerE2E
```

Para ese segundo comando Docker Desktop debe estar encendido.

## Validación con Docker Compose

Levantar todo el stack:

```powershell
docker compose up --build -d
docker compose ps
```

Comprobar salud de los servicios:

```powershell
Invoke-RestMethod http://localhost:8081/health
Invoke-RestMethod http://localhost:8082/health
Invoke-RestMethod http://localhost:8080/actuator/health
```

Publicar el pedido de ejemplo:

```powershell
.\scripts\publish-order.ps1
```

Revisar el resultado enriquecido:

```powershell
docker compose exec mongodb mongosh orders --quiet --eval 'db.getCollection("enriched-orders").find().pretty()'
```

Si Kafka o ZooKeeper quedan con datos de pruebas anteriores, lo más seguro es detener primero:

```powershell
docker compose down
```

Usa `docker compose down -v` solo si estás cómodo eliminando pedidos, caché y datos locales de Kafka.

## Validación en GitHub

El workflow `.github/workflows/ci.yml` corre automáticamente en push y pull request:

- `Products API`: `go test ./...`
- `Clients API`: `npm ci`, pruebas unitarias, build y e2e
- `Order Worker`: `./mvnw -B verify`

El job `Order Worker E2E` queda como ejecución manual desde GitHub Actions porque usa Testcontainers y tarda más.
