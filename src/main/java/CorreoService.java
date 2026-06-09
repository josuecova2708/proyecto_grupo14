import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Arrays;
import java.util.Date;

public class CorreoService {

    public static void procesar() {
        System.out.println("Iniciando ciclo de correo...");
        Session pop3Session = Session.getInstance(ConexionCorreo.getPop3Props());
        Store  store  = null;
        Folder inbox  = null;

        try {
            store = pop3Session.getStore("pop3");
            store.connect(ConexionCorreo.POP3_HOST, ConexionCorreo.USUARIO, ConexionCorreo.PASSWORD);

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            Message[] mensajes = inbox.getMessages();
            
            // Ordenar mensajes desde el más antiguo al más reciente (Cola / FIFO)
            Arrays.sort(mensajes, (m1, m2) -> {
                try {
                    Date d1 = m1.getSentDate();
                    Date d2 = m2.getSentDate();
                    if (d1 == null) return -1;
                    if (d2 == null) return 1;
                    return d1.compareTo(d2);
                } catch (Exception e) {
                    return 0; // Si hay error, mantener orden original
                }
            });

            System.out.println("Mensajes en bandeja: " + mensajes.length);

            for (Message msg : mensajes) {
                try {
                    procesarMensaje(msg);
                } catch (Exception e) {
                    System.out.println("  [ERROR procesando mensaje]: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.out.println("Error POP3: " + e.getMessage());
        } finally {
            try { if (inbox != null && inbox.isOpen()) inbox.close(true); } catch (Exception ignored) {}
            try { if (store != null && store.isConnected()) store.close();  } catch (Exception ignored) {}
        }
    }

    private static void procesarMensaje(Message msg) throws Exception {
        Address[] fromArr = msg.getFrom();
        if (fromArr == null || fromArr.length == 0) {
            msg.setFlag(Flags.Flag.DELETED, true);
            return;
        }

        InternetAddress fromAddr  = (InternetAddress) fromArr[0];
        String          fromEmail = fromAddr.getAddress();
        String          subject   = msg.getSubject();

        // Ignorar remitentes automáticos
        String fromLower = fromEmail.toLowerCase();
        if (fromLower.contains("noreply") || fromLower.contains("no-reply")
                || fromLower.contains("mailer-daemon") || fromLower.contains("postmaster")
                || fromLower.contains("bounce")) {
            System.out.println("  [IGNORADO] remitente automatico: " + fromEmail);
            msg.setFlag(Flags.Flag.DELETED, true);
            return;
        }

        System.out.println("  Procesando: " + fromEmail + " | " + subject);

        Respuesta respuesta = ProcesadorComandos.procesar(subject);
        String    cuerpo    = construirCuerpo(subject, respuesta);

        enviarRespuesta(fromEmail, "RE: " + subject, cuerpo);
        msg.setFlag(Flags.Flag.DELETED, true);

        System.out.println("  -> " + (respuesta.exito ? "EXITO" : "ERROR") + " | Respondido a: " + fromEmail);
    }

    private static String construirCuerpo(String subject, Respuesta respuesta) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Morón Diseño de Interiores ===\n");
        sb.append("Comando: ").append(subject).append("\n");
        sb.append("Estado:  ").append(respuesta.exito ? "ÉXITO" : "ERROR").append("\n\n");
        if (respuesta.exito) {
            sb.append(respuesta.mensaje).append("\n");
        } else {
            sb.append("Detalle: ").append(respuesta.mensaje).append("\n");
        }
        sb.append("\n---\n");
        sb.append("Sistema automático ").append(ConexionCorreo.CORREO);
        return sb.toString();
    }

    private static void enviarRespuesta(String to, String subject, String body) throws Exception {
        Session smtpSession = Session.getInstance(ConexionCorreo.getSmtpProps());
        MimeMessage reply = new MimeMessage(smtpSession);
        reply.setFrom(new InternetAddress(ConexionCorreo.CORREO));
        reply.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
        reply.setSubject(subject, "UTF-8");
        reply.setText(body, "UTF-8");

        Transport transport = smtpSession.getTransport("smtp");
        transport.connect(ConexionCorreo.SMTP_HOST, ConexionCorreo.SMTP_PORT, null, null);
        transport.sendMessage(reply, reply.getAllRecipients());
        transport.close();
    }
}
