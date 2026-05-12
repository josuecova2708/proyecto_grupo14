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
        String sqlPago = "SELECT id, tipo, total, cuotas, fecha FROM pago WHERE id_venta=?";
        String sqlCuotas = "SELECT pc.id, pc.numero_cuota, pc.monto, pc.fecha_vencimiento, pc.pagado, pc.fecha_pago " +
                           "FROM pago_cuota pc JOIN pago p ON pc.id_pago = p.id WHERE p.id_venta=? ORDER BY pc.numero_cuota";

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

    // INSPAG["id_venta","tipo","total","cuotas"]
    public static Respuesta insertar(String[] params) {
        if (params.length < 4) {
            return new Respuesta(false, "INSPAG requiere 4 parametros: id_venta,tipo(CONTADO/CUOTAS),total,cuotas");
        }
        int    idVenta = Integer.parseInt(params[0]);
        String tipo    = params[1].toUpperCase();
        BigDecimal total = new BigDecimal(params[2]);
        int    numCuotas = Integer.parseInt(params[3]);

        if (!tipo.equals("CONTADO") && !tipo.equals("CUOTAS")) {
            return new Respuesta(false, "Tipo invalido: " + tipo + ". Use CONTADO o CUOTAS.");
        }

        String sqlPago  = "INSERT INTO pago (id_venta, tipo, total, cuotas) VALUES (?,?,?,?) RETURNING id";
        String sqlCuota = "INSERT INTO pago_cuota (id_pago, numero_cuota, monto, fecha_vencimiento) VALUES (?,?,?,?)";

        try (Connection con = ConexionDB.getConnection()) {
            int idPago;
            try (PreparedStatement ps = con.prepareStatement(sqlPago)) {
                ps.setInt(1, idVenta);
                ps.setString(2, tipo);
                ps.setBigDecimal(3, total);
                ps.setInt(4, numCuotas);
                ResultSet rs = ps.executeQuery();
                rs.next();
                idPago = rs.getInt(1);
            }

            // Distribuir monto en cuotas
            BigDecimal montoCuota = total.divide(BigDecimal.valueOf(numCuotas), 2, RoundingMode.FLOOR);
            BigDecimal resto      = total.subtract(montoCuota.multiply(BigDecimal.valueOf(numCuotas)));

            LocalDate hoy = LocalDate.now();
            try (PreparedStatement ps = con.prepareStatement(sqlCuota)) {
                for (int i = 1; i <= numCuotas; i++) {
                    BigDecimal monto = (i == numCuotas) ? montoCuota.add(resto) : montoCuota;
                    LocalDate vencimiento = hoy.plusMonths(i);
                    ps.setInt(1, idPago);
                    ps.setInt(2, i);
                    ps.setBigDecimal(3, monto);
                    ps.setDate(4, java.sql.Date.valueOf(vencimiento));
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            return new Respuesta(true, "Pago id=" + idPago + " creado | Tipo: " + tipo +
                    " | Total: " + total + " | " + numCuotas + " cuota(s) generada(s).");
        } catch (Exception e) {
            return new Respuesta(false, "Error al insertar pago: " + e.getMessage());
        }
    }

    // MODPAG["id_cuota","pagado"]  (pagado: true/false)
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
