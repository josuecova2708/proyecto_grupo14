import java.sql.Connection;

public class Main {
    public static void main(String[] args) {
        System.out.println("MoronCorreo arrancando...");
        System.out.println("MODE: " + ConexionDB.MODE);

        try (Connection con = ConexionDB.getConnection()) {
            System.out.println("DB: OK (" + con.getMetaData().getURL() + ")");
        } catch (Exception e) {
            System.out.println("DB FALLO: " + e.getMessage());
            return;
        }

        while (true) {
            CorreoService.procesar();
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                System.out.println("Ciclo interrumpido.");
                break;
            }
        }
    }
}
