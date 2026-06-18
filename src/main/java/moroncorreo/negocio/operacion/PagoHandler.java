package moroncorreo.negocio.operacion;

import moroncorreo.infra.ConexionDB;
import moroncorreo.infra.PagoFacilClient;
import moroncorreo.negocio.Respuesta;
import moroncorreo.negocio.TablaFormato;

import java.sql.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.JsonObject;

public class PagoHandler {

    // LISPAG["id_venta"]
    public static Respuesta listar(String[] params) {
        if (params.length < 1) {
            return new Respuesta(false, "LISPAG requiere 1 parametro: id_venta");
        }
        String sqlPago = "SELECT id, tipo, total, cuotas, tasa_interes, cuota_inicial, fecha " +
                         "FROM pago WHERE id_venta=?";
        String sqlCuotas = "SELECT pc.id, pc.numero_cuota, pc.monto, pc.fecha_vencimiento, pc.pagado, pc.fecha_pago " +
                           "FROM pago_cuota pc JOIN pago p ON pc.id_pago = p.id " +
                           "WHERE p.id_venta=? ORDER BY pc.numero_cuota";

        try (Connection con = ConexionDB.getConnection()) {
            StringBuilder sb = new StringBuilder();

            try (PreparedStatement ps = con.prepareStatement(sqlPago)) {
                ps.setInt(1, Integer.parseInt(params[0]));
                ResultSet rs = ps.executeQuery();
                sb.append("-- PAGO --\n").append(TablaFormato.formatear(rs)).append("\n\n");
            }

            try (PreparedStatement ps = con.prepareStatement(sqlCuotas)) {
                ps.setInt(1, Integer.parseInt(params[0]));
                ResultSet rs = ps.executeQuery();
                sb.append("-- CUOTAS --\n").append(TablaFormato.formatear(rs));
            }

            return new Respuesta(true, sb.toString());
        } catch (Exception e) {
            return new Respuesta(false, "Error al listar pago: " + e.getMessage());
        }
    }

