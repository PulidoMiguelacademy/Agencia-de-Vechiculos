# Especificación de API REST - Agencia de Vehículos

Este documento define la arquitectura, contratos, endpoints, seguridad y especificación OpenAPI (Swagger) para el API REST de **Agencia de Vehículos**, diseñada para conectar el frontend (aplicación Android y web) de forma segura y estructurada.

---

## 1. Arquitectura y Reglas Generales

- **Protocolo:** HTTP / HTTPS sobre REST.
- **Formato de Datos:** JSON (`Content-Type: application/json`).
- **Autenticación:** JSON Web Tokens (JWT) Bearer Authentication.
- **Seguridad de Credenciales:** Bajo ninguna circunstancia el API devolverá campos de contraseña (`password`, `password_hash`, etc.) en las respuestas JSON.
- **Validación:** Todos los endpoints con entradas de datos (POST, PUT) validarán obligatoriamente la integridad y tipos de los campos requeridos, respondiendo con `400 Bad Request` en caso de fallos de validación.

---

## 2. Módulo de Autenticación (`/auth`)

### `POST /auth/login`
Autentica a un usuario del sistema y emite un token JWT de acceso.

- **Request Body:**
```json
{
  "email": "vendedor@agenciav.com",
  "password": "SecurePassword123"
}
```
- **Responses:**
  - `200 OK`:
    ```json
    {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "expires_in": 86400,
      "user": {
        "id": 1,
        "name": "Carlos Vendedor",
        "email": "vendedor@agenciav.com",
        "role": "VENDEDOR"
      }
    }
    ```
  - `400 Bad Request`: Datos incompletos o malformados.
  - `401 Unauthorized`: Credenciales inválidas.

---

## 3. Seguridad y Control de Acceso (Criterios 1 y 2)

Todos los endpoints (excepto `/auth/login`) requieren el encabezado HTTP de autorización:
```http
Authorization: Bearer <token_jwt>
```

- **Criterio 2 (Sin Token / Token Inválido):** Si la petición se realiza sin el header `Authorization`, con un token expirado o un token inválido, el servidor responderá inmediatamente con:
  - **Status Code:** `401 Unauthorized`
  - **Body:**
    ```json
    {
      "error": "Unauthorized",
      "message": "Token de autenticación ausente o inválido"
    }
    ```

---

## 4. Endpoints y Operaciones CRUD (Criterio 3)

### 4.1. Recursos de Vehículos (`/vehiculos`)
Permite gestionar el inventario de vehículos de la agencia (sincronizado con el modelo Android).

- **`GET /vehiculos`**
  - **Descripción:** Lista todos los vehículos disponibles en el inventario.
  - **Respuesta (`200 OK`):**
    ```json
    [
      {
        "id": 1,
        "brand": "Toyota",
        "model": "Corolla",
        "year": 2024,
        "price": 28500000,
        "condition": "NUEVO",
        "mileageKm": 0,
        "status": "DISPONIBLE",
        "fuel": "HIBRIDO",
        "vin": "1NXBR32E0FZ123456",
        "photoUrl": "https://example.com/images/corolla.jpg"
      }
    ]
    ```

- **`POST /vehiculos`**
  - **Descripción:** Registra un nuevo vehículo en el inventario.
  - **Request Body:** (Campos requeridos validados)
    ```json
    {
      "brand": "Mazda",
      "model": "CX-5",
      "year": 2025,
      "price": 42000000,
      "condition": "NUEVO",
      "mileageKm": 10,
      "status": "DISPONIBLE",
      "fuel": "GASOLINA",
      "vin": "JM3KFBDM5S0987654",
      "photoUrl": "https://example.com/images/cx5.jpg"
    }
    ```
  - **Respuesta (`201 Created`):** Retorna el objeto creado con su `id` asignado.

- **`PUT /vehiculos/{id}`**
  - **Descripción:** Actualiza los datos de un vehículo existente.
  - **Request Body:** Datos a actualizar.
  - **Respuesta (`200 OK`):** Objeto actualizado.

- **`DELETE /vehiculos/{id}`**
  - **Descripción:** Elimina o marca como inactivo un vehículo.
  - **Respuesta (`204 No Content`):** Eliminación exitosa.

---

### 4.2. Recursos Adicionales

- **Clientes (`/clientes`)**
  - `GET /clientes` (Listar clientes)
  - `POST /clientes` (Registrar cliente)
  - `PUT /clientes/{id}` (Actualizar cliente)
  - `DELETE /clientes/{id}` (Eliminar cliente)

- **Leads (`/leads`)**
  - `GET /leads` (Listar prospectos / leads)
  - `POST /leads` (Crear lead)
  - `PUT /leads/{id}` (Actualizar estado de lead)
  - `DELETE /leads/{id}` (Eliminar lead)

- **Cotizaciones (`/cotizaciones`)**
  - `GET /cotizaciones` (Listar cotizaciones)
  - `POST /cotizaciones` (Generar nueva cotización)
  - `PUT /cotizaciones/{id}` (Modificar cotización)
  - `DELETE /cotizaciones/{id}` (Anular/eliminar cotización)

- **Usuarios (`/usuarios`)**
  - `GET /usuarios` (Listar usuarios del sistema - *Excluye password*)
  - `POST /usuarios` (Crear usuario administrativo/vendedor)
  - `PUT /usuarios/{id}` (Actualizar usuario)
  - `DELETE /usuarios/{id}` (Desactivar usuario)

---

## 5. Documentación Interactiva (Swagger / OpenAPI 3.0)

El proyecto incluye Swagger UI integrado accesible en:
- `http://localhost:8080/swagger-ui/index.html`

### Fragmento de Definición OpenAPI (YAML)
```yaml
openapi: 3.0.3
info:
  title: API REST - Agencia de Vehículos
  version: 1.0.0
  description: API segura para gestión de inventario, clientes, leads, cotizaciones y autenticación.
paths:
  /auth/login:
    post:
      summary: Inicio de sesión
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                email:
                  type: string
                password:
                  type: string
      responses:
        '200':
          description: Login exitoso con token JWT
        '401':
          description: Credenciales inválidas
  /vehiculos:
    get:
      summary: Obtener lista de vehículos
      security:
        - BearerAuth: []
      responses:
        '200':
          description: Lista de vehículos
        '401':
          description: Token faltante o inválido
components:
  securitySchemes:
    BearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
```

---

## 6. Criterios de Aceptación Verificados

| Criterio | Estado | Detalle Técnico |
| :--- | :---: | :--- |
| **Criterio 1 (Login y Token)** | ✅ Cumplido | Endpoint `/auth/login` valida credenciales y retorna token JWT con tiempo de expiración. |
| **Criterio 2 (Protección 401)** | ✅ Cumplido | Middleware de autenticación intercepta peticiones sin header Bearer y retorna `401 Unauthorized`. |
| **Criterio 3 (CRUD Vehículos)** | ✅ Cumplido | Endpoints implementados para GET, POST, PUT y DELETE en `/vehiculos` con códigos HTTP correctos (`200`, `201`, `204`, `400`, `401`). |
| **Reglas (Validación y Seguridad)** | ✅ Cumplido | Validación de payloads y exclusión estricta de contraseñas en respuestas. |
