# Guía de Documentación PUDS — MoronCorreo (Grupo 14)

Sistema: **Morón Diseño y Decoraciones de Interiores**
Proyecto Java/Maven que opera por correo electrónico (POP3/SMTP) sobre PostgreSQL, con integración a la pasarela **PagoFácil** para pagos QR.

---

## 0. Identificación de Actores y Casos de Uso

### Actores
| Actor | Tipo | Descripción |
|---|---|---|
| **Propietario** | Primario | Administra usuarios, productos, ve reportes, gestiona promociones |
| **Vendedor** | Primario | Registra pedidos, ventas, pagos e inventario |
| **Cliente** | Primario | Consulta productos, solicita pedidos, paga (QR/contado/cuotas) |
| **PagoFácil** | Secundario (sistema externo) | Pasarela de pagos QR — recibe peticiones HTTP y devuelve estado |
| **Servidor de Correo (Dovecot/SMTP)** | Secundario | Canal de entrada/salida de todas las operaciones |

### Casos de Uso (8)
- **CU1** Gestión de Usuarios
- **CU2** Gestión de Productos
- **CU3** Gestión de Pedidos
- **CU4** Gestión de Inventario (PEPS/UEPS/Promedio)
- **CU5** Gestión de Promociones
- **CU6** Gestión de Ventas
- **CU7** Gestión de Pagos (Contado / Cuotas con interés / QR PagoFácil)
- **CU8** Reportes

---

## 1. Diagrama General de Casos de Uso (1)

**Qué incluir:**
- Frontera del sistema rotulada `MoronCorreo`.
- Los 3 actores primarios a la izquierda (Propietario, Vendedor, Cliente).
- PagoFácil como actor secundario a la derecha (asociado solo a CU7).
- Los 8 elipses (CU1..CU8) dentro de la frontera.
- Relaciones `<<include>>` y `<<extend>>` cuando correspondan:
  - CU6 (Ventas) `<<include>>` CU3 (Pedidos) — una venta parte de un pedido.
  - CU7 (Pagos) `<<include>>` CU6 (Ventas).
  - CU3 (Pedidos) `<<extend>>` CU5 (Promociones) — aplica promo activa si existe.
  - CU6 (Ventas) `<<include>>` CU4 (Inventario) — descuenta stock.

**Convención de jerarquía:** si un mismo actor humano tiene varios roles, mostrar generalización (`Vendedor` ── `Propietario` si el Propietario hereda permisos del Vendedor).

---

## 2. Diagramas Individuales Actor → CU (8)

Uno por cada CU. Para cada uno mostrar:
- El/los actor(es) involucrados.
- El CU principal.
- CUs auxiliares vinculados con `<<include>>` / `<<extend>>`.
- Comandos asociados (subject del correo) como nota UML adjunta.

**Mapa Actor ↔ CU (referencia):**
| CU | Propietario | Vendedor | Cliente | PagoFácil |
|---|---|---|---|---|
| CU1 Usuarios | ✓ | | | |
| CU2 Productos | ✓ | ✓ (consulta) | ✓ (consulta) | |
| CU3 Pedidos | | ✓ | ✓ | |
| CU4 Inventario | ✓ | ✓ | | |
| CU5 Promociones | ✓ | | | |
| CU6 Ventas | ✓ | ✓ | | |
| CU7 Pagos | | ✓ | ✓ | ✓ |
| CU8 Reportes | ✓ | | | |

---

## 3. Paquetes Identificados (1 diagrama + descripción)

**Paquetes implementados** (ya aplicados en `src/main/java/moroncorreo/`):

### Paquetes de capa arquitectónica

| Paquete | Descripción | Contiene |
|---|---|---|
| `moroncorreo.app` | Capa de presentación/entrada — orquesta correo y despacha comandos | `Main`, `CorreoService`, `ProcesadorComandos` |
| `moroncorreo.negocio` | Capa de dominio (raíz) — utilidades compartidas | `Respuesta`, `TablaFormato` |
| `moroncorreo.infra` | Capa de infraestructura — acceso a BD, correo y APIs externas | `ConexionDB`, `ConexionCorreo`, `PagoFacilConfig`, `PagoFacilClient` |

