# Demo de Defensa — Guion de 5 minutos (versión reducida)

> **Sistema correr antes**: `java -DMODE=DEV -jar target/MoronCorreo-1.0.jar` (o en SERVER).
> **Destinatario de todos los correos**: `grupo14sa@tecnoweb.org.bo`
> **Cuerpo**: vacío.
> **Tiempo por comando**: ~15 segundos (10s del ciclo POP3 + 5s ida y vuelta SMTP).

**Cobertura:** CU1, CU2, CU3, CU6, CU7 (cuotas + QR). **No se muestran** CU4 (Inventario), CU5 (Promociones) ni CU8 (Reportes) — si te preguntan, dices que "están implementados y se pueden demostrar bajo solicitud".

---

## ⚠️ Pre-flight (hacer 5 min antes de la defensa)

- [ ] JAR compilado: `mvn clean package` → `target/MoronCorreo-1.0.jar`
- [ ] Programa arrancado, ver `DB: OK` + `Mensajes en bandeja: 0`
- [ ] Gmail abierto en pestaña para enviar y otra para ver respuestas
- [ ] Bandeja de gmail **vacía de respuestas viejas**
- [ ] **VERIFICAR IDs actuales** enviando antes:
  - `LISPED["*"]` → mira el último ID
  - `LISVEN["*"]` → mira el último ID
- [ ] **Tener este documento ABIERTO** durante la defensa.

---

## 📋 IDs predichos para la defensa

> Asumiendo que **NADIE más** inserta antes de la demo.

| Recurso | Último ID en BD | Próximo (1er INSERT) | 2do INSERT |
|---|---|---|---|
| `pedido` | 12 | **13** | **14** |
| `venta` | 8 | **9** | **10** |
| `pago` | 9 | **10** | **11** |

> ⚠️ **Cada vez que corras el demo completo, los IDs suben +2** (porque se crean 2 pedidos, 2 ventas y 2 pagos). Si vuelves a probar antes de la defensa, ajusta otra vez con +2.

---

## 🎬 Guion del demo (4 actos, ~4 min)

### Acto 1 — Mostrar catálogo y usuarios (30s) — **CU1 + CU2**

Envía estos 2 correos seguidos:

```
LISUSR["*"]
LISPROD["*"]
```

**Narrativa al tribunal:**
> "Tenemos 7 usuarios con 3 roles (PROPIETARIO, VENDEDOR, CLIENTE) y 8 productos en catálogo. La cliente **Ana Flores (ID 4)** hará dos compras."

---

### Acto 2 — Pedido del cliente (1 min) — **CU3**

```
- INSPED["id_usuario","observaciones"] → INSERT pedido, responde con el ID generado

INSPED["4","Demo defensa"]
```
⏸ **Esperar respuesta. Confirmar que dice "Pedido creado con ID: 13"** (o el que sea).

```
- INSDET["id_pedido","id_producto","cantidad"] → INSERT detalle; aplica automáticamente la 
INSDET["13","7","2"]

LISPED["*"]

- MODPED["id","estado"] → UPDATE estado (valores: PENDIENTE/CONFIRMADO/ENTREGADO/CANCELADO)

MODPED["13","CONFIRMADO"]
```

**Narrativa:**
> "Ana solicita **2 cortinas blackout (ID 7) a 380 Bs c/u**. El sistema crea el pedido por **760 Bs**, lo muestra en la lista y lo pasa de PENDIENTE a CONFIRMADO. Cada cambio de estado se valida en BD."

---

### Acto 3 — Venta + Pago en cuotas con interés (1 min 30s) — **CU6 + CU7**

```
INSVEN["id_usuario","id_pedido","total"] → INSERT venta, responde con el ID generado
INSVEN["2","13","760"]
```
⏸ **Esperar respuesta. Confirmar "Venta creada con ID: 9"**.

```
- INSPAG["id_venta","tipo","total","cuotas"] → pago simple sin interés. Si el tipo es "QR", genera un QR interactuando con PagoFácil y envía la imagen adjunta `qr.png` al correo

INSPAG["9","CUOTAS","760","3","0.10"]

- LISPAG["id_venta"] → tabla del pago + tabla de cuotas

LISPAG["9"]
```

**Narrativa:**
> "La **vendedora Laura (ID 2)** registra la venta. El pago es a **3 cuotas con interés simple del 10%**.
> Fórmula: capital × tasa × n = 760 × 0.10 × 3 = **228 Bs de interés**.
> Total con interés = 988 Bs → **cuota mensual ≈ 329.33 Bs**.
> `LISPAG` muestra el pago y el plan de cuotas con sus fechas de vencimiento."

---

### Acto 4 — Pago QR REAL con PagoFácil (1 min 30s) ⭐ — **CU7 QR**

