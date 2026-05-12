import java.sql.*;
import java.util.Set;

public class ProductoHandler {

    // LISPROD["*"]
    public static Respuesta listar(String[] params) {
        String sql = "SELECT id, nombre, descripcion, precio, stock, categoria, activo, fecha_registro " +
                     "FROM producto ORDER BY id";
        try (Connection con = ConexionDB.getConnection();
             Statement st  = con.createStatement();
             ResultSet rs  = st.executeQuery(sql)) {
            return new Respuesta(true, TablaFormato.formatear(rs));
        } catch (Exception e) {
            return new Respuesta(false, "Error al listar productos: " + e.getMessage());
        }
    }

    // INSPROD["nombre","desc","precio","stock","categoria"]
    public static Respuesta insertar(String[] params) {
        if (params.length < 5) {
            return new Respuesta(false, "INSPROD requiere 5 parametros: nombre,desc,precio,stock,categoria");
        }
        String sql = "INSERT INTO producto (nombre, descripcion, precio, stock, categoria) VALUES (?,?,?,?,?)";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, params[0]);
            ps.setString(2, params[1]);
            ps.setBigDecimal(3, new java.math.BigDecimal(params[2]));
            ps.setInt(4, Integer.parseInt(params[3]));
            ps.setString(5, params[4]);
            ps.executeUpdate();
            return new Respuesta(true, "Producto insertado correctamente.");
        } catch (Exception e) {
            return new Respuesta(false, "Error al insertar producto: " + e.getMessage());
        }
    }

    // MODPROD["id","campo","valor"]
    public static Respuesta modificar(String[] params) {
        if (params.length < 3) {
            return new Respuesta(false, "MODPROD requiere 3 parametros: id,campo,valor");
        }
        Set<String> camposValidos = Set.of("nombre","descripcion","precio","stock","categoria");
        String campo = params[1].toLowerCase();
        if (!camposValidos.contains(campo)) {
            return new Respuesta(false, "Campo no permitido: " + campo + ". Validos: " + camposValidos);
        }
        String sql = "UPDATE producto SET " + campo + "=? WHERE id=?";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, params[2]);
            ps.setInt(2, Integer.parseInt(params[0]));
            int filas = ps.executeUpdate();
            if (filas == 0) return new Respuesta(false, "No existe producto con id=" + params[0]);
            return new Respuesta(true, "Producto id=" + params[0] + " actualizado: " + campo + " = " + params[2]);
        } catch (Exception e) {
            return new Respuesta(false, "Error al modificar producto: " + e.getMessage());
        }
    }

    // ELIPROD["id"]
    public static Respuesta eliminar(String[] params) {
        if (params.length < 1) {
            return new Respuesta(false, "ELIPROD requiere 1 parametro: id");
        }
        String sql = "UPDATE producto SET activo=false WHERE id=?";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(params[0]));
            int filas = ps.executeUpdate();
            if (filas == 0) return new Respuesta(false, "No existe producto con id=" + params[0]);
            return new Respuesta(true, "Producto id=" + params[0] + " desactivado.");
        } catch (Exception e) {
            return new Respuesta(false, "Error al eliminar producto: " + e.getMessage());
        }
    }
}
