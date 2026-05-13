import java.sql.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

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

        if (!tipo.equals("CONTADO") && !tipo.equals("CUOTAS")) {
            return new Respuesta(false, "Tipo invalido: " + tipo + ". Use CONTADO o CUOTAS.");
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
}
