package ritech.exportaciones.servicio;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import ritech.exportaciones.modelo.TasaCambio;

/**
 * Consulta la tasa de cambio del dia en internet.
 *
 * De donde sale cada cifra:
 *
 * 1. Pesos por dolar: de la TRM oficial, que publica la
 *    Superintendencia Financiera de Colombia en datos.gov.co.
 *    Esa es la tasa con la que legalmente se liquida una
 *    exportacion en Colombia, no una cotizacion cualquiera.
 *
 * 2. Pesos por euro: la TRM solo cubre el dolar, asi que se
 *    combina con la tasa euro-dolar del Banco Central Europeo.
 *    Si un euro vale 1,08 dolares y un dolar vale 4.100 pesos,
 *    entonces un euro vale 1,08 x 4.100 pesos.
 *
 * La consulta se hace una sola vez al dia y se guarda en memoria,
 * para no estar preguntandole a internet en cada pantalla.
 *
 * Si no hay internet, el sistema NO se cae: devuelve el motivo y
 * la pantalla deja escribir la tasa a mano, como se hacia antes.
 *
 * Nota sobre como se lee la respuesta: las dos direcciones
 * devuelven un texto en formato JSON. En vez de agregar una
 * libreria entera al proyecto solo para sacar dos numeros, se
 * buscan directamente con una expresion regular. Son respuestas
 * cortas y con una forma conocida.
 */
@Service
public class TasaCambioServicio {

    // Se piden varias filas, no una: la TRM se publica con un dia de
    // anticipacion, asi que la mas reciente suele ser la de manana.
    // De las que vengan, abajo se escoge la que rige hoy.
    private static final String URL_TRM =
        "https://www.datos.gov.co/resource/32sa-8pi3.json?$limit=8&$order=vigenciadesde%20DESC";

    private static final String URL_EURO =
        "https://api.frankfurter.app/latest?from=EUR&to=USD";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    // Memoria del dia
    private LocalDate diaGuardado;
    private TasaCambio guardada;

    public synchronized TasaCambio consultar() {

        // Si ya se consulto hoy y salio bien, se reutiliza
        if (guardada != null && LocalDate.now().equals(diaGuardado)) {
            return guardada;
        }

        TasaCambio tasa = new TasaCambio();

        try {
            String respuesta = pedir(URL_TRM);

            // De todas las filas publicadas, la que esta rigiendo hoy
            String fila = filaVigenteHoy(respuesta);

            if (fila == null) {
                // Ninguna ha entrado en vigencia todavia: se usa lo que haya
                fila = respuesta;
            }

            String valor = buscar(fila, "valor");

            if (valor == null) {
                throw new RuntimeException(
                    "La respuesta no trae el campo 'valor'. Vino asi: " + recortar(respuesta));
            }

            double copPorDolar = Double.parseDouble(valor.replace(",", "."));

            tasa.setCopPorDolar(copPorDolar);
            tasa.setFecha(fechaDe(fila));
            tasa.setFuente("TRM oficial - Superintendencia Financiera de Colombia");

            // El euro es opcional: si falla, al menos queda el dolar
            try {
                String euro = pedir(URL_EURO);
                String usd = buscar(euro, "USD");

                if (usd != null) {
                    double dolaresPorEuro = Double.parseDouble(usd);
                    tasa.setCopPorEuro(copPorDolar * dolaresPorEuro);
                }

            } catch (Exception e) {
                System.out.println("[RiTech] No se pudo traer la tasa del euro: " + e);
            }

            // Solo se guarda cuando salio bien, para que un corte de
            // internet no deje el sistema sin tasa por el resto del dia
            guardada = tasa;
            diaGuardado = LocalDate.now();

            System.out.println("[RiTech] TRM del " + tasa.getFecha() + ": "
                             + copPorDolar + " pesos por dolar");

        } catch (Exception e) {
            tasa.setError("No se pudo consultar la tasa automaticamente. "
                        + "Escribala a mano. Detalle: " + e.getMessage());
            System.out.println("[RiTech] Error consultando la TRM: " + e);
        }

        return tasa;
    }

    // ----------------------------------------------------------
    // Pide una direccion de internet y devuelve el texto crudo
    // ----------------------------------------------------------
    private String pedir(String url) throws Exception {

        HttpRequest peticion = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> respuesta =
                http.send(peticion, HttpResponse.BodyHandlers.ofString());

        if (respuesta.statusCode() != 200) {
            throw new RuntimeException("El servidor respondio " + respuesta.statusCode());
        }

        return respuesta.body();
    }

    // ----------------------------------------------------------
    // Escoge, entre las filas que devolvio el portal, la TRM que
    // esta rigiendo hoy.
    //
    // Por que hace falta: la Superintendencia calcula hoy la tasa
    // que va a regir manana, asi que lo mas reciente publicado es
    // del dia siguiente. Para un pedido que se registra hoy, la
    // tasa que legalmente aplica es la de hoy, no la de manana.
    //
    // Las fechas vienen como 2026-11-19T00:00:00.000, y en ese
    // formato comparar los textos es lo mismo que comparar las
    // fechas: el ano va primero, luego el mes y luego el dia.
    // ----------------------------------------------------------
    private String filaVigenteHoy(String json) {

        String hoy = LocalDate.now().toString();

        String mejorFila = null;
        String mejorFecha = null;

        for (String fila : json.split("\\},\\s*\\{")) {

            String desde = buscar(fila, "vigenciadesde");
            if (desde == null) {
                continue;
            }

            String fecha = desde.length() >= 10 ? desde.substring(0, 10) : desde;

            // Ya entro en vigencia y es la mas reciente de las que cumplen
            if (fecha.compareTo(hoy) <= 0
                && (mejorFecha == null || fecha.compareTo(mejorFecha) > 0)) {
                mejorFila = fila;
                mejorFecha = fecha;
            }
        }

        return mejorFila;
    }

    // ----------------------------------------------------------
    // Saca el valor de un campo dentro del texto JSON.
    //
    // Sirve tanto para  "valor":"3950.00"  (entre comillas)
    // como para         "USD":1.08         (sin comillas)
    // ----------------------------------------------------------
    private String buscar(String json, String campo) {

        if (json == null) {
            return null;
        }

        Pattern patron = Pattern.compile(
                "\"" + Pattern.quote(campo) + "\"\\s*:\\s*\"?([^\",}\\]]+)\"?");

        Matcher encontrado = patron.matcher(json);

        return encontrado.find() ? encontrado.group(1).trim() : null;
    }

    // ----------------------------------------------------------
    // La fecha viene como 2026-11-19T00:00:00.000
    // ----------------------------------------------------------
    private String fechaDe(String json) {

        String desde = buscar(json, "vigenciadesde");

        if (desde == null) {
            return LocalDate.now().toString();
        }

        return desde.length() >= 10 ? desde.substring(0, 10) : desde;
    }

    // Para que un mensaje de error no ocupe la consola entera
    private String recortar(String texto) {
        if (texto == null) {
            return "(vacio)";
        }
        return texto.length() > 200 ? texto.substring(0, 200) + "..." : texto;
    }
}