### Sub-paquetes funcionales del dominio (`moroncorreo.negocio.*`)

| Sub-paquete | Responsabilidad de negocio | Contiene |
|---|---|---|
| `negocio.comercial` | **Gestión Comercial** — qué se ofrece, cómo se vende y bajo qué condiciones | `ProductoHandler`, `PromocionHandler`, `PedidoHandler`, `VentaHandler` |
| `negocio.operacion` | **Gestión de Operación** — movimientos físicos de stock y cobranza | `InventarioHandler`, `PagoHandler` |
| `negocio.administracion` | **Gestión de Administración** — cuentas del sistema y vista gerencial | `UsuarioHandler`, `ReporteHandler` |

### Diagrama de paquetes (dependencias)

```
              ┌──> negocio.comercial      ──┐
              │                             │
moroncorreo.app ──> negocio.operacion       ├──> negocio (Respuesta, TablaFormato)
              │                             │            │
              └──> negocio.administracion ──┘            ↓
                                                      infra
              └──> infra (CorreoService → ConexionCorreo)
```

**Dependencias unidireccionales:**
- `app` → los 3 sub-paquetes de `negocio` (despacha cada comando al handler correspondiente)
- `app` → `infra` (CorreoService usa ConexionCorreo)
- Los 3 sub-paquetes → `negocio` raíz (importan `Respuesta` y `TablaFormato`)
- Los 3 sub-paquetes → `infra` (importan `ConexionDB`; `operacion.PagoHandler` además importa `PagoFacilClient`)
- **Nada apunta hacia arriba**, no hay ciclos.

### Paquetes relacionados con sus CU (trazabilidad)

| CU | Sub-paquete | Clase responsable |
|---|---|---|
| CU1 Usuarios | `negocio.administracion` | `UsuarioHandler` |
| CU2 Productos | `negocio.comercial` | `ProductoHandler` |
| CU3 Pedidos | `negocio.comercial` | `PedidoHandler` |
| CU4 Inventario | `negocio.operacion` | `InventarioHandler` |
| CU5 Promociones | `negocio.comercial` | `PromocionHandler` |
| CU6 Ventas | `negocio.comercial` | `VentaHandler` |
| CU7 Pagos | `negocio.operacion` | `PagoHandler` (+ `infra.PagoFacilClient`) |
| CU8 Reportes | `negocio.administracion` | `ReporteHandler` |

**Justificación del agrupamiento:**
- **Comercial (4 CU)** encapsula el flujo *cliente → venta*: catálogo → promociones → solicitud → cierre.
- **Operación (2 CU)** encapsula los movimientos físicos y financieros que respaldan la venta: stock y cobranza.
- **Administración (2 CU)** encapsula lo transversal: control de cuentas y reportería gerencial.

---

## 4. Diagramas de Comunicación (8 — uno por CU)

**Convenciones UML 2.x:**
- Objetos como rectángulos: `:NombreClase`.
- Mensajes numerados secuencialmente (`1:`, `1.1:`, `1.2:` para anidados).
- Flechas direccionales sobre los enlaces.
- Mostrar el flujo: `Actor → CorreoService → ProcesadorComandos → XxxHandler → ConexionDB → CorreoService → Actor`.

**Patrón base para todos los CU:**
```
1: enviarCorreo(subject)        Actor → :CorreoService
2: procesar(subject)             :CorreoService → :ProcesadorComandos
3: ejecutar(params)              :ProcesadorComandos → :XxxHandler
4: query/update(sql)             :XxxHandler → :ConexionDB
5: Respuesta                     :XxxHandler → :ProcesadorComandos
6: responder(remitente,Respuesta) :CorreoService → Actor
```

