import jakarta.mail.*;
import java.sql.Connection;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("MoronCorreo arrancando...");
        System.out.println("MODE: " + ConexionDB.MODE);

        // Prueba DB
        System.out.print("Conexion DB... ");
        try (Connection con = ConexionDB.getConnection()) {
            System.out.println("OK");
        } catch (Exception e) {
            System.out.println("FALLO: " + e.getMessage());
            return;
        }

        // Prueba POP3 - solo leer, no borrar
        System.out.println("Conectando POP3 " + ConexionCorreo.POP3_HOST + ":" + ConexionCorreo.POP3_PORT + "...");
        Session session = Session.getInstance(ConexionCorreo.getPop3Props());
        try (Store store = session.getStore("pop3")) {
            store.connect(ConexionCorreo.POP3_HOST, ConexionCorreo.USUARIO, ConexionCorreo.PASSWORD);
            System.out.println("POP3 OK");

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            int total = inbox.getMessageCount();
            System.out.println("Mensajes en bandeja: " + total);

            for (int i = 1; i <= total; i++) {
                Message msg = inbox.getMessage(i);
                System.out.println("  [" + i + "] De: " + msg.getFrom()[0]
                        + " | Subject: " + msg.getSubject());
            }

            inbox.close(false); // false = no expunge
        } catch (Exception e) {
            System.out.println("FALLO POP3: " + e.getMessage());
        }
    }
}
