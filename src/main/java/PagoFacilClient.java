import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class PagoFacilClient {

    private static final Gson gson = new Gson();
    private static final HttpClient http = HttpClient.newHttpClient();

    // Token cacheado en memoria
    private static String accessToken = null;

    /**
     * Autentica con PagoFácil y obtiene el accessToken.
     * Si ya tenemos un token en caché, intenta usarlo primero.
     */
    public static String login() throws Exception {
        if (accessToken != null) return accessToken;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(PagoFacilConfig.BASE_URL + "/login"))
                .header("Content-Type", "application/json")
                .header("tcTokenService", PagoFacilConfig.TOKEN_SERVICE)
                .header("tcTokenSecret", PagoFacilConfig.TOKEN_SECRET)
                .POST(HttpRequest.BodyPublishers.ofString(""))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        JsonObject json = gson.fromJson(resp.body(), JsonObject.class);

        if (json.get("error").getAsInt() != 0) {
            throw new Exception("PagoFacil login falló: " + json.get("message").getAsString());
        }

        accessToken = json.getAsJsonObject("values").get("accessToken").getAsString();
        System.out.println("  [PagoFacil] Login OK, token obtenido.");
        return accessToken;
    }

    /**
     * Fuerza un nuevo login (por si el token expiró).
     */
    public static String relogin() throws Exception {
        accessToken = null;
        return login();
    }

    /**
     * Genera un QR de pago en PagoFácil.
     *
     * @return JsonObject con: transactionId, checkoutUrl, qrBase64
     */
    public static JsonObject generarQR(String paymentNumber, BigDecimal monto,
                                        String clientName, String email, String telefono,
                                        String documentId, List<DetalleQR> detalle) throws Exception {
        String token = login();

        // Armar el orderDetail
        JsonArray orderDetail = new JsonArray();
        for (int i = 0; i < detalle.size(); i++) {
            DetalleQR d = detalle.get(i);
            JsonObject item = new JsonObject();
            item.addProperty("serial", i + 1);
            item.addProperty("product", d.producto);
            item.addProperty("quantity", d.cantidad);
            item.addProperty("price", d.precio);
            item.addProperty("discount", 0);
            item.addProperty("total", d.subtotal);
            orderDetail.add(item);
        }

        // Armar el body
        JsonObject body = new JsonObject();
        body.addProperty("paymentMethod", 34); // QR estándar
        body.addProperty("clientName", clientName);
        body.addProperty("documentType", 1);
        body.addProperty("documentId", documentId);
        body.addProperty("phoneNumber", telefono);
        body.addProperty("email", email);
        body.addProperty("paymentNumber", paymentNumber);
        body.addProperty("amount", monto);
        body.addProperty("currency", 2); // BOB (Bolivianos)
        body.addProperty("clientCode", documentId);
        body.addProperty("callbackUrl", "https://www.tecnoweb.org.bo");
        body.add("orderDetail", orderDetail);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(PagoFacilConfig.BASE_URL + "/generate-qr"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        JsonObject json = gson.fromJson(resp.body(), JsonObject.class);

        // Si el token expiró, hacer relogin y reintentar
        if (json.get("error").getAsInt() != 0 && resp.statusCode() == 401) {
            token = relogin();
            req = HttpRequest.newBuilder()
                    .uri(URI.create(PagoFacilConfig.BASE_URL + "/generate-qr"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                    .build();
            resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            json = gson.fromJson(resp.body(), JsonObject.class);
        }

        if (json.get("error").getAsInt() != 0) {
            throw new Exception("PagoFacil generarQR falló: " + json.get("message").getAsString());
        }

        System.out.println("  [PagoFacil] QR generado exitosamente.");
        System.out.println("  [PagoFacil] Respuesta: " + json.getAsJsonObject("values").toString());
        return json.getAsJsonObject("values");
    }

    /**
     * Consulta el estado de una transacción en PagoFácil.
     *
     * @return JsonObject con: paymentStatus, amount, paymentDate, etc.
     */
    public static JsonObject consultarTransaccion(String pagofacilTransactionId) throws Exception {
        String token = login();

        JsonObject body = new JsonObject();
        body.addProperty("pagofacilTransactionId", Long.parseLong(pagofacilTransactionId));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(PagoFacilConfig.BASE_URL + "/query-transaction"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        JsonObject json = gson.fromJson(resp.body(), JsonObject.class);

        // Si el token expiró, hacer relogin y reintentar
        if (json.get("error").getAsInt() != 0 && resp.statusCode() == 401) {
            token = relogin();
            req = HttpRequest.newBuilder()
                    .uri(URI.create(PagoFacilConfig.BASE_URL + "/query-transaction"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                    .build();
            resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            json = gson.fromJson(resp.body(), JsonObject.class);
        }

        if (json.get("error").getAsInt() != 0) {
            throw new Exception("PagoFacil consulta falló: " + json.get("message").getAsString());
        }

        return json.getAsJsonObject("values");
    }

    /**
     * Clase auxiliar para representar un producto en el detalle del QR.
     */
    public static class DetalleQR {
        public final String producto;
        public final int cantidad;
        public final BigDecimal precio;
        public final BigDecimal subtotal;

        public DetalleQR(String producto, int cantidad, BigDecimal precio, BigDecimal subtotal) {
            this.producto = producto;
            this.cantidad = cantidad;
            this.precio   = precio;
            this.subtotal = subtotal;
        }
    }
}
