# Contratos de integración

Este documento resume los contratos que usa el flujo local. No reemplaza las pruebas, pero sirve para revisar rápido qué espera y qué produce cada pieza.

## Products API

### `GET /health`

Respuesta esperada:

```json
{
  "status": "UP"
}
```

### `GET /products/{productId}`

Ejemplo:

```powershell
Invoke-RestMethod http://localhost:8081/products/PRD-001
```

Respuesta `200`:

```json
{
  "productId": "PRD-001",
  "name": "Gaseosa 600ml",
  "sku": "GAS-600-PET",
  "category": "Bebidas",
  "taxCategory": "GRAVADO",
  "unitOfMeasure": "UNIDAD"
}
```

Si el producto no existe, responde `404`:

```json
{
  "error": "product not found"
}
```

## Clients API

### `GET /health`

Respuesta esperada:

```json
{
  "status": "UP"
}
```

### `GET /clients/{clientId}`

Ejemplo:

```powershell
Invoke-RestMethod http://localhost:8082/clients/CLI-99821
```

Respuesta `200`:

```json
{
  "clientId": "CLI-99821",
  "name": "Comercializadora Andina",
  "segment": "MAYORISTA",
  "taxRegime": "RESPONSABLE_IVA",
  "region": "Bogotá",
  "channel": "B2B"
}
```

Si el cliente no existe, responde `404`:

```json
{
  "message": "client not found",
  "error": "Not Found",
  "statusCode": 404
}
```

## Pedido de entrada

El worker consume pedidos desde `orders-topic`.

Ejemplo:

```json
{
  "orderId": "ORD-2024-COL-00147",
  "clientId": "CLI-99821",
  "channel": "B2B",
  "createdAt": "2024-09-12T10:45:00Z",
  "items": [
    {
      "productId": "PRD-001",
      "quantity": 24,
      "unitPrice": 3500.00
    },
    {
      "productId": "PRD-008",
      "quantity": 12,
      "unitPrice": 8200.00
    }
  ]
}
```

Validaciones principales:

- `orderId` y `clientId` son obligatorios.
- `items` no puede venir vacío.
- cada item necesita `productId`, `quantity` y `unitPrice`.
- `quantity` debe ser mayor que cero.
- `unitPrice` no puede ser negativo.

## Pedido enriquecido

El resultado se guarda en MongoDB, colección `enriched-orders`, usando `orderId` como identificador del documento.

Campos principales:

| Campo | Descripción |
| --- | --- |
| `orderId` | Identificador del pedido procesado |
| `status` | Estado final; actualmente `PROCESSED` |
| `client` | Datos del cliente consultado en Clients API |
| `items` | Líneas calculadas con producto, cantidad, precio, subtotal e impuesto |
| `summary` | Totales del pedido |
| `processedAt` | Fecha de procesamiento generada por el worker |

Para el pedido de ejemplo:

```json
{
  "summary": {
    "subtotal": 182400.00,
    "totalTax": 20880.00,
    "grandTotal": 203280.00,
    "currency": "COP"
  }
}
```

## Dead Letter Topic

Si el procesamiento agota los intentos configurados, el mensaje original se publica en `orders-dlt`.

Campos principales:

| Campo | Descripción |
| --- | --- |
| `originalTopic` | Tópico de entrada |
| `originalPartition` | Partición del mensaje original |
| `originalOffset` | Offset del mensaje original |
| `originalKey` | Key recibida desde Kafka |
| `originalPayload` | Payload original sin modificar |
| `failedAt` | Fecha del fallo |
| `cause` | Error que explica por qué se envió a DLT |
| `attempts` | Intentos agotados antes de publicar en DLT |

El offset de entrada solo se confirma después de guardar el pedido o después de publicar correctamente el mensaje en DLT.