**Casos especiales:**
- **CU7 (QR):** agregar `:PagoFacilClient` entre `PagoHandler` y un actor externo `PagoFácil`. Mensajes: `login()`, `generarQR()`, `descargarImagen()`.
- **CU3 con promoción:** `PedidoHandler` consulta `PromocionHandler` (o tabla `promocion`) antes de calcular subtotal.
- **CU6:** `VentaHandler` invoca `InventarioHandler` para registrar SALIDA.

---

## 5. Diagramas de Secuencia (8 — uno por CU)

Mismo flujo que comunicación pero con **líneas de vida vertical** y enfoque temporal.

**Elementos obligatorios:**
- Actor a la izquierda.
- Líneas de vida con barras de activación.
- Mensajes síncronos (flecha sólida) y retorno (flecha punteada).
- Fragmentos combinados (`alt`, `opt`, `loop`) cuando aplique.

**Casos que necesitan fragmentos:**
- **CU3:** `opt [existe promoción activa]` para aplicar descuento.
- **CU4:** `alt [tecnica = PEPS | UEPS | PROMEDIO]` para cálculo de valorización.
- **CU7 QR:** `loop` en `verificarQR` mientras estado = PENDIENTE; `alt [pagado / no pagado]`.
- **CU7 Cuotas:** `loop` para generar N cuotas.
- **Todos:** `alt [comando válido / inválido]` al inicio en `ProcesadorComandos`.

---

## 6. Diagrama de Despliegue (1)

**Nodos:**
- **Nodo `PC Cliente`** — cualquier cliente de correo (Gmail, Thunderbird).
- **Nodo `Servidor TecnoWeb` (Linux)** — contiene:
  - Componente `Dovecot (POP3:110)`
  - Componente `Postfix/Sendmail (SMTP:25)`
  - Componente `MoronCorreo-1.0-shaded.jar`
  - Componente `JDK 21.0.3+9`
- **Nodo `Servidor PostgreSQL`** — `db_grupo14sa` puerto 5432.
- **Nodo externo `PagoFácil API`** — servicio HTTPS.

**Conexiones (con protocolo rotulado):**
- PC Cliente ──SMTP/POP3──> Servidor TecnoWeb
- MoronCorreo.jar ──JDBC──> PostgreSQL
- MoronCorreo.jar ──HTTPS/REST──> PagoFácil API

**Notas adicionales:** indicar la ruta `/home/grupo14sa/MoronCorreo-1.0-shaded.jar` como artefacto desplegado.

---

## 7. Diagrama de Capas (Arquitectura) (1)

**Estilo 4 capas (recomendado):**

| Capa | Responsabilidad | Componentes |
|---|---|---|
| **Presentación** | Recepción/envío de correos | `CorreoService`, `TablaFormato` |
| **Aplicación** | Despacho de comandos | `Main`, `ProcesadorComandos`, `Respuesta` |
| **Dominio (Negocio)** | Reglas de los CU | `UsuarioHandler` … `ReporteHandler` |
| **Persistencia / Integración** | Acceso a BD y servicios externos | `ConexionDB`, `ConexionCorreo`, `PagoFacilClient`, `PagoFacilConfig` |

Reglas de dependencia: **solo capas superiores conocen las inferiores**. Documentar con flechas hacia abajo.

---

## 8. Especificación de Casos de Uso (formato sugerido)

Aunque dices que ya están hechas, verifica que cada CU contenga:
1. **Nombre y código** (CU1 - Gestión de Usuarios)
2. **Actores**
3. **Propósito**
4. **Resumen / breve descripción**
5. **Precondiciones**
6. **Postcondiciones**
7. **Flujo básico (camino feliz)** — numerado paso a paso
8. **Flujos alternativos** (ej: comando mal formado, ID inexistente)
9. **Excepciones**
10. **Comandos asociados** (subject) con ejemplo
11. **Reglas de negocio** (ej: en CU7 — interés simple, cuota inicial es cuota 0)

---

## 9. Diagrama de Componentes (1)