    /*
     * INSPAG["id_venta","tipo","total","cuotas"]                          (CONTADO o CUOTAS sin interés)
     * INSPAG["id_venta","CUOTAS","total","cuotas","tasa"]                 (con interés, sin cuota inicial)
     * INSPAG["id_venta","CUOTAS","total","cuotas","tasa","cuota_inicial"] (con interés y cuota inicial)
     *
     * Fórmula interés simple:
     *   capital          = total - cuota_inicial
     *   interes_total    = capital × tasa × n_cuotas
     *   total_financiado = capital + interes_total
     *   cuota_regular    = total_financiado / n_cuotas
     *
     * La cuota 1 es la cuota_inicial (si > 0), las siguientes son cuota_regular.
     * Ejemplo: total=5000, cuota_inicial=2500, cuotas=3, tasa=0.10
     *   capital=2500, interés=750, total_fin=3250, cuota_reg=1083.33
     *   Cuota 1: 2500.00 (inicial) — Cuotas 2-4: 1083.33
     */
    public static Respuesta insertar(String[] params) {
        if (params.length < 4) {
            return new Respuesta(false,
                    "INSPAG requiere al menos 4 parametros: id_venta,tipo,total,cuotas[,tasa][,cuota_inicial]");
        }

        int        idVenta      = Integer.parseInt(params[0]);
        String     tipo         = params[1].toUpperCase();
        BigDecimal total        = new BigDecimal(params[2]);
        int        numCuotas    = Integer.parseInt(params[3]);
        BigDecimal tasa         = params.length > 4 ? new BigDecimal(params[4]) : BigDecimal.ZERO;
        BigDecimal cuotaInicial = params.length > 5 ? new BigDecimal(params[5]) : BigDecimal.ZERO;

        if (!tipo.equals("CONTADO") && !tipo.equals("CUOTAS") && !tipo.equals("QR")) {
            return new Respuesta(false, "Tipo invalido: " + tipo + ". Use CONTADO, CUOTAS o QR.");
        }

        // === Flujo QR PagoFácil ===
        if (tipo.equals("QR")) {
            return generarPagoQR(idVenta, total);
        }
        if (cuotaInicial.compareTo(total) >= 0) {
            return new Respuesta(false, "La cuota inicial no puede ser mayor o igual al total.");
        }

        // Capital a financiar
        BigDecimal capital = total.subtract(cuotaInicial);

        // Interés simple sobre el capital financiado
        BigDecimal interesTotal    = capital.multiply(tasa).multiply(BigDecimal.valueOf(numCuotas))
                                            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalFinanciado = capital.add(interesTotal);

        // Cuota regular (el residuo centavos va a la última cuota)
        BigDecimal cuotaRegular = totalFinanciado.divide(BigDecimal.valueOf(numCuotas), 2, RoundingMode.FLOOR);
        BigDecimal residuo      = totalFinanciado.subtract(cuotaRegular.multiply(BigDecimal.valueOf(numCuotas)));

        String sqlPago  = "INSERT INTO pago (id_venta, tipo, total, cuotas, tasa_interes, cuota_inicial) " +
                          "VALUES (?,?,?,?,?,?) RETURNING id";
        String sqlCuota = "INSERT INTO pago_cuota (id_pago, numero_cuota, monto, fecha_vencimiento) VALUES (?,?,?,?)";

        try (Connection con = ConexionDB.getConnection()) {
            int idPago;
            try (PreparedStatement ps = con.prepareStatement(sqlPago)) {
                ps.setInt(1, idVenta);
                ps.setString(2, tipo);
                ps.setBigDecimal(3, total);
                ps.setInt(4, numCuotas);
                ps.setBigDecimal(5, tasa);
                ps.setBigDecimal(6, cuotaInicial);
                ResultSet rs = ps.executeQuery();
                rs.next();
                idPago = rs.getInt(1);
            }

            LocalDate hoy = LocalDate.now();
            try (PreparedStatement ps = con.prepareStatement(sqlCuota)) {
                // Cuota 0: cuota inicial (si existe)
                if (cuotaInicial.compareTo(BigDecimal.ZERO) > 0) {
                    ps.setInt(1, idPago);
                    ps.setInt(2, 0);
                    ps.setBigDecimal(3, cuotaInicial);
                    ps.setDate(4, java.sql.Date.valueOf(hoy));
                    ps.addBatch();
                }
                // Cuotas regulares
                for (int i = 1; i <= numCuotas; i++) {
                    BigDecimal monto = (i == numCuotas) ? cuotaRegular.add(residuo) : cuotaRegular;
                    ps.setInt(1, idPago);
                    ps.setInt(2, i);
                    ps.setBigDecimal(3, monto);
                    ps.setDate(4, java.sql.Date.valueOf(hoy.plusMonths(i)));
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // Resumen legible
            StringBuilder resumen = new StringBuilder();
            resumen.append("Pago id=").append(idPago).append(" creado\n");
            resumen.append("  Tipo           : ").append(tipo).append("\n");
            resumen.append("  Total venta    : ").append(total).append(" Bs.\n");
            if (cuotaInicial.compareTo(BigDecimal.ZERO) > 0)
                resumen.append("  Cuota inicial  : ").append(cuotaInicial).append(" Bs. (hoy)\n");
            resumen.append("  Capital        : ").append(capital).append(" Bs.\n");
            if (tasa.compareTo(BigDecimal.ZERO) > 0) {
                resumen.append("  Tasa (simple)  : ").append(tasa.multiply(BigDecimal.valueOf(100))).append("% por cuota\n");
                resumen.append("  Interés total  : ").append(interesTotal).append(" Bs.\n");
                resumen.append("  Total financiad: ").append(totalFinanciado).append(" Bs.\n");
            }
            resumen.append("  Cuotas         : ").append(numCuotas).append(" x ").append(cuotaRegular).append(" Bs.\n");
            resumen.append("  Vencimientos   : 1 mes por cuota a partir de hoy");

            return new Respuesta(true, resumen.toString());
        } catch (Exception e) {
            return new Respuesta(false, "Error al insertar pago: " + e.getMessage());
        }
    }

    // MODPAG["id_cuota","pagado"]
    public static Respuesta modificar(String[] params) {
        if (params.length < 2) {
            return new Respuesta(false, "MODPAG requiere 2 parametros: id_cuota,pagado(true/false)");
        }
        boolean pagado = Boolean.parseBoolean(params[1]);
        String sql = pagado
                ? "UPDATE pago_cuota SET pagado=true,  fecha_pago=NOW() WHERE id=?"
                : "UPDATE pago_cuota SET pagado=false, fecha_pago=NULL  WHERE id=?";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(params[0]));
            int filas = ps.executeUpdate();
            if (filas == 0) return new Respuesta(false, "No existe cuota con id=" + params[0]);
            return new Respuesta(true, "Cuota id=" + params[0] + " marcada como pagado=" + pagado);
        } catch (Exception e) {
            return new Respuesta(false, "Error al modificar cuota: " + e.getMessage());
        }
    }

