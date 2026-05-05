# InfoTienda

InfoTienda es un backend para una tienda e-commerce desarrollado con Spring Boot. Expone una API REST para gestionar catalogo, usuarios, carrito, checkout, ordenes y pagos con Mercado Pago Checkout Pro.

El proyecto esta pensado como base para una tienda online: permite administrar productos y categorias, manejar carritos de compra, crear ordenes desde el checkout y confirmar pagos mediante notificaciones de Mercado Pago.

## Stack

- Java 21
- Spring Boot 3.5
- Spring Web
- Spring Security
- Spring Data JPA
- Spring Validation
- Spring OAuth2 Client
- JWT con `jjwt`
- MySQL
- AWS S3
- Mercado Pago SDK Java
- Lombok
- Maven
- Docker Compose
- Spring Boot Actuator

## Funcionalidades

- Registro, login, logout y refresh token.
- Autenticacion con JWT y cookies.
- Login OAuth2 con Google.
- Roles `USER` y `ADMIN`.
- Proteccion de endpoints administrativos.
- CRUD de categorias.
- CRUD de productos con imagenes en AWS S3.
- Listado publico de productos con filtros, paginacion y ordenamiento.
- Carrito para usuarios autenticados.
- Carrito para visitantes mediante sesion invitada.
- Merge del carrito invitado al iniciar sesion.
- Validacion de stock y productos activos.
- Checkout con envio a domicilio o retiro en sucursal.
- Creacion de ordenes en estado `PENDING`.
- Integracion con Mercado Pago Checkout Pro.
- Webhook para actualizar ordenes pagadas.
- Estados de orden: `PENDING`, `PAID`, `SHIPPED`, `DELIVERED`, `CANCELLED`.

## Rutas principales

### Autenticacion

- `GET /api/v1/auth/csrf`
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `GET /api/v1/auth/me`

### Productos

- `GET /api/v1/products`
- `GET /api/v1/products/{id}`
- `POST /api/v1/products`
- `PUT /api/v1/products/{id}`
- `DELETE /api/v1/products/{id}`

### Categorias

- `GET /api/v1/categories`
- `GET /api/v1/categories/{id}`
- `POST /api/v1/categories`
- `PUT /api/v1/categories/{id}`
- `DELETE /api/v1/categories/{id}`

### Carrito

- `GET /api/v1/cart`
- `POST /api/v1/cart/items`
- `PUT /api/v1/cart/items/{cartItemId}`
- `DELETE /api/v1/cart/items/{cartItemId}`
- `DELETE /api/v1/cart`

### Checkout y pagos

- `POST /api/v1/checkout`
- `POST /api/v1/payments/mercado-pago/webhook`

## Flujo de compra

1. El usuario agrega productos al carrito.
2. Inicia el checkout.
3. Selecciona envio a domicilio o retiro en sucursal.
4. Selecciona Mercado Pago como metodo de pago.
5. El backend crea una orden en estado `PENDING`.
6. Se genera una preferencia de Mercado Pago Checkout Pro.
7. El frontend redirige al usuario al checkout de Mercado Pago.
8. Mercado Pago notifica el resultado al webhook.
9. Si el pago es aprobado, la orden pasa a `PAID`.
