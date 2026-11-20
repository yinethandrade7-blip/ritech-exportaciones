# RiTech S.A.S — Sistema de gestión de exportaciones de café y cacao

Aplicación web para administrar la exportación de café y cacao colombiano hacia la
Unión Europea, con trazabilidad del origen según el **Reglamento (UE) 2023/1115 (EUDR)**.

Desde diciembre de 2024 la Unión Europea no permite importar café ni cacao sin poder
demostrar la finca exacta de donde salió y que ese terreno no fue deforestado. Ese es el
problema que resuelve este sistema: cada lote queda amarrado a una finca con coordenadas,
y el comprador europeo puede consultar ese origen antes de aceptar la compra.

Proyecto académico de **Ingeniería de Requisitos** — Universidad Cooperativa de Colombia,
Campus Pasto.

---

## Equipo

| Integrante | Código |
|---|---|
| Alison Mendieta Sterling | 958478 |
| Yineth Sofía Valencia Andrade | 953577 |
| Nicolás Esteban Erazo Pérez | 958242 |
| Juan José Rodríguez Mora | 888676 |

Docente: Ing. Aida Liliana Rosero Ruiz

---

## Tecnologías

| Capa | Herramienta |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.1.2 |
| Vistas | Thymeleaf |
| Acceso a datos | JdbcTemplate (SQL a la vista) |
| Base de datos | MySQL 8 |
| Servidor | Tomcat embebido, puerto 8080 |
| Construcción | Maven (incluye `mvnw`, no hay que instalar Maven) |
| Mapas | Leaflet + OpenStreetMap |

### Arquitectura en tres capas

```
controlador/   recibe lo que pide el navegador y decide qué mostrar
servicio/      reglas que no son ni pantalla ni base de datos (tasa de cambio)
dao/           todo el SQL, una clase por tabla
modelo/        los datos que viajan entre capas
templates/     las pantallas (HTML con Thymeleaf)
```

Se usó `JdbcTemplate` en lugar de JPA a propósito: así el SQL queda escrito y se puede
leer, que es lo que pide el requisito no funcional de mantenibilidad (RNF-013).

---

## Cómo ejecutarlo

### 1. Requisitos previos

- Java 21 o superior (`java -version`)
- MySQL 8 corriendo en `localhost:3306`

### 2. Crear la base de datos

En MySQL Workbench, ejecutar el archivo `ritech_exportaciones.sql`. Eso crea el esquema
y los datos de ejemplo (fincas de Nariño, proveedores, clientes europeos).

### 3. Configurar la conexión

En `src/main/resources/application.properties` van el usuario y la contraseña de MySQL.
Son credenciales de desarrollo local: solo sirven contra una base de datos en el mismo
computador, no hay nada publicado en internet.

### 4. Arrancar

En la carpeta del proyecto (la que tiene `pom.xml`):

```
.\mvnw.cmd spring-boot:run
```

En Mac o Linux:

```
./mvnw spring-boot:run
```

Cuando aparezca `Started ExportacionesApplication`, abrir el navegador en:

```
http://localhost:8080
```

No hay que crear tablas ni usuarios a mano. Al arrancar, la clase `DatosIniciales`
revisa lo que falte —el rol de gerente, las columnas nuevas— y lo agrega sola. Si ya
existe, no toca nada.

---

## Cuentas de prueba

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | `admin@ritech.com` | `admin123` |
| Gerente | `gerente@ritech.com` | `gerente2026` |
| Auxiliar | `auxiliar@ritech.com` | `1234` |

Los proveedores y clientes entran con el correo registrado en su ficha.

---

## Quién puede hacer qué

El sistema no tiene un usuario que lo pueda todo. Cada rol ve únicamente lo que su
trabajo necesita, siguiendo el **principio de mínimo privilegio**.

| | Administrador | Gerente | Auxiliar | Proveedor | Cliente |
|---|---|---|---|---|---|
| Crear y bloquear usuarios | Sí | | | | |
| Editar proveedores y clientes | Sí | Ver | Ver | | |
| Registrar lotes y pedidos | | Ver | Sí | | |
| Autorizar el despacho | | Sí | | | |
| Ver reportes del negocio | | Sí | | | |
| Autorizar pagos al productor | | Sí | | | |
| Ver sus propios lotes y su pago | | | | Sí | |
| Aceptar o rechazar la cotización | | | | | Sí |
| Consultar el origen del lote | | Sí | Sí | | Sí |

El administrador maneja las cuentas, pero **no ve ventas ni movimiento del negocio**.
El gerente ve el negocio, pero **no puede crear usuarios**. Están separados a propósito:
si una cuenta se ve comprometida, el daño queda limitado a lo que esa cuenta podía hacer.

El permiso se pregunta en código por capacidad, no por nombre de rol:

```java
if (!usuario.puedeDespachar()) {
    return "redirect:/menu";
}
```

---

## Ciclo de vida de un pedido

```
BORRADOR  →  COTIZADO  →  CREADO  →  DESPACHADO
                 │
                 └──→  RECHAZADO
```

| Estado | Qué significa |
|---|---|
| `BORRADOR` | El auxiliar todavía está armando el pedido. El cliente no lo ve. |
| `COTIZADO` | Se le envió el precio al cliente. Los lotes quedan reservados. |
| `CREADO` | El cliente aceptó el precio. Ahora sí se puede despachar. |
| `RECHAZADO` | El cliente no aceptó. Los lotes vuelven a quedar disponibles. |
| `DESPACHADO` | El gerente autorizó la salida de la mercancía. |

La separación entre `BORRADOR` y `CREADO` corrige un error de modelado detectado en
pruebas: un mismo estado significaba dos cosas distintas (pedido en construcción y
pedido aprobado por el cliente), y eso permitía despachar sin que el cliente hubiera
aceptado.

---

## Conexiones externas

| Para qué | De dónde |
|---|---|
| Pesos por dólar | TRM oficial — Superintendencia Financiera de Colombia (`datos.gov.co`) |
| Pesos por euro | Banco Central Europeo (`frankfurter.app`), combinado con la TRM |
| Mapa de las fincas | OpenStreetMap |

La TRM es la tasa con la que legalmente se liquida una exportación en Colombia, no una
cotización cualquiera. Se consulta una vez al día y se guarda en memoria.

Si no hay internet el sistema **no se cae**: avisa y deja escribir la tasa a mano.

---

## Estado del proyecto

**Sprint 4 — en curso**

- [x] Separación del rol de gerente (principio de mínimo privilegio)
- [x] Rediseño completo de la interfaz
- [x] Conexión a la API de tasa de cambio
- [x] El cliente acepta o rechaza la cotización, con motivo
- [x] El proveedor consulta cuánto le van a pagar
- [x] Publicación del código en repositorio
- [ ] Notificaciones por correo