    // =============================================
    //  PagoFácil QR — Generar QR
    // =============================================
    private static Respuesta generarPagoQR(int idVenta, BigDecimal total) {
        // SQL para obtener datos del cliente a través de venta → pedido → usuario
        String sqlCliente = """
                SELECT u.nombre || ' ' || u.apellido AS nombre_completo,
                       u.email, u.telefono, u.ci
                FROM venta v
                JOIN pedido p ON v.id_pedido = p.id
                JOIN usuario u ON p.id_usuario = u.id
                WHERE v.id = ?
                """;
        // SQL para obtener los productos del pedido asociado a la venta
        String sqlDetalle = """
                SELECT pr.nombre, pd.cantidad, pd.precio_unitario, pd.subtotal
                FROM venta v
                JOIN pedido_detalle pd ON pd.id_pedido = v.id_pedido
                JOIN producto pr ON pr.id = pd.id_producto
                WHERE v.id = ?
                """;
        String sqlPago = "INSERT INTO pago (id_venta, tipo, total, cuotas) VALUES (?,?,?,?) RETURNING id";
        String sqlCuota = "INSERT INTO pago_cuota (id_pago, numero_cuota, monto, fecha_vencimiento) VALUES (?,?,?,?)";
        String sqlQR = "INSERT INTO pago_qr (id_pago, pagofacil_transaction_id, payment_number, checkout_url) VALUES (?,?,?,?)";

        try (Connection con = ConexionDB.getConnection()) {
            // 1. Obtener datos del cliente
            String nombreCliente, email, telefono, ci;
            try (PreparedStatement ps = con.prepareStatement(sqlCliente)) {
                ps.setInt(1, idVenta);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) return new Respuesta(false, "No existe venta con id=" + idVenta);
                nombreCliente = rs.getString("nombre_completo");
                email         = rs.getString("email");
                telefono      = rs.getString("telefono");
                ci            = rs.getString("ci");
                if (telefono == null || telefono.isBlank()) telefono = "70000000";
            }

            // 2. Obtener detalle del pedido
            List<PagoFacilClient.DetalleQR> detalle = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(sqlDetalle)) {
                ps.setInt(1, idVenta);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    detalle.add(new PagoFacilClient.DetalleQR(
                            rs.getString("nombre"),
                            rs.getInt("cantidad"),
                            rs.getBigDecimal("precio_unitario"),
                            rs.getBigDecimal("subtotal")
                    ));
                }
            }
            if (detalle.isEmpty()) {
                return new Respuesta(false, "La venta id=" + idVenta + " no tiene productos asociados.");
            }

            // 3. Generar identificador único para PagoFácil
            String paymentNumber = "VENTA-" + idVenta + "-" + System.currentTimeMillis();

            // 4. Llamar a la API de PagoFácil para generar el QR
            JsonObject qrResponse = PagoFacilClient.generarQR(
                    paymentNumber, total, nombreCliente, email, telefono, ci, detalle);

            String transactionId = qrResponse.get("transactionId").getAsString();

            // PagoFácil puede devolver varias URLs según el método de pago
            String checkoutUrl   = extraerUrl(qrResponse, "checkoutUrl");
            String qrContentUrl  = extraerUrl(qrResponse, "qrContentUrl");
            String universalUrl  = extraerUrl(qrResponse, "universalUrl");
            String deepLink      = extraerUrl(qrResponse, "deepLink");

            // Usar la mejor URL disponible para guardar en BD
            String mejorUrl = !checkoutUrl.isBlank() ? checkoutUrl
                            : !qrContentUrl.isBlank() ? qrContentUrl
                            : !universalUrl.isBlank() ? universalUrl
                            : deepLink;

            // 5. Guardar en la BD: pago + cuota + pago_qr
            int idPago;
            try (PreparedStatement ps = con.prepareStatement(sqlPago)) {
                ps.setInt(1, idVenta);
                ps.setString(2, "QR");
                ps.setBigDecimal(3, total);
                ps.setInt(4, 1);
                ResultSet rs = ps.executeQuery();
                rs.next();
                idPago = rs.getInt(1);
            }

            try (PreparedStatement ps = con.prepareStatement(sqlCuota)) {
                ps.setInt(1, idPago);
                ps.setInt(2, 1);
                ps.setBigDecimal(3, total);
                ps.setDate(4, java.sql.Date.valueOf(LocalDate.now()));
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement(sqlQR)) {
                ps.setInt(1, idPago);
                ps.setString(2, transactionId);
                ps.setString(3, paymentNumber);
                ps.setString(4, mejorUrl);
                ps.executeUpdate();
            }

            // 6. Armar respuesta con links de pago
            StringBuilder resumen = new StringBuilder();
            resumen.append("=== PAGO QR PagoFacil ===").append("\n");
            resumen.append("Venta ID      : ").append(idVenta).append("\n");
            resumen.append("Total         : ").append(total).append(" Bs.\n");
            resumen.append("Cliente       : ").append(nombreCliente).append("\n");
            resumen.append("Transaccion PF: ").append(transactionId).append("\n");
            resumen.append("\n");

            // Mostrar todos los links disponibles
            boolean hayLink = false;
            if (!checkoutUrl.isBlank()) {
                resumen.append("LINK DE PAGO (Checkout):\n").append(checkoutUrl).append("\n\n");
                hayLink = true;
            }
            if (!qrContentUrl.isBlank()) {
                resumen.append("LINK QR DIRECTO:\n").append(qrContentUrl).append("\n\n");
                hayLink = true;
            }
            if (!universalUrl.isBlank()) {
                resumen.append("LINK UNIVERSAL:\n").append(universalUrl).append("\n\n");
                hayLink = true;
            }
            if (!deepLink.isBlank()) {
                resumen.append("DEEP LINK (App):\n").append(deepLink).append("\n\n");
                hayLink = true;
            }

            byte[] imagenQr = null;
            if (qrResponse.has("qrBase64") && !qrResponse.get("qrBase64").isJsonNull()) {
                String base64 = qrResponse.get("qrBase64").getAsString();
                if (!base64.isBlank()) {
                    try {
                        imagenQr = java.util.Base64.getDecoder().decode(base64);
                    } catch (Exception e) {
                        System.out.println("  [PagoFacil] Error al decodificar qrBase64: " + e.getMessage());
                    }
                }
            }

            if (hayLink) {
                resumen.append("Abra cualquiera de estos enlaces para pagar con QR.\n");
            } else if (imagenQr != null) {
                resumen.append("Se adjuntó la imagen del código QR a este correo para que pueda realizar el escaneo.\n");
            } else {
                resumen.append("No se recibieron enlaces de pago. Contacte al administrador.\n");
            }
            resumen.append("\nPara verificar si el pago fue realizado, envie:\n");
            resumen.append("VERPAG[\"" + idVenta + "\"]\n");

            return new Respuesta(true, resumen.toString(), imagenQr);
        } catch (Exception e) {
            return new Respuesta(false, "Error al generar pago QR: " + e.getMessage());
        }
    }

    /** Extrae una URL del JSON de PagoFácil de forma segura */
    private static String extraerUrl(JsonObject json, String campo) {
        if (json.has(campo) && !json.get(campo).isJsonNull()) {
            String val = json.get(campo).getAsString();
            return val != null ? val.trim() : "";
        }
        return "";
    }

    // =============================================
    //  VERPAG["id_venta"] — Verificar pago QR
    // =============================================
    public static Respuesta verificarQR(String[] params) {
        if (params.length < 1) {
            return new Respuesta(false, "VERPAG requiere 1 parametro: id_venta");
        }
        int idVenta = Integer.parseInt(params[0]);

        String sqlBuscar = """
                SELECT pq.id AS pq_id, pq.pagofacil_transaction_id, pq.estado, pq.checkout_url,
                       p.id AS id_pago, p.total,
                       pc.id AS id_cuota
                FROM pago_qr pq
                JOIN pago p ON pq.id_pago = p.id
                JOIN pago_cuota pc ON pc.id_pago = p.id
                WHERE p.id_venta = ?
                ORDER BY pq.id DESC LIMIT 1
                """;

        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sqlBuscar)) {
            ps.setInt(1, idVenta);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                return new Respuesta(false, "No existe un pago QR para la venta id=" + idVenta);
            }

            int    pqId           = rs.getInt("pq_id");
            String transactionId  = rs.getString("pagofacil_transaction_id");
            String estadoLocal    = rs.getString("estado");
            String checkoutUrl    = rs.getString("checkout_url");
            BigDecimal totalPago  = rs.getBigDecimal("total");
            int    idCuota        = rs.getInt("id_cuota");

            // Si ya lo marcamos como PAGADO localmente, no consultar de nuevo
            if ("PAGADO".equals(estadoLocal)) {
                return new Respuesta(true,
                        "El pago QR de la venta id=" + idVenta + " ya fue CONFIRMADO anteriormente.\n" +
                        "Total: " + totalPago + " Bs.");
            }

            // Consultar a PagoFácil el estado actual
            JsonObject txn = PagoFacilClient.consultarTransaccion(transactionId);
            int paymentStatus = txn.get("paymentStatus").getAsInt();

            // 1=PENDIENTE, 2=PAGADO, 3=REVERTIDO, 4=ANULADO
            String nuevoEstado;
            switch (paymentStatus) {
                case 2  -> nuevoEstado = "PAGADO";
                case 3  -> nuevoEstado = "ANULADO";
                case 4  -> nuevoEstado = "ANULADO";
                default -> nuevoEstado = "PENDIENTE";
            }

            // Actualizar estado en pago_qr
            String sqlUpdateQR = nuevoEstado.equals("PAGADO")
                    ? "UPDATE pago_qr SET estado=?, fecha_pago=NOW() WHERE id=?"
                    : "UPDATE pago_qr SET estado=? WHERE id=?";
            try (PreparedStatement psUpd = con.prepareStatement(sqlUpdateQR)) {
                psUpd.setString(1, nuevoEstado);
                psUpd.setInt(2, pqId);
                psUpd.executeUpdate();
            }

            // Si está PAGADO, marcar la cuota automáticamente
            if ("PAGADO".equals(nuevoEstado)) {
                try (PreparedStatement psUpd = con.prepareStatement(
                        "UPDATE pago_cuota SET pagado=true, fecha_pago=NOW() WHERE id=?")) {
                    psUpd.setInt(1, idCuota);
                    psUpd.executeUpdate();
                }
                String fechaPago = txn.has("paymentDate") && !txn.get("paymentDate").isJsonNull()
                        ? txn.get("paymentDate").getAsString() : "hoy";
                return new Respuesta(true,
                        "PAGO QR CONFIRMADO!\n" +
                        "Venta ID     : " + idVenta + "\n" +
                        "Total pagado : " + totalPago + " Bs.\n" +
                        "Fecha pago   : " + fechaPago + "\n" +
                        "Transaccion  : " + transactionId + "\n" +
                        "\nLa cuota ha sido marcada como pagada automaticamente.");
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("Estado del pago QR: ").append(nuevoEstado).append("\n");
                sb.append("Venta ID: ").append(idVenta).append("\n");
                sb.append("Total   : ").append(totalPago).append(" Bs.\n");
                if ("PENDIENTE".equals(nuevoEstado) && checkoutUrl != null && !checkoutUrl.isBlank()) {
                    sb.append("\nEl cliente aun no ha pagado. Link de pago:\n");
                    sb.append(checkoutUrl).append("\n");
                }
                return new Respuesta(true, sb.toString());
            }
        } catch (Exception e) {
            return new Respuesta(false, "Error al verificar pago QR: " + e.getMessage());
        }
    }
}
