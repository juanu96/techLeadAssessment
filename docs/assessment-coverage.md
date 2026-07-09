# Cobertura de la prueba

Resumen corto de lo que cubre la entrega y cómo validarlo.

## Funcionalidad implementada

| Área | Estado | Dónde revisarlo |
| --- | --- | --- |
| API de productos | Implementado | `products-api` |
| API de clientes | Implementado | `clients-api` |
| Worker de pedidos | Implementado | `order-worker` |
| Consumo desde Kafka | Implementado | `KafkaOrderConsumer` |
| Enriquecimiento con productos y clientes | Implementado | `OrderEnrichmentService` |
| Cálculo de impuestos | Implementado | `TaxCalculator` |
| Persistencia en MongoDB | Implementado | `MongoEnrichedOrderAdapter` |
| Idempotencia por `orderId` | Implementado | `OrderProcessingService` |
| Caché en Redis | Implementado | `RedisProductCacheAdapter`, `RedisClientCacheAdapter` |
| Retry y Circuit Breaker | Implementado | `WorkerConfiguration`, adaptadores HTTP |
| Dead Letter Topic | Implementado | `KafkaDeadLetterPublisher` |
| Docker Compose | Implementado | `docker-compose.yml` |
| CI | Implementado | `.github/workflows` |

## Validación rápida para revisión

```powershell
Copy-Item .env.example .env
docker compose up --build -d
docker compose ps
```

Luego:

```powershell
.\scripts\publish-order.ps1
docker compose exec mongodb mongosh orders --quiet --eval 'db.getCollection("enriched-orders").find().pretty()'
```

Para pruebas por módulo:

```powershell
.\scripts\verify-local.ps1
```

Para el E2E completo del worker:

```powershell
cd order-worker
.\mvnw.cmd -Pe2e verify
```

## Puntos de revisión recomendados

- Reenviar el mismo `orderId` y confirmar que no se duplica en MongoDB.
- Cambiar un `productId` por uno inexistente y revisar publicación en `orders-dlt`.
- Apagar temporalmente una API de catálogo y ver retry/circuit breaker en logs.
- Revisar que los totales del pedido de ejemplo sean:
  - subtotal `182400.00`
  - impuesto total `20880.00`
  - total `203280.00 COP`

## Límites asumidos

- Los catálogos de productos y clientes son estáticos para mantener la prueba enfocada en integración y procesamiento.
- No se agregó autenticación entre servicios porque la prueba se ejecuta en entorno local controlado.
- Las métricas de negocio quedan fuera del alcance; el worker expone healthcheck y logs estructurados.
