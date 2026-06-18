package moroncorreo.negocio.administracion;

import moroncorreo.infra.ConexionDB;
import moroncorreo.negocio.Respuesta;
import moroncorreo.negocio.TablaFormato;

import java.sql.*;

public class ReporteHandler {

    // REPVEN["mes","anio"]
    public static Respuesta ventasMes(String[] params) {
        if (params.length < 2) {
            return new Respuesta(false, "REPVEN requiere 2 parametros: mes,anio");
        }
        String sql = "SELECT v.id, v.fecha, u.nombre || ' ' || u.apellido AS vendedor, " +
                     "v.id_pedido, v.total, v.estado " +
                     "FROM venta v JOIN usuario u ON v.id_usuario = u.id " +
                     "WHERE EXTRACT(MONTH FROM v.fecha) = ? AND EXTRACT(YEAR FROM v.fecha) = ? " +
                     "ORDER BY v.fecha";
        String sqlTotal = "SELECT COALESCE(SUM(total), 0) AS total_mes FROM venta " +
                          "WHERE EXTRACT(MONTH FROM fecha) = ? AND EXTRACT(YEAR FROM fecha) = ?";
        try (Connection con = ConexionDB.getConnection()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Reporte de ventas - ").append(params[0]).append("/").append(params[1]).append("\n\n");

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, Integer.parseInt(params[0]));
                ps.setInt(2, Integer.parseInt(params[1]));
                ResultSet rs = ps.executeQuery();
                sb.append(TablaFormato.formatear(rs)).append("\n\n");
            }

            try (PreparedStatement ps = con.prepareStatement(sqlTotal)) {
                ps.setInt(1, Integer.parseInt(params[0]));
                ps.setInt(2, Integer.parseInt(params[1]));
                ResultSet rs = ps.executeQuery();
                rs.next();
                sb.append("TOTAL DEL MES: ").append(rs.getBigDecimal(1)).append(" Bs.");
            }

            return new Respuesta(true, sb.toString());
        } catch (Exception e) {
            return new Respuesta(false, "Error al generar reporte de ventas: " + e.getMessage());
        }
    }

    // REPINV["*"]
    public static Respuesta stockActual(String[] params) {
        String sql = "SELECT id, nombre, categoria, stock, precio, " +
                     "CAST(stock * precio AS NUMERIC(10,2)) AS valor_total " +
                     "FROM producto WHERE activo=true ORDER BY categoria, nombre";
        String sqlResumen = "SELECT COUNT(*) AS productos, SUM(stock) AS unidades_totales, " +
                            "SUM(stock * precio) AS valor_total_inventario FROM producto WHERE activo=true";
        try (Connection con = ConexionDB.getConnection()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Reporte de inventario actual\n\n");

            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                sb.append(TablaFormato.formatear(rs)).append("\n\n");
            }

            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sqlResumen)) {
                rs.next();
                sb.append("Productos activos : ").append(rs.getInt("productos")).append("\n");
                sb.append("Unidades totales  : ").append(rs.getInt("unidades_totales")).append("\n");
                sb.append("Valor inventario  : ").append(rs.getBigDecimal("valor_total_inventario")).append(" Bs.");
            }

            return new Respuesta(true, sb.toString());
        } catch (Exception e) {
            return new Respuesta(false, "Error al generar reporte de inventario: " + e.getMessage());
        }
    }

    // REPPRO["*"]
    public static Respuesta promocionesActivas(String[] params) {
        String sql = "SELECT p.id, prod.nombre AS producto, p.porcentaje, " +
                     "p.fecha_inicio, p.fecha_fin, p.descripcion " +
                     "FROM promocion p JOIN producto prod ON p.id_producto = prod.id " +
                     "WHERE p.activo = true AND CURRENT_DATE BETWEEN p.fecha_inicio AND p.fecha_fin " +
                     "ORDER BY p.id";
        try (Connection con = ConexionDB.getConnection();
             Statement st  = con.createStatement();
             ResultSet rs  = st.executeQuery(sql)) {
            String tabla = TablaFormato.formatear(rs);
            return new Respuesta(true, "Promociones activas hoy (" +
                    java.time.LocalDate.now() + "):\n\n" + tabla);
        } catch (Exception e) {
            return new Respuesta(false, "Error al generar reporte de promociones: " + e.getMessage());
        }
    }

    // REPCUO["*"]
    public static Respuesta cuotasPendientes(String[] params) {
        String sql = "SELECT pc.id, pc.numero_cuota, pc.monto, pc.fecha_vencimiento, " +
                     "pag.id_venta, u.nombre || ' ' || u.apellido AS cliente, " +
                     "CASE WHEN pc.fecha_vencimiento < CURRENT_DATE THEN 'VENCIDA' ELSE 'VIGENTE' END AS situacion " +
                     "FROM pago_cuota pc " +
                     "JOIN pago pag ON pc.id_pago = pag.id " +
                     "JOIN venta v ON pag.id_venta = v.id " +
                     "JOIN usuario u ON v.id_usuario = u.id " +
                     "WHERE pc.pagado = false ORDER BY pc.fecha_vencimiento";
        String sqlTotal = "SELECT COALESCE(SUM(monto), 0) FROM pago_cuota WHERE pagado = false";

        try (Connection con = ConexionDB.getConnection()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Cuotas pendientes de pago:\n\n");

            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                sb.append(TablaFormato.formatear(rs)).append("\n\n");
            }

            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sqlTotal)) {
                rs.next();
                sb.append("TOTAL PENDIENTE: ").append(rs.getBigDecimal(1)).append(" Bs.");
            }

            return new Respuesta(true, sb.toString());
        } catch (Exception e) {
            return new Respuesta(false, "Error al generar reporte de cuotas: " + e.getMessage());
        }
    }
}
