package moroncorreo.infra;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionDB {

    public static final String MODE = System.getProperty("MODE", "DEV");

    private static final String HOST = MODE.equals("SERVER")
            ? "localhost"
            : "www.tecnoweb.org.bo";

    private static final String URL =
            "jdbc:postgresql://" + HOST + ":5432/db_grupo14sa";

    private static final String USER = "grupo14sa";
    private static final String PASS = "grup014grup014*";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    /**
     * Verifica si la BD está disponible con un ping rápido (timeout 3s).
     * No lanza excepción — devuelve true/false.
     */
    public static boolean estaDisponible() {
        try (Connection con = DriverManager.getConnection(URL, USER, PASS)) {
            return con.isValid(3); // timeout 3 segundos
        } catch (Exception e) {
            return false;
        }
    }
}
