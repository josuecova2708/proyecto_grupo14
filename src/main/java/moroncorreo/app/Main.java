package moroncorreo.app;

import moroncorreo.infra.ConexionDB;

public class Main {
    public static void main(String[] args) {
        System.out.println("MoronCorreo arrancando...");
        System.out.println("MODE: " + ConexionDB.MODE);

        // Verificar BD al arranque — solo avisa, no detiene el programa
        if (ConexionDB.estaDisponible()) {
            System.out.println("DB: OK");
        } else {
            System.out.println("DB: NO DISPONIBLE al arranque — se reintentará en cada ciclo.");
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
