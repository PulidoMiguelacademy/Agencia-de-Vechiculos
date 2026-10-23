# Especificación de Control de Acceso Basado en Roles (RBAC) - Agencia de Vehículos

Este documento define la matriz de permisos por rol y módulo, las reglas de validación de seguridad y el mecanismo de bloqueo para acciones no autorizadas.

---

## 1. Definición de Roles del Sistema

1. **ADMINISTRADOR**: Acceso total a todos los módulos y operaciones del sistema. Gestión de usuarios y roles.
2. **VENDEDOR**: Gestión de catálogo de vehículos, clientes, leads y creación de cotizaciones.
3. **ASESOR_SERVICIO**: Gestión de citas de taller, órdenes de servicio y entregas.
4. **GERENTE_VENTAS**: Supervisión y aprobación de cotizaciones y contratos de venta.

---

## 2. Matriz de Permisos por Rol y Módulo

Cada celda indica si el rol tiene permiso (`Sí` / `No`) para realizar una acción específica (`Ver`, `Crear`, `Editar`, `Aprobar`, `Eliminar`) en cada módulo.

| Módulo / Recurso | Acción | Administrador | Vendedor | Asesor de Servicio | Gerente de Ventas |
| :--- | :--- | :---: | :---: | :---: | :---: |
| **Vehículos** | Ver | Sí | Sí | Sí | Sí |
| | Crear | Sí | No | No | No |
| | Editar | Sí | No | No | No |
| | Eliminar | Sí | No | No | No |
| **Clientes** | Ver | Sí | Sí | Sí | Sí |
| | Crear | Sí | Sí | Sí | No |
| | Editar | Sí | Sí | No | No |
| | Eliminar | Sí | No | No | No |
| **Leads** | Ver | Sí | Sí | No | Sí |
| | Crear | Sí | Sí | No | No |
| | Editar | Sí | Sí | No | Sí |
| | Eliminar | Sí | No | No | No |
| **Cotizaciones** | Ver | Sí | Sí | No | Sí |
| | Crear | Sí | Sí | No | Sí |
| | Editar | Sí | Sí | No | Sí |
| | Aprobar | Sí | No | No | Sí |
| **Taller / Servicio** | Ver | Sí | No | Sí | No |
| | Crear | Sí | No | Sí | No |
| | Editar | Sí | No | Sí | No |
| **Usuarios / Roles** | Ver | Sí | No | No | No |
| | Crear | Sí | No | No | No |
| | Editar | Sí | No | No | No |
| | Eliminar | Sí | No | No | No |

---

## 3. Criterios de Aceptación y Mecanismos de Bloqueo

### Criterio 1: Herencia de Permisos por Rol
- Los permisos se configuran a nivel de **Rol**.
- Cualquier cambio en la matriz de un rol aplica de manera inmediata a **todos los usuarios** asignados a dicho rol sin necesidad de reconfigurar cuenta por cuenta.

### Criterio 2: Bloqueo de Acciones No Autorizadas
- Cuando un usuario intenta realizar una acción para la cual su rol no tiene permiso (ej. un Vendedor intentando eliminar un vehículo o un Asesor intentando aprobar una cotización):
  - **Código HTTP:** `403 Forbidden`
  - **Respuesta JSON:**
    ```json
    {
      "error": "Forbidden",
      "code": "INSUFFICIENT_PERMISSIONS",
      "message": "Acción denegada: Su rol actual no cuenta con permisos para 'Eliminar' en el módulo 'Vehículos'."
    }
    ```
  - **En el Frontend (Android/Web):** El sistema interceptará el código 403 y mostrará un componente de retroalimentación (`FeedbackCard` / Toast / Dialog) informando: *"No tienes permisos para realizar esta acción."*

---

## 4. Reglas Críticas del Sistema

- **Regla de Administrador Único / Activo:**
  - El sistema validará en todo momento que exista al menos **un (1) Administrador activo**.
  - Si un administrador intenta desactivar, cambiar de rol o eliminar al último administrador activo del sistema, la petición será rechazada con `400 Bad Request` y el mensaje: *"No se puede eliminar o degradar al último Administrador activo del sistema."*
