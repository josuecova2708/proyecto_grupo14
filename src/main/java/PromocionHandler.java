import java.sql.*;

public class PromocionHandler {

    // LISPRO["*"]
    public static Respuesta listar(String[] params) {
        String sql = "SELECT p.id, prod.nombre AS producto, p.porcentaje, p.fecha_inicio, p.fecha_fin, p.activo, p.descripcion " +
                     "FROM promocion p JOIN producto prod ON p.id_producto = prod.id ORDER BY p.id";
        try (Connection con = ConexionDB.getConnection();
             Statement st  = con.createStatement();
             ResultSet rs  = st.executeQuery(sql)) {
            return new Respuesta(true, TablaFormato.formatear(rs));
        } catch (Exception e) {
            return new Respuesta(false, "Error al listar promociones: " + e.getMessage());
        }
    }

    // INSPRO["id_producto","porcentaje","inicio","fin"]  (inicio/fin: YYYY-MM-DD)
    public static Respuesta insertar(String[] params) {
        if (params.length < 4) {
            return new Respuesta(false, "INSPRO requiere 4 parametros: id_producto,porcentaje,inicio(YYYY-MM-DD),fin(YYYY-MM-DD)");
        }
        String sql = "INSERT INTO promocion (id_producto, porcentaje, fecha_inicio, fecha_fin) VALUES (?,?,?,?)";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(params[0]));
            ps.setBigDecimal(2, new java.math.BigDecimal(params[1]));
            ps.setDate(3, java.sql.Date.valueOf(params[2]));
            ps.setDate(4, java.sql.Date.valueOf(params[3]));
            ps.executeUpdate();
            return new Respuesta(true, "Promocion creada para producto id=" + params[0] +
                    " | " + params[1] + "% | " + params[2] + " al " + params[3]);
        } catch (Exception e) {
            return new Respuesta(false, "Error al insertar promocion: " + e.getMessage());
        }
    }

    // MODPRO["id","activo"]  (activo: true/false)
    public static Respuesta modificar(String[] params) {
        if (params.length < 2) {
            return new Respuesta(false, "MODPRO requiere 2 parametros: id,activo(true/false)");
        }
        String sql = "UPDATE promocion SET activo=? WHERE id=?";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBoolean(1, Boolean.parseBoolean(params[1]));
            ps.setInt(2, Integer.parseInt(params[0]));
            int filas = ps.executeUpdate();
            if (filas == 0) return new Respuesta(false, "No existe promocion con id=" + params[0]);
            return new Respuesta(true, "Promocion id=" + params[0] + " activo=" + params[1]);
        } catch (Exception e) {
            return new Respuesta(false, "Error al modificar promocion: " + e.getMessage());
        }
    }

    // ELIPRO["id"]
    public static Respuesta eliminar(String[] params) {
        if (params.length < 1) {
            return new Respuesta(false, "ELIPRO requiere 1 parametro: id");
        }
        String sql = "DELETE FROM promocion WHERE id=?";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(params[0]));
            int filas = ps.executeUpdate();
            if (filas == 0) return new Respuesta(false, "No existe promocion con id=" + params[0]);
            return new Respuesta(true, "Promocion id=" + params[0] + " eliminada.");
        } catch (Exception e) {
            return new Respuesta(false, "Error al eliminar promocion: " + e.getMessage());
        }
    }
}
