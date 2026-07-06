# B2B Order Processing

Sistema para procesar pedidos B2B publicados en Kafka. Cada pedido se enriquece con datos de productos y clientes, se calculan sus impuestos y se almacena en MongoDB.

## Servicios

- `order-worker`: worker reactivo en Java 21 y Spring WebFlux.
- `products-api`: catálogo de productos en Go.
- `clients-api`: catálogo de clientes en NestJS.
- Kafka, MongoDB y Redis como infraestructura local.

## Estado

El proyecto está en construcción. Actualmente incluye las APIs de productos y clientes con catálogos en memoria y respuestas JSON.

## Requisitos locales

- Git
- Docker
- Docker Compose

## Products API

La API requiere la variable `PRODUCTS_API_PORT` y expone:

- `GET /products/{productId}`
- `GET /health`

Para ejecutar las pruebas sin instalar Go en el host:

```bash
docker build -t products-api ./products-api
docker run --rm -e PRODUCTS_API_PORT=8081 -p 8081:8081 products-api
```

El Dockerfile ejecuta la suite de pruebas antes de compilar el binario.

Una vez iniciado, se puede consultar un producto con:

```bash
curl http://localhost:8081/products/PRD-001
```

## Clients API

La API requiere la variable `CLIENTS_API_PORT` y expone:

- `GET /clients/{clientId}`
- `GET /health`

Para construir y ejecutar el servicio:

```bash
docker build -t clients-api ./clients-api
docker run --rm -e CLIENTS_API_PORT=8082 -p 8082:8082 clients-api
```

El Dockerfile ejecuta las pruebas unitarias y end-to-end antes de compilar la aplicación.

Una vez iniciado, se puede consultar un cliente con:

```bash
curl http://localhost:8082/clients/CLI-99821
```
