package moroncorreo.negocio.comercial;

import moroncorreo.infra.ConexionDB;
import moroncorreo.negocio.Respuesta;
import moroncorreo.negocio.TablaFormato;

import java.sql.*;
import java.util.Set;

public class PedidoHandler {

    // LISPED["*"]
    public static Respuesta listar(String[] params) {
        String sql = "SELECT id, id_usuario, fecha, estado, total, observaciones FROM pedido ORDER BY id";
        try (Connection con = ConexionDB.getConnection();
             Statement st  = con.createStatement();
             ResultSet rs  = st.executeQuery(sql)) {
            return new Respuesta(true, TablaFormato.formatear(rs));
        } catch (Exception e) {
            return new Respuesta(false, "Error al listar pedidos: " + e.getMessage());
        }
    }

    // INSPED["id_usuario","observaciones"]
    public static Respuesta insertar(String[] params) {
        if (params.length < 2) {
            return new Respuesta(false, "INSPED requiere 2 parametros: id_usuario,observaciones");
        }
        String sql = "INSERT INTO pedido (id_usuario, observaciones) VALUES (?,?) RETURNING id";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(params[0]));
            ps.setString(2, params[1]);
            ResultSet rs = ps.executeQuery();
            rs.next();
            int id = rs.getInt(1);
            return new Respuesta(true, "Pedido creado con id=" + id + " (estado: PENDIENTE, total: 0)");
        } catch (Exception e) {
            return new Respuesta(false, "Error al insertar pedido: " + e.getMessage());
        }
    }

    // INSDET["id_pedido","id_producto","cantidad"]
    public static Respuesta insertarDetalle(String[] params) {
        if (params.length < 3) {
            return new Respuesta(false, "INSDET requiere 3 parametros: id_pedido,id_producto,cantidad");
        }
        int idPedido   = Integer.parseInt(params[0]);
        int idProducto = Integer.parseInt(params[1]);
        int cantidad   = Integer.parseInt(params[2]);

        // Trae precio base y, si hay promoción activa hoy, su porcentaje de descuento
        String sqlPrecio = """
                SELECT p.precio,
                       COALESCE(pr.porcentaje, 0) AS descuento
                FROM producto p
                LEFT JOIN promocion pr
                       ON pr.id_producto = p.id
                      AND pr.activo = true
                      AND CURRENT_DATE BETWEEN pr.fecha_inicio AND pr.fecha_fin
                WHERE p.id = ? AND p.activo = true
                LIMIT 1
                """;
        String sqlDet   = "INSERT INTO pedido_detalle (id_pedido, id_producto, cantidad, precio_unitario, subtotal) VALUES (?,?,?,?,?)";
        String sqlTotal = "UPDATE pedido SET total = total + ? WHERE id=?";

        try (Connection con = ConexionDB.getConnection()) {
            java.math.BigDecimal precioBase, descuento, precioFinal;
            try (PreparedStatement ps = con.prepareStatement(sqlPrecio)) {
                ps.setInt(1, idProducto);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) return new Respuesta(false, "Producto id=" + idProducto + " no existe o está inactivo.");
                precioBase = rs.getBigDecimal("precio");
                descuento  = rs.getBigDecimal("descuento");
            }

            // Aplicar descuento: precio_final = precio * (1 - descuento/100)
            java.math.BigDecimal factor = java.math.BigDecimal.ONE
                    .subtract(descuento.divide(java.math.BigDecimal.valueOf(100)));
            precioFinal = precioBase.multiply(factor)
                    .setScale(2, java.math.RoundingMode.HALF_UP);

            java.math.BigDecimal subtotal = precioFinal.multiply(java.math.BigDecimal.valueOf(cantidad));

            try (PreparedStatement ps = con.prepareStatement(sqlDet)) {
                ps.setInt(1, idPedido);
                ps.setInt(2, idProducto);
                ps.setInt(3, cantidad);
                ps.setBigDecimal(4, precioFinal);
                ps.setBigDecimal(5, subtotal);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement(sqlTotal)) {
                ps.setBigDecimal(1, subtotal);
                ps.setInt(2, idPedido);
                ps.executeUpdate();
            }

            String infoPromo = descuento.compareTo(java.math.BigDecimal.ZERO) > 0
                    ? " (promo -" + descuento + "%, precio base: " + precioBase + ")"
                    : "";
            return new Respuesta(true, "Detalle agregado al pedido id=" + idPedido +
                    ": " + cantidad + " x " + precioFinal + infoPromo + " = " + subtotal);
        } catch (Exception e) {
            return new Respuesta(false, "Error al insertar detalle: " + e.getMessage());
        }
    }

    // MODPED["id","estado"]
    public static Respuesta modificar(String[] params) {
        if (params.length < 2) {
            return new Respuesta(false, "MODPED requiere 2 parametros: id,estado");
        }
        Set<String> estadosValidos = Set.of("PENDIENTE","CONFIRMADO","ENTREGADO","CANCELADO");
        String estado = params[1].toUpperCase();
        if (!estadosValidos.contains(estado)) {
            return new Respuesta(false, "Estado invalido: " + estado + ". Validos: " + estadosValidos);
        }
        String sql = "UPDATE pedido SET estado=? WHERE id=?";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setInt(2, Integer.parseInt(params[0]));
            int filas = ps.executeUpdate();
            if (filas == 0) return new Respuesta(false, "No existe pedido con id=" + params[0]);
            return new Respuesta(true, "Pedido id=" + params[0] + " actualizado a estado: " + estado);
        } catch (Exception e) {
            return new Respuesta(false, "Error al modificar pedido: " + e.getMessage());
        }
    }
}
