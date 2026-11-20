package ritech.exportaciones.modelo;

/**
 * Cuanta plata le debe RiTech a un productor.
 *
 * No es una tabla de la base de datos: es el resultado de sumar
 * los lotes de ese productor. Por eso no tiene id propio.
 */
public class DeudaProveedor {

    private int idProveedor;
    private String nombre;
    private String finca;
    private int lotesPendientes;
    private double kilosPendientes;
    private double totalPendiente;
    private double totalPagado;

    public int getIdProveedor() {
        return idProveedor;
    }

    public void setIdProveedor(int idProveedor) {
        this.idProveedor = idProveedor;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getFinca() {
        return finca;
    }

    public void setFinca(String finca) {
        this.finca = finca;
    }

    public int getLotesPendientes() {
        return lotesPendientes;
    }

    public void setLotesPendientes(int lotesPendientes) {
        this.lotesPendientes = lotesPendientes;
    }

    public double getKilosPendientes() {
        return kilosPendientes;
    }

    public void setKilosPendientes(double kilosPendientes) {
        this.kilosPendientes = kilosPendientes;
    }

    public double getTotalPendiente() {
        return totalPendiente;
    }

    public void setTotalPendiente(double totalPendiente) {
        this.totalPendiente = totalPendiente;
    }

    public double getTotalPagado() {
        return totalPagado;
    }

    public void setTotalPagado(double totalPagado) {
        this.totalPagado = totalPagado;
    }

    public boolean isDebeAlgo() {
        return totalPendiente > 0;
    }
}