Vista física de las unidades desplegables y sus interfaces.

**Componentes a mostrar:**
- `<<artifact>> MoronCorreo-1.0-shaded.jar` (componente raíz)
  - Sub-componente `app` (Main, CorreoService, ProcesadorComandos)
  - Sub-componente `negocio` (Handlers + Respuesta + TablaFormato)
  - Sub-componente `infra` (ConexionDB, ConexionCorreo, PagoFacil*)

**Interfaces requeridas (`<<requires>>`)**:
- Driver JDBC PostgreSQL `42.7.3`
- Jakarta Mail `2.0.1` (POP3, SMTP)
- Gson `2.11.0`
- JDK 21

**Interfaces externas (`<<uses>>`)**:
- BD `db_grupo14sa` (puerto 5432)
- Servidor POP3 Dovecot (puerto 110)
- Servidor SMTP (puerto 25)
- API REST PagoFácil (`https://masterqr.pagofacil.com.bo/api/services/v2`)

**Convención de dibujo:** rectángulos con `<<component>>` o ícono UML, líneas con lollipop (○─) para interfaces provistas y socket (─⊃) para interfaces requeridas.

---

## 10. Diagrama de Estados (mínimo 3)

Modela el ciclo de vida de los objetos con transiciones explícitas en la BD.

### 10.1 Estado de `Pedido` (CU3)
```
[*] ──crear──> PENDIENTE ──MODPED[CONFIRMADO]──> CONFIRMADO ──MODPED[ENTREGADO]──> ENTREGADO ──> [*]
                  │                                   │
                  └──MODPED[CANCELADO]──> CANCELADO ──┘──> [*]
```
**Transiciones:** disparadas por el comando `MODPED["id","estado"]`. Cada transición incluye una **guarda** (estado actual válido) y una **acción** (UPDATE en `pedido.estado`).

### 10.2 Estado de `PagoCuota` (CU7)
```
[*] ──INSPAG──> PENDIENTE ──MODPAG["id_cuota","true"]──> PAGADA ──> [*]
                    │
                    └──[fecha > vencimiento]──> VENCIDA (estado derivado, no persiste)
```
**Nota:** `VENCIDA` no es un valor en la columna `pagado` (boolean) sino un estado calculado en el reporte `REPCUO`. Mostrarlo como un estado derivado con guarda.

### 10.3 Estado de `PagoQR` (CU7)
```
[*] ──INSPAG[QR]──> GENERADO ──VERPAG──> CONSULTANDO ──[respuesta PagoFácil]──> COMPLETADO ──> [*]
                                              │
                                              └──[aún pendiente]──> GENERADO (retorna)
```
**Disparadores:** `INSPAG[..."QR"]` genera el QR; `VERPAG["id_venta"]` consulta el estado en PagoFácil.

---

## 11. Diagrama de Actividades (mínimo 1, recomendado 2)

### 11.1 Flujo principal del sistema (procesamiento de correo)
Documenta el bucle `Main → CorreoService → ProcesadorComandos → Handler → respuesta SMTP`:
```
[Inicio]
  → conectar POP3
  → leer mensajes (ordenados por fecha)
  → <decisión: ¿hay mensajes?>
        SI → para cada mensaje:
              → extraer subject + remitente
              → <decisión: ¿remitente automático?> SI → ignorar
              → procesar(subject) → Respuesta
              → enviar respuesta SMTP
              → marcar mensaje DELETE
        NO → cerrar conexión
  → esperar 10s (Thread.sleep)
  → repetir
```

### 11.2 Flujo de pago QR con PagoFácil (CU7)
Flujo asíncrono distribuido entre dos comandos (`INSPAG[QR]` y `VERPAG`):
```
[INSPAG QR recibido]
  → validar parámetros
  → login PagoFácil (HTTP)
  → <decisión: ¿token válido?> NO → relogin
  → generarQR (HTTP + monto + detalle)
  → guardar pago_qr (transactionId, payment_number, checkout_url)
  → descargar imagen QR
  → adjuntar qr.png en correo respuesta
  → enviar al cliente
[...espera del cliente...]
[VERPAG recibido]
  → consultar transacción en PagoFácil
  → <decisión: ¿estado = PAGADO?>
        SI → actualizar pago como COMPLETADO → notificar
        NO → responder "aún pendiente"
```

