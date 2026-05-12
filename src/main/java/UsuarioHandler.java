import java.sql.*;
import java.util.Set;

public class UsuarioHandler {

    // LISUSR["*"]
    public static Respuesta listar(String[] params) {
        String sql = "SELECT id, ci, nombre, apellido, rol, ci_nit, telefono, email, activo, fecha_registro " +
                     "FROM usuario ORDER BY id";
        try (Connection con = ConexionDB.getConnection();
             Statement st  = con.createStatement();
             ResultSet rs  = st.executeQuery(sql)) {
            return new Respuesta(true, TablaFormato.formatear(rs));
        } catch (Exception e) {
            return new Respuesta(false, "Error al listar usuarios: " + e.getMessage());
        }
    }

    // INSUSR["ci","nombre","apellido","rol","ci_nit","tel","email","pass"]
    public static Respuesta insertar(String[] params) {
        if (params.length < 8) {
            return new Respuesta(false, "INSUSR requiere 8 parametros: ci,nombre,apellido,rol,ci_nit,tel,email,pass");
        }
        String sql = "INSERT INTO usuario (ci,nombre,apellido,rol,ci_nit,telefono,email,password) " +
                     "VALUES (?,?,?,?,?,?,?,?)";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, params[0]);
            ps.setString(2, params[1]);
            ps.setString(3, params[2]);
            ps.setString(4, params[3].toUpperCase());
            ps.setString(5, params[4]);
            ps.setString(6, params[5]);
            ps.setString(7, params[6]);
            ps.setString(8, params[7]);
            ps.executeUpdate();
            return new Respuesta(true, "Usuario insertado correctamente.");
        } catch (Exception e) {
            return new Respuesta(false, "Error al insertar usuario: " + e.getMessage());
        }
    }

    // MODUSR["id","campo","valor"]
    public static Respuesta modificar(String[] params) {
        if (params.length < 3) {
            return new Respuesta(false, "MODUSR requiere 3 parametros: id,campo,valor");
        }
        Set<String> camposValidos = Set.of("nombre","apellido","rol","ci_nit","telefono","email","password");
        String campo = params[1].toLowerCase();
        if (!camposValidos.contains(campo)) {
            return new Respuesta(false, "Campo no permitido: " + campo + ". Validos: " + camposValidos);
        }
        String sql = "UPDATE usuario SET " + campo + "=? WHERE id=?";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, params[2]);
            ps.setInt(2, Integer.parseInt(params[0]));
            int filas = ps.executeUpdate();
            if (filas == 0) return new Respuesta(false, "No existe usuario con id=" + params[0]);
            return new Respuesta(true, "Usuario id=" + params[0] + " actualizado: " + campo + " = " + params[2]);
        } catch (Exception e) {
            return new Respuesta(false, "Error al modificar usuario: " + e.getMessage());
        }
    }

    // ELIUSR["id"]
    public static Respuesta eliminar(String[] params) {
        if (params.length < 1) {
            return new Respuesta(false, "ELIUSR requiere 1 parametro: id");
        }
        String sql = "UPDATE usuario SET activo=false WHERE id=?";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(params[0]));
            int filas = ps.executeUpdate();
            if (filas == 0) return new Respuesta(false, "No existe usuario con id=" + params[0]);
            return new Respuesta(true, "Usuario id=" + params[0] + " desactivado.");
        } catch (Exception e) {
            return new Respuesta(false, "Error al eliminar usuario: " + e.getMessage());
        }
    }
}
