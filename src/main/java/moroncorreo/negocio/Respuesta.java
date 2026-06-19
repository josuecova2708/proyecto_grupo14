package moroncorreo.negocio;

public class Respuesta {
    public final boolean exito;
    public final String  mensaje;
    public final byte[]  imagenAdjunta;  // QR u otra imagen (puede ser null)
    public final boolean errorBD;        // true = falló la BD, el correo NO debe eliminarse

    public Respuesta(boolean exito, String mensaje) {
        this.exito         = exito;
        this.mensaje       = mensaje;
        this.imagenAdjunta = null;
        this.errorBD       = false;
    }

    public Respuesta(boolean exito, String mensaje, byte[] imagenAdjunta) {
        this.exito         = exito;
        this.mensaje       = mensaje;
        this.imagenAdjunta = imagenAdjunta;
        this.errorBD       = false;
    }

    /** Usa este factory cuando el error es de BD — el mensaje se conservará para reintentar. */
    public static Respuesta errorBD(String detalle) {
        return new Respuesta(false, detalle, null, true);
    }

    private Respuesta(boolean exito, String mensaje, byte[] imagenAdjunta, boolean errorBD) {
        this.exito         = exito;
        this.mensaje       = mensaje;
        this.imagenAdjunta = imagenAdjunta;
        this.errorBD       = errorBD;
    }

    public boolean tieneImagen() {
        return imagenAdjunta != null && imagenAdjunta.length > 0;
    }
}