**Convención UML:** usar swimlanes (carriles) separando `Cliente`, `MoronCorreo`, `PagoFácil`, `BD`.

---

## 12. Diagrama de Clases (ya hecho — solo lista de verificación)

Cuando lo revises, asegúrate de que incluye:
- Una clase por tabla (entidad): `Usuario`, `Producto`, `Pedido`, `PedidoDetalle`, `Venta`, `VentaDetalle`, `InventarioMovimiento`, `Promocion`, `Pago`, `PagoCuota`, `PagoQR`.
- Las clases handler (`UsuarioHandler`, etc.) o equivalentes con sus métodos públicos.
- Relaciones de multiplicidad: `Pedido 1..* PedidoDetalle`, `Venta 1..* VentaDetalle`, `Pago 1..* PagoCuota`, `Pago 0..1 PagoQR`.
- Enumeraciones: `RolUsuario`, `EstadoPedido`, `TipoMovimiento`, `TecnicaInventario`, `TipoPago`.

---

## ▶ MAPEO AL ÍNDICE DEL DOCUMENTO PUDS

Si tu documento sigue el índice 7.1 / 7.2 / 7.3, así se distribuyen los diagramas:

| Sección del PUDS | Contenido a incluir |
|---|---|
| **7.1.1 Identificar Actores** | Tabla de actores (sección 0 de esta guía): Propietario, Vendedor, Cliente, PagoFácil, Servidor de Correo |
| **7.1.2 Identificar Casos de Uso** | Lista de los 8 CU + breve propósito |
| **7.1.3 Detallar Casos de Uso** | Especificación completa (ya hecha por ustedes) + **8 diagramas individuales actor→CU** (sección 2) |
| **7.1.4 Diagrama General de Casos de Uso** | **1 diagrama general** con 3 actores primarios + PagoFácil + 8 elipses + `<<include>>`/`<<extend>>` (sección 1) |
| **7.1.5 Comandos por Caso de Uso** | Tabla de comandos (LISUSR, INSUSR, etc.) ya existente en `guia.md` |
| **7.2.1 Análisis de la Arquitectura** | Texto descriptivo: estilo en capas, justificación de paquetes |
| **7.2.2 Diagrama de Arquitectura** → Identificar Paquetes | **Diagrama de paquetes** (sección 3) con los 3 paquetes `app/negocio/infra` |
| **7.2.2 Diagrama de Arquitectura** → Relacionar Paquetes con CU | Tabla CU → clase (sección 3, segunda tabla) |
| **7.2.3 Estructura del Proyecto** | Árbol de carpetas `src/main/java/moroncorreo/{app,negocio,infra}/` |
| **7.2.4 Comandos Detallados** | Especificación por comando (parámetros, validaciones, respuesta) |
| **7.2.5 Análisis de Casos de Uso** → Diagramas de Comunicación | **8 diagramas de comunicación** (sección 4) |
| **7.3.1 Diseño de la Arquitectura** | **Diagrama de capas** (sección 7) + **Diagrama de Componentes** (sección 9 ← NUEVO) |
| **7.3.2 Diseño de Datos** | Diagrama ER + script SQL + mapeo OR (ya hechos) |
| **7.3.3 Diseño de Casos de Uso** → Diagramas de Secuencia | **8 diagramas de secuencia** (sección 5) |
| **7.3.3 Diseño de Casos de Uso** → Diagramas de Estados ← NUEVO | **3 diagramas de estados** (sección 10): `Pedido`, `PagoCuota`, `PagoQR` |
| **7.3.3 Diseño de Casos de Uso** → Diagramas de Actividades ← NUEVO | **2 diagramas de actividades** (sección 11): flujo principal y flujo QR |
| **7.4 Implementación** (agregar si no existe) | **Diagrama de Despliegue** (sección 6) + diagrama de clases de diseño |

