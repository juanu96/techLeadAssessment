# Infraestructura local

El stack local incluye Kafka con Zookeeper, MongoDB y Redis. Los tópicos se crean automáticamente cuando el broker queda disponible.

## Iniciar los servicios

```bash
docker compose up -d
docker compose ps
```

Tópicos esperados:

- `orders-topic` con tres particiones
- `orders-dlt` con una partición

Para listar los tópicos:

```bash
docker compose exec kafka kafka-topics --bootstrap-server kafka:29092 --list
```

Para verificar MongoDB y Redis:

```bash
docker compose exec mongodb mongosh --quiet --eval "db.adminCommand('ping').ok"
docker compose exec redis redis-cli ping
```

## Detener los servicios

```bash
docker compose down
```

Los volúmenes conservan los datos entre ejecuciones. Usa `docker compose down -v` únicamente cuando necesites comenzar con datos limpios.
