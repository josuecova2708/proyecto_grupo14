package moroncorreo.negocio.administracion;

import moroncorreo.negocio.Respuesta;

public class HelpHandler {

    public static Respuesta ayuda(String[] params) {
        StringBuilder sb = new StringBuilder();

        sb.append("╔══════════════════════════════════════════════════════════════╗\n");
        sb.append("║       MORÓN DISEÑO DE INTERIORES — Referencia de Comandos    ║\n");
        sb.append("╚══════════════════════════════════════════════════════════════╝\n");
        sb.append("Envíe cada comando como ASUNTO del correo a:\n");
        sb.append("  grupo14sa@tecnoweb.org.bo\n");
        sb.append("Formato general:  COMANDO[\"param1\",\"param2\",...]\n\n");

        // ── CU1 ─────────────────────────────────────────────────────────────
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("CU1 — USUARIOS\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        sb.append("► LISUSR[\"*\"]\n");
        sb.append("  Lista todos los usuarios del sistema.\n");
        sb.append("  Ej: LISUSR[\"*\"]\n\n");

        sb.append("► INSUSR[\"ci\",\"nombre\",\"apellido\",\"rol\",\"ci_nit\",\"telefono\",\"email\",\"password\"]\n");
        sb.append("  Registra un usuario nuevo.\n");
        sb.append("  rol: PROPIETARIO | VENDEDOR | CLIENTE\n");
        sb.append("  Ej: INSUSR[\"12345678\",\"Maria\",\"Lopez\",\"CLIENTE\",\"12345678\",\"76543210\",\"maria@gmail.com\",\"pass123\"]\n\n");

        sb.append("► MODUSR[\"id\",\"campo\",\"nuevo_valor\"]\n");
        sb.append("  Modifica un campo de un usuario.\n");
        sb.append("  campos: nombre | apellido | rol | ci_nit | telefono | email | password\n");
        sb.append("  Ej: MODUSR[\"3\",\"telefono\",\"77001122\"]\n\n");

        sb.append("► ELIUSR[\"id\"]\n");
        sb.append("  Desactiva un usuario (no lo borra de la BD).\n");
        sb.append("  Ej: ELIUSR[\"5\"]\n\n");

        // ── CU2 ─────────────────────────────────────────────────────────────
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("CU2 — PRODUCTOS\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        sb.append("► LISPROD[\"*\"]\n");
        sb.append("  Lista todos los productos del catálogo.\n");
        sb.append("  Ej: LISPROD[\"*\"]\n\n");

        sb.append("► INSPROD[\"nombre\",\"descripcion\",\"precio\",\"stock\",\"categoria\"]\n");
        sb.append("  Registra un producto nuevo.\n");
        sb.append("  Ej: INSPROD[\"Silla Ergonomica\",\"Silla de oficina ajustable\",\"850.00\",\"15\",\"Oficina\"]\n\n");

        sb.append("► MODPROD[\"id\",\"campo\",\"nuevo_valor\"]\n");
        sb.append("  Modifica un campo de un producto.\n");
        sb.append("  campos: nombre | descripcion | precio | stock | categoria\n");
        sb.append("  Ej: MODPROD[\"4\",\"precio\",\"780.00\"]\n\n");

        sb.append("► ELIPROD[\"id\"]\n");
        sb.append("  Desactiva un producto (no lo borra de la BD).\n");
        sb.append("  Ej: ELIPROD[\"4\"]\n\n");

        // ── CU3 ─────────────────────────────────────────────────────────────
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("CU3 — PEDIDOS\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        sb.append("► LISPED[\"*\"]\n");
        sb.append("  Lista todos los pedidos.\n");
        sb.append("  Ej: LISPED[\"*\"]\n\n");

        sb.append("► INSPED[\"id_usuario\",\"observaciones\"]\n");
        sb.append("  Crea un pedido para un cliente. Responde con el ID_PEDIDO generado.\n");
        sb.append("  Ej: INSPED[\"4\",\"Pedido urgente para living\"]\n\n");

        sb.append("► INSDET[\"id_pedido\",\"id_producto\",\"cantidad\"]\n");
        sb.append("  Agrega un producto al detalle del pedido.\n");
        sb.append("  Aplica descuento de promoción activa automáticamente.\n");
        sb.append("  Ej: INSDET[\"11\",\"7\",\"2\"]\n\n");

        sb.append("► MODPED[\"id_pedido\",\"estado\"]\n");
        sb.append("  Cambia el estado del pedido.\n");
        sb.append("  estados: PENDIENTE | CONFIRMADO | ENTREGADO | CANCELADO\n");
        sb.append("  Ej: MODPED[\"11\",\"CONFIRMADO\"]\n\n");

        // ── CU4 ─────────────────────────────────────────────────────────────
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("CU4 — INVENTARIO\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        sb.append("► LISINV[\"id_producto\"]\n");
        sb.append("  Lista los movimientos de inventario de un producto.\n");
        sb.append("  Ej: LISINV[\"7\"]\n\n");

        sb.append("► INSINV[\"id_producto\",\"tipo\",\"cantidad\",\"costo_unitario\",\"tecnica\"]\n");
        sb.append("  Registra un ingreso o salida de inventario.\n");
        sb.append("  tipo: INGRESO | SALIDA\n");
        sb.append("  tecnica: PEPS | UEPS | PROMEDIO\n");
        sb.append("  Ej: INSINV[\"7\",\"INGRESO\",\"20\",\"350.00\",\"PEPS\"]\n\n");

        sb.append("► VALINV[\"id_producto\"]\n");
        sb.append("  Valorización del inventario en las 3 técnicas (PEPS, UEPS, PROMEDIO).\n");
        sb.append("  Ej: VALINV[\"7\"]\n\n");

        // ── CU5 ─────────────────────────────────────────────────────────────
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("CU5 — PROMOCIONES\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        sb.append("► LISPRO[\"*\"]\n");
        sb.append("  Lista todas las promociones (activas e inactivas).\n");
        sb.append("  Ej: LISPRO[\"*\"]\n\n");

        sb.append("► REPPRO[\"*\"]\n");
        sb.append("  Lista SOLO las promociones activas en este momento.\n");
        sb.append("  Ej: REPPRO[\"*\"]\n\n");

        sb.append("► INSPRO[\"id_producto\",\"porcentaje\",\"fecha_inicio\",\"fecha_fin\"]\n");
        sb.append("  Crea una promoción con descuento % para un producto.\n");
        sb.append("  Fechas en formato YYYY-MM-DD.\n");
        sb.append("  Ej: INSPRO[\"7\",\"20\",\"2026-06-01\",\"2026-12-31\"]\n\n");

        sb.append("► MODPRO[\"id_promocion\",\"activo\"]\n");
        sb.append("  Activa o desactiva una promoción.\n");
        sb.append("  Ej: MODPRO[\"2\",\"false\"]\n\n");

        sb.append("► ELIPRO[\"id_promocion\"]\n");
        sb.append("  Elimina permanentemente una promoción.\n");
        sb.append("  Ej: ELIPRO[\"2\"]\n\n");

        // ── CU6 ─────────────────────────────────────────────────────────────
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("CU6 — VENTAS\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        sb.append("► LISVEN[\"*\"]\n");
        sb.append("  Lista todas las ventas con nombre del vendedor.\n");
        sb.append("  Ej: LISVEN[\"*\"]\n\n");

        sb.append("► INSVEN[\"id_usuario\",\"id_pedido\",\"total\"]\n");
        sb.append("  Registra una venta sobre un pedido CONFIRMADO.\n");
        sb.append("  Responde con el ID_VENTA generado.\n");
        sb.append("  Ej: INSVEN[\"2\",\"11\",\"608.00\"]\n\n");

        sb.append("► MODVEN[\"id_venta\",\"estado\"]\n");
        sb.append("  Cambia el estado de una venta.\n");
        sb.append("  Ej: MODVEN[\"7\",\"COMPLETADA\"]\n\n");

        // ── CU7 ─────────────────────────────────────────────────────────────
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("CU7 — PAGOS\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        sb.append("► LISPAG[\"id_venta\"]\n");
        sb.append("  Muestra el pago y cuotas de una venta.\n");
        sb.append("  Ej: LISPAG[\"7\"]\n\n");

        sb.append("► INSPAG[\"id_venta\",\"CONTADO\",\"total\",\"1\"]\n");
        sb.append("  Registra pago al contado (sin interés).\n");
        sb.append("  Ej: INSPAG[\"7\",\"CONTADO\",\"608.00\",\"1\"]\n\n");

        sb.append("► INSPAG[\"id_venta\",\"CUOTAS\",\"total\",\"n_cuotas\",\"tasa\"]\n");
        sb.append("  Pago en cuotas con interés simple.\n");
        sb.append("  tasa: valor decimal, ej. 0.10 = 10%\n");
        sb.append("  Ej: INSPAG[\"7\",\"CUOTAS\",\"608.00\",\"3\",\"0.10\"]\n\n");

        sb.append("► INSPAG[\"id_venta\",\"CUOTAS\",\"total\",\"n_cuotas\",\"tasa\",\"cuota_inicial\"]\n");
        sb.append("  Pago en cuotas con interés y cuota inicial.\n");
        sb.append("  capital = total - cuota_inicial  |  interes = capital x tasa x n\n");
        sb.append("  Ej: INSPAG[\"7\",\"CUOTAS\",\"608.00\",\"3\",\"0.10\",\"100.00\"]\n\n");

        sb.append("► INSPAG[\"id_venta\",\"QR\",\"total\",\"1\"]\n");
        sb.append("  Genera pago QR via PagoFácil. Adjunta imagen qr.png al correo.\n");
        sb.append("  Ej: INSPAG[\"8\",\"QR\",\"0.10\",\"1\"]\n\n");

        sb.append("► MODPAG[\"id_cuota\",\"true\"]\n");
        sb.append("  Marca una cuota como pagada (registra fecha_pago automáticamente).\n");
        sb.append("  Ej: MODPAG[\"12\",\"true\"]\n\n");

        sb.append("► VERPAG[\"id_venta\"]\n");
        sb.append("  Consulta el estado del pago QR en PagoFácil y actualiza la BD.\n");
        sb.append("  Ej: VERPAG[\"8\"]\n\n");

        // ── CU8 ─────────────────────────────────────────────────────────────
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("CU8 — REPORTES\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        sb.append("► REPVEN[\"mes\",\"anio\"]\n");
        sb.append("  Ventas del mes indicado con total acumulado.\n");
        sb.append("  Ej: REPVEN[\"6\",\"2026\"]\n\n");

        sb.append("► REPINV[\"*\"]\n");
        sb.append("  Stock actual de todos los productos con valor total del inventario.\n");
        sb.append("  Ej: REPINV[\"*\"]\n\n");

        sb.append("► REPCUO[\"*\"]\n");
        sb.append("  Cuotas pendientes con columna VIGENTE/VENCIDA y total por cobrar.\n");
        sb.append("  Ej: REPCUO[\"*\"]\n\n");

        // ── Flujo de compra ──────────────────────────────────────────────────
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("EJEMPLO — FLUJO COMPLETO DE UNA COMPRA\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        sb.append("Supuestos: cliente ID=4, vendedor ID=2, producto ID=7 (precio 380 Bs)\n\n");

        sb.append("  1. Crear el pedido\n");
        sb.append("     INSPED[\"4\",\"Pedido sala de estar\"]\n");
        sb.append("     >> El sistema responde con el ID_PEDIDO generado (ej: 11)\n\n");

        sb.append("  2. Agregar producto al pedido  (aplica promo activa automáticamente)\n");
        sb.append("     INSDET[\"11\",\"7\",\"2\"]\n\n");

        sb.append("  3. Confirmar el pedido\n");
        sb.append("     MODPED[\"11\",\"CONFIRMADO\"]\n\n");

        sb.append("  4. Registrar la venta\n");
        sb.append("     INSVEN[\"2\",\"11\",\"760.00\"]\n");
        sb.append("     >> El sistema responde con el ID_VENTA generado (ej: 7)\n\n");

        sb.append("  5. ELEGIR TIPO DE PAGO:\n\n");

        sb.append("     ┌─ OPCIÓN A: CONTADO ──────────────────────────────────\n");
        sb.append("     │  Sin interés, pago inmediato.\n");
        sb.append("     │  INSPAG[\"7\",\"CONTADO\",\"760.00\",\"1\"]\n");
        sb.append("     └──────────────────────────────────────────────────────\n\n");

        sb.append("     ┌─ OPCIÓN B: CUOTAS (con interés simple) ──────────────\n");
        sb.append("     │  3 cuotas al 10% de interés simple.\n");
        sb.append("     │  Fórmula: interés = capital x tasa x n_cuotas\n");
        sb.append("     │           cuota   = (capital + interés) / n_cuotas\n");
        sb.append("     │  INSPAG[\"7\",\"CUOTAS\",\"760.00\",\"3\",\"0.10\"]\n");
        sb.append("     │\n");
        sb.append("     │  Con cuota inicial (ej: 100 Bs de entrada):\n");
        sb.append("     │  INSPAG[\"7\",\"CUOTAS\",\"760.00\",\"3\",\"0.10\",\"100.00\"]\n");
        sb.append("     │  (la cuota inicial se descuenta del capital)\n");
        sb.append("     └──────────────────────────────────────────────────────\n\n");

        sb.append("     ┌─ OPCIÓN C: QR PAGOFÁCIL ─────────────────────────────\n");
        sb.append("     │  Genera un QR real para pagar con app bancaria.\n");
        sb.append("     │  La imagen qr.png llega adjunta a este correo.\n");
        sb.append("     │  INSPAG[\"7\",\"QR\",\"760.00\",\"1\"]\n");
        sb.append("     │\n");
        sb.append("     │  Luego verificar si el cliente ya pagó:\n");
        sb.append("     │  VERPAG[\"7\"]\n");
        sb.append("     └──────────────────────────────────────────────────────\n\n");

        sb.append("  6. Ver detalle del pago y cuotas\n");
        sb.append("     LISPAG[\"7\"]\n\n");

        sb.append("  7. Marcar una cuota como pagada (solo para CUOTAS)\n");
        sb.append("     MODPAG[\"id_cuota\",\"true\"]\n\n");

        // ── Pie ──────────────────────────────────────────────────────────────
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("HELP[\"*\"]  →  muestra esta lista\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("Sistema automático — Morón Diseño de Interiores\n");
        sb.append("grupo14sa@tecnoweb.org.bo\n");

        return new Respuesta(true, sb.toString());
    }
}
