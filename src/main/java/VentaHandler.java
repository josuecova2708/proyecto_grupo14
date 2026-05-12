import java.sql.*;

public class VentaHandler {

    // LISVEN["*"]
    public static Respuesta listar(String[] params) {
        String sql = "SELECT v.id, u.nombre || ' ' || u.apellido AS vendedor, v.id_pedido, " +
                     "v.fecha, v.total, v.estado " +
                     "FROM venta v JOIN usuario u ON v.id_usuario = u.id ORDER BY v.id";
        try (Connection con = ConexionDB.getConnection();
             Statement st  = con.createStatement();
             ResultSet rs  = st.executeQuery(sql)) {
            return new Respuesta(true, TablaFormato.formatear(rs));
        } catch (Exception e) {
            return new Respuesta(false, "Error al listar ventas: " + e.getMessage());
        }
    }

    // INSVEN["id_usuario","id_pedido","total"]
    public static Respuesta insertar(String[] params) {
        if (params.length < 3) {
            return new Respuesta(false, "INSVEN requiere 3 parametros: id_usuario,id_pedido,total");
        }
        String sql = "INSERT INTO venta (id_usuario, id_pedido, total) VALUES (?,?,?) RETURNING id";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(params[0]));
            ps.setInt(2, Integer.parseInt(params[1]));
            ps.setBigDecimal(3, new java.math.BigDecimal(params[2]));
            ResultSet rs = ps.executeQuery();
            rs.next();
            int id = rs.getInt(1);
            return new Respuesta(true, "Venta creada con id=" + id + " | Total: " + params[2]);
        } catch (Exception e) {
            return new Respuesta(false, "Error al insertar venta: " + e.getMessage());
        }
    }

    // MODVEN["id","estado"]
    public static Respuesta modificar(String[] params) {
        if (params.length < 2) {
            return new Respuesta(false, "MODVEN requiere 2 parametros: id,estado");
        }
        String sql = "UPDATE venta SET estado=? WHERE id=?";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, params[1].toUpperCase());
            ps.setInt(2, Integer.parseInt(params[0]));
            int filas = ps.executeUpdate();
            if (filas == 0) return new Respuesta(false, "No existe venta con id=" + params[0]);
            return new Respuesta(true, "Venta id=" + params[0] + " actualizada a estado: " + params[1].toUpperCase());
        } catch (Exception e) {
            return new Respuesta(false, "Error al modificar venta: " + e.getMessage());
        }
    }
}