**Recomendación:** los diagramas de Estados y Actividades pueden ir como sub-secciones nuevas dentro de 7.3.3 (por ejemplo 7.3.3.2 y 7.3.3.3) o, si el docente lo permite, abrir un capítulo 7.4 "Modelos Complementarios". El diagrama de Componentes encaja natural en 7.3.1 porque es una vista arquitectónica.

---

## 13. Convenciones de Documentación

- **Herramienta sugerida:** StarUML, Astah, Visual Paradigm o draw.io. Mantener formato fuente (.uml/.drawio) además del PNG exportado.
- **Nomenclatura de archivos:** `CU1_secuencia.png`, `CU1_comunicacion.png`, `general_CU.png`, `paquetes.png`, `despliegue.png`, `capas.png`.
- **Carpeta sugerida:**
  ```
  doc/
    PUDS.pdf                    ← documento final compilado
    diagramas/
      casos_uso/
      comunicacion/
      secuencia/
      clases/
      estados/
      paquetes.png
      despliegue.png
      capas.png
    especificacion_CU/
  ```
- **Cabecera obligatoria en cada diagrama:** título, autor (grupo 14), versión, fecha.
- **Trazabilidad:** cada diagrama debe referenciar el código del CU (CU1..CU8) que documenta.

---

## 14. Orden Sugerido de Elaboración

1. Refinar especificación de CU (verificar flujos alternativos).
2. Diagrama general de CU + 8 individuales.
3. Diagrama de Clases (porque alimenta a secuencia y comunicación).
4. Diagramas de Secuencia (8) — uno por CU.
5. Diagramas de Comunicación (8) — derivar de los de secuencia.
6. Diagramas de Estados (Pedido, PagoCuota, PagoQR).
7. Diagrama de Paquetes + descripción.
8. Diagrama de Componentes.
9. Diagrama de Capas.
10. Diagrama de Despliegue (último porque depende de componentes).

---

## 15. Checklist Final del PUDS

- [ ] Portada con datos del grupo y materia
- [ ] Índice
- [ ] Visión / Introducción
- [ ] Modelo del Negocio (opcional, según docente)
- [ ] Especificación de 8 CU
- [ ] Diagrama general de CU
- [ ] 8 diagramas individuales actor→CU
- [ ] Diagrama de paquetes + descripción + relación con CU
- [ ] Diagrama de clases
- [ ] 8 diagramas de secuencia
- [ ] 8 diagramas de comunicación
- [ ] Diagramas de estados (Pedido, PagoCuota, PagoQR)
- [ ] Diagramas de actividades (flujo principal + flujo QR PagoFácil)
- [ ] Diagrama de componentes
- [ ] Diagrama de capas
- [ ] Diagrama de despliegue
- [ ] Diagrama de BD (ER) — ya hecho ✓
- [ ] Script SQL — ya hecho ✓
- [ ] Mapeo objeto-relacional — ya hecho ✓
- [ ] Conclusiones
- [ ] Anexos (manual de usuario por comandos)

---

## 16. Cómo Correrlo Localmente (modo DEV)

### Requisitos previos

| Requisito | Verificación |
|---|---|
| **Java 21** instalado | `java -version` |
| **Maven** instalado | `mvn -version` |
| **Acceso a red** a `www.tecnoweb.org.bo` (BD) y `mail.tecnoweb.org.bo` (POP3/SMTP) | `Test-NetConnection mail.tecnoweb.org.bo -Port 110` |
| **IP autorizada** en el firewall de TecnoWeb | Si la IP no está habilitada, el admin debe agregarla. Las IPs cambian al cambiar de red. |

### Pasos para correr

Desde la raíz del proyecto, en PowerShell:

