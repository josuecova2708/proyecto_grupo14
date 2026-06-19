package moroncorreo.negocio.administracion;

import moroncorreo.infra.ConexionDB;
import moroncorreo.negocio.Respuesta;
import moroncorreo.negocio.TablaFormato;

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

    private static final Set<String> ROLES_VALIDOS = Set.of("PROPIETARIO", "VENDEDOR", "CLIENTE");

    // INSUSR["ci","nombre","apellido","rol","ci_nit","tel","email","pass"]
    public static Respuesta insertar(String[] params) {
        if (params.length < 8) {
            return new Respuesta(false, "INSUSR requiere 8 parametros: ci,nombre,apellido,rol,ci_nit,tel,email,pass");
        }

        String ci       = params[0].trim();
        String nombre   = params[1].trim();
        String apellido = params[2].trim();
        String rol      = params[3].trim().toUpperCase();
        String ciNit    = params[4].trim();
        String telefono = params[5].trim();
        String email    = params[6].trim();
        String password = params[7].trim();

        // ── Validaciones ──────────────────────────────────────────────────
        if (ci.isEmpty())
            return new Respuesta(false, "El CI no puede estar vacío.");
        if (!ci.matches("\\d+"))
            return new Respuesta(false, "CI inválido: \"" + ci + "\". Solo se permiten dígitos numéricos. Ej: \"12345678\"");

        if (nombre.isEmpty())
            return new Respuesta(false, "El nombre no puede estar vacío.");
        if (apellido.isEmpty())
            return new Respuesta(false, "El apellido no puede estar vacío.");

        if (!ROLES_VALIDOS.contains(rol))
            return new Respuesta(false,
                "Rol inválido: \"" + rol + "\".\n" +
                "Roles permitidos: PROPIETARIO | VENDEDOR | CLIENTE\n" +
                "Ej: INSUSR[\"12345678\",\"Maria\",\"Lopez\",\"CLIENTE\",...]");

        if (!telefono.isEmpty() && !telefono.matches("\\d+"))
            return new Respuesta(false, "Teléfono inválido: \"" + telefono + "\". Solo dígitos. Ej: \"76543210\"");

        if (!email.contains("@"))
            return new Respuesta(false, "Email inválido: \"" + email + "\". Debe contener @. Ej: \"maria@gmail.com\"");

        if (password.length() < 4)
            return new Respuesta(false, "La contraseña debe tener al menos 4 caracteres.");
        // ─────────────────────────────────────────────────────────────────

        String sql = "INSERT INTO usuario (ci,nombre,apellido,rol,ci_nit,telefono,email,password) " +
                     "VALUES (?,?,?,?,?,?,?,?)";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, ci);
            ps.setString(2, nombre);
            ps.setString(3, apellido);
            ps.setString(4, rol);
            ps.setString(5, ciNit);
            ps.setString(6, telefono);
            ps.setString(7, email);
            ps.setString(8, password);
            ps.executeUpdate();
            return new Respuesta(true, "Usuario insertado correctamente.\n" +
                    "Nombre: " + nombre + " " + apellido + "\nRol: " + rol + "\nCI: " + ci);
        } catch (Exception e) {
            // CI o email duplicado
            String msg = e.getMessage();
            if (msg != null && msg.contains("duplicate key")) {
                if (msg.contains("ci"))    return new Respuesta(false, "Ya existe un usuario con CI=\"" + ci + "\".");
                if (msg.contains("email")) return new Respuesta(false, "Ya existe un usuario con email=\"" + email + "\".");
            }
            return new Respuesta(false, "Error al insertar usuario: " + msg);
        }
    }

    // MODUSR["id","campo","valor"]
    public static Respuesta modificar(String[] params) {
        if (params.length < 3) {
            return new Respuesta(false, "MODUSR requiere 3 parametros: id,campo,valor");
        }
        Set<String> camposValidos = Set.of("nombre","apellido","rol","ci_nit","telefono","email","password");
        String campo = params[1].trim().toLowerCase();
        String valor = params[2].trim();

        if (!camposValidos.contains(campo)) {
            return new Respuesta(false,
                "Campo no permitido: \"" + campo + "\".\n" +
                "Campos válidos: nombre | apellido | rol | ci_nit | telefono | email | password");
        }

        // ── Validaciones por campo ────────────────────────────────────────
        switch (campo) {
            case "rol" -> {
                valor = valor.toUpperCase();
                if (!ROLES_VALIDOS.contains(valor))
                    return new Respuesta(false,
                        "Rol inválido: \"" + valor + "\".\n" +
                        "Roles permitidos: PROPIETARIO | VENDEDOR | CLIENTE");
            }
            case "telefono" -> {
                if (!valor.isEmpty() && !valor.matches("\\d+"))
                    return new Respuesta(false,
                        "Teléfono inválido: \"" + valor + "\". Solo dígitos. Ej: \"76543210\"");
            }
            case "email" -> {
                if (!valor.contains("@"))
                    return new Respuesta(false,
                        "Email inválido: \"" + valor + "\". Debe contener @. Ej: \"maria@gmail.com\"");
            }
            case "password" -> {
                if (valor.length() < 4)
                    return new Respuesta(false, "La contraseña debe tener al menos 4 caracteres.");
            }
            case "nombre", "apellido" -> {
                if (valor.isEmpty())
                    return new Respuesta(false, "El campo \"" + campo + "\" no puede estar vacío.");
            }
        }
        // ─────────────────────────────────────────────────────────────────

        String sql = "UPDATE usuario SET " + campo + "=? WHERE id=?";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, valor);
            ps.setInt(2, Integer.parseInt(params[0]));
            int filas = ps.executeUpdate();
            if (filas == 0) return new Respuesta(false, "No existe usuario con id=" + params[0]);
            return new Respuesta(true, "Usuario id=" + params[0] + " actualizado: " + campo + " = " + valor);
        } catch (NumberFormatException e) {
            return new Respuesta(false, "ID inválido: \"" + params[0] + "\". Debe ser un número.");
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
