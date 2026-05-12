import java.util.Properties;

public class ConexionCorreo {

    static final String MODE = ConexionDB.MODE;

    private static final String HOST = MODE.equals("SERVER")
            ? "localhost"
            : "mail.tecnoweb.org.bo";

    static final String POP3_HOST = HOST;
    static final int    POP3_PORT = 110;

    static final String SMTP_HOST = HOST;
    static final int    SMTP_PORT = 25;

    static final String USUARIO  = "grupo14sa";
    static final String PASSWORD = "grup014grup014*";
    static final String CORREO   = "grupo14sa@tecnoweb.org.bo";

    static Properties getPop3Props() {
        Properties p = new Properties();
        p.put("mail.pop3.host",            POP3_HOST);
        p.put("mail.pop3.port",            String.valueOf(POP3_PORT));
        p.put("mail.pop3.ssl.enable",      "false");
        p.put("mail.pop3.starttls.enable", "false");
        return p;
    }

    static Properties getSmtpProps() {
        Properties p = new Properties();
        p.put("mail.smtp.host",            SMTP_HOST);
        p.put("mail.smtp.port",            String.valueOf(SMTP_PORT));
        p.put("mail.smtp.auth",            "false");
        p.put("mail.smtp.starttls.enable", "false");
        return p;
    }
}
