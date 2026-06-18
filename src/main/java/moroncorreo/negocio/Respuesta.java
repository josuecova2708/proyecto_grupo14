package moroncorreo.negocio;

public class Respuesta {
    public final boolean exito;
    public final String  mensaje;
    public final byte[]  imagenAdjunta;  // QR u otra imagen (puede ser null)

    public Respuesta(boolean exito, String mensaje) {
        this.exito         = exito;
        this.mensaje       = mensaje;
        this.imagenAdjunta = null;
    }

    public Respuesta(boolean exito, String mensaje, byte[] imagenAdjunta) {
        this.exito         = exito;
        this.mensaje       = mensaje;
        this.imagenAdjunta = imagenAdjunta;
    }

    public boolean tieneImagen() {
        return imagenAdjunta != null && imagenAdjunta.length > 0;
    }
}
