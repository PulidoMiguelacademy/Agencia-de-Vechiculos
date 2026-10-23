# Especificación de Bitácora de Auditoría (Audit Trail) - Agencia de Vehículos

Este documento define la estructura de datos, eventos rastreados, filtros y políticas de inmutabilidad para el registro de auditoría del sistema.

---

## 1. Estructura de Datos del Registro (`AuditLog`)

Cada evento importante registrado en el sistema genera una entrada inmutable con los siguientes campos:

```json
{
  "id": "log_9f8e7d6c5b4a3",
  "timestamp": "2026-10-21T15:30:00Z",
  "userId": 1,
  "userName": "Carlos Vendedor",
  "userRole": "ADMINISTRADOR",
  "module": "VEHICULOS",
  "action": "UPDATE_PRICE",
  "targetId": "veh_105",
  "previousValue": {
    "price": 28500000
  },
  "newValue": {
    "price": 29000000
  },
  "ipAddress": "192.168.1.50"
}
```

---

## 2. Eventos Importantes Registrados (Criterio 1)

El sistema genera automáticamente registros de auditoría ante las siguientes acciones críticas:
1. **Vehículos:** Cambio de precio (`UPDATE_PRICE`), cambio de estado (`UPDATE_STATUS`: DISPONIBLE, RESERVADO, VENDIDO).
2. **Cotizaciones y Contratos:** Aprobación, modificación o anulación.
3. **Usuarios y Permisos:** Modificación de roles, permisos de módulos, creación o desactivación de usuarios.
4. **Clientes y Leads:** Cambios en datos sensibles o reasignación de prospectos.

---

## 3. Endpoints y Filtrado de la Bitácora (Criterio 2)

### `GET /audit-logs`
Permite consultar los eventos registrados con capacidades de filtrado avanzado.

- **Parámetros de Consulta (Query Parameters):**
  - `userId`: Filtrar por ID de usuario específico.
  - `startDate`: Fecha de inicio (ISO 8601, ej. `2026-10-01T00:00:00Z`).
  - `endDate`: Fecha de fin (ISO 8601, ej. `2026-10-21T23:59:59Z`).
  - `module`: Filtrar por módulo (ej. `VEHICULOS`, `USUARIOS`, `COTIZACIONES`).
  - `action`: Filtrar por tipo de acción.

- **Ejemplo de Petición:**
  ```http
  GET /audit-logs?userId=1&startDate=2026-10-21T00:00:00Z&module=VEHICULOS
  Authorization: Bearer <token_admin>
  ```

- **Respuesta (`200 OK`):**
  ```json
  {
    "total": 1,
    "filters_applied": {
      "userId": 1,
      "startDate": "2026-10-21T00:00:00Z",
      "module": "VEHICULOS"
    },
    "data": [
      {
        "id": "log_9f8e7d6c5b4a3",
        "timestamp": "2026-10-21T15:30:00Z",
        "userId": 1,
        "userName": "Carlos Vendedor",
        "module": "VEHICULOS",
        "action": "UPDATE_PRICE",
        "previousValue": { "price": 28500000 },
        "newValue": { "price": 29000000 }
      }
    ]
  }
  ```

---

## 4. Reglas de Inmutabilidad y Seguridad

- **Acceso Restringido:** Solo los usuarios con rol `ADMINISTRADOR` tienen permiso para consultar la bitácora de auditoría.
- **Inmutabilidad Absoluta:**
  - Los registros se almacenan en una tabla/colección de base de datos de **solo escritura (Append-Only)**.
  - El API **no expone** métodos `PUT`, `PATCH` ni `DELETE` para el recurso `/audit-logs`.
  - Intentar modificar o eliminar un registro de auditoría arrojará un error `405 Method Not Allowed` o `403 Forbidden`.
