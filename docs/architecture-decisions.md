# Decisiones técnicas

Estas notas dejan el razonamiento detrás de la solución, con el nivel de detalle suficiente para una revisión técnica.

## 1. Separar dominio, aplicación e infraestructura

El worker usa puertos para catálogo de productos, catálogo de clientes, caché, persistencia y DLT. Así las reglas de impuestos y enriquecimiento no dependen directamente de Kafka, Redis, MongoDB ni HTTP.

Esto permite probar la lógica principal sin levantar infraestructura y deja los adaptadores como piezas reemplazables.

## 2. Procesamiento reactivo de Kafka

El consumo se hizo con Reactor Kafka para mantener el flujo no bloqueante de punta a punta. La confirmación del offset queda dentro del pipeline:

- se confirma después de guardar el pedido enriquecido;
- se confirma después de publicar correctamente en DLT;
- no se confirma si también falla la publicación en DLT.

La idea es evitar pérdida silenciosa de mensajes.

## 3. Idempotencia en MongoDB

MongoDB guarda el documento con `orderId` como identificador y el servicio consulta si ya existe en estado `PROCESSED` antes de enriquecer.

Con eso, si se reenvía el mismo pedido, el worker confirma el mensaje y no duplica el resultado.

## 4. Caché cache-aside en Redis

Productos y clientes se consultan primero en Redis. Si no existen en caché, se consultan las APIs y luego se guardan con TTL.

Los TTL son independientes porque productos y clientes no tienen por qué cambiar con la misma frecuencia.

## 5. Retry, Circuit Breaker y errores 404

Las fallas transitorias de red o servidor pasan por retry y circuit breaker. Los `404` de productos o clientes se tratan como datos inexistentes, no como errores transitorios.

Eso evita reintentos innecesarios cuando el problema está en el contenido del pedido.

## 6. DLT con contexto suficiente

El DLT conserva payload original, key, tópico, partición, offset, fecha, causa e intentos agotados.

Ese contexto permite reproducir o analizar el fallo sin depender de logs locales.

## 7. Pruebas en dos niveles

Las pruebas rápidas corren en cada push y pull request:

- Go para Products API.
- Jest para Clients API.
- Maven verify con JaCoCo para Order Worker.

El E2E con Testcontainers queda manual porque levanta Kafka, MongoDB y Redis reales. Es valioso para cierre o revisión final, pero no hace falta ejecutarlo en cada cambio pequeño.

## 8. Docker Compose como entorno de revisión

`docker-compose.yml` levanta todo el stack con healthchecks y creación automática de tópicos. La intención es que alguien pueda revisar la solución sin instalar Go, Node ni Java localmente.
