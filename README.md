# B2B Order Processing

Sistema para procesar pedidos B2B publicados en Kafka. Cada pedido se enriquece con datos de productos y clientes, se calculan sus impuestos y se almacena en MongoDB.

## Servicios

- `order-worker`: worker reactivo en Java 21 y Spring WebFlux.
- `products-api`: catálogo de productos en Go.
- `clients-api`: catálogo de clientes en NestJS.
- Kafka, MongoDB y Redis como infraestructura local.

## Estado

El proyecto está en construcción. La primera etapa prepara el repositorio y define la configuración que compartirán los servicios.

## Requisitos locales

- Git
- Docker
- Docker Compose

Las instrucciones de ejecución se agregarán cuando exista el primer flujo funcional.
