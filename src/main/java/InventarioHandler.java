import java.sql.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class InventarioHandler {

    // LISINV["id_producto"]
    public static Respuesta listar(String[] params) {
        if (params.length < 1) {
            return new Respuesta(false, "LISINV requiere 1 parametro: id_producto");
        }
        String sql = "SELECT id, tipo, cantidad, costo_unitario, tecnica, fecha, observaciones " +
                     "FROM inventario_movimiento WHERE id_producto=? ORDER BY fecha";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(params[0]));
            ResultSet rs = ps.executeQuery();
            return new Respuesta(true, TablaFormato.formatear(rs));
        } catch (Exception e) {
            return new Respuesta(false, "Error al listar inventario: " + e.getMessage());
        }
    }

    // INSINV["id_producto","tipo","cantidad","costo","tecnica"]
    public static Respuesta insertar(String[] params) {
        if (params.length < 5) {
            return new Respuesta(false, "INSINV requiere 5 parametros: id_producto,tipo,cantidad,costo,tecnica");
        }
        int idProducto = Integer.parseInt(params[0]);
        String tipo    = params[1].toUpperCase();
        int cantidad   = Integer.parseInt(params[2]);
        BigDecimal costo = new BigDecimal(params[3]);
        String tecnica = params[4].toUpperCase();

        if (!tipo.equals("INGRESO") && !tipo.equals("SALIDA")) {
            return new Respuesta(false, "Tipo invalido: " + tipo + ". Use INGRESO o SALIDA.");
        }
        if (!tecnica.equals("PEPS") && !tecnica.equals("UEPS") && !tecnica.equals("PROMEDIO")) {
            return new Respuesta(false, "Tecnica invalida: " + tecnica + ". Use PEPS, UEPS o PROMEDIO.");
        }

        String sqlStock = "SELECT stock FROM producto WHERE id=? AND activo=true";
        String sqlMov   = "INSERT INTO inventario_movimiento (id_producto, tipo, cantidad, costo_unitario, tecnica) VALUES (?,?,?,?,?)";
        String sqlUpd   = tipo.equals("INGRESO")
                ? "UPDATE producto SET stock = stock + ? WHERE id=?"
                : "UPDATE producto SET stock = stock - ? WHERE id=?";

        try (Connection con = ConexionDB.getConnection()) {
            // Validar stock suficiente para salidas
            if (tipo.equals("SALIDA")) {
                try (PreparedStatement ps = con.prepareStatement(sqlStock)) {
                    ps.setInt(1, idProducto);
                    ResultSet rs = ps.executeQuery();
                    if (!rs.next()) return new Respuesta(false, "Producto id=" + idProducto + " no existe.");
                    int stockActual = rs.getInt("stock");
                    if (stockActual < cantidad) {
                        return new Respuesta(false, "Stock insuficiente. Actual: " + stockActual + ", Solicitado: " + cantidad);
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement(sqlMov)) {
                ps.setInt(1, idProducto);
                ps.setString(2, tipo);
                ps.setInt(3, cantidad);
                ps.setBigDecimal(4, costo);
                ps.setString(5, tecnica);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement(sqlUpd)) {
                ps.setInt(1, cantidad);
                ps.setInt(2, idProducto);
                ps.executeUpdate();
            }

            return new Respuesta(true, tipo + " registrado: " + cantidad + " unidades del producto id=" + idProducto +
                    " | Tecnica: " + tecnica + " | Costo unitario: " + costo);
        } catch (Exception e) {
            return new Respuesta(false, "Error al registrar movimiento: " + e.getMessage());
        }
    }

    // VALINV["id_producto"]
    public static Respuesta valorizar(String[] params) {
        if (params.length < 1) {
            return new Respuesta(false, "VALINV requiere 1 parametro: id_producto");
        }
        int idProducto = Integer.parseInt(params[0]);

        String sqlProd = "SELECT nombre, stock FROM producto WHERE id=?";
        String sqlMovs = "SELECT tipo, cantidad, costo_unitario, fecha FROM inventario_movimiento " +
                         "WHERE id_producto=? ORDER BY fecha";

        try (Connection con = ConexionDB.getConnection()) {
            String nombreProd;
            int stockActual;
            try (PreparedStatement ps = con.prepareStatement(sqlProd)) {
                ps.setInt(1, idProducto);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) return new Respuesta(false, "Producto id=" + idProducto + " no existe.");
                nombreProd  = rs.getString("nombre");
                stockActual = rs.getInt("stock");
            }

            List<int[]>         cantidades = new ArrayList<>();
            List<BigDecimal>    costos     = new ArrayList<>();
            int totalIngresado = 0, totalSalido = 0;
            BigDecimal totalCostoIngresos = BigDecimal.ZERO;

            try (PreparedStatement ps = con.prepareStatement(sqlMovs)) {
                ps.setInt(1, idProducto);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    int cant = rs.getInt("cantidad");
                    BigDecimal costo = rs.getBigDecimal("costo_unitario");
                    if (costo == null) costo = BigDecimal.ZERO;
                    if ("INGRESO".equals(rs.getString("tipo"))) {
                        cantidades.add(new int[]{cant});
                        costos.add(costo);
                        totalIngresado += cant;
                        totalCostoIngresos = totalCostoIngresos.add(costo.multiply(BigDecimal.valueOf(cant)));
                    } else {
                        totalSalido += cant;
                    }
                }
            }

            // PEPS: consume los lotes más antiguos primero
            BigDecimal valorPEPS = calcularValor(cantidades, costos, stockActual, true);
            // UEPS: consume los lotes más nuevos primero
            BigDecimal valorUEPS = calcularValor(cantidades, costos, stockActual, false);
            // PROMEDIO ponderado
            BigDecimal costoPromedio = totalIngresado > 0
                    ? totalCostoIngresos.divide(BigDecimal.valueOf(totalIngresado), 2, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal valorPromedio = costoPromedio.multiply(BigDecimal.valueOf(stockActual));

            String resultado = "Producto: " + nombreProd + " (id=" + idProducto + ")\n" +
                    "Stock actual    : " + stockActual + " unidades\n" +
                    "Total ingresado : " + totalIngresado + " unidades\n" +
                    "Total salido    : " + totalSalido + " unidades\n\n" +
                    "Valorización del inventario:\n" +
                    "  PEPS    (primero en entrar, primero en salir): " + valorPEPS + " Bs.\n" +
                    "  UEPS    (ultimo en entrar, primero en salir) : " + valorUEPS + " Bs.\n" +
                    "  PROMEDIO (costo promedio ponderado)          : " + valorPromedio + " Bs.";

            return new Respuesta(true, resultado);
        } catch (Exception e) {
            return new Respuesta(false, "Error al valorizar inventario: " + e.getMessage());
        }
    }

    // PEPS: fifo=true, UEPS: fifo=false
    private static BigDecimal calcularValor(List<int[]> cantidades, List<BigDecimal> costos, int stockActual, boolean fifo) {
        // Clonar lotes para no mutar la lista original
        int n = cantidades.size();
        int[] lotes = new int[n];
        for (int i = 0; i < n; i++) lotes[i] = cantidades.get(i)[0];

        // Calcular total ingresado para saber cuánto se consumió
        int totalIngresado = 0;
        for (int l : lotes) totalIngresado += l;
        int consumido = totalIngresado - stockActual;

        // Consumir lotes según técnica
        if (fifo) {
            for (int i = 0; i < n && consumido > 0; i++) {
                int quitar = Math.min(lotes[i], consumido);
                lotes[i] -= quitar;
                consumido -= quitar;
            }
        } else {
            for (int i = n - 1; i >= 0 && consumido > 0; i--) {
                int quitar = Math.min(lotes[i], consumido);
                lotes[i] -= quitar;
                consumido -= quitar;
            }
        }

        BigDecimal valor = BigDecimal.ZERO;
        for (int i = 0; i < n; i++) {
            if (lotes[i] > 0) {
                valor = valor.add(costos.get(i).multiply(BigDecimal.valueOf(lotes[i])));
            }
        }
        return valor.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