```
- INSPED["id_usuario","observaciones"] → INSERT pedido, responde con el ID generado

INSPED["4","Demo QR PagoFacil"]
```
⏸ **Esperar. Anotar ID (probable: 14).**

```
- INSDET["id_pedido","id_producto","cantidad"] → INSERT detalle; aplica automáticamente la 

INSDET["14","8","1"]
MODPED["14","CONFIRMADO"]

INSVEN["id_usuario","id_pedido","total"] → INSERT venta, responde con el ID generado

INSVEN["2","14","0.10"]
```
⏸ **Esperar. Anotar ID de venta (probable: 10).**

```
- INSPAG["id_venta","tipo","total","cuotas"] → pago simple sin interés. Si el tipo es "QR", genera un QR interactuando con PagoFácil y envía la imagen adjunta `qr.png` al correo


INSPAG["10","QR","0.10","1"]
```
⏸ **Llega correo con QR adjunto (qr.png).** Mostrar QR en pantalla, **escanear con celular**, **pagar 0.10 Bs reales en la app PagoFácil**.

```
VERPAG["10"]
```

**Narrativa:**
> "Para el segundo flujo, Ana paga con QR. El sistema genera un QR **conectándose en tiempo real a la API de PagoFácil** y lo envía como adjunto (qr.png). Al escanear y pagar, `VERPAG` consulta el estado en PagoFácil y actualiza la BD a COMPLETADO. Esto demuestra **integración con pasarela externa vía REST** + manejo de adjuntos en correo."

---

## 🆘 Plan B (si algo falla)

| Falla | Solución rápida |
|---|---|
| Comando no llega (no aparece en consola) | Espera 15s más. Si nada, re-enviarlo. |
| `Error POP3: Connection reset` | Es transitorio, espera el siguiente ciclo. |
| Olvidaste un ID | `LISPED["*"]` o `LISVEN["*"]` para verlo. |
| El QR falla por red | Cierras con: "este flujo requiere conexión externa con PagoFácil, ya lo demostramos antes con éxito". |
| Comando con error de formato | El sistema responde con `ERROR: Formato invalido`. **No es bug**, solo error de tipeo. |

---

## 📝 Comandos copy-paste (orden completo)

Solo para emergencias — copiar uno y pegar como **asunto** del correo:

```
LISUSR["*"]
LISPROD["*"]
INSPED["4","Demo defensa"]
INSDET["13","7","2"]
LISPED["*"]
MODPED["13","CONFIRMADO"]
INSVEN["2","13","760"]
INSPAG["9","CUOTAS","760","3","0.10"]
LISPAG["9"]
INSPED["4","Demo QR PagoFacil"]
INSDET["14","8","1"]
MODPED["14","CONFIRMADO"]
INSVEN["2","14","0.10"]
INSPAG["10","QR","0.10","1"]
VERPAG["10"]
```

---

## 🎯 CUs cubiertos en este flujo

| CU | Demostrado | Dónde |
|---|---|---|
| CU1 Usuarios | ✅ | Acto 1 (LISUSR) |
| CU2 Productos | ✅ | Acto 1 (LISPROD) |
| CU3 Pedidos | ✅ | Acto 2 |
| CU4 Inventario | ⚠️ No mostrado | "Implementado, comandos LISINV/INSINV/VALINV disponibles" |
| CU5 Promociones | ⚠️ No mostrado | "Implementado, comandos LISPRO/INSPRO/MODPRO/ELIPRO disponibles" |
| CU6 Ventas | ✅ | Acto 3 |
| CU7 Pagos | ✅✅ | Acto 3 (cuotas con interés) + Acto 4 (QR PagoFácil) |
| CU8 Reportes | ⚠️ No mostrado | "Implementado, comandos REPVEN/REPINV/REPPRO/REPCUO disponibles" |

**5 de 8 CU demostrados en vivo. Los 3 restantes están implementados y disponibles si los piden.**

---

## 💡 Si el tribunal pregunta por los CU no mostrados

**Respuesta corta y segura:**
> "Por tiempo elegimos el flujo comercial completo: catálogo → pedido → venta → pago. Los CUs de **Inventario, Promociones y Reportes** están implementados y los podemos demostrar ahora mismo si lo desean. Por ejemplo, `REPVEN["6","2026"]` da el reporte del mes."

**Si insisten, comandos rápidos de respaldo (cada uno ~15s):**
```
REPVEN["6","2026"]      ← Reporte de ventas del mes
REPINV["*"]             ← Stock y valor total del inventario
REPCUO["*"]             ← Cuotas pendientes (vigentes/vencidas)
VALINV["7"]             ← Valorización PEPS/UEPS/Promedio del producto 7
LISPRO["*"]             ← Lista de promociones
```
