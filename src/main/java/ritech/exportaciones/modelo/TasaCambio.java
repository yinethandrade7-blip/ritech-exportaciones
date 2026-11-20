package ritech.exportaciones.modelo;

/**
 * Guarda el resultado de consultar la tasa de cambio del dia.
 *
 * Si la consulta salio bien, trae cuantos pesos vale un dolar y
 * cuantos vale un euro. Si salio mal, trae el motivo en "error"
 * y el sistema sigue funcionando: el usuario escribe la tasa a mano.
 */
public class TasaCambio {

    private Double copPorDolar;
    private Double copPorEuro;
    private String fecha;
    private String fuente;
    private String error;

    public Double getCopPorDolar() {
        return copPorDolar;
    }

    public void setCopPorDolar(Double copPorDolar) {
        this.copPorDolar = copPorDolar;
    }

    public Double getCopPorEuro() {
        return copPorEuro;
    }

    public void setCopPorEuro(Double copPorEuro) {
        this.copPorEuro = copPorEuro;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getFuente() {
        return fuente;
    }

    public void setFuente(String fuente) {
        this.fuente = fuente;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    /** True cuando al menos se pudo traer la TRM del dolar. */
    public boolean isDisponible() {
        return copPorDolar != null;
    }

    /**
     * Devuelve cuantos pesos vale una unidad de esa moneda,
     * o null si no se sabe.
     */
    public Double paraMoneda(String codigoIso) {

        if (codigoIso == null) {
            return null;
        }

        if ("USD".equalsIgnoreCase(codigoIso)) {
            return copPorDolar;
        }

        if ("EUR".equalsIgnoreCase(codigoIso)) {
            return copPorEuro;
        }

        if ("COP".equalsIgnoreCase(codigoIso)) {
            return 1.0;
        }

        return null;
    }
}