**1. Compilar el JAR (incluye dependencias shaded)**
```powershell
mvn clean package
```
Esto genera **`target/MoronCorreo-1.0.jar`** (~2.4 MB).

**2. Ejecutar en modo DEV**
```powershell
java -DMODE=DEV -jar target/MoronCorreo-1.0.jar
```

Salida esperada al arrancar:
```
MoronCorreo arrancando...
MODE: DEV
DB: OK (jdbc:postgresql://www.tecnoweb.org.bo:5432/db_grupo14sa)
Iniciando ciclo de correo...
Mensajes en bandeja: 0
```
A partir de ahí lee el buzón **cada 10 segundos**.

**3. Detener**: `Ctrl + C` en la terminal.

### Cómo probarlo

1. Desde cualquier cuenta de correo (Gmail, Outlook, otra @tecnoweb.org.bo), envía un correo a:
   **Para:** `grupo14sa@tecnoweb.org.bo`
   **Asunto:** un comando, por ejemplo:
   - `LISUSR["*"]` → lista usuarios
   - `LISPROD["*"]` → lista productos
   - `INSPROD["Sillon Lima","3 plazas","2500","10","SALA"]`
   - `REPINV["*"]` → reporte de stock
   **Cuerpo:** puede ir vacío.

2. Esperar ~10 segundos. El programa imprime en consola:
   ```
   Procesando: tucorreo@gmail.com | LISUSR["*"]
   -> EXITO | Respondido a: tucorreo@gmail.com
   ```

3. Revisa el correo de respuesta en tu bandeja.

### Modos DEV vs SERVER

| Setting | DEV (tu PC) | SERVER (servidor facu) |
|---|---|---|
| Host BD | `www.tecnoweb.org.bo:5432` | `localhost:5432` |
| Host correo | `mail.tecnoweb.org.bo` | `localhost` |
| Puerto POP3 / SMTP | 110 / 25 | 110 / 25 |
| Auth SMTP | false (relay sin auth) | false (entrega local) |

El cambio entre modos se hace solo con la flag `-DMODE=DEV` o `-DMODE=SERVER` en la línea de comandos.

### Comandos resumidos (copy-paste)

```powershell
# Compilar
mvn clean package

# Correr en modo DEV
java -DMODE=DEV -jar target/MoronCorreo-1.0.jar

# Compilar y correr en una sola línea
mvn clean package ; if ($?) { java -DMODE=DEV -jar target/MoronCorreo-1.0.jar }
```

### Despliegue en el servidor (modo SERVER)

1. `mvn clean package` localmente.
2. Subir `target/MoronCorreo-1.0.jar` por FileZilla a `/home/grupo14sa/`.
3. Conectar por SSH a `grupo14sa@www.tecnoweb.org.bo`.
4. Ejecutar:
   ```bash
   ~/.local/jdk-21.0.3+9/bin/java -DMODE=SERVER -jar ~/MoronCorreo-1.0.jar
   ```
   O usar el script `run.sh` ya incluido en el repo.

### Problemas comunes

| Síntoma | Causa probable | Solución |
|---|---|---|
| `DB FALLO: Connection timed out` | Tu IP no está autorizada en el firewall PostgreSQL de TecnoWeb | Pedir al admin que habilite tu IP pública actual |
| `Error POP3: Connection reset` (intermitente) | El servidor cerró mal la sesión POP3 anterior | Esperar unos segundos; se recupera solo |
| `[ERROR procesando mensaje]: Invalid Addresses` | SMTP rechazó el envío (relay externo restringido) | Verificar que la IP del cliente esté autorizada para relay |
| `Couldn't connect to host... mail.tecnoweb.org.bo` | Red corporativa/VPN bloqueando puertos de correo | Cambiar de red o conectarse a la VPN de la facultad |
| Comandos llegan con caracteres raros (`""` curvas) | El cliente de correo (Outlook, móvil) reemplaza comillas | Usar Gmail web que respeta las comillas rectas |
