package moroncorreo.app;

import moroncorreo.negocio.Respuesta;
import moroncorreo.negocio.administracion.*;
import moroncorreo.negocio.comercial.*;
import moroncorreo.negocio.operacion.*;

import java.util.regex.*;

public class ProcesadorComandos {

    private static final Pattern PATRON = Pattern.compile("^([A-Z]+)\\[(.+)\\]$");

    public static Respuesta procesar(String subject) {
        if (subject == null || subject.isBlank()) {
            return new Respuesta(false, "Subject vacio o nulo.");
        }

        // HELP no necesita corchetes — funciona solo con "HELP"
        if (subject.trim().equalsIgnoreCase("HELP")) {
            return HelpHandler.ayuda(new String[0]);
        }

        Matcher m = PATRON.matcher(subject.trim());
        if (!m.matches()) {
            return new Respuesta(false,
                    "Formato invalido. Use: COMANDO[\"param1\",\"param2\"]\n" +
                    "Envíe HELP para ver todos los comandos disponibles.");
        }

        String   comando = m.group(1);
        String[] params  = parsearParams(m.group(2));

        Respuesta r = switch (comando) {
            // CU1 - Usuarios
            case "LISUSR" -> UsuarioHandler.listar(params);
            case "INSUSR" -> UsuarioHandler.insertar(params);
            case "MODUSR" -> UsuarioHandler.modificar(params);
            case "ELIUSR" -> UsuarioHandler.eliminar(params);

            // CU2 - Productos
            case "LISPROD" -> ProductoHandler.listar(params);
            case "INSPROD" -> ProductoHandler.insertar(params);
            case "MODPROD" -> ProductoHandler.modificar(params);
            case "ELIPROD" -> ProductoHandler.eliminar(params);

            // CU3 - Pedidos
            case "LISPED"  -> PedidoHandler.listar(params);
            case "INSPED"  -> PedidoHandler.insertar(params);
            case "INSDET"  -> PedidoHandler.insertarDetalle(params);
            case "MODPED"  -> PedidoHandler.modificar(params);

            // CU4 - Inventario
            case "LISINV"  -> InventarioHandler.listar(params);
            case "INSINV"  -> InventarioHandler.insertar(params);
            case "VALINV"  -> InventarioHandler.valorizar(params);

            // CU5 - Promociones
            case "LISPRO"  -> PromocionHandler.listar(params);
            case "INSPRO"  -> PromocionHandler.insertar(params);
            case "MODPRO"  -> PromocionHandler.modificar(params);
            case "ELIPRO"  -> PromocionHandler.eliminar(params);

            // CU6 - Ventas
            case "LISVEN"  -> VentaHandler.listar(params);
            case "INSVEN"  -> VentaHandler.insertar(params);
            case "MODVEN"  -> VentaHandler.modificar(params);

            // CU7 - Pagos
            case "LISPAG"  -> PagoHandler.listar(params);
            case "INSPAG"  -> PagoHandler.insertar(params);
            case "MODPAG"  -> PagoHandler.modificar(params);
            case "VERPAG"  -> PagoHandler.verificarQR(params);

            // CU8 - Reportes
            case "REPVEN"  -> ReporteHandler.ventasMes(params);
            case "REPINV"  -> ReporteHandler.stockActual(params);
            case "REPPRO"  -> ReporteHandler.promocionesActivas(params);
            case "REPCUO"  -> ReporteHandler.cuotasPendientes(params);

            // Ayuda
            case "HELP"    -> HelpHandler.ayuda(params);

            default -> new Respuesta(false, "Comando desconocido: " + comando +
                    ". Envíe HELP[\"*\"] para ver todos los comandos disponibles.");
        };

        // Si el handler reportó error, verificar si fue culpa de la BD
        if (!r.exito && !r.errorBD && esErrorBD(r.mensaje)) {
            return Respuesta.errorBD(r.mensaje);
        }
        return r;
    }

    /**
     * Detecta si un mensaje de error corresponde a un fallo de BD/conexión.
     */
    private static boolean esErrorBD(String msg) {
        if (msg == null) return false;
        String lower = msg.toLowerCase();
        return lower.contains("connection")
            || lower.contains("refused")
            || lower.contains("timeout")
            || lower.contains("jdbc")
            || lower.contains("postgresql")
            || lower.contains("sql")
            || lower.contains("socket");
    }

    // "param1", "param2", "param3"  →  ["param1","param2","param3"]
    static String[] parsearParams(String paramsStr) {
        String[] parts = paramsStr.split("\"\\s*,\\s*\"");
        if (parts[0].startsWith("\""))
            parts[0] = parts[0].substring(1);
        String last = parts[parts.length - 1];
        if (last.endsWith("\""))
            parts[parts.length - 1] = last.substring(0, last.length() - 1);
        return parts;
    }
}
